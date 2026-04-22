package espaoil.server.infrastructure.adapters

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class UpstreamGeocodingException(message: String, cause: Throwable? = null) : Exception(message, cause)

private const val NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search"
private const val USER_AGENT = "espaoil/1.0 (https://espaoil.app)"
private const val CONNECT_TIMEOUT_MS = 5000
private const val READ_TIMEOUT_MS = 8000
private const val CACHE_MAX_SIZE = 500

data class GeocodeSuggestion(
    val label: String,
    val lat: Double,
    val lon: Double,
)

private data class NominatimResult(
    val lat: String,
    val lon: String,
    val display_name: String,
)

open class NominatimGeocoder {

    private val gson = Gson()
    private val LOG = LoggerFactory.getLogger(NominatimGeocoder::class.java)

    // LRU cache: LinkedHashMap with accessOrder=true, remove eldest when over max
    private val cache: LinkedHashMap<String, List<GeocodeSuggestion>> =
        object : LinkedHashMap<String, List<GeocodeSuggestion>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, List<GeocodeSuggestion>>): Boolean =
                size > CACHE_MAX_SIZE
        }

    @Synchronized
    open fun search(query: String, limit: Int = 5): List<GeocodeSuggestion> {
        val trimmed = query.trim()
        if (trimmed.length < 3) return emptyList()

        val cacheKey = "$trimmed:$limit"
        cache[cacheKey]?.let { return it }

        val result = fetchFromNominatim(trimmed, limit)
        cache[cacheKey] = result
        return result
    }

    /**
     * Resolves a full address to a single coordinate pair.
     * Returns:
     *   Success(GeocodeSuggestion)       — found
     *   Failure(NoSuchElementException)  — 0 results from Nominatim
     *   Failure(IllegalStateException)   — result has non-finite lat/lon
     *   Failure(UpstreamGeocodingException) — Nominatim unreachable or non-200
     */
    @Synchronized
    open fun resolve(query: String): Result<GeocodeSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return Result.failure(NoSuchElementException("Empty query"))

        val cacheKey = "$trimmed:1"
        cache[cacheKey]?.let { cached ->
            return if (cached.isEmpty()) Result.failure(NoSuchElementException("Address not found"))
            else Result.success(cached.first())
        }

        return try {
            val results = fetchFromNominatimOrThrow(trimmed, limit = 1)
            if (results.isEmpty()) {
                cache[cacheKey] = emptyList()
                Result.failure(NoSuchElementException("Address not found"))
            } else {
                val suggestion = results.first()
                if (!suggestion.lat.isFinite() || !suggestion.lon.isFinite()) {
                    Result.failure(IllegalStateException("Invalid coordinates in geocoding result"))
                } else {
                    cache[cacheKey] = listOf(suggestion)
                    Result.success(suggestion)
                }
            }
        } catch (e: UpstreamGeocodingException) {
            Result.failure(e)
        }
    }

    private fun fetchFromNominatim(query: String, limit: Int): List<GeocodeSuggestion> {
        return try {
            fetchFromNominatimOrThrow(query, limit)
        } catch (e: UpstreamGeocodingException) {
            LOG.error("Failed to fetch geocoding suggestions from Nominatim", e)
            emptyList()
        }
    }

    private fun fetchFromNominatimOrThrow(query: String, limit: Int): List<GeocodeSuggestion> {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val url = "$NOMINATIM_SEARCH_URL?q=$encodedQuery&format=jsonv2&limit=$limit&addressdetails=0"

        val connection: HttpURLConnection
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
        } catch (e: Exception) {
            throw UpstreamGeocodingException("Cannot connect to Nominatim", e)
        }

        if (connection.responseCode != 200) {
            LOG.warn("Nominatim returned status {}", connection.responseCode)
            throw UpstreamGeocodingException("Nominatim returned HTTP ${connection.responseCode}")
        }

        return try {
            val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
            val type = object : TypeToken<List<NominatimResult>>() {}.type
            val results: List<NominatimResult> = gson.fromJson(body, type) ?: emptyList()

            results.mapNotNull { result ->
                val lat = result.lat.toDoubleOrNull() ?: return@mapNotNull null
                val lon = result.lon.toDoubleOrNull() ?: return@mapNotNull null
                if (!lat.isFinite() || !lon.isFinite()) return@mapNotNull null
                GeocodeSuggestion(label = result.display_name, lat = lat, lon = lon)
            }
        } catch (e: UpstreamGeocodingException) {
            throw e
        } catch (e: Exception) {
            throw UpstreamGeocodingException("Failed to parse Nominatim response", e)
        }
    }
}
