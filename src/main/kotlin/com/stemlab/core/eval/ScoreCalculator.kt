package com.stemlab.core.eval

import com.stemlab.core.model.EvalResult
import com.stemlab.core.model.EvalTask

object ScoreCalculator {

    fun score(task: EvalTask, response: String): Double {
        val lowerResponse = response.lowercase()
        val matched = task.expectedIssueKeywords.filter { kw ->
            lowerResponse.contains(kw.lowercase())
        }
        return if (task.expectedIssueKeywords.isEmpty()) 0.0
        else matched.size.toDouble() / task.expectedIssueKeywords.size.toDouble()
    }

    fun matchedKeywords(task: EvalTask, response: String): List<String> {
        val lowerResponse = response.lowercase()
        return task.expectedIssueKeywords.filter { kw ->
            lowerResponse.contains(kw.lowercase())
        }
    }

    fun aggregateScore(results: List<EvalResult>): Double =
        if (results.isEmpty()) 0.0 else results.sumOf { it.score } / results.size

    fun totalTokens(results: List<EvalResult>): Int =
        results.sumOf { it.tokensUsed }

    fun totalCost(results: List<EvalResult>): Double =
        results.sumOf { it.costEstimate }
}
