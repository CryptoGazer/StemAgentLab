package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectSpec(
    val id: String,
    val name: String,
    val domain: String,
    val description: String = "",
    val createdAt: String,
    val updatedAt: String,
    val lastRunId: String? = null
)
