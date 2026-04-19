package espaoil.server.infrastructure.api

import com.google.gson.Gson
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import espaoil.server.application.ports.GasStationPersister
import espaoil.server.application.usecases.RetrieveNearGasStation
import espaoil.server.domain.valueobject.Coordinates
import espaoil.server.infrastructure.adapters.NominatimGeocoder
import espaoil.server.infrastructure.adapters.UpstreamGeocodingException
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

private const val MAX_NEAR_RESULTS = 30

class ApiServer(
    private val gasStationPersister: GasStationPersister,
    private val geocoder: NominatimGeocoder = NominatimGeocoder(),
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
                ok(exchange, mapOf("status" to "ok", "live-reload" to "enabled", "test" to "hot-reload-working"))
            }
        }

        server.createContext("/gas-stations/near") { exchange ->
            handleGet(exchange) {
                val params = queryParams(exchange.requestURI)
                validateNearParameters(params).onFailure {
                    return@handleGet badRequest(exchange, mapOf("error" to it.message))
                }
                val lat = params["lat"]!!.toDouble()
                val lon = params["lon"]!!.toDouble()
                val distance = params["distance"]!!.toInt()
                val gasType = normalizeGasType(params["gasType"]!!)
                val sortBy = (params["sortBy"] ?: "price").lowercase()
                val userCoords = Coordinates(lat, lon)

                val stations = RetrieveNearGasStation(gasStationPersister).execute(
                    userCoords, distance, gasType
                )
                val payload = stations
                    .map { NearGasStationDto.from(it, gasType, userCoords) }
                    .let { dtos ->
                        when (sortBy) {
                            "price" -> dtos.sortedBy { it.price }
                            "distance" -> dtos.sortedBy { it.distance }
                            else -> dtos.sortedBy { it.price }
                        }
                    }
                    .take(MAX_NEAR_RESULTS)
                return@handleGet ok(exchange, payload)
            }
        }

        server.createContext("/geocoding/search") { exchange ->
            handleGet(exchange) {
                val params = queryParams(exchange.requestURI)
                val q = params["q"]?.trim()
                if (q.isNullOrBlank() || q.length < 3) {
                    return@handleGet badRequest(exchange, mapOf("error" to "Parameter 'q' required, min 3 chars"))
                }
                val limit = params["limit"]?.toIntOrNull() ?: 5
                val results = geocoder.search(q, limit)
                return@handleGet ok(exchange, results)
            }
        }

        server.createContext("/geocoding/resolve") { exchange ->
            handleGet(exchange) {
                val params = queryParams(exchange.requestURI)
                val q = params["q"]?.trim()
                if (q.isNullOrBlank()) {
                    return@handleGet badRequest(exchange, mapOf("error" to "Parameter 'q' required"))
                }
                val result = geocoder.resolve(q)
                result
                    .onSuccess { suggestion ->
                        return@handleGet ok(exchange, mapOf("lat" to suggestion.lat, "lon" to suggestion.lon))
                    }
                    .onFailure { error ->
                        return@handleGet when (error) {
                            is NoSuchElementException -> respond(exchange, 404, mapOf("error" to "Address not found"))
                            is UpstreamGeocodingException -> respond(exchange, 502, mapOf("error" to "Upstream geocoding failure"))
                            is IllegalStateException -> respond(exchange, 422, mapOf("error" to "Invalid coordinates in result"))
                            else -> internalError(exchange, mapOf("error" to (error.message ?: "Unknown error")))
                        }
                    }
            }
        }

        server.start()
        LOG.info("HTTP API server started on port {}", port)
    }

    private fun validateNearParameters(params: Map<String, String>): Result<Unit> {
        val lat = params["lat"]
        val lon = params["lon"]
        val distance = params["distance"]
        val gasType = params["gasType"]
        val sortBy = params["sortBy"]

        if (lat == null || lon == null || lat.toDoubleOrNull() == null || lon.toDoubleOrNull() == null) {
            return Result.failure(IllegalArgumentException("Missing or invalid lat/lon parameters"))
        }
        if (distance == null || distance.toIntOrNull() == null) {
            return Result.failure(IllegalArgumentException("Missing or invalid distance parameter"))
        }
        if (gasType == null || gasType.isBlank()) {
            return Result.failure(IllegalArgumentException("Missing or invalid gas type parameter"))
        }
        if (sortBy != null && sortBy.lowercase() != "price" && sortBy.lowercase() != "distance") {
            return Result.failure(IllegalArgumentException("Invalid sortBy parameter. Allowed: price, distance"))
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
