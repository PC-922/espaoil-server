package espaoil.server.infrastructure.utils

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import kotlin.text.Charsets.UTF_8

private const val CONNECT_TIMEOUT_MS = 10_000L
private const val READ_TIMEOUT_MS = 30_000L

class URLWrapper {
    private val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
        .sslContext(SSLContext.getInstance("TLSv1.2").apply { init(null, null, null) })
        .sslParameters(SSLParameters().apply { protocols = arrayOf("TLSv1.2") })
        .build()

    fun get(url: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Encoding", "gzip")
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
