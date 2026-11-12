package com.example.util

import com.example.data.model.MysteryPlace
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Módulo de utilidades geodésicas y cálculos matemáticos sobre la curvatura terrestre.
 *
 * Funcionalidades clave:
 * - Cálculo de distancias geodésicas entre coordenadas WGS84 (`android.location.Location.distanceBetween` con respaldo Haversine en JVM).
 * - Formateo de distancias a cadenas legibles en metros o kilómetros.
 * - Ordenamiento por proximidad a la ubicación GPS del usuario.
 * - Determinación de cajas delimitadoras ([GeoBounds] / Bounding Box) para el encuadre dinámico del mapa.
 * - Cálculo de longitud total de rutas compuestas por múltiples puntos.
 */
object GeoUtils {

    /**
     * Estructura de datos que representa una caja delimitadora geográfica (Bounding Box).
     * Define los límites máximos Norte, Sur, Este y Oeste de un grupo de coordenadas.
     */
    data class GeoBounds(
        val north: Double,
        val south: Double,
        val east: Double,
        val west: Double
    )

    /**
     * Calcula la distancia en metros entre dos puntos geográficos (latitud/longitud WGS84).
     *
     * 1. En dispositivo Android: Invoca `android.location.Location.distanceBetween`.
     * 2. En pruebas unitarias de JVM: Utiliza la fórmula trigonométrica de Haversine
     *    con el radio medio terrestre R = 6,371,000 metros.
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
            // Respaldo de la fórmula de Haversine para ejecuciones locales en JVM
            val earthRadius = 6371000.0 // Radio medio de la Tierra en metros
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
     * Formatea distancias numéricas en metros a cadenas legibles ("450 m", "12,4 km", "1.420 km").
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
     * Ordena una lista de lugares según la distancia a las coordenadas de referencia del usuario.
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
     * Calcula los límites geográficos (Norte, Sur, Este, Oeste) que abarcan exactamente a la colección
     * de lugares dada. Retorna null si la lista está vacía.
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
     * Calcula la distancia acumulada total en metros de una ruta que une secuencialmente los lugares de la lista.
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
