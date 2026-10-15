package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.ComponentThumbnail
import com.example.ui.components.FullScreenImageViewerDialog
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ComponentCategory
import com.example.model.TuningComponent
import com.example.ui.components.CharacteristicsView
import com.example.ui.components.DeadTimeTablesView
import com.example.ui.components.InjectorFlowCalculatorCard
import com.example.ui.components.ThrottleDimensionsCard
import com.example.ui.components.ComponentHeroImage
import com.example.ui.components.PinoutView
import com.example.ui.components.PullupWarningCard
import com.example.ui.components.RatingBarView
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
  component: TuningComponent,
  onBack: () -> Unit,
  onEditClick: (TuningComponent) -> Unit,
  onVote: (Int) -> Unit,
  onDelete: ((TuningComponent) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  BackHandler {
    onBack()
  }

  val allPhotos = component.allImages()
  var selectedPhotoIndex by remember(component) { mutableIntStateOf(0) }
  var showFullScreenImage by remember { mutableStateOf(false) }
  var fullScreenConnectorImage by remember { mutableStateOf<String?>(null) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  val activeHeroImage = allPhotos.getOrNull(selectedPhotoIndex) ?: component.primaryImage

  if (showDeleteDialog && onDelete != null) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = {
        Text("Usuń czujnik z bazy", fontWeight = FontWeight.Bold, color = TextPrimary)
      },
      text = {
        Text(
          "Czy na pewno chcesz usunąć \"${component.name}\"? Zostanie trwale usunięty ze wspólnej bazy w chmurze.",
          color = TextSecondary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteDialog = false
            onDelete(component)
          },
          colors = ButtonDefaults.buttonColors(containerColor = TuningRed)
        ) {
          Text("Usuń", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
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
            modifier = Modifier.testTag("detail_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Powrót",
              tint = Color.White
            )
          }
        },
        title = {
          Column {
            Text(
              text = component.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              color = TextPrimary
            )
            if (component.partNumber.isNotBlank()) {
              Text(
                text = component.partNumber,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TunerCyan,
                maxLines = 1
              )
            }
          }
        },
        actions = {
          if (onDelete != null) {
            IconButton(
              onClick = { showDeleteDialog = true },
              modifier = Modifier.testTag("delete_component_detail_button")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Usuń wpis",
                tint = TuningRed
              )
            }
          }
          // Edit entry button: allows modifying any entry
          FilledTonalButton(
            onClick = { onEditClick(component) },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = DarkSurfaceVariant,
              contentColor = TunerAmber
            ),
            modifier = Modifier
              .padding(end = 8.dp)
              .testTag("edit_component_button")
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Edytuj wpis", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edytuj", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      // 1. ZDJĘCIE PODZESPOŁU (Możliwość wejścia i powiększenia)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .clickable { showFullScreenImage = true }
          .testTag("component_hero_image_clickable")
      ) {
        ComponentHeroImage(
          imageUrl = activeHeroImage,
          vectorKey = component.vectorIllustrationKey,
          modifier = Modifier.testTag("component_hero_image")
        )

        // Arrow navigation if multiple photos
        if (allPhotos.size > 1) {
          if (selectedPhotoIndex > 0) {
            IconButton(
              onClick = { selectedPhotoIndex-- },
              modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .background(Color.Black.copy(alpha = 0.60f), RoundedCornerShape(20.dp))
                .size(36.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Poprzednie",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          if (selectedPhotoIndex < allPhotos.size - 1) {
            IconButton(
              onClick = { selectedPhotoIndex++ },
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .background(Color.Black.copy(alpha = 0.60f), RoundedCornerShape(20.dp))
                .size(36.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Następne",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          // Counter badge
          Box(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(10.dp)
              .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "Zdjęcie ${selectedPhotoIndex + 1} z ${allPhotos.size}",
              fontSize = 11.sp,
              color = TunerCyan,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Odznaka z podpowiedzią powiększenia
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(10.dp)
            .background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.ZoomIn,
              contentDescription = "Powiększ zdjęcie",
              tint = TunerCyan,
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = "Dotknij, aby powiększyć",
              fontSize = 11.sp,
              color = Color.White,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // Mini gallery carousel below hero if multiple photos exist
      if (allPhotos.size > 1) {
        Spacer(Modifier.height(10.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          itemsIndexed(allPhotos) { index, photoUrl ->
            Box(
              modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                  width = if (index == selectedPhotoIndex) 2.dp else 1.dp,
                  color = if (index == selectedPhotoIndex) TunerCyan else BorderStroke,
                  shape = RoundedCornerShape(10.dp)
                )
                .clickable { selectedPhotoIndex = index }
            ) {
              ComponentThumbnail(
                imageUrl = photoUrl,
                vectorKey = component.vectorIllustrationKey,
                modifier = Modifier.fillMaxSize()
              )
              if (index == 0) {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(TunerCyan, RoundedCornerShape(bottomEnd = 6.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                  Text(
                    text = "Główne",
                    fontSize = 8.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(16.dp))

      // Header Meta: Category, Author, and Edit history
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .background(TuningRed, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = component.category.displayName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
              text = "Dodał: ${component.authorNick}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = TextSecondary
            )
          }

          Spacer(Modifier.width(6.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.CloudDone, contentDescription = "Zsynchronizowano", tint = TunerCyan, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
            Text(
              text = "Chmura",
              fontSize = 11.sp,
              color = TextSecondary,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      Spacer(Modifier.height(12.dp))

      // Short Description / Zastosowanie z obsługą klikalnych linków www
      if (component.shortDescription.isNotBlank()) {
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        val urlRegex = Regex("""(https?://[^\s]+|www\.[^\s]+)""")
        val text = component.shortDescription
        val matches = urlRegex.findAll(text).toList()

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            if (matches.isEmpty()) {
              Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
              )
            } else {
              val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                var currentIndex = 0
                for (match in matches) {
                  val start = match.range.first
                  val end = match.range.last + 1
                  if (start > currentIndex) {
                    append(text.substring(currentIndex, start))
                  }
                  val url = match.value
                  pushStringAnnotation(tag = "URL", annotation = if (url.startsWith("http")) url else "https://$url")
                  pushStyle(
                    androidx.compose.ui.text.SpanStyle(
                      color = TunerCyan,
                      textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                      fontWeight = FontWeight.SemiBold
                    )
                  )
                  append(url)
                  pop() // pop style
                  pop() // pop annotation
                  currentIndex = end
                }
                if (currentIndex < text.length) {
                  append(text.substring(currentIndex))
                }
              }

              androidx.compose.foundation.text.ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = TextSecondary,
                  lineHeight = 20.sp
                ),
                onClick = { offset ->
                  annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                      try {
                        uriHandler.openUri(annotation.item)
                      } catch (_: Exception) {
                      }
                    }
                }
              )
            }
          }
        }

        Spacer(Modifier.height(14.dp))
      }

      // Community Rating and Voting Box
      RatingBarView(
        rating = component.rating,
        votesCount = component.ratingVotesCount,
        userVote = component.userVote,
        onVote = onVote,
        modifier = Modifier.testTag("detail_rating_bar")
      )

      Spacer(Modifier.height(16.dp))

      // Pull-up Warning for Temperature Sensors (IAT / CLT) and combo sensors
      val canHavePullup = component.category == ComponentCategory.TEMP_SENSOR ||
          (component.category == ComponentCategory.MAP_SENSOR && component.resistanceCharacteristic != null) ||
          (component.category == ComponentCategory.FUEL_PRESSURE && component.resistanceCharacteristic != null) ||
          (component.category == ComponentCategory.OIL_PRESSURE && component.resistanceCharacteristic != null) ||
          (component.category == ComponentCategory.MODULES && component.resistanceCharacteristic != null) ||
          (component.category == ComponentCategory.OTHER && component.pullupRequired)
      if (component.pullupRequired && canHavePullup) {
        PullupWarningCard(
          recommendedValue = component.pullupRecommendedValue,
          customNote = component.pullupNote,
          modifier = Modifier.testTag("pullup_warning_card")
        )
        Spacer(Modifier.height(16.dp))
      }

      // 2. PINOUT STEROWNIKA / CZUJNIKA
      PinoutView(
        pins = component.pinout,
        componentName = component.name,
        connectorImageUrl = component.connectorImageUrl,
        onConnectorImageClick = { url -> fullScreenConnectorImage = url },
        modifier = Modifier.testTag("detail_pinout_view")
      )

      Spacer(Modifier.height(20.dp))

      // WYMIARY PRZEPUSTNICY I ROZSTAW ŚRUB DLA KATEGORII PRZEPUSTNICA
      if (component.category == ComponentCategory.THROTTLE) {
        ThrottleDimensionsCard(
          innerDiameterMm = component.innerDiameterMm,
          outerDiameterMm = component.outerDiameterMm,
          boltSpacingWidthMm = component.boltSpacingWidthMm,
          boltSpacingHeightMm = component.boltSpacingHeightMm,
          boltDiagonal1Mm = component.boltDiagonal1Mm,
          boltDiagonal2Mm = component.boltDiagonal2Mm,
          modifier = Modifier.testTag("detail_throttle_dimensions_card")
        )
        Spacer(Modifier.height(20.dp))
      }

      // 3. WYDAJNOŚĆ WTRYSKIWACZA & KALKULATOR CIŚNIENIA DLA WTRYSKIWACZY
      if (component.category == ComponentCategory.INJECTORS) {
        if (component.injectorImpedance.isNotBlank()) {
          Card(
            modifier = Modifier.fillMaxWidth().testTag("detail_injector_impedance_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(10.dp))
              Column {
                Text(
                  text = "Typ / Impedancja wtryskiwacza",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
                Text(
                  text = component.injectorImpedance,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = TunerCyan
                )
              }
            }
          }
          Spacer(Modifier.height(14.dp))
        }

        if (component.flowRateCc != null) {
          InjectorFlowCalculatorCard(
            nominalFlowRateCc = component.flowRateCc,
            nominalPressureBar = component.flowPressureBar,
            modifier = Modifier.testTag("detail_injector_flow_calculator")
          )
          Spacer(Modifier.height(20.dp))
        }
      }

      // SPIS WYJŚĆ I SPECYFIKACJA STEROWNIKA ECU
      if (component.category == ComponentCategory.ECU && (component.ecuOutputsCount.isNotBlank() || component.ecuBoardDimensions.isNotBlank())) {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("detail_ecu_specs_card"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Specyfikacja sterownika ECU",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
              )
            }
            if (component.ecuOutputsCount.isNotBlank()) {
              Spacer(Modifier.height(10.dp))
              Text(
                text = "Spis wyjść na płytce ECU:",
                fontSize = 11.5.sp,
                color = TextSecondary
              )
              Text(
                text = component.ecuOutputsCount,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                lineHeight = 18.sp
              )
            }
            if (component.ecuBoardDimensions.isNotBlank()) {
              Spacer(Modifier.height(10.dp))
              Text(
                text = "Wymiary płytki / obudowy:",
                fontSize = 11.5.sp,
                color = TextSecondary
              )
              Text(
                text = component.ecuBoardDimensions,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TunerAmber
              )
            }
          }
        }
        Spacer(Modifier.height(20.dp))
      }

      // 4. CHARAKTERYSTYKA DLA WTRYSKIWACZY (DEAD TIME) LUB GŁÓWNA CHARAKTERYSTYKA (DWELL / KALIBRACJA MAP / CIŚNIENIE / MODUŁY)
      if (component.category == ComponentCategory.INJECTORS) {
        if (component.deadTimeTables.isNotEmpty()) {
          DeadTimeTablesView(
            tables = component.deadTimeTables,
            modifier = Modifier.testTag("detail_dead_time_tables_view")
          )
        }
      } else if ((component.category == ComponentCategory.COILS ||
          component.category == ComponentCategory.MAP_SENSOR ||
          component.category == ComponentCategory.FUEL_PRESSURE ||
          component.category == ComponentCategory.OIL_PRESSURE ||
          component.category == ComponentCategory.MODULES ||
          component.category == ComponentCategory.OTHER) && component.characteristic.points.isNotEmpty()) {
        CharacteristicsView(
          characteristic = component.characteristic,
          modifier = Modifier.testTag("detail_characteristics_view")
        )
      }

      // 5. CHARAKTERYSTYKA OPORNOŚCIOWA DLA CZUJNIKÓW TEMPERATURY (REZYSTANCJA NTC VS TEMPERATURA)
      val canHaveResistance = component.category == ComponentCategory.TEMP_SENSOR ||
          (component.category == ComponentCategory.MAP_SENSOR && component.resistanceCharacteristic != null) ||
          (component.category == ComponentCategory.FUEL_PRESSURE && component.resistanceCharacteristic != null) ||
          (component.category == ComponentCategory.OIL_PRESSURE && component.resistanceCharacteristic != null) ||
          (component.category == ComponentCategory.MODULES && component.resistanceCharacteristic != null) ||
          (component.category == ComponentCategory.OTHER && component.resistanceCharacteristic != null)
      if (canHaveResistance && component.resistanceCharacteristic != null) {
        Spacer(Modifier.height(20.dp))
        CharacteristicsView(
          characteristic = component.resistanceCharacteristic,
          modifier = Modifier.testTag("detail_resistance_characteristics_view")
        )
      }

      // 5. WAŻNE INFORMACJE DLA TUNERA
      if (component.importantNotes.isNotBlank()) {
        Spacer(Modifier.height(20.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Ważne uwagi tunera / Wskazówki",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
              )
            }

            Spacer(Modifier.height(8.dp))

            Text(
              text = component.importantNotes,
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
              lineHeight = 20.sp
            )
          }
        }
      }

      Spacer(Modifier.height(32.dp))
    }
  }

  // Pełnoekranowy podgląd zdjęć z powiększaniem (Zoom) oraz przełączaniem
  if (showFullScreenImage) {
    FullScreenImageViewerDialog(
      imageUrl = component.primaryImage,
      images = allPhotos,
      initialIndex = selectedPhotoIndex,
      vectorKey = component.vectorIllustrationKey,
      title = component.name,
      partNumber = component.partNumber,
      onDismiss = { showFullScreenImage = false }
    )
  }

  // Pełnoekranowy podgląd zdjęcia wtyczki
  if (fullScreenConnectorImage != null) {
    FullScreenImageViewerDialog(
      imageUrl = fullScreenConnectorImage!!,
      images = listOf(fullScreenConnectorImage!!),
      initialIndex = 0,
      vectorKey = "vw_coils",
      title = "Wtyczka / Pinout: ${component.name}",
      partNumber = component.partNumber,
      onDismiss = { fullScreenConnectorImage = null }
    )
  }
}
