package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderStroke as BorderColor
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TunerCyan
import com.example.ui.theme.TuningRed
import java.util.Locale
import kotlin.math.hypot

/**
 * Karta wymiarów i geometrii otworów montażowych przepustnicy (Throttle Body).
 * Prezentuje średnicę wewnętrzną, zewnętrzną, rozstawy śrub montażowych oraz przekątne,
 * wraz z profesjonalnym rysunkiem technicznym 2D CAD / Blueprint.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThrottleDimensionsCard(
  innerDiameterMm: Double?,
  outerDiameterMm: Double?,
  boltSpacingWidthMm: Double?,
  boltSpacingHeightMm: Double?,
  boltDiagonal1Mm: Double?,
  boltDiagonal2Mm: Double?,
  modifier: Modifier = Modifier
) {
  val hasDiameters = innerDiameterMm != null || outerDiameterMm != null
  val hasBoltPattern = boltSpacingWidthMm != null || boltSpacingHeightMm != null || boltDiagonal1Mm != null

  if (!hasDiameters && !hasBoltPattern) return

  val safeWidth = boltSpacingWidthMm ?: 60.0
  val safeHeight = boltSpacingHeightMm ?: 60.0
  val pythagorasDiagonal = hypot(safeWidth, safeHeight)
  val effectiveDiag1 = boltDiagonal1Mm ?: if (boltSpacingWidthMm != null && boltSpacingHeightMm != null) pythagorasDiagonal else null
  val effectiveDiag2 = boltDiagonal2Mm ?: effectiveDiag1

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = BorderStroke(1.dp, TunerCyan.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // 1. NAGŁÓWEK KARTY
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .background(TunerCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Architecture,
            contentDescription = null,
            tint = TunerCyan,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Wymiary przepustnicy i rozstaw śrub",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Średnica dolotu oraz geometria kołnierza montażowego",
            fontSize = 11.5.sp,
            color = TextSecondary
          )
        }
      }

      Spacer(Modifier.height(14.dp))

      // 2. KAFELKI GŁÓWNYCH WYMIARÓW
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Średnica wewnętrzna (gardziel)
        if (innerDiameterMm != null) {
          DimensionBadge(
            label = "Średnica wewnętrzna",
            sublabel = "Gardziel (Bore)",
            value = "${formatDim(innerDiameterMm)} mm",
            accentColor = TunerCyan,
            modifier = Modifier.weight(1f)
          )
        }

        // Średnica zewnętrzna (pod wąż / dolot)
        if (outerDiameterMm != null) {
          val inchEstimate = outerDiameterMm / 25.4
          DimensionBadge(
            label = "Średnica zewnętrzna",
            sublabel = "Dolot (~${String.format(Locale.US, "%.1f", inchEstimate)}\")",
            value = "${formatDim(outerDiameterMm)} mm",
            accentColor = TunerAmber,
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 3. ROZMIESZCZENIE OTWORÓW NA ŚRUBY (BOKI I PRZEKĄTNE)
      if (hasBoltPattern) {
        Spacer(Modifier.height(10.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Straighten,
                contentDescription = null,
                tint = TunerAmber,
                modifier = Modifier.size(16.dp)
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = "ROZSTAW ŚRUB MONTAŻOWYCH (OD ŚRODKA DO ŚRODKA ŚRUBY)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = TunerAmber,
                letterSpacing = 0.5.sp
              )
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (boltSpacingWidthMm != null) {
                BoltItemPill(
                  label = "Bok 1 (szerokość)",
                  value = "${formatDim(boltSpacingWidthMm)} mm",
                  accentColor = TextPrimary
                )
              }
              if (boltSpacingHeightMm != null) {
                BoltItemPill(
                  label = "Bok 2 (wysokość)",
                  value = "${formatDim(boltSpacingHeightMm)} mm",
                  accentColor = TextPrimary
                )
              }
              if (effectiveDiag1 != null) {
                BoltItemPill(
                  label = "Przekątna D1",
                  value = "${formatDim(effectiveDiag1)} mm",
                  accentColor = TunerCyan
                )
              }
              if (boltDiagonal2Mm != null && Math.abs(boltDiagonal2Mm - (effectiveDiag1 ?: 0.0)) > 0.1) {
                BoltItemPill(
                  label = "Przekątna D2",
                  value = "${formatDim(boltDiagonal2Mm)} mm",
                  accentColor = TunerCyan
                )
              }
            }
          }
        }
      }

      Spacer(Modifier.height(14.dp))

      // 4. TECHNICZNY RYSUNEK 2D (BLUEPRINT CAD)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(CarbonBlack)
          .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "SCHEMAT TECHNICZNY FLANSZY I DOLOTU",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = TextMuted,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "Wymiary w [mm]",
              fontSize = 10.5.sp,
              color = TunerCyan,
              fontWeight = FontWeight.Medium
            )
          }

          Spacer(Modifier.height(8.dp))

          // Płótno Canvas rysujące rzut czołowy przepustnicy
          ThrottleBlueprintCanvas(
            innerDiameterMm = innerDiameterMm ?: 68.0,
            outerDiameterMm = outerDiameterMm ?: 76.0,
            spacingWidth = boltSpacingWidthMm ?: 60.0,
            spacingHeight = boltSpacingHeightMm ?: 60.0,
            diagonal = effectiveDiag1 ?: pythagorasDiagonal,
            modifier = Modifier
              .fillMaxWidth()
              .height(210.dp)
          )

          Spacer(Modifier.height(6.dp))

          // Opis legendy pod rysunkiem
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            LegendItem(color = TunerCyan, text = "Gardziel (Bore)")
            LegendItem(color = TunerAmber, text = "Zewnętrzny kołnierz")
            LegendItem(color = StatusGreen, text = "Otwory śrub")
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      // Informacja pomocnicza dla tunera / pasowania kolektora
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
          text = "Pomiary od środka do środka śruby (rozstaw osiowy). Pozwala sprawdzić pasowanie adaptera lub flanszy kolektora dolotowego.",
          fontSize = 10.5.sp,
          color = TextMuted,
          lineHeight = 14.sp
        )
      }
    }
  }
}

@Composable
private fun DimensionBadge(
  label: String,
  sublabel: String,
  value: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(DarkSurface)
      .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
      .padding(12.dp)
  ) {
    Column {
      Text(
        text = label.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 0.5.sp
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = value,
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        color = accentColor,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = sublabel,
        fontSize = 11.sp,
        color = TextMuted
      )
    }
  }
}

@Composable
private fun BoltItemPill(
  label: String,
  value: String,
  accentColor: Color
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(DarkSurfaceVariant)
      .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "$label: ",
        fontSize = 11.sp,
        color = TextSecondary
      )
      Text(
        text = value,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

@Composable
private fun LegendItem(color: Color, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .background(color, RoundedCornerShape(2.dp))
    )
    Spacer(Modifier.width(4.dp))
    Text(
      text = text,
      fontSize = 10.sp,
      color = TextSecondary
    )
  }
}

/**
 * Płótno Canvas renderujące wektorowy rzut czołowy przepustnicy
 */
