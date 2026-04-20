package com.gleanread.android.data.repository

import java.util.UUID

object EntityIdGenerator {
    fun newDraftExcerptId(): String = UUID.randomUUID().toString()
    fun newNodeId(): String = UUID.randomUUID().toString()
    fun newRelationId(): String = UUID.randomUUID().toString()
    fun newTagId(): String = UUID.randomUUID().toString()
}
