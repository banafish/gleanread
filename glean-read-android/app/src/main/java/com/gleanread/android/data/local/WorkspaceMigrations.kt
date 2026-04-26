package com.gleanread.android.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateKnowledgeTreeNode(db)
        migrateTags(db)
        migrateExcerpts(db)
        migrateExcerptTags(db)
    }

    private fun migrateKnowledgeTreeNode(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE knowledge_tree_node_new (
                id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                parent_id TEXT,
                node_title TEXT NOT NULL,
                outline_markdown TEXT,
                create_time INTEGER NOT NULL,
                update_time INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL,
                device_id TEXT,
                sync_status TEXT NOT NULL,
                last_sync_time INTEGER,
                sync_error TEXT,
                retry_count INTEGER NOT NULL DEFAULT 0,
                local_dirty_time INTEGER,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO knowledge_tree_node_new (
                id, user_id, parent_id, node_title, outline_markdown, create_time, update_time,
                is_deleted, device_id, sync_status, last_sync_time, sync_error, retry_count, local_dirty_time
            )
            SELECT
                id, user_id, parent_id, node_title, outline_markdown, create_time, update_time,
                is_deleted, NULL, ${syncStatusExpression()}, NULL, NULL, 0, NULL
            FROM knowledge_tree_node
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE knowledge_tree_node")
        db.execSQL("ALTER TABLE knowledge_tree_node_new RENAME TO knowledge_tree_node")
        db.execSQL("CREATE INDEX index_knowledge_tree_node_user_id ON knowledge_tree_node(user_id)")
        db.execSQL("CREATE INDEX index_knowledge_tree_node_parent_id ON knowledge_tree_node(parent_id)")
    }

    private fun migrateTags(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tags_new (
                id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                tag_name TEXT NOT NULL,
                color_icon TEXT,
                heat_weight INTEGER NOT NULL,
                create_time INTEGER NOT NULL,
                update_time INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL,
                device_id TEXT,
                sync_status TEXT NOT NULL,
                last_sync_time INTEGER,
                sync_error TEXT,
                retry_count INTEGER NOT NULL DEFAULT 0,
                local_dirty_time INTEGER,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO tags_new (
                id, user_id, tag_name, color_icon, heat_weight, create_time, update_time,
                is_deleted, device_id, sync_status, last_sync_time, sync_error, retry_count, local_dirty_time
            )
            SELECT
                id, user_id, tag_name, color_icon, heat_weight, create_time, update_time,
                is_deleted, NULL, ${syncStatusExpression()}, NULL, NULL, 0, NULL
            FROM tags
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE tags")
        db.execSQL("ALTER TABLE tags_new RENAME TO tags")
        db.execSQL("CREATE UNIQUE INDEX index_tags_user_id_tag_name ON tags(user_id, tag_name)")
        db.execSQL("CREATE INDEX index_tags_heat_weight ON tags(heat_weight)")
    }

    private fun migrateExcerpts(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE excerpts_new (
                id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                content TEXT NOT NULL,
                url TEXT,
                source_title TEXT,
                user_thought TEXT,
                tree_node_id TEXT,
                create_time INTEGER NOT NULL,
                update_time INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL,
                device_id TEXT,
                sync_status TEXT NOT NULL,
                last_sync_time INTEGER,
                sync_error TEXT,
                retry_count INTEGER NOT NULL DEFAULT 0,
                local_dirty_time INTEGER,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO excerpts_new (
                id, user_id, content, url, source_title, user_thought, tree_node_id, create_time, update_time,
                is_deleted, device_id, sync_status, last_sync_time, sync_error, retry_count, local_dirty_time
            )
            SELECT
                id, user_id, content, url, source_title, user_thought, tree_node_id, create_time, update_time,
                is_deleted, NULL, ${syncStatusExpression()}, NULL, NULL, 0, NULL
            FROM excerpts
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE excerpts")
        db.execSQL("ALTER TABLE excerpts_new RENAME TO excerpts")
        db.execSQL("CREATE INDEX index_excerpts_user_id ON excerpts(user_id)")
        db.execSQL("CREATE INDEX index_excerpts_tree_node_id ON excerpts(tree_node_id)")
        db.execSQL("CREATE INDEX index_excerpts_create_time ON excerpts(create_time)")
    }

    private fun migrateExcerptTags(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE excerpt_tags_new (
                id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                excerpt_id TEXT NOT NULL,
                tag_id TEXT NOT NULL,
                create_time INTEGER NOT NULL,
                update_time INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL,
                device_id TEXT,
                sync_status TEXT NOT NULL,
                last_sync_time INTEGER,
                sync_error TEXT,
                retry_count INTEGER NOT NULL DEFAULT 0,
                local_dirty_time INTEGER,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO excerpt_tags_new (
                id, user_id, excerpt_id, tag_id, create_time, update_time, is_deleted,
                device_id, sync_status, last_sync_time, sync_error, retry_count, local_dirty_time
            )
            SELECT
                id, user_id, excerpt_id, tag_id, create_time, update_time, is_deleted,
                NULL, ${syncStatusExpression()}, NULL, NULL, 0, NULL
            FROM excerpt_tags
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE excerpt_tags")
        db.execSQL("ALTER TABLE excerpt_tags_new RENAME TO excerpt_tags")
        db.execSQL("CREATE UNIQUE INDEX index_excerpt_tags_excerpt_id_tag_id ON excerpt_tags(excerpt_id, tag_id)")
        db.execSQL("CREATE INDEX index_excerpt_tags_excerpt_id ON excerpt_tags(excerpt_id)")
        db.execSQL("CREATE INDEX index_excerpt_tags_tag_id ON excerpt_tags(tag_id)")
    }

    private fun syncStatusExpression(): String {
        return """
            CASE CAST(sync_status AS TEXT)
                WHEN '0' THEN 'SYNCED'
                WHEN '1' THEN 'PENDING_CREATE'
                WHEN '2' THEN 'PENDING_UPDATE'
                WHEN '3' THEN 'PENDING_DELETE'
                WHEN '4' THEN 'SYNCING'
                WHEN '5' THEN 'FAILED'
                WHEN '6' THEN 'CONFLICT'
                ELSE CAST(sync_status AS TEXT)
            END
        """.trimIndent()
    }
}
