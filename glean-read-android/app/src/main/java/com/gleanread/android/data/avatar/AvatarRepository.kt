package com.gleanread.android.data.avatar

import com.gleanread.android.data.auth.SupabaseSessionStore
import com.gleanread.android.data.remote.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AvatarRepository(
    private val config: SupabaseConfig,
    private val httpClient: HttpClient,
    private val sessionStore: SupabaseSessionStore
) {
    suspend fun uploadAvatar(
        userId: String,
        imageBytes: ByteArray,
        contentType: String = "image/jpeg",
        extension: String = "jpg"
    ): Result<String> =
        withContext(Dispatchers.IO) {
            if (!config.isConfigured) {
                return@withContext Result.failure(Exception("Supabase 尚未配置"))
            }
            val token =
                sessionStore.session.value?.accessToken ?: return@withContext Result.failure(
                    Exception("尚未登录")
                )
 
            try {
                val fileName = "$userId.$extension"
                val url = "${config.normalizedUrl}/storage/v1/object/avatars/$fileName"
 
                val response = httpClient.put(url) {
                    header("apikey", config.anonKey)
                    bearerAuth(token)
                    header("x-upsert", "true")
                    header(HttpHeaders.ContentLength, imageBytes.size.toString())
                    contentType(ContentType.parse(contentType))
                    setBody(imageBytes)
                }
 
                if (response.status.isSuccess()) {
                    // 增加时间戳以绕过缓存
                    val timestamp = System.currentTimeMillis()
                    val publicUrl =
                        "${config.normalizedUrl}/storage/v1/object/public/avatars/$fileName?t=$timestamp"
                    Result.success(publicUrl)
                } else {
                    val errorBody = response.bodyAsText()
                    Result.failure(Exception("上传头像失败: ${response.status} - $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
