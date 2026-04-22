package espaoil.server.infrastructure.api

import com.google.gson.JsonParser
import espaoil.server.infrastructure.adapters.GeocodeSuggestion
import espaoil.server.infrastructure.adapters.NominatimGeocoder
import espaoil.server.infrastructure.adapters.UpstreamGeocodingException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeocodingEndpointsShould {

    private var server: ApiServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
        server = null
    }

    // --- /geocoding/search ---

    @Test
    fun `search returns suggestions from geocoder`() {
        val port = findFreePort()
        val fakeGeocoder = StubGeocoder(
            searchResult = listOf(
                GeocodeSuggestion("Gran Vía, Madrid", 40.42, -3.705)
            )
        )
        server = ApiServer(FakePersister(), geocoder = fakeGeocoder, port = port)
        server!!.startAsync()
        waitForHealthy(port)

        val conn = get(port, "/geocoding/search?q=Gran+Via+Madrid")
        val status = conn.responseCode
        val body = conn.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        conn.disconnect()

        assertEquals(200, status)
        val arr = JsonParser.parseString(body).asJsonArray
        assertEquals(1, arr.size())
        val first = arr[0].asJsonObject
        assertEquals("Gran Vía, Madrid", first.get("label").asString)
        assertEquals(40.42, first.get("lat").asDouble, 0.0001)
        assertEquals(-3.705, first.get("lon").asDouble, 0.0001)
    }

    @Test
    fun `search returns empty array when geocoder returns nothing`() {
        val port = findFreePort()
        server = ApiServer(FakePersister(), geocoder = StubGeocoder(searchResult = emptyList()), port = port)
        server!!.startAsync()
        waitForHealthy(port)

        val conn = get(port, "/geocoding/search?q=zzz")
        val status = conn.responseCode
        val body = conn.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        conn.disconnect()

        assertEquals(200, status)
        assertTrue(JsonParser.parseString(body).asJsonArray.isEmpty)
    }

    @Test
    fun `search returns 400 when q is too short`() {
        val port = findFreePort()
        server = ApiServer(FakePersister(), port = port)
        server!!.startAsync()
        waitForHealthy(port)

        val conn = get(port, "/geocoding/search?q=ab")
        assertEquals(400, conn.responseCode)
        conn.disconnect()
    }

    @Test
    fun `search returns 400 when q is missing`() {
        val port = findFreePort()
        server = ApiServer(FakePersister(), port = port)
        server!!.startAsync()
        waitForHealthy(port)

        val conn = get(port, "/geocoding/search")
        assertEquals(400, conn.responseCode)
        conn.disconnect()
    }

    // --- /geocoding/resolve ---

    @Test
    fun `resolve returns lat and lon on success`() {
        val port = findFreePort()
        val fakeGeocoder = StubGeocoder(
            resolveResult = Result.success(GeocodeSuggestion("Gran Vía, Madrid", 40.42, -3.705))
        )
        server = ApiServer(FakePersister(), geocoder = fakeGeocoder, port = port)
        server!!.startAsync()
        waitForHealthy(port)

        val conn = get(port, "/geocoding/resolve?q=Gran+Via+Madrid")
        val status = conn.responseCode
        val body = conn.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        conn.disconnect()

        assertEquals(200, status)
        val obj = JsonParser.parseString(body).asJsonObject
        assertEquals(40.42, obj.get("lat").asDouble, 0.0001)
        assertEquals(-3.705, obj.get("lon").asDouble, 0.0001)
    }

    @Test
    fun `resolve returns 404 when address not found`() {
        val port = findFreePort()
        val fakeGeocoder = StubGeocoder(
            resolveResult = Result.failure(NoSuchElementException("Address not found"))
        )
        server = ApiServer(FakePersister(), geocoder = fakeGeocoder, port = port)
        server!!.startAsync()
        waitForHealthy(port)

        val conn = get(port, "/geocoding/resolve?q=nonexistent+address+xyz")
        assertEquals(404, errorCode(conn))
        conn.disconnect()
    }

    @Test
    fun `resolve returns 502 on upstream failure`() {
        val port = findFreePort()
        val fakeGeocoder = StubGeocoder(
            resolveResult = Result.failure(UpstreamGeocodingException("Nominatim down"))
        )
        server = ApiServer(FakePersister(), geocoder = fakeGeocoder, port = port)
        server!!.startAsync()
        waitForHealthy(port)

        val conn = get(port, "/geocoding/resolve?q=Gran+Via")
        assertEquals(502, errorCode(conn))
        conn.disconnect()
    }

    @Test
    fun `resolve returns 422 on invalid coordinates`() {
        val port = findFreePort()
        val fakeGeocoder = StubGeocoder(
            resolveResult = Result.failure(IllegalStateException("Invalid coordinates in geocoding result"))
        )
        server = ApiServer(FakePersister(), geocoder = fakeGeocoder, port = port)
        server!!.startAsync()
        waitForHealthy(port)

        val conn = get(port, "/geocoding/resolve?q=bad+place")
        assertEquals(422, errorCode(conn))
        conn.disconnect()
    }

    @Test
    fun `resolve returns 400 when q is missing`() {
        val port = findFreePort()
        server = ApiServer(FakePersister(), port = port)
        server!!.startAsync()
        waitForHealthy(port)

        val conn = get(port, "/geocoding/resolve")
        assertEquals(400, conn.responseCode)
        conn.disconnect()
    }

    // --- Helpers ---

    private fun get(port: Int, path: String): HttpURLConnection {
        val url = URL("http://localhost:$port$path")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3000
            readTimeout = 3000
        }
    }

    /** HttpURLConnection throws on non-2xx for getResponseCode() sometimes — read from error stream */
    private fun errorCode(conn: HttpURLConnection): Int {
        return try { conn.responseCode } catch (_: Exception) { conn.responseCode }
    }

    private fun waitForHealthy(port: Int, attempts: Int = 10, sleepMs: Long = 100L) {
        repeat(attempts) {
            try {
                val conn = get(port, "/health")
                if (conn.responseCode == 200) { conn.disconnect(); return }
                conn.disconnect()
            } catch (_: Exception) {}
            Thread.sleep(sleepMs)
        }
    }

    private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }
}

class StubGeocoder(
    private val searchResult: List<GeocodeSuggestion> = emptyList(),
    private val resolveResult: Result<GeocodeSuggestion> = Result.failure(NoSuchElementException("not found"))
) : NominatimGeocoder() {
    override fun search(query: String, limit: Int): List<GeocodeSuggestion> = searchResult
    override fun resolve(query: String): Result<GeocodeSuggestion> = resolveResult
}
