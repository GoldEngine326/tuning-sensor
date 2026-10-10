package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TuningRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NicknameDialog(
  currentNick: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit
) {
  var nickInput by remember { mutableStateOf(currentNick) }

  val presets = listOf("Tuner_Boost", "VAG_Spec", "UAEFI_Master", "Turbo_18T", "Standalone_PL", "ECU_Dr")

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .background(TunerAmber.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Badge, contentDescription = null, tint = TunerAmber, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
              text = "Twój Nick Tunera",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = TextSecondary)
          }
        }

        Spacer(Modifier.height(10.dp))

        Text(
          text = "Nadaj sobie unikalny nick, aby inni użytkownicy w społeczności wiedzieli, kto dodał czujnik, pinout lub ocenił wpis.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          lineHeight = 19.sp
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
          value = nickInput,
          onValueChange = { nickInput = it.take(24) },
          label = { Text("Twój nick / podpis") },
          placeholder = { Text("np. TunerPRO_PL") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TunerAmber) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("nick_input_field"),
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TunerAmber,
            unfocusedBorderColor = BorderStroke,
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        Spacer(Modifier.height(12.dp))

        Text(text = "Szybkie propozycje:", fontSize = 12.sp, color = TextMuted)
        Spacer(Modifier.height(6.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          presets.forEach { preset ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceVariant)
                .clickable { nickInput = preset }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(text = preset, fontSize = 11.sp, color = TextSecondary)
            }
          }
        }

        Spacer(Modifier.height(20.dp))

        Button(
          onClick = {
            if (nickInput.isNotBlank()) {
              onSave(nickInput.trim())
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("save_nick_button"),
          colors = ButtonDefaults.buttonColors(containerColor = TuningRed),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(8.dp))
          Text("Zapisz mój nick", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
