package com.stemlab.app

import com.stemlab.core.model.EvalTask
import kotlinx.serialization.json.Json

object DemoScenario {

    fun loadTasks(): List<EvalTask> {
        val json = Json { ignoreUnknownKeys = true }
        val resource = DemoScenario::class.java.getResourceAsStream("/datasets/python_qa_tasks.json")
            ?: error("python_qa_tasks.json not found in resources")
        return json.decodeFromString(resource.bufferedReader().readText())
    }

    fun domain(): String = "Python QA"
}
