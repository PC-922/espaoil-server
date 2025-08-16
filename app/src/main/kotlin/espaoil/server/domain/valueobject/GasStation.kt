package espaoil.server.domain.valueobject

data class GasStation(
    val name: String,
    val location: Location,
    val prices: Map<String, Double>
) {
    fun latitude() = location.latitude()

    fun longitude() = location.longitude()

    fun postalCode() = location.postalCode

    fun address() = location.address

    fun time() = location.time

    fun locality() = location.locality

    fun municipality() = location.municipality

}
