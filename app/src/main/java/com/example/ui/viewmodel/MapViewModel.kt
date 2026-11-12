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
 * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
 *
 * Destino de navegación/animación de cámara centrado en un punto con nivel de zoom.
 */
data class CameraTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
 *
 * Destino de encuadre inteligente mediante Bounding Box con padding.
 */
data class BoundingBoxTarget(
    val bounds: GeoUtils.GeoBounds,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estado general de la UI para Enigma Atlas.
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

    // Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
    val currentZoomLevel: Double = 2.5,
    val temporaryDiscovery: Pair<Double, Double>? = null,

    // Operaciones del motor de mapas (tiles y gestos)
    val isRouteActive: Boolean = false,
    val routeDistanceMeters: Float = 0f
)

class MapViewModel(
    private val repository: PlacesRepository = PlacesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
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
     * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
     *
     * Búsqueda en vivo con debounce de 400 ms para evitar animaciones espurias en cada tecla.
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
     * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
     *
     * Selección de categoría con encuadre inteligente inmediato.
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
     * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
     *
     * Aplica el filtrado de lugares, cálculo de distancias y encuadre inteligente:
     * - Si hay varios resultados: encuadre inteligente con Bounding Box.
     * - Si hay un solo resultado: vuela a él con zoom 12.5.
     * - Si no hay resultados: mantiene la cámara actual.
     */
    private fun applyFiltersAndFrame(
        query: String,
        category: Category?,
        frameCamera: Boolean
    ) {
        val rawFiltered = repository.filterPlaces(query, category)
        val userLoc = _uiState.value.userLocation

        // Ordenamiento por cercanía ("Cerca de mí") si está activo y se conoce la ubicación
        val processedPlaces = if (_uiState.value.isSortByProximity && userLoc != null) {
            GeoUtils.sortByProximity(rawFiltered, userLoc.first, userLoc.second).map { it.first }
        } else {
            rawFiltered
        }

        // Distancias actualizadas
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

        // Cálculo de ruta de exploración si está activa
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
     * Acceso al hardware de geolocalización (android.location)
     *
     * Registra la posición del usuario en el dispositivo sin enviar datos a la red.
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
     * Acceso al hardware de geolocalización (android.location)
     *
     * Centra la cámara del mapa en la ubicación del usuario.
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
     * Acceso al hardware de geolocalización (android.location)
     *
     * Alterna el modo "Cerca de mí" ordenando el carrusel por proximidad.
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
     * Acceso al hardware de geolocalización (android.location)
     *
     * Gestiona la respuesta del diálogo de permisos de ubicación.
     */
    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                locationPermissionDenied = !granted
            )
        }
    }

    /**
     * Acceso al hardware de geolocalización (android.location)
     */
    fun setGpsDisabled(disabled: Boolean) {
        _uiState.update { it.copy(isGpsDisabled = disabled) }
    }

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

    fun onSurpriseMe() {
        val pool = _uiState.value.filteredPlaces.ifEmpty { _uiState.value.allPlaces }
        if (pool.isEmpty()) return

        val randomPlace = pool[Random.nextInt(pool.size)]
        onPlaceSelected(randomPlace, flyTo = true)
    }

    fun onResetWorldView() {
        _uiState.update { current ->
            current.copy(
                cameraTarget = CameraTarget(latitude = 20.0, longitude = 0.0, zoom = 2.5)
            )
        }
    }

    /**
     * Operaciones del motor de mapas (tiles y gestos)
     *
     * Activa o desactiva la "Ruta de misterios", calculando la distancia y encuadrándola.
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
     * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
     *
     * Coloca un marcador temporal "Mi hallazgo" al hacer toque largo sobre el mapa.
     */
    fun onMapLongPress(lat: Double, lon: Double) {
        _uiState.update { it.copy(temporaryDiscovery = lat to lon) }
    }

    /**
     * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
     *
     * Quita el marcador temporal "Mi hallazgo" ante un toque simple.
     */
    fun onMapSingleTap() {
        if (_uiState.value.temporaryDiscovery != null) {
            _uiState.update { it.copy(temporaryDiscovery = null) }
        }
    }

    /**
     * Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
     *
     * Registra el nivel de zoom real reportado por el listener del mapa.
     */
    fun onMapZoomChanged(zoom: Double) {
        _uiState.update { it.copy(currentZoomLevel = zoom) }
    }

    fun setOffline(isOffline: Boolean) {
        _uiState.update { it.copy(isOffline = isOffline) }
    }
}
