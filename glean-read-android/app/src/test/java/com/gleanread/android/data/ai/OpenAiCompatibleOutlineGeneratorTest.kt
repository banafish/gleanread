package com.gleanread.android.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiCompatibleOutlineGeneratorTest {
    @Test
    fun `chat completions url normalizes domain and v1 suffix`() {
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            buildOpenAiChatCompletionsUrl("api.example.com"),
        )
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            buildOpenAiChatCompletionsUrl("https://api.example.com/"),
        )
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            buildOpenAiChatCompletionsUrl("https://api.example.com/v1"),
        )
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            buildOpenAiChatCompletionsUrl("https://api.example.com/v1/"),
        )
        assertEquals(
            "http://localhost:11434/v1/chat/completions",
            buildOpenAiChatCompletionsUrl("http://localhost:11434/v1"),
        )
    }

    @Test
    fun `generate sends OpenAI compatible request and parses markdown`() = runBlocking {
        var requestedUrl = ""
        var authorization = ""
        var requestBody = ""
        val httpClient = HttpClient(
            MockEngine { request ->
                requestedUrl = request.url.toString()
                authorization = request.headers[HttpHeaders.Authorization].orEmpty()
                requestBody = (request.body as TextContent).text
                respond(
                    content = """{"choices":[{"message":{"role":"assistant","content":"# 阅读主题\n- 核心观点"}}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )
        val generator = generator(httpClient)

        val draft = generator.generate(listOf("第一条摘录", "第二条摘录"))
        val payload = Json.parseToJsonElement(requestBody).jsonObject
        val messages = payload.getValue("messages").jsonArray

        assertEquals("https://api.example.com/v1/chat/completions", requestedUrl)
        assertEquals("Bearer test-token", authorization)
        assertEquals("gpt-test", payload.getValue("model").jsonPrimitive.content)
        assertTrue(messages.toString().contains("第一条摘录"))
        assertTrue(messages.toString().contains("第二条摘录"))
        assertEquals("阅读主题", draft.title)
        assertEquals("# 阅读主题\n- 核心观点", draft.markdown)
        httpClient.close()
    }

    @Test
    fun `generate fails before request when config is missing`() = runBlocking {
        var requestCount = 0
        val httpClient = HttpClient(
            MockEngine {
                requestCount += 1
                respond("")
            },
        )
        val generator = OpenAiCompatibleOutlineGenerator(
            httpClient = httpClient,
            configProvider = { AiConfig() },
        )

        val error = runCatching {
            generator.generate(listOf("摘录"))
        }.exceptionOrNull()

        assertTrue(error is AiOutlineException)
        assertEquals("请先在设置中配置 AI 接口", error?.message)
        assertEquals(0, requestCount)
        httpClient.close()
    }

    @Test
    fun `generate returns readable error when server rejects request`() = runBlocking {
        val httpClient = HttpClient(
            MockEngine {
                respond(
                    content = """{"error":{"message":"invalid api key"}}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )
        val generator = generator(httpClient)

        val error = runCatching {
            generator.generate(listOf("摘录"))
        }.exceptionOrNull()

        assertTrue(error is AiOutlineException)
        assertTrue(error?.message.orEmpty().contains("invalid api key"))
        httpClient.close()
    }

    @Test
    fun `generate fails when response has no choices`() = runBlocking {
        val httpClient = HttpClient(
            MockEngine {
                respond(
                    content = """{"choices":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )
        val generator = generator(httpClient)

        val error = runCatching {
            generator.generate(listOf("摘录"))
        }.exceptionOrNull()

        assertTrue(error is AiOutlineException)
        assertEquals("AI 接口未返回可用的大纲内容", error?.message)
        httpClient.close()
    }

    @Test
    fun `testConnection returns success and failure results`() = runBlocking {
        val successClient = HttpClient(
            MockEngine {
                respond(
                    content = """{"choices":[{"message":{"role":"assistant","content":"OK"}}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )
        val failureClient = HttpClient(
            MockEngine {
                respond(
                    content = """{"error":{"message":"rate limited"}}""",
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )

        assertTrue(generator(successClient).testConnection().isSuccess)
        assertTrue(generator(failureClient).testConnection().isFailure)
        successClient.close()
        failureClient.close()
    }

    private fun generator(httpClient: HttpClient): OpenAiCompatibleOutlineGenerator {
        return OpenAiCompatibleOutlineGenerator(
            httpClient = httpClient,
            configProvider = {
                AiConfig(
                    baseUrl = "https://api.example.com/v1/",
                    token = "test-token",
                    model = "gpt-test",
                )
            },
        )
    }
}
