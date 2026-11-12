package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MysteryPlace
import com.example.ui.theme.EnigmaBorder
import com.example.ui.theme.EnigmaCyan
import com.example.ui.theme.EnigmaGold
import com.example.ui.theme.EnigmaGoldBright
import com.example.ui.theme.EnigmaTextMuted
import com.example.ui.theme.EnigmaTextPrimary
import com.example.ui.theme.EnigmaTextSecondary
import com.example.ui.theme.MysteryDarkBackground
import com.example.ui.theme.MysteryDarkSurfaceVariant

/**
 * Ficha de detalle completa en formato BottomSheet modal.
 * Muestra fotografía representativa del lugar, qué se sabe (hechos), el enigma aún no resuelto
 * y el nivel de evidencia con una cuidada jerarquía visual y estética nocturna.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaceDetailBottomSheet(
    place: MysteryPlace,
    onDismiss: () -> Unit,
    onFlyToPlace: (MysteryPlace) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF090E1F),
        scrimColor = Color.Black.copy(alpha = 0.72f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.5.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(EnigmaGold.copy(alpha = 0.4f), EnigmaBorder, EnigmaGold.copy(alpha = 0.4f))
                        ),
                        shape = CircleShape
                    )
            )
        },
        modifier = modifier.testTag("detail_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Tira de acento sutil con el color de la categoría
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, place.category.color, Color.Transparent)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fotografía representativa del lugar (Hero Image Banner)
            if (place.imageRes != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .shadow(10.dp, RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = place.imageRes),
                        contentDescription = place.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Degradado sombreado para dar profundidad nocturna y resaltar el contenido
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x55090E1F),
                                        Color(0xD9090E1F)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Cabecera con Categoría, Nivel de Evidencia y Botón Cerrar
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
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(place.category.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = place.category.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = place.category.color
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    EvidenceBadge(evidence = place.evidence)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = EnigmaTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título y País
            Text(
                text = place.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp
                ),
                color = EnigmaTextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = EnigmaGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = place.country,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = EnigmaTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Coordenadas WGS84 en cápsula con botón de copiado
            Card(
                colors = CardDefaults.cardColors(containerColor = MysteryDarkSurfaceVariant.copy(alpha = 0.75f)),
                border = BorderStroke(1.dp, EnigmaBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = null,
                            tint = EnigmaGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("%.4f° N/S,  %.4f° E/O", place.latitude, place.longitude),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.4.sp
                            ),
                            color = EnigmaTextPrimary
                        )
                    }

                    Surface(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Coordenadas", "${place.latitude}, ${place.longitude}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Coordenadas copiadas", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33F6C14B),
                        border = BorderStroke(1.dp, EnigmaGold.copy(alpha = 0.4f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar coordenadas",
                                tint = EnigmaGold,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN: QUÉ SE SABE (Hechos verificados)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x2B10B981) // Resplandor verde esmeralda
                ),
                border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Qué se sabe (hechos verificados)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            color = Color(0xFF34D399)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = place.whatWeKnow,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = EnigmaTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECCIÓN: EL MISTERIO (El enigma aún sin resolver)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x2BF6C14B) // Resplandor dorado místico
                ),
                border = BorderStroke(1.2.dp, EnigmaGold.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = EnigmaGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "El misterio",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            color = EnigmaGold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = place.theMystery,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = EnigmaTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Descripción del nivel de evidencia
            Surface(
                color = place.evidence.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, place.evidence.color.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Criterio de rigor: ${place.evidence.description}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = place.evidence.color,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Palabras clave
            Text(
                text = "Palabras clave",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = EnigmaTextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                place.keywords.forEach { keyword ->
                    Surface(
                        color = MysteryDarkSurfaceVariant.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EnigmaBorder)
                    ) {
                        Text(
                            text = "#$keyword",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = EnigmaTextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón de acción principal: Volar al lugar en el mapa (ancho completo)
            Button(
                onClick = {
                    onFlyToPlace(place)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = EnigmaGold)
                    .testTag("btn_fly_from_sheet"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnigmaGold,
                    contentColor = Color(0xFF0F172A)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Volar al lugar en el mapa",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
