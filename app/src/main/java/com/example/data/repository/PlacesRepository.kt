package com.example.data.repository

import com.example.R
import com.example.data.model.Category
import com.example.data.model.EvidenceLevel
import com.example.data.model.MysteryPlace
import com.example.util.TextNormalizer

/**
 * Repositorio local de lugares misteriosos del mundo.
 * Ofrece el catálogo completo verificado y métodos de filtrado y búsqueda.
 */
class PlacesRepository {

    private val places: List<MysteryPlace> = listOf(
        MysteryPlace(
            id = "rano_raraku",
            name = "Moai de Rano Raraku",
            country = "Chile (Isla de Pascua / Rapa Nui)",
            latitude = -27.1258,
            longitude = -109.2869,
            category = Category.MEGALITOS,
            whatWeKnow = "Volcán de tobas volcánicas que sirvió de cantera matriz para la inmensa mayoría de los moai de Rapa Nui. Allí aún descansan casi 400 estatuas en distintas etapas de talla. Es Patrimonio de la Humanidad UNESCO desde 1995.",
            theMystery = "¿Cómo se transportaron colosos de hasta 80 toneladas por senderos irregulares a través de la isla, y qué colapso o motivo obligó a abandonar los talleres de repente?",
            evidence = EvidenceLevel.CONSENSO,
            keywords = listOf("moai", "rapa nui", "isla de pascua", "chile", "cantera", "estatuas", "volcan", "polinesia"),
            imageRes = R.drawable.img_moai_pascua
        ),
        MysteryPlace(
            id = "tongariki",
            name = "Ahu Tongariki",
            country = "Chile (Isla de Pascua / Rapa Nui)",
            latitude = -27.1258,
            longitude = -109.2770,
            category = Category.MEGALITOS,
            whatWeKnow = "Es la mayor plataforma ceremonial de toda la Polinesia, con 15 moai alineados de espaldas al océano Pacífico. Tras ser arrasada por un tsunami en 1960, fue minuciosamente restaurada en la década de 1990.",
            theMystery = "El trasfondo astronómico de su orientación hacia el solsticio de verano austral y las razones rituales exactas por las que los ancestros debían vigilar permanentemente la aldea interior.",
            evidence = EvidenceLevel.CONSENSO,
            keywords = listOf("tongariki", "ahu", "moai", "astronomia", "solsticio", "tsunami", "rapa nui", "chile"),
            imageRes = R.drawable.img_moai_pascua
        ),
        MysteryPlace(
            id = "stonehenge",
            name = "Stonehenge",
            country = "Reino Unido",
            latitude = 51.1789,
            longitude = -1.8262,
            category = Category.MEGALITOS,
            whatWeKnow = "Monumento neolítico erigido en la llanura de Salisbury en múltiples fases entre el 3000 y el 1500 a. C. Los grandes sarsens proceden de unos 25 km, mientras que las piedras azules se originaron en las colinas de Preseli (Gales), a 240 km.",
            theMystery = "¿Cómo transportaron las piedras azules a través de ríos y colinas con tecnología de la Edad de Piedra, y cuál fue su función primaria: observatorio astronómico solar, calendario de curación o necrópolis dinástica?",
            evidence = EvidenceLevel.EN_DEBATE,
            keywords = listOf("stonehenge", "salisbury", "megalito", "inglaterra", "reino unido", "solsticio", "druidas", "neolitico"),
            imageRes = R.drawable.img_stonehenge
        ),
        MysteryPlace(
            id = "gobekli_tepe",
            name = "Göbekli Tepe",
            country = "Turquía",
            latitude = 37.2233,
            longitude = 38.9225,
            category = Category.MEGALITOS,
            whatWeKnow = "Complejo ceremonial megalítico con gigantescos pilares en forma de T tallados con relieves de fauna salvaje. Data de hace más de 11.500 años (aprox. 9500 a. C.), siendo anterior a la agricultura organizada y a la cerámica.",
            theMystery = "¿Cómo bandas de cazadores-recolectores coordinaron semejante esfuerzo arquitectónico monumental y por qué el complejo entero fue enterrado intencionalmente bajo miles de toneladas de tierra?",
            evidence = EvidenceLevel.EN_DEBATE,
            keywords = listOf("gobekli tepe", "turquia", "anatolia", "neolitico preceramico", "santuario", "pilares", "templo antiguo"),
            imageRes = R.drawable.gobekli_tepe
        ),
        MysteryPlace(
            id = "carnac",
            name = "Alineamientos de Carnac",
            country = "Francia",
            latitude = 47.5842,
            longitude = -3.0786,
            category = Category.MEGALITOS,
            whatWeKnow = "El yacimiento megalítico más extenso del mundo, con más de 3.000 menhires erigidos en hileras paralelas a lo largo de 4 kilómetros en la costa bretona. Su antigüedad se remonta a entre 4500 y 3300 a. C.",
            theMystery = "A pesar de dos siglos de excavaciones, no existe consenso concluyente sobre si trazaban un complejo calendario telúrico-astronómico, defensas mágicas costeras o veneración a deidades subterráneas.",
            evidence = EvidenceLevel.EN_DEBATE,
            keywords = listOf("carnac", "menhir", "bretaña", "francia", "megalitos", "dolmen", "alineamiento"),
            imageRes = R.drawable.alineamientos_de_carnac
        ),
        MysteryPlace(
            id = "san_agustin",
            name = "Parque Arqueológico de San Agustín",
            country = "Colombia",
            latitude = 1.8870,
            longitude = -76.2900,
            category = Category.MEGALITOS,
            whatWeKnow = "La mayor necrópolis megalítica de América del Sur y Patrimonio Mundial UNESCO (1995). Cientos de monumentales estatuas líticas antropomorfas y zoomorfas custodian tumbas ceremoniales en el Macizo Colombiano.",
            theMystery = "Se desconoce la identidad, lengua y estructura social de la civilización escultora, así como las causas de su declive y repentina desaparición siglos antes de la llegada de los conquistadores.",
            evidence = EvidenceLevel.EN_DEBATE,
            keywords = listOf("san agustin", "colombia", "huila", "macizo colombiano", "estatuas", "necropolis", "escultura megalitica"),
            imageRes = R.drawable.san_agustin
        ),
        MysteryPlace(
            id = "machu_picchu",
            name = "Machu Picchu",
            country = "Perú",
            latitude = -13.1631,
            longitude = -72.5450,
            category = Category.CIUDADES_ANTIGUAS,
            whatWeKnow = "Impresionante ciudadela inca construida a mediados del siglo XV en una cresta montañosa a 2.430 m.s.n.m. Considerada una hacienda real y centro astronómico bajo el gobierno del inca Pachacútec (UNESCO 1983).",
            theMystery = "La perfección de sus uniones líticas antisísmicas sin argamasa, sus secretos de drenaje subterráneo y las razones de su discreta evacuación antes del colapso del Tahuantinsuyo.",
            evidence = EvidenceLevel.CONSENSO,
            keywords = listOf("machu picchu", "peru", "incas", "cusco", "andes", "pachacutec", "intihuatana", "valle sagrado"),
            imageRes = R.drawable.img_machu_picchu
        ),
        MysteryPlace(
            id = "giza",
            name = "Gran Pirámide de Giza",
            country = "Egipto",
            latitude = 29.9792,
            longitude = 31.1342,
            category = Category.CIUDADES_ANTIGUAS,
            whatWeKnow = "Mandada erigir hacia el 2560 a. C. durante el reinado del faraón Keops. Construida con más de 2,3 millones de bloques de piedra caliza y granito. Es la única maravilla del mundo antiguo que permanece en pie.",
            theMystery = "En 2017 el proyecto ScanPyramids confirmó mediante tomografía de muones una cavidad hermética de al menos 30 metros de longitud sobre la Gran Galería, cuyo contenido y propósito siguen sin descifrarse.",
            evidence = EvidenceLevel.EN_DEBATE,
            keywords = listOf("giza", "egipto", "keops", "piramide", "muones", "scanpyramids", "faraon", "camara secreta"),
            imageRes = R.drawable.img_giza_pyramid
        ),
        MysteryPlace(
            id = "chichen_itza",
            name = "Chichén Itzá",
            country = "México",
            latitude = 20.6843,
            longitude = -88.5678,
            category = Category.CIUDADES_ANTIGUAS,
            whatWeKnow = "Ciudad sagrada maya-tolteca en la península de Yucatán. Su pirámide de Kukulkán despliega durante los equinoccios una sombra serpentina descendente y posee una acústica que imita el canto del quetzal.",
            theMystery = "El alcance de la ingeniería acústica maya intencional y los secretos que aún esconde el cenote sagrado subterráneo bajo los cimientos de la pirámide.",
            evidence = EvidenceLevel.CONSENSO,
            keywords = listOf("chichen itza", "mexico", "yucatan", "mayas", "kukulkan", "serpiente emplumada", "equinoccio", "cenote"),
            imageRes = R.drawable.img_chichen_itza
        ),
        MysteryPlace(
            id = "teotihuacan",
            name = "Teotihuacán",
            country = "México",
            latitude = 19.6925,
            longitude = -98.8438,
            category = Category.CIUDADES_ANTIGUAS,
            whatWeKnow = "Una de las mayores metrópolis del mundo preindustrial, hogar de más de 100.000 personas en su apogeo con las Pirámides del Sol y de la Luna. Cuando los aztecas la encontraron siglos más tarde, ya estaba en ruinas.",
            theMystery = "Se desconoce el nombre original de la urbe, la lengua de sus fundadores, su forma de gobierno y los motivos precisos del incendio catastrófico que la destruyó hacia el siglo VII d. C.",
            evidence = EvidenceLevel.EN_DEBATE,
            keywords = listOf("teotihuacan", "mexico", "piramide del sol", "calzada de los muertos", "mesoamerica", "aztecas"),
            imageRes = R.drawable.teotihuacan
        ),
        MysteryPlace(
            id = "tiwanaku",
            name = "Tiwanaku y Puma Punku",
            country = "Bolivia",
            latitude = -16.5547,
            longitude = -68.6733,
            category = Category.CIUDADES_ANTIGUAS,
            whatWeKnow = "Antigua capital espiritual y política del imperio tiwanakota a 3.850 m de altura cerca del lago Titicaca. Destacan la Puerta del Sol y los bloques de andesita y arenisca roja de Puma Punku (UNESCO 2000).",
            theMystery = "La asombrosa precisión milimétrica de cortes geométricos en forma de 'H' en andesita (una de las piedras más duras), y la técnica usada para encajar bloques de hasta 130 toneladas sin tracción animal.",
            evidence = EvidenceLevel.EN_DEBATE,
            keywords = listOf("tiwanaku", "puma punku", "bolivia", "titicaca", "puerta del sol", "megalitos andinos", "andesita"),
            imageRes = R.drawable.tiwanaku
        ),
        MysteryPlace(
            id = "nazca",
            name = "Líneas de Nazca",
            country = "Perú",
            latitude = -14.7390,
            longitude = -75.1300,
            category = Category.ENIGMAS,
            whatWeKnow = "Cientos de figuras geométricas y geoglifos de fauna (mono, colibrí, araña) trazados retirando piedras oxidadas del árido desierto por la cultura Nazca (200 a. C. – 600 d. C.). Patrimonio UNESCO 1994.",
            theMystery = "¿Fueron concebidas como senderos rituales para procesiones de petición de agua, gigantescos esquemas astronómicos celestes o mensajes a deidades del cosmos visibles solo desde el cielo?",
            evidence = EvidenceLevel.EN_DEBATE,
            keywords = listOf("nazca", "peru", "geoglifos", "colibri", "desierto", "arqueologia", "lineas de nazca"),
            imageRes = R.drawable.img_nazca_lines
        ),
        MysteryPlace(
            id = "yonaguni",
            name = "Monumento submarino de Yonaguni",
            country = "Japón",
            latitude = 24.4347,
            longitude = 123.0114,
            category = Category.ENIGMAS,
            whatWeKnow = "Formación rocosa submarina escalonada de arenisca descubierta en 1986 cerca de las islas Ryukyu. Mide unos 150 metros de largo y presenta ángulos rectos, plataformas planas y paredes verticales.",
            theMystery = "El apasionado debate geológico: mientras la corriente mayoritaria apunta a una fractura tectónica y erosión marina natural, varios investigadores sugieren terrazas talladas por el ser humano antes de la última glaciación.",
            evidence = EvidenceLevel.EN_DEBATE,
            keywords = listOf("yonaguni", "japon", "submarino", "monumento", "oceano", "terrazas", "ryukyu", "enigma marino"),
            imageRes = R.drawable.monumento_yonaguni
        ),
        MysteryPlace(
            id = "loch_ness",
            name = "Lago Ness",
            country = "Reino Unido (Escocia)",
            latitude = 57.3229,
            longitude = -4.4244,
            category = Category.LEYENDAS,
            whatWeKnow = "Extenso y profundo lago de agua dulce en las Tierras Altas escocesas. El estudio científico de ADN ambiental liderado por la Universidad de Otago en 2018 descartó la presencia de reptiles gigantes y detectó abundancia de anguilas.",
            theMystery = "¿Por qué el mito popular de 'Nessie' se resiste a morir y sigue atrayendo a miles de exploradores y científicos en busca de anomalías en sus frías y oscuras aguas?",
            evidence = EvidenceLevel.LEYENDA,
            keywords = listOf("lago ness", "escocia", "reino unido", "nessie", "monstruo", "criptozoologia", "highlands"),
            imageRes = R.drawable.img_loch_ness
        ),
        MysteryPlace(
            id = "las_lajas",
            name = "Santuario de Las Lajas",
            country = "Colombia (Ipiales, Nariño)",
            latitude = 0.805575,
            longitude = -77.586027,
            category = Category.LEYENDAS,
            whatWeKnow = "Espectacular basílica de estilo neogótico construida sobre un puente de 50 metros en el vertiginoso cañón del río Guáitara. La imagen de Nuestra Señora del Rosario permanece grabada en la piedra laja viva.",
            theMystery = "La tradición relata la milagrosa aparición en 1754 y la curación de una niña sordomuda; geológicamente la pigmentación parece penetrar la roca natural a varios centímetros de profundidad.",
            evidence = EvidenceLevel.LEYENDA,
            keywords = listOf("las lajas", "ipiales", "pasto", "narino", "colombia", "santuario", "guaitara", "laja", "milagro"),
            imageRes = R.drawable.img_las_lajas
        )
    )

    fun getAllPlaces(): List<MysteryPlace> = places

    fun filterPlaces(query: String, category: Category?): List<MysteryPlace> {
        return places.filter { place ->
            val matchesCategory = category == null || place.category == category
            val matchesQuery = TextNormalizer.matchesQuery(place, query)
            matchesCategory && matchesQuery
        }
    }

    fun getPlaceById(id: String): MysteryPlace? {
        return places.firstOrNull { it.id == id }
    }
}
