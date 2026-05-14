package com.stemlab

import com.stemlab.core.tasks.MockTaskGenerator
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskGeneratorTest {

    private val generator = MockTaskGenerator()

    @Test
    fun `python qa domain loads bundled tasks`() = runBlocking {
        val tasks = generator.generate("Python QA", 5)
        assertTrue(tasks.isNotEmpty(), "Should return tasks for Python QA domain")
        assertTrue(tasks.all { it.expectedIssueKeywords.isNotEmpty() })
    }

    @Test
    fun `python qa domain respects count limit`() = runBlocking {
        val tasks = generator.generate("Python QA", 3)
        assertEquals(3, tasks.size)
    }

    @Test
    fun `generic domain returns generic tasks`() = runBlocking {
        val tasks = generator.generate("SQL Optimizer", 5)
        assertEquals(5, tasks.size)
        assertTrue(tasks.all { it.id.startsWith("gen_") })
    }

    @Test
    fun `generic domain embeds domain name in descriptions`() = runBlocking {
        val domain = "SQL Optimizer"
        val tasks = generator.generate(domain, 5)
        assertTrue(tasks.any { it.description.contains(domain) })
    }

    @Test
    fun `generic domain count limit respected`() = runBlocking {
        val tasks = generator.generate("Code Reviewer", 2)
        assertEquals(2, tasks.size)
    }

    @Test
    fun `all tasks have non-empty expected keywords`() = runBlocking {
        val tasks = generator.generate("Data Pipeline", 5)
        assertTrue(tasks.all { it.expectedIssueKeywords.isNotEmpty() })
    }

    @Test
    fun `code domain uses bundled tasks`() = runBlocking {
        val tasks = generator.generate("code quality")
        assertTrue(tasks.isNotEmpty())
    }
}
