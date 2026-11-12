# PROMPT — App Android "Enigma Atlas": buscador de lugares misteriosos del mundo

> Cómo usarlo: pega todo el contenido (desde "ROL") en tu asistente de código junto con el proyecto `pasto-maps.zip` descomprimido. El prompt asume que la base técnica es ese proyecto (Kotlin + XML + ViewBinding + MVVM + osmdroid) y la transforma al nuevo contexto.

---

## 1. ROL

Actúa como desarrollador Android senior (Kotlin) y diseñador UI/UX móvil. Vas a adaptar el proyecto base "Pasto Maps" a una app nueva, funcional, sin crashes, bien documentada y lista para sustentar en clase.

## 2. CONTEXTO ACADÉMICO

- Curso: Desarrollo de Aplicaciones Móviles, Universidad de Nariño. Taller "Mapas y publicación".
- Rúbrica: **Saber 40 %** (dominio conceptual y práctico de mapas y publicación), **Hacer 40 %** (código funcional, limpio, sin errores de compilación ni crashes; implementación del ejercicio práctico; capacidad de justificar el código), **Ser 20 %** (entrega a tiempo, commits ordenados en Git, rigor en pruebas, ética en el manejo de datos y fuentes).
- Entregables: (a) código en repositorio Git/GitHub, (b) documento con la investigación del proceso de publicación en Android e iOS y de la implementación de mapas, (c) socialización de 15 minutos el **12 de noviembre** con la app funcionando.
- Todo texto visible al usuario, comentarios KDoc y documentación van **en español**.

## 3. OBJETIVO DE LA APP

**Enigma Atlas** es un buscador de lugares turísticos misteriosos del mundo sobre un mapa interactivo. El usuario escribe un nombre, país o tema (por ejemplo "moai", "Stonehenge", "Perú", "geoglifos"), el mapa vuela al lugar y aparece una ficha con lo que **se sabe** y cuál es el **misterio**. Debe sentirse como una app "de exploración nocturna": oscura, atmosférica y llamativa, pero rigurosa con la información.

## 4. RESTRICCIONES TÉCNICAS (heredadas del zip, no negociables)

1. **Mapas: OpenStreetMap con `org.osmdroid:osmdroid-android:6.1.20`.** Prohibido Google Maps SDK, claves de API o cuentas de facturación.
2. Kotlin, **XML + Material Components 3 + ViewBinding** (no migrar la UI a Compose), arquitectura **MVVM** con `ViewModel` + `StateFlow`.
3. `minSdk 24`, `targetSdk 36`, Gradle Kotlin DSL y Version Catalog (`libs.versions.toml`). No modificar los bloques `signingConfigs` ni la asignación de `signingConfig` de `app/build.gradle.kts`.
4. Cumplir la política de uso de tiles de OSM: configurar `Configuration.getInstance().userAgentValue = packageName` **antes** de inflar la vista del mapa.
5. Delegar `onResume()` y `onPause()` al `MapView`. Permisos mínimos: `INTERNET` y `ACCESS_NETWORK_STATE`. **No pedir ubicación** (la app no la necesita; menos datos de usuario = mejor ética).
6. Mostrar siempre la atribución "© Colaboradores de OpenStreetMap (ODbL)".
7. Limpieza del build: el scaffold del zip trae dependencias que no se usan (Firebase, `google-services`, Compose, `play-services-location`, Room, Retrofit, Moshi). Elimina las que no se necesiten para que Gradle sincronice sin advertencias. Reemplaza `android.preference.PreferenceManager` (obsoleto) por `getSharedPreferences` o `androidx.preference`.
8. Sin imágenes remotas por defecto (evita problemas de derechos de autor y dependencia de red). Usa iconos vectoriales por categoría, degradados y tipografía para lograr el impacto visual.

## 5. FUNCIONALIDADES

