package com.stemlab.core.eval

import com.stemlab.core.model.EvalResult
import com.stemlab.core.model.EvalTask

interface Evaluator {
    suspend fun evaluate(
        agentId: String,
        tasks: List<EvalTask>,
        runTask: suspend (EvalTask) -> Pair<String, Int>
    ): List<EvalResult>
}
