package com.stemlab.util

object JsonExtractor {

    // Strip ```json ... ``` or ``` ... ``` fences that LLMs commonly add
    private fun stripCodeFences(text: String): String =
        text.replace(Regex("```[a-zA-Z]*\\n?"), "").trim()

    fun extractObject(text: String): String {
        val cleaned = stripCodeFences(text)
        val start = cleaned.indexOf('{')
        if (start == -1) throw IllegalArgumentException("No JSON object found in LLM response")
        return findMatchingClose(cleaned, start, '{', '}')
    }

    fun extractArray(text: String): String {
        val cleaned = stripCodeFences(text)
        val start = cleaned.indexOf('[')
        if (start == -1) throw IllegalArgumentException("No JSON array found in LLM response")
        return findMatchingClose(cleaned, start, '[', ']')
    }

    // Walks forward from `start`, tracking string/escape state and bracket depth.
    // Returns the substring from `start` through the matching close bracket (inclusive).
    private fun findMatchingClose(text: String, start: Int, open: Char, close: Char): String {
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until text.length) {
            val c = text[i]
            when {
                escape          -> escape = false
                inString && c == '\\' -> escape = true
                inString && c == '"'  -> inString = false
                !inString && c == '"' -> inString = true
                !inString && c == open  -> depth++
                !inString && c == close -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        throw IllegalArgumentException("Unmatched '$open' — malformed JSON in LLM response")
    }
}
