package espaoil.server.infrastructure.utils

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import kotlin.text.Charsets.UTF_8

private const val CONNECT_TIMEOUT_MS = 10_000L
private const val READ_TIMEOUT_MS = 60_000L
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

class URLWrapper {
    private val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
        .build()

    fun get(url: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json, text/plain, */*")
            .header("User-Agent", USER_AGENT)
            .header("Accept-Encoding", "gzip")
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .header("Cache-Control", "no-cache")
            .timeout(Duration.ofMillis(READ_TIMEOUT_MS))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())

        if (response.statusCode() >= 400) {
            throw RuntimeException("HTTP error: ${response.statusCode()}")
        }

        val encoding = response.headers().firstValue("Content-Encoding").orElse("")
        val bodyBytes = response.body()

        val inputStream = if (encoding.equals("gzip", ignoreCase = true)) {
            GZIPInputStream(ByteArrayInputStream(bodyBytes))
        } else {
            ByteArrayInputStream(bodyBytes)
        }

        return inputStream.use { it.readBytes() }.toString(UTF_8)
    }
}
