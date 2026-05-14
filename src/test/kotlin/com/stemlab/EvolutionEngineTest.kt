package com.stemlab

import com.stemlab.core.evolution.EvolutionEngine
import com.stemlab.core.model.Budget
import com.stemlab.core.registry.ToolRegistry
import com.stemlab.llm.OpenAiLlmClient
import com.stemlab.util.DotEnvLoader
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class EvolutionEngineTest {

    private val llm = OpenAiLlmClient(DotEnvLoader.requireApiKey())
    private val registry = ToolRegistry()
    private val budget = Budget(maxCandidates = 3, maxRounds = 1)
    private val engine = EvolutionEngine(llm, registry, budget)
    private val tasks = com.stemlab.app.DemoScenario.loadTasks().take(2)

    @Test
    fun `openai evolution run evaluates baseline and candidates`() = runTest {
        val logs = mutableListOf<String>()
        val result = engine.run("Python QA", tasks) { logs.add(it) }

        assertEquals("Python QA", result.domain)
        assertEquals(4, result.candidates.size, "Baseline plus three OpenAI-proposed candidates should be evaluated")
        assertTrue(result.baselineScore >= 0.0)
        assertTrue(result.totalTokensUsed > 0L)
        assertTrue(result.totalCost > 0.0)
        assertTrue(result.candidates.filter { it.id != "baseline" }.all { it.score >= 0.0 })
        assertTrue(logs.any { it.contains("Baseline score") })
        assertTrue(logs.any { it.contains("OpenAI-parsed") })
        assertTrue(logs.any { it.contains("VersionManager") })
    }
}
