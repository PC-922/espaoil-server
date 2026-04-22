package espaoil.server.domain.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoordinatesShould {
    @Test
    fun `distanceTo returns haversine distance in km`() {
        // Madrid Puerta del Sol to Madrid Retiro ~2.9 km
        val sol = Coordinates(40.4168, -3.7038)
        val retiro = Coordinates(40.4153, -3.6844)
        val distance = sol.distanceTo(retiro)
        assertEquals(1.65, distance, 0.05, "Expected ~1.65 km between Sol and Retiro")

        // Same point = 0 km
        assertEquals(0.0, sol.distanceTo(sol), 0.0001)
    }

    @Test
    fun calculateMaximumCoordinates() {
        val coordinates = Coordinates("28.0427319".toDouble(), "-16.7116703".toDouble())
        val maximumDistanceInMeters = 5000

        val maximumCoordinates = coordinates.calculateMaximumCoordinates(maximumDistanceInMeters)

        val expectedMaximumCoordinates = MaximumCoordinates(
            "27.997816135794025".toDouble(),
            "28.087647664205974".toDouble(),
            "-16.762560744378813".toDouble(),
            "-16.66077985562119".toDouble()
        )
        assertEquals(expectedMaximumCoordinates.maximumSouthCoordinate, maximumCoordinates.maximumSouthCoordinate)
        assertEquals(expectedMaximumCoordinates.maximumNorthCoordinate, maximumCoordinates.maximumNorthCoordinate)
        assertEquals(expectedMaximumCoordinates.maximumEastCoordinate, maximumCoordinates.maximumEastCoordinate)
        assertEquals(expectedMaximumCoordinates.maximumWestCoordinate, maximumCoordinates.maximumWestCoordinate)
    }
}
