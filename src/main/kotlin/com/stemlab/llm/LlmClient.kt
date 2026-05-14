package com.stemlab.llm

data class LlmResponse(
    val text: String,
    val tokensUsed: Int,
    val costEstimate: Double
)

interface LlmClient {
    val isMock: Boolean
    suspend fun complete(prompt: String): LlmResponse
}
