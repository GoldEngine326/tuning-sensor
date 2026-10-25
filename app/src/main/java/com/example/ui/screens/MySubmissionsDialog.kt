package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ModerationRequest
import com.example.model.ModerationStatus
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
import com.example.ui.components.ComponentThumbnail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MySubmissionsDialog(
  requests: List<ModerationRequest>,
  currentUserId: String?,
  onDismiss: () -> Unit
) {
  val myRequests = remember(requests, currentUserId) {
    if (currentUserId.isNullOrBlank()) emptyList()
    else requests.filter { it.requesterId == currentUserId }
  }

  val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.7f))
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Dialog Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(DarkSurface)
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Moje zgłoszenia",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
              )
              Text(
                text = "Status propozycji czujników przesłanych do Admina",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = TextSecondary)
            }
          }

          if (myRequests.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.HourglassEmpty,
                  contentDescription = null,
                  tint = TextMuted,
                  modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                  text = "Brak wysłanych zgłoszeń",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                  text = "Gdy dodasz nowy czujnik lub zaproponujesz edycję, tutaj sprawdzisz status weryfikacji przez Administratora.",
                  fontSize = 12.sp,
                  color = TextSecondary,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(myRequests, key = { it.id }) { req ->
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = CarbonBlack),
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = req.type.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(req.type.badgeColor)
                      )
                      Text(
                        text = dateFormat.format(Date(req.createdAtMillis)),
                        fontSize = 10.sp,
                        color = TextMuted
                      )
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      val img = (req.sensorData["imageUrl"] as? String)?.ifBlank { null }
                        ?: (req.sensorData["images"] as? List<*>)?.filterIsInstance<String>()?.firstOrNull()
                      if (img != null) {
                        ComponentThumbnail(
                          imageUrl = img,
                          vectorKey = "map_3b",
                          modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                      }
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = req.targetSensorName,
                          fontSize = 15.sp,
                          fontWeight = FontWeight.Bold,
                          color = TextPrimary
                        )
                        val partNo = req.sensorData["partNumber"] as? String ?: ""
                        if (partNo.isNotBlank()) {
                          Text(
                            text = "OEM: $partNo",
                            fontSize = 11.sp,
                            color = TunerCyan
                          )
                        }
                      }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = when (req.status) {
                          ModerationStatus.APPROVED -> Icons.Default.CheckCircle
                          ModerationStatus.REJECTED -> Icons.Default.Close
                          ModerationStatus.PENDING -> Icons.Default.HourglassEmpty
                        },
                        contentDescription = null,
                        tint = Color(req.status.color),
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(Modifier.width(4.dp))
                      Text(
                        text = req.status.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(req.status.color)
                      )
                    }

                    if (!req.reviewNotes.isNullOrBlank()) {
                      Spacer(Modifier.height(4.dp))
                      Text(
                        text = "Wiadomość od Admina: ${req.reviewNotes}",
                        fontSize = 11.sp,
                        color = TunerAmber
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
