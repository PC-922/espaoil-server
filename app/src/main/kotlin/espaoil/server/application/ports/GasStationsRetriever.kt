package espaoil.server.application.ports

interface GasStationsRetriever {
    fun apply(): Result<List<espaoil.server.domain.valueobject.GasStation>>
}
