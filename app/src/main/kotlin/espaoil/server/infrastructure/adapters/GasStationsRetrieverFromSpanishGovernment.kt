package espaoil.server.infrastructure.adapters

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import espaoil.server.application.exceptions.FailedToRetrieveGasStations
import espaoil.server.application.ports.GasStationsRetriever
import espaoil.server.domain.valueobject.GasStation
import espaoil.server.infrastructure.dtos.input.RetrieverResponseDto
import espaoil.server.infrastructure.utils.URLWrapper
import java.io.IOException


private const val GAS_STATIONS_SOURCE =
    "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/"

private const val MAX_RETRIES = 3
private const val RETRY_DELAY_MS = 2_000L

class GasStationsRetrieverFromSpanishGovernment(
    private val url: URLWrapper,
    private val maxRetries: Int = MAX_RETRIES,
    private val retryDelayMs: Long = RETRY_DELAY_MS,
) : GasStationsRetriever {
    override fun apply(): Result<List<GasStation>> {
        var lastError: Throwable? = null
        repeat(maxRetries) { attempt ->
            if (attempt > 0) Thread.sleep(retryDelayMs * (1L shl (attempt - 1)))
            runCatching {
                val gasStationsAsJson = url.get(GAS_STATIONS_SOURCE)
                gasStationsFrom(gasStationsAsJson)
            }.onSuccess { return Result.success(it) }
             .onFailure { lastError = it }
        }
        return Result.failure(FailedToRetrieveGasStations(lastError!!))
    }

    private fun gasStationsFrom(gasStationInfoJson: String): List<GasStation> =
        GsonBuilder()
            .registerTypeAdapter(Double::class.java, DoubleAdapter())
            .create()
            .fromJson(gasStationInfoJson, RetrieverResponseDto::class.java)
            .prices
            .map { it.toDomain() }

    internal class DoubleAdapter : TypeAdapter<Double?>() {
        @Throws(IOException::class)
        override fun write(out: JsonWriter, value: Double?) {
            out.value(value)
        }

        @Throws(IOException::class)
        override fun read(`in`: JsonReader): Double {
            val value = `in`.nextString()
            return if (value.isNotEmpty()) value.replace(",", ".").toDouble() else Double.NaN
        }
    }
}
