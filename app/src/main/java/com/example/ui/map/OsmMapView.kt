package com.example.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Category
import com.example.data.model.MysteryPlace
import com.example.ui.viewmodel.BoundingBoxTarget
import com.example.ui.viewmodel.CameraTarget
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Contenedor MapView y Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
 *
 * Vista interactiva de OpenStreetMap con soporte de geolocalización en dispositivo,
 * overlays profesionales (Escala, Brújula, Atribución clicable ODbL), eventos táctiles (toque largo/simple),
 * trazado de rutas de exploración, encuadre inteligente por Bounding Box y caché de íconos.
 */
@Composable
fun OsmMapView(
    places: List<MysteryPlace>,
    selectedPlace: MysteryPlace?,
    cameraTarget: CameraTarget?,
    boundingBoxTarget: BoundingBoxTarget?,
    isRouteActive: Boolean,
    temporaryDiscovery: Pair<Double, Double>?,
    hasLocationPermission: Boolean,
    onMarkerClick: (MysteryPlace) -> Unit,
    onMapLongPress: (Double, Double) -> Unit,
    onMapSingleTap: () -> Unit,
    onZoomChanged: (Double) -> Unit,
    onUserLocationUpdated: (Double, Double) -> Unit,
    mapViewInstance: ((MapView) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val sharedPrefs = remember(context) {
        context.getSharedPreferences("osmdroid_enigma_atlas", Context.MODE_PRIVATE)
    }

    // Caché de íconos de marcadores (clave: categoría + estado seleccionado)
    val markerIconCache = remember { mutableMapOf<Pair<Category, Boolean>, Drawable>() }

    fun getCachedMarkerIcon(category: Category, isSelected: Boolean): Drawable {
        return markerIconCache.getOrPut(category to isSelected) {
            createLuxuryMarkerIcon(context, category, isSelected)
        }
    }

    // Caché para el marcador especial de "Mi hallazgo"
    val discoveryIcon = remember {
        createDiscoveryMarkerIcon(context)
    }

    // Overlay de ubicación del usuario
    val locationOverlayRef = remember { arrayOfNulls<MyLocationNewOverlay>(1) }

    val mapView = remember(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, sharedPrefs)

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            minZoomLevel = 2.0
            maxZoomLevel = 18.0

            // Operaciones del motor de mapas (tiles y gestos)
            // Filtro cartográfico zafiro y pizarra nocturna
            val invertMatrix = ColorMatrix(floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            ))
            val desaturateMatrix = ColorMatrix().apply { setSaturation(0.12f) }
            val tintMatrix = ColorMatrix(floatArrayOf(
                0.60f, 0f,    0f,    0f,  6f,
                0f,    0.72f, 0f,    0f, 14f,
                0f,    0f,    1.18f, 0f, 34f,
                0f,    0f,    0f,    1f,  0f
            ))
            val cartographyFilter = ColorMatrix().apply {
                postConcat(invertMatrix)
                postConcat(desaturateMatrix)
                postConcat(tintMatrix)
            }
            overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(cartographyFilter))
            overlayManager.tilesOverlay.loadingBackgroundColor = AndroidColor.parseColor("#060913")
            overlayManager.tilesOverlay.loadingLineColor = AndroidColor.parseColor("#141E38")

            // Contenedor MapView & Control lógico y eventos (equivalente a OnMapReadyCallback)
            // Fija centro y zoom iniciales una vez el mapa tiene dimensiones medidas
            addOnFirstLayoutListener { _, _, _, _, _ ->
                controller.setZoom(2.5)
                controller.setCenter(GeoPoint(20.0, 0.0))
            }

            // Complementos de mapa: ScaleBarOverlay (Escala métrica)
            val scaleBarOverlay = ScaleBarOverlay(this).apply {
                setCentred(false)
                setScaleBarOffset(24, 180)
                setTextSize(26f)
                setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric)
            }
            overlays.add(scaleBarOverlay)

            // Complementos de mapa: CompassOverlay (Brújula)
            val compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), this).apply {
                enableCompass()
                setCompassCenter(36f, 36f)
            }
            overlays.add(compassOverlay)

            // Complementos de mapa: CopyrightOverlay (Atribución ODbL oficial y clicable)
            val copyrightOverlay = CopyrightOverlay(context).apply {
                setTextSize(10)
                setTextColor(AndroidColor.parseColor("#94A3B8"))
                setAlignRight(true)
                setOffset(16, 150)
            }
            overlays.add(copyrightOverlay)

            // Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
            // MapEventsOverlay para toque largo (Mi hallazgo) y toque simple (quitar)
            val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    onMapSingleTap()
                    return false
                }

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        onMapLongPress(p.latitude, p.longitude)
                        return true
                    }
                    return false
                }
            })
            overlays.add(eventsOverlay)

            // MapListener para registrar zoom y scroll en tiempo real
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean = false
                override fun onZoom(event: ZoomEvent?): Boolean {
                    onZoomChanged(zoomLevelDouble)
                    return false
                }
            })

            // Acceso al hardware de geolocalización (android.location)
            // MyLocationNewOverlay + GpsMyLocationProvider
            val locationProvider = GpsMyLocationProvider(context)
            val myLocationOverlay = MyLocationNewOverlay(locationProvider, this).apply {
                setDrawAccuracyEnabled(true)
                runOnFirstFix {
                    myLocation?.let { loc ->
                        onUserLocationUpdated(loc.latitude, loc.longitude)
                    }
                }
            }
            overlays.add(myLocationOverlay)
            locationOverlayRef[0] = myLocationOverlay
        }
    }

    // Exportar la instancia del MapView para control directo de zoom real
    LaunchedEffect(mapView) {
        mapViewInstance?.invoke(mapView)
    }

    // Acceso al hardware de geolocalización (android.location)
    // Habilitar o deshabilitar overlay según permisos
    LaunchedEffect(hasLocationPermission) {
        val overlay = locationOverlayRef[0]
        if (hasLocationPermission) {
            overlay?.enableMyLocation()
        } else {
            overlay?.disableMyLocation()
        }
    }

    // Manejo de ciclo de vida del MapView y ahorro de batería
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    if (hasLocationPermission) {
                        locationOverlayRef[0]?.enableMyLocation()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Detener actualizaciones de ubicación en onPause para ahorrar batería
                    locationOverlayRef[0]?.disableMyLocation()
                    mapView.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            locationOverlayRef[0]?.disableMyLocation()
            mapView.onDetach()
        }
    }

    // Actualización de marcadores, polilínea de ruta y hallazgo temporal
    LaunchedEffect(places, selectedPlace, isRouteActive, temporaryDiscovery) {
        // Remover marcadores antiguos y polilíneas
        mapView.overlays.removeAll { it is Marker || it is Polyline }

        // Operaciones del motor de mapas (tiles y gestos)
        // Dibujar polilínea de ruta de misterios conectando los lugares filtrados
        if (isRouteActive && places.size > 1) {
            val routePolyline = Polyline(mapView).apply {
                outlinePaint.color = AndroidColor.parseColor("#22D3EE") // Cian aurora
                outlinePaint.strokeWidth = 10f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                setPoints(places.map { GeoPoint(it.latitude, it.longitude) })
            }
            mapView.overlays.add(routePolyline)
        }

        // Colocar marcadores con íconos cacheados
        places.forEach { place ->
            val isSelected = place.id == selectedPlace?.id
            val marker = Marker(mapView).apply {
                position = GeoPoint(place.latitude, place.longitude)
                title = place.name
                subDescription = place.country
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = getCachedMarkerIcon(place.category, isSelected)
                setOnMarkerClickListener { _, _ ->
                    onMarkerClick(place)
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        // Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
        // Marcador temporal "Mi hallazgo"
        temporaryDiscovery?.let { (lat, lon) ->
            val discoveryMarker = Marker(mapView).apply {
                position = GeoPoint(lat, lon)
                title = "Mi hallazgo"
                subDescription = String.format("%.4f°, %.4f°", lat, lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = discoveryIcon
                showInfoWindow()
            }
            mapView.overlays.add(discoveryMarker)
        }

        mapView.invalidate()
    }

    // Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
    // Animación suave de cámara puntual
    LaunchedEffect(cameraTarget?.timestamp) {
        cameraTarget?.let { target ->
            val point = GeoPoint(target.latitude, target.longitude)
            mapView.controller.animateTo(point, target.zoom, 1000L)
        }
    }

    // Control lógico y eventos (equivalente a OnMapReadyCallback / MapController)
    // Encuadre inteligente con Bounding Box y padding de animación
    LaunchedEffect(boundingBoxTarget?.timestamp) {
        boundingBoxTarget?.let { boxTarget ->
            val box = BoundingBox(
                boxTarget.bounds.north,
                boxTarget.bounds.east,
                boxTarget.bounds.south,
                boxTarget.bounds.west
            )
            // zoomToBoundingBox con animación y padding de 220px para no solapar con controles
            mapView.zoomToBoundingBox(box, true, 220)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Operaciones del motor de mapas (tiles y gestos)
 *
 * Marcador especial dorado para "Mi hallazgo" colocado por toque largo.
 */
private fun createDiscoveryMarkerIcon(context: Context): Drawable {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(90, 246, 193, 75)
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawCircle(size / 2f, size * 0.35f, size * 0.32f, auraPaint)

    val pinPath = Path().apply {
        val centerX = size / 2f
        val radius = size * 0.28f
        val centerY = radius + 4f
        val tipY = size * 0.88f
        arcTo(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 140f, 260f, false)
        lineTo(centerX, tipY)
        close()
    }

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#F6C14B")
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, bodyPaint)

    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#060913")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size * 0.32f, size * 0.12f, corePaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Operaciones del motor de mapas (tiles y gestos)
 *
 * Genera un pin tridimensional con gradiente lumínico y núcleo de perla reflectante.
 */
private fun createLuxuryMarkerIcon(
    context: Context,
    category: Category,
    isSelected: Boolean
): Drawable {
    val width = if (isSelected) 92 else 72
    val height = if (isSelected) 118 else 94
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val baseColor = category.color.toArgb()

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(100, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawOval(
        width * 0.22f,
        height * 0.88f,
        width * 0.78f,
        height * 0.98f,
        shadowPaint
    )

    if (isSelected) {
        val outerAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(80, 246, 193, 75)
            style = Paint.Style.STROKE
            strokeWidth = 9f
        }
        val haloRadius = width * 0.44f
        val haloCenterY = width * 0.38f + 5f
        canvas.drawCircle(width / 2f, haloCenterY, haloRadius, outerAuraPaint)

        val innerAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(180, 34, 211, 238)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(width / 2f, haloCenterY, haloRadius - 4f, innerAuraPaint)
    }

    val pinPath = Path().apply {
        val centerX = width / 2f
        val headRadius = width * 0.36f
        val centerY = headRadius + 6f
        val tipY = height * 0.88f

        arcTo(centerX - headRadius, centerY - headRadius, centerX + headRadius, centerY + headRadius, 140f, 260f, false)
        lineTo(centerX, tipY)
        close()
    }

    val bodyShader = LinearGradient(
        0f, 0f, 0f, height * 0.88f,
        lightenColor(baseColor, 0.25f),
        darkenColor(baseColor, 0.30f),
        Shader.TileMode.CLAMP
    )
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = bodyShader
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, bodyPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) AndroidColor.parseColor("#F6C14B") else AndroidColor.parseColor("#060913")
        style = Paint.Style.STROKE
        strokeWidth = if (isSelected) 4.5f else 3f
    }
    canvas.drawPath(pinPath, strokePaint)

    val innerRadius = width * 0.17f
    val innerY = width * 0.36f + 6f
    val pearlShader = RadialGradient(
        width / 2f - 2f, innerY - 2f, innerRadius,
        AndroidColor.WHITE,
        AndroidColor.parseColor("#E2E8F0"),
        Shader.TileMode.CLAMP
    )
    val pearlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = pearlShader
        style = Paint.Style.FILL
    }
    canvas.drawCircle(width / 2f, innerY, innerRadius, pearlPaint)

    val gemShader = RadialGradient(
        width / 2f - 1f, innerY - 1f, innerRadius * 0.65f,
        lightenColor(baseColor, 0.3f),
        baseColor,
        Shader.TileMode.CLAMP
    )
    val gemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = gemShader
        style = Paint.Style.FILL
    }
    canvas.drawCircle(width / 2f, innerY, innerRadius * 0.65f, gemPaint)

    val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(width / 2f - innerRadius * 0.25f, innerY - innerRadius * 0.25f, innerRadius * 0.18f, glintPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun lightenColor(color: Int, factor: Float): Int {
    val r = ((AndroidColor.red(color) * (1f + factor)).coerceAtMost(255f)).toInt()
    val g = ((AndroidColor.green(color) * (1f + factor)).coerceAtMost(255f)).toInt()
    val b = ((AndroidColor.blue(color) * (1f + factor)).coerceAtMost(255f)).toInt()
    return AndroidColor.argb(AndroidColor.alpha(color), r, g, b)
}

private fun darkenColor(color: Int, factor: Float): Int {
    val r = (AndroidColor.red(color) * (1f - factor)).toInt().coerceAtLeast(0)
    val g = (AndroidColor.green(color) * (1f - factor)).toInt().coerceAtLeast(0)
    val b = (AndroidColor.blue(color) * (1f - factor)).toInt().coerceAtLeast(0)
    return AndroidColor.argb(AndroidColor.alpha(color), r, g, b)
}
