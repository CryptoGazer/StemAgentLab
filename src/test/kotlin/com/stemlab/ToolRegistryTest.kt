package com.stemlab

import com.stemlab.core.registry.ToolRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToolRegistryTest {

    private val registry = ToolRegistry()

    @Test
    fun `known tool resolves correctly`() {
        val tools = registry.resolve(listOf("code_reader"))
        assertEquals(1, tools.size)
        assertEquals("code_reader", tools[0].id)
    }

    @Test
    fun `unknown tool id returns generic spec instead of failing`() {
        val tools = registry.resolve(listOf("custom_tool_xyz"))
        assertEquals(1, tools.size)
        assertEquals("custom_tool_xyz", tools[0].id)
    }

    @Test
    fun `mixed known and unknown tools all resolve`() {
        val tools = registry.resolve(listOf("code_reader", "unknown_tool", "test_generator"))
        assertEquals(3, tools.size)
    }

    @Test
    fun `generic spec has non-zero cost estimate`() {
        val tools = registry.resolve(listOf("brand_new_tool"))
        assertTrue(tools[0].estimatedCostPerCall > 0)
    }

    @Test
    fun `generic spec name is human readable`() {
        val tools = registry.resolve(listOf("sql_optimizer"))
        assertNotNull(tools[0].name)
        assertTrue(tools[0].name.isNotBlank())
    }

    @Test
    fun `empty id list returns empty list`() {
        val tools = registry.resolve(emptyList())
        assertTrue(tools.isEmpty())
    }

    @Test
    fun `all six known tools are resolvable`() {
        val ids = listOf("code_reader", "test_generator", "python_runner", "failure_analyzer", "static_analyzer", "patch_suggester")
        val tools = registry.resolve(ids)
        assertEquals(6, tools.size)
        assertEquals(ids, tools.map { it.id })
    }
}
