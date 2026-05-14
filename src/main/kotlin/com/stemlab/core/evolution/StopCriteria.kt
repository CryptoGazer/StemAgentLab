package com.stemlab.core.evolution

import com.stemlab.core.model.Budget
import com.stemlab.core.model.CandidateAgent

class StopCriteria(private val budget: Budget = Budget()) {

    fun shouldStop(
        round: Int,
        candidatesEvaluated: Int,
        tokensUsed: Long,
        costIncurred: Double
    ): Boolean = when {
        round >= budget.maxRounds -> true
        candidatesEvaluated >= budget.maxCandidates -> true
        tokensUsed >= budget.maxTokens -> true
        costIncurred >= budget.maxCost -> true
        else -> false
    }

    fun isAcceptable(candidate: CandidateAgent, baselineScore: Double): Boolean =
        candidate.score > baselineScore
}
