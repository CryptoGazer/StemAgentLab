package com.stemlab.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object JsonStorage {
    @PublishedApi
    internal val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    inline fun <reified T> save(value: T, path: String) {
        File(path).apply {
            parentFile?.mkdirs()
            writeText(json.encodeToString(value))
        }
    }

    inline fun <reified T> load(path: String): T? =
        try { json.decodeFromString<T>(File(path).readText()) } catch (_: Exception) { null }

    fun exists(path: String): Boolean = File(path).exists()
}
