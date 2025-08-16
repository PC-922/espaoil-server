package espaoil.server.infrastructure.adapters

import com.mongodb.client.MongoCollection
import espaoil.server.application.exceptions.FailedToQueryNearGasStations
import espaoil.server.application.exceptions.FailedToReplaceGasStations
import espaoil.server.application.ports.GasStationPersister
import espaoil.server.domain.valueobject.GasStation
import espaoil.server.domain.valueobject.MaximumCoordinates
import espaoil.server.infrastructure.dtos.persistence.GasStationDto
import org.bson.Document
import org.litote.kmongo.deleteMany
import java.util.*

private const val ASCENDANT_ORDER = 1

private const val MAX_GAS_STATIONS_TO_RETRIEVE = 30

class GasStationPersisterMongo(private val collection: MongoCollection<GasStationDto>) : GasStationPersister {

    override fun replace(gasStation: List<GasStation>) = runCatching {
        removeGasStations()
        saveGasStations(gasStation)
    }.onFailure {
        throw FailedToReplaceGasStations(it)
    }

    override fun queryNearGasStations(coordinates: MaximumCoordinates, gasType: String) =
        runCatching {
            val gasPriceFieldPath = "gasPrices.$gasType"
            val query = Document(
                "\$and", Arrays.asList(
                    Document("latitude", Document("\$gt", coordinates.maximumSouthCoordinate)),
                    Document("latitude", Document("\$lt", coordinates.maximumNorthCoordinate)),
                    Document("longitude", Document("\$gt", coordinates.maximumWestCoordinate)),
                    Document("longitude", Document("\$lt", coordinates.maximumEastCoordinate)),
                    Document(gasPriceFieldPath, Document("\$gt", 0.0))
                )
            )

            val gasPriceAscFilter = Document(gasPriceFieldPath, ASCENDANT_ORDER)
            val results = mutableListOf<GasStationDto>()
            collection.find(query)
                .sort(gasPriceAscFilter)
                .limit(MAX_GAS_STATIONS_TO_RETRIEVE)
                .into(results)
            results.map { gasStationDto -> gasStationDto.toDomain() }
        }.onFailure {
            throw FailedToQueryNearGasStations(it)
        }

    private fun removeGasStations() {
        collection.deleteMany("{}")
    }

    private fun saveGasStations(gasStation: List<GasStation>) {
        if (gasStation.isEmpty()) {
            return
        }
        collection.insertMany(gasStation.map { GasStationDto.from(it) })
    }

}
