package com.gleanread.android.core.model

import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TagProjectionFactoryTest {
    private val factory = TagProjectionFactory()

    @Test
    fun `hierarchical tags only keep the first two levels`() {
        val projection = factory.create(
            listOf(
                tag(id = "tag-1", tagName = "技术/前端/React", heatWeight = 6),
                tag(id = "tag-2", tagName = "技术/后端", heatWeight = 4),
                tag(id = "tag-3", tagName = "效率工具", heatWeight = 2),
            )
        )

        val techGroup = projection.tagGroups.first { it.folder == "技术" }
        val uncategorizedGroup = projection.tagGroups.first { it.folder == "Uncategorized" }

        assertEquals(listOf("前端", "后端"), techGroup.items.map { it.displayName })
        assertEquals(listOf("技术/前端/React", "技术/后端"), techGroup.items.map { it.fullName })
        assertEquals(listOf("效率工具"), uncategorizedGroup.items.map { it.displayName })
        assertEquals(
            listOf("#前端", "#后端", "#效率工具"),
            projection.suggestedTags.map { it.label },
        )
    }

    private fun tag(
        id: String,
        tagName: String,
        heatWeight: Int,
    ) = TagEntity(
        id = id,
        userId = LOCAL_USER_ID,
        tagName = tagName,
        colorIcon = null,
        heatWeight = heatWeight,
        createTime = 1L,
        updateTime = 1L,
        syncStatus = SyncStatus.SYNCED,
    )
}
