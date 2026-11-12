package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.ui.theme.EnigmaBorder
import com.example.ui.theme.EnigmaCyan
import com.example.ui.theme.EnigmaGold
import com.example.ui.theme.EnigmaGoldBright
import com.example.ui.theme.EnigmaPrimary
import com.example.ui.theme.EnigmaTextMuted
import com.example.ui.theme.EnigmaTextPrimary
import com.example.ui.theme.EnigmaTextSecondary
import com.example.ui.theme.MysteryDarkBackground
import com.example.ui.theme.MysteryDarkSurfaceVariant

/**
 * Cabecera flotante estilo cristal esmerilado con:
 * - Buscador en vivo con debounce
 * - Selector desplegable de categorías (Megalitos, Ciudades antiguas, Enigmas, Leyendas)
 * - Botón "Cerca de mí" totalmente responsive y sin truncamiento
 * - Acciones rápidas "Mundial" y "Azar"
 */
@Composable
fun TopSearchBarAndFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: Category?,
    onCategorySelect: (Category?) -> Unit,
    onResetWorldView: () -> Unit,
    onSurpriseMe: () -> Unit,
    isSortByProximity: Boolean,
    onToggleSortByProximity: () -> Unit,
    categoryCounts: Map<Category?, Int>,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val headerBorder = Brush.linearGradient(
        colors = listOf(
            EnigmaGold.copy(alpha = 0.55f),
            EnigmaCyan.copy(alpha = 0.35f),
            EnigmaPrimary.copy(alpha = 0.20f)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = EnigmaGold.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xF20B1124)
        ),
        border = BorderStroke(1.2.dp, headerBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            // 1. Fila de logotipo y acciones rápidas ("Mundial" y "Azar")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emblema y título
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(EnigmaGoldBright, EnigmaGold, Color(0xFF7A5400))
                                ),
                                shape = CircleShape
                            )
                            .shadow(8.dp, CircleShape, ambientColor = EnigmaGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Enigma Atlas",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            ),
                            color = EnigmaGold
                        )
                        Text(
                            text = "Lugares misteriosos del mundo",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = EnigmaTextSecondary
                        )
                    }
                }

                // Acciones rápidas con suficiente espacio sin apretarse
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onResetWorldView,
                        shape = RoundedCornerShape(12.dp),
                        color = MysteryDarkSurfaceVariant.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, EnigmaBorder),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_world_view")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Vista mundial",
                                tint = EnigmaCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Mundial",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = EnigmaTextPrimary
                            )
                        }
                    }

                    Surface(
                        onClick = onSurpriseMe,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF281E04),
                        border = BorderStroke(1.2.dp, EnigmaGold.copy(alpha = 0.75f)),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_surprise_me")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "¡Sorpréndeme!",
                                tint = EnigmaGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Azar",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = EnigmaGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Buscador en vivo
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                placeholder = {
                    Text(
                        text = "Buscar por nombre, país, cultura o enigma…",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = EnigmaTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = EnigmaGold,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda",
                                tint = EnigmaTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MysteryDarkBackground.copy(alpha = 0.90f),
                    unfocusedContainerColor = MysteryDarkBackground.copy(alpha = 0.75f),
                    focusedBorderColor = EnigmaGold,
                    unfocusedBorderColor = EnigmaBorder,
                    focusedTextColor = EnigmaTextPrimary,
                    unfocusedTextColor = EnigmaTextPrimary,
                    cursorColor = EnigmaGold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Fila de Filtro Desplegable de Categorías + Botón "Cerca de mí"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menú desplegable de categorías
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { dropdownExpanded = !dropdownExpanded },
                        shape = RoundedCornerShape(14.dp),
                        color = MysteryDarkBackground.copy(alpha = 0.85f),
                        border = BorderStroke(1.2.dp, selectedCategory?.color ?: EnigmaBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("category_dropdown_trigger")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(selectedCategory?.color ?: EnigmaPrimary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedCategory == null) "Todas (${categoryCounts[null] ?: 0})"
                                           else "${selectedCategory.displayName} (${categoryCounts[selectedCategory] ?: 0})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = selectedCategory?.color ?: EnigmaTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = if (dropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Desplegar categorías",
                                tint = EnigmaGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .background(Color(0xF80B1124))
                            .border(1.dp, EnigmaBorder, RoundedCornerShape(12.dp))
                            .testTag("category_dropdown_menu")
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(EnigmaPrimary, CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Todas las categorías (${categoryCounts[null] ?: 0})",
                                        color = if (selectedCategory == null) EnigmaPrimary else EnigmaTextPrimary,
                                        fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                onCategorySelect(null)
                                dropdownExpanded = false
                            }
                        )

                        Category.values().forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(cat.color, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${cat.displayName} (${categoryCounts[cat] ?: 0})",
                                            color = if (selectedCategory == cat) cat.color else EnigmaTextPrimary,
                                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    onCategorySelect(cat)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Botón "Cerca de mí" bien formateado, responsivo y sin truncamiento
                Surface(
                    onClick = onToggleSortByProximity,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSortByProximity) EnigmaCyan.copy(alpha = 0.22f) else MysteryDarkBackground.copy(alpha = 0.85f),
                    border = BorderStroke(1.2.dp, if (isSortByProximity) EnigmaCyan else EnigmaBorder),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("btn_near_me")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Cerca de mí",
                            tint = if (isSortByProximity) EnigmaCyan else EnigmaTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cerca de mí",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSortByProximity) EnigmaCyan else EnigmaTextPrimary
                        )
                    }
                }
            }
        }
    }
}
