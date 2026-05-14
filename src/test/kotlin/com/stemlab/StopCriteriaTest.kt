package com.stemlab

import com.stemlab.core.evolution.StopCriteria
import com.stemlab.core.model.Budget
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StopCriteriaTest {

    @Test
    fun `does not stop when all values are within budget`() {
        val criteria = StopCriteria(Budget(maxTokens = 100_000, maxCost = 2.0, maxCandidates = 3, maxRounds = 2))
        assertFalse(criteria.shouldStop(round = 0, candidatesEvaluated = 1, tokensUsed = 1000, costIncurred = 0.01))
    }

    @Test
    fun `stops when round reaches maxRounds`() {
        val criteria = StopCriteria(Budget(maxRounds = 2))
        assertTrue(criteria.shouldStop(round = 2, candidatesEvaluated = 0, tokensUsed = 0, costIncurred = 0.0))
    }

    @Test
    fun `stops when candidates evaluated reaches maxCandidates`() {
        val criteria = StopCriteria(Budget(maxCandidates = 3))
        assertTrue(criteria.shouldStop(round = 0, candidatesEvaluated = 3, tokensUsed = 0, costIncurred = 0.0))
    }

    @Test
    fun `stops when token budget exceeded`() {
        val criteria = StopCriteria(Budget(maxTokens = 1000))
        assertTrue(criteria.shouldStop(round = 0, candidatesEvaluated = 0, tokensUsed = 1000, costIncurred = 0.0))
    }

    @Test
    fun `stops when cost budget exceeded`() {
        val criteria = StopCriteria(Budget(maxCost = 0.50))
        assertTrue(criteria.shouldStop(round = 0, candidatesEvaluated = 0, tokensUsed = 0, costIncurred = 0.50))
    }

    @Test
    fun `does not stop at round zero with one candidate evaluated`() {
        val criteria = StopCriteria(Budget(maxRounds = 2, maxCandidates = 3))
        assertFalse(criteria.shouldStop(round = 0, candidatesEvaluated = 1, tokensUsed = 5000, costIncurred = 0.01))
    }

    @Test
    fun `isAcceptable returns true when candidate beats baseline`() {
        val criteria = StopCriteria()
        val candidate = com.stemlab.core.model.CandidateAgent(
            id = "x",
            config = com.stemlab.core.model.AgentConfig(id = "x", name = "X", tools = emptyList(), skills = emptyList()),
            score = 0.8
        )
        assertTrue(criteria.isAcceptable(candidate, baselineScore = 0.5))
    }

    @Test
    fun `isAcceptable returns false when candidate equals baseline`() {
        val criteria = StopCriteria()
        val candidate = com.stemlab.core.model.CandidateAgent(
            id = "x",
            config = com.stemlab.core.model.AgentConfig(id = "x", name = "X", tools = emptyList(), skills = emptyList()),
            score = 0.5
        )
        assertFalse(criteria.isAcceptable(candidate, baselineScore = 0.5))
    }
}
