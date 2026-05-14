package com.stemlab.tools

object FileReaderTool {
    fun read(path: String): String =
        try { java.io.File(path).readText() } catch (e: Exception) { "Error reading $path: ${e.message}" }

    fun describe(code: String): String {
        val lines = code.lines()
        val functions = lines.count { it.trimStart().startsWith("def ") }
        val classes = lines.count { it.trimStart().startsWith("class ") }
        return "Code: ${lines.size} lines, $functions function(s), $classes class(es)"
    }
}
