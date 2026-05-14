package com.stemlab.core.tasks

import com.stemlab.core.model.EvalTask

interface TaskGenerator {
    suspend fun generate(domain: String, count: Int = 5): List<EvalTask>
}
