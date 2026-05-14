package com.stemlab.core.eval

import com.stemlab.core.model.EvalResult
import com.stemlab.core.model.EvalTask
import com.stemlab.llm.LlmResponse

class PythonQaEvaluator : Evaluator {

    override suspend fun evaluate(
        agentId: String,
        tasks: List<EvalTask>,
        runTask: suspend (EvalTask) -> LlmResponse
    ): List<EvalResult> = tasks.map { task ->
        val response = runTask(task)
        val score = ScoreCalculator.score(task, response.text)
        val matched = ScoreCalculator.matchedKeywords(task, response.text)

        EvalResult(
            taskId = task.id,
            agentId = agentId,
            agentResponse = response.text,
            matchedKeywords = matched,
            score = score,
            tokensUsed = response.tokensUsed,
            costEstimate = response.costEstimate
        )
    }
}
