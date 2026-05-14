package com.stemlab.storage

import com.stemlab.core.model.EvolutionResult
import com.stemlab.core.model.ProjectSpec
import kotlinx.serialization.Serializable
import java.io.File

class ProjectStore(private val rootDir: String = "projects") {

    @Serializable
    private data class ProjectIndex(val projects: List<ProjectSpec> = emptyList())

    private val indexPath = "$rootDir/index.json"

    @Synchronized
    fun loadProjects(): List<ProjectSpec> =
        JsonStorage.load<ProjectIndex>(indexPath)?.projects.orEmpty()

    @Synchronized
    fun saveProjects(projects: List<ProjectSpec>) {
        projects.forEach { requireSafeProjectId(it.id) }
        JsonStorage.save(ProjectIndex(projects.distinctBy { it.id }.sortedBy { it.createdAt }), indexPath)
    }

    @Synchronized
    fun saveProject(project: ProjectSpec) {
        requireSafeProjectId(project.id)
        val projects = loadProjects()
            .filterNot { it.id == project.id } + project
        saveProjects(projects)
    }

    @Synchronized
    fun deleteProject(projectId: String) {
        requireSafeProjectId(projectId)
        saveProjects(loadProjects().filterNot { it.id == projectId })
        projectDir(projectId).deleteRecursively()
    }

    @Synchronized
    fun saveRun(projectId: String, result: EvolutionResult) {
        requireSafeProjectId(projectId)
        requireSafeFileId(result.runId, "run id")
        JsonStorage.save(result, runPath(projectId, result.runId))
    }

    @Synchronized
    fun loadRuns(projectId: String): List<EvolutionResult> {
        requireSafeProjectId(projectId)
        val dir = File(runsDir(projectId))
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { JsonStorage.load<EvolutionResult>(it.path) }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    @Synchronized
    fun latestRun(projectId: String): EvolutionResult? = loadRuns(projectId).firstOrNull()

    @Synchronized
    fun reportPath(projectId: String, runId: String): String =
        "${projectRoot(projectId)}/reports/report-${safeFileId(runId, "run id")}.md"

    private fun runPath(projectId: String, runId: String): String =
        "${runsDir(projectId)}/${safeFileId(runId, "run id")}.json"

    private fun runsDir(projectId: String): String =
        "${projectRoot(projectId)}/runs"

    private fun projectRoot(projectId: String): String =
        projectDir(projectId).path

    private fun projectDir(projectId: String): File {
        requireSafeProjectId(projectId)
        val base = File(rootDir).canonicalFile
        val dir = File(base, projectId).canonicalFile
        require(dir.path == base.path || dir.path.startsWith(base.path + File.separator)) {
            "Project path escapes storage root: $projectId"
        }
        return dir
    }

    private fun requireSafeProjectId(projectId: String) {
        require(projectId.matches(PROJECT_ID_PATTERN)) {
            "Invalid project id: $projectId"
        }
    }

    private fun safeFileId(value: String, label: String): String {
        requireSafeFileId(value, label)
        return value
    }

    private fun requireSafeFileId(value: String, label: String) {
        require(value.matches(FILE_ID_PATTERN)) {
            "Invalid $label: $value"
        }
    }

    private companion object {
        val PROJECT_ID_PATTERN = Regex("[a-z0-9][a-z0-9-]{0,63}")
        val FILE_ID_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")
    }
}
