package com.example.util

import com.example.data.model.MysteryPlace
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Acceso al hardware de geolocalización (android.location)
 *
 * Utilidades puras y testeables para cálculo geodésico de distancias,
 * formateo amigable de distancias métricas, ordenamiento por proximidad ("Cerca de mí")
 * y cálculo de límites geográficos (Bounding Box) para encuadre inteligente.
 */
object GeoUtils {

    /**
     * Representación geométrica pura de límites geográficos (Bounding Box).
     */
    data class GeoBounds(
        val north: Double,
        val south: Double,
        val east: Double,
        val west: Double
    )

    /**
     * Acceso al hardware de geolocalización (android.location)
     *
     * Calcula la distancia geodésica en metros entre dos coordenadas WGS84.
     * Utiliza [android.location.Location.distanceBetween] cuando el runtime de Android está disponible
     * y la fórmula de Haversine como respaldo matemáticamente riguroso para pruebas unitarias de JVM.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        return try {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            results[0]
        } catch (_: RuntimeException) {
            // Respaldo de Haversine para ejecución en JVM local sin mocks de Android
            val earthRadius = 6371000.0 // Metros
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            (earthRadius * c).toFloat()
        }
    }

    /**
     * Formatea una distancia en metros a una cadena legible en español (ej. "850 m", "12 km", "1.420 km").
     */
    fun formatDistance(meters: Float): String {
        if (meters < 0) return ""
        val symbols = DecimalFormatSymbols(Locale("es", "ES")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }

        return if (meters < 1000f) {
            "${meters.toInt()} m"
        } else {
            val km = meters / 1000.0
            if (km < 10.0) {
                val df = DecimalFormat("#,##0.0", symbols)
                "${df.format(km)} km"
            } else {
                val df = DecimalFormat("#,##0", symbols)
                "${df.format(km.toInt())} km"
            }
        }
    }

    /**
     * Acceso al hardware de geolocalización (android.location)
     *
     * Ordena una lista de lugares por cercanía a una posición de referencia (lat/lon de usuario),
     * retornando cada lugar asociado a su distancia en metros.
     */
    fun sortByProximity(
        places: List<MysteryPlace>,
        userLat: Double,
        userLon: Double
    ): List<Pair<MysteryPlace, Float>> {
        return places.map { place ->
            val distance = calculateDistanceMeters(userLat, userLon, place.latitude, place.longitude)
            place to distance
        }.sortedBy { it.second }
    }

    /**
     * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
     *
     * Calcula la caja delimitadora (Bounding Box) que contiene exactamente a la colección de lugares dada.
     * Retorna null si la lista está vacía.
     */
    fun calculateBoundingBox(places: List<MysteryPlace>): GeoBounds? {
        if (places.isEmpty()) return null

        var maxLat = -90.0
        var minLat = 90.0
        var maxLon = -180.0
        var minLon = 180.0

        for (place in places) {
            if (place.latitude > maxLat) maxLat = place.latitude
            if (place.latitude < minLat) minLat = place.latitude
            if (place.longitude > maxLon) maxLon = place.longitude
            if (place.longitude < minLon) minLon = place.longitude
        }

        return GeoBounds(
            north = maxLat,
            south = minLat,
            east = maxLon,
            west = minLon
        )
    }

    /**
     * Operaciones del motor de mapas (tiles y gestos)
     *
     * Calcula la distancia acumulada total de la ruta que une a los lugares dados en el orden de la lista.
     */
    fun calculateTotalRouteDistanceMeters(places: List<MysteryPlace>): Float {
        if (places.size < 2) return 0f
        var total = 0f
        for (i in 0 until places.size - 1) {
            val p1 = places[i]
            val p2 = places[i + 1]
            total += calculateDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        }
        return total
    }
}
