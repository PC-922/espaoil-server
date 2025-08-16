package espaoil.server.infrastructure.dtos.output

import espaoil.server.domain.valueobject.GasStation
import java.util.*
import java.util.Locale.getDefault
import java.util.regex.Pattern

private const val ARTICLES_REGEX = "(.*)\\s?\\((OS|A|OS|A|O|LAS|AS|LA|LES|LOS|S'|EL|L'|ELS|SES|ES|SA)\\)(.*)?|(.*)"

data class NearGasStationDto(
    val name: String,
    val town: String,
    val municipality: String,
    val schedule: String,
    val price: String,
    val latitude: String,
    val longitude: String,
) {
    companion object {
        fun from(gasStation: GasStation, gasType: String): NearGasStationDto {
            return NearGasStationDto(
                name = gasStation.name,
                town = formattedLocality(gasStation.locality()),
                municipality = gasStation.municipality(),
                schedule = gasStation.time(),
                price = formatDecimal(gasStation.prices.getValue(gasType)),
                latitude = formatDecimal(gasStation.latitude()),
                longitude = formatDecimal(gasStation.longitude())
            )
        }

        private fun formatDecimal(value: Double): String {
            return String.format(Locale.US, "%.3f", value)
        }

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
