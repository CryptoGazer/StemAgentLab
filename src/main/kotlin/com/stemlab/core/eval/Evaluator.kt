package com.stemlab.core.eval

import com.stemlab.core.model.EvalResult
import com.stemlab.core.model.EvalTask
import com.stemlab.llm.LlmResponse

interface Evaluator {
    suspend fun evaluate(
        agentId: String,
        tasks: List<EvalTask>,
        runTask: suspend (EvalTask) -> LlmResponse
    ): List<EvalResult>
}
