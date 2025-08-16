package espaoil.server.domain.fixtures.valueobjects

import espaoil.server.domain.valueobject.Coordinates
import espaoil.server.domain.valueobject.GasStation
import espaoil.server.domain.valueobject.Location
import espaoil.server.infrastructure.utils.GASOIL_A
import espaoil.server.infrastructure.utils.GASOIL_B
import espaoil.server.infrastructure.utils.GASOLINA_95_E5

class GasStationFixtures {
    companion object {
        fun multipleGasStationsWithinAFiveKilometersRadius(): List<GasStation> {
            return listOf(
                GasStation(
                    "GasStation1",
                    Location(
                        "38660",
                        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
                        "L-D: 08:00-17:30",
                        Coordinates(48.045632, -16.737889),
                        "Adeje", "SANTA CRUZ DE TENERIFE", "Adeje"
                    ),
                    mapOf(


                        GASOIL_A to 1.529,


                    )
                ),
                GasStation(
                    "GasStation2",
                    Location(
                        "38661",
                        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
                        "L-D: 08:00-17:30",
                        Coordinates(28.069, -20.7845454),
                        "Adeje", "SANTA CRUZ DE TENERIFE", "Adeje"
                    ),
                    mapOf(


                        GASOIL_A to 1.529,


                    )
                ),
                GasStation(
                    "GasStation3",
                    Location(
                        "38662",
                        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
                        "L-D: 08:00-17:30",
                        Coordinates(28.011861, -16.662639),
                        "Adeje", "SANTA CRUZ DE TENERIFE", "Adeje"
                    ),
                    mapOf(


                        GASOIL_A to 1.529,


                    )
                ),
                GasStation(
                    "GasStation4",
                    Location(
                        "38663",
                        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
                        "L-D: 08:00-17:30",
                        Coordinates(28.053583, -16.714611),
                        "Adeje", "SANTA CRUZ DE TENERIFE", "Adeje"
                    ),
                    mapOf(


                        GASOIL_A to 1.529,


                    )
                ),
                GasStation(
                    "GasStation5",
                    Location(
                        "38663",
                        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
                        "L-D: 08:00-17:30",
                        Coordinates(5.053583, -16.714611),
                        "Adeje", "SANTA CRUZ DE TENERIFE", "Adeje"
                    ),
                    mapOf(


                        GASOIL_A to 1.529,


                    )
                ),
                GasStation(
                    "GasStation6",
                    Location(
                        "38663",
                        "URBANIZACIÓN SAN EUGENIO, PLAYA DE LAS AMERICAS",
                        "L-D: 08:00-17:30",
                        Coordinates(28.069, -1.714611),
                        "Adeje", "SANTA CRUZ DE TENERIFE", "Adeje"
                    ),
                    mapOf(


                        GASOIL_A to 1.529,


                    )
                )
            )
        }

        fun aGasStation(): List<GasStation> = listOf(
            GasStation(
                "CEPSA",
                Location(
                    "02250", "AVENIDA CASTILLA LA MANCHA, 26", "L-D: 07:00-22:00",
                    Coordinates(39.211417,-1.539167 ),
                    "Abengibre",
                    "ALBACETE",
                    "Abengibre",
                ),
                mapOf(
                    GASOLINA_95_E5 to 1.759,

                    GASOIL_A to 1.779,
                    GASOIL_B to 1.270,

                )
            )
        )
    }
}
