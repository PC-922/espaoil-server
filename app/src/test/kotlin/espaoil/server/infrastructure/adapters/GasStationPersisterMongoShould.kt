package espaoil.server.infrastructure.adapters

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import espaoil.server.application.exceptions.FailedToQueryNearGasStations
import espaoil.server.application.exceptions.FailedToReplaceGasStations
import espaoil.server.domain.fixtures.valueobjects.GasStationFixtures.Companion.aGasStation
import espaoil.server.domain.fixtures.valueobjects.GasStationFixtures.Companion.multipleGasStationsWithinAFiveKilometersRadius
import espaoil.server.domain.valueobject.GasStation
import espaoil.server.domain.valueobject.MaximumCoordinates
import espaoil.server.infrastructure.dtos.persistence.GasStationDto
import espaoil.server.infrastructure.utils.*
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.litote.kmongo.KMongo
import org.litote.kmongo.deleteMany
import org.litote.kmongo.getCollectionOfName
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockKExtension::class)
class GasStationPersisterMongoShould() {
    private val mongoDBContainer: MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:5.0.0"))
        .withExposedPorts(27017)

    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase
    private lateinit var collection: MongoCollection<GasStationDto>
    private lateinit var gasStationPersisterMongo: GasStationPersisterMongo


    @BeforeEach
    fun setUp() {
        mongoDBContainer.start()
        client = KMongo.createClient(mongoDBContainer.replicaSetUrl)
        database = client.getDatabase("espaoil")
        collection = database.getCollectionOfName("gas_stations")
        collection.insertMany(gasStationDtos())
        gasStationPersisterMongo = GasStationPersisterMongo(collection)
    }

    @Test
    fun searchForNearestGasStationOrderedByGas95E5Asc() {
        val maximumSouthCoordinate = "4.997816135794025".toDouble()
        val maximumNorthCoordinate = "50.087647664205974".toDouble()
        val maximumWestCoordinate = "-18.762560744378813".toDouble()
        val maximumEastCoordinate = "-1.56077985562119".toDouble()
        val coordinates = MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
        val gasStations =
            gasStationPersisterMongo
                .queryNearGasStations(coordinates, GASOLINA_95_E5)
                .getOrThrow()
        assertEquals(5, gasStations.size)
        assertEquals("GasStation6", gasStations[0].name)
        assertEquals("GasStation4", gasStations[1].name)
        assertEquals("GasStation1", gasStations[2].name)
        assertEquals("GasStation3", gasStations[3].name)
        assertEquals("GasStation5", gasStations[4].name)

    }

    @Test
    fun `Search for nearest gas station ordered by gas95E10 ascendant`() {
        val maximumSouthCoordinate = "4.997816135794025".toDouble()
        val maximumNorthCoordinate = "50.087647664205974".toDouble()
        val maximumWestCoordinate = "-18.762560744378813".toDouble()
        val maximumEastCoordinate = "-1.56077985562119".toDouble()
        val coordinates = MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
        val gasStations =
            gasStationPersisterMongo
                .queryNearGasStations(coordinates, GASOLINA_95_E10)
                .getOrThrow()
        assertEquals(3, gasStations.size)
        assertEquals("GasStation4", gasStations[0].name)
        assertEquals("GasStation6", gasStations[1].name)
        assertEquals("GasStation1", gasStations[2].name)
    }

    @Test
    fun `Search for nearest gas station ordered by gas95Premium ascendant`() {
        val maximumSouthCoordinate = "4.997816135794025".toDouble()
        val maximumNorthCoordinate = "50.087647664205974".toDouble()
        val maximumWestCoordinate = "-18.762560744378813".toDouble()
        val maximumEastCoordinate = "-1.56077985562119".toDouble()
        val coordinates = MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
        val gasStations =
            gasStationPersisterMongo
                .queryNearGasStations(coordinates, GASOLINA_95_E5_PREMIUM)
                .getOrThrow()
        assertEquals(3, gasStations.size)
        assertEquals("GasStation1", gasStations[0].name)
        assertEquals("GasStation4", gasStations[1].name)
        assertEquals("GasStation6", gasStations[2].name)
    }

    @Test
    fun `Search for nearest gas station ordered by gas98E5 ascendant`() {
        val maximumSouthCoordinate = "4.997816135794025".toDouble()
        val maximumNorthCoordinate = "50.087647664205974".toDouble()
        val maximumWestCoordinate = "-18.762560744378813".toDouble()
        val maximumEastCoordinate = "-1.56077985562119".toDouble()
        val coordinates = MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
        val gasStations =
            gasStationPersisterMongo
                .queryNearGasStations(coordinates, GASOLINA_98_E5)
                .getOrThrow()
        assertEquals(3, gasStations.size)
        assertEquals("GasStation1", gasStations[0].name)
        assertEquals("GasStation6", gasStations[1].name)
        assertEquals("GasStation4", gasStations[2].name)
    }

