package com.stemlab

import com.stemlab.core.tasks.LlmTaskGenerator
import com.stemlab.llm.OpenAiLlmClient
import com.stemlab.util.DotEnvLoader
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskGeneratorTest {

    private val generator = LlmTaskGenerator(OpenAiLlmClient(DotEnvLoader.requireApiKey()))

    @Test
    fun `openai task generator returns requested evaluable tasks`() = runBlocking {
        val tasks = generator.generate("Python QA", 2)
        assertEquals(2, tasks.size)
        assertTrue(tasks.all { it.id.isNotBlank() })
        assertTrue(tasks.all { it.description.isNotBlank() })
        assertTrue(tasks.all { it.code.isNotBlank() })
        assertTrue(tasks.all { it.expectedIssueKeywords.isNotEmpty() })
    }
}
