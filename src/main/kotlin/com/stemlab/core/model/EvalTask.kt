package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class EvalTask(
    val id: String,
    val description: String,
    val code: String,
    val expectedIssueKeywords: List<String>
)
