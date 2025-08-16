package espaoil.server.infrastructure.dtos.input

import com.google.gson.annotations.SerializedName
import espaoil.server.domain.valueobject.Coordinates
import espaoil.server.domain.valueobject.GasStation
import espaoil.server.domain.valueobject.Location
import espaoil.server.infrastructure.utils.*
import java.util.*

data class GasStationDto(
    @SerializedName("C.P.") val postalCode: String,
    @SerializedName("Dirección") val address: String,
    @SerializedName("Horario") val time: String,
    @SerializedName("Latitud") val latitude: Double,
    @SerializedName("Localidad") val locality: String,
    @SerializedName("Longitud (WGS84)") val longitude: Double,
    @SerializedName("Municipio") val municipality: String,
    @SerializedName("Precio Gasolina 95 E10") val gas95E10Price: Double,
    @SerializedName("Precio Gasolina 95 E5") val gas95E5Price: Double,
    @SerializedName("Precio Gasolina 95 E5 Premium") val gas95E5PremiumPrice: Double,
    @SerializedName("Precio Gasolina 98 E10") val gas98E10Price: Double,
    @SerializedName("Precio Gasolina 98 E5") val gas98E5Price: Double,
    @SerializedName("Provincia") val province: String,
    @SerializedName("Rótulo") val name: String,
    @SerializedName("Precio Gasoleo A") val gasoilA: Double,
    @SerializedName("Precio Gasoleo B") val gasoilB: Double,
    @SerializedName("Precio Gasoleo Premium") val gasoilPremium: Double,
    @SerializedName("Precio Biodiesel") val biodieselPrice: Double = 0.0,
    @SerializedName("Precio Bioetanol") val bioethanolPrice: Double = 0.0,
    @SerializedName("Precio Gas Natural Comprimido") val gasNaturalCompressedPrice: Double = 0.0,
    @SerializedName("Precio Gas Natural Licuado") val gasNaturalLiquefiedPrice: Double = 0.0,
    @SerializedName("Precio Gases Licuados del Petróleo") val liquefiedPetroleumGasesPrice: Double = 0.0,
    @SerializedName("Precio Hidrógeno") val hydrogenPrice: Double = 0.0
) {
    fun toDomain() = GasStation(
        name,
        Location(
            postalCode,
            address,
            time,
            Coordinates(latitude, longitude),
            municipality,
            province,
            normalizeLocality(locality)
        ),
        buildNormalizedPrices()
    )

    private fun buildNormalizedPrices(): Map<String, Double> {
        val raw = mapOf(
            GASOLINA_95_E10 to gas95E10Price,
            GASOLINA_95_E5 to gas95E5Price,
            GASOLINA_95_E5_PREMIUM to gas95E5PremiumPrice,
            GASOLINA_98_E10 to gas98E10Price,
            GASOLINA_98_E5 to gas98E5Price,
            GASOIL_A to gasoilA,
            GASOIL_B to gasoilB,
            GASOIL_PREMIUM to gasoilPremium,
            BIODIESEL to biodieselPrice,
            BIOETANOL to bioethanolPrice,
            GAS_NATURAL_COMPRIMIDO to gasNaturalCompressedPrice,
            GAS_NATURAL_LICUADO to gasNaturalLiquefiedPrice,
            GASES_LICUADOS_DEL_PETROLEO to liquefiedPetroleumGasesPrice,
            HIDROGENO to hydrogenPrice
        )
        return raw.filterValues { v -> !v.isNaN() && v > 0.0 }
    }

    private fun normalizeLocality(value: String): String = value
        .lowercase(Locale.getDefault())
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }
}
