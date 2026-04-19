package espaoil.server.infrastructure.api

import com.google.gson.JsonParser
import espaoil.server.application.ports.GasStationPersister
import espaoil.server.domain.valueobject.Coordinates
import espaoil.server.domain.valueobject.GasStation
import espaoil.server.domain.valueobject.Location
import espaoil.server.domain.valueobject.MaximumCoordinates
import espaoil.server.infrastructure.utils.GASOLINA_95_E5
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiServerShould {
    private var server: ApiServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
        server = null
    }

    @Test
    fun `serve near gas stations when using short key gasType 95_E5`() {
        val port = findFreePort()
        val fakePersister = FakePersister()
        server = ApiServer(fakePersister, port = port)
        server!!.startAsync()

        waitForHealthy(port)

        val url = URL("http://localhost:$port/gas-stations/near?lat=40.4168&lon=-3.7038&distance=5000&gasType=95_E5&sortBy=price")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3000
            readTimeout = 3000
        }
        val status = conn.responseCode
        val body = conn.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        conn.disconnect()

        assertEquals(200, status)
        val jsonArray = JsonParser.parseString(body).asJsonArray
        assertTrue(jsonArray.size() > 0, "Expected non-empty stations list")
        val firstStation = jsonArray[0].asJsonObject
        // price is now numeric
        assertEquals(1.234, firstStation.get("price").asDouble, 0.0001)
        assertTrue(firstStation.has("distance"), "Response must include distance field")
        assertTrue(firstStation.has("trader"), "Response must include trader field")
        // sortBy=price: cheapest first
        val prices = jsonArray.map { it.asJsonObject.get("price").asDouble }
        assertEquals(prices.sorted(), prices, "Results must be sorted by price ascending")
    }

    @Test
    fun `defaults to price sort when sortBy missing`() {
        val port = findFreePort()
        server = ApiServer(FakePersister(), port = port)
        server!!.startAsync()

        waitForHealthy(port)

        val url = URL("http://localhost:$port/gas-stations/near?lat=40.4168&lon=-3.7038&distance=5000&gasType=95_E5")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3000
            readTimeout = 3000
        }
        val status = conn.responseCode
        val body = conn.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        conn.disconnect()

        assertEquals(200, status)
        val jsonArray = JsonParser.parseString(body).asJsonArray
        val prices = jsonArray.map { it.asJsonObject.get("price").asDouble }
        assertEquals(prices.sorted(), prices, "Default sort must be price ascending")
    }

    @Test
    fun `sort by distance when sortBy=distance`() {
        val port = findFreePort()
        server = ApiServer(FakePersister(), port = port)
        server!!.startAsync()

        waitForHealthy(port)

        val url = URL("http://localhost:$port/gas-stations/near?lat=40.4168&lon=-3.7038&distance=5000&gasType=95_E5&sortBy=distance")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3000
            readTimeout = 3000
        }
        val status = conn.responseCode
        val body = conn.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        conn.disconnect()

        assertEquals(200, status)
        val jsonArray = JsonParser.parseString(body).asJsonArray
        val distances = jsonArray.map { it.asJsonObject.get("distance").asDouble }
        assertEquals(distances.sorted(), distances, "Results must be sorted by distance ascending")
    }

    private fun waitForHealthy(port: Int, attempts: Int = 10, sleepMs: Long = 100L) {
        repeat(attempts) {
            try {
                val url = URL("http://localhost:$port/health")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 300
                    readTimeout = 300
                }
                if (conn.responseCode == 200) {
                    conn.disconnect()
                    return
                }
                conn.disconnect()
            } catch (_: Exception) {
                // ignore and retry
            }
            Thread.sleep(sleepMs)
        }
    }

    private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }
}

internal class FakePersister : GasStationPersister {
    override fun replace(gasStation: List<GasStation>) = Result.success(Unit)

    override fun queryNearGasStations(coordinates: MaximumCoordinates, gasType: String): Result<List<GasStation>> {
        val s1 = GasStation(
            name = "Station A",
            location = Location(
                postalCode = "28001",
                address = "Calle A",
                time = "24H",
                coordinates = Coordinates(40.0, -3.7),
                municipality = "Madrid",
                province = "Madrid",
                locality = "Madrid"
            ),
            prices = mapOf(
                GASOLINA_95_E5 to 1.234,
            )
        )
        val s2 = GasStation(
            name = "Station B",
            location = Location(
                postalCode = "28002",
                address = "Calle B",
                time = "24H",
                coordinates = Coordinates(40.1, -3.6),
                municipality = "Madrid",
                province = "Madrid",
                locality = "Madrid"
            ),
            prices = mapOf(
                GASOLINA_95_E5 to 1.567,
            )
        )
        return Result.success(listOf(s1, s2))
    }
}
