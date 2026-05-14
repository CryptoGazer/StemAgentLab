package com.stemlab

import com.stemlab.util.JsonExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonExtractorTest {

    // ── extractObject ────────────────────────────────────────────────────────

    @Test
    fun `extractObject returns simple JSON object`() {
        val result = JsonExtractor.extractObject("""{"key": "value"}""")
        assertEquals("""{"key": "value"}""", result)
    }

    @Test
    fun `extractObject ignores trailing garbage after closing brace`() {
        val result = JsonExtractor.extractObject("""{"ok": true} trailing garbage {not json}""")
        assertEquals("""{"ok": true}""", result)
    }

    @Test
    fun `extractObject ignores leading prose before opening brace`() {
        val result = JsonExtractor.extractObject("""Here is the JSON: {"ok": true}""")
        assertEquals("""{"ok": true}""", result)
    }

    @Test
    fun `extractObject handles brace inside string value`() {
        val result = JsonExtractor.extractObject("""{"msg": "contains { and } inside"} trailing}""")
        assertEquals("""{"msg": "contains { and } inside"}""", result)
    }

    @Test
    fun `extractObject handles escaped quote inside string`() {
        val result = JsonExtractor.extractObject("""{"msg": "say \"hello\""} trailing}""")
        assertEquals("""{"msg": "say \"hello\""}""", result)
    }

    @Test
    fun `extractObject handles nested objects`() {
        val input = """{"outer": {"inner": 1}} extra}"""
        val result = JsonExtractor.extractObject(input)
        assertEquals("""{"outer": {"inner": 1}}""", result)
    }

    @Test
    fun `extractObject strips markdown code fence`() {
        val input = "```json\n{\"key\": \"value\"}\n```"
        val result = JsonExtractor.extractObject(input)
        assertEquals("""{"key": "value"}""", result)
    }

    @Test
    fun `extractObject strips plain code fence`() {
        val input = "```\n{\"key\": 1}\n```"
        val result = JsonExtractor.extractObject(input)
        assertEquals("""{"key": 1}""", result)
    }

    @Test
    fun `extractObject throws when no object present`() {
        assertFailsWith<IllegalArgumentException> {
            JsonExtractor.extractObject("no braces here at all")
        }
    }

    // ── extractArray ─────────────────────────────────────────────────────────

    @Test
    fun `extractArray returns simple array`() {
        val result = JsonExtractor.extractArray("""[1, 2, 3]""")
        assertEquals("[1, 2, 3]", result)
    }

    @Test
    fun `extractArray ignores trailing bracket in prose`() {
        // This was the real bug: items[0] after the array would fool lastIndexOf
        val result = JsonExtractor.extractArray("""[{"id": "t1"}] See items[0] for reference.""")
        assertEquals("""[{"id": "t1"}]""", result)
    }

    @Test
    fun `extractArray ignores bracket inside string value`() {
        val result = JsonExtractor.extractArray("""[{"desc": "check items[0]"}] items[1] extra]""")
        assertEquals("""[{"desc": "check items[0]"}]""", result)
    }

    @Test
    fun `extractArray handles nested arrays`() {
        val result = JsonExtractor.extractArray("""[["a", "b"], ["c"]] extra]""")
        assertEquals("""[["a", "b"], ["c"]]""", result)
    }

    @Test
    fun `extractArray strips markdown code fence`() {
        val input = "```json\n[{\"id\": \"t1\"}]\n```"
        val result = JsonExtractor.extractArray(input)
        assertEquals("""[{"id": "t1"}]""", result)
    }

    @Test
    fun `extractArray throws when no array present`() {
        assertFailsWith<IllegalArgumentException> {
            JsonExtractor.extractArray("no brackets here")
        }
    }
}
