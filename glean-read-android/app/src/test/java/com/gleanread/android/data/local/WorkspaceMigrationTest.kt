package com.gleanread.android.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.model.SyncStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun `migration 1 to 2 maps sync status integers to strings and keeps data`() = runBlocking {
        createVersionOneDatabase()

        val database = Room.databaseBuilder(
            context,
            WorkspaceDatabase::class.java,
            TEST_DATABASE,
        )
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val saved = database.excerptDao().findExcerptById("excerpt-1")

        assertEquals("摘录正文", saved?.content)
        assertEquals(SyncStatus.PENDING_UPDATE, saved?.syncStatus)
        assertNotNull(saved)
        database.close()
    }

    private fun createVersionOneDatabase() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DATABASE)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createVersionOneSchema(db)
                            db.execSQL(
                                """
                                INSERT INTO excerpts (
                                    id, user_id, content, url, source_title, user_thought,
                                    tree_node_id, create_time, update_time, is_deleted, sync_status
                                ) VALUES (
                                    'excerpt-1', 'local-user', '摘录正文', NULL, NULL, NULL,
                                    NULL, 100, 200, 0, 2
                                )
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        helper.writableDatabase.close()
        helper.close()
    }

    private fun createVersionOneSchema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE knowledge_tree_node (
                id TEXT NOT NULL PRIMARY KEY,
                user_id TEXT NOT NULL,
                parent_id TEXT,
                node_title TEXT NOT NULL,
                outline_markdown TEXT,
                create_time INTEGER NOT NULL,
                update_time INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL,
                sync_status INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE tags (
                id TEXT NOT NULL PRIMARY KEY,
                user_id TEXT NOT NULL,
                tag_name TEXT NOT NULL,
                color_icon TEXT,
                heat_weight INTEGER NOT NULL,
                create_time INTEGER NOT NULL,
                update_time INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL,
                sync_status INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE excerpts (
                id TEXT NOT NULL PRIMARY KEY,
                user_id TEXT NOT NULL,
                content TEXT NOT NULL,
                url TEXT,
                source_title TEXT,
                user_thought TEXT,
                tree_node_id TEXT,
                create_time INTEGER NOT NULL,
                update_time INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL,
                sync_status INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE excerpt_tags (
                id TEXT NOT NULL PRIMARY KEY,
                user_id TEXT NOT NULL,
                excerpt_id TEXT NOT NULL,
                tag_id TEXT NOT NULL,
                create_time INTEGER NOT NULL,
                update_time INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL,
                sync_status INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    private companion object {
        const val TEST_DATABASE = "workspace-migration-test.db"
    }
}
