package com.stemlab

import com.stemlab.core.model.EvolutionResult
import com.stemlab.core.model.ProjectSpec
import com.stemlab.storage.ProjectStore
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectStoreTest {

    private fun tempStore(): Pair<ProjectStore, File> {
        val dir = File("build/test-project-store/${UUID.randomUUID()}")
        return ProjectStore(dir.path) to dir
    }

    private fun project(id: String = "sql-review") = ProjectSpec(
        id = id,
        name = "SQL Review",
        domain = "SQL Optimizer",
        createdAt = Instant.now().toString(),
        updatedAt = Instant.now().toString()
    )

    private fun result(runId: String = "run-1") = EvolutionResult(
        runId = runId,
        domain = "SQL Optimizer",
        baselineScore = 0.0,
        candidates = emptyList(),
        selectedCandidateId = null,
        improvement = 0.0,
        totalTokensUsed = 10,
        totalCost = 0.001,
        logs = listOf("started"),
        timestamp = Instant.now().toString()
    )

    @Test
    fun `saves and loads project index`() {
        val (store, dir) = tempStore()
        try {
            val project = project()
            store.saveProject(project)

            assertEquals(listOf(project), store.loadProjects())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `saves project runs under project directory`() {
        val (store, dir) = tempStore()
        try {
            val project = project()
            val result = result()

            store.saveProject(project)
            store.saveRun(project.id, result)

            val latest = store.latestRun(project.id)
            assertNotNull(latest)
            assertEquals(result.runId, latest.runId)
            assertTrue(File(store.reportPath(project.id, result.runId)).path.contains(project.id))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `rejects unsafe project ids`() {
        val (store, dir) = tempStore()
        try {
            assertFailsWith<IllegalArgumentException> {
                store.saveProject(project(id = "../outside"))
            }
            assertFailsWith<IllegalArgumentException> {
                store.deleteProject("../../outside")
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `rejects unsafe run ids in project paths`() {
        val (store, dir) = tempStore()
        try {
            val project = project()
            store.saveProject(project)

            assertFailsWith<IllegalArgumentException> {
                store.saveRun(project.id, result(runId = "../run"))
            }
            assertFailsWith<IllegalArgumentException> {
                store.reportPath(project.id, "../report")
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
