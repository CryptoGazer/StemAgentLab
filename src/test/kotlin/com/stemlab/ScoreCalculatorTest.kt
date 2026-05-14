package com.stemlab

import com.stemlab.core.eval.ScoreCalculator
import com.stemlab.core.model.EvalResult
import com.stemlab.core.model.EvalTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoreCalculatorTest {

    private val task = EvalTask(
        id = "t1",
        description = "Test task",
        code = "def f(): pass",
        expectedIssueKeywords = listOf("ZeroDivisionError", "division by zero", "check b != 0", "guard clause")
    )

    @Test
    fun `perfect response scores 1_0`() {
        val response = "ZeroDivisionError division by zero check b != 0 guard clause"
        assertEquals(1.0, ScoreCalculator.score(task, response), absoluteTolerance = 0.001)
    }

    @Test
    fun `empty response scores 0_0`() {
        assertEquals(0.0, ScoreCalculator.score(task, ""), absoluteTolerance = 0.001)
    }

    @Test
    fun `partial match scores proportionally`() {
        val response = "ZeroDivisionError division by zero"
        val score = ScoreCalculator.score(task, response)
        assertEquals(0.5, score, absoluteTolerance = 0.01)
    }

    @Test
    fun `matching is case-insensitive`() {
        val response = "zerodivisionerror DIVISION BY ZERO"
        val score = ScoreCalculator.score(task, response)
        assertTrue(score > 0.0)
    }

    @Test
    fun `aggregate score averages correctly`() {
        val results = listOf(
            EvalResult("t1", "a", "", emptyList(), 0.5, 100, 0.0002),
            EvalResult("t2", "a", "", emptyList(), 0.8, 100, 0.0002),
            EvalResult("t3", "a", "", emptyList(), 0.3, 100, 0.0002)
        )
        assertEquals(0.533, ScoreCalculator.aggregateScore(results), absoluteTolerance = 0.01)
    }

    @Test
    fun `empty task keyword list gives 0`() {
        val emptyTask = task.copy(expectedIssueKeywords = emptyList())
        assertEquals(0.0, ScoreCalculator.score(emptyTask, "anything"))
    }
}
