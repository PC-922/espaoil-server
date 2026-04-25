package espaoil.server.infrastructure.utils

import java.net.URL
import kotlin.text.Charsets.UTF_8

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 30_000

class URLWrapper {
    fun get(url: String): String {
        val connection = URL(url).openConnection()
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        return connection.getInputStream().use {
            it.readBytes()
        }.toString(UTF_8)
    }
}
