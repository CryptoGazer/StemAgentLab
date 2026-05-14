package com.stemlab.util

object DotEnvLoader {

    fun loadApiKey(): String? {
        System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }?.let { return it }

        val envFile = java.io.File(".env")
        if (!envFile.exists()) return null

        return envFile.readLines()
            .firstOrNull { it.trim().startsWith("OPENAI_API_KEY=") }
            ?.trim()
            ?.removePrefix("OPENAI_API_KEY=")
            ?.trim()
            ?.removeSurrounding("\"")
            ?.removeSurrounding("'")
            ?.takeIf { it.isNotBlank() }
    }
}