**F1 — Buscador.** Barra de búsqueda superior (Material `TextInputLayout`/`SearchBar`) con filtrado en vivo por nombre, país, categoría y palabras clave. La búsqueda ignora tildes y mayúsculas ("pascua" encuentra "Isla de Pascua"). Muestra el estado vacío "No encontramos ese misterio… todavía".

**F2 — Filtros por categoría.** Fila horizontal de `Chip`s (selección única): Todos · Megalitos · Ciudades antiguas · Enigmas · Leyendas.

**F3 — Mapa.** `MapView` con tiles Mapnik en **modo oscuro** (`mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)`), un `Marker` por lugar con icono y color según categoría, zoom inicial mundial (zoom ≈ 2.5, centro lat 20.0 / lon 0.0), `setMultiTouchControls(true)` y límites de zoom 2–18. Al tocar un marcador se selecciona el lugar.

**F4 — Carrusel de tarjetas.** `RecyclerView` horizontal anclado abajo (con `PagerSnapHelper`) con los resultados del filtro. Deslizar el carrusel mueve el mapa al lugar (`animateTo`, zoom 12–15); tocar un marcador desplaza el carrusel a su tarjeta. Mapa y carrusel siempre sincronizados.

**F5 — Ficha de detalle.** Al tocar una tarjeta se abre un `BottomSheet` con: nombre, país, categoría, coordenadas, texto **"Qué se sabe"**, texto **"El misterio"**, y una etiqueta de rigor: `Consenso científico` / `En debate` / `Leyenda`. Botón "Volar al lugar".

**F6 — Controles.** FABs de zoom (+/−), botón "Vista mundial" (regresa al zoom inicial) y botón "Sorpréndeme" (elige un lugar aleatorio y vuela hacia él).

**F7 — Robustez.** Sin conexión: `Snackbar` informativo (usa `ConnectivityManager`) sin crashear. La selección y el texto de búsqueda sobreviven a rotaciones (viven en el `ViewModel`). Cero `!!`, manejo seguro de nulos.

## 6. DISEÑO UI/UX ("llamativo")

- Tema **Material 3 oscuro** propio: fondo `#0B1020`, superficie `#141B34`, primario violeta `#7C5CFF`, acento dorado `#F5B83D`, texto `#E8ECF8`. Mantén `@color/accent_gold` del zip. Un color por categoría para marcadores y chips.
- Tarjetas con esquinas de 20 dp, degradado sutil por categoría, icono grande, elevación suave; el título en tipografía con carácter (p. ej. Google Fonts "Cinzel" descargable o `serif` del sistema como respaldo) y el cuerpo en sans-serif.
- Animaciones breves: aparición escalonada de tarjetas, `animateTo` fluido en el mapa, transición del `BottomSheet`.
- Cabecera flotante tipo vidrio (panel semitransparente) con el nombre "Enigma Atlas" y el buscador.
- Accesibilidad: `contentDescription` en iconos y FABs, contraste AA, textos desde `strings.xml` (nada hardcodeado).

## 7. DATOS (catálogo local en `PlacesRepository`, sin red ni base de datos)

Modelo (reemplaza/extiende `PlaceOfInterest`):

```kotlin
data class MysteryPlace(
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val category: Category,        // MEGALITOS, CIUDADES_ANTIGUAS, ENIGMAS, LEYENDAS
    val whatWeKnow: String,
    val theMystery: String,
    val evidence: EvidenceLevel,   // CONSENSO, EN_DEBATE, LEYENDA
    val keywords: List<String>
)
```

Catálogo mínimo (15 lugares). Las coordenadas están en grados decimales (WGS84) y son aproximadas: verifica cada una colocando el marcador en el mapa y contrastando con la fuente oficial o con OSM antes de la entrega.

