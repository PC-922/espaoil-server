package espaoil.server.application.usecases

import espaoil.server.application.exceptions.FailedToUpdateGasStation
import espaoil.server.application.ports.GasStationPersister
import espaoil.server.application.ports.GasStationsRetriever
import espaoil.server.domain.valueobject.GasStation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UpdateGasStations(
    private val gasStationsRetriever: GasStationsRetriever,
    private val gasStationPersister: GasStationPersister,
) {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(UpdateGasStations::class.java)
    }

    fun execute() = runCatching {
        val gasStations: List<GasStation> = gasStationsRetriever.apply().getOrThrow()

        if (gasStations.isEmpty()) {
            LOG.info("Gas stations were not found")
            return Result.success(Unit)
        }
        gasStationPersister.replace(gasStations).getOrThrow()
        LOG.info("Gas stations were updated in database")
    }.onFailure {
        LOG.error("Error occurred while updating gas stations in database", it)
        throw FailedToUpdateGasStation(it)
    }
}
