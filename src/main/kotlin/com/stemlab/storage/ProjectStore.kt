package com.stemlab.storage

import com.stemlab.core.model.EvolutionResult
import com.stemlab.core.model.ProjectSpec
import kotlinx.serialization.Serializable
import java.io.File

class ProjectStore(private val rootDir: String = "projects") {

    @Serializable
    private data class ProjectIndex(val projects: List<ProjectSpec> = emptyList())

    private val indexPath = "$rootDir/index.json"

    fun loadProjects(): List<ProjectSpec> =
        JsonStorage.load<ProjectIndex>(indexPath)?.projects.orEmpty()

    fun saveProjects(projects: List<ProjectSpec>) {
        JsonStorage.save(ProjectIndex(projects.sortedBy { it.createdAt }), indexPath)
    }

    fun saveProject(project: ProjectSpec) {
        val projects = loadProjects()
            .filterNot { it.id == project.id } + project
        saveProjects(projects)
    }

    fun deleteProject(projectId: String) {
        saveProjects(loadProjects().filterNot { it.id == projectId })
        File(projectRoot(projectId)).deleteRecursively()
    }

    fun saveRun(projectId: String, result: EvolutionResult) {
        JsonStorage.save(result, runPath(projectId, result.runId))
    }

    fun loadRuns(projectId: String): List<EvolutionResult> {
        val dir = File(runsDir(projectId))
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { JsonStorage.load<EvolutionResult>(it.path) }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    fun latestRun(projectId: String): EvolutionResult? = loadRuns(projectId).firstOrNull()

    fun reportPath(projectId: String, runId: String): String =
        "${projectRoot(projectId)}/reports/report-$runId.md"

    private fun runPath(projectId: String, runId: String): String =
        "${runsDir(projectId)}/$runId.json"

    private fun runsDir(projectId: String): String =
        "${projectRoot(projectId)}/runs"

    private fun projectRoot(projectId: String): String =
        "$rootDir/$projectId"
}
