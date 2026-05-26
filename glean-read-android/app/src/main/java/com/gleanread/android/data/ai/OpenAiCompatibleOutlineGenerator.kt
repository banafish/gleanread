package com.gleanread.android.data.ai

import com.gleanread.android.data.model.OutlineDraft
import com.gleanread.android.data.repository.OutlineGenerator
import com.gleanread.android.data.repository.AiExcerptInput
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
    override suspend fun generate(excerpts: List<AiExcerptInput>): OutlineDraft {
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

    private fun buildOutlineMessages(excerpts: List<AiExcerptInput>): List<OpenAiMessage> {
        val excerptText = excerpts
            .take(MAX_EXCERPT_COUNT)
            .mapIndexed { index, e ->
                buildString {
                    append("【摘录 ${index + 1}】\n原文：${e.content.trim()}")
                    if (!e.userThought.isNullOrBlank()) {
                        append("\n思考：${e.userThought.trim()}")
                    }
                    if (!e.sourceTitle.isNullOrBlank() || !e.url.isNullOrBlank()) {
                        val source = listOfNotNull(e.sourceTitle?.trim(), e.url?.trim())
                            .filter { it.isNotBlank() }
                            .joinToString(" - ")
                        if (source.isNotBlank()) {
                            append("\n来源：$source")
                        }
                    }
                }
            }
            .joinToString(separator = "\n\n")

        return listOf(
            OpenAiMessage(
                role = ROLE_SYSTEM,
                content = "你是一个极其专业且敏锐的知识提取与大纲整理专家，担任用户的“第二大脑”知识管理助手。请用中文输出 Markdown 大纲，内容必须结构清晰、见解深刻、排版优雅，可直接编辑并挂载到知识树节点。",
            ),
            OpenAiMessage(
                role = ROLE_USER,
                content = """
                    请基于以下阅读摘录（部分摘录附带了用户的【思考】和【来源】）生成一份结构清晰的知识大纲：

                    [摘录列表]
                    $excerptText
                    [/摘录列表]

                    【生成规则与要求】：
                    1. **结构化层次**：使用标准的 Markdown 标题（#、##、###）和项目符号（-）来展现清晰的逻辑大纲。
                    2. **大纲结构框架**：
                       - **# 主题标题**：提炼一个能概括这批摘录核心本质的简明标题。
                       - **## 核心概念与背景**：简要阐述这批摘录所涉及的核心概念、定义或基本背景。
                       - **## 关键观点与深度提炼**：将零散观点进行分类合并，归纳为 2-3 个清晰的主题论点。在每个论点下，提炼核心见解，并结合摘录原文中的具体细节或论据进行结构化展开。**必须充分结合并呼应用户的【思考】**，把用户的感悟、痛点和思考火花融入到对应的论点分析中。
                       - **## 知识关联与启发**（可选）：指出摘录之间的递进、因果、对比或潜在冲突等逻辑联系，并对【思考】中提出的疑问进行提炼和解答。
                       - **## 下一步行动建议**：为用户提供 1-2 条切实可行的后续整理、学习或实践的“行动指南”。
                    3. **知识溯源（可选）**：在大纲的具体观点或细节展开处，如适用，可使用简洁的括号或引用，在末尾简要标注其【来源】（如：来源：xxx）。
                    4. **严谨性**：仅基于上述摘录内容与思考进行提炼和推导，严禁编造任何未提及的外部事实。
                    5. **纯净输出**：直接以 Markdown 一级标题（# ）开始输出大纲内容，绝对不要包含任何前言（如“好的，这是为您整理的...”）、问候语、结语或解释性文段。
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
