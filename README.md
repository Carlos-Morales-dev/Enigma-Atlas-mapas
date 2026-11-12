# Enigma Atlas 🗺️✨
## Guía de Socialización e Implementación Práctica de Mapas en Android

**Enigma Atlas** es una aplicación móvil nativa construida con **Kotlin**, **Jetpack Compose** y la biblioteca open-source **OpenStreetMap (`osmdroid`)**. La aplicación permite explorar interactivamente 15 lugares turísticos misteriosos del mundo sobre un mapa con cartografía personalizada en modo exploración, trazado de rutas vectoriales, encuadre inteligente por Bounding Box y geolocalización en tiempo real.

---

## 🛠️ Paso a Paso: ¿Cómo se Implementó el Mapa en este Ejemplo Práctico?

### Paso 1: Configuración de Permisos en el Manifiesto
Se declararon los permisos necesarios en `AndroidManifest.xml`:
- `INTERNET` y `ACCESS_NETWORK_STATE`: Para la descarga de mosaicos (tiles) de OpenStreetMap.
- `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`: Para acceder al hardware de geolocalización (GPS).

### Paso 2: Inclusión de la Dependencia
En `app/build.gradle.kts` se integró la biblioteca nativa osmdroid:
```kotlin
implementation("org.osmdroid:osmdroid-android:6.1.20")
```

### Paso 3: Inicialización del Motor osmdroid
Antes de cargar mapas, osmdroid requiere definir el `userAgentValue` (identificador único de la app ante los servidores de OSM) y cargar las preferencias para la caché local de mosaicos:
```kotlin
Configuration.getInstance().userAgentValue = context.packageName
Configuration.getInstance().load(context, sharedPrefs)
```

### Paso 4: Envoltorio para Jetpack Compose (`AndroidView`)
Como `MapView` de osmdroid es una `View` tradicional de Android, se integró en la interfaz declarativa de Jetpack Compose mediante `AndroidView`, permitiendo manejar su ciclo de vida y estado reactivo.

### Paso 5: Superposición de Capas (Overlays)
Se añadieron capas nativas sobre el mapa:
- **`ScaleBarOverlay`**: Barra de escala métrica gráfica.
- **`CompassOverlay`**: Brújula magnética orientada en tiempo real.
- **`CopyrightOverlay`**: Atribución legal ODbL requerida por OpenStreetMap.
- **`MapEventsOverlay`**: Detección de toque largo (para colocar el marcador dorado "Mi hallazgo") y toque simple.
- **`MyLocationNewOverlay` + `GpsMyLocationProvider`**: Muestra la posición GPS del usuario y su círculo de precisión.
- **`Polyline`**: Traza líneas vectoriales cian conectando las ubicaciones.

### Paso 6: Renderizado Vectorial de Marcadores
Se implementaron funciones con `Canvas` y `Shader` de Android para dibujar marcadores 3D personalizados según la categoría del sitio, optimizados mediante una caché en memoria (`mutableMapOf`).

### Paso 7: Control de Cámara y Encuadre Inteligente
A través del `MapViewModel`, se coordinan dos formas de mover el mapa:
1. **`CameraTarget`**: Animación suave de vuelo (`controller.animateTo`) hacia un punto con zoom específico.
2. **`BoundingBoxTarget`**: Ajuste automático del área visible (`zoomToBoundingBox`) para encuadrar varios lugares a la vez.

---

## 👥 Repartición de Archivos para la Socialización (2 Estudiantes)

