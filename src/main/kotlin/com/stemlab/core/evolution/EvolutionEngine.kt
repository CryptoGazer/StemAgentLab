package com.stemlab.core.evolution

import com.stemlab.core.agent.BaselineAgent
import com.stemlab.core.agent.CandidateAgentBuilder
import com.stemlab.core.agent.StemAgent
import com.stemlab.core.eval.PythonQaEvaluator
import com.stemlab.core.eval.ScoreCalculator
import com.stemlab.core.model.*
import com.stemlab.core.registry.ToolRegistry
import com.stemlab.llm.LlmClient
import java.time.Instant
import java.util.UUID

class EvolutionEngine(
    private val llmClient: LlmClient,
    private val toolRegistry: ToolRegistry,
    private val budget: Budget = Budget()
) {
    private val evaluator = PythonQaEvaluator()
    private val stopCriteria = StopCriteria(budget)
    private val versionManager = VersionManager()

    suspend fun run(
        domain: String,
        tasks: List<EvalTask>,
        onLog: (String) -> Unit
    ): EvolutionResult {
        val runId = UUID.randomUUID().toString().take(8)
        var totalTokens = 0L
        var totalCost = 0.0

        onLog("[$runId] Evolution started — domain: $domain")
        onLog("[$runId] Loaded ${tasks.size} evaluation tasks from dataset")

        // 1. Baseline
        onLog("[$runId] Running Baseline Agent on ${tasks.size} tasks...")
        val baselineAgent = BaselineAgent(llmClient)
        val baselineResults = evaluator.evaluate("baseline", tasks) { task ->
            val resp = baselineAgent.runOnTask(task)
            totalTokens += resp.tokensUsed
            totalCost += resp.costEstimate
            resp.text to resp.tokensUsed
        }
        val baselineScore = ScoreCalculator.aggregateScore(baselineResults)
        onLog("[$runId] Baseline score: ${"%.3f".format(baselineScore)} (avg keyword match)")

        // 2. Generate candidates
        onLog("[$runId] StemAgent proposing candidate configurations...")
        val stemAgent = StemAgent(llmClient)
        val candidateConfigs = stemAgent.proposeCandidates(domain, baselineScore)
        val configSource = if (llmClient.isMock) "default (mock mode)" else "LLM-parsed"
        onLog("[$runId] Generated ${candidateConfigs.size} candidates [$configSource]: ${candidateConfigs.joinToString(", ") { it.name }}")

        // 3. Evaluate candidates
        val builder = CandidateAgentBuilder(llmClient)
        val evaluatedCandidates = mutableListOf<CandidateAgent>()

        // round=0 always: this engine runs a single propose→evaluate cycle.
        // maxRounds guards multi-cycle loops in future extensions.
        for ((index, pair) in builder.buildAll(candidateConfigs).withIndex()) {
            val (candidate, specializedAgent) = pair
            val label = candidate.config.name

            if (stopCriteria.shouldStop(0, index, totalTokens, totalCost)) {
                onLog("[$runId] Stop criteria reached — halting evolution")
                break
            }

            onLog("[$runId] Evaluating $label...")
            val results = evaluator.evaluate(candidate.id, tasks) { task ->
                val resp = specializedAgent.runOnTask(task)
                totalTokens += resp.tokensUsed
                totalCost += resp.costEstimate
                resp.text to resp.tokensUsed
            }

            val score = ScoreCalculator.aggregateScore(results)
            val taskTokens = ScoreCalculator.totalTokens(results).toLong()
            val taskCost = ScoreCalculator.totalCost(results)

            val evaluated = candidate.copy(
                score = score,
                estimatedTokens = taskTokens,
                estimatedCost = taskCost,
                status = CandidateStatus.REJECTED
            )
            evaluatedCandidates.add(evaluated)

            val delta = score - baselineScore
            onLog("[$runId] $label score: ${"%.3f".format(score)} (Δ${if (delta >= 0) "+" else ""}${"%.3f".format(delta)})")
        }

        // 4. Select best
        onLog("[$runId] VersionManager selecting best candidate...")
        val (selected, annotated) = versionManager.selectBest(evaluatedCandidates, baselineScore)

        val allCandidates = buildList {
            add(
                CandidateAgent(
                    id = "baseline",
                    config = baselineAgent.config,
                    score = baselineScore,
                    estimatedTokens = ScoreCalculator.totalTokens(baselineResults).toLong(),
                    estimatedCost = ScoreCalculator.totalCost(baselineResults),
                    status = CandidateStatus.REJECTED
                )
            )
            addAll(annotated)
        }

        if (selected != null) {
            val improvement = selected.score - baselineScore
            onLog("[$runId] Selected: ${selected.config.name} (score=${"%.3f".format(selected.score)}, cost=\$${"%.4f".format(selected.estimatedCost)}, ratio=${"%.2f".format(selected.scoreCostRatio)})")
            annotated.filter { it.id != selected.id }.forEach { rejected ->
                onLog("[$runId] Rejected: ${rejected.config.name}")
            }
            onLog("[$runId] SpecializedAgent frozen with ${selected.config.name} configuration")
            onLog("[$runId] Evolution complete — improvement: +${"%.1f".format((improvement / baselineScore) * 100)}%")
        } else {
            onLog("[$runId] No candidate outperformed baseline — keeping baseline")
        }

        return EvolutionResult(
            runId = runId,
            domain = domain,
            baselineScore = baselineScore,
            candidates = allCandidates,
            selectedCandidateId = selected?.id,
            improvement = (selected?.score ?: baselineScore) - baselineScore,
            totalTokensUsed = totalTokens,
            totalCost = totalCost,
            logs = emptyList(),
            timestamp = Instant.now().toString()
        )
    }
}
