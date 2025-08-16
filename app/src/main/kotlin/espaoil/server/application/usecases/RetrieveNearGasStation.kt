package espaoil.server.application.usecases

import espaoil.server.application.exceptions.FailedToRetrieveNearGasStations

class RetrieveNearGasStation(
    private val gasStationRepository: espaoil.server.application.ports.GasStationPersister
) {
    fun execute(coordinates: espaoil.server.domain.valueobject.Coordinates, maximumDistanceInMeters: Int, gasType: String): List<espaoil.server.domain.valueobject.GasStation> {
        return gasStationRepository.queryNearGasStations(
            coordinates.calculateMaximumCoordinates(maximumDistanceInMeters),
            gasType
        ).onFailure { error ->
            throw FailedToRetrieveNearGasStations(error)
        }.getOrThrow()
    }
}
