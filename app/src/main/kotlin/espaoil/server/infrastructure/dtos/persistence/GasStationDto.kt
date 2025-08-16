package espaoil.server.infrastructure.dtos.persistence

import espaoil.server.domain.valueobject.Coordinates
import espaoil.server.domain.valueobject.GasStation
import espaoil.server.domain.valueobject.Location

data class GasStationDto(
    val postalCode: String,
    val address: String,
    val time: String,
    val name: String,
    val latitude: Double,
    val locality: String,
    val longitude: Double,
    val municipality: String,
    val gasPrices: Map<String, Double>,
) {
    fun toDomain(): GasStation {
        return GasStation(
            name = name,
            location = Location(
                postalCode,
                address,
                time,
                Coordinates(latitude, longitude),
                municipality,
                province = "",
                locality = locality
            ),
            prices = gasPrices
        )
    }


    companion object {
        fun from(gasStation: GasStation) = GasStationDto(
            gasStation.postalCode(),
            gasStation.address(),
            gasStation.time(),
            gasStation.name,
            gasStation.latitude(),
            gasStation.locality(),
            gasStation.longitude(),
            gasStation.municipality(),
            gasStation.prices
        )
    }
}