| id | Lugar | País | Lat | Lon | Categoría | Evidencia |
|---|---|---|---|---|---|---|
| rano_raraku | Moai de Rano Raraku (Isla de Pascua / Rapa Nui) | Chile | -27.1258 | -109.2869 | Megalitos | Consenso |
| tongariki | Ahu Tongariki (Isla de Pascua / Rapa Nui) | Chile | -27.1258 | -109.2770 | Megalitos | Consenso |
| stonehenge | Stonehenge | Reino Unido | 51.1789 | -1.8262 | Megalitos | En debate |
| gobekli_tepe | Göbekli Tepe | Turquía | 37.2233 | 38.9225 | Megalitos | En debate |
| carnac | Alineamientos de Carnac | Francia | 47.5842 | -3.0786 | Megalitos | En debate |
| san_agustin | Parque Arqueológico de San Agustín | Colombia | 1.8870 | -76.2900 | Megalitos | En debate |
| machu_picchu | Machu Picchu | Perú | -13.1631 | -72.5450 | Ciudades antiguas | Consenso |
| giza | Gran Pirámide de Giza | Egipto | 29.9792 | 31.1342 | Ciudades antiguas | En debate |
| chichen_itza | Chichén Itzá | México | 20.6843 | -88.5678 | Ciudades antiguas | Consenso |
| teotihuacan | Teotihuacán | México | 19.6925 | -98.8438 | Ciudades antiguas | En debate |
| tiwanaku | Tiwanaku y Puma Punku | Bolivia | -16.5547 | -68.6733 | Ciudades antiguas | En debate |
| nazca | Líneas de Nazca | Perú | -14.7390 | -75.1300 | Enigmas | En debate |
| yonaguni | Monumento de Yonaguni | Japón | 24.4347 | 123.0114 | Enigmas | En debate |
| loch_ness | Lago Ness | Reino Unido | 57.3229 | -4.4244 | Leyendas | Leyenda |
| las_lajas | Santuario de Las Lajas (Ipiales, Nariño) | Colombia | 0.8114 | -77.5858 | Leyendas | Leyenda |

Contenido base de cada ficha (redáctalo en 2–3 frases por campo, tono evocador pero preciso; las palabras clave son libres):

