package espaoil.server.infrastructure.utils

import java.net.URL
import kotlin.text.Charsets.UTF_8

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 30_000
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36 espaoil/1.0"

class URLWrapper {
    fun get(url: String): String {
        val connection = URL(url).openConnection()
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", USER_AGENT)
        return connection.getInputStream().use {
            it.readBytes()
        }.toString(UTF_8)
    }
}
