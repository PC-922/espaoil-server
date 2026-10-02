package espaoil.server.infrastructure.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class URLWrapper {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        return client.newCall(request).execute().use { response: Response ->
            if (!response.isSuccessful) {
                throw IOException("Código de error HTTP inesperado: ${response.code}")
            }
            response.body?.string() ?: ""
        }
    }
}
