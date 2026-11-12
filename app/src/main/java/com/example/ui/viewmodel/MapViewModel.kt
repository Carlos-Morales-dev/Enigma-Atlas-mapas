package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.MysteryPlace
import com.example.data.repository.PlacesRepository
import com.example.util.GeoUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * =========================================================================================
 * MODELO DE DATOS DE EVENTOS Y VUELOS DE CÁMARA
 * =========================================================================================
 */

/**
 * Representa la orden para que el MapView vuele suavemente (animateTo)
 * hacia una coordenada específica con un nivel de zoom determinado.
 */
data class CameraTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Representa la orden para que el MapView realice un encuadre inteligente
 * (zoomToBoundingBox) ajustando la cámara para mostrar simultáneamente varios lugares.
 */
data class BoundingBoxTarget(
    val bounds: GeoUtils.GeoBounds,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estado inmutable completo de la interfaz del Mapa (StateFlow / Jetpack Compose UI State).
 */
data class MapUiState(
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val allPlaces: List<MysteryPlace> = emptyList(),
    val filteredPlaces: List<MysteryPlace> = emptyList(),
    val selectedPlace: MysteryPlace? = null,
    val showDetailSheet: Boolean = false,
    val cameraTarget: CameraTarget? = null,
    val boundingBoxTarget: BoundingBoxTarget? = null,
    val isOffline: Boolean = false,

    // Acceso al hardware de geolocalización (android.location)
    val userLocation: Pair<Double, Double>? = null,
    val isSortByProximity: Boolean = false,
    val placeDistances: Map<String, Float> = emptyMap(),
    val locationPermissionDenied: Boolean = false,
    val isGpsDisabled: Boolean = false,

    // Control lógico y eventos (equivalente a MapController / OnMapReadyCallback)
    val currentZoomLevel: Double = 2.5,
    val temporaryDiscovery: Pair<Double, Double>? = null,

    // Operaciones del motor de mapas (tiles, rutas y gestos)
    val isRouteActive: Boolean = false,
    val routeDistanceMeters: Float = 0f
)

/**
 * =========================================================================================
 * GESTOR DE ESTADO REACTIVO: MapViewModel
 * =========================================================================================
 *
 * Administra la lógica de negocio del mapa, filtrado de ubicaciones, ordenamiento por proximidad GPS,
 * encuadre de cámara mediante Bounding Box, rutas de exploración y sincronización con el MapView.
 */
class MapViewModel(
    private val repository: PlacesRepository = PlacesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Carga inicial del catálogo de 15 lugares turísticos misteriosos
        val all = repository.getAllPlaces()
        _uiState.update {
            it.copy(
                allPlaces = all,
                filteredPlaces = all,
                selectedPlace = all.firstOrNull(),
                cameraTarget = CameraTarget(latitude = 20.0, longitude = 0.0, zoom = 2.5)
            )
        }
    }

    /**
     * CONTROL LÓGICO Y EVENTOS:
     * Búsqueda en tiempo real con debounce de 400ms para optimizar las animaciones de cámara y renderizado del mapa.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400L)
            applyFiltersAndFrame(
                query = query,
                category = _uiState.value.selectedCategory,
                frameCamera = true
            )
        }
    }

    /**
     * CONTROL LÓGICO Y EVENTOS:
     * Filtra los marcadores del mapa por categoría (Megalitos, Ciudades Antiguas, Enigmas, Leyendas).
     */
    fun onCategorySelected(category: Category?) {
        val targetCategory = if (_uiState.value.selectedCategory == category) null else category
        _uiState.update { it.copy(selectedCategory = targetCategory) }
        applyFiltersAndFrame(
            query = _uiState.value.searchQuery,
            category = targetCategory,
            frameCamera = true
        )
    }

    /**
     * CONTROL LÓGICO Y EVENTOS DE CÁMARA (Equivalente a MapController):
     * Aplica los filtros de búsqueda y decide la estrategia de encuadre de la cámara:
     * - Si hay múltiples resultados: Calcula el Bounding Box (caja delimitadora) y encuadra el grupo.
     * - Si hay un solo resultado: Realiza una animación de vuelo (animateTo) con nivel de zoom 12.5.
     */
    private fun applyFiltersAndFrame(
        query: String,
        category: Category?,
        frameCamera: Boolean
    ) {
        val rawFiltered = repository.filterPlaces(query, category)
        val userLoc = _uiState.value.userLocation

        // ACCESO A GEOLOCALIZACIÓN: Ordenamiento por proximidad GPS si la ubicación está disponible
        val processedPlaces = if (_uiState.value.isSortByProximity && userLoc != null) {
            GeoUtils.sortByProximity(rawFiltered, userLoc.first, userLoc.second).map { it.first }
        } else {
            rawFiltered
        }

        // ACCESO A GEOLOCALIZACIÓN: Cálculo de distancia geodésica desde la ubicación del usuario a cada lugar
        val distances = if (userLoc != null) {
            _uiState.value.allPlaces.associate { place ->
                place.id to GeoUtils.calculateDistanceMeters(
                    userLoc.first,
                    userLoc.second,
                    place.latitude,
                    place.longitude
                )
            }
        } else {
            emptyMap()
        }

        val updatedSelection = if (processedPlaces.contains(_uiState.value.selectedPlace)) {
            _uiState.value.selectedPlace
        } else {
            processedPlaces.firstOrNull()
        }

        // MOTOR DE MAPAS: Cálculo de la distancia total de la ruta vectorial
        val routeDistance = if (_uiState.value.isRouteActive) {
            GeoUtils.calculateTotalRouteDistanceMeters(processedPlaces)
        } else {
            0f
        }

        var newCameraTarget = _uiState.value.cameraTarget
        var newBoxTarget: BoundingBoxTarget? = null

        if (frameCamera) {
            when {
                processedPlaces.size > 1 -> {
                    GeoUtils.calculateBoundingBox(processedPlaces)?.let { bounds ->
                        newBoxTarget = BoundingBoxTarget(bounds)
                    }
                }
                processedPlaces.size == 1 -> {
                    val single = processedPlaces.first()
                    newCameraTarget = CameraTarget(
                        latitude = single.latitude,
                        longitude = single.longitude,
                        zoom = 12.5
                    )
                }
            }
        }

        _uiState.update { current ->
            current.copy(
                filteredPlaces = processedPlaces,
                selectedPlace = updatedSelection,
                placeDistances = distances,
                routeDistanceMeters = routeDistance,
                cameraTarget = newCameraTarget,
                boundingBoxTarget = newBoxTarget
            )
        }
    }

    /**
     * ACCESO AL HARDWARE DE GEOLOCALIZACIÓN (android.location):
     * Actualiza las coordenadas GPS del usuario en tiempo real desde el hardware.
     */
    fun onUserLocationUpdated(latitude: Double, longitude: Double) {
        _uiState.update { current ->
            val distances = current.allPlaces.associate { place ->
                place.id to GeoUtils.calculateDistanceMeters(
                    latitude,
                    longitude,
                    place.latitude,
                    place.longitude
                )
            }
            current.copy(
                userLocation = latitude to longitude,
                placeDistances = distances,
                isGpsDisabled = false,
                locationPermissionDenied = false
            )
        }

        if (_uiState.value.isSortByProximity) {
            applyFiltersAndFrame(
                query = _uiState.value.searchQuery,
                category = _uiState.value.selectedCategory,
                frameCamera = false
            )
        }
    }

    /**
     * ACCESO AL HARDWARE DE GEOLOCALIZACIÓN:
     * Vuela la cámara del mapa hacia las coordenadas exactas de la ubicación GPS del usuario.
     */
    fun onCenterOnUserLocation() {
        val loc = _uiState.value.userLocation
        if (loc != null) {
            _uiState.update {
                it.copy(
                    cameraTarget = CameraTarget(
                        latitude = loc.first,
                        longitude = loc.second,
                        zoom = 14.0
                    )
                )
            }
        }
    }

    /**
     * ACCESO AL HARDWARE DE GEOLOCALIZACIÓN:
     * Alterna la opción "Cerca de mí" para reordenar las tarjetas por distancia al usuario.
     */
    fun toggleSortByProximity() {
        val newSort = !_uiState.value.isSortByProximity
        _uiState.update { it.copy(isSortByProximity = newSort) }
        applyFiltersAndFrame(
            query = _uiState.value.searchQuery,
            category = _uiState.value.selectedCategory,
            frameCamera = false
        )
    }

    /**
     * ACCESO AL HARDWARE DE GEOLOCALIZACIÓN:
     * Registra si el permiso de ubicación fue denegado.
     */
    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                locationPermissionDenied = !granted
            )
        }
    }

    fun setGpsDisabled(disabled: Boolean) {
        _uiState.update { it.copy(isGpsDisabled = disabled) }
    }

    /**
     * CONTROL LÓGICO Y EVENTOS DE CÁMARA:
     * Selecciona un lugar y anima el vuelo de la cámara hacia sus coordenadas WGS84.
     */
    fun onPlaceSelected(place: MysteryPlace, flyTo: Boolean = true) {
        _uiState.update { current ->
            current.copy(
                selectedPlace = place,
                cameraTarget = if (flyTo) CameraTarget(
                    latitude = place.latitude,
                    longitude = place.longitude,
                    zoom = 13.5
                ) else current.cameraTarget
            )
        }
    }

    /**
     * CONTROL LÓGICO Y EVENTOS DE CÁMARA:
     * Evento al pulsar un marcador sobre el mapa.
     */
    fun onMarkerClicked(place: MysteryPlace) {
        _uiState.update { current ->
            current.copy(
                selectedPlace = place,
                cameraTarget = CameraTarget(
                    latitude = place.latitude,
                    longitude = place.longitude,
                    zoom = 13.0
                )
            )
        }
    }

    fun onOpenDetail(place: MysteryPlace? = null) {
        _uiState.update { current ->
            val targetPlace = place ?: current.selectedPlace
            current.copy(
                selectedPlace = targetPlace,
                showDetailSheet = true
            )
        }
    }

    fun onDismissDetail() {
        _uiState.update { it.copy(showDetailSheet = false) }
    }

    /**
     * CONTROL LÓGICO Y EVENTOS:
     * Selecciona un lugar misterioso al azar y desplaza la cámara hacia él.
     */
    fun onSurpriseMe() {
        val pool = _uiState.value.filteredPlaces.ifEmpty { _uiState.value.allPlaces }
        if (pool.isEmpty()) return

        val randomPlace = pool[Random.nextInt(pool.size)]
        onPlaceSelected(randomPlace, flyTo = true)
    }

    /**
     * CONTROL LÓGICO Y EVENTOS:
     * Restablece la vista planetaria del mapa (zoom 2.5, latitud 20.0, longitud 0.0).
     */
    fun onResetWorldView() {
        _uiState.update { current ->
            current.copy(
                cameraTarget = CameraTarget(latitude = 20.0, longitude = 0.0, zoom = 2.5)
            )
        }
    }

    /**
     * OPERACIONES DEL MOTOR DE MAPAS:
     * Activa o desactiva la polilínea de ruta que une los lugares misteriosos.
     */
    fun toggleRoute() {
        val willBeActive = !_uiState.value.isRouteActive
        val places = _uiState.value.filteredPlaces
        val distance = if (willBeActive) GeoUtils.calculateTotalRouteDistanceMeters(places) else 0f
        val bounds = if (willBeActive && places.size > 1) GeoUtils.calculateBoundingBox(places) else null

        _uiState.update {
            it.copy(
                isRouteActive = willBeActive,
                routeDistanceMeters = distance,
                boundingBoxTarget = bounds?.let { b -> BoundingBoxTarget(b) }
            )
        }
    }

    /**
     * CONTROL LÓGICO Y EVENTOS DE MAPA:
     * Coloca un marcador temporal ("Mi hallazgo") ante un toque largo en cualquier punto del mapa.
     */
    fun onMapLongPress(lat: Double, lon: Double) {
        _uiState.update { it.copy(temporaryDiscovery = lat to lon) }
    }

    /**
     * CONTROL LÓGICO Y EVENTOS DE MAPA:
     * Remueve el marcador temporal ante un toque simple sobre el mapa.
     */
    fun onMapSingleTap() {
        if (_uiState.value.temporaryDiscovery != null) {
            _uiState.update { it.copy(temporaryDiscovery = null) }
        }
    }

    /**
     * CONTROL LÓGICO Y EVENTOS DE MAPA:
     * Actualiza en el estado de la UI el nivel de zoom real reportado por el listener de osmdroid.
     */
    fun onMapZoomChanged(zoom: Double) {
        _uiState.update { it.copy(currentZoomLevel = zoom) }
    }

    fun setOffline(isOffline: Boolean) {
        _uiState.update { it.copy(isOffline = isOffline) }
    }
}
