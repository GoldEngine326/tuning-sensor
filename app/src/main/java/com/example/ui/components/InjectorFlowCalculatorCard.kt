package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderStroke as BorderColor
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
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Komponent kalkulatora wydajności wtryskiwacza przy zadanym ciśnieniu.
 * Oblicza przeskalowaną wydajność zgodnie z prawem Bernoulliego dla wtryskiwaczy:
 * Q2 = Q1 * sqrt(P2 / P1)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InjectorFlowCalculatorCard(
  nominalFlowRateCc: Double?,
  nominalPressureBar: Double?,
  modifier: Modifier = Modifier
) {
  // Wartości bazowe (z publikacji wtryskiwacza lub domyślne do edycji)
  val baseFlow = nominalFlowRateCc ?: 630.0
  val basePressure = nominalPressureBar ?: 3.0

  // Ciśnienie docelowe podawane przez użytkownika (np. znajomy ma 4.0 bar)
  var targetPressureInput by remember(nominalPressureBar) {
    val initialTarget = if (nominalPressureBar != null && nominalPressureBar == 3.0) "4.0" else "4.0"
    mutableStateOf(initialTarget)
  }

  // Opcjonalna możliwość dostosowania bazy jeśli wtryskiwacz nie miał wpisanej
  var customBaseFlowInput by remember(nominalFlowRateCc) {
    mutableStateOf(if (nominalFlowRateCc != null) "" else "630")
  }
  var customBasePressureInput by remember(nominalPressureBar) {
    mutableStateOf(if (nominalPressureBar != null) "" else "3.0")
  }

  val activeBaseFlow = if (nominalFlowRateCc != null) {
    nominalFlowRateCc
  } else {
    customBaseFlowInput.replace(",", ".").trim().toDoubleOrNull() ?: 630.0
  }

  val activeBasePressure = if (nominalPressureBar != null) {
    nominalPressureBar
  } else {
    customBasePressureInput.replace(",", ".").trim().toDoubleOrNull() ?: 3.0
  }

  val parsedTargetPressure = targetPressureInput.replace(",", ".").trim().toDoubleOrNull()

  // Obliczenie nowej wydajności ze wzoru Bernoulliego
  val calculatedFlow = if (parsedTargetPressure != null && parsedTargetPressure > 0.0 && activeBasePressure > 0.0) {
    activeBaseFlow * sqrt(parsedTargetPressure / activeBasePressure)
  } else null

  val percentDiff = if (calculatedFlow != null && activeBaseFlow > 0.0) {
    ((calculatedFlow - activeBaseFlow) / activeBaseFlow) * 100.0
  } else null

  val baseLbHr = activeBaseFlow / 10.5
  val calculatedLbHr = calculatedFlow?.let { it / 10.5 }

  val presets = listOf("2.5", "3.0", "3.5", "3.8", "4.0", "4.5", "5.0")

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
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            tint = TunerCyan,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Wydajność wtryskiwacza i kalkulator ciśnienia",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Automatyczne przeliczanie przepływu dla Twojego układu paliwowego",
            fontSize = 11.5.sp,
            color = TextSecondary
          )
        }
      }

      Spacer(Modifier.height(14.dp))

      // 2. DANE BAZOWE (Z PUBLIKACJI)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(DarkSurface)
          .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
          .padding(12.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "WYDAJNOŚĆ BAZOWA (NOMINALNA)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = TunerAmber,
                letterSpacing = 0.5.sp
              )
              Spacer(Modifier.height(2.dp))
              Row(verticalAlignment = Alignment.Bottom) {
                Text(
                  text = formatFlow(activeBaseFlow),
                  fontSize = 22.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = TextPrimary,
                  fontFamily = FontFamily.Monospace
                )
                Text(
                  text = " cc/min",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = TunerAmber,
                  modifier = Modifier.padding(bottom = 2.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "(~${String.format(Locale.US, "%.1f", baseLbHr)} lb/hr)",
                  fontSize = 12.sp,
                  color = TextSecondary,
                  modifier = Modifier.padding(bottom = 2.dp)
                )
              }
            }

            // Plakietka ciśnienia bazowego
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TunerAmber.copy(alpha = 0.15f))
                .border(1.dp, TunerAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "przy ciśnieniu",
                  fontSize = 9.5.sp,
                  color = TextSecondary
                )
                Text(
                  text = "${formatPressure(activeBasePressure)} bar",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TunerAmber,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }

          if (nominalFlowRateCc == null) {
            Spacer(Modifier.height(8.dp))
            Text(
              text = "Autor nie podał wydajności bazowej. Wpisz ją poniżej:",
              fontSize = 11.sp,
              color = TextMuted
            )
            Spacer(Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = customBaseFlowInput,
                onValueChange = { customBaseFlowInput = it },
                label = { Text("Baza cc/min") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary,
                  focusedBorderColor = TunerAmber,
                  unfocusedContainerColor = DarkSurfaceVariant,
                  focusedContainerColor = DarkSurfaceVariant
                )
              )
              OutlinedTextField(
                value = customBasePressureInput,
                onValueChange = { customBasePressureInput = it },
                label = { Text("Baza bar") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary,
                  focusedBorderColor = TunerAmber,
                  unfocusedContainerColor = DarkSurfaceVariant,
                  focusedContainerColor = DarkSurfaceVariant
                )
              )
            }
          }
        }
      }

      Spacer(Modifier.height(16.dp))

      // 3. SEKCJA WPISYWANIA CIŚNIENIA TWOJEGO UKŁADU
      Text(
        text = "Wpisz ciśnienie Twojego układu paliwowego:",
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
      )
      Spacer(Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = targetPressureInput,
          onValueChange = { targetPressureInput = it },
          label = { Text("Twoje ciśnienie [bar]") },
          placeholder = { Text("np. 4.0") },
          modifier = Modifier
            .weight(1f)
            .testTag("target_pressure_input"),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              tint = TunerCyan,
              modifier = Modifier.size(18.dp)
            )
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TunerCyan,
            unfocusedTextColor = TunerCyan,
            focusedBorderColor = TunerCyan,
            cursorColor = TunerCyan,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface
          )
        )
      }

      Spacer(Modifier.height(8.dp))

      // Szybkie presety ciśnienia (np. 3.0 bar, 3.5 bar, 3.8 bar, 4.0 bar)
      Text(
        text = "Szybki wybór popularnych ciśnień:",
        fontSize = 11.sp,
        color = TextSecondary
      )
      Spacer(Modifier.height(4.dp))
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        presets.forEach { preset ->
          val isSelected = targetPressureInput.trim().replace(",", ".") == preset
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) TunerCyan else DarkSurfaceVariant)
              .border(
                1.dp,
                if (isSelected) TunerCyan else BorderColor,
                RoundedCornerShape(6.dp)
              )
              .clickable { targetPressureInput = preset }
              .padding(horizontal = 10.dp, vertical = 5.dp)
              .testTag("preset_pressure_$preset")
          ) {
            Text(
              text = "$preset bar",
              fontSize = 11.5.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) Color.Black else TextPrimary
            )
          }
        }
      }

      Spacer(Modifier.height(16.dp))

      // 4. WYNIK PRZELICZENIA
      if (calculatedFlow != null && parsedTargetPressure != null) {
        val isDifferent = Math.abs(parsedTargetPressure - activeBasePressure) > 0.01
        val isHigher = (percentDiff ?: 0.0) > 0.05
        val isLower = (percentDiff ?: 0.0) < -0.05

        val accentColor = when {
          isHigher -> StatusGreen
          isLower -> TunerAmber
          else -> TunerCyan
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("calculated_flow_result_card")
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "WYLICZONA WYDAJNOŚĆ PRZY ${formatPressure(parsedTargetPressure)} BAR:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 0.5.sp
              )

              if (percentDiff != null && isDifferent) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Icon(
                    imageVector = if (isHigher) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(Modifier.width(2.dp))
                  Text(
                    text = "${if (isHigher) "+" else ""}${String.format(Locale.US, "%.1f", percentDiff)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                  )
                }
              }
            }

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = formatFlow(calculatedFlow),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = " cc/min",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                modifier = Modifier.padding(bottom = 3.dp)
              )
              Spacer(Modifier.width(10.dp))
              calculatedLbHr?.let { lb ->
                Text(
                  text = "(~${String.format(Locale.US, "%.1f", lb)} lb/hr)",
                  fontSize = 13.5.sp,
                  color = TextSecondary,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(bottom = 3.dp)
                )
              }
            }

            Spacer(Modifier.height(8.dp))

            // Tekst wyjaśniający
            Text(
              text = if (isDifferent) {
                "Przy zmianie ciśnienia z ${formatPressure(activeBasePressure)} bar na ${formatPressure(parsedTargetPressure)} bar wydajność ${if (isHigher) "wzrasta" else "maleje"} o ${String.format(Locale.US, "%.1f", Math.abs(percentDiff ?: 0.0))}%. Wpisz tę nową wartość do konfiguracji wtryskiwaczy w sterowniku ECU (np. EMU Black, Ecumaster Classic, Standalone)."
              } else {
                "Ciśnienie jest identyczne z bazowym ciśnieniem testowym wtryskiwacza."
              },
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 16.sp
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp)
        ) {
          Text(
            text = "Wpisz prawidłową wartość ciśnienia w barach (np. 3.5 lub 4.0), aby przeliczyć wydajność.",
            fontSize = 11.5.sp,
            color = TextMuted
          )
        }
      }

      Spacer(Modifier.height(14.dp))

      // 5. MINI TABELA SZYBKIEGO PODGLĄDU DLA RÓŻNYCH CIŚNIEŃ
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(DarkSurface)
          .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
          .padding(12.dp)
      ) {
        Text(
          text = "Tabela porównawcza dla wtryskiwacza:",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))

        // Nagłówki kolumn
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Ciśnienie", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(1f))
          Text("Wydajność", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(1.2f))
          Text("W lb/hr", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(0.9f))
          Text("Różnica", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(0.9f))
        }

        val tablePressures = listOf(2.5, 3.0, 3.5, 3.8, 4.0, 4.5, 5.0)
        tablePressures.forEach { p ->
          val flow = activeBaseFlow * sqrt(p / activeBasePressure)
          val diff = ((flow - activeBaseFlow) / activeBaseFlow) * 100.0
          val isCurrent = parsedTargetPressure != null && Math.abs(parsedTargetPressure - p) < 0.05
          val isBase = Math.abs(activeBasePressure - p) < 0.05

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(4.dp))
              .background(if (isCurrent) TunerCyan.copy(alpha = 0.15f) else Color.Transparent)
              .clickable { targetPressureInput = p.toString() }
              .padding(vertical = 5.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${p} bar" + if (isBase) " (baza)" else "",
              fontSize = 11.sp,
              fontWeight = if (isCurrent || isBase) FontWeight.Bold else FontWeight.Normal,
              color = if (isCurrent) TunerCyan else if (isBase) TunerAmber else TextPrimary,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = "${formatFlow(flow)} cc",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = if (isCurrent) TunerCyan else TextPrimary,
              modifier = Modifier.weight(1.2f)
            )
            Text(
              text = "${String.format(Locale.US, "%.1f", flow / 10.5)} lb",
              fontSize = 10.5.sp,
              color = TextSecondary,
              modifier = Modifier.weight(0.9f)
            )
            Text(
              text = if (Math.abs(diff) < 0.1) "0%" else "${if (diff > 0) "+" else ""}${String.format(Locale.US, "%.0f", diff)}%",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = if (diff > 0.1) StatusGreen else if (diff < -0.1) TunerAmber else TextMuted,
              modifier = Modifier.weight(0.9f)
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      // Informacja o wzorze
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
          text = "Wzór fizyczny: Q₂ = Q₁ × √(P₂ / P₁) (zależność przepływu cieczy od ciśnienia)",
          fontSize = 10.sp,
          color = TextMuted
        )
      }
    }
  }
}

private fun formatFlow(v: Double): String {
  return if (v % 1.0 == 0.0) {
    v.toInt().toString()
  } else {
    String.format(Locale.US, "%.1f", v)
  }
}

private fun formatPressure(v: Double): String {
  return if (v % 1.0 == 0.0) {
    v.toInt().toString()
  } else {
    String.format(Locale.US, "%.1f", v)
  }
}
