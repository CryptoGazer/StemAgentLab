package com.stemlab.core.eval

import com.stemlab.core.model.EvalResult
import com.stemlab.core.model.EvalTask

class PythonQaEvaluator : Evaluator {

    override suspend fun evaluate(
        agentId: String,
        tasks: List<EvalTask>,
        runTask: suspend (EvalTask) -> Pair<String, Int>
    ): List<EvalResult> = tasks.map { task ->
        val (response, tokensUsed) = runTask(task)
        val score = ScoreCalculator.score(task, response)
        val matched = ScoreCalculator.matchedKeywords(task, response)
        val cost = tokensUsed * 0.000002

        EvalResult(
            taskId = task.id,
            agentId = agentId,
            agentResponse = response,
            matchedKeywords = matched,
            score = score,
            tokensUsed = tokensUsed,
            costEstimate = cost
        )
    }
}
