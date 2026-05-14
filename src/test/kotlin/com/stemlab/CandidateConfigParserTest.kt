package com.stemlab

import com.stemlab.core.agent.CandidateConfigParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CandidateConfigParserTest {

    private val validJson = """
        {
          "candidates": [
            {
              "id": "candidate_a",
              "name": "Candidate A",
              "description": "Lightweight",
              "tools": ["code_reader", "static_analyzer"],
              "skills": ["direct_reasoning"],
              "promptStrategy": "tool_augmented",
              "maxTokens": 800
            },
            {
              "id": "candidate_b",
              "name": "Candidate B",
              "description": "Full pipeline",
              "tools": ["code_reader", "test_generator", "python_runner"],
              "skills": ["test_driven_analysis"],
              "promptStrategy": "tool_augmented",
              "maxTokens": 1200
            },
            {
              "id": "candidate_c",
              "name": "Candidate C",
              "description": "Maximal",
              "tools": ["code_reader", "test_generator", "patch_suggester"],
              "skills": ["patch_suggestion"],
              "promptStrategy": "tool_augmented",
              "maxTokens": 1500
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parses valid JSON with 3 candidates`() {
        val result = CandidateConfigParser.parse(validJson)
        assertNotNull(result)
        assertEquals(3, result.size)
    }

    @Test
    fun `parsed candidates have correct ids`() {
        val result = CandidateConfigParser.parse(validJson)!!
        assertEquals("candidate_a", result[0].id)
        assertEquals("candidate_b", result[1].id)
        assertEquals("candidate_c", result[2].id)
    }

    @Test
    fun `parsed candidates have correct tools`() {
        val result = CandidateConfigParser.parse(validJson)!!
        assertEquals(listOf("code_reader", "static_analyzer"), result[0].tools)
        assertEquals(listOf("code_reader", "test_generator", "python_runner"), result[1].tools)
    }

    @Test
    fun `returns null for invalid JSON`() {
        val result = CandidateConfigParser.parse("not json at all")
        assertNull(result)
    }

    @Test
    fun `returns null for JSON with wrong candidate count`() {
        val twoOnly = """{"candidates": [{"id":"a","name":"A","tools":[],"skills":[]},{"id":"b","name":"B","tools":[],"skills":[]}]}"""
        val result = CandidateConfigParser.parse(twoOnly)
        assertNull(result)
    }

    @Test
    fun `extracts JSON from surrounding prose`() {
        val withProse = """
            Here are the candidate configurations:
            $validJson
            End of response.
        """.trimIndent()
        val result = CandidateConfigParser.parse(withProse)
        assertNotNull(result)
        assertEquals(3, result.size)
    }

    @Test
    fun `returns null for empty string`() {
        assertNull(CandidateConfigParser.parse(""))
    }
}