    @Test
    fun `Search for nearest gas station ordered by gas98E10 ascendant`() {
        val maximumSouthCoordinate = "4.997816135794025".toDouble()
        val maximumNorthCoordinate = "50.087647664205974".toDouble()
        val maximumWestCoordinate = "-18.762560744378813".toDouble()
        val maximumEastCoordinate = "-1.56077985562119".toDouble()
        val coordinates = MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
        val gasStations =
            gasStationPersisterMongo
                .queryNearGasStations(coordinates, GASOLINA_98_E10)
                .getOrThrow()
        assertEquals(3, gasStations.size)
        assertEquals("GasStation4", gasStations[0].name)
        assertEquals("GasStation1", gasStations[1].name)
        assertEquals("GasStation6", gasStations[2].name)
    }

    @Test
    fun `Search for nearest gas station ordered by gasoilA ascendant`() {
        val maximumSouthCoordinate = "4.997816135794025".toDouble()
        val maximumNorthCoordinate = "50.087647664205974".toDouble()
        val maximumWestCoordinate = "-18.762560744378813".toDouble()
        val maximumEastCoordinate = "-1.56077985562119".toDouble()
        val coordinates = MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
        val gasStations =
            gasStationPersisterMongo
                .queryNearGasStations(coordinates, GASOIL_A)
                .getOrThrow()
        assertEquals(3, gasStations.size)
        assertEquals("GasStation6", gasStations[0].name)
        assertEquals("GasStation1", gasStations[1].name)
        assertEquals("GasStation4", gasStations[2].name)
    }

    @Test
    fun `Search for nearest gas station ordered by gasoilB ascendant`() {
        val maximumSouthCoordinate = "4.997816135794025".toDouble()
        val maximumNorthCoordinate = "50.087647664205974".toDouble()
        val maximumWestCoordinate = "-18.762560744378813".toDouble()
        val maximumEastCoordinate = "-1.56077985562119".toDouble()
        val coordinates = MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
        val gasStations =
            gasStationPersisterMongo
                .queryNearGasStations(coordinates, GASOIL_B)
                .getOrThrow()
        assertEquals(2, gasStations.size)
        assertEquals("GasStation1", gasStations[0].name)
        assertEquals("GasStation6", gasStations[1].name)
    }

    @Test
    fun `Search for nearest gas station ordered by gasoilPremium ascendant`() {
        val maximumSouthCoordinate = "4.997816135794025".toDouble()
        val maximumNorthCoordinate = "50.087647664205974".toDouble()
        val maximumWestCoordinate = "-18.762560744378813".toDouble()
        val maximumEastCoordinate = "-1.56077985562119".toDouble()
        val coordinates = MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
        val gasStations =
            gasStationPersisterMongo
                .queryNearGasStations(coordinates, GASOIL_PREMIUM)
                .getOrThrow()
        assertEquals(2, gasStations.size)
        assertEquals("GasStation6", gasStations[0].name)
        assertEquals("GasStation1", gasStations[1].name)
    }


    @Test
    fun `raise an error if something fails removing gas stations`() {
        collection = mockk()
        gasStationPersisterMongo = GasStationPersisterMongo(collection)
        every { collection.deleteMany("{}") } throws RuntimeException()

        assertFailsWith<FailedToReplaceGasStations> {
            gasStationPersisterMongo
                .replace(multipleGasStationsWithinAFiveKilometersRadius())
                .getOrThrow()
        }
    }

    @Test
    fun `raise an error if something fails saving gas stations`() {
        collection = mockk()
        gasStationPersisterMongo = GasStationPersisterMongo(collection)
        val gasStations = multipleGasStationsWithinAFiveKilometersRadius()
        val gasStationsDto = gasStations.map { GasStationDto.from(it) }
        every { collection.insertMany(gasStationsDto) } throws RuntimeException()

        assertFailsWith<FailedToReplaceGasStations> {
            gasStationPersisterMongo
                .replace(gasStations)
                .getOrThrow()
        }
    }

    @Test
    fun `raise an error if something fails querying near gas stations`() {
        collection = mockk()
        gasStationPersisterMongo = GasStationPersisterMongo(collection)
        val maximumSouthCoordinate = "4.997816135794025".toDouble()
        val maximumNorthCoordinate = "50.087647664205974".toDouble()
        val maximumWestCoordinate = "-18.762560744378813".toDouble()
        val maximumEastCoordinate = "-1.56077985562119".toDouble()
        val coordinates = MaximumCoordinates(
            maximumSouthCoordinate,
            maximumNorthCoordinate,
            maximumWestCoordinate,
            maximumEastCoordinate
        )
        val gasType = GASOLINA_98_E5
        val gasPriceFieldPath = "gasPrices.$gasType"
        val expectedQuery = Document(
            "\$and", Arrays.asList(
                Document("latitude", Document("\$gt", maximumSouthCoordinate)),
                Document("latitude", Document("\$lt", maximumNorthCoordinate)),
                Document("longitude", Document("\$gt", maximumWestCoordinate)),
                Document("longitude", Document("\$lt", maximumEastCoordinate)),
                Document(gasPriceFieldPath, Document("\$gt", 0.0)),
            )
        )
        every { collection.find(expectedQuery) } throws RuntimeException()

        assertFailsWith<FailedToQueryNearGasStations> {
            gasStationPersisterMongo
                .queryNearGasStations(coordinates, gasType)
        }
    }