Para la sustentación de **15 minutos en clase**, la exposición se divide equitativamente entre los 2 integrantes del equipo:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    ESTRUCTURA DE SUSTENTACIÓN (15 MINUTOS)                   │
├──────────────────────────────────────┬───────────────────────────────────────┤
│ ESTUDIANTE 1 (7.5 min)               │ ESTUDIANTE 2 (7.5 min)                │
│ • Permisos & osmdroid (Gradle)       │ • Geolocalización GPS & Privacidad    │
│ • Motor de Mapa (OsmMapView.kt)      │ • Interfaz & Permisos (EnigmaAtlas)   │
│ • Marcadores Canvas & Polilíneas     │ • Gestor de Estado (MapViewModel)     │
│ • Geodesia (GeoUtils / BoundingBox)  │ • Zoom Real (MapFloatingControls)     │
└──────────────────────────────────────┴───────────────────────────────────────┘
```

---

## 👨‍💻 ESTUDIANTE 1: Configuración, Motor de Mapas, Marcadores Vectoriales y Geodesia

### 📄 Archivos a cargo de Estudiante 1:
1. `app/src/main/AndroidManifest.xml` & `app/build.gradle.kts`
2. `app/src/main/java/com/example/ui/map/OsmMapView.kt`
3. `app/src/main/java/com/example/util/GeoUtils.kt`

---

### 1. `AndroidManifest.xml` y `app/build.gradle.kts`
- **Descripción**: Contiene la infraestructura del sistema y dependencias de mapas.
- **Apartados clave a explicar**:
  - Declaración de `INTERNET`, `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`.
  - Inclusión del artefacto `org.osmdroid:osmdroid-android:6.1.20`.

```xml
<!-- Permisos para descarga de tiles cartográficos y hardware GPS -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

### 2. `OsmMapView.kt`
- **Descripción**: Es el componente Composable central que encapsula el `MapView` nativo de osmdroid en Compose.
- **Apartados clave a explicar**:
  - **Identificación de UserAgent y Caché**: `Configuration.getInstance().userAgentValue = context.packageName`.
  - **Filtro Cartográfico Personalizado**: `ColorMatrixColorFilter` para dar una apariencia visual personalizada a los mosaicos.
  - **Overlays Integrados**:
    - `ScaleBarOverlay`: Muestra la escala métrica en pantalla.
    - `CompassOverlay`: Brújula magnética integrada.
    - `CopyrightOverlay`: Atribución legal ODbL oficial.
  - **Generación Vectorial de Marcadores**: `createLuxuryMarkerIcon` utiliza `Canvas`, `LinearGradient` y `RadialGradient` para pintar pines tridimensionales en formato de gota con halo luminoso cuando el lugar está seleccionado.
  - **Rutas Vectoriales**: `Polyline` conecta las coordenadas de los lugares filtrados con una línea de color cian.
  - **Eventos Táctiles**: `MapEventsOverlay` captura pulsaciones largas para crear marcadores temporales ("Mi hallazgo").

```kotlin
// Integración de View nativa en Compose mediante AndroidView
AndroidView(
    factory = { mapView },
    modifier = Modifier.fillMaxSize()
)

// Creación de Marcador Vectorial con Canvas
val bodyShader = LinearGradient(
    0f, 0f, 0f, height * 0.88f,
    lightenColor(baseColor, 0.25f),
    darkenColor(baseColor, 0.30f),
    Shader.TileMode.CLAMP
)
```

---

### 3. `GeoUtils.kt`
- **Descripción**: Módulo de utilidad matemática geodésica pura y testeable.
- **Apartados clave a explicar**:
  - **Fórmula Geodésica**: Utiliza `android.location.Location.distanceBetween` en el dispositivo y un respaldo matemáticamente riguroso con la **Fórmula de Haversine** para pruebas de JVM local sin emulador.
  - **Bounding Box (`calculateBoundingBox`)**: Recorre las coordenadas de los lugares visibles para determinar los puntos extremos Norte, Sur, Este y Oeste ([GeoBounds]), permitiendo que el mapa encuadre automáticamente todos los resultados.

```kotlin
// Respaldo trigonométrico de Haversine para cálculo geodésico
val earthRadius = 6371000.0 // Metros
val dLat = Math.toRadians(lat2 - lat1)
val dLon = Math.toRadians(lon2 - lon1)
val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)
val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
return (earthRadius * c).toFloat()
```

---

## 👨‍💻 ESTUDIANTE 2: Geolocalización en Hardware, Control de Cámara, Estado Reactivo y Robustez Offline

