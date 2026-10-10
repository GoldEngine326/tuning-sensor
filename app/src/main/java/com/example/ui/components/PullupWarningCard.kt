package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TunerCyan
import com.example.ui.theme.TuningRed

@Composable
fun PullupWarningCard(
  recommendedValue: String = "2.49 kΩ do +5V VREF",
  customNote: String = "",
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFF261D12) // Subtle dark amber tint
    ),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, TunerAmber.copy(alpha = 0.8f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(TunerAmber.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Ostrzeżenie o pull-upie",
              tint = TunerAmber,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(Modifier.width(12.dp))

          Column {
            Text(
              text = "Konieczny rezystor PULL-UP",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TunerAmber
            )
            Text(
              text = "Zalecana wartość: $recommendedValue",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isExpanded = !isExpanded },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (isExpanded) "Zwiń szczegóły i schemat połączenia" else "Kliknij, aby rozwinąć szczegóły i schemat...",
          style = MaterialTheme.typography.bodySmall,
          color = TunerAmber,
          fontWeight = FontWeight.SemiBold
        )
        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = "Przełącz szczegóły",
          tint = TunerAmber,
          modifier = Modifier.size(20.dp)
        )
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
        ) {
          Text(
            text = if (customNote.isNotBlank()) customNote else "Czujnik to element NTC (zmiennooporowy). Wejście analogowe mikrokontrolera ECU mierzy wyłącznie napięcie 0-5V. Aby uzyskać sygnał, należy zastosować dzielnik napięcia z rezystorem podciągającym (Pull-Up) do szyny +5V VREF.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
          )

          Spacer(Modifier.height(12.dp))
          // Visual Circuit Schematic Box
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
              .border(1.dp, TunerAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
              .padding(12.dp)
          ) {
            Column {
              Text(
                text = "SCHEMAT POŁĄCZENIA W WIĄZCE:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TunerAmber,
                fontFamily = FontFamily.Monospace
              )
              Spacer(Modifier.height(6.dp))
              Text(
                text = "+5V VREF (ECU) ────[ Rezystor 2.49 kΩ ]────┐\n" +
                  "                                            │\n" +
                  "Wejście Sygnału ECU (np. IAT / CLT) ────────┼──── (Pin 1 Czujnika NTC)\n" +
                  "                                            \n" +
                  "Sensor GND (Masa ECU) ─────────────────────────── (Pin 2 Czujnika NTC)",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TunerCyan,
                lineHeight = 16.sp
              )
            }
          }

          Spacer(Modifier.height(10.dp))

          // Practical Tuner Tips
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
              .padding(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                text = "Wskazówka montażowa:",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TextPrimary
              )
            }
            Spacer(Modifier.height(4.dp))
            Text(
              text = "• Dedykowane wejścia IAT/CLT w sterownikach takich jak UAEFI, EMU Black, Ecumaster czy Speeduino posiadają już najczęściej fabryczny rezystor 2.49 kΩ na płycie głównej.\n• Jeśli jednak podłączasz ten czujnik do uniwersalnego wejścia analogowego (0-5V Analog In) lub własnego sterownika Arduino/STM32, musisz wlutować rezystor pull-up w wiązce!",
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
