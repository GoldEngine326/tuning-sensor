package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalibrationPoint
import com.example.model.CharType
import com.example.model.CharacteristicInfo
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TunerCyan
import java.util.Locale

@Composable
fun CharacteristicsView(
  characteristic: CharacteristicInfo,
  modifier: Modifier = Modifier
) {
  val isResistance = characteristic.type == CharType.RESISTANCE_TEMP

  val conciseInputHeader = remember(characteristic.inputLabel, isResistance) {
    if (isResistance) {
      "Temperatura (°C)"
    } else {
      "Wartość wyjściowa"
    }
  }

  val conciseOutputHeader = remember(characteristic.outputLabel, isResistance) {
    if (isResistance) {
      "Rezystancja (Ω)"
    } else {
      "Wartość wejściowa"
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    // Nagłówek sekcji
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = if (isResistance) Icons.Default.Thermostat else Icons.Default.ShowChart,
        contentDescription = null,
        tint = if (isResistance) TunerAmber else TunerCyan,
        modifier = Modifier.size(22.dp)
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = characteristic.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
    }

    Spacer(Modifier.height(10.dp))

    // 1. Tabela z wyraźnymi liniami siatki (Grid lines)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderStroke)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        if (isResistance) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TableChart, contentDescription = null, tint = TunerAmber, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Tabela charakterystyki (${conciseInputHeader} ➔ ${conciseOutputHeader})",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextSecondary
            )
          }
          Spacer(Modifier.height(8.dp))
        }

        // Siatka tabeli z wyrazistymi liniami kolumn i wierszy
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, BorderStroke, RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
          // Nagłówek tabeli: Wartość wyjściowa (niebieski) | Wartość wejściowa (pomarańczowy)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(DarkSurfaceVariant, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
              .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = conciseInputHeader,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = TunerCyan,
              modifier = Modifier.weight(1f)
            )
            // Pionowa linia podziału w nagłówku
            Box(
              modifier = Modifier
                .width(1.5.dp)
                .height(20.dp)
                .background(BorderStroke)
            )
            Text(
              text = conciseOutputHeader,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = TunerAmber,
              modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
            )
          }

          // Pozioma linia oddzielająca nagłówek od wierszy
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.5.dp)
              .background(BorderStroke)
          )

          // Wiersze tabeli z liniami: lewa kolumna w kolorze niebieskim (TunerCyan), prawa w kolorze pomarańczowym (TunerAmber)
          characteristic.points.forEachIndexed { idx, point ->
            val isEven = idx % 2 == 0
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (isEven) CarbonBlack.copy(alpha = 0.4f) else DarkSurfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${point.inputValue} ${point.inputUnit}".trim(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TunerCyan,
                modifier = Modifier.weight(1f)
              )
              // Pionowa linia podziału kolumn
              Box(
                modifier = Modifier
                  .width(1.5.dp)
                  .height(22.dp)
                  .background(BorderStroke)
              )
              Text(
                text = "${point.outputValue} ${point.outputUnit}".trim(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TunerAmber,
                modifier = Modifier
                  .weight(1f)
                  .padding(start = 12.dp)
              )
            }

            if (idx < characteristic.points.size - 1) {
              // Pozioma linia siatki pomiędzy wierszami
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(1.dp)
                  .background(BorderStroke)
              )
            }
          }
        }

        // 2. Wykres z punktów tabeli
        if (characteristic.points.size >= 2) {
          Spacer(Modifier.height(16.dp))
          if (isResistance) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.ShowChart, contentDescription = null, tint = TunerAmber, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                text = "Wykres charakterystyki NTC (Oś X: Rezystancja [Ω] ➔ Oś Y: Temperatura [°C])",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
              )
            }
            Spacer(Modifier.height(8.dp))
          }

          CharacteristicChart(
            points = characteristic.points,
            isResistance = isResistance,
            xUnit = conciseInputHeader,
            yUnit = conciseOutputHeader,
            modifier = Modifier.fillMaxWidth()
          )
        }

        if (isResistance) {
          Spacer(Modifier.height(10.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = TunerAmber,
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = "Charakterystyka: Ujemna temperaturowa (NTC)",
              fontWeight = FontWeight.SemiBold,
              fontSize = 12.sp,
              color = TunerAmber
            )
          }
        } else if (characteristic.type == CharType.VOLTAGE_PRESSURE) {
          Spacer(Modifier.height(12.dp))
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, TunerCyan.copy(alpha = 0.35f))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = TunerCyan,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "Charakterystyka czujnika: Liniowa napięciowa (Analog 0–5V)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.5.sp,
                  color = TunerCyan
                )
              }
              Spacer(Modifier.height(6.dp))
              Text(
                text = "Napięcie wyjściowe (0–5V) wzrasta proporcjonalnie do mierzonego ciśnienia (kPa). Punkty kalibracyjne z tabeli należy wprowadzić do oprogramowania ECU, aby dokładnie zdefiniować skalowanie MAP sensora.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
              )
            }
          }
        }

        // Dodatkowe notatki tunera (bez automatycznych wzorów pull-up)
        if (characteristic.formulaOrNotes.isNotBlank() &&
          !characteristic.formulaOrNotes.contains("pull-up", ignoreCase = true) &&
          !characteristic.formulaOrNotes.contains("pullup", ignoreCase = true) &&
          !characteristic.formulaOrNotes.contains("dzielnik", ignoreCase = true)
        ) {
          Spacer(Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
              .padding(10.dp)
          ) {
            Text(
              text = characteristic.formulaOrNotes,
              fontSize = 12.sp,
              color = TextSecondary,
              lineHeight = 18.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun DeadTimeTablesView(
  tables: List<com.example.model.DeadTimeTable>,
  modifier: Modifier = Modifier
) {
  if (tables.isEmpty()) return

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Speed,
          contentDescription = null,
          tint = TunerAmber,
          modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "Tabela Dead Time wtryskiwacza (Czas martwy)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }

      Spacer(Modifier.height(4.dp))
      Text(
        text = "Kompensacja opóźnienia otwarcia wtryskiwacza w funkcji napięcia instalacji (V). Wartości w milisekundach (ms).",
        fontSize = 11.5.sp,
        color = TextSecondary,
        lineHeight = 16.sp
      )

      tables.forEachIndexed { tIdx, table ->
        Spacer(Modifier.height(14.dp))

        // Nagłówek dla konkretnej tabeli ciśnienia paliwa
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TableChart, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = table.tableName.ifBlank { "Ciśnienie paliwa: Tabela ${tIdx + 1}" },
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = TunerCyan
            )
          }

          if (table.points.isNotEmpty()) {
            Text(
              text = "${table.points.size} pkt",
              fontSize = 11.sp,
              color = TextMuted,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Spacer(Modifier.height(6.dp))

        // Tabela Dead Time
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column {
            // Nagłówki kolumn
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                  text = "Napięcie [V]",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.5.sp,
                  color = TunerCyan
                )
              }

              Box(
                modifier = Modifier
                  .width(1.5.dp)
                  .height(20.dp)
                  .background(BorderStroke)
              )

              Row(
                modifier = Modifier
                  .weight(1f)
                  .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = TunerAmber, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                  text = "Dead Time [ms]",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.5.sp,
                  color = TunerAmber
                )
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(BorderStroke)
            )

            // Wiersze Dead Time
            table.points.forEachIndexed { pIdx, pt ->
              val isEven = pIdx % 2 == 0
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (isEven) CarbonBlack.copy(alpha = 0.4f) else DarkSurfaceVariant.copy(alpha = 0.35f))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (pt.voltage.endsWith("V", ignoreCase = true)) pt.voltage else "${pt.voltage} V",
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  color = TunerCyan,
                  modifier = Modifier.weight(1f)
                )

                Box(
                  modifier = Modifier
                    .width(1.5.dp)
                    .height(20.dp)
                    .background(BorderStroke)
                )

                Text(
                  text = if (pt.timeMs.endsWith("ms", ignoreCase = true)) pt.timeMs else "${pt.timeMs} ms",
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  color = TunerAmber,
                  modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                )
              }

              if (pIdx < table.points.size - 1) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderStroke)
                )
              }
            }
          }
        }

        if (table.notes.isNotBlank()) {
          Spacer(Modifier.height(6.dp))
          Text(
            text = "• ${table.notes}",
            fontSize = 11.5.sp,
            color = TextSecondary,
            lineHeight = 16.sp
          )
        }
      }
    }
  }
}

