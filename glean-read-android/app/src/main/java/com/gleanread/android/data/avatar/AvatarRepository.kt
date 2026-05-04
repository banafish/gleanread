package com.gleanread.android.data.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.gleanread.android.data.auth.SupabaseSessionStore
import com.gleanread.android.data.remote.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AvatarRepository(
    private val config: SupabaseConfig,
    private val httpClient: HttpClient,
    private val sessionStore: SupabaseSessionStore
) {
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        if (!config.isConfigured) {
            return@withContext Result.failure(Exception("Supabase 尚未配置"))
        }
        val token = sessionStore.session.value?.accessToken
            ?: return@withContext Result.failure(Exception("尚未登录"))

        try {
            val fileName = "$userId.jpg"
            val url = "${config.normalizedUrl}/storage/v1/object/avatars/$fileName"

            val response = httpClient.post(url) {
                header("apikey", config.anonKey)
                bearerAuth(token)
                header("x-upsert", "true")
                contentType(ContentType.Image.JPEG)
                setBody(imageBytes)
            }

            if (response.status.isSuccess()) {
                val publicUrl = "${config.normalizedUrl}/storage/v1/object/public/avatars/$fileName"
                Result.success(publicUrl)
            } else {
                Result.failure(Exception("上传头像失败: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
