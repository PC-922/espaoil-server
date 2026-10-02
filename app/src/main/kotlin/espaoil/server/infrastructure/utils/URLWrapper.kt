package espaoil.server.infrastructure.utils

import java.io.IOException
import java.util.concurrent.TimeUnit

class URLWrapper {

    fun get(url: String): String {
        val process = ProcessBuilder(
                "curl",
                "-s",                  // Silent mode (no progress bar)
                "-S",                  // Show error message if it fails
                "-L",                  // Follow redirects
                "--max-time", "60",    // Overall timeout in seconds
                "--tlsv1.2",           // Force TLS 1.2
                "-H", "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "-H", "Accept: application/json",
                url
        ).start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val error = process.errorStream.bufferedReader().use { it.readText() }

        val completed = process.waitFor(60, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            throw IOException("curl execution timed out for URL: $url")
        }

        if (process.exitValue() != 0) {
            throw IOException("curl failed with exit code ${process.exitValue()}: $error")
        }

        return output
    }
}