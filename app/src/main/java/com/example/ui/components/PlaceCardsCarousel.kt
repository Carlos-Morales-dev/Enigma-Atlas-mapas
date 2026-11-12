package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EvidenceLevel
import com.example.data.model.MysteryPlace
import com.example.ui.theme.EnigmaBorder
import com.example.ui.theme.EnigmaCyan
import com.example.ui.theme.EnigmaGold
import com.example.ui.theme.EnigmaPrimary
import com.example.ui.theme.EnigmaTextPrimary
import com.example.ui.theme.EnigmaTextSecondary
import com.example.ui.theme.MysteryDarkBackground
import com.example.ui.theme.MysteryDarkSurface
import com.example.ui.theme.MysteryDarkSurfaceVariant
import com.example.ui.theme.MysterySurfaceElevated

/**
 * Carrusel horizontal de tarjetas interactivo con iluminación temática y sincronización fluida con el mapa.
 */
@Composable
fun PlaceCardsCarousel(
    places: List<MysteryPlace>,
    selectedPlace: MysteryPlace?,
    placeDistances: Map<String, Float> = emptyMap(),
    onCardClick: (MysteryPlace) -> Unit,
    onViewDetailClick: (MysteryPlace) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Sincronizar desplazamiento del carrusel cuando cambia el lugar seleccionado
    LaunchedEffect(selectedPlace?.id) {
        selectedPlace?.let { place ->
            val index = places.indexOfFirst { it.id == place.id }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    if (places.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("empty_state_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF20B1124)),
            border = BorderStroke(1.2.dp, EnigmaBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(EnigmaGold.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = EnigmaGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No encontramos ese misterio… todavía",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = EnigmaTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Intenta buscar por otro país, cultura o cambia el filtro de categoría.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EnigmaTextSecondary
                )
            }
        }
        return
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("places_carousel")
    ) {
        itemsIndexed(
            items = places,
            key = { _, place -> place.id }
        ) { _, place ->
            val isSelected = place.id == selectedPlace?.id
            val distance = placeDistances[place.id]
            MysteryPlaceCard(
                place = place,
                isSelected = isSelected,
                distanceMeters = distance,
                onClick = { onCardClick(place) },
                onDetailClick = { onViewDetailClick(place) },
                modifier = Modifier.testTag("place_card_${place.id}")
            )
        }
    }
}

@Composable
private fun MysteryPlaceCard(
    place: MysteryPlace,
    isSelected: Boolean,
    distanceMeters: Float?,
    onClick: () -> Unit,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) EnigmaGold else place.category.color.copy(alpha = 0.38f),
        animationSpec = tween(durationMillis = 250),
        label = "card_border"
    )

    val backgroundGradient = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(
                MysterySurfaceElevated,
                Color(0xFF131D38)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xF010182E),
                Color(0xF00A0F1E)
            )
        )
    }

    Card(
        modifier = modifier
            .width(312.dp)
            .shadow(
                elevation = if (isSelected) 14.dp else 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = if (isSelected) EnigmaGold else place.category.color
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundGradient)
        ) {
            Column {
                // Cabecera con Foto representativa o tira luminosa
                if (place.imageRes != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                    ) {
                        Image(
                            painter = painterResource(id = place.imageRes),
                            contentDescription = place.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Degradado sutil para fusionar la foto con el cuerpo de la tarjeta
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x220A0F1E),
                                            Color(0x660A0F1E),
                                            Color(0xF010182E)
                                        )
                                    )
                                )
                        )
                        // Tira luminosa superior en el borde de la foto
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (isSelected) {
                                            listOf(EnigmaGold, EnigmaCyan, place.category.color)
                                        } else {
                                            listOf(place.category.color.copy(alpha = 0.8f), place.category.color.copy(alpha = 0.2f))
                                        }
                                    )
                                )
                        )
                        // Badges de Categoría y Evidencia flotantes sobre la foto
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge categoría con fondo oscuro translúcido
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = Color(0xD00A0F1E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(place.category.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = place.category.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = place.category.color
                                )
                            }

                            // Badge de evidencia
                            EvidenceBadge(evidence = place.evidence)
                        }
                    }
                } else {
                    // Fallback sin foto: tira de acento luminoso
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = if (isSelected) {
                                        listOf(EnigmaGold, EnigmaCyan, place.category.color)
                                    } else {
                                        listOf(place.category.color.copy(alpha = 0.8f), place.category.color.copy(alpha = 0.2f))
                                    }
                                )
                            )
                    )
                }

                Column(modifier = Modifier.padding(14.dp)) {
                    // Si no había foto, mostrar badges aquí
                    if (place.imageRes == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = place.category.color.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(place.category.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = place.category.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = place.category.color
                                )
                            }
                            EvidenceBadge(evidence = place.evidence)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                Spacer(modifier = Modifier.height(8.dp))

                // Título del lugar
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    color = EnigmaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // País y ubicación
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = EnigmaGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = place.country,
                        style = MaterialTheme.typography.bodySmall,
                        color = EnigmaTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (distanceMeters != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = EnigmaCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, EnigmaCyan.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = com.example.util.GeoUtils.formatDistance(distanceMeters),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = EnigmaCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Breve misterio
                Text(
                    text = place.theMystery,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = EnigmaTextPrimary.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Acciones de la tarjeta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDetailClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, EnigmaPrimary.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EnigmaPrimary
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("detail_button_${place.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ficha",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) EnigmaGold else EnigmaPrimary,
                            contentColor = if (isSelected) Color(0xFF1B1500) else Color.White
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("fly_button_${place.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSelected) "Centrado" else "Volar",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun EvidenceBadge(
    evidence: EvidenceLevel,
    modifier: Modifier = Modifier
) {
    val icon = when (evidence) {
        EvidenceLevel.CONSENSO -> Icons.Default.CheckCircle
        EvidenceLevel.EN_DEBATE -> Icons.Default.HelpOutline
        EvidenceLevel.LEYENDA -> Icons.Default.AutoAwesome
    }

    Surface(
        color = evidence.color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, evidence.color.copy(alpha = 0.40f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = evidence.color,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = evidence.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = evidence.color
            )
        }
    }
}
