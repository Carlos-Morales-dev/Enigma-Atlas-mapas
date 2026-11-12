package com.example.util

import com.example.data.model.MysteryPlace
import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern

/**
 * Utilidad para normalizar texto eliminando tildes, diacríticos y homogeneizando mayúsculas.
 * Permite que búsquedas como "pascua", "peru", "stonehenge" o "geoglifos" encuentren sus coincidencias.
 */
object TextNormalizer {

    private val DIACRITICS_PATTERN: Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

    /**
     * Convierte el texto a minúsculas sin acentos ni marcas diacríticas.
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val trimmed = input.trim().lowercase(Locale.ROOT)
        val normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("")
    }

    /**
     * Evalúa si un lugar misterioso coincide con la consulta dada (nombre, país, categoría, misterio, keywords).
     */
    fun matchesQuery(place: MysteryPlace, query: String): Boolean {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return true

        val normalizedName = normalize(place.name)
        val normalizedCountry = normalize(place.country)
        val normalizedCategory = normalize(place.category.displayName)
        val normalizedMystery = normalize(place.theMystery)
        val normalizedKnown = normalize(place.whatWeKnow)

        if (normalizedName.contains(normalizedQuery)) return true
        if (normalizedCountry.contains(normalizedQuery)) return true
        if (normalizedCategory.contains(normalizedQuery)) return true
        if (normalizedMystery.contains(normalizedQuery)) return true
        if (normalizedKnown.contains(normalizedQuery)) return true

        return place.keywords.any { keyword ->
            normalize(keyword).contains(normalizedQuery)
        }
    }
}
