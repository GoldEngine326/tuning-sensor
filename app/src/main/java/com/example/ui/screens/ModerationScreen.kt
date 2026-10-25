package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.ModerationRequest
import com.example.model.ModerationStatus
import com.example.model.ModerationType
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
import com.example.ui.theme.TuningRed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ZoomIn
import com.example.ui.components.ComponentThumbnail
import com.example.ui.components.FullScreenImageViewerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ModerationPhotoPreviewState(
  val images: List<String>,
  val initialIndex: Int = 0,
  val title: String,
  val partNumber: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(
  requests: List<ModerationRequest>,
  adminNick: String,
  onApprove: (ModerationRequest) -> Unit,
  onReject: (ModerationRequest, String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }

  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabTitles = listOf("Oczekujące", "Zatwierdzone", "Odrzucone")

  val pendingRequests = remember(requests) { requests.filter { it.status == ModerationStatus.PENDING } }
  val approvedRequests = remember(requests) { requests.filter { it.status == ModerationStatus.APPROVED } }
  val rejectedRequests = remember(requests) { requests.filter { it.status == ModerationStatus.REJECTED } }

  val activeList = when (selectedTabIndex) {
    0 -> pendingRequests
    1 -> approvedRequests
    else -> rejectedRequests
  }

  var rejectingRequest by remember { mutableStateOf<ModerationRequest?>(null) }
  var rejectionReasonInput by remember { mutableStateOf("") }
  var isProcessing by remember { mutableStateOf(false) }
  var photoPreviewState by remember { mutableStateOf<ModerationPhotoPreviewState?>(null) }

  // Powiększenie zdjęć w pełnym ekranie ze sterowaniem zoomem (pinch-to-zoom)
  if (photoPreviewState != null) {
    val state = photoPreviewState!!
    FullScreenImageViewerDialog(
      imageUrl = state.images.getOrNull(state.initialIndex),
      images = state.images,
      initialIndex = state.initialIndex,
      vectorKey = "map_3b",
      title = state.title,
      partNumber = state.partNumber,
      onDismiss = { photoPreviewState = null }
    )
  }

  // Modal Dialog do wpisania powodu odrzucenia
  if (rejectingRequest != null) {
    AlertDialog(
      onDismissRequest = {
        if (!isProcessing) {
          rejectingRequest = null
          rejectionReasonInput = ""
        }
      },
      title = {
        Text("Odrzuć wniosek użytkownika", fontWeight = FontWeight.Bold, color = TextPrimary)
      },
      text = {
        Column {
          Text(
            text = "Podaj powód odrzucenia wniosku (${rejectingRequest?.type?.label} dla \"${rejectingRequest?.targetSensorName}\"). Zostanie on wyświetlony użytkownikowi.",
            fontSize = 12.sp,
            color = TextSecondary
          )
          Spacer(Modifier.height(10.dp))
          OutlinedTextField(
            value = rejectionReasonInput,
            onValueChange = { rejectionReasonInput = it },
            placeholder = { Text("np. Błędny pinout / brak numeru części OEM / duplikat", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TuningRed,
              unfocusedBorderColor = BorderStroke,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val req = rejectingRequest ?: return@Button
            isProcessing = true
            onReject(req, rejectionReasonInput.trim().ifBlank { "Odrzucone przez Administratora" })
            isProcessing = false
            rejectingRequest = null
            rejectionReasonInput = ""
          },
          colors = ButtonDefaults.buttonColors(containerColor = TuningRed),
          enabled = !isProcessing
        ) {
          if (isProcessing) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          } else {
            Text("Odrzuć wniosek", fontWeight = FontWeight.Bold)
          }
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            rejectingRequest = null
            rejectionReasonInput = ""
          },
          enabled = !isProcessing
        ) {
          Text("Anuluj", color = TextSecondary)
        }
      },
      containerColor = DarkSurfaceElevated
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = CarbonBlack,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DarkSurface,
          titleContentColor = TextPrimary
        ),
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("moderation_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć", tint = TextPrimary)
          }
        },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .background(TunerAmber, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
              Text(
                text = "PANEL ADMINISTRATORA",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextPrimary
              )
              Text(
                text = "yetigiusz@gmail.com • Moderacja bazy",
                fontSize = 11.sp,
                color = TunerAmber
              )
            }
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Zakładki stanu wniosków
      TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = DarkSurfaceElevated,
        contentColor = TextPrimary,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
            color = TunerAmber
          )
        }
      ) {
        tabTitles.forEachIndexed { index, title ->
          val count = when (index) {
            0 -> pendingRequests.size
            1 -> approvedRequests.size
            else -> rejectedRequests.size
          }
          Tab(
            selected = selectedTabIndex == index,
            onClick = { selectedTabIndex = index },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = title,
                  fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                  color = if (selectedTabIndex == index) TunerAmber else TextSecondary,
                  fontSize = 13.sp
                )
                if (count > 0) {
                  Spacer(Modifier.width(6.dp))
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (index == 0) TuningRed else DarkSurfaceVariant)
                      .padding(horizontal = 6.dp, vertical = 1.dp)
                  ) {
                    Text(
                      text = "$count",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                }
              }
            }
          )
        }
      }

      // Lista zgłoszeń
      if (activeList.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = if (selectedTabIndex == 0) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
              contentDescription = null,
              tint = if (selectedTabIndex == 0) Color(0xFF34C759) else TextMuted,
              modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
              text = when (selectedTabIndex) {
                0 -> "Brak oczekujących wniosków"
                1 -> "Brak zatwierdzonych wniosków w historii"
                else -> "Brak odrzuconych wniosków"
              },
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = when (selectedTabIndex) {
                0 -> "Wszystkie propozycje dodania, edycji i usunięcia czujników zostały rozpatrzone!"
                else -> "Tutaj będą pojawiać się zarchiwizowane wnioski."
              },
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
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(activeList, key = { it.id }) { request ->
            ModerationCard(
              request = request,
              onApprove = { onApprove(request) },
              onReject = { rejectingRequest = request },
              onPhotoClick = { imgs, idx, title, partNo ->
                photoPreviewState = ModerationPhotoPreviewState(
                  images = imgs,
                  initialIndex = idx,
                  title = title,
                  partNumber = partNo
                )
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun ModerationCard(
  request: ModerationRequest,
  onApprove: () -> Unit,
  onReject: () -> Unit,
  onPhotoClick: (images: List<String>, index: Int, title: String, partNumber: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
  val dateStr = remember(request.createdAtMillis) {
    dateFormat.format(Date(request.createdAtMillis))
  }

  val typeColor = Color(request.type.badgeColor)
  val typeIcon = when (request.type) {
    ModerationType.ADD -> Icons.Default.AddCircle
    ModerationType.EDIT -> Icons.Default.Edit
    ModerationType.DELETE -> Icons.Default.Delete
  }

  var isDetailsExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header: Badge Typu Zgłoszenia + Data
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(typeColor.copy(alpha = 0.15f))
            .border(1.dp, typeColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(14.dp))
          Spacer(Modifier.width(4.dp))
          Text(request.type.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = typeColor)
        }

        Text(dateStr, fontSize = 11.sp, color = TextMuted)
      }

      Spacer(Modifier.height(10.dp))

      // Tytuł i nazwa podzespołu
      Text(
        text = request.targetSensorName,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )

      // Autor zgłoszenia
      Spacer(Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Person, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(
          text = "Zgłaszający: ${request.requesterNick} (${request.requesterEmail.ifBlank { "Anonim" }})",
          fontSize = 11.sp,
          color = TextSecondary
        )
      }

      // Powód wniosku lub szczegóły
      if (request.requestReason.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .padding(8.dp)
        ) {
          Column {
            Text(
              text = if (request.type == ModerationType.DELETE) "Powód usunięcia:" else "Opis od użytkownika:",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = TunerAmber
            )
            Spacer(Modifier.height(2.dp))
            Text(
              text = request.requestReason,
              fontSize = 12.sp,
              color = TextPrimary
            )
          }
        }
      }

      // Podgląd dodatkowych informacji dla dodania lub edycji
      if (request.sensorData.isNotEmpty()) {
        val partNum = request.sensorData["partNumber"] as? String ?: ""
        val categoryStr = request.sensorData["category"] as? String ?: ""
        val pinsCount = (request.sensorData["pins"] as? List<*>)?.size ?: 0

        // Bezpieczne pobranie wszystkich zdjęć (głównych i dodatkowych)
        val rawImages = (request.sensorData["images"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val singleImg = (request.sensorData["imageUrl"] as? String)?.ifBlank { null }
        val allSensorPhotos = remember(request.sensorData) {
          if (rawImages.isNotEmpty()) rawImages else listOfNotNull(singleImg)
        }
        val connectorPhoto = remember(request.sensorData) {
          (request.sensorData["connectorImageUrl"] as? String)?.ifBlank { null }
        }

        Spacer(Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (partNum.isNotBlank()) {
            InfoChip(label = "OEM: $partNum")
          }
          if (categoryStr.isNotBlank()) {
            InfoChip(label = "Kategoria: $categoryStr")
          }
          if (pinsCount > 0) {
            InfoChip(label = "Piny: $pinsCount")
          }
          if (allSensorPhotos.isNotEmpty()) {
            InfoChip(label = "Zdjęcia czujnika: 📷 ${allSensorPhotos.size}")
          }
          if (connectorPhoto != null) {
            InfoChip(label = "Wtyczka: 🔌 1")
          }
        }

        // --- GALERIA PODGLĄDU ZDJĘĆ ZGŁOSZENIA ---
        if (allSensorPhotos.isNotEmpty() || connectorPhoto != null) {
          Spacer(Modifier.height(10.dp))
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(DarkSurfaceVariant)
              .padding(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                  text = "Załączone zdjęcia (${allSensorPhotos.size + (if (connectorPhoto != null) 1 else 0)}):",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TunerCyan
                )
              }
              Text(
                text = "Dotknij, aby powiększyć",
                fontSize = 10.sp,
                color = TextMuted
              )
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              itemsIndexed(allSensorPhotos) { index, photoUrl ->
                Box(
                  modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                      width = if (index == 0) 2.dp else 1.dp,
                      color = if (index == 0) TunerCyan else BorderStroke,
                      shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                      onPhotoClick(allSensorPhotos, index, request.targetSensorName, partNum)
                    }
                ) {
                  ComponentThumbnail(
                    imageUrl = photoUrl,
                    vectorKey = request.sensorData["vectorIllustrationKey"] as? String ?: "map_3b",
                    modifier = Modifier.fillMaxSize()
                  )

                  if (index == 0) {
                    Box(
                      modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(TunerCyan, RoundedCornerShape(bottomEnd = 4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                      Text("Główne", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                  }

                  Box(
                    modifier = Modifier
                      .align(Alignment.BottomEnd)
                      .padding(2.dp)
                      .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                      .padding(horizontal = 4.dp, vertical = 2.dp)
                  ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Powiększ", tint = TunerCyan, modifier = Modifier.size(12.dp))
                  }
                }
              }

              if (connectorPhoto != null) {
                item {
                  Box(
                    modifier = Modifier
                      .size(92.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .border(1.dp, TunerAmber, RoundedCornerShape(8.dp))
                      .clickable {
                        onPhotoClick(listOf(connectorPhoto), 0, "Wtyczka: ${request.targetSensorName}", partNum)
                      }
                  ) {
                    ComponentThumbnail(
                      imageUrl = connectorPhoto,
                      vectorKey = "map_3b",
                      modifier = Modifier.fillMaxSize()
                    )

                    Box(
                      modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(TunerAmber, RoundedCornerShape(bottomEnd = 4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                      Text("Wtyczka", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Box(
                      modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                      Icon(Icons.Default.ZoomIn, contentDescription = "Powiększ", tint = TunerAmber, modifier = Modifier.size(12.dp))
                    }
                  }
                }
              }
            }
          }
        }

        // Rozwijany podgląd techniczny (piny, opis)
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { isDetailsExpanded = !isDetailsExpanded }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (isDetailsExpanded) "Ukryj szczegóły techniczne" else "Pokaż szczegóły techniczne (piny, kalibracja)",
            fontSize = 11.sp,
            color = TunerAmber,
            fontWeight = FontWeight.SemiBold
          )
          Icon(
            imageVector = if (isDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = TunerAmber,
            modifier = Modifier.size(16.dp)
          )
        }

        if (isDetailsExpanded) {
          val pins = (request.sensorData["pins"] as? List<Map<String, Any>>) ?: emptyList()
          val shortDesc = request.sensorData["shortDescription"] as? String ?: ""
          val importantNotes = request.sensorData["importantNotes"] as? String ?: ""

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(DarkSurfaceVariant.copy(alpha = 0.7f))
              .padding(8.dp)
          ) {
            if (shortDesc.isNotBlank()) {
              Text("Opis:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
              Text(shortDesc, fontSize = 11.sp, color = TextPrimary)
              Spacer(Modifier.height(4.dp))
            }
            if (pins.isNotEmpty()) {
              Text("Rozpiska pinów (${pins.size}):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TunerCyan)
              pins.forEach { p ->
                val num = p["pinNumber"] as? String ?: ""
                val pName = p["name"] as? String ?: ""
                val pDesc = p["description"] as? String ?: ""
                Text("• Pin $num: $pName ${if (pDesc.isNotBlank()) "($pDesc)" else ""}", fontSize = 11.sp, color = TextPrimary)
              }
              Spacer(Modifier.height(4.dp))
            }
            if (importantNotes.isNotBlank()) {
              Text("Ważne uwagi:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TuningRed)
              Text(importantNotes, fontSize = 11.sp, color = TextPrimary)
            }
          }
        }
      }

      // Jeśli wniosek rozpatrzony, wyświetl status i notatkę
      if (request.status != ModerationStatus.PENDING) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (request.status == ModerationStatus.APPROVED) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = Color(request.status.color),
            modifier = Modifier.size(14.dp)
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = request.status.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(request.status.color)
          )
          if (!request.reviewerNick.isNullOrBlank()) {
            Text(
              text = " • Rozpatrzył: ${request.reviewerNick}",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }
        if (!request.reviewNotes.isNullOrBlank()) {
          Text(
            text = "Notatka: ${request.reviewNotes}",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }

      // Przyciski akcji dla administratora
      if (request.status == ModerationStatus.PENDING) {
        Spacer(Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onReject,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TuningRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, TuningRed.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Odrzuć", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          Button(
            onClick = onApprove,
            modifier = Modifier.weight(1.3f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
              text = when (request.type) {
                ModerationType.ADD -> "Zatwierdź i dodaj"
                ModerationType.EDIT -> "Zatwierdź zmiany"
                ModerationType.DELETE -> "Potwierdź usunięcie"
              },
              color = Color.Black,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun InfoChip(label: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .background(DarkSurfaceVariant)
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
  }
}
