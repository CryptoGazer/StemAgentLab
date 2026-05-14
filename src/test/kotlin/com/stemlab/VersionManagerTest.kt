package com.stemlab

import com.stemlab.core.evolution.VersionManager
import com.stemlab.core.model.AgentConfig
import com.stemlab.core.model.CandidateAgent
import com.stemlab.core.model.CandidateStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VersionManagerTest {

    private val manager = VersionManager()

    private fun candidate(id: String, score: Double, cost: Double) = CandidateAgent(
        id = id,
        config = AgentConfig(id = id, name = id, tools = emptyList(), skills = emptyList()),
        score = score,
        estimatedCost = cost
    )

    @Test
    fun `selects candidate with best score-cost ratio above baseline`() {
        val baseline = 0.3
        val candidates = listOf(
            candidate("a", 0.55, 0.005),  // ratio = 110
            candidate("b", 0.85, 0.009),  // ratio = 94.4 — but score is much higher
            candidate("c", 0.72, 0.003)   // ratio = 240 — best ratio
        )
        val (selected, _) = manager.selectBest(candidates, baseline)
        assertEquals("c", selected?.id)
    }

    @Test
    fun `rejects all candidates that score below baseline`() {
        val candidates = listOf(
            candidate("a", 0.1, 0.001),
            candidate("b", 0.2, 0.002)
        )
        val (selected, annotated) = manager.selectBest(candidates, 0.5)
        assertNull(selected)
        annotated.forEach { assertEquals(CandidateStatus.REJECTED, it.status) }
    }

    @Test
    fun `marks selected candidate with SELECTED status`() {
        val candidates = listOf(
            candidate("a", 0.6, 0.004),
            candidate("b", 0.8, 0.008)
        )
        val (_, annotated) = manager.selectBest(candidates, 0.3)
        val selected = annotated.find { it.status == CandidateStatus.SELECTED }
        assertEquals("a", selected?.id) // a has better ratio: 0.6/0.004=150 vs 0.8/0.008=100
    }

    @Test
    fun `handles empty candidate list`() {
        val (selected, annotated) = manager.selectBest(emptyList(), 0.5)
        assertNull(selected)
        assertEquals(0, annotated.size)
    }
}
