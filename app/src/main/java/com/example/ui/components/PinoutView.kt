package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PinInfo
import com.example.model.PinType
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TunerCyan
import com.example.ui.theme.TuningRed
import com.example.ui.theme.WireBlack
import com.example.ui.theme.WireBlue
import com.example.ui.theme.WireBrown
import com.example.ui.theme.WireGreen
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Cable

@Composable
fun PinoutView(
  pins: List<PinInfo>,
  componentName: String,
  connectorImageUrl: String = "",
  onConnectorImageClick: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Column(modifier = modifier.fillMaxWidth()) {
    // Zdjęcie wtyczki nad układem pinów jeśli dodano
    if (connectorImageUrl.isNotBlank()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = onConnectorImageClick != null) {
            onConnectorImageClick?.invoke(connectorImageUrl)
          },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Cable, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Zdjęcie wtyczki / gniazda", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            if (onConnectorImageClick != null) {
              Text("Dotknij, aby powiększyć", fontSize = 11.sp, color = TunerCyan)
            }
          }
          Spacer(Modifier.height(8.dp))
          ComponentThumbnail(
            imageUrl = connectorImageUrl,
            vectorKey = "vw_coils",
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .clip(RoundedCornerShape(8.dp))
          )
        }
      }
      Spacer(Modifier.height(14.dp))
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.ElectricalServices,
          contentDescription = null,
          tint = TunerAmber,
          modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "Układ wyprowadzeń (Pinout)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }

      FilledTonalButton(
        onClick = {
          val textBuilder = StringBuilder()
          textBuilder.append("PINOUT: $componentName\n\n")
          val grouped = pins.groupBy { it.connectorName.ifBlank { "Wtyczka główna" } }
          grouped.forEach { (connName, connPins) ->
            if (grouped.size > 1 || connName != "Wtyczka główna") {
              textBuilder.append("[$connName]\n")
            }
            connPins.forEach { pin ->
              textBuilder.append("${pin.pinNumber}: ${pin.name}\n")
              if (pin.wireColor.isNotBlank()) textBuilder.append("  Kolor: ${pin.wireColor}\n")
              textBuilder.append("  Typ: ${pin.type.label}\n")
              if (pin.description.isNotBlank()) textBuilder.append("  Opis: ${pin.description}\n")
              textBuilder.append("\n")
            }
          }
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val clip = ClipData.newPlainText("Pinout $componentName", textBuilder.toString())
          clipboard.setPrimaryClip(clip)
          Toast.makeText(context, "Skopiowano pinout do schowka!", Toast.LENGTH_SHORT).show()
        },
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Kopiuj pinout", modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Kopiuj", fontSize = 12.sp)
      }
    }

    Spacer(Modifier.height(12.dp))

    if (pins.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
          .padding(20.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("Brak danych o pinoucie", color = TextSecondary)
      }
    } else {
      val grouped = pins.groupBy { it.connectorName.ifBlank { "Wtyczka główna" } }
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        grouped.forEach { (connector, connPins) ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
              // Nagłówek wtyczki w jednym zbiorze z pinami
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(
                  Icons.Default.ElectricalServices,
                  contentDescription = null,
                  tint = TunerAmber,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = connector,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary,
                  fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                  text = "${connPins.size} pinów",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }

              Spacer(Modifier.height(6.dp))

              // Lista pinów w formie kompaktowej tabeli/listy (blisko siebie jeden pod drugim)
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, BorderStroke.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                  .clip(RoundedCornerShape(6.dp))
              ) {
                connPins.forEachIndexed { index, pin ->
                  PinTableRow(pin = pin, index = index, isLast = index == connPins.size - 1)
                }
              }
            }
          }
        }
      }
    }
  }
}

fun resolvePinTextColor(wireColor: String): Color {
  val lower = wireColor.lowercase().trim()
  return when {
    "czerwon" in lower || "red" in lower -> Color(0xFFFF453A)      // Czerwony
    "brąz" in lower || "braz" in lower || "brown" in lower -> Color(0xFFC68A4C) // Ciepły brązowy
    "niebiesk" in lower || "blue" in lower -> Color(0xFF38BDF8)     // Niebieski
    "biał" in lower || "bial" in lower || "white" in lower -> Color(0xFFF5F5F7) // Biały
    else -> Color(0xFFF5F5F7)
  }
}

@Composable
fun PinTableRow(pin: PinInfo, index: Int, isLast: Boolean) {
  val pinTextColor = resolvePinTextColor(pin.wireColor)
  var isExpanded by rememberSaveable { mutableStateOf(false) }
  val hasNotes = pin.description.isNotBlank()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(if (index % 2 == 0) CarbonBlack.copy(alpha = 0.45f) else DarkSurfaceVariant.copy(alpha = 0.35f))
  ) {
    // Kompaktowy wiersz pinu: po lewej nr pinu, od środka opis pinu, po prawej znak "i"
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = hasNotes) { isExpanded = !isExpanded }
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Po lewej: Numer pinu
      Box(
        modifier = Modifier
          .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
          .border(0.6.dp, BorderStroke, RoundedCornerShape(4.dp))
          .padding(horizontal = 6.dp, vertical = 2.5.dp)
      ) {
        Text(
          text = pin.pinNumber.ifBlank { "PIN ${index + 1}" },
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 11.5.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(Modifier.width(8.dp))

      // Od środka do prawej: Opis pinu w wybranym kolorze (Czerwony, Brązowy, Biały, Niebieski)
      Text(
        text = pin.name.ifBlank { "Sygnał ${index + 1}" },
        color = pinTextColor,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        modifier = Modifier.weight(1f)
      )

      Spacer(Modifier.width(6.dp))

      // Znak "i" po prawej stronie (zamiast kropki)
      if (hasNotes) {
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isExpanded) pinTextColor.copy(alpha = 0.25f) else DarkSurfaceVariant)
            .border(1.dp, if (isExpanded) pinTextColor else BorderStroke, CircleShape)
            .clickable { isExpanded = !isExpanded },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "i",
            color = if (isExpanded) pinTextColor else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      } else {
        // Mały subtelny wskaźnik koloru, jeśli brak uwag
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(pinTextColor)
        )
      }
    }

    // Rozwijane uwagi do pinu po kliknięciu znaku "i" (równo od lewej do prawej pod nr pinu i opisem)
    if (hasNotes && isExpanded) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface.copy(alpha = 0.7f))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = pin.description,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          fontSize = 11.5.sp,
          lineHeight = 15.sp,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    if (!isLast) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(0.6.dp)
          .background(BorderStroke.copy(alpha = 0.6f))
      )
    }
  }
}
