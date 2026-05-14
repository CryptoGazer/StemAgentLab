package com.stemlab.core.evolution

import com.stemlab.core.model.CandidateAgent
import com.stemlab.core.model.CandidateStatus

class VersionManager {

    fun selectBest(
        candidates: List<CandidateAgent>,
        baselineScore: Double
    ): Pair<CandidateAgent?, List<CandidateAgent>> {
        val eligible = candidates.filter { it.score > baselineScore }

        val selected = eligible.maxByOrNull { it.scoreCostRatio }

        val annotated = candidates.map { candidate ->
            candidate.copy(
                status = when {
                    candidate.id == selected?.id -> CandidateStatus.SELECTED
                    else -> CandidateStatus.REJECTED
                }
            )
        }
        return selected to annotated
    }
}
