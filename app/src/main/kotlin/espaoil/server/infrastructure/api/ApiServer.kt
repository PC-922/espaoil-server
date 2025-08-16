package espaoil.server.infrastructure.api

import com.google.gson.Gson
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import espaoil.server.application.ports.GasStationPersister
import espaoil.server.application.usecases.RetrieveNearGasStation
import espaoil.server.domain.valueobject.Coordinates
import espaoil.server.infrastructure.dtos.output.NearGasStationDto
import espaoil.server.infrastructure.utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class ApiServer(
    private val gasStationPersister: GasStationPersister,
    private val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080
) {
    private val gson = Gson()
    private var httpServer: HttpServer? = null

    fun startAsync() {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        httpServer = server
        val executor = Executors.newFixedThreadPool(4)
        server.executor = executor

        server.createContext("/health") { exchange ->
            handleGet(exchange) {
                ok(exchange, mapOf("status" to "ok"))
            }
        }

        server.createContext("/gas-stations/near") { exchange ->
            handleGet(exchange) {
                val params = queryParams(exchange.requestURI)
                validateParameters(params).onFailure {
                    return@handleGet badRequest(exchange, mapOf("error" to it.message))
                }
                val lat = params["lat"]?.toDoubleOrNull()
                val lon = params["lon"]?.toDoubleOrNull()
                val distance = params["distance"]?.toIntOrNull() ?: 5000
                val gasType = normalizeGasType(params["gasType"]!!)

                if (lat == null || lon == null) {
                    return@handleGet badRequest(exchange, mapOf("error" to "Missing or invalid lat/lon"))
                }

                val stations = RetrieveNearGasStation(gasStationPersister).execute(
                        Coordinates(lat, lon),
                        distance,
                        gasType
                    )
                val payload = stations.map { NearGasStationDto.from(it, gasType) }
                return@handleGet ok(exchange, payload)
            }
        }

        server.start()
        LOG.info("HTTP API server started on port {}", port)
    }

    private fun validateParameters(params: Map<String, String>): Result<Unit> {
        val lat = params["lat"]
        val lon = params["lon"]
        val distance = params["distance"]
        val gasType = params["gasType"]

        if (lat == null || lon == null || lat.toDoubleOrNull() == null || lon.toDoubleOrNull() == null) {
            return Result.failure(IllegalArgumentException("Missing or invalid lat/lon parameters"))
        }
        if (distance == null || distance.toIntOrNull() == null) {
            return Result.failure(IllegalArgumentException("Missing or invalid distance parameter"))
        }
        if (gasType == null || gasType.isBlank()) {
            return Result.failure(IllegalArgumentException("Missing or invalid gas type parameter"))
        }
        return Result.success(Unit)
    }

    private fun handleGet(exchange: HttpExchange, block: () -> Unit) {
        if (exchange.requestMethod != "GET") {
            methodNotAllowed(exchange)
            return
        }
        try { block() } catch (ex: IllegalArgumentException) {
            badRequest(exchange, mapOf("error" to ex.message))
        } catch (ex: Exception) {
            LOG.error("Unhandled exception in GET handler", ex)
            internalError(exchange, mapOf("error" to (ex.message ?: "Unknown error")))
        }
    }

    private fun ok(exchange: HttpExchange, body: Any) = respond(exchange, 200, body)
    private fun badRequest(exchange: HttpExchange, body: Any) = respond(exchange, 400, body)
    private fun internalError(exchange: HttpExchange, body: Any) = respond(exchange, 500, body)

    private fun methodNotAllowed(exchange: HttpExchange) {
        exchange.responseHeaders.add("Allow", "GET, POST")
        respond(exchange, 405, mapOf("error" to "Method not allowed"))
    }

    private fun respond(exchange: HttpExchange, statusCode: Int, body: Any) {
        val json = gson.toJson(body)
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        exchange.responseBody.use { os: OutputStream -> os.write(bytes) }
    }

    private fun queryParams(uri: URI): Map<String, String> {
        val query = uri.rawQuery ?: return emptyMap()
        return query.split('&').mapNotNull { pair ->
            if (pair.isBlank()) return@mapNotNull null
            val idx = pair.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val key = decode(pair.substring(0, idx))
            val value = decode(pair.substring(idx + 1))
            key to value
        }.toMap()
    }

    private fun decode(s: String): String = URLDecoder.decode(s, StandardCharsets.UTF_8)

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(ApiServer::class.java)
    }

    private fun normalizeGasType(g: String): String {
        // Accept both enum-like names and internal keys
        return when (g.uppercase()) {
            "GASOLINA_95_E5" -> GASOLINA_95_E5
            "GASOLINA_95_E5_PREMIUM" -> GASOLINA_95_E5_PREMIUM
            "GASOLINA_95_E10" -> GASOLINA_95_E10
            "GASOLINA_98_E5" -> GASOLINA_98_E5
            "GASOLINA_98_E10" -> GASOLINA_98_E10
            "GASOIL_A" -> GASOIL_A
            "GASOIL_B" -> GASOIL_B
            "GASOIL_PREMIUM" -> GASOIL_PREMIUM
            else -> g
        }
    }

    fun stop(delaySeconds: Int = 0) {
        httpServer?.stop(delaySeconds)
        httpServer = null
    }
}
