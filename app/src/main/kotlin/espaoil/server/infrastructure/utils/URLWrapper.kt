package espaoil.server.infrastructure.utils

import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.text.Charsets.UTF_8

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 60_000
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

class URLWrapper {
    fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept-Encoding", "gzip")

        val inputStream = if (connection.contentEncoding == "gzip") {
            GZIPInputStream(connection.inputStream)
        } else {
            connection.inputStream
        }

        return inputStream.use { it.readBytes() }.toString(UTF_8)
    }
}
