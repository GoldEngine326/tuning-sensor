package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ComponentCategory
import com.example.ui.theme.BorderStroke as ThemeBorderStroke
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TunerCyan
import com.example.ui.theme.TuningRed
import com.example.util.DatasheetExtractor
import com.example.util.ExtractedComponentData
import kotlinx.coroutines.launch

private enum class AutoFillTab(val title: String) {
  IMAGE("Zdjęcie z danymi"),
  FILE("Plik z danymi"),
  LINK("Link do danych")
}

@Composable
fun AutoFillDatasheetCard(
  currentCategory: ComponentCategory,
  onDataExtracted: (ExtractedComponentData) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedTab by remember { mutableIntStateOf(0) }
  var isProcessing by remember { mutableStateOf(false) }
  var processingMessage by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var extractedResult by remember { mutableStateOf<ExtractedComponentData?>(null) }
  var showApiKeyDialog by remember { mutableStateOf(false) }
  var customApiKeyInput by remember { mutableStateOf(DatasheetExtractor.getApiKey(context)) }
  var isTestingKey by remember { mutableStateOf(false) }
  var testKeyResult by remember { mutableStateOf<String?>(null) }
  var testKeyIsSuccess by remember { mutableStateOf(false) }

  // 1. Rubryka: Zdjęcie z danymi
  var selectedDataImageUri by remember { mutableStateOf<Uri?>(null) }
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedDataImageUri = uri
      errorMessage = null
    }
  }

  // 2. Rubryka: Plik z danymi
  var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
  var selectedFileName by remember { mutableStateOf<String?>(null) }
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      selectedFileUri = uri
      selectedFileName = queryFileName(context, uri) ?: "dokument_z_danymi.pdf"
      errorMessage = null
    }
  }

  // 3. Rubryka: Link do danych
  var urlInput by remember { mutableStateOf("") }

  fun triggerExtraction(action: suspend () -> Result<ExtractedComponentData>, stepName: String) {
    scope.launch {
      isProcessing = true
      processingMessage = stepName
      errorMessage = null

      val result = action()
      isProcessing = false

      result.onSuccess { data ->
        extractedResult = data
        onDataExtracted(data)
        Toast.makeText(context, "Dane zaczerpnięte pomyślnie! Sprawdź formularz.", Toast.LENGTH_LONG).show()
      }.onFailure { err ->
        errorMessage = err.message ?: "Wystąpił błąd podczas analizy danych."
      }
    }
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = BorderStroke(1.2.dp, TunerCyan.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Nagłówek rubryk AI
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(TunerCyan.copy(alpha = 0.15f))
              .border(1.dp, TunerCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = TunerCyan,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(Modifier.width(10.dp))
          Column {
            Text(
              text = "Automatyczne zaczerpnięcie danych",
              fontWeight = FontWeight.Bold,
              fontSize = 14.5.sp,
              color = TextPrimary
            )
            Text(
              text = "Wklej link, dodaj plik lub zdjęcie tabelki — dane zostaną wpisane za Ciebie!",
              fontSize = 11.sp,
              color = TextSecondary,
              lineHeight = 15.sp
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          val isConfigured = DatasheetExtractor.hasApiKey(context)
          Box(
            modifier = Modifier
              .padding(end = 4.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(if (isConfigured) TunerCyan.copy(alpha = 0.15f) else TunerAmber.copy(alpha = 0.15f))
              .border(1.dp, if (isConfigured) TunerCyan.copy(alpha = 0.5f) else TunerAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
              .clickable { showApiKeyDialog = true }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(if (isConfigured) TunerCyan else TunerAmber)
              )
              Spacer(Modifier.width(5.dp))
              Text(
                text = if (isConfigured) "AI aktywne" else "Klucz API",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isConfigured) TunerCyan else TunerAmber
              )
            }
          }

          IconButton(
            onClick = { showApiKeyDialog = true },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Key,
              contentDescription = "Status klucza API",
              tint = if (isConfigured) TunerCyan else TunerAmber,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(Modifier.height(12.dp))

      // Zakładki (Trzy osobne rubryki zgodnie z prośbą użytkownika)
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = DarkSurfaceVariant,
        contentColor = TunerCyan,
        indicator = { tabPositions ->
          if (selectedTab < tabPositions.size) {
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = TunerCyan,
              height = 2.5.dp
            )
          }
        },
        divider = {}
      ) {
        AutoFillTab.values().forEachIndexed { index, tab ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                text = tab.title,
                fontSize = 12.sp,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedTab == index) TunerCyan else TextSecondary
              )
            }
          )
        }
      }

      Spacer(Modifier.height(14.dp))

      // ZAWARTOŚĆ POSZCZEGÓLNYCH RUBRYK
      when (selectedTab) {
        // --- 1. RUBRYKA NA ZDJĘCIE Z DANYMI ---
        0 -> {
          Column {
            Text(
              text = "Rubryka na zdjęcie z danymi (tabela, wykres, dead time, schemat):",
              fontSize = 12.sp,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "Wybierz zdjęcie tabelki z parametrami podzespołu (np. zrzut ekranu tabeli Dead Time wtryskiwacza, wykres napięcia MAP sensora czy schemat pinoutu).",
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 15.sp
            )

            Spacer(Modifier.height(10.dp))

            if (selectedDataImageUri != null) {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, ThemeBorderStroke)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  AsyncImage(
                    model = selectedDataImageUri,
                    contentDescription = "Zdjęcie z tabelą",
                    modifier = Modifier
                      .size(60.dp)
                      .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                  )
                  Spacer(Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Wybrano zdjęcie z danymi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Gotowe do analizy OCR", fontSize = 11.sp, color = TunerCyan)
                  }
                  IconButton(onClick = { selectedDataImageUri = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Usuń", tint = TuningRed, modifier = Modifier.size(18.dp))
                  }
                }
              }
              Spacer(Modifier.height(10.dp))
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = BorderStroke(1.dp, ThemeBorderStroke)
              ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (selectedDataImageUri == null) "Wybierz zdjęcie z danymi" else "Zmień zdjęcie", fontSize = 12.sp)
              }

              Button(
                onClick = {
                  val uri = selectedDataImageUri
                  if (uri != null) {
                    triggerExtraction(
                      action = { DatasheetExtractor.extractFromDataImage(context, uri) },
                      stepName = "Odczyt tabeli ze zdjęcia przez AI..."
                    )
                  } else {
                    Toast.makeText(context, "Najpierw wybierz zdjęcie z tabelą lub schematem", Toast.LENGTH_SHORT).show()
                  }
                },
                enabled = selectedDataImageUri != null && !isProcessing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TunerCyan, contentColor = Color.Black)
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Odczytaj ze zdjęcia", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }

        // --- 2. RUBRYKA NA PLIKI Z DANYMI ---
        1 -> {
          Column {
            Text(
              text = "Rubryka na pliki z danymi (PDF, TXT, CSV, Karta katalogowa):",
              fontSize = 12.sp,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "Wskaż plik z dokumentacją techniczną podzespołu (np. plik PDF od Boscha/Densa lub plik kalibracyjny w TunerStudio/EMU).",
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 15.sp
            )

            Spacer(Modifier.height(10.dp))

            if (selectedFileUri != null && selectedFileName != null) {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, ThemeBorderStroke)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Description, contentDescription = null, tint = TunerAmber, modifier = Modifier.size(28.dp))
                  Spacer(Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(selectedFileName ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Plik gotowy do zaczerpnięcia informacji", fontSize = 11.sp, color = TunerAmber)
                  }
                  IconButton(onClick = {
                    selectedFileUri = null
                    selectedFileName = null
                  }) {
                    Icon(Icons.Default.Close, contentDescription = "Usuń", tint = TuningRed, modifier = Modifier.size(18.dp))
                  }
                }
              }
              Spacer(Modifier.height(10.dp))
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  filePickerLauncher.launch("*/*")
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = BorderStroke(1.dp, ThemeBorderStroke)
              ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (selectedFileUri == null) "Wybierz plik (PDF / TXT)" else "Zmień plik", fontSize = 12.sp)
              }

              Button(
                onClick = {
                  val uri = selectedFileUri
                  val name = selectedFileName ?: "plik.pdf"
                  if (uri != null) {
                    triggerExtraction(
                      action = { DatasheetExtractor.extractFromFile(context, uri, name) },
                      stepName = "Odczytywanie danych z pliku $name..."
                    )
                  } else {
                    Toast.makeText(context, "Najpierw wybierz plik z dokumentacją", Toast.LENGTH_SHORT).show()
                  }
                },
                enabled = selectedFileUri != null && !isProcessing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TunerAmber, contentColor = Color.Black)
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Odczytaj z pliku", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }

        // --- 3. RUBRYKA NA LINKI Z DANYMI ---
        2 -> {
          Column {
            Text(
              text = "Rubryka na linki z danymi (karta katalogowa, strona WWW, plik PDF z sieci):",
              fontSize = 12.sp,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "Wklej bezpośredni adres URL do strony produktu, karty PDF lub specyfikacji tunera, a AI pobierze stronę i wyciągnie istotne parametry.",
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 15.sp
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
              value = urlInput,
              onValueChange = { urlInput = it },
              label = { Text("Link URL do danych / karty") },
              placeholder = { Text("np. https://bosch-motorsport.com/... lub datasheet.pdf") },
              leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = TunerCyan) },
              modifier = Modifier.fillMaxWidth().testTag("autofill_url_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = TunerCyan,
                unfocusedBorderColor = ThemeBorderStroke,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
              ),
              singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            Button(
              onClick = {
                if (urlInput.isNotBlank()) {
                  triggerExtraction(
                    action = { DatasheetExtractor.extractFromUrl(context, urlInput) },
                    stepName = "Pobieranie i odczyt danych z linku..."
                  )
                } else {
                  Toast.makeText(context, "Wklej link URL do karty katalogowej", Toast.LENGTH_SHORT).show()
                }
              },
              enabled = urlInput.isNotBlank() && !isProcessing,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = TunerCyan, contentColor = Color.Black)
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Pobierz z linku i wypełnij formularz", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
          }
        }
      }

      // Stan ładowania
      AnimatedVisibility(visible = isProcessing) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          CircularProgressIndicator(color = TunerCyan, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
          Spacer(Modifier.height(8.dp))
          Text(text = processingMessage, fontSize = 12.sp, color = TunerCyan, fontWeight = FontWeight.SemiBold)
          Text(text = "Proces może potrwać kilka sekund...", fontSize = 11.sp, color = TextMuted)
        }
      }

      // Komunikat błędu
      if (errorMessage != null) {
        Spacer(Modifier.height(10.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(containerColor = TuningRed.copy(alpha = 0.15f)),
          border = BorderStroke(1.dp, TuningRed.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = TuningRed, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text("Nie udało się zaczerpnąć danych:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = TuningRed)
                Text(errorMessage ?: "", fontSize = 11.sp, color = TextPrimary)
              }
              IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = TextMuted, modifier = Modifier.size(16.dp))
              }
            }

            if (errorMessage?.contains("klucz", ignoreCase = true) == true || errorMessage?.contains("API", ignoreCase = true) == true || errorMessage?.contains("kod", ignoreCase = true) == true) {
              Spacer(Modifier.height(8.dp))
              OutlinedButton(
                onClick = {
                  showApiKeyDialog = true
                  testKeyResult = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan),
                border = BorderStroke(1.dp, TunerCyan)
              ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("🧪 Przetestuj klucz lub wprowadź nowy darmowy klucz", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Karta sprawdzenia i weryfikacji przed publikacją
      AnimatedVisibility(visible = extractedResult != null) {
        val res = extractedResult
        if (res != null) {
          Spacer(Modifier.height(12.dp))
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = TunerCyan.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, TunerCyan.copy(alpha = 0.6f))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(TunerCyan),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "Dane zaczerpnięte pomyślnie!",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = TunerCyan
                )
                Spacer(Modifier.weight(1f))
                Text(
                  text = "Sprawdź poniżej przed publikacją",
                  fontSize = 10.5.sp,
                  color = TextSecondary
                )
              }

              Spacer(Modifier.height(6.dp))
              Text(
                text = "Pola w formularzu poniżej zostały uzupełnione na podstawie dokumentacji. Możesz je teraz dokładnie przejrzeć, skorygować i dodać własne uwagi przed zapisaniem:",
                fontSize = 11.5.sp,
                color = TextPrimary,
                lineHeight = 16.sp
              )

              if (res.summaryBullets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  res.summaryBullets.forEach { bullet ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("• ", color = TunerCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      Text(bullet, fontSize = 11.sp, color = TextPrimary)
                    }
                  }
                }
              }

              Spacer(Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                TextButton(
                  onClick = { extractedResult = null }
                ) {
                  Text("Ukryj podsumowanie", fontSize = 11.sp, color = TunerCyan)
                }
              }
            }
          }
        }
      }

      // Przycisk trybu demo / testowego dla ułatwienia testów użytkownikowi
      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (DatasheetExtractor.hasApiKey(context)) "Klucz Gemini API: aktywny" else "Klucz API: wymagany do odczytu online",
          fontSize = 10.5.sp,
          color = if (DatasheetExtractor.hasApiKey(context)) TunerCyan else TextMuted
        )

        Text(
          text = "Wypełnij przykładowymi danymi (${currentCategory.displayName})",
          fontSize = 10.5.sp,
          color = TunerAmber,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier
            .clickable {
              val demoData = DatasheetExtractor.generateSampleDemoData(currentCategory)
              extractedResult = demoData
              onDataExtracted(demoData)
              Toast.makeText(context, "Załadowano wzorcowe dane podzespołu do weryfikacji!", Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 4.dp)
        )
      }
    }
  }

  // Dialog konfiguracji klucza API
  if (showApiKeyDialog) {
    AlertDialog(
      onDismissRequest = { showApiKeyDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Key, contentDescription = null, tint = TunerCyan)
          Spacer(Modifier.width(8.dp))
          Text("Klucz Google Gemini API")
        }
      },
      text = {
        Column {
          val isFromProject = DatasheetExtractor.isKeyConfiguredInProject()
          if (isFromProject) {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              colors = CardDefaults.cardColors(containerColor = TunerCyan.copy(alpha = 0.12f)),
              border = BorderStroke(1.dp, TunerCyan.copy(alpha = 0.5f))
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Klucz z panelu Secrets jest AKTYWNY!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TunerCyan
                  )
                  Text(
                    text = "Aplikacja automatycznie odczytuje zdjęcia tabel, PDF i linki bez konieczności wpisywania klucza w telefonie.",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    lineHeight = 14.5.sp
                  )
                }
              }
            }
            Spacer(Modifier.height(12.dp))
          }

          Text(
            text = "Do inteligentnego odczytywania tabel, wykresów i plików PDF aplikacja używa modelu Google Gemini. Klucz jest w 100% darmowy (konto Google).",
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
          )

          Spacer(Modifier.height(10.dp))

          // Przycisk bezpośredniego otwarcia strony z kluczem
          Button(
            onClick = {
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                context.startActivity(intent)
              } catch (_: Exception) {
                Toast.makeText(context, "Otwórz w przeglądarce: aistudio.google.com/app/apikey", Toast.LENGTH_LONG).show()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = TunerCyan.copy(alpha = 0.2f), contentColor = TunerCyan),
            border = BorderStroke(1.dp, TunerCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pobierz darmowy klucz w Google AI Studio", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(Modifier.height(10.dp))

          Text(
            text = if (isFromProject) "Możesz także nadpisać klucz własnym kluczem lokalnym (opcjonalne):" else "Kroki:\n1. Kliknij przycisk powyżej\n2. Zaloguj się kontem Google i kliknij „Create API key”\n3. Wklej klucz poniżej:",
            fontSize = 11.sp,
            color = TextMuted,
            lineHeight = 15.sp
          )

          Spacer(Modifier.height(10.dp))

          val customSaved = DatasheetExtractor.getCustomKeyOnly(context)
          if (customSaved.isNotBlank()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Zapisany klucz w telefonie: ...${customSaved.takeLast(6)}",
                fontSize = 11.sp,
                color = TunerAmber
              )
              TextButton(
                onClick = {
                  DatasheetExtractor.clearCustomApiKey(context)
                  customApiKeyInput = ""
                  testKeyResult = null
                  Toast.makeText(context, "Wyczyszczono klucz z telefonu. Aktywny jest klucz z projektu!", Toast.LENGTH_SHORT).show()
                }
              ) {
                Text("Przywróć klucz Secrets", fontSize = 11.sp, color = TunerCyan)
              }
            }
            Spacer(Modifier.height(6.dp))
          }

          OutlinedTextField(
            value = customApiKeyInput,
            onValueChange = { 
              customApiKeyInput = it
              testKeyResult = null
            },
            label = { Text("Klucz API (AIzaSy...)") },
            placeholder = { Text(if (isFromProject) "Aktywny klucz z Secrets (wklej by zmienić)" else "Wklej tutaj klucz") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = TunerCyan,
              unfocusedBorderColor = ThemeBorderStroke,
              focusedContainerColor = DarkSurface,
              unfocusedContainerColor = DarkSurface
            )
          )

          Spacer(Modifier.height(10.dp))

          // Przycisk testowania klucza
          OutlinedButton(
            onClick = {
              isTestingKey = true
              testKeyResult = null
              scope.launch {
                val res = DatasheetExtractor.testApiKey(context, customApiKeyInput.ifBlank { null })
                isTestingKey = false
                res.onSuccess { msg ->
                  testKeyIsSuccess = true
                  testKeyResult = msg
                }.onFailure { err ->
                  testKeyIsSuccess = false
                  testKeyResult = err.message ?: "Błąd testu połączenia."
                }
              }
            },
            enabled = !isTestingKey,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan),
            border = BorderStroke(1.dp, TunerCyan.copy(alpha = 0.7f))
          ) {
            if (isTestingKey) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TunerCyan, strokeWidth = 2.dp)
              Spacer(Modifier.width(8.dp))
              Text("Sprawdzam klucz w Google Gemini...", fontSize = 11.5.sp)
            } else {
              Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("🧪 Przetestuj działanie klucza teraz", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }

          // Wynik testu
          if (testKeyResult != null) {
            Spacer(Modifier.height(8.dp))
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (testKeyIsSuccess) TunerCyan.copy(alpha = 0.15f) else TuningRed.copy(alpha = 0.15f)
              ),
              border = BorderStroke(1.dp, if (testKeyIsSuccess) TunerCyan.copy(alpha = 0.6f) else TuningRed.copy(alpha = 0.6f))
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (testKeyIsSuccess) Icons.Default.Check else Icons.Default.Warning,
                  contentDescription = null,
                  tint = if (testKeyIsSuccess) TunerCyan else TuningRed,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = testKeyResult ?: "",
                  fontSize = 11.sp,
                  color = if (testKeyIsSuccess) TextPrimary else TuningRed,
                  lineHeight = 14.sp
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Row {
          Button(
            onClick = {
              if (customApiKeyInput.isBlank()) {
                DatasheetExtractor.clearCustomApiKey(context)
                Toast.makeText(context, "Używany jest domyślny klucz z panelu Secrets!", Toast.LENGTH_SHORT).show()
              } else {
                DatasheetExtractor.saveApiKey(context, customApiKeyInput)
                Toast.makeText(context, "Zapisano klucz API!", Toast.LENGTH_SHORT).show()
              }
              showApiKeyDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = TunerCyan, contentColor = Color.Black)
          ) {
            Text("Zapisz klucz")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showApiKeyDialog = false }) {
          Text("Zamknij", color = TextSecondary)
        }
      },
      containerColor = DarkSurfaceElevated
    )
  }
}

private fun queryFileName(context: Context, uri: Uri): String? {
  return try {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (cursor.moveToFirst() && nameIndex >= 0) {
        cursor.getString(nameIndex)
      } else null
    }
  } catch (_: Exception) {
    null
  }
}
