package com.stemlab.core.registry

import com.stemlab.core.model.SkillSpec
import kotlinx.serialization.json.Json

class SkillRegistry {

    private val skills: Map<String, SkillSpec> by lazy { loadSkills() }

    fun all(): List<SkillSpec> = skills.values.toList()

    fun find(id: String): SkillSpec? = skills[id]

    fun resolve(ids: List<String>): List<SkillSpec> = ids.map {
        skills[it] ?: com.stemlab.core.model.SkillSpec(
            id = it,
            name = it.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
            description = "Generic skill: $it"
        )
    }

    private fun loadSkills(): Map<String, SkillSpec> {
        val json = Json { ignoreUnknownKeys = true }
        val resource = SkillRegistry::class.java.getResourceAsStream("/registries/skills.json")
            ?: error("skills.json not found in resources")
        val list = json.decodeFromString<List<SkillSpec>>(resource.bufferedReader().readText())
        return list.associateBy { it.id }
    }
}
