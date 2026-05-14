package com.stemlab.tools

object StaticAnalyzerTool {
    fun analyze(code: String): List<String> {
        val issues = mutableListOf<String>()
        val lines = code.lines()

        lines.forEachIndexed { i, line ->
            if (line.contains("/ 0") || line.contains("/0")) issues += "Line ${i + 1}: potential division by zero"
            if (line.contains("def ") && line.contains("=[") || line.contains("={}") || line.contains("=()"))
                issues += "Line ${i + 1}: mutable default argument detected"
            if (line.trimStart().startsWith("return ") && line.contains("[len("))
                issues += "Line ${i + 1}: off-by-one risk in index access"
        }

        if (issues.isEmpty()) issues += "No static issues found"
        return issues
    }
}
