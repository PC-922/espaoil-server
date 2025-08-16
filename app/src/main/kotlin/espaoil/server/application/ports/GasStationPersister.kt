package espaoil.server.application.ports

interface GasStationPersister {
    fun replace(gasStation: List<espaoil.server.domain.valueobject.GasStation>): Result<Unit>
    fun queryNearGasStations(coordinates: espaoil.server.domain.valueobject.MaximumCoordinates, gasType: String): Result<List<espaoil.server.domain.valueobject.GasStation>>
}