private data class ChartPoint(
  val x: Float,
  val y: Float,
  val labelX: String,
  val labelY: String
)

private fun extractFirstNumber(text: String): Float? {
  val clean = text.replace(",", ".")
  val regex = Regex("""[-+]?\d+(?:\.\d+)?""")
  return regex.find(clean)?.value?.toFloatOrNull()
}

private fun extractYValue(text: String, unit: String, isResistance: Boolean): Float? {
  val normalized = text.replace(" ", "").replace("\u00A0", "").replace(",", ".")
  val isKilo = isResistance && (normalized.contains("k", ignoreCase = true) || unit.contains("k", ignoreCase = true))
  val regex = Regex("""[-+]?\d+(?:\.\d+)?""")
  val match = regex.find(normalized)?.value?.toFloatOrNull() ?: return null
  return if (isResistance && isKilo && match < 1000f) {
    match * 1000f
  } else {
    match
  }
}

@Composable
fun CharacteristicChart(
  points: List<CalibrationPoint>,
  isResistance: Boolean,
  xUnit: String,
  yUnit: String,
  modifier: Modifier = Modifier
) {
  val parsedPoints = remember(points, isResistance) {
    points.mapNotNull { pt ->
      val inVal = extractFirstNumber(pt.inputValue) ?: return@mapNotNull null
      val outVal = extractYValue(pt.outputValue, pt.outputUnit, isResistance) ?: return@mapNotNull null
      if (isResistance) {
        // Oś rezystancji ma być poziomo (oś X)!
        // pt.outputValue to Rezystancja w Ω (outVal), pt.inputValue to Temperatura w °C (inVal)
        ChartPoint(x = outVal, y = inVal, labelX = pt.outputValue, labelY = pt.inputValue)
      } else {
        ChartPoint(x = inVal, y = outVal, labelX = pt.inputValue, labelY = pt.outputValue)
      }
    }.sortedBy { it.x }
  }

  if (parsedPoints.size < 2) return

  val curveColor = if (isResistance) TunerAmber else TunerCyan

  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = CarbonBlack),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
    ) {
      Canvas(modifier = Modifier.size(280.dp).padding(8.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val padLeft = 44.dp.toPx()
        val padRight = 14.dp.toPx()
        val padTop = 14.dp.toPx()
        val padBottom = 26.dp.toPx()

        // Kwadratowy obszar kreślenia (równa szerokość i wysokość siatki)
        val plotDim = minOf(canvasWidth - padLeft - padRight, canvasHeight - padTop - padBottom)
        val plotWidth = plotDim
        val plotHeight = plotDim

        if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

        val minX = parsedPoints.minOf { it.x }
        val maxX = parsedPoints.maxOf { it.x }
        val minY = parsedPoints.minOf { it.y }
        val maxY = parsedPoints.maxOf { it.y }

        val spanX = if (maxX > minX) maxX - minX else 1f
        val spanY = if (maxY > minY) maxY - minY else 1f

        val textPaint = Paint().apply {
          color = android.graphics.Color.GRAY
          textSize = 9.sp.toPx()
          isAntiAlias = true
        }

        val accentPaint = Paint().apply {
          color = if (isResistance) android.graphics.Color.parseColor("#FFC107") else android.graphics.Color.parseColor("#00E5FF")
          textSize = 9.sp.toPx()
          isAntiAlias = true
          typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // Kwadratowa siatka: dokładnie taka sama liczba podziałów na osi X i Y (5 pól = kwadratowe kratki)
        val xEndUnit = if (isResistance) "Ω" else (points.firstOrNull()?.inputUnit?.ifBlank { "kPa" } ?: "kPa")
        val yEndUnit = if (isResistance) "°C" else (points.firstOrNull()?.outputUnit?.ifBlank { "V" } ?: "V")

        // Kwadratowa siatka: dokładnie taka sama liczba podziałów na osi X i Y
        val gridDivisions = 4
        for (i in 0..gridDivisions) {
          val frac = i.toFloat() / gridDivisions
          val yPos = padTop + plotHeight * (1f - frac)
          val xPos = padLeft + plotWidth * frac

          // Pozioma linia siatki
          drawLine(
            color = Color(0xFF2C2D35),
            start = Offset(padLeft, yPos),
            end = Offset(padLeft + plotWidth, yPos),
            strokeWidth = 1.dp.toPx()
          )

          // Pionowa linia siatki (kwadratowa odległość równa poziomej)
          drawLine(
            color = Color(0xFF2C2D35),
            start = Offset(xPos, padTop),
            end = Offset(xPos, padTop + plotHeight),
            strokeWidth = 1.dp.toPx()
          )

          // Etykieta osi Y - wartości bez jednostek
          val valueAtGridY = minY + spanY * frac
          val yLabel = if (isResistance) {
            String.format(Locale.US, "%.0f", valueAtGridY)
          } else {
            String.format(Locale.US, "%.1f", valueAtGridY)
          }
          drawContext.canvas.nativeCanvas.drawText(yLabel, 8.dp.toPx(), yPos + 3.dp.toPx(), textPaint)

          // Etykieta osi X - wartości bez jednostek
          val valueAtGridX = minX + spanX * frac
          val xLabel = if (isResistance) {
            if (valueAtGridX >= 1000f) String.format(Locale.US, "%.1fk", valueAtGridX / 1000f) else String.format(Locale.US, "%.0f", valueAtGridX)
          } else {
            String.format(Locale.US, "%.0f", valueAtGridX)
          }
          drawContext.canvas.nativeCanvas.drawText(
            xLabel,
            xPos - 8.dp.toPx(),
            padTop + plotHeight + 16.dp.toPx(),
            textPaint
          )
        }

        // Jednostki na końcach osi
        // Jednostka osi Y na samej górze
        drawContext.canvas.nativeCanvas.drawText(
          "[$yEndUnit]",
          padLeft - 6.dp.toPx(),
          padTop - 4.dp.toPx(),
          accentPaint
        )
        // Jednostka osi X na prawym końcu
        drawContext.canvas.nativeCanvas.drawText(
          "[$xEndUnit]",
          padLeft + plotWidth + 3.dp.toPx(),
          padTop + plotHeight + 16.dp.toPx(),
          accentPaint
        )

        // Obramowanie kwadratowego pola wykresu
        drawRect(
          color = BorderStroke,
          topLeft = Offset(padLeft, padTop),
          size = androidx.compose.ui.geometry.Size(plotWidth, plotHeight),
          style = Stroke(width = 1.dp.toPx())
        )

        // Ścieżka wykresu
        val linePath = Path()
        val screenPoints = parsedPoints.map { pt ->
          val xPos = padLeft + ((pt.x - minX) / spanX) * plotWidth
          val yPos = padTop + (1f - (pt.y - minY) / spanY) * plotHeight
          Offset(xPos, yPos)
        }

        if (!isResistance) {
          // Dla charakterystyki napięciowej: proste linie między punktami
          screenPoints.forEachIndexed { index, offset ->
            if (index == 0) {
              linePath.moveTo(offset.x, offset.y)
            } else {
              linePath.lineTo(offset.x, offset.y)
            }
          }
        } else {
          // Dla charakterystyki opornościowej: gładkie przejścia (nie kanciaste, ale też nie pofalowane - monotonic cubic)
          if (screenPoints.isNotEmpty()) {
            linePath.moveTo(screenPoints[0].x, screenPoints[0].y)
            for (i in 0 until screenPoints.size - 1) {
              val p0 = screenPoints[maxOf(0, i - 1)]
              val p1 = screenPoints[i]
              val p2 = screenPoints[i + 1]
              val p3 = screenPoints[minOf(screenPoints.size - 1, i + 2)]

              // Monotoniczne nachylenie (Catmull-Rom z ograniczeniem nienadwyżkowym)
              val dx = (p2.x - p1.x) / 3f
              val dy1 = (p2.y - p0.y) / 6f
              val dy2 = (p3.y - p1.y) / 6f

              val cp1 = Offset(p1.x + dx, p1.y + dy1)
              val cp2 = Offset(p2.x - dx, p2.y - dy2)

              linePath.cubicTo(
                cp1.x, cp1.y,
                cp2.x, cp2.y,
                p2.x, p2.y
              )
            }
          }
        }

        // Czysta, czytelna linia wykresu (bez cienia)
        drawPath(
          path = linePath,
          color = curveColor,
          style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
          )
        )

        // Punkty pomiarowe na wykresie (wyraźne, bez rozmytego cienia)
        screenPoints.forEach { offset ->
          drawCircle(
            color = curveColor,
            radius = 3.5.dp.toPx(),
            center = offset
          )
          drawCircle(
            color = Color.White,
            radius = 1.5.dp.toPx(),
            center = offset
          )
        }
      }
    }
  }
}
