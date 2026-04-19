package espaoil.server.infrastructure.dtos.output

import espaoil.server.domain.valueobject.Coordinates
import espaoil.server.domain.valueobject.GasStation
import java.util.*
import java.util.Locale.getDefault
import java.util.regex.Pattern

private const val ARTICLES_REGEX = "(.*)\\s?\\((OS|A|OS|A|O|LAS|AS|LA|LES|LOS|S'|EL|L'|ELS|SES|ES|SA)\\)(.*)?|(.*)"

data class NearGasStationDto(
    val trader: String,
    val name: String,
    val town: String,
    val municipality: String,
    val schedule: String,
    val price: Double,
    val latitude: Double,
    val longitude: Double,
    val distance: Double,
) {
    companion object {
        fun from(gasStation: GasStation, gasType: String, userCoordinates: Coordinates): NearGasStationDto {
            val stationCoords = Coordinates(gasStation.latitude(), gasStation.longitude())
            return NearGasStationDto(
                trader = gasStation.name,
                name = gasStation.name,
                town = formattedLocality(gasStation.locality()),
                municipality = gasStation.municipality(),
                schedule = gasStation.time(),
                price = gasStation.prices.getValue(gasType),
                latitude = gasStation.latitude(),
                longitude = gasStation.longitude(),
                distance = roundKm(userCoordinates.distanceTo(stationCoords))
            )
        }

        private fun roundKm(km: Double): Double = Math.round(km * 1000) / 1000.0

        internal fun formattedLocality(locality: String): String {
            return capitalize(
                Pattern.compile(ARTICLES_REGEX)
                    .matcher(locality)
                    .replaceAll("$4$2 $1$3")
                    .trim()
                    .lowercase(getDefault())
                    .removeExtraSpaces()
            )
        }

        private fun capitalize(text: String): String {
            val capitalizedText = StringBuffer()
            val matcher = Pattern.compile("\\b(\\w)").matcher(text)
            while (matcher.find()) matcher.appendReplacement(capitalizedText, matcher.group(1).uppercase(getDefault()))
            matcher.appendTail(capitalizedText)
            return capitalizedText.toString()
        }

        private fun CharSequence.removeExtraSpaces(): String {
            return replace("\\s+".toRegex(), " ")
        }
    }
}
