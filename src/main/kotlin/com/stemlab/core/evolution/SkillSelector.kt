package com.stemlab.core.evolution

class SkillSelector {

    fun selectForBaseline(): List<String> = listOf("direct_reasoning")

    fun selectForCandidate(tier: Int): List<String> = when (tier) {
        1 -> listOf("direct_reasoning", "edge_case_reasoning")
        2 -> listOf("direct_reasoning", "edge_case_reasoning", "test_driven_analysis", "root_cause_analysis")
        3 -> listOf("direct_reasoning", "edge_case_reasoning", "test_driven_analysis", "root_cause_analysis", "patch_suggestion")
        else -> listOf("direct_reasoning")
    }
}
