package com.stemlab.tools

object PythonRunner {
    data class RunResult(val stdout: String, val stderr: String, val exitCode: Int)

    fun run(code: String, timeoutMs: Long = 5000L): RunResult {
        return try {
            val process = ProcessBuilder("python3", "-c", code)
                .redirectErrorStream(false)
                .start()
            val finished = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                RunResult("", "Timeout after ${timeoutMs}ms", 1)
            } else {
                RunResult(
                    stdout = process.inputStream.bufferedReader().readText(),
                    stderr = process.errorStream.bufferedReader().readText(),
                    exitCode = process.exitValue()
                )
            }
        } catch (e: Exception) {
            RunResult("", "Runner error: ${e.message}", 1)
        }
    }
}
