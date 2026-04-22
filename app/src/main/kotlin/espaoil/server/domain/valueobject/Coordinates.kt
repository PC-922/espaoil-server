package espaoil.server.domain.valueobject

import java.lang.Math.PI
import java.lang.Math.cos
import kotlin.math.*

private const val EARTH_RADIUS_KM = 6371.0

data class Coordinates(val latitude: Double, val longitude: Double) {

    fun distanceTo(other: Coordinates): Double {
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return EARTH_RADIUS_KM * 2 * asin(sqrt(a))
    }

    fun calculateMaximumCoordinates(maximumDistanceInMeters: Int): MaximumCoordinates {
        val earth = 6378.137
        val m = (1 / ((2 * PI / 360) * earth)) / 1000
        val maximumNorthCoordinate = latitude + (maximumDistanceInMeters * m)
        val maximumSouthCoordinate = latitude + (-maximumDistanceInMeters * m)
        val maximumEastCoordinate = longitude + (maximumDistanceInMeters * m) / cos(latitude * (PI / 180))
        val maximumWestCoordinate = longitude + (-maximumDistanceInMeters * m) / cos(latitude * (PI / 180))
        return MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
    }

}
