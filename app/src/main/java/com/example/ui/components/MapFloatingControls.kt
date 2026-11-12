package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EnigmaBorder
import com.example.ui.theme.EnigmaCyan
import com.example.ui.theme.EnigmaGold
import com.example.ui.theme.EnigmaTextSecondary
import com.example.util.GeoUtils
import java.util.Locale

/**
 * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
 *
 * Controles flotantes sobre el mapa:
 * - Etiqueta pequeña con nivel de zoom actual en tiempo real
 * - Botones de Zoom In (+) y Zoom Out (-) que operan directamente sobre el zoom real del MapView sin recentrar
 * - Botón "Mi ubicación" (GpsMyLocationProvider + MyLocationNewOverlay)
 * - Botón "Ruta de misterios" (Polyline de lugares filtrados)
 */
@Composable
fun MapFloatingControls(
    currentZoomLevel: Double,
    isRouteActive: Boolean,
    routeDistanceMeters: Float,
    hasLocationPermission: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMyLocationClick: () -> Unit,
    onToggleRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val controlBorder = Brush.verticalGradient(
        colors = listOf(
            EnigmaGold.copy(alpha = 0.5f),
            EnigmaCyan.copy(alpha = 0.35f)
        )
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Indicador flotante de distancia total de la ruta cuando está activa
        if (isRouteActive && routeDistanceMeters > 0) {
            Surface(
                color = Color(0xF20B1124),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, EnigmaCyan),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("route_distance_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = EnigmaCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Ruta: ${GeoUtils.formatDistance(routeDistanceMeters)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = EnigmaCyan
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = EnigmaGold.copy(alpha = 0.15f))
                .testTag("map_zoom_controls"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF20B1124)),
            border = BorderStroke(1.2.dp, controlBorder)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Etiqueta pequeña del nivel de zoom actual (MapListener)
                Text(
                    text = String.format(Locale.ROOT, "%.1fx", currentZoomLevel),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    ),
                    color = EnigmaTextSecondary,
                    modifier = Modifier
                        .padding(top = 2.dp, bottom = 4.dp)
                        .testTag("zoom_level_label")
                )

                // Zoom In
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Acercar mapa",
                        tint = EnigmaGold,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .size(1.dp)
                        .background(EnigmaBorder)
                )

                // Zoom Out
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Alejar mapa",
                        tint = EnigmaGold,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .size(1.dp)
                        .background(EnigmaBorder)
                )

                // Acceso al hardware de geolocalización (android.location)
                // Botón "Mi ubicación"
                IconButton(
                    onClick = onMyLocationClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_my_location")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Mi ubicación",
                        tint = if (hasLocationPermission) EnigmaCyan else EnigmaGold,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .size(1.dp)
                        .background(EnigmaBorder)
                )

                // Operaciones del motor de mapas (tiles y gestos)
                // Botón "Ruta de misterios"
                IconButton(
                    onClick = onToggleRoute,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_toggle_route")
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = "Ruta de misterios",
                        tint = if (isRouteActive) EnigmaCyan else EnigmaTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Notificación informativa cuando no hay conexión (F7 - Robustez sin crashes).
 */
@Composable
fun OfflineNoticeBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = Color(0xF0991B1B),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFEF4444)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("offline_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sin conexión a Internet. Visualizando tiles almacenados en caché.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = Color.White
                )
            }
        }
    }
}
