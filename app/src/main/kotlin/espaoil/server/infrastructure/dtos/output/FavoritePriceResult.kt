package espaoil.server.infrastructure.dtos.output

data class FavoritePriceResult(
    val id: String,
    val station: NearGasStationDto?,
)
