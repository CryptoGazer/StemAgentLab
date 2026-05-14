package com.stemlab.storage

import com.stemlab.core.model.EvolutionResult
import java.io.File

class RunHistoryStore(private val runsDir: String = "runs") {

    fun save(result: EvolutionResult) {
        val path = "$runsDir/${result.runId}.json"
        JsonStorage.save(result, path)
    }

    fun loadAll(): List<EvolutionResult> {
        val dir = File(runsDir)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { JsonStorage.load<EvolutionResult>(it.path) }
            ?: emptyList()
    }

    fun latest(): EvolutionResult? = loadAll().maxByOrNull { it.timestamp }
}