- **Moai de Rano Raraku:** Volcán de tobas volcánicas que sirvió de cantera para la mayoría de los moai de Rapa Nui; allí quedan estatuas a medio tallar (patrimonio UNESCO desde 1995, Parque Nacional Rapa Nui). *Misterio:* cómo se transportaron estatuas de decenas de toneladas por la isla y por qué se detuvo de golpe la talla.
- **Ahu Tongariki:** La plataforma ceremonial más grande de la isla, con 15 moai, a un kilómetro de Rano Raraku; fue derribada por el tsunami de 1960 y restaurada después. *Misterio:* el significado de las alineaciones astronómicas y por qué los moai de la costa miran hacia tierra adentro.
- **Stonehenge:** Monumento neolítico de Salisbury Plain (Wiltshire), construido por fases entre aprox. 3000 y 1500 a. C.; los sarsens (hasta unas 25 toneladas) vienen de Marlborough Downs, a unos 25 km, y las piedras azules de las colinas Preseli, en Gales. *Misterio:* para qué se usó (calendario, ritual, entierros) y cómo se transportaron las piedras azules cientos de kilómetros.
- **Göbekli Tepe:** Santuario con pilares en T esculpidos de hace más de 11.000 años, anterior a la agricultura y la cerámica (UNESCO 2018). *Misterio:* quién lo construyó, con qué fin y por qué fue enterrado deliberadamente.
- **Carnac:** Miles de menhires alineados en Bretaña, levantados en el Neolítico. *Misterio:* su función (astronómica, funeraria o ritual) sigue sin consenso.
- **San Agustín:** Conjunto de estatuas y tumbas monolíticas del sur de Colombia (UNESCO 1995). *Misterio:* se sabe muy poco de la cultura que las talló y de por qué desapareció.
- **Machu Picchu:** Ciudadela inca de mediados del siglo XV en los Andes peruanos (UNESCO 1983); la mayoría de los estudiosos la ven como una residencia real de Pachacútec. *Misterio:* su función exacta y por qué fue abandonada.
- **Giza:** La Gran Pirámide se levantó hacia el 2560 a. C. para Keops. En 2017 el proyecto ScanPyramids detectó un gran vacío interior con muones. *Misterio:* cómo se organizó su construcción y qué hay en ese vacío.
- **Chichén Itzá:** Ciudad maya con El Castillo, cuya escalinata proyecta la sombra de una serpiente durante los equinoccios. *Misterio:* hasta dónde llegó la intención astronómica y acústica del diseño.
- **Teotihuacán:** "Ciudad de los dioses" (nombre azteca; los aztecas la encontraron ya abandonada). *Misterio:* quiénes la fundaron, qué lengua hablaban y por qué declinó.
- **Tiwanaku y Puma Punku:** Centro ceremonial a unos 3.800 m de altitud con bloques de piedra cortados con gran precisión (UNESCO 2000). *Misterio:* cómo se labraron y transportaron esos bloques.
- **Nazca:** Geoglifos gigantes de colibrí, mono y araña, visibles sobre todo desde el aire (UNESCO 1994). *Misterio:* su propósito (ritual, agua, astronomía). No hay evidencia de intervención extraterrestre.
- **Yonaguni:** Formación sumergida con terrazas que parecen escalones. *Misterio:* ¿natural o tallada? La mayoría de los geólogos se inclina por el origen natural; el debate sigue abierto.
- **Lago Ness:** Leyenda del "monstruo" desde 1933 (con relatos medievales previos). En 2018 un estudio de ADN ambiental no halló reptil marino y sí mucho ADN de anguila. *Misterio:* por qué la leyenda persiste.
- **Las Lajas:** Basílica neogótica construida sobre el cañón del río Guáitara, junto a Ipiales; la tradición cuenta la aparición de una imagen de la Virgen en la roca. *Misterio:* leyenda y fe que la convirtieron en lugar de peregrinación (sirve de ancla local para la sustentación).

**Regla de rigor (ética, rúbrica "Ser"):** en cada ficha separa hechos verificados de hipótesis. No presentes teorías pseudocientíficas (visitantes extraterrestres, civilizaciones perdidas sin evidencia) como hechos; si se mencionan, etiquétalas como "Leyenda" o "En debate". Cita las fuentes (UNESCO, English Heritage, Parque Nacional Rapa Nui, publicaciones académicas) en `docs/FUENTES.md`.

## 8. ARQUITECTURA Y ARCHIVOS

```
app/src/main/java/com/example/
├── MainActivity.kt                    # Solo orquesta UI, mapa y observación de estado
├── data/model/MysteryPlace.kt         # Modelo + enums Category y EvidenceLevel
├── data/repository/PlacesRepository.kt# Catálogo local de los 15 lugares
├── ui/viewmodel/MapViewModel.kt       # UiState (query, categoría, resultados, selección) en StateFlow
├── ui/adapter/PlaceCardAdapter.kt     # RecyclerView del carrusel
├── ui/PlaceDetailBottomSheet.kt       # Ficha de detalle
├── ui/map/MarkerFactory.kt            # Crea Markers por categoría
└── util/TextNormalizer.kt             # Normaliza tildes y mayúsculas para la búsqueda
res/ (layouts, drawables vectoriales por categoría, colors.xml, strings.xml en español, themes.xml M3 oscuro)
```

- La lógica de filtrado vive en el `ViewModel`/repositorio, no en la Activity. Recolecta los `StateFlow` con `repeatOnLifecycle(STARTED)`.
- Usa corrutinas solo donde haga falta; no bloquees el hilo principal.

## 9. PRUEBAS

- Pruebas unitarias (JUnit) para: normalización de texto, búsqueda ("pascua", "stonehenge", "perú"), filtro por categoría y combinación búsqueda + categoría.
- Una prueba que verifique que todas las coordenadas del catálogo están en rango válido (lat −90..90, lon −180..180) y que no hay ids duplicados.
- Lista de verificación manual en `docs/PRUEBAS.md`: rotación, sin internet, búsqueda sin resultados, 15 marcadores, selección sincronizada, onPause/onResume.

