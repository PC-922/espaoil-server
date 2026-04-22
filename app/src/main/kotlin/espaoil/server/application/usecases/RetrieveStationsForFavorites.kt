package espaoil.server.application.usecases

import espaoil.server.application.ports.GasStationPersister
import espaoil.server.domain.valueobject.Coordinates
import espaoil.server.domain.valueobject.GasStation

private const val SEARCH_RADIUS_METERS = 500

data class FavoriteQuery(
    val id: String,
    val lat: Double,
    val lon: Double,
)

data class FavoriteResult(
    val id: String,
    val station: GasStation?,
    val origin: Coordinates,
)

class RetrieveStationsForFavorites(
    private val gasStationPersister: GasStationPersister,
) {
    fun execute(favorites: List<FavoriteQuery>, gasType: String): List<FavoriteResult> {
        return favorites.map { favorite ->
            val origin = Coordinates(favorite.lat, favorite.lon)
            val bounding = origin.calculateMaximumCoordinates(SEARCH_RADIUS_METERS)

            val nearby = gasStationPersister.queryNearGasStations(bounding, gasType)
                .getOrNull()
                .orEmpty()

            // Pick the closest station by exact Haversine distance
            val closest = nearby.minByOrNull { station ->
                origin.distanceTo(Coordinates(station.latitude(), station.longitude()))
            }

            FavoriteResult(id = favorite.id, station = closest, origin = origin)
        }
    }
}
