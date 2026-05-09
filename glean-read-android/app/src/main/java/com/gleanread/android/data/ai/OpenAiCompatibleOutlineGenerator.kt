package com.gleanread.android.data.ai

import com.gleanread.android.data.model.OutlineDraft
import com.gleanread.android.data.repository.OutlineGenerator
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OpenAiCompatibleOutlineGenerator(
    private val httpClient: HttpClient,
    private val configProvider: suspend () -> AiConfig,
) : OutlineGenerator {
    override suspend fun generate(excerpts: List<String>): OutlineDraft {
        if (excerpts.isEmpty()) {
            return OutlineDraft(title = DEFAULT_TITLE, markdown = "")
        }
        val markdown = requestOutline(
            config = configProvider(),
            messages = buildOutlineMessages(excerpts),
            maxTokens = OUTLINE_MAX_TOKENS,
        )
        return OutlineDraft(
            title = markdown.toOutlineTitle(),
            markdown = markdown,
        )
    }

    suspend fun testConnection(): Result<Unit> {
        return testConnection(configProvider())
    }

    suspend fun testConnection(config: AiConfig): Result<Unit> {
        return runCatching {
            requestOutline(
                config = config,
                messages = listOf(
                    OpenAiMessage(
                        role = ROLE_USER,
                        content = "请只回复 OK，用于测试连接。",
                    ),
                ),
                maxTokens = TEST_MAX_TOKENS,
            )
        }.map { Unit }
    }

    private suspend fun requestOutline(
        config: AiConfig,
        messages: List<OpenAiMessage>,
        maxTokens: Int,
    ): String {
        validateConfig(config)
        val response = try {
            httpClient.post(config.chatCompletionsUrl()) {
                contentType(ContentType.Application.Json)
                bearerAuth(config.token)
                setBody(
                    AiJson.encodeToString(
                        OpenAiChatRequest(
                            model = config.model.trim(),
                            messages = messages,
                            temperature = DEFAULT_TEMPERATURE,
                            maxTokens = maxTokens,
                        ),
                    ),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw AiOutlineException("AI 接口连接失败：${error.message.orEmpty()}")
        }

        val responseBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw AiOutlineException(responseBody.toOpenAiErrorMessage(response.status))
        }

        val content = runCatching {
            AiJson.decodeFromString<OpenAiChatResponse>(responseBody)
                .choices
                .firstOrNull()
                ?.message
                ?.content
                ?.trim()
        }.getOrNull()

        if (content.isNullOrBlank()) {
            throw AiOutlineException("AI 接口未返回可用的大纲内容")
        }
        return content
    }

    private fun validateConfig(config: AiConfig) {
        if (config.normalizedBaseUrl().isBlank() || config.token.isBlank() || config.model.isBlank()) {
            throw AiOutlineException("请先在设置中配置 AI 接口")
        }
    }

    private fun buildOutlineMessages(excerpts: List<String>): List<OpenAiMessage> {
        val excerptText = excerpts
            .take(MAX_EXCERPT_COUNT)
            .mapIndexed { index, text -> "${index + 1}. ${text.trim()}" }
            .joinToString(separator = "\n")

        return listOf(
            OpenAiMessage(
                role = ROLE_SYSTEM,
                content = "你是一个帮助用户整理阅读摘录的知识管理助手。请用中文输出 Markdown 大纲，内容要可直接编辑并挂载到知识树节点。",
            ),
            OpenAiMessage(
                role = ROLE_USER,
                content = """
                    请基于以下摘录生成一份结构清晰的知识大纲：

                    $excerptText

                    要求：
                    - 使用 Markdown 标题和项目符号。
                    - 先给出一个简短主题标题。
                    - 提炼核心观点、关系和可行动的后续整理建议。
                    - 不要编造摘录中没有的事实。
                """.trimIndent(),
            ),
        )
    }

    private fun String.toOutlineTitle(): String {
        return lineSequence()
            .map { it.trim().trimStart('#').trim() }
            .firstOrNull { it.isNotBlank() }
            ?.take(TITLE_MAX_LENGTH)
            ?: DEFAULT_TITLE
    }

    private fun String.toOpenAiErrorMessage(status: HttpStatusCode): String {
        val message = runCatching {
            AiJson.decodeFromString<OpenAiChatResponse>(this).error?.message
        }.getOrNull()
        return if (message.isNullOrBlank()) {
            "AI 接口请求失败：HTTP ${status.value}"
        } else {
            "AI 接口请求失败：$message"
        }
    }

    companion object {
        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"
        private const val DEFAULT_TITLE = "AI 总结"
        private const val DEFAULT_TEMPERATURE = 0.2
        private const val MAX_EXCERPT_COUNT = 20
        private const val OUTLINE_MAX_TOKENS = 1200
        private const val TEST_MAX_TOKENS = 16
        private const val TITLE_MAX_LENGTH = 30
    }
}

class AiOutlineException(message: String) : RuntimeException(message)

@Serializable
private data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double,
    @SerialName("max_tokens")
    val maxTokens: Int,
)

@Serializable
private data class OpenAiMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val error: OpenAiError? = null,
)

@Serializable
private data class OpenAiChoice(
    val message: OpenAiResponseMessage,
)

@Serializable
private data class OpenAiResponseMessage(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
private data class OpenAiError(
    val message: String? = null,
)

private val AiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
