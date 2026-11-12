package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Category
import com.example.ui.components.MapFloatingControls
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.PlaceCardsCarousel
import com.example.ui.components.PlaceDetailBottomSheet
import com.example.ui.components.TopSearchBarAndFilters
import com.example.ui.map.OsmMapView
import com.example.ui.theme.EnigmaBorder
import com.example.ui.theme.EnigmaCyan
import com.example.ui.theme.EnigmaGold
import com.example.ui.theme.EnigmaTextPrimary
import com.example.ui.theme.EnigmaTextSecondary
import com.example.ui.theme.MysteryDarkBackground
import com.example.ui.viewmodel.MapViewModel
import org.osmdroid.views.MapView
import java.util.Locale

/**
 * Contenedor MapView y Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
 *
 * Pantalla principal de Enigma Atlas.
 * Coordina la visualización cartográfica, geolocalización local sin envío a servidores,
 * encuadre inteligente Bounding Box, trazado de rutas de misterios y eventos del mapa.
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun EnigmaAtlasScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Referencia al MapView para control de zoom real directo sin recentrar
    var activeMapView by remember { mutableStateOf<MapView?>(null) }

    // Estado del diálogo de privacidad de ubicación
    var showLocationPrivacyDialog by remember { mutableStateOf(false) }

    // Acceso al hardware de geolocalización (android.location)
    // Comprobación inicial de permisos
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Acceso al hardware de geolocalización (android.location)
    // Lanzador para solicitar permisos en tiempo de ejecución
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        viewModel.onLocationPermissionResult(granted)

        if (granted) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val isGpsActive = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                             locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            viewModel.setGpsDisabled(!isGpsActive)
        }
    }

    // Monitoreo seguro de conectividad a Internet (F7 - Robustez sin crashes)
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModel.setOffline(false)
            }

            override fun onLost(network: Network) {
                viewModel.setOffline(true)
            }
        }

        try {
            val isConnected = connectivityManager?.activeNetwork?.let { net ->
                connectivityManager.getNetworkCapabilities(net)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } == true
            viewModel.setOffline(!isConnected)

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {}

        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {}
        }
    }

    val categoryCounts = remember(uiState.allPlaces) {
        val map = mutableMapOf<Category?, Int>()
        map[null] = uiState.allPlaces.size
        Category.values().forEach { cat ->
            map[cat] = uiState.allPlaces.count { it.category == cat }
        }
        map
    }

    // Acción para solicitar ubicación con aviso explícito de privacidad en dispositivo
    val requestLocationWithPrivacy = {
        if (hasLocationPermission) {
            viewModel.onCenterOnUserLocation()
        } else {
            showLocationPrivacyDialog = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MysteryDarkBackground)
    ) {
        // 1. Contenedor MapView con renderizado y overlays
        OsmMapView(
            places = uiState.filteredPlaces,
            selectedPlace = uiState.selectedPlace,
            cameraTarget = uiState.cameraTarget,
            boundingBoxTarget = uiState.boundingBoxTarget,
            isRouteActive = uiState.isRouteActive,
            temporaryDiscovery = uiState.temporaryDiscovery,
            hasLocationPermission = hasLocationPermission,
            onMarkerClick = { place ->
                viewModel.onMarkerClicked(place)
            },
            onMapLongPress = { lat, lon ->
                viewModel.onMapLongPress(lat, lon)
            },
            onMapSingleTap = {
                viewModel.onMapSingleTap()
            },
            onZoomChanged = { zoom ->
                viewModel.onMapZoomChanged(zoom)
            },
            onUserLocationUpdated = { lat, lon ->
                viewModel.onUserLocationUpdated(lat, lon)
            },
            mapViewInstance = { mv ->
                activeMapView = mv
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viñetas superior e inferior para fusionar el mapa con la interfaz flotante
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MysteryDarkBackground.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MysteryDarkBackground.copy(alpha = 0.90f)
                        )
                    )
                )
        )

        // 2. Cabecera flotante con buscador, filtros y botón "Cerca de mí"
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
        ) {
            OfflineNoticeBanner(
                isOffline = uiState.isOffline,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Banner informativo si el permiso de ubicación fue denegado
            AnimatedVisibility(
                visible = uiState.locationPermissionDenied,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Surface(
                    color = Color(0xF01E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EnigmaBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("location_denied_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = EnigmaGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Permiso de ubicación denegado. La app continúa funcionando normalmente en modo exploración.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = EnigmaTextPrimary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onLocationPermissionResult(true) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = EnigmaTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Banner informativo si el GPS está desactivado
            AnimatedVisibility(
                visible = uiState.isGpsDisabled && hasLocationPermission,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Surface(
                    color = Color(0xF03B2605),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EnigmaGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("gps_disabled_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = EnigmaGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "El GPS del dispositivo está desactivado. Actívalo para obtener tu posición exacta.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = EnigmaGold
                        )
                    }
                }
            }

            // Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
            // Banner flotante para el marcador temporal "Mi hallazgo"
            uiState.temporaryDiscovery?.let { (lat, lon) ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
                    border = BorderStroke(1.2.dp, EnigmaGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("discovery_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = EnigmaGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Mi hallazgo",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EnigmaGold
                                )
                                Text(
                                    text = String.format(Locale.ROOT, "Lat: %.4f°,  Lon: %.4f° · Toque simple para quitar", lat, lon),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = EnigmaTextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = viewModel::onMapSingleTap,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar hallazgo",
                                tint = EnigmaGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            TopSearchBarAndFilters(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::onCategorySelected,
                onResetWorldView = viewModel::onResetWorldView,
                onSurpriseMe = viewModel::onSurpriseMe,
                isSortByProximity = uiState.isSortByProximity,
                onToggleSortByProximity = {
                    if (hasLocationPermission) {
                        viewModel.toggleSortByProximity()
                    } else {
                        showLocationPrivacyDialog = true
                    }
                },
                categoryCounts = categoryCounts
            )
        }

        // 3. Controles flotantes: Zoom real sin recentrar, Mi ubicación y Ruta de misterios
        MapFloatingControls(
            currentZoomLevel = uiState.currentZoomLevel,
            isRouteActive = uiState.isRouteActive,
            routeDistanceMeters = uiState.routeDistanceMeters,
            hasLocationPermission = hasLocationPermission,
            onZoomIn = {
                // Operación directa en MapView: preserva centro y gestos del usuario sin saltos
                activeMapView?.controller?.zoomIn()
            },
            onZoomOut = {
                activeMapView?.controller?.zoomOut()
            },
            onMyLocationClick = requestLocationWithPrivacy,
            onToggleRoute = viewModel::toggleRoute,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
        )

        // 4. Carrusel inferior sincronizado con distancias "Cerca de mí"
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 6.dp)
                .fillMaxWidth()
        ) {
            PlaceCardsCarousel(
                places = uiState.filteredPlaces,
                selectedPlace = uiState.selectedPlace,
                placeDistances = uiState.placeDistances,
                onCardClick = { place ->
                    viewModel.onPlaceSelected(place, flyTo = true)
                },
                onViewDetailClick = { place ->
                    viewModel.onOpenDetail(place)
                }
            )
        }

        // 5. Ficha de detalle BottomSheet con "Cómo llegar"
        if (uiState.showDetailSheet && uiState.selectedPlace != null) {
            PlaceDetailBottomSheet(
                place = uiState.selectedPlace!!,
                onDismiss = viewModel::onDismissDetail,
                onFlyToPlace = { place ->
                    viewModel.onPlaceSelected(place, flyTo = true)
                }
            )
        }

        // Acceso al hardware de geolocalización (android.location)
        // Diálogo explícito de privacidad previa a la solicitud del permiso
        if (showLocationPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showLocationPrivacyDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EnigmaCyan,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Ubicación en el dispositivo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EnigmaTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Enigma Atlas utiliza tu ubicación únicamente de forma local en tu teléfono para calcular distancias a los misterios ('Cerca de mí') y ubicarte sobre el mapa.\n\nTus datos de ubicación NO se guardan ni se envían a ningún servidor externo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EnigmaTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLocationPrivacyDialog = false
                            locationPermissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EnigmaCyan, contentColor = Color(0xFF0F172A))
                    ) {
                        Text("Continuar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationPrivacyDialog = false }) {
                        Text("Ahora no", color = EnigmaTextSecondary)
                    }
                },
                containerColor = Color(0xFF0D1428),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
