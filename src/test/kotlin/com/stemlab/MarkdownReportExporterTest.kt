package com.stemlab

import com.stemlab.core.model.*
import com.stemlab.report.MarkdownReportExporter
import java.io.File
import kotlin.test.*

class MarkdownReportExporterTest {

    private fun makeResult(
        baselineScore: Double = 0.314,
        selectedId: String? = "candidate_b",
        candidates: List<CandidateAgent> = defaultCandidates()
    ) = EvolutionResult(
        runId = "test-run",
        domain = "Python QA",
        baselineScore = baselineScore,
        candidates = candidates,
        selectedCandidateId = selectedId,
        improvement = 0.543,
        totalTokensUsed = 12_000,
        totalCost = 0.024,
        logs = listOf("[12:00] Evolution started", "[12:01] Baseline score: 0.314"),
        timestamp = "2026-05-14T12:00:00Z"
    )

    private fun defaultCandidates() = listOf(
        CandidateAgent(
            id = "baseline",
            config = AgentConfig(id = "baseline", name = "Baseline Agent", tools = emptyList(), skills = emptyList()),
            score = 0.314,
            estimatedCost = 0.003,
            status = CandidateStatus.REJECTED
        ),
        CandidateAgent(
            id = "candidate_a",
            config = AgentConfig(id = "candidate_a", name = "Candidate A", tools = listOf("code_reader"), skills = emptyList()),
            score = 0.543,
            estimatedCost = 0.007,
            status = CandidateStatus.REJECTED
        ),
        CandidateAgent(
            id = "candidate_b",
            config = AgentConfig(id = "candidate_b", name = "Candidate B", tools = listOf("code_reader", "test_generator"), skills = emptyList()),
            score = 0.857,
            estimatedCost = 0.009,
            status = CandidateStatus.SELECTED
        )
    )

    @Test
    fun `buildReport contains domain`() {
        val report = MarkdownReportExporter.buildReport(makeResult())
        assertTrue(report.contains("Python QA"), "Report must contain the domain")
    }

    @Test
    fun `buildReport contains baseline score`() {
        val report = MarkdownReportExporter.buildReport(makeResult(baselineScore = 0.314))
        assertTrue(report.contains("0.314"), "Report must show baseline score")
    }

    @Test
    fun `buildReport contains best candidate score`() {
        val report = MarkdownReportExporter.buildReport(makeResult())
        assertTrue(report.contains("0.857"), "Report must show best candidate score")
    }

    @Test
    fun `buildReport contains selected candidate name`() {
        val report = MarkdownReportExporter.buildReport(makeResult())
        assertTrue(report.contains("Candidate B"), "Report must name the selected candidate")
    }

    @Test
    fun `buildReport marks selected candidate as selected`() {
        val report = MarkdownReportExporter.buildReport(makeResult())
        assertTrue(report.contains("Selected") || report.contains("✅"), "Report must mark the selected candidate")
    }

    @Test
    fun `buildReport lists rejected candidates`() {
        val report = MarkdownReportExporter.buildReport(makeResult())
        assertTrue(report.contains("Candidate A"), "Report must list rejected candidates")
        assertTrue(report.contains("Rejected") || report.contains("❌"))
    }

    @Test
    fun `buildReport contains evolution log entries`() {
        val report = MarkdownReportExporter.buildReport(makeResult())
        assertTrue(report.contains("Evolution started"))
    }

    @Test
    fun `buildReport contains safeguards section`() {
        val report = MarkdownReportExporter.buildReport(makeResult())
        assertTrue(report.contains("Safeguard") || report.contains("safeguard") || report.contains("budget"),
            "Report must contain safeguards section")
    }

    @Test
    fun `buildReport contains human summary section`() {
        val report = MarkdownReportExporter.buildReport(makeResult())
        assertTrue(report.contains("Human Summary"), "Report must contain human summary section")
    }

    @Test
    fun `buildReport includes provided llm narrative`() {
        val narrative = "Candidate B is the best trade-off for this run."
        val report = MarkdownReportExporter.buildReport(makeResult(), narrative)
        assertTrue(report.contains(narrative), "Report must include the LLM-generated narrative")
    }

    @Test
    fun `export creates file on disk`() {
        val outPath = "build/test-reports/test-report.md"
        val result = makeResult()
        val returnedPath = MarkdownReportExporter.export(result, outPath)
        assertEquals(outPath, returnedPath)
        assertTrue(File(outPath).exists(), "Report file must exist on disk")
        File(outPath).delete()
    }

    @Test
    fun `buildReport handles no selected candidate gracefully`() {
        val report = MarkdownReportExporter.buildReport(makeResult(selectedId = null))
        assertFalse(report.isEmpty())
        assertTrue(report.contains("0.314"))
    }

    @Test
    fun `buildReport contains run id`() {
        val report = MarkdownReportExporter.buildReport(makeResult())
        assertTrue(report.contains("test-run"))
    }
}
