package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SkillSpec(
    val id: String,
    val name: String,
    val description: String
)
