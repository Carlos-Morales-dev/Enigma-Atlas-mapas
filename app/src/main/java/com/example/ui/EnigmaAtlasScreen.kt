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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
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
import com.example.ui.components.PlacesListBottomSheet
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
 * Pantalla principal de la aplicación Enigma Atlas.
 *
 * Funcionalidades clave:
 * - Alojamiento del visor de mapas [OsmMapView] en la capa base de Compose.
 * - Gestión de permisos runtime de geolocalización, aviso explícito de privacidad y estado del hardware GPS.
 * - Sincronización entre buscador en vivo, filtros de categoría, carrusel de tarjetas y marcadores del mapa.
 * - Detección de estado de red con `ConnectivityManager` para notificación de modo offline.
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun EnigmaAtlasScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Referencia al MapView nativo para operarlo directamente desde los controles de zoom flotantes
    var activeMapView by remember { mutableStateOf<MapView?>(null) }

    // Control de visibilidad del diálogo de privacidad previo a solicitar permiso de ubicación
    var showLocationPrivacyDialog by remember { mutableStateOf(false) }

    // Control de visibilidad del catálogo en lista desplegable BottomSheet
    var showPlacesListSheet by remember { mutableStateOf(false) }

    // Comprobación inicial de permisos runtime FINE/COARSE LOCATION
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

    // Lanzador para solicitud de permisos runtime en tiempo de ejecución
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

    // Registra callback de conectividad para alertar sobre el estado de red
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

    // Acción para solicitar ubicación con aviso previo de privacidad en el dispositivo
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
        // Visor de mapas OpenStreetMap (OsmMapView)
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

        // Viñetas oscuras superior e inferior para integrar visualmente el mapa
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

        // Cabecera flotante: Buscador en vivo, Filtros de categoría y Banners informativos
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

            // Banner explicativo cuando el usuario deniega el permiso de ubicación
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

            // Banner explicativo si el sensor GPS se encuentra deshabilitado en Android
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

            // Tarjeta informativa flotante cuando se pulsa sostenidamente el mapa ("Mi hallazgo")
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

        // Controles flotantes laterales: Zoom real sin des-centrar y Botón Mi Ubicación
        MapFloatingControls(
            currentZoomLevel = uiState.currentZoomLevel,
            hasLocationPermission = hasLocationPermission,
            onZoomIn = {
                activeMapView?.controller?.zoomIn()
            },
            onZoomOut = {
                activeMapView?.controller?.zoomOut()
            },
            onMyLocationClick = requestLocationWithPrivacy,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
        )

        // Carrusel inferior de tarjetas y acceso al catálogo
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                onClick = { showPlacesListSheet = true },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xF20B1124),
                border = BorderStroke(1.2.dp, EnigmaCyan),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .testTag("btn_open_places_list_above_carousel")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = "Ver lista completa",
                        tint = EnigmaCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ver catálogo en lista (${uiState.filteredPlaces.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = EnigmaCyan
                    )
                }
            }

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

        // Ficha modal de detalle (BottomSheet)
        if (uiState.showDetailSheet && uiState.selectedPlace != null) {
            PlaceDetailBottomSheet(
                place = uiState.selectedPlace!!,
                onDismiss = viewModel::onDismissDetail,
                onFlyToPlace = { place ->
                    viewModel.onPlaceSelected(place, flyTo = true)
                }
            )
        }

        // Catálogo completo en lista desplegable
        if (showPlacesListSheet) {
            PlacesListBottomSheet(
                places = uiState.filteredPlaces,
                selectedPlace = uiState.selectedPlace,
                placeDistances = uiState.placeDistances,
                onDismiss = { showPlacesListSheet = false },
                onSelectPlace = { place ->
                    viewModel.onPlaceSelected(place, flyTo = true)
                },
                onOpenDetail = { place ->
                    viewModel.onOpenDetail(place)
                }
            )
        }

        // Diálogo explícito de privacidad previo a solicitar ubicación
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