@Composable
private fun ThrottleBlueprintCanvas(
  innerDiameterMm: Double,
  outerDiameterMm: Double,
  spacingWidth: Double,
  spacingHeight: Double,
  diagonal: Double,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    // Siatka tła CAD (delikatna)
    val gridStep = 24.dp.toPx()
    var gx = 0f
    while (gx < w) {
      drawLine(
        color = Color(0xFF1E2330),
        start = Offset(gx, 0f),
        end = Offset(gx, h),
        strokeWidth = 1f
      )
      gx += gridStep
    }
    var gy = 0f
    while (gy < h) {
      drawLine(
        color = Color(0xFF1E2330),
        start = Offset(0f, gy),
        end = Offset(w, gy),
        strokeWidth = 1f
      )
      gy += gridStep
    }

    // Skalowanie proporcji: dopasowujemy flanszę do dostępnej wysokości
    val maxModelDim = maxOf(outerDiameterMm, spacingWidth * 1.35, spacingHeight * 1.35, 80.0)
    val scale = (h * 0.72f) / maxModelDim.toFloat()

    val rInner = (innerDiameterMm.toFloat() / 2f) * scale
    val rOuter = (outerDiameterMm.toFloat() / 2f) * scale

    val halfBoxW = (spacingWidth.toFloat() / 2f) * scale
    val halfBoxH = (spacingHeight.toFloat() / 2f) * scale

    // 1. Osiowe linie środka (Centerlines)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f, 2f, 6f), 0f)
    drawLine(
      color = Color(0xFF3A4259),
      start = Offset(cx - halfBoxW - 24f, cy),
      end = Offset(cx + halfBoxW + 24f, cy),
      strokeWidth = 1.5f,
      pathEffect = dashEffect
    )
    drawLine(
      color = Color(0xFF3A4259),
      start = Offset(cx, cy - halfBoxH - 24f),
      end = Offset(cx, cy + halfBoxH + 24f),
      strokeWidth = 1.5f,
      pathEffect = dashEffect
    )

    // 2. Kołnierz zewnętrzny (zaokrąglony prostokąt montażowy)
    val flangePad = 14f * (scale / 1.5f).coerceIn(0.8f, 1.8f)
    drawRoundRect(
      color = Color(0xFF181C26),
      topLeft = Offset(cx - halfBoxW - flangePad, cy - halfBoxH - flangePad),
      size = Size((halfBoxW + flangePad) * 2f, (halfBoxH + flangePad) * 2f),
      cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
      color = Color(0xFF2C344A),
      topLeft = Offset(cx - halfBoxW - flangePad, cy - halfBoxH - flangePad),
      size = Size((halfBoxW + flangePad) * 2f, (halfBoxH + flangePad) * 2f),
      cornerRadius = CornerRadius(12f, 12f),
      style = Stroke(width = 1.5f)
    )

    // 3. Zewnętrzny kołnierz dolotu (okrąg przerywany / wąż dolotowy)
    drawCircle(
      color = TunerAmber.copy(alpha = 0.85f),
      radius = rOuter,
      center = Offset(cx, cy),
      style = Stroke(
        width = 2.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
      )
    )

    // 4. Wewnętrzny otwór gardzieli (Bore - jasny cyjan)
    drawCircle(
      color = Color(0xFF0F1522),
      radius = rInner,
      center = Offset(cx, cy)
    )
    drawCircle(
      color = TunerCyan,
      radius = rInner,
      center = Offset(cx, cy),
      style = Stroke(width = 3.5f)
    )

    // 5. Motylek przepustnicy (Oś i tarcza wewnątrz gardzieli)
    val axisPath = Path().apply {
      moveTo(cx, cy - rInner * 0.95f)
      lineTo(cx, cy + rInner * 0.95f)
    }
    drawLine(
      color = TunerAmber,
      start = Offset(cx, cy - rInner * 0.95f),
      end = Offset(cx, cy + rInner * 0.95f),
      strokeWidth = 3f,
      cap = StrokeCap.Round
    )
    // Dwie śruby mocujące motylek na osi
    drawCircle(color = TextPrimary, radius = 2.5f, center = Offset(cx, cy - rInner * 0.4f))
    drawCircle(color = TextPrimary, radius = 2.5f, center = Offset(cx, cy + rInner * 0.4f))

    // 6. 4 Otwory na śruby montażowe w rogach
    val boltHoles = listOf(
      Offset(cx - halfBoxW, cy - halfBoxH),
      Offset(cx + halfBoxW, cy - halfBoxH),
      Offset(cx + halfBoxW, cy + halfBoxH),
      Offset(cx - halfBoxW, cy + halfBoxH)
    )

    boltHoles.forEach { pt ->
      // Otwór montażowy
      drawCircle(color = CarbonBlack, radius = 7f, center = pt)
      drawCircle(color = StatusGreen, radius = 7f, center = pt, style = Stroke(width = 2.5f))
      drawCircle(color = StatusGreen, radius = 1.5f, center = pt) // punkt centralny
    }

    // 7. Wymiarowanie poziome: bok 1 (szerokość)
    val dimY = cy - halfBoxH - 12f
    drawLine(
      color = TunerCyan.copy(alpha = 0.8f),
      start = Offset(cx - halfBoxW, dimY),
      end = Offset(cx + halfBoxW, dimY),
      strokeWidth = 1.5f
    )
    // Grot strzałki lewej
    drawLine(color = TunerCyan, start = Offset(cx - halfBoxW, dimY), end = Offset(cx - halfBoxW + 6f, dimY - 4f), strokeWidth = 1.5f)
    drawLine(color = TunerCyan, start = Offset(cx - halfBoxW, dimY), end = Offset(cx - halfBoxW + 6f, dimY + 4f), strokeWidth = 1.5f)
    // Grot strzałki prawej
    drawLine(color = TunerCyan, start = Offset(cx + halfBoxW, dimY), end = Offset(cx + halfBoxW - 6f, dimY - 4f), strokeWidth = 1.5f)
    drawLine(color = TunerCyan, start = Offset(cx + halfBoxW, dimY), end = Offset(cx + halfBoxW - 6f, dimY + 4f), strokeWidth = 1.5f)

    // 8. Wymiarowanie pionowe: bok 2 (wysokość)
    val dimX = cx - halfBoxW - 12f
    drawLine(
      color = TunerAmber.copy(alpha = 0.8f),
      start = Offset(dimX, cy - halfBoxH),
      end = Offset(dimX, cy + halfBoxH),
      strokeWidth = 1.5f
    )
    // Groty pionowe
    drawLine(color = TunerAmber, start = Offset(dimX, cy - halfBoxH), end = Offset(dimX - 4f, cy - halfBoxH + 6f), strokeWidth = 1.5f)
    drawLine(color = TunerAmber, start = Offset(dimX, cy - halfBoxH), end = Offset(dimX + 4f, cy - halfBoxH + 6f), strokeWidth = 1.5f)
    drawLine(color = TunerAmber, start = Offset(dimX, cy + halfBoxH), end = Offset(dimX - 4f, cy + halfBoxH - 6f), strokeWidth = 1.5f)
    drawLine(color = TunerAmber, start = Offset(dimX, cy + halfBoxH), end = Offset(dimX + 4f, cy + halfBoxH - 6f), strokeWidth = 1.5f)

    // 9. Przekątna (Dashed Cyan)
    drawLine(
      color = Color(0xFF64D2FF).copy(alpha = 0.6f),
      start = Offset(cx - halfBoxW, cy - halfBoxH),
      end = Offset(cx + halfBoxW, cy + halfBoxH),
      strokeWidth = 1.5f,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
    )
  }
}

private fun formatDim(v: Double): String {
  return if (v % 1.0 == 0.0) {
    v.toInt().toString()
  } else {
    String.format(Locale.US, "%.1f", v)
  }
}
