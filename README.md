# Enigma Atlas 🗺️✨
### Buscador de Lugares Turísticos Misteriosos del Mundo

**Enigma Atlas** es una aplicación móvil nativa para Android construida con **Kotlin**, **Jetpack Compose** y **OpenStreetMap (osmdroid)**. Permite explorar y descubrir los lugares turísticos más enigmáticos y fascinantes del planeta sobre un mapa interactivo con estética nocturna/oscura de exploración.

---

## 🏛️ Características Principales

- **Mapas con OpenStreetMap (`osmdroid`):** Integración nativa sin dependencias de servicios privativos o claves de Google Maps. Configuración de tiles Mapnik en modo nocturno/invertido, con atribución legal obligatoria (ODbL) visible.
- **Catálogo de 15 Lugares Verificados:**
  - *Megalitos:* Moai de Rano Raraku, Ahu Tongariki, Stonehenge, Göbekli Tepe, Alineamientos de Carnac, Parque Arqueológico de San Agustín.
  - *Ciudades antiguas:* Machu Picchu, Gran Pirámide de Giza, Chichén Itzá, Teotihuacán, Tiwanaku y Puma Punku.
  - *Enigmas:* Líneas de Nazca, Monumento submarino de Yonaguni.
  - *Leyendas:* Lago Ness, Santuario de Las Lajas (ancla en Nariño, Colombia).
- **Ficha de Detalle Rigurosa (Rúbrica "Ser"):** Separa claramente los hechos verificados por la arqueología e historia (*"Qué se sabe"*) de las incógnitas científicas abiertas (*"El misterio"*), clasificando cada sitio en:
  - 🟢 **Consenso científico**
  - 🟡 **En debate**
  - 🟣 **Leyenda**
- **Buscador Inteligente sin Acentos:** Normalización de texto (`java.text.Normalizer`) que permite buscar por nombre, país, cultura o palabra clave (ej. *"pascua"*, *"peru"*, *"geoglifos"*, *"muones"*).
- **Filtros por Categoría:** Fila horizontal de `FilterChips` con conteo en vivo de resultados.
- **Sincronización Bidireccional:**
  - Tocar un marcador en el mapa desplaza el carrusel a su tarjeta correspondiente.
  - Deslizar el carrusel o presionar *"Volar"* anima la cámara del mapa hacia el lugar seleccionado con nivel de zoom óptimo.
- **Controles Intuitivos:** Botones flotantes de Zoom (+/−), botón de *"Vista Mundial"* (para regresar a la vista planetaria) y botón *"¡Sorpréndeme!"* (selección aleatoria de un misterio).
- **Diseño UI/UX Inmersivo:** Tema oscuro Material 3 con paleta de medianoche (`#0B1020`), superficie profunda (`#141B34`), acentos dorados (`#F5B83D`) y violetas (`#7C5CFF`).

---

## 🏗️ Arquitectura del Proyecto

El proyecto sigue una arquitectura **MVVM (Model-View-ViewModel)** reactiva con Jetpack Compose y Kotlin Coroutines:

```
app/src/main/java/com/example/
├── MainActivity.kt                      # Punto de entrada, configuración Edge-to-Edge y tema
├── data/
│   ├── model/
│   │   └── MysteryPlace.kt              # Modelos: MysteryPlace, Category, EvidenceLevel
│   └── repository/
│       └── PlacesRepository.kt          # Catálogo local in-memory de los 15 lugares
├── ui/
│   ├── EnigmaAtlasScreen.kt             # Orquestador principal Compose (mapa + controles + carrusel)
│   ├── components/
│   │   ├── TopSearchBarAndFilters.kt    # Buscador en vivo y chips de categorías
│   │   ├── PlaceCardsCarousel.kt        # Carrusel horizontal con tarjetas interactivas
│   │   ├── PlaceDetailBottomSheet.kt    # ModalBottomSheet con rigor arqueológico y coordenadas
│   │   └── MapFloatingControls.kt       # Controles flotantes de zoom y banner offline
│   ├── map/
│   │   └── OsmMapView.kt                # Envoltorio Composable de osmdroid MapView con pins dinámicos
│   ├── theme/
│   │   ├── Color.kt                     # Paleta de colores temáticos y categorías
│   │   ├── Theme.kt                     # EnigmaAtlasTheme (Material 3 Dark)
│   │   └── Type.kt                      # Tipografía de la aplicación
│   └── viewmodel/
│       └── MapViewModel.kt              # Estado de la UI (StateFlow), lógica de búsqueda y selección
└── util/
    └── TextNormalizer.kt                # Normalización de acentos, diacríticos y mayúsculas
```

---

## 🧪 Pruebas Unitarias

El proyecto incluye pruebas unitarias completas en `app/src/test/java/com/example/`:
- `EnigmaAtlasTests.kt`:
  - `testTextNormalizer_removesAccentsAndLowercases`: Verifica eliminación de tildes ("Perú" -> "peru", "Chichén Itzá" -> "chichen itza").
  - `testCatalogIntegrity_fifteenPlacesValidCoordinatesAndUniqueIds`: Verifica que las 15 ubicaciones tienen IDs únicos, campos completos y coordenadas en rangos WGS84 válidos `[-90, 90]` y `[-180, 180]`.
  - `testSearchQuery_matchesWithoutAccentsOrCase`: Valida búsquedas por término ("pascua", "stonehenge", "geoglifos").
  - `testCategoryFilter`: Valida el filtrado por cada una de las 4 categorías.
  - `testCombinedSearchAndCategoryFilter`: Valida la conjunción de búsqueda + categoría.
- `ExampleRobolectricTest.kt`: Prueba con Robolectric para validar recursos en el contexto de Android.

Para ejecutar las pruebas:
```bash
gradle :app:testDebugUnitTest
```

---

## 📜 Licencias y Fuentes

- Documentación detallada de fuentes bibliográficas y arqueológicas: [`docs/FUENTES.md`](docs/FUENTES.md).
- Lista de verificación de pruebas manuales: [`docs/PRUEBAS.md`](docs/PRUEBAS.md).
- Mapas © Colaboradores de OpenStreetMap (bajo licencia ODbL).