    @Test
    fun `Replace all gas stations with new ones`() {
        val gasStation = aGasStation()
        gasStationPersisterMongo
            .replace(gasStation)

        val replacedResult = mutableListOf<GasStationDto>()
        collection.find().into(replacedResult)
        assertEquals(1, replacedResult.size)
        assertEquals(gasStation[0].name, replacedResult[0].name)
    }

    @Test
    fun `Replace all gas stations with no one given an empty list of gas stations`() {
        val gasStation = listOf<GasStation>()
        gasStationPersisterMongo
            .replace(gasStation)

        val replacedResult = mutableListOf<GasStationDto>()
        collection.find().into(replacedResult)
        assertEquals(0, replacedResult.size)
    }
}

private fun gasStationDtos(): List<GasStationDto> = listOf(
    GasStationDto(
        "38660",
        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
        "L-D: 08:00-17:30",
        "GasStation1",
        48.045632,
        "SANTA CRUZ DE TENERIFE",
        -16.737889,
        "Adeje",
        mapOf(
            GASOLINA_95_E10 to 1.30,
            GASOLINA_95_E5 to 1.538,
            GASOLINA_95_E5_PREMIUM to 1.10,
            GASOLINA_98_E10 to 1.20,
            GASOLINA_98_E5 to 1.10,
            GASOIL_A to 1.20,
            GASOIL_B to 1.10,
            GASOIL_PREMIUM to 1.20
        )
    ),
    GasStationDto(
        "38660",
        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
        "L-D: 08:00-17:30",
        "GasStation2",
        28.069,
        "SANTA CRUZ DE TENERIFE",
        -20.7845454,
        "Adeje",
        mapOf(
            GASOLINA_95_E10 to Double.NaN,
            GASOLINA_95_E5 to 1.648,
            GASOLINA_95_E5_PREMIUM to Double.NaN,
            GASOLINA_98_E10 to Double.NaN,
            GASOLINA_98_E5 to Double.NaN,
            GASOIL_A to Double.NaN,
            GASOIL_B to Double.NaN,
            GASOIL_PREMIUM to Double.NaN
        )
    ), GasStationDto(
        "38660",
        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
        "L-D: 08:00-17:30",
        "GasStation3",
        28.011861,
        "SANTA CRUZ DE TENERIFE",
        -16.662639,
        "Adeje",
        mapOf(
            GASOLINA_95_E10 to Double.NaN,
            GASOLINA_95_E5 to 1.648,
            GASOLINA_95_E5_PREMIUM to Double.NaN,
            GASOLINA_98_E10 to Double.NaN,
            GASOLINA_98_E5 to Double.NaN,
            GASOIL_A to Double.NaN,
            GASOIL_B to Double.NaN,
            GASOIL_PREMIUM to Double.NaN
        )
    ), GasStationDto(
        "38660",
        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
        "L-D: 08:00-17:30",
        "GasStation4",
        28.053583,
        "SANTA CRUZ DE TENERIFE",
        -16.714611,
        "Adeje",
        mapOf(
            GASOLINA_95_E10 to 1.10,
            GASOLINA_95_E5 to 1.238,
            GASOLINA_95_E5_PREMIUM to 1.20,
            GASOLINA_98_E10 to 1.10,
            GASOLINA_98_E5 to 1.30,
            GASOIL_A to 1.30,
            GASOIL_B to Double.NaN,
            GASOIL_PREMIUM to Double.NaN
        )
    ), GasStationDto(
        "38660",
        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
        "L-D: 08:00-17:30",
        "GasStation5",
        5.053583,
        "SANTA CRUZ DE TENERIFE",
        -16.714611,
        "Adeje",
        mapOf(
            GASOLINA_95_E10 to Double.NaN,
            GASOLINA_95_E5 to 1.938,
            GASOLINA_95_E5_PREMIUM to Double.NaN,
            GASOLINA_98_E10 to Double.NaN,
            GASOLINA_98_E5 to Double.NaN,
            GASOIL_A to Double.NaN,
            GASOIL_B to Double.NaN,
            GASOIL_PREMIUM to Double.NaN
        )
    ), GasStationDto(
        "38660",
        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
        "L-D: 08:00-17:30",
        "GasStation6",
        28.069,
        "SANTA CRUZ DE TENERIFE",
        -1.714611,
        "Adeje",
        mapOf(
            GASOLINA_95_E10 to 1.20,
            GASOLINA_95_E5 to 1.138,
            GASOLINA_95_E5_PREMIUM to 1.30,
            GASOLINA_98_E10 to 1.30,
            GASOLINA_98_E5 to 1.20,
            GASOIL_A to 1.10,
            GASOIL_B to 1.20,
            GASOIL_PREMIUM to 1.10
        )
    )
)