## 10. DOCUMENTACIÓN A GENERAR (parte "Saber")

1. `README.md` actualizado al nuevo contexto: justificación técnica (por qué osmdroid/OSM frente a Google Maps, ViewBinding, MVVM, política de tiles), estructura, cómo ejecutar, errores comunes y soluciones.
2. `docs/PUBLICACION.md` con la investigación (verificada contra documentación oficial, con enlaces):
   - **Android:** cuenta de Google Play Console, tarifa de registro, firma de la app (Play App Signing, keystore), generar **AAB**, `versionCode/versionName`, ficha de la tienda (capturas, icono, descripción), política de privacidad, formulario de Seguridad de los datos, clasificación de contenido, pistas de prueba (interna, cerrada, abierta, producción) y requisitos de prueba para cuentas personales nuevas.
   - **iOS:** Apple Developer Program, Xcode, certificados y perfiles de aprovisionamiento, App Store Connect, TestFlight, revisión de la App Store y sus guías.
   - Comparativa Android vs iOS en una tabla (costos, tiempos, requisitos, revisión).
   - **Implementación de mapas:** comparación OSM/osmdroid vs Google Maps SDK vs Mapbox (costos, privacidad, licencias, offline).
   - **Advertencia de producción:** los servidores públicos `tile.openstreetmap.org` no están pensados para uso comercial intensivo; para publicar a gran escala se recomienda un proveedor de tiles contratado o propio. Explica cómo cambiaría el `TileSource`.
3. `docs/FUENTES.md` con las fuentes de cada ficha y las licencias (ODbL para OSM, etc.).
4. `docs/GUION_SOCIALIZACION.md`: guion de 15 minutos (2 min contexto, 5 min proceso de publicación Android/iOS, 5 min demo de la app, 2 min arquitectura y preguntas) con las preguntas probables del docente y respuestas breves ("¿por qué osmdroid?", "¿cómo manejas el ciclo de vida del mapa?", "¿cómo publicarías esto?").

## 11. GIT (parte "Ser")

- `.gitignore` que excluya `.env`, `*.jks`, `local.properties`, `build/`.
- Commits pequeños, con mensajes en español y prefijos tipo Conventional Commits, por ejemplo: `feat: agrega catálogo de lugares`, `feat: buscador con filtrado sin tildes`, `docs: guía de publicación Android e iOS`, `test: pruebas del filtro`.
- Propón la secuencia de commits sugerida al final.

## 12. CRITERIOS DE ACEPTACIÓN

- [ ] Compila y corre en un emulador API 24+ sin crashes ni advertencias de dependencias.
- [ ] Buscar "pascua", "stonehenge" o "perú" filtra el carrusel y los marcadores, y el mapa vuela al resultado.
- [ ] Mapa oscuro, 15 marcadores por categoría, atribución OSM visible.
- [ ] Ficha de detalle con "Qué se sabe", "El misterio" y etiqueta de evidencia.
- [ ] Sobrevive a rotación y a pérdida de conexión.
- [ ] Pruebas unitarias en verde.
- [ ] README y carpeta `docs/` completos, en español.

## 13. FORMATO DE LA RESPUESTA

1. Primero, un plan breve (5–8 líneas) con las decisiones de diseño y los supuestos.
2. Luego, el código **archivo por archivo**, indicando la ruta completa, empezando por `libs.versions.toml` y `app/build.gradle.kts` limpios.
3. Después, los documentos de `docs/` y el `README.md`.
4. Al final, los pasos para ejecutar, la lista de verificación manual y la secuencia de commits.
5. No inventes APIs: si algo de osmdroid o Material no existe en esas versiones, dilo y propone la alternativa más cercana.
