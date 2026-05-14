package com.stemlab.report

import com.stemlab.core.model.CandidateStatus
import com.stemlab.core.model.EvolutionResult
import java.io.File
import java.time.Instant

object MarkdownReportExporter {

    fun export(result: EvolutionResult, outputPath: String = "projects/default/reports/report-${result.runId}.md"): String {
        val md = buildReport(result)
        File(outputPath).apply {
            parentFile?.mkdirs()
            writeText(md)
        }
        return outputPath
    }

    fun buildReport(result: EvolutionResult): String = buildString {
        appendLine("# Stem Agent Lab — Evolution Report")
        appendLine()
        appendLine("**Run ID:** `${result.runId}`  ")
        appendLine("**Domain:** ${result.domain}  ")
        appendLine("**Timestamp:** ${result.timestamp}  ")
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("## Summary")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Baseline Score | ${"%.3f".format(result.baselineScore)} |")
        val selected = result.selectedCandidate
        appendLine("| Best Candidate | ${selected?.config?.name ?: "None"} |")
        appendLine("| Best Score | ${"%.3f".format(selected?.score ?: result.baselineScore)} |")
        appendLine("| Improvement | +${"%.1f".format(result.improvementPercent)}% |")
        appendLine("| Total Tokens | ${result.totalTokensUsed} |")
        appendLine("| Estimated Cost | \$${"%.4f".format(result.totalCost)} |")
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("## Candidates")
        appendLine()
        appendLine("| Name | Score | Tokens | Cost | Status |")
        appendLine("|------|-------|--------|------|--------|")
        result.candidates.forEach { c ->
            val badge = if (c.status == CandidateStatus.SELECTED) "✅ Selected" else "❌ Rejected"
            appendLine("| ${c.config.name} | ${"%.3f".format(c.score)} | ${c.estimatedTokens} | \$${"%.4f".format(c.estimatedCost)} | $badge |")
        }
        appendLine()

        if (selected != null) {
            appendLine("---")
            appendLine()
            appendLine("## Winning Configuration: ${selected.config.name}")
            appendLine()
            appendLine("**Tools:** ${selected.config.tools.joinToString(", ").ifEmpty { "(none)" }}")
            appendLine()
            appendLine("**Skills:** ${selected.config.skills.joinToString(", ").ifEmpty { "(none)" }}")
            appendLine()
            appendLine("**Score/Cost Ratio:** ${"%.2f".format(selected.scoreCostRatio)}")
            appendLine()
        }

        appendLine("---")
        appendLine()
        appendLine("## Stem Agent Interpretation")
        appendLine()
        appendLine("The **stem agent** in this project does not rewrite source code freely.")
        appendLine("It generates and evaluates `AgentConfig` objects — structured configurations")
        appendLine("that specify tools, skills, prompt strategy, and token budget for a specialized agent.")
        appendLine()
        appendLine("The loop is: **propose → evaluate → compare → select → freeze**.")
        appendLine()
        appendLine("The winning configuration is locked after a single propose-evaluate cycle.")
        appendLine("The LLM never has write access to the project's own source files.")
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("## Safeguards")
        appendLine()
        appendLine("| Constraint | Value |")
        appendLine("|------------|-------|")
        appendLine("| Max candidates per run | 3 |")
        appendLine("| Max evolution rounds | 1 (single propose-evaluate cycle) |")
        appendLine("| Max tokens per run | 100,000 |")
        appendLine("| Max estimated cost | \$2.00 |")
        appendLine("| LLM write access to source | **None** |")
        appendLine("| API key storage | Environment variable only — never in source |")
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("## What Happened in This Run")
        appendLine()
        if (selected == null) {
            appendLine("No candidate outperformed the baseline agent.")
            appendLine("The baseline configuration is retained.")
        } else {
            appendLine("${selected.config.name} was selected with score `${"%.3f".format(selected.score)}`.")
            appendLine("All other candidates were evaluated and rejected by `VersionManager.selectBest()`.")
            appendLine()
            val rejected = result.candidates.filter { it.status == com.stemlab.core.model.CandidateStatus.REJECTED && it.id != "baseline" }
            if (rejected.isNotEmpty()) {
                appendLine("**Rejected candidates and reasons:**")
                rejected.forEach { c ->
                    val reason = when {
                        c.score <= result.baselineScore -> "score ${"%.3f".format(c.score)} did not beat baseline (${"%.3f".format(result.baselineScore)})"
                        else -> "lower score/cost ratio than ${selected.config.name}"
                    }
                    appendLine("- ${c.config.name}: $reason")
                }
            }
        }
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("## What Would Be Improved With More Time")
        appendLine()
        appendLine("- **Multi-round evolution**: run a second propose-evaluate cycle using the winner of round 1 as the new baseline")
        appendLine("- **LLM-as-judge scoring**: replace keyword matching with an LLM evaluator for semantic correctness")
        appendLine("- **Persistent frozen agent**: save the selected `AgentConfig` and reload it for subsequent runs")
        appendLine("- **Per-task response diff view**: a UI tab showing what each candidate said vs. the baseline")
        appendLine("- **Domain-specific tool libraries**: richer registries for SQL, TypeScript, documentation, etc.")
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("## Evolution Log")
        appendLine()
        appendLine("```")
        result.logs.forEach { appendLine(it) }
        appendLine("```")
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("*Generated by Stem Agent Lab on ${Instant.now()}*")
    }
}
