package com.stemlab

import com.stemlab.app.AppController
import com.stemlab.llm.LlmClient
import com.stemlab.llm.LlmResponse
import com.stemlab.storage.ProjectStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppControllerProjectTest {

    private class FakeLlmClient : LlmClient {
        override suspend fun complete(prompt: String): LlmResponse = when {
            prompt.contains("GENERATE_TASKS") -> LlmResponse(
                text = """
                    [
                      {
                        "id": "task_001",
                        "description": "Find a bug",
                        "code": "fun value() = 1",
                        "expectedIssueKeywords": ["bug"]
                      }
                    ]
                """.trimIndent(),
                tokensUsed = 10,
                costEstimate = 0.001
            )
            prompt.contains("PROPOSE_CANDIDATES") -> LlmResponse(
                text = """
                    {
                      "candidates": [
                        {"id":"candidate_a","name":"Candidate A","description":"A","tools":["code_reader"],"skills":["direct_reasoning"],"promptStrategy":"tool_augmented","maxTokens":800},
                        {"id":"candidate_b","name":"Candidate B","description":"B","tools":["code_reader","test_generator"],"skills":["direct_reasoning"],"promptStrategy":"tool_augmented","maxTokens":1200},
                        {"id":"candidate_c","name":"Candidate C","description":"C","tools":["code_reader","python_runner"],"skills":["direct_reasoning"],"promptStrategy":"tool_augmented","maxTokens":1500}
                      ]
                    }
                """.trimIndent(),
                tokensUsed = 20,
                costEstimate = 0.002
            )
            prompt.contains("REPORT_NARRATIVE") -> LlmResponse(
                "Candidate B gives the clearest improvement for this benchmark run.",
                tokensUsed = 7,
                costEstimate = 0.0007
            )
            prompt.contains("TOOLS: (none)") -> LlmResponse("needs review", 10, 0.001)
            else -> LlmResponse("bug found", 20, 0.002)
        }
    }

    private fun controller(): Pair<AppController, File> {
        val dir = File("build/test-app-controller/${UUID.randomUUID()}")
        return AppController(
            llmClient = FakeLlmClient(),
            projectStore = ProjectStore(dir.path)
        ) to dir
    }

    @Test
    fun `creates and selects a new project`() {
        val (controller, dir) = controller()
        try {
            controller.createProject("SQL Review", "SQL Optimizer")

            val state = controller.state.value
            assertEquals(2, state.projects.size)
            assertEquals("SQL Review", state.activeProject?.name)
            assertEquals("SQL Optimizer", state.activeProject?.domain)
        } finally {
            controller.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `updates active project name and domain together`() {
        val (controller, dir) = controller()
        try {
            controller.updateActiveProject("Python Bug Hunt", "Python Edge Cases", "Renamed project")

            val project = controller.state.value.activeProject
            assertNotNull(project)
            assertEquals("Python Bug Hunt", project.name)
            assertEquals("Python Edge Cases", project.domain)
            assertEquals("Renamed project", project.spec.description)
            assertTrue(project.lastResult == null)
        } finally {
            controller.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `sanitizes project text fields before saving`() {
        val (controller, dir) = controller()
        try {
            controller.createProject(
                name = "  SQL\n\nReview  ",
                domain = "  SQL\tOptimizer  ",
                description = "  Notes\nwith   spacing  "
            )

            val project = controller.state.value.activeProject
            assertNotNull(project)
            assertEquals("SQL Review", project.name)
            assertEquals("SQL Optimizer", project.domain)
            assertEquals("Notes with spacing", project.spec.description)
        } finally {
            controller.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `runs two projects as independent sessions`() = runBlocking {
        val (controller, dir) = controller()
        try {
            controller.createProject("SQL Review", "SQL Optimizer")
            val sqlId = controller.state.value.activeProject?.id ?: error("No SQL project")
            controller.selectProject("python-qa")
            controller.runEvolution()
            controller.selectProject(sqlId)
            controller.runEvolution()

            repeat(50) {
                val projects = controller.state.value.projects
                if (projects.all { it.lastResult != null }) return@repeat
                delay(20)
            }

            val projects = controller.state.value.projects
            assertTrue(projects.all { !it.isRunning })
            assertTrue(projects.all { it.lastResult != null })
            assertNotNull(projects.first { it.id == "python-qa" }.lastResult)
            assertNotNull(projects.first { it.id == sqlId }.lastResult)
            projects.forEach { project ->
                assertEquals(100, project.lastResult?.totalTokensUsed)
                assertEquals(0.01, project.lastResult?.totalCost ?: 0.0, absoluteTolerance = 0.0001)
            }
        } finally {
            controller.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `export report includes llm generated human summary`() = runBlocking {
        val (controller, dir) = controller()
        try {
            controller.runEvolution()
            repeat(50) {
                if (controller.state.value.activeProject?.lastResult != null) return@repeat
                delay(20)
            }

            controller.exportReport()
            repeat(50) {
                if (controller.state.value.activeProject?.lastExportPath != null) return@repeat
                delay(20)
            }

            val project = controller.state.value.activeProject ?: error("No active project")
            val exportPath = project.lastExportPath ?: error("No report was exported")
            val report = File(exportPath).readText()
            assertTrue(report.contains("Human Summary"))
            assertTrue(report.contains("Candidate B gives the clearest improvement"))
            assertEquals(107, project.lastResult?.totalTokensUsed)
        } finally {
            controller.close()
            dir.deleteRecursively()
        }
    }
}