### 📄 Archivos a cargo de Estudiante 2:
1. `app/src/main/java/com/example/ui/EnigmaAtlasScreen.kt`
2. `app/src/main/java/com/example/ui/viewmodel/MapViewModel.kt`
3. `app/src/main/java/com/example/ui/components/MapFloatingControls.kt`

---

### 1. `EnigmaAtlasScreen.kt`
- **Descripción**: Orquestador visual de la pantalla principal que gestiona la interacción del usuario, permisos y conectividad.
- **Apartados clave a explicar**:
  - **Permisos Runtime**: `rememberLauncherForActivityResult` con `ActivityResultContracts.RequestMultiplePermissions` para solicitar GPS en tiempo de ejecución.
  - **Diálogo de Privacidad de Ubicación**: Antes de pedir el permiso de ubicación, se presenta un `AlertDialog` explicativo informando al usuario que sus datos de posición permanecen locales en el teléfono y no se envían a servidores externos.
  - **Gestión de Red y Modo Offline**: Suscripción a `ConnectivityManager.NetworkCallback` para detectar pérdidas de conexión a Internet y mostrar una alerta informativa avisando que los tiles provienen de la caché local.
  - **Sincronización Bidireccional**: Vincula la selección en el carrusel o en la lista desplegable con los movimientos del mapa.

```kotlin
// Solicitud segura de permisos runtime para GPS
val locationPermissionsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
) { permissionsMap ->
    val granted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                  permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    hasLocationPermission = granted
    viewModel.onLocationPermissionResult(granted)
}
```

---

### 2. `MapViewModel.kt`
- **Descripción**: ViewModel que mantiene y transforma el `MapUiState` mediante `StateFlow` reactivo.
- **Apartados clave a explicar**:
  - **Búsqueda con Debounce de 400ms**: Evita recalcular distancias o reiniciar la cámara de forma espuria en cada pulsación del teclado.
  - **Estrategias de Encuadre**:
    - Genera un `CameraTarget` para vuelos dirigidos (`animateTo`) a un lugar individual.
    - Genera un `BoundingBoxTarget` cuando hay 2 o más resultados filtrados.
  - **Modo "Cerca de mí" (`toggleSortByProximity`)**: Reordena las tarjetas del carrusel según la distancia real en metros desde la posición actual del usuario.
  - **Manejo de Marcadores Temporales**: Procesa `onMapLongPress` para capturar la latitud y longitud donde el usuario mantuvo presionado el mapa.

```kotlin
// Coordinación de encuadre automático por BoundingBox o por lugar individual
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
```

---

### 3. `MapFloatingControls.kt`
- **Descripción**: Tarjeta flotante con los controles interactivos de Zoom y Posicionamiento.
- **Apartados clave a explicar**:
  - **Zoom Real sin Recentrar**: Los botones `+` y `-` ejecutan directamente `activeMapView?.controller?.zoomIn()` y `zoomOut()`. Esto incrementa o decrementa el nivel de zoom preservando el centro que el usuario está observando, evitando saltos hacia coordenadas por defecto.
  - **Indicador de Zoom en Tiempo Real**: Muestra el valor exacto (ej. `3.5x`, `12.0x`) reportado por el listener `MapListener` de osmdroid.
  - **Botón Mi Ubicación**: Desplaza la vista a las coordenadas GPS del dispositivo.

```kotlin
// Zoom directo en la instancia nativa del MapView
IconButton(
    onClick = { activeMapView?.controller?.zoomIn() },
    modifier = Modifier.size(40.dp)
) {
    Icon(imageVector = Icons.Default.Add, contentDescription = "Acercar mapa")
}
```

---

## 🧪 Pruebas Unitarias

Para verificar la lógica de mapas y la integridad del catálogo:
```bash
gradle :app:testDebugUnitTest
```

---

## 📜 Licencias
- Mapas © Colaboradores de OpenStreetMap (Licencia ODbL).
