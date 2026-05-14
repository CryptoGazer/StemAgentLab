package com.stemlab

import com.stemlab.core.eval.ScoreCalculator
import com.stemlab.core.evolution.EvolutionEngine
import com.stemlab.core.model.Budget
import com.stemlab.core.model.CandidateStatus
import com.stemlab.core.registry.ToolRegistry
import com.stemlab.llm.MockLlmClient
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class EvolutionEngineTest {

    private val llm = MockLlmClient()
    private val registry = ToolRegistry()
    private val budget = Budget(maxCandidates = 3, maxRounds = 1)
    private val engine = EvolutionEngine(llm, registry, budget)
    private val tasks = com.stemlab.app.DemoScenario.loadTasks()

    @Test
    fun `baseline score is lower than best candidate`() = runTest {
        val logs = mutableListOf<String>()
        val result = engine.run("Python QA", tasks) { logs.add(it) }

        assertTrue(result.baselineScore > 0.0, "Baseline should have a non-zero score")
        val selected = result.selectedCandidate
        assertNotNull(selected, "A candidate should be selected")
        assertTrue(selected.score > result.baselineScore, "Selected candidate must beat baseline")
    }

    @Test
    fun `candidate B is selected as best`() = runTest {
        val logs = mutableListOf<String>()
        val result = engine.run("Python QA", tasks) { logs.add(it) }

        val selected = result.selectedCandidate
        assertNotNull(selected)
        assertEquals("candidate_b", selected.id, "Candidate B should win in mock mode")
    }

    @Test
    fun `all candidates have evaluated scores`() = runTest {
        val result = engine.run("Python QA", tasks) {}
        val nonBaseline = result.candidates.filter { it.id != "baseline" }
        nonBaseline.forEach { candidate ->
            assertTrue(candidate.score >= 0.0, "Every candidate must have a score")
        }
    }

    @Test
    fun `improvement is positive when a candidate beats baseline`() = runTest {
        val result = engine.run("Python QA", tasks) {}
        if (result.selectedCandidateId != null) {
            assertTrue(result.improvement > 0.0, "Improvement should be positive")
        }
    }

    @Test
    fun `logs contain expected milestone entries`() = runTest {
        val logs = mutableListOf<String>()
        engine.run("Python QA", tasks) { logs.add(it) }

        assertTrue(logs.any { it.contains("Baseline score") })
        assertTrue(logs.any { it.contains("Evaluating") })
        assertTrue(logs.any { it.contains("Selected") || it.contains("No candidate") })
    }

    @Test
    fun `mock score ordering Baseline beats A beats B`() = runTest {
        val result = engine.run("Python QA", tasks) {}
        val byId = result.candidates.associateBy { it.id }

        val baselineScore = byId["baseline"]?.score ?: 0.0
        val aScore = byId["candidate_a"]?.score ?: 0.0
        val bScore = byId["candidate_b"]?.score ?: 0.0

        assertTrue(bScore > baselineScore, "B must beat baseline")
        assertTrue(aScore > baselineScore, "A must beat baseline")
        assertTrue(bScore > aScore, "B must beat A")
    }
}
