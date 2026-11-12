package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ColorCategoryCiudades
import com.example.ui.theme.ColorCategoryEnigmas
import com.example.ui.theme.ColorCategoryLeyendas
import com.example.ui.theme.ColorCategoryMegalitos
import com.example.ui.theme.ColorEvidenceConsenso
import com.example.ui.theme.ColorEvidenceDebate
import com.example.ui.theme.ColorEvidenceLeyenda

/**
 * Categorías temáticas de lugares misteriosos.
 */
enum class Category(val displayName: String, val color: Color) {
    MEGALITOS("Megalitos", ColorCategoryMegalitos),
    CIUDADES_ANTIGUAS("Ciudades antiguas", ColorCategoryCiudades),
    ENIGMAS("Enigmas", ColorCategoryEnigmas),
    LEYENDAS("Leyendas", ColorCategoryLeyendas)
}

/**
 * Nivel de rigor y evidencia según la comunidad científica y arqueológica.
 */
enum class EvidenceLevel(val displayName: String, val color: Color, val description: String) {
    CONSENSO(
        displayName = "Consenso científico",
        color = ColorEvidenceConsenso,
        description = "Explicado y respaldado por evidencia arqueológica, histórica y científica sólida."
    ),
    EN_DEBATE(
        displayName = "En debate",
        color = ColorEvidenceDebate,
        description = "Existen hipótesis encontradas; la ciencia y arqueología continúan investigando."
    ),
    LEYENDA(
        displayName = "Leyenda",
        color = ColorEvidenceLeyenda,
        description = "Tradición oral, folclore popular o fenómeno sin sustento empírico material."
    )
}

/**
 * Modelo de datos representativo de cada lugar misterioso del mundo.
 */
data class MysteryPlace(
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val category: Category,
    val whatWeKnow: String,
    val theMystery: String,
    val evidence: EvidenceLevel,
    val keywords: List<String>,
    @DrawableRes val imageRes: Int? = null,
    val imageUrl: String? = null
)
