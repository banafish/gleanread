package com.gleanread.android.data.local

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 多账号数据库管理器。
 *
 * 支持按 user_id 独立管理 Room 数据库实例：
 * - 访客态：使用 [GUEST_DB_NAME] (guest.db)
 * - 已登录：使用 local_data_{userId}.db
 *
 * 切换账号时只需调用 [switchToUser] / [switchToGuest]，
 * UI 层通过 [currentDatabase] 响应式获取当前活跃数据库。
 */
class WorkspaceDatabaseManager(
    private val appContext: Context,
) {
    private val _currentDatabase = MutableStateFlow<WorkspaceDatabase>(openDatabase(GUEST_DB_NAME))
    val currentDatabase: StateFlow<WorkspaceDatabase> = _currentDatabase.asStateFlow()

    private val databaseCache = mutableMapOf<String, WorkspaceDatabase>()
    private var currentDbName: String = GUEST_DB_NAME

    init {
        databaseCache[GUEST_DB_NAME] = _currentDatabase.value
    }

    /**
     * 切换到指定用户的数据库。如果数据库文件已存在则直接打开（零延迟），
     * 否则创建新的空数据库。
     */
    fun switchToUser(userId: String) {
        val dbName = userDbName(userId)
        if (dbName == currentDbName) return
        val db = getOrCreateDatabase(dbName)
        _currentDatabase.value = db
        currentDbName = dbName
        Log.d(TAG, "Switched to user database: $dbName")
    }

    /**
     * 切换到访客数据库。
     */
    fun switchToGuest() {
        if (currentDbName == GUEST_DB_NAME) return
        val db = getOrCreateDatabase(GUEST_DB_NAME)
        _currentDatabase.value = db
        currentDbName = GUEST_DB_NAME
        Log.d(TAG, "Switched to guest database")
    }

    /**
     * 关闭当前数据库连接并切回访客态。
     * 不删除数据库文件，以便下次登录时零延迟加载。
     */
    fun closeCurrentDatabase() {
        val currentDb = _currentDatabase.value
        if (currentDb.isOpen) {
            currentDb.close()
        }
        databaseCache.remove(currentDbName)
        switchToGuest()
    }

    /**
     * 获取访客数据库实例。
     * 即使当前活跃数据库不是 guest.db，也可通过此属性访问访客数据库。
     */
    val guestDatabase: WorkspaceDatabase
        get() = getOrCreateDatabase(GUEST_DB_NAME)

    /**
     * 关闭并删除指定用户的数据库文件。
     * 不可删除当前活跃数据库或访客数据库。
     *
     * @return true 如果成功删除
     */
    fun deleteDatabase(dbName: String): Boolean {
        if (dbName == currentDbName || dbName == GUEST_DB_NAME) return false
        databaseCache[dbName]?.let { db ->
            if (db.isOpen) db.close()
            databaseCache.remove(dbName)
        }
        val deleted = appContext.deleteDatabase(dbName)
        if (deleted) {
            Log.d(TAG, "Deleted database: $dbName")
        }
        return deleted
    }

    /**
     * 获取所有本地数据库文件名（不含 guest.db）。
     */
    fun listUserDatabases(): List<String> {
        val dbDir = appContext.getDatabasePath(GUEST_DB_NAME).parentFile ?: return emptyList()
        return dbDir.listFiles()
            ?.filter { it.name.startsWith(USER_DB_PREFIX) && it.name.endsWith(DB_SUFFIX) }
            ?.map { it.name }
            ?: emptyList()
    }

    /**
     * 关闭所有非当前活跃数据库的连接。
     */
    fun closeInactiveDatabases() {
        val entriesToClose = databaseCache.entries.filter { it.key != currentDbName }
        entriesToClose.forEach { (name, db) ->
            if (db.isOpen) db.close()
            databaseCache.remove(name)
        }
    }

    /**
     * 删除所有非当前活跃数据库的文件（含 guest.db，除非当前正在使用）。
     *
     * @return 被删除的数据库文件数量
     */
    fun deleteInactiveDatabases(): Int {
        val allDbNames = listUserDatabases().toMutableList()
        if (currentDbName != GUEST_DB_NAME) {
            allDbNames.add(GUEST_DB_NAME)
        }
        return allDbNames.count { deleteDatabase(it) }
    }

    /**
     * 删除超过指定天数的非活跃用户数据库文件。
     */
    fun deleteExpiredDatabases(maxAgeDays: Int = DEFAULT_EXPIRY_DAYS): Int {
        val dbDir = appContext.getDatabasePath(GUEST_DB_NAME).parentFile ?: return 0
        val cutoffMillis = System.currentTimeMillis() - maxAgeDays * 24 * 60 * 60 * 1000L
        val expiredFiles = dbDir.listFiles()
            ?.filter {
                it.name.startsWith(USER_DB_PREFIX) &&
                    it.name.endsWith(DB_SUFFIX) &&
                    it.name != currentDbName &&
                    it.lastModified() < cutoffMillis
            }
            ?: return 0
        return expiredFiles.count { file ->
            val dbName = file.name
            deleteDatabase(dbName)
        }
    }

    /**
     * 检查访客数据库是否有业务数据。
     */
    suspend fun hasGuestData(): Boolean {
        val guestDb = getOrCreateDatabase(GUEST_DB_NAME)
        return guestDb.excerptDao().countExcerpts() > 0 ||
            guestDb.nodeDao().countNodes() > 0 ||
            guestDb.tagDao().countTags() > 0 ||
            guestDb.excerptTagDao().countExcerptTagsByUserId(LOCAL_USER_ID) > 0
    }

    /**
     * 清空访客数据库中的所有业务数据。
     */
    suspend fun clearGuestData() {
        val guestDb = getOrCreateDatabase(GUEST_DB_NAME)
        guestDb.excerptTagDao().deleteAllExcerptTags()
        guestDb.excerptDao().deleteAllExcerpts()
        guestDb.tagDao().deleteAllTags()
        guestDb.nodeDao().deleteAllNodes()
    }

    private fun getOrCreateDatabase(dbName: String): WorkspaceDatabase {
        return databaseCache.getOrPut(dbName) { openDatabase(dbName) }
    }

    private fun openDatabase(dbName: String): WorkspaceDatabase {
        return Room.databaseBuilder(
            appContext,
            WorkspaceDatabase::class.java,
            dbName,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }

    companion object {
        private const val TAG = "WorkspaceDbManager"
        const val GUEST_DB_NAME = "guest.db"
        const val USER_DB_PREFIX = "local_data_"
        const val DB_SUFFIX = ".db"
        const val DEFAULT_EXPIRY_DAYS = 30

        fun userDbName(userId: String): String = "${USER_DB_PREFIX}${userId}${DB_SUFFIX}"
    }
}
