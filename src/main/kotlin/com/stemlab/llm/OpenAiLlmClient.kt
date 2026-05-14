package com.stemlab.llm

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OpenAiLlmClient(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    private val maxRetries: Int = 3
) : LlmClient {

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun complete(prompt: String): LlmResponse {
        val request = ChatRequest(
            model = model,
            messages = listOf(
                Message(
                    role = "system",
                    content = "You are an expert code quality analyst and software engineering agent. " +
                        "Analyze code carefully and identify bugs, anti-patterns, and quality issues. " +
                        "When asked to return JSON, return only valid JSON with no surrounding text or markdown."
                ),
                Message(role = "user", content = prompt)
            ),
            maxTokens = 800
        )

        val response: ChatResponse = postWithRetry(request).body()

        val text = response.choices.firstOrNull()?.message?.content ?: ""
        val usage = response.usage
        val cost = (usage.promptTokens * 0.00000015) + (usage.completionTokens * 0.0000006)

        return LlmResponse(
            text = text,
            tokensUsed = usage.totalTokens,
            costEstimate = cost
        )
    }

    private suspend fun postWithRetry(request: ChatRequest): HttpResponse {
        var attempt = 0
        while (true) {
            val response = http.post("https://api.openai.com/v1/chat/completions") {
                bearerAuth(apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) return response

            val shouldRetry = response.status == HttpStatusCode.TooManyRequests ||
                response.status.value in listOf(500, 502, 503, 504)
            if (!shouldRetry || attempt >= maxRetries) {
                val body = runCatching { response.bodyAsText().take(400) }.getOrDefault("")
                error("OpenAI request failed: HTTP ${response.status.value} ${response.status.description}. $body")
            }

            delay(retryDelayMillis(response, attempt))
            attempt++
        }
    }

    private fun retryDelayMillis(response: HttpResponse, attempt: Int): Long {
        val retryAfterSeconds = response.headers["Retry-After"]?.toLongOrNull()
        if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            return retryAfterSeconds * 1000L
        }
        return 1000L * (attempt + 1)
    }

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        @SerialName("max_tokens") val maxTokens: Int
    )

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(
        val choices: List<Choice>,
        val usage: Usage
    )

    @Serializable
    private data class Choice(val message: Message)

    @Serializable
    private data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int,
        @SerialName("completion_tokens") val completionTokens: Int,
        @SerialName("total_tokens") val totalTokens: Int
    )
}
