package com.stemlab.tools

object TestGeneratorTool {
    fun generateTests(functionName: String, code: String): String = """
import pytest

# Auto-generated test stub for: $functionName
class Test${functionName.replaceFirstChar { it.uppercase() }}:
    def test_happy_path(self):
        # TODO: fill in happy-path assertion
        pass

    def test_edge_case_zero(self):
        # TODO: test with zero / empty / None inputs
        pass

    def test_type_mismatch(self):
        # TODO: test with unexpected input types
        pass
""".trimIndent()
}
