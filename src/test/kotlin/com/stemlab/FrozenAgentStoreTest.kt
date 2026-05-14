package com.stemlab

import com.stemlab.core.model.AgentConfig
import com.stemlab.core.model.FrozenAgent
import com.stemlab.core.model.ProjectSpec
import com.stemlab.storage.ProjectStore
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrozenAgentStoreTest {

    private fun tempStore(): Pair<ProjectStore, File> {
        val dir = File("build/test-frozen-agent/${UUID.randomUUID()}")
        return ProjectStore(dir.path) to dir
    }

    private val projectId = "python-qa"

    private fun spec() = ProjectSpec(
        id = projectId,
        name = "Python QA",
        domain = "Python QA",
        createdAt = Instant.now().toString(),
        updatedAt = Instant.now().toString()
    )

    private fun config(name: String = "CandidateB") = AgentConfig(
        id = name.lowercase(),
        name = name,
        description = "Test candidate",
        tools = listOf("python_runner", "file_reader"),
        skills = listOf("edge_case_reasoning", "test_generation"),
        promptStrategy = "chain-of-thought",
        maxTokens = 800
    )

    private fun frozen(score: Double = 0.857, baseline: Double = 0.314) = FrozenAgent(
        projectId = projectId,
        runId = "run-abc123",
        domain = "Python QA",
        frozenAt = Instant.now().toString(),
        config = config(),
        score = score,
        baselineScore = baseline,
        improvement = score - baseline,
        estimatedTokens = 2400L,
        estimatedCost = 0.0048
    )

    @Test
    fun `save and load frozen agent round-trips correctly`() {
        val (store, dir) = tempStore()
        try {
            store.saveProject(spec())
            val agent = frozen()
            store.saveFrozenAgent(projectId, agent)

            val loaded = store.loadFrozenAgent(projectId)
            assertNotNull(loaded)
            assertEquals(agent.config.name, loaded.config.name)
            assertEquals(agent.score, loaded.score)
            assertEquals(agent.baselineScore, loaded.baselineScore)
            assertEquals(agent.domain, loaded.domain)
            assertEquals(agent.estimatedTokens, loaded.estimatedTokens)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `frozen agent is stored at projects slash projectId slash agent dot json`() {
        val (store, dir) = tempStore()
        try {
            store.saveProject(spec())
            store.saveFrozenAgent(projectId, frozen())

            val expectedPath = store.frozenAgentPath(projectId)
            assertTrue(File(expectedPath).exists(), "agent.json should exist at $expectedPath")
            assertTrue(expectedPath.endsWith("$projectId/agent.json"), "path should end with <projectId>/agent.json")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `load returns null when no frozen agent has been saved`() {
        val (store, dir) = tempStore()
        try {
            store.saveProject(spec())
            assertNull(store.loadFrozenAgent(projectId))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `saving a new frozen agent overwrites the previous one`() {
        val (store, dir) = tempStore()
        try {
            store.saveProject(spec())

            val first = frozen(score = 0.543, baseline = 0.314)
            store.saveFrozenAgent(projectId, first)

            val second = frozen(score = 0.857, baseline = 0.314).copy(runId = "run-xyz789")
            store.saveFrozenAgent(projectId, second)

            val loaded = store.loadFrozenAgent(projectId)
            assertNotNull(loaded)
            assertEquals("run-xyz789", loaded.runId)
            assertEquals(0.857, loaded.score)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `frozen agent config fields are preserved`() {
        val (store, dir) = tempStore()
        try {
            store.saveProject(spec())
            val cfg = config("CandidateB")
            store.saveFrozenAgent(projectId, frozen().copy(config = cfg))

            val loaded = store.loadFrozenAgent(projectId)
            assertNotNull(loaded)
            assertEquals(cfg.tools, loaded.config.tools)
            assertEquals(cfg.skills, loaded.config.skills)
            assertEquals(cfg.promptStrategy, loaded.config.promptStrategy)
            assertEquals(cfg.description, loaded.config.description)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `improvementPercent is correct on loaded agent`() {
        val (store, dir) = tempStore()
        try {
            store.saveProject(spec())
            store.saveFrozenAgent(projectId, frozen(score = 0.857, baseline = 0.314))

            val loaded = store.loadFrozenAgent(projectId)!!
            val expected = ((0.857 - 0.314) / 0.314) * 100.0
            assertEquals(expected, loaded.improvementPercent, 0.001)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `frozen agent is removed when project is deleted`() {
        val (store, dir) = tempStore()
        try {
            store.saveProject(spec())
            store.saveFrozenAgent(projectId, frozen())
            assertNotNull(store.loadFrozenAgent(projectId))

            store.deleteProject(projectId)

            // The project directory (and agent.json inside) should be gone
            assertNull(store.loadFrozenAgent(projectId))
        } finally {
            dir.deleteRecursively()
        }
    }
}
