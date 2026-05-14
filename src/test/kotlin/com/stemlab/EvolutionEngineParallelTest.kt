package com.stemlab

import com.stemlab.core.evolution.EvolutionEngine
import com.stemlab.core.model.Budget
import com.stemlab.core.model.EvalTask
import com.stemlab.core.registry.ToolRegistry
import com.stemlab.llm.LlmClient
import com.stemlab.llm.LlmResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvolutionEngineParallelTest {

    private val task = EvalTask(
        id = "task_001",
        description = "Find the bug",
        code = "fun sample() = 1",
        expectedIssueKeywords = listOf("bug")
    )

    private class ParallelTrackingLlmClient : LlmClient {
        private val activeCandidates = AtomicInteger(0)
        val maxConcurrentCandidates = AtomicInteger(0)

        override suspend fun complete(prompt: String): LlmResponse = when {
            prompt.contains("PROPOSE_CANDIDATES") -> LlmResponse(candidateJson(), 7, 0.07)
            prompt.contains("TOOLS: (none)") -> LlmResponse("baseline review", 5, 0.05)
            isCandidatePrompt(prompt) -> {
                val active = activeCandidates.incrementAndGet()
                maxConcurrentCandidates.updateAndGet { current -> maxOf(current, active) }
                delay(100)
                activeCandidates.decrementAndGet()
                candidateResponse(prompt)
            }
            else -> LlmResponse("unknown", 1, 0.01)
        }

        private fun isCandidatePrompt(prompt: String): Boolean =
            prompt.contains("tool_a") || prompt.contains("tool_b") || prompt.contains("tool_c")

        private fun candidateResponse(prompt: String): LlmResponse = when {
            prompt.contains("tool_a") -> LlmResponse("bug found by A", 10, 0.10)
            prompt.contains("tool_b") -> LlmResponse("bug found by B", 20, 0.20)
            prompt.contains("tool_c") -> LlmResponse("bug found by C", 30, 0.30)
            else -> LlmResponse("bug", 1, 0.01)
        }

        private fun candidateJson(): String = """
            {
              "candidates": [
                {"id":"candidate_a","name":"Candidate A","description":"A","tools":["tool_a"],"skills":["direct_reasoning"],"promptStrategy":"tool_augmented","maxTokens":800},
                {"id":"candidate_b","name":"Candidate B","description":"B","tools":["tool_b"],"skills":["direct_reasoning"],"promptStrategy":"tool_augmented","maxTokens":1200},
                {"id":"candidate_c","name":"Candidate C","description":"C","tools":["tool_c"],"skills":["direct_reasoning"],"promptStrategy":"tool_augmented","maxTokens":1500}
              ]
            }
        """.trimIndent()
    }

    @Test
    fun `candidate evaluation respects parallelism limit`() = runTest {
        val llm = ParallelTrackingLlmClient()
        val engine = EvolutionEngine(
            llmClient = llm,
            toolRegistry = ToolRegistry(),
            budget = Budget(maxParallelCandidates = 2)
        )

        engine.run("Parallel QA", listOf(task)) {}

        assertEquals(2, llm.maxConcurrentCandidates.get())
    }

    @Test
    fun `candidate order and usage totals stay stable after parallel evaluation`() = runTest {
        val llm = ParallelTrackingLlmClient()
        val engine = EvolutionEngine(
            llmClient = llm,
            toolRegistry = ToolRegistry(),
            budget = Budget(maxParallelCandidates = 3)
        )

        val result = engine.run("Parallel QA", listOf(task)) {}

        assertEquals(listOf("baseline", "candidate_a", "candidate_b", "candidate_c"), result.candidates.map { it.id })
        assertEquals(72, result.totalTokensUsed)
        assertEquals(0.72, result.totalCost, absoluteTolerance = 0.0001)
        assertTrue(result.candidates.drop(1).all { it.score == 1.0 })
    }
}
