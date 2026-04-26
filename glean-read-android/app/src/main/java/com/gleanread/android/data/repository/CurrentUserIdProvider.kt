package com.gleanread.android.data.repository

import com.gleanread.android.data.model.LOCAL_USER_ID

fun interface CurrentUserIdProvider {
    fun currentUserId(): String
}

object LocalCurrentUserIdProvider : CurrentUserIdProvider {
    override fun currentUserId(): String = LOCAL_USER_ID
}
