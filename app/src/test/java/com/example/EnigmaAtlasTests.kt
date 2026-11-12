package com.example

import com.example.data.model.Category
import com.example.data.repository.PlacesRepository
import com.example.util.GeoUtils
import com.example.util.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EnigmaAtlasTests {

    private lateinit var repository: PlacesRepository

    @Before
    fun setUp() {
        repository = PlacesRepository()
    }

    @Test
    fun testTextNormalizer_removesAccentsAndLowercases() {
        assertEquals("peru", TextNormalizer.normalize("Perú"))
        assertEquals("chichen itza", TextNormalizer.normalize("Chichén Itzá"))
        assertEquals("gobekli tepe", TextNormalizer.normalize("Göbekli Tepe"))
        assertEquals("pascua", TextNormalizer.normalize("PASCUA"))
        assertEquals("lineas de nazca", TextNormalizer.normalize("Líneas de Nazca"))
    }

    @Test
    fun testCatalogIntegrity_fifteenPlacesValidCoordinatesAndUniqueIds() {
        val places = repository.getAllPlaces()
        assertEquals("El catálogo debe contener 15 lugares mínimos", 15, places.size)

        val ids = mutableSetOf<String>()
        places.forEach { place ->
            assertTrue("ID duplicado detectado: ${place.id}", ids.add(place.id))
            assertTrue("Nombre vacío para el lugar ${place.id}", place.name.isNotBlank())
            assertTrue("País vacío para el lugar ${place.id}", place.country.isNotBlank())
            assertTrue("Qué se sabe vacío para el lugar ${place.id}", place.whatWeKnow.isNotBlank())
            assertTrue("El misterio vacío para el lugar ${place.id}", place.theMystery.isNotBlank())

            // Validación de coordenadas WGS84
            assertTrue(
                "Latitud fuera de rango [-90, 90]: ${place.latitude} en ${place.name}",
                place.latitude >= -90.0 && place.latitude <= 90.0
            )
            assertTrue(
                "Longitud fuera de rango [-180, 180]: ${place.longitude} en ${place.name}",
                place.longitude >= -180.0 && place.longitude <= 180.0
            )
        }
    }

    @Test
    fun testSearchQuery_matchesWithoutAccentsOrCase() {
        val resultsPascua = repository.filterPlaces("pascua", null)
        assertTrue(resultsPascua.any { it.id == "rano_raraku" })
        assertTrue(resultsPascua.any { it.id == "tongariki" })

        val resultsStonehenge = repository.filterPlaces("stonehenge", null)
        assertEquals(1, resultsStonehenge.size)
        assertEquals("stonehenge", resultsStonehenge.first().id)

        val resultsPeru = repository.filterPlaces("peru", null)
        assertTrue(resultsPeru.any { it.id == "machu_picchu" })
        assertTrue(resultsPeru.any { it.id == "nazca" })

        val resultsGeoglifos = repository.filterPlaces("geoglifos", null)
        assertTrue(resultsGeoglifos.any { it.id == "nazca" })
    }

    @Test
    fun testCategoryFilter() {
        val megalitos = repository.filterPlaces("", Category.MEGALITOS)
        assertTrue(megalitos.isNotEmpty())
        assertTrue(megalitos.all { it.category == Category.MEGALITOS })

        val ciudades = repository.filterPlaces("", Category.CIUDADES_ANTIGUAS)
        assertTrue(ciudades.isNotEmpty())
        assertTrue(ciudades.all { it.category == Category.CIUDADES_ANTIGUAS })

        val enigmas = repository.filterPlaces("", Category.ENIGMAS)
        assertTrue(enigmas.isNotEmpty())
        assertTrue(enigmas.all { it.category == Category.ENIGMAS })

        val leyendas = repository.filterPlaces("", Category.LEYENDAS)
        assertTrue(leyendas.isNotEmpty())
        assertTrue(leyendas.all { it.category == Category.LEYENDAS })
    }

    @Test
    fun testCombinedSearchAndCategoryFilter() {
        val megalitosColombia = repository.filterPlaces("colombia", Category.MEGALITOS)
        assertEquals(1, megalitosColombia.size)
        assertEquals("san_agustin", megalitosColombia.first().id)

        val ciudadesColombia = repository.filterPlaces("colombia", Category.CIUDADES_ANTIGUAS)
        assertTrue(ciudadesColombia.isEmpty())

        val leyendasColombia = repository.filterPlaces("colombia", Category.LEYENDAS)
        assertEquals(1, leyendasColombia.size)
        assertEquals("las_lajas", leyendasColombia.first().id)
    }

    @Test
    fun testGetPlaceById() {
        val place = repository.getPlaceById("las_lajas")
        assertNotNull(place)
        assertEquals("Santuario de Las Lajas", place?.name)
        assertEquals(Category.LEYENDAS, place?.category)
    }

    // =========================================================================
    // Pruebas unitarias de geolocalización, Bounding Box y rutas (GeoUtils)
    // =========================================================================

    @Test
    fun testCalculateDistanceMeters_samePointIsZero() {
        val dist = GeoUtils.calculateDistanceMeters(0.8114, -77.5858, 0.8114, -77.5858)
        assertEquals(0f, dist, 0.01f)
    }

    @Test
    fun testCalculateDistanceMeters_knownLocations() {
        // Distancia aproximada entre Las Lajas (Ipiales) y San Agustín (Huila): aprox 180 km (180.000 m)
        val dist = GeoUtils.calculateDistanceMeters(0.8114, -77.5858, 1.8870, -76.2900)
        assertTrue("La distancia debe rondar entre 170 km y 200 km, fue: $dist", dist in 170000f..200000f)
    }

    @Test
    fun testFormatDistance() {
        assertEquals("450 m", GeoUtils.formatDistance(450f))
        assertEquals("12 km", GeoUtils.formatDistance(12000f))
        assertEquals("1,4 km", GeoUtils.formatDistance(1420f))
        assertEquals("1.420 km", GeoUtils.formatDistance(1420000f))
    }

    @Test
    fun testSortByProximity_placesOrderedAscending() {
        val places = repository.getAllPlaces()
        // Posición de usuario en Ipiales / Pasto cerca de Las Lajas
        val userLat = 0.8120
        val userLon = -77.5850

        val sorted = GeoUtils.sortByProximity(places, userLat, userLon)
        assertEquals(15, sorted.size)

        // El primer lugar debe ser Las Lajas (a menos de 2 km)
        assertEquals("las_lajas", sorted.first().first.id)
        assertTrue("La distancia a Las Lajas debe ser menor a 2000 m", sorted.first().second < 2000f)

        // La lista debe estar estrictamente ordenada de menor a mayor distancia
        for (i in 0 until sorted.size - 1) {
            assertTrue(
                "El orden por proximidad debe ser ascendente: ${sorted[i].second} <= ${sorted[i+1].second}",
                sorted[i].second <= sorted[i + 1].second
            )
        }
    }

    @Test
    fun testBoundingBox_zeroPlacesReturnsNull() {
        val bounds = GeoUtils.calculateBoundingBox(emptyList())
        assertNull("Con cero lugares el Bounding Box debe ser null", bounds)
    }

    @Test
    fun testBoundingBox_onePlaceHasIdenticalNorthSouthEastWest() {
        val place = repository.getPlaceById("stonehenge")!!
        val bounds = GeoUtils.calculateBoundingBox(listOf(place))
        assertNotNull(bounds)
        assertEquals(place.latitude, bounds?.north ?: 0.0, 0.0001)
        assertEquals(place.latitude, bounds?.south ?: 0.0, 0.0001)
        assertEquals(place.longitude, bounds?.east ?: 0.0, 0.0001)
        assertEquals(place.longitude, bounds?.west ?: 0.0, 0.0001)
    }

    @Test
    fun testBoundingBox_multiplePlacesContainsAllCoordinates() {
        val places = repository.getAllPlaces()
        val bounds = GeoUtils.calculateBoundingBox(places)
        assertNotNull(bounds)

        places.forEach { place ->
            assertTrue(
                "La latitud de ${place.name} (${place.latitude}) debe estar dentro de [south, north]",
                place.latitude in bounds!!.south..bounds.north
            )
            assertTrue(
                "La longitud de ${place.name} (${place.longitude}) debe estar dentro de [west, east]",
                place.longitude in bounds!!.west..bounds.east
            )
        }
    }

    @Test
    fun testCalculateTotalRouteDistanceMeters() {
        val emptyDist = GeoUtils.calculateTotalRouteDistanceMeters(emptyList())
        assertEquals(0f, emptyDist, 0.01f)

        val singlePlace = repository.getPlaceById("giza")!!
        val singleDist = GeoUtils.calculateTotalRouteDistanceMeters(listOf(singlePlace))
        assertEquals(0f, singleDist, 0.01f)

        // Ruta de los 2 moai de Rapa Nui (Rano Raraku y Tongariki, a aprox 1 km de distancia)
        val rano = repository.getPlaceById("rano_raraku")!!
        val tongariki = repository.getPlaceById("tongariki")!!
        val routeDist = GeoUtils.calculateTotalRouteDistanceMeters(listOf(rano, tongariki))
        assertTrue("La distancia entre los dos moai debe ser de aprox 1000 m (±300 m)", routeDist in 700f..1500f)
    }
}
