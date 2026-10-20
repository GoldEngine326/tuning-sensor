package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.CharType
import com.example.model.ComponentCategory
import com.example.model.PinType
import com.example.ui.screens.CalPointDraft
import com.example.ui.screens.DeadTimePointDraft
import com.example.ui.screens.DeadTimeTableDraft
import com.example.ui.screens.PinDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Result data class representing all fields extracted from datasheet / photo / file / URL.
 */
data class ExtractedComponentData(
  val name: String? = null,
  val partNumber: String? = null,
  val category: ComponentCategory? = null,
  val shortDescription: String? = null,
  val flowRateCc: Double? = null,
  val flowPressureBar: Double? = null,
  val deadTimeTables: List<DeadTimeTableDraft> = emptyList(),
  val calibrationPoints: List<CalPointDraft> = emptyList(),
  val calInputUnit: String? = null,
  val calOutputUnit: String? = null,
  val charTitle: String? = null,
  val charType: CharType? = null,
  val resistancePoints: List<CalPointDraft> = emptyList(),
  val resInputUnit: String? = null,
  val resOutputUnit: String? = null,
  val resCharTitle: String? = null,
  val includeResistanceChar: Boolean = false,
  val pullupRequired: Boolean? = null,
  val pullupRecommendedValue: String? = null,
  val innerDiameterMm: Double? = null,
  val outerDiameterMm: Double? = null,
  val boltSpacingWidthMm: Double? = null,
  val boltSpacingHeightMm: Double? = null,
  val boltDiagonal1Mm: Double? = null,
  val boltDiagonal2Mm: Double? = null,
  val pins: List<PinDraft> = emptyList(),
  val importantNotes: String? = null,
  val summaryBullets: List<String> = emptyList()
)

object DatasheetExtractor {
  private const val TAG = "DatasheetExtractor"
  private const val PREFS_NAME = "tuning_sensor_ai_prefs"
  private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

  // Model recommended for multimodal reasoning and OCR table extraction
  private const val MODEL_NAME = "gemini-3.5-flash"
  private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  /**
   * Dokładnie oczyszcza wklejony lub przekazany klucz API z białych znaków,
   * znaków cudzysłowu, prefiksów takich jak GEMINI_API_KEY= lub Bearer,
   * oraz niewidocznych spacji zero-width.
   */
  fun sanitizeApiKey(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    var clean = raw.trim()
    clean = clean.removePrefix("GEMINI_API_KEY=").removePrefix("GEMINI_API_KEY :").removePrefix("GEMINI_API_KEY:")
    clean = clean.removePrefix("API_KEY=").removePrefix("API_KEY :").removePrefix("API_KEY:")
    clean = clean.removePrefix("Bearer ")
    clean = clean.replace("\"", "").replace("'", "")
    // Usuń wszelkie białe znaki w tym spacje twarde (\u00A0), BOM (\uFEFF) i zero-width (\u200B)
    clean = clean.filter { !it.isWhitespace() && it != '\u00A0' && it != '\u200B' && it != '\uFEFF' }

    // Jeśli użytkownik wkleił ciąg zawierający klucz Google AIzaSy... (39 znaków), wyodrębnij go precyzyjnie
    val matchAiza = Regex("""(AIzaSy[A-Za-z0-9_-]{33})""").find(clean)
    if (matchAiza != null) {
      return matchAiza.groupValues[1]
    }
    // Jeśli to klucz w nowym formacie AI Studio (AQ....)
    val matchAq = Regex("""(AQ\.[A-Za-z0-9_-]{20,80})""").find(clean)
    if (matchAq != null) {
      return matchAq.groupValues[1]
    }
    return clean
  }

  fun isValidKey(key: String?): Boolean {
    val clean = sanitizeApiKey(key)
    return clean.isNotBlank() &&
      clean != "MY_GEMINI_API_KEY" &&
      clean != "dummy" &&
      clean != "DUMMY" &&
      clean != "null"
  }

  /**
   * Retrieves active Gemini API key from BuildConfig or SharedPreferences.
   */
  fun getApiKey(context: Context? = null): String {
    // 1. Sprawdź najpierw klucz wbudowany z panelu Secrets w AI Studio (BuildConfig)
    val buildKey = sanitizeApiKey(try {
      BuildConfig.GEMINI_API_KEY
    } catch (_: Exception) {
      ""
    })
    if (isValidKey(buildKey)) {
      return buildKey
    }

    // 2. Jeśli brak w Secrets, sprawdź czy użytkownik zapisał własny klucz w telefonie
    if (context != null) {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val customKey = sanitizeApiKey(prefs.getString(KEY_CUSTOM_API_KEY, ""))
      if (isValidKey(customKey)) return customKey
    }

    return ""
  }

  /**
   * Zwraca opcjonalny klucz wprowadzony ręcznie w aplikacji (z SharedPreferences).
   */
  fun getCustomKeyOnly(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sanitizeApiKey(prefs.getString(KEY_CUSTOM_API_KEY, ""))
  }

  /**
   * Zwraca klucz z panelu Secrets (BuildConfig).
   */
  fun getBuildConfigKey(): String {
    val buildKey = sanitizeApiKey(try {
      BuildConfig.GEMINI_API_KEY
    } catch (_: Exception) {
      ""
    })
    return if (isValidKey(buildKey)) buildKey else ""
  }

  /**
   * Sprawdza, czy klucz API pochodzi bezpośrednio z panelu Secrets projektu.
   */
  fun isKeyConfiguredInProject(): Boolean {
    return isValidKey(getBuildConfigKey())
  }

  /**
   * Saves a user-provided custom Gemini API key.
   */
  fun saveApiKey(context: Context, key: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val sanitized = sanitizeApiKey(key)
    prefs.edit().putString(KEY_CUSTOM_API_KEY, sanitized).apply()
  }

  /**
   * Usuwa ręcznie zapisany klucz z pamięci telefonu, przywracając klucz z Secrets.
   */
  fun clearCustomApiKey(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
  }

  /**
   * Sprawdza czy aplikacja ma skonfigurowany dowolny poprawny klucz Gemini API.
   */
  fun hasApiKey(context: Context? = null): Boolean {
    return isKeyConfiguredInProject() || (context != null && isValidKey(getCustomKeyOnly(context)))
  }

  /**
   * Extracts data from a datasheet / table photo (e.g. photo of dead time table or calibration chart).
   */
  suspend fun extractFromDataImage(context: Context, imageUri: Uri): Result<ExtractedComponentData> =
    withContext(Dispatchers.IO) {
      try {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
          return@withContext Result.failure(
            IllegalStateException("Do automatycznego odczytu ze zdjęć tabel przez AI wymagany jest darmowy klucz Gemini API. Aby działało to w 100% samo bez wpisywania w telefonie, dodaj klucz GEMINI_API_KEY w panelu Secrets w AI Studio, albo wprowadź go raz w aplikacji (żółta ikona klucza).")
          )
        }

        val base64Data = compressImageToBase64(context, imageUri)
          ?: return@withContext Result.failure(IllegalArgumentException("Nie udało się odczytać wybranego zdjęcia z danymi."))

        val prompt = buildAutomotiveExtractionPrompt()
        val requestJson = buildGeminiRequestWithInlineData(
          prompt = prompt,
          mimeType = "image/jpeg",
          base64Data = base64Data
        )

        callGeminiApi(context, apiKey, requestJson)
      } catch (e: Exception) {
        Log.e(TAG, "extractFromDataImage failed", e)
        Result.failure(e)
      }
    }

  /**
   * Extracts data from a file (PDF, TXT, CSV, JSON, or image).
   */
  suspend fun extractFromFile(context: Context, fileUri: Uri, fileName: String): Result<ExtractedComponentData> =
    withContext(Dispatchers.IO) {
      try {
        val apiKey = getApiKey(context)
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(fileUri) ?: when {
          fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
          fileName.endsWith(".csv", ignoreCase = true) -> "text/csv"
          fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
          fileName.endsWith(".tsv", ignoreCase = true) -> "text/tab-separated-values"
          fileName.endsWith(".json", ignoreCase = true) -> "application/json"
          fileName.endsWith(".png", ignoreCase = true) -> "image/png"
          fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
          else -> "application/octet-stream"
        }

        // Jeśli to plik tekstowy lub CSV, spróbuj najpierw odczytać zawartość i sparsować lokalnie (bez konieczności użycia API key)
        if (mimeType.startsWith("text/") || mimeType == "application/json" || fileName.endsWith(".csv", ignoreCase = true) || fileName.endsWith(".txt", ignoreCase = true) || fileName.endsWith(".tsv", ignoreCase = true)) {
          val textContent = contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { it.readText() }
            ?: return@withContext Result.failure(IllegalArgumentException("Nie udało się odczytać zawartości pliku tekstowego."))

          // Sprawdź czy możemy odczytać dane z tabeli lokalnie na urządzeniu
          val localResult = parseTextTableLocally(textContent, fileName)
          if (localResult != null && (apiKey.isBlank() || localResult.deadTimeTables.isNotEmpty() || localResult.calibrationPoints.isNotEmpty())) {
            return@withContext Result.success(localResult)
          }

          if (apiKey.isBlank()) {
            return@withContext Result.failure(
              IllegalStateException("Do zaawansowanej analizy dokumentu tekstowego przez AI wymagany jest klucz Gemini API. Dodaj GEMINI_API_KEY w panelu Secrets w AI Studio lub podaj go w ikonie klucza.")
            )
          }

          val prompt = buildAutomotiveExtractionPrompt()
          val fullPrompt = "$prompt\n\n--- DOKUMENTACJA Z PLIKU ($fileName) ---\n$textContent"
          val requestJson = buildGeminiTextOnlyRequest(fullPrompt)
          return@withContext callGeminiApi(context, apiKey, requestJson)
        }

        if (apiKey.isBlank()) {
          return@withContext Result.failure(
            IllegalStateException("Do automatycznego odczytu zdjęć tabel i plików PDF przez sztuczną inteligencję wymagany jest klucz Gemini API. Aby działało to w 100% samo bez wpisywania w telefonie, dodaj klucz GEMINI_API_KEY w panelu Secrets w AI Studio lub podaj go raz w ikonie klucza.")
          )
        }

        val prompt = buildAutomotiveExtractionPrompt()

        // For binary files (PDF or images), encode to Base64
        val bytes = contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
          ?: return@withContext Result.failure(IllegalArgumentException("Nie udało się odczytać zawartości pliku."))

        val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val targetMime = if (mimeType == "application/pdf") "application/pdf" else "image/jpeg"

        val requestJson = buildGeminiRequestWithInlineData(
          prompt = prompt,
          mimeType = targetMime,
          base64Data = base64Data
        )

        callGeminiApi(context, apiKey, requestJson)
      } catch (e: Exception) {
        Log.e(TAG, "extractFromFile failed", e)
        Result.failure(e)
      }
    }

  /**
   * Extracts data from a remote URL (datasheet link, manufacturer website, or PDF URL).
   */
  suspend fun extractFromUrl(context: Context, urlString: String): Result<ExtractedComponentData> =
    withContext(Dispatchers.IO) {
      try {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
          return@withContext Result.failure(
            IllegalStateException("Brak klucza Gemini API. Podaj klucz API w ustawieniach lub panelu Secrets.")
          )
        }

        val trimmedUrl = urlString.trim()
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
          return@withContext Result.failure(IllegalArgumentException("Podaj prawidłowy link zaczynający się od https:// lub http://"))
        }

        // Download document or web page
        val req = Request.Builder()
          .url(trimmedUrl)
          .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/119.0")
          .build()

        val response = httpClient.newCall(req).execute()
        if (!response.isSuccessful) {
          return@withContext Result.failure(
            IllegalStateException("Błąd pobierania ze strony: HTTP ${response.code} (${response.message})")
          )
        }

        val body = response.body ?: return@withContext Result.failure(
          IllegalStateException("Pusta odpowiedź z podanego adresu URL.")
        )

        val contentType = response.header("Content-Type")?.lowercase() ?: ""
        val prompt = buildAutomotiveExtractionPrompt()

        if (contentType.contains("application/pdf") || trimmedUrl.endsWith(".pdf", ignoreCase = true)) {
          val pdfBytes = body.bytes()
          val base64Data = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
          val requestJson = buildGeminiRequestWithInlineData(
            prompt = "$prompt\n\nLink źródłowy: $trimmedUrl",
            mimeType = "application/pdf",
            base64Data = base64Data
          )
          return@withContext callGeminiApi(context, apiKey, requestJson)
        }

        if (contentType.contains("image/") || trimmedUrl.endsWith(".jpg", ignoreCase = true) || trimmedUrl.endsWith(".png", ignoreCase = true)) {
          val imgBytes = body.bytes()
          val base64Data = Base64.encodeToString(imgBytes, Base64.NO_WRAP)
          val requestJson = buildGeminiRequestWithInlineData(
            prompt = "$prompt\n\nLink źródłowy do zdjęcia: $trimmedUrl",
            mimeType = if (contentType.contains("png")) "image/png" else "image/jpeg",
            base64Data = base64Data
          )
          return@withContext callGeminiApi(context, apiKey, requestJson)
        }

        // HTML or text webpage
        val textBody = body.string()
        // Zachowaj pełną treść dokumentacji z tabelami pinoutu i wtyczek (do 200 000 znaków)
        val cleanText = stripHtml(textBody).take(200000)

        val fullPrompt = "$prompt\n\n--- ZAWARTOŚĆ STRONY Z DOKUMENTACJĄ ($trimmedUrl) ---\n$cleanText"
        val requestJson = buildGeminiTextOnlyRequest(fullPrompt)
        callGeminiApi(context, apiKey, requestJson)
      } catch (e: Exception) {
        Log.e(TAG, "extractFromUrl failed", e)
        Result.failure(e)
      }
    }

  /**
   * Generates sample mock extraction data for testing purposes (when testing without Gemini API key).
   */
  fun generateSampleDemoData(category: ComponentCategory): ExtractedComponentData {
    return when (category) {
      ComponentCategory.INJECTORS -> ExtractedComponentData(
        name = "Bosch EV14 630cc",
        partNumber = "0 280 158 117",
        category = ComponentCategory.INJECTORS,
        shortDescription = "Wysokooporowe wtryskiwacze EV14 630cc/min przy 3.0 bar ze stożkowym rozpylaniem.",
        flowRateCc = 630.0,
        flowPressureBar = 3.0,
        deadTimeTables = listOf(
          DeadTimeTableDraft(
            tableName = "3.0 bar (Diff)",
            points = androidx.compose.runtime.mutableStateListOf(
              DeadTimePointDraft(voltage = "8.0", timeMs = "2.35"),
              DeadTimePointDraft(voltage = "10.0", timeMs = "1.72"),
              DeadTimePointDraft(voltage = "12.0", timeMs = "1.30"),
              DeadTimePointDraft(voltage = "14.0", timeMs = "1.02"),
              DeadTimePointDraft(voltage = "16.0", timeMs = "0.86")
            ),
            notes = "Bosch Motorsport 3.0 bar test bench"
          ),
          DeadTimeTableDraft(
            tableName = "4.0 bar (Diff)",
            points = androidx.compose.runtime.mutableStateListOf(
              DeadTimePointDraft(voltage = "8.0", timeMs = "2.60"),
              DeadTimePointDraft(voltage = "10.0", timeMs = "1.92"),
              DeadTimePointDraft(voltage = "12.0", timeMs = "1.45"),
              DeadTimePointDraft(voltage = "14.0", timeMs = "1.15"),
              DeadTimePointDraft(voltage = "16.0", timeMs = "0.98")
            ),
            notes = "Test przy podwyższonym ciśnieniu listwy 4.0 bar"
          )
        ),
        pins = listOf(
          PinDraft(pinNumber = "1", name = "Zasilanie +12V (Po stacyjce / Przekaźnik)", description = "Zasilanie cewki wtryskiwacza", wireColor = "Czerwony", type = PinType.POWER, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "2", name = "Sterowanie ECU (Low-Side Injector)", description = "Kluczowanie masą z wyjścia wtrysku ECU", wireColor = "Niebieski", type = PinType.INJECTOR, connectorName = "Wtyczka A")
        ),
        importantNotes = "• Rezystancja cewki: ~12.0 Ω (High-Z).\n• Kąt strugi: 20° dual spray.\n• Wymaga gniazda USCAR / EV6.",
        summaryBullets = listOf(
          "✔ Rozpoznano: Bosch EV14 630cc (0 280 158 117)",
          "✔ Odczytano wydajność: 630 cc/min @ 3.0 bar",
          "✔ Wyodrębniono 2 tabele Dead Time (3.0 bar i 4.0 bar)",
          "✔ Skonfigurowano 2 piny (Zasilanie +12V i Kluczowanie ECU)"
        )
      )

      ComponentCategory.THROTTLE -> ExtractedComponentData(
        name = "Przepustnica Bosch 68mm DBW",
        partNumber = "0 280 750 156",
        category = ComponentCategory.THROTTLE,
        shortDescription = "Elektroniczna przepustnica DBW 68 mm ze zintegrowanym silnikiem DC i podwójnym potencjometrem TPS.",
        innerDiameterMm = 68.0,
        outerDiameterMm = 76.0,
        boltSpacingWidthMm = 60.0,
        boltSpacingHeightMm = 60.0,
        boltDiagonal1Mm = 84.8,
        boltDiagonal2Mm = 84.8,
        pins = listOf(
          PinDraft(pinNumber = "1", name = "Silnik Motor +", description = "Sterowanie mostkiem H (H-Bridge A)", wireColor = "Żółty", type = PinType.MOTOR, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "2", name = "Silnik Motor -", description = "Sterowanie mostkiem H (H-Bridge B)", wireColor = "Niebieski", type = PinType.MOTOR, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "3", name = "Sensor GND", description = "Masa sensoryczna z ECU", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "4", name = "TPS 1 Sygnał", description = "Potencjometr główny 0.5V - 4.5V", wireColor = "Biały", type = PinType.TPS, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "5", name = "+5V VREF", description = "Napięcie referencyjne z ECU", wireColor = "Czerwony", type = PinType.VREF, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "6", name = "TPS 2 Sygnał", description = "Potencjometr redundantny 4.5V - 0.5V", wireColor = "Zielony", type = PinType.TPS, connectorName = "Wtyczka A")
        ),
        importantNotes = "• Wymiary gardzieli: 68.0 mm wewnątrz, 76.0 mm zewnątrz (3 cale).\n• Rozstaw śrub: 60 × 60 mm, przekątna 84.8 mm.\n• Nie wymaga charakterystyki napięciowej ani opornościowej.",
        summaryBullets = listOf(
          "✔ Wykryto: Przepustnica Bosch 68mm DBW",
          "✔ Odczytano wymiary: Średnica 68/76 mm, rozstaw śrub 60×60 mm",
          "✔ Skonfigurowano pinout DBW (6 pinów)",
          "✔ Wyłączono niepotrzebne charakterystyki"
        )
      )

      ComponentCategory.TEMP_SENSOR -> ExtractedComponentData(
        name = "Czujnik temperatury IAT Bosch M12",
        partNumber = "0 280 130 039",
        category = ComponentCategory.TEMP_SENSOR,
        shortDescription = "Szybki czujnik temperatury powietrza dolotowego (IAT) z otwartym termistorem NTC.",
        pullupRequired = true,
        pullupRecommendedValue = "2.49 kΩ do +5V VREF",
        includeResistanceChar = true,
        resCharTitle = "Charakterystyka opornościowa Bosch NTC M12",
        resInputUnit = "°C",
        resOutputUnit = "Ω",
        resistancePoints = listOf(
          CalPointDraft(inVal = "-20", inUnit = "°C", outVal = "15462", outUnit = "Ω"),
          CalPointDraft(inVal = "0", inUnit = "°C", outVal = "5896", outUnit = "Ω"),
          CalPointDraft(inVal = "20", inUnit = "°C", outVal = "2500", outUnit = "Ω"),
          CalPointDraft(inVal = "40", inUnit = "°C", outVal = "1175", outUnit = "Ω"),
          CalPointDraft(inVal = "80", inUnit = "°C", outVal = "332", outUnit = "Ω"),
          CalPointDraft(inVal = "100", inUnit = "°C", outVal = "177", outUnit = "Ω")
        ),
        pins = listOf(
          PinDraft(pinNumber = "1", name = "Sensor GND", description = "Masa sensoryczna z ECU", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "2", name = "Sygnał IAT (NTC)", description = "Wejście analogowe z rezystorem pull-up", wireColor = "Biały", type = PinType.SIGNAL_IN, connectorName = "Wtyczka A")
        ),
        importantNotes = "• Termistor NTC (opór maleje wraz ze wzrostem temp).\n• Wymagany rezystor pull-up 2.49 kΩ do szyny +5V VREF.",
        summaryBullets = listOf(
          "✔ Wykryto: Czujnik IAT Bosch M12 (0 280 130 039)",
          "✔ Odczytano tabelę NTC: 6 punktów (-20°C do 100°C)",
          "✔ Ustawiono pull-up: 2.49 kΩ do +5V VREF",
          "✔ Przypisano 2 piny"
        )
      )

      ComponentCategory.ECU -> ExtractedComponentData(
        name = "uaEFI Standalone ECU (5 wtyczek Molex)",
        partNumber = "uaEFI-v1.4",
        category = ComponentCategory.ECU,
        shortDescription = "Samodzielny sterownik silnika rusEFI uaEFI z 5 złączami Molex Mini-Fit Jr. i wbudowanym kontrolerem sondy LSU 4.9.",
        pins = listOf(
          // Wtyczka A (8 pinów)
          PinDraft(pinNumber = "1", name = "Masa główna Power GND 1", description = "Masa zasilająca do bloku silnika", wireColor = "Czarny", type = PinType.GROUND, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "2", name = "Masa główna Power GND 2", description = "Masa zasilająca do bloku silnika", wireColor = "Czarny", type = PinType.GROUND, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "3", name = "Zasilanie +12V po stacyjce", description = "Sterowanie przekaźnikiem głównym / zapłon", wireColor = "Czerwony", type = PinType.POWER, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "4", name = "+12V stałe z akumulatora", description = "Podtrzymanie pamięci zegara i NVRAM", wireColor = "Żółty", type = PinType.POWER, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "5", name = "Wyjście przekaźnika pompy paliwa", description = "Low-Side Fuel Pump Relay", wireColor = "Niebieski", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "6", name = "Wyjście przekaźnika wentylatora 1", description = "Low-Side Fan Relay 1", wireColor = "Zielony", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "7", name = "Zawór wolnych obrotów Idle PWM", description = "Sterowanie zaworem IACV", wireColor = "Szary", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "8", name = "Zawór doładowania Boost Solenoid", description = "PWM Boost control", wireColor = "Fioletowy", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka A"),

          // Wtyczka B (18 pinów)
          PinDraft(pinNumber = "1", name = "Wtryskiwacz 1 (INJ 1)", description = "Low-Side cylindra 1", wireColor = "Niebieski", type = PinType.INJECTOR, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "2", name = "Wtryskiwacz 2 (INJ 2)", description = "Low-Side cylindra 2", wireColor = "Niebieski", type = PinType.INJECTOR, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "3", name = "Wtryskiwacz 3 (INJ 3)", description = "Low-Side cylindra 3", wireColor = "Niebieski", type = PinType.INJECTOR, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "4", name = "Wtryskiwacz 4 (INJ 4)", description = "Low-Side cylindra 4", wireColor = "Niebieski", type = PinType.INJECTOR, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "5", name = "Wtryskiwacz 5 (INJ 5)", description = "Low-Side cylindra 5", wireColor = "Niebieski", type = PinType.INJECTOR, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "6", name = "Wtryskiwacz 6 (INJ 6)", description = "Low-Side cylindra 6", wireColor = "Niebieski", type = PinType.INJECTOR, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "7", name = "Cewka zapłonowa 1 (IGN 1)", description = "Sygnał 5V logic / wyzwalanie", wireColor = "Żółty", type = PinType.IGNITION, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "8", name = "Cewka zapłonowa 2 (IGN 2)", description = "Sygnał 5V logic / wyzwalanie", wireColor = "Żółty", type = PinType.IGNITION, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "9", name = "Cewka zapłonowa 3 (IGN 3)", description = "Sygnał 5V logic / wyzwalanie", wireColor = "Żółty", type = PinType.IGNITION, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "10", name = "Cewka zapłonowa 4 (IGN 4)", description = "Sygnał 5V logic / wyzwalanie", wireColor = "Żółty", type = PinType.IGNITION, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "11", name = "Cewka zapłonowa 5 (IGN 5)", description = "Sygnał 5V logic / wyzwalanie", wireColor = "Żółty", type = PinType.IGNITION, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "12", name = "Cewka zapłonowa 6 (IGN 6)", description = "Sygnał 5V logic / wyzwalanie", wireColor = "Żółty", type = PinType.IGNITION, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "13", name = "Wyjście pomocnicze AUX 1", description = "Low-Side driver", wireColor = "Biały", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "14", name = "Wyjście pomocnicze AUX 2", description = "Low-Side driver", wireColor = "Biały", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "15", name = "Wyjście pomocnicze AUX 3", description = "Low-Side driver", wireColor = "Biały", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "16", name = "Wyjście pomocnicze AUX 4", description = "Low-Side driver", wireColor = "Biały", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "17", name = "Obrotomierz TACHO", description = "Sygnał obrotów do licznika 12V pullup", wireColor = "Zielony", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka B"),
          PinDraft(pinNumber = "18", name = "Zawór zmiennych faz VVT 1", description = "Solenoid VVT Cam phasing PWM", wireColor = "Pomarańczowy", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka B"),

          // Wtyczka C (20 pinów)
          PinDraft(pinNumber = "1", name = "+5V VREF zasilanie czujników", description = "Napięcie referencyjne z ECU", wireColor = "Czerwony", type = PinType.VREF, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "2", name = "Masa sensoryczna Sensor GND", description = "Czysta masa analogowa czujników", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "3", name = "Czujnik temp. cieczy (CLT)", description = "Wejście analogowe NTC", wireColor = "Biały", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "4", name = "Czujnik temp. dolotu (IAT)", description = "Wejście analogowe NTC", wireColor = "Biały", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "5", name = "Położenie przepustnicy TPS 1", description = "Wejście analogowe 0-5V", wireColor = "Zielony", type = PinType.TPS, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "6", name = "Czujnik ciśnienia MAP", description = "Wejście analogowe 0-5V", wireColor = "Żółty", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "7", name = "Czujnik wału Crank Trigger +", description = "Sygnał VR+ lub Hall wału korbowego", wireColor = "Czarny", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "8", name = "Czujnik wału Crank Trigger -", description = "Masa VR- lub ekran przewodu", wireColor = "Szary", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "9", name = "Czujnik wałka Cam Trigger +", description = "Sygnał VR+ lub Hall wałka rozrządu", wireColor = "Niebieski", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "10", name = "Czujnik wałka Cam Trigger -", description = "Masa VR- lub ekran", wireColor = "Szary", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "11", name = "Czujnik ciśnienia oleju", description = "Wejście analogowe 0.5-4.5V", wireColor = "Fioletowy", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "12", name = "Czujnik ciśnienia paliwa", description = "Wejście analogowe 0.5-4.5V", wireColor = "Pomarańczowy", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "13", name = "Czujnik stukania Knock 1", description = "Wejście piezoelektryczne", wireColor = "Biały", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "14", name = "Czujnik stukania Knock 2", description = "Wejście piezoelektryczne", wireColor = "Biały", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "15", name = "Czujnik Flex Fuel (E85)", description = "Wejście częstotliwościowe 50-150Hz", wireColor = "Żółty", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "16", name = "Przełącznik procedury startu (Launch)", description = "Zwierny do masy (Digital In)", wireColor = "Zielony", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "17", name = "Wejście analogowe dodatkowe AIN 1", description = "0-5V Spare Analog", wireColor = "Różowy", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "18", name = "Wejście analogowe dodatkowe AIN 2", description = "0-5V Spare Analog", wireColor = "Szary", type = PinType.SIGNAL_IN, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "19", name = "Dodatkowa masa czujników Sensor GND 2", description = "Masa ekranów czujników", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka C"),
          PinDraft(pinNumber = "20", name = "Dodatkowe +5V VREF 2", description = "Drugie wyjście referencyjne", wireColor = "Czerwony", type = PinType.VREF, connectorName = "Wtyczka C"),

          // Wtyczka D (16 pinów)
          PinDraft(pinNumber = "1", name = "Przepustnica DBW Motor +", description = "Wyjście mostka H-Bridge silnika", wireColor = "Żółty", type = PinType.MOTOR, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "2", name = "Przepustnica DBW Motor -", description = "Wyjście mostka H-Bridge silnika", wireColor = "Niebieski", type = PinType.MOTOR, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "3", name = "Potencjometr przepustnicy TPS 2", description = "Ścieżka redundantna DBW", wireColor = "Biały", type = PinType.TPS, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "4", name = "Pedał gazu PPS 1", description = "Główny potencjometr pedału gazu", wireColor = "Zielony", type = PinType.TPS, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "5", name = "Pedał gazu PPS 2", description = "Drugi potencjometr pedału gazu", wireColor = "Pomarańczowy", type = PinType.TPS, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "6", name = "Magistrala CAN 1 High", description = "CAN-Bus 2.0B 500kbps / 1Mbps", wireColor = "Żółty", type = PinType.COMMUNICATION, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "7", name = "Magistrala CAN 1 Low", description = "CAN-Bus 2.0B 500kbps / 1Mbps", wireColor = "Zielony", type = PinType.COMMUNICATION, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "8", name = "Magistrala CAN 2 High", description = "Druga magistrala CAN (opcja)", wireColor = "Biały", type = PinType.COMMUNICATION, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "9", name = "Magistrala CAN 2 Low", description = "Druga magistrala CAN (opcja)", wireColor = "Niebieski", type = PinType.COMMUNICATION, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "10", name = "Przełącznik map Map Switch", description = "Wybór mapy paliwa/zapłonu", wireColor = "Czarny", type = PinType.SIGNAL_IN, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "11", name = "Czujnik prędkości pojazdu VSS", description = "Sygnał prędkości Hall", wireColor = "Szary", type = PinType.SIGNAL_IN, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "12", name = "Wejście sprzęgła Clutch Switch", description = "Flat shift / Procedura", wireColor = "Czerwony", type = PinType.SIGNAL_IN, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "13", name = "Zawór zmiennych faz VVT 2", description = "Drugi wałek rozrządu PWM", wireColor = "Fioletowy", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "14", name = "Wyjście kontrolki Check Engine", description = "Low-Side MIL lamp", wireColor = "Brązowy", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "15", name = "Masa sensoryczna DBW GND", description = "Dedykowana masa TPS2/PPS", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka D"),
          PinDraft(pinNumber = "16", name = "+5V VREF dla pedału PPS", description = "Dedykowane 5V dla pedału", wireColor = "Czerwony", type = PinType.VREF, connectorName = "Wtyczka D"),

          // Wtyczka E (6 pinów)
          PinDraft(pinNumber = "1", name = "Sonda LSU 4.9: IP", description = "Prąd pompy komórki Nernsta (Pump current)", wireColor = "Czerwony", type = PinType.SIGNAL_IN, connectorName = "Wtyczka E"),
          PinDraft(pinNumber = "2", name = "Sonda LSU 4.9: VM", description = "Wirtualna masa sondy szerokopasmowej", wireColor = "Żółty", type = PinType.SENSOR_GND, connectorName = "Wtyczka E"),
          PinDraft(pinNumber = "3", name = "Sonda LSU 4.9: H-", description = "Sterowanie masą grzałki sondy (PWM Heater)", wireColor = "Biały", type = PinType.HEATER, connectorName = "Wtyczka E"),
          PinDraft(pinNumber = "4", name = "Sonda LSU 4.9: H+", description = "Zasilanie grzałki sondy (+12V z przekaźnika)", wireColor = "Szary", type = PinType.POWER, connectorName = "Wtyczka E"),
          PinDraft(pinNumber = "5", name = "Sonda LSU 4.9: IA / RT", description = "Rezystor kalibracyjny wtyczki sondy", wireColor = "Zielony", type = PinType.SIGNAL_IN, connectorName = "Wtyczka E"),
          PinDraft(pinNumber = "6", name = "Sonda LSU 4.9: UN / RE", description = "Napięcie ogniwa Nernsta (Nernst voltage)", wireColor = "Czarny", type = PinType.SIGNAL_IN, connectorName = "Wtyczka E")
        ),
        importantNotes = "• Sterownik silnika rusEFI uaEFI posiada 5 dedykowanych złączy Molex Mini-Fit Jr. 4.2mm:\n  - Wtyczka A (8 pinów): Zasilanie i przekaźniki\n  - Wtyczka B (18 pinów): Wtryskiwacze 1..6, Cewki zapłonowe 1..6, wyjścia AUX\n  - Wtyczka C (20 pinów): Czujniki analogowe CLT, IAT, TPS, MAP, czujniki wału Crank/Cam\n  - Wtyczka D (16 pinów): Przepustnica DBW, pedał PPS, podwójna magistrala CAN\n  - Wtyczka E (6 pinów): Zintegrowany kontroler szerokopasmowej sondy Bosch LSU 4.9.\n• Zasilanie sondy (H+) wymaga przekaźnika z bezpiecznikiem 5A.",
        summaryBullets = listOf(
          "✔ Rozpoznano: uaEFI Standalone ECU (rusEFI)",
          "✔ Odczytano wszystkie 5 wtyczek Molex (Wtyczka A, B, C, D, E)",
          "✔ Skonfigurowano łącznie 68 pinów sterownika silnika",
          "✔ Skonfigurowano wbudowany kontroler szerokopasmowej sondy LSU 4.9"
        )
      )

      else -> ExtractedComponentData(
        name = "Bosch 3.0 Bar MAP Sensor",
        partNumber = "0 281 002 401",
        category = ComponentCategory.MAP_SENSOR,
        shortDescription = "Czujnik ciśnienia bezwzględnego w kolektorze (MAP) do 300 kPa (2.0 bar doładowania).",
        charTitle = "Kalibracja liniowa MAP (kPa vs V)",
        charType = CharType.VOLTAGE_PRESSURE,
        calInputUnit = "kPa",
        calOutputUnit = "V",
        calibrationPoints = listOf(
          CalPointDraft(inVal = "20.0", inUnit = "kPa", outVal = "0.40", outUnit = "V"),
          CalPointDraft(inVal = "100.0", inUnit = "kPa", outVal = "1.60", outUnit = "V"),
          CalPointDraft(inVal = "200.0", inUnit = "kPa", outVal = "3.10", outUnit = "V"),
          CalPointDraft(inVal = "300.0", inUnit = "kPa", outVal = "4.65", outUnit = "V")
        ),
        pins = listOf(
          PinDraft(pinNumber = "1", name = "Sensor GND", description = "Masa sensoryczna z ECU", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "2", name = "+5V VREF", description = "Stabilizowane zasilanie 5.0V z ECU", wireColor = "Czerwony", type = PinType.VREF, connectorName = "Wtyczka A"),
          PinDraft(pinNumber = "3", name = "Sygnał MAP", description = "Napięcie wyjściowe 0.4V - 4.65V", wireColor = "Biały", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka A")
        ),
        importantNotes = "• Zakres pomiarowy: 20 - 300 kPa.\n• Wzór w EMU: 0.4V = 20 kPa, 4.65V = 300 kPa.",
        summaryBullets = listOf(
          "✔ Wykryto: Bosch 3.0 Bar MAP Sensor (0 281 002 401)",
          "✔ Odczytano krzywą kalibracyjną: 4 punkty (20-300 kPa vs V)",
          "✔ Skonfigurowano 3 piny (GND, 5V, Sygnał)"
        )
      )
    }
  }

  /**
   * Lokalne parsowanie tabel z plików CSV / TXT / TSV bez konieczności używania klucza API.
   */
  fun parseTextTableLocally(textContent: String, fileName: String): ExtractedComponentData? {
    val lines = textContent.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return null

    val dtPoints = mutableListOf<DeadTimePointDraft>()
    val calPoints = mutableListOf<CalPointDraft>()
    val resPoints = mutableListOf<CalPointDraft>()
    val pinsList = mutableListOf<PinDraft>()

    var activeConnector = "Wtyczka A"

    for (line in lines) {
      // Wykrywanie nagłówka nowej wtyczki, np. "Connector A", "Wtyczka 2", "### Connector C", "Złącze Molex B"
      val connHeaderMatch = Regex("""^(?:#{1,4}\s*)?(?:Wtyczka|Connector|Złącze|Plug|Header)\s+([A-Za-z0-9\-_]+)""", RegexOption.IGNORE_CASE).find(line)
      if (connHeaderMatch != null) {
        val rawConn = connHeaderMatch.groupValues[1].uppercase()
        activeConnector = "Wtyczka $rawConn"
        continue
      }

      // Wykrywanie definicji pinów, np. "Pin 1: GND", "A1 - Main Relay", "B17 - Low-side", "1 | Sygnał MAP | Biały"
      val pinMatch = Regex("""^(?:Pin\s*)?([A-Za-z]?\d+)[\s:\-–—|]+([^\-,:;|]+)(?:[\-–—:;|](.+))?""", RegexOption.IGNORE_CASE).find(line)
      if (pinMatch != null && !line.matches(Regex("""^[\d.,\s\t;|\-]+$"""))) {
        val rawNum = pinMatch.groupValues[1]
        val pinName = pinMatch.groupValues[2].trim()
        val pinDesc = pinMatch.groupValues.getOrNull(3)?.trim() ?: ""

        var pinConn = activeConnector
        var cleanNum = rawNum

        val letterMatch = Regex("""^([A-Ea-e])(\d+)$""").find(rawNum)
        if (letterMatch != null) {
          pinConn = "Wtyczka ${letterMatch.groupValues[1].uppercase()}"
          cleanNum = letterMatch.groupValues[2]
        }

        val pinType = when {
          pinName.contains("can", ignoreCase = true) -> PinType.COMMUNICATION
          pinName.contains("ign", ignoreCase = true) || pinName.contains("cewk", ignoreCase = true) || pinName.contains("zapłon", ignoreCase = true) -> PinType.IGNITION
          pinName.contains("crank", ignoreCase = true) || pinName.contains("cam", ignoreCase = true) || pinName.contains("vr", ignoreCase = true) || pinName.contains("hall", ignoreCase = true) || pinName.contains("trigger", ignoreCase = true) -> PinType.SIGNAL_IN
          pinName.contains("gnd", ignoreCase = true) || pinName.contains("masa", ignoreCase = true) ->
            if (pinName.contains("sensor", ignoreCase = true)) PinType.SENSOR_GND else PinType.GROUND
          pinName.contains("5v", ignoreCase = true) || pinName.contains("vref", ignoreCase = true) -> PinType.VREF
          pinName.contains("12v", ignoreCase = true) || pinName.contains("power", ignoreCase = true) || pinName.contains("zasil", ignoreCase = true) || pinName.contains("bateria", ignoreCase = true) || pinName.contains("relay", ignoreCase = true) -> PinType.POWER
          pinName.contains("tps", ignoreCase = true) || pinName.contains("pps", ignoreCase = true) || pinName.contains("pedal", ignoreCase = true) -> PinType.TPS
          pinName.contains("inj", ignoreCase = true) || pinName.contains("wtrysk", ignoreCase = true) -> PinType.INJECTOR
          pinName.contains("motor", ignoreCase = true) || pinName.contains("silnik", ignoreCase = true) || pinName.contains("dbw", ignoreCase = true) -> PinType.MOTOR
          pinName.contains("lsu", ignoreCase = true) || pinName.contains("lambda", ignoreCase = true) || pinName.contains("heater", ignoreCase = true) -> PinType.HEATER
          pinName.contains("out", ignoreCase = true) || pinName.contains("sygnał wy", ignoreCase = true) || pinName.contains("low-side", ignoreCase = true) || pinName.contains("ls", ignoreCase = true) -> PinType.SIGNAL_OUT
          pinName.contains("in", ignoreCase = true) || pinName.contains("sygnał we", ignoreCase = true) || pinName.contains("clt", ignoreCase = true) || pinName.contains("iat", ignoreCase = true) -> PinType.SIGNAL_IN
          else -> PinType.OTHER
        }
        pinsList.add(
          PinDraft(
            pinNumber = cleanNum,
            name = pinName,
            description = pinDesc,
            type = pinType,
            connectorName = pinConn
          )
        )
        continue
      }

      // Wykrywanie par liczb w wierszach tabeli (rozdzielonych średnikiem, przecinkiem, tabulatorem lub spacją)
      val cleanLine = line.replace(",", ".")
      val tokens = cleanLine.split(Regex("""[;\t\s|]+""")).filter { it.isNotBlank() }
      if (tokens.size >= 2) {
        val v1 = tokens[0].toDoubleOrNull()
        val v2 = tokens[1].toDoubleOrNull()
        if (v1 != null && v2 != null) {
          if (v1 in 6.0..19.0 && v2 in 0.1..8.0) {
            // Napięcie (V) vs Czas martwy (ms)
            dtPoints.add(DeadTimePointDraft(voltage = v1.toString(), timeMs = v2.toString()))
          } else if (v1 in -50.0..160.0 && v2 in 30.0..100000.0) {
            // Temperatura (°C) vs Rezystancja (Ω) dla czujników NTC
            resPoints.add(CalPointDraft(inVal = v1.toString(), inUnit = "°C", outVal = v2.toString(), outUnit = "Ω"))
          } else if (v2 in 0.0..5.5 && v1 > 0.0) {
            // Wartość wejściowa (kPa / bar) vs Napięcie wyjściowe (V)
            val unit = if (v1 > 15.0) "kPa" else "bar"
            calPoints.add(CalPointDraft(inVal = v1.toString(), inUnit = unit, outVal = v2.toString(), outUnit = "V"))
          }
        }
      }
    }

    if (dtPoints.isEmpty() && calPoints.isEmpty() && resPoints.isEmpty() && pinsList.isEmpty()) {
      return null
    }

    val distinctConnectors = pinsList.map { it.connectorName.ifBlank { "Wtyczka A" } }.distinct()
    val inferredCategory = when {
      distinctConnectors.size >= 2 -> ComponentCategory.ECU
      dtPoints.isNotEmpty() -> ComponentCategory.INJECTORS
      resPoints.isNotEmpty() -> ComponentCategory.TEMP_SENSOR
      calPoints.isNotEmpty() -> ComponentCategory.MAP_SENSOR
      pinsList.size >= 5 && pinsList.any { it.type == PinType.MOTOR || it.type == PinType.TPS } -> ComponentCategory.THROTTLE
      pinsList.size >= 12 -> ComponentCategory.ECU
      else -> null
    }

    val baseName = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
    val bullets = mutableListOf<String>()
    if (distinctConnectors.size > 1) bullets.add("✔ Odczytano pinout ze wszystkich ${distinctConnectors.size} wtyczek (${distinctConnectors.joinToString(", ")}) — łącznie ${pinsList.size} pinów")
    else if (pinsList.isNotEmpty()) bullets.add("✔ Wykryto pinout (${pinsList.size} pinów)")
    if (dtPoints.isNotEmpty()) bullets.add("✔ Odczytano lokalnie tabelę Dead Time (${dtPoints.size} punktów)")
    if (calPoints.isNotEmpty()) bullets.add("✔ Odczytano lokalnie kalibrację ciśnienia (${calPoints.size} punktów)")
    if (resPoints.isNotEmpty()) bullets.add("✔ Odczytano lokalnie rezystancję NTC (${resPoints.size} punktów)")
    if (pinsList.isNotEmpty()) bullets.add("✔ Wykryto pinout (${pinsList.size} pinów)")

    return ExtractedComponentData(
      name = baseName.ifBlank { null },
      category = inferredCategory,
      shortDescription = "Dane odczytane lokalnie na urządzeniu z pliku $fileName",
      deadTimeTables = if (dtPoints.isNotEmpty()) listOf(
        DeadTimeTableDraft(
          tableName = "Tabela z pliku $fileName",
          points = androidx.compose.runtime.mutableStateListOf<DeadTimePointDraft>().apply { addAll(dtPoints) }
        )
      ) else emptyList(),
      calibrationPoints = calPoints,
      resistancePoints = resPoints,
      includeResistanceChar = resPoints.isNotEmpty(),
      pins = pinsList,
      summaryBullets = bullets
    )
  }

  // --- Internal Gemini helpers ---

  private fun buildAutomotiveExtractionPrompt(): String {
    return """
Jesteś profesjonalnym systemem OCR i inżynierem tuningu motorsportu, elektroniki samochodowej oraz samodzielnych sterowników silnika (Standalone ECU: UAEFI, EMU Black, Ecumaster, TunerStudio, Megasquirt, MaxxECU, Haltech, Link ECU).
Twoim zadaniem jest bezbłędny, skrupulatny i KOMPLETNY odczyt tabel, wykresów, schematów pinoutu oraz danych kalibracyjnych z załączonego zdjęcia, linku lub dokumentu.

BARDZO WAŻNE WYMAGANIA JĘZYKOWE (JĘZYK POLSKI):
1. Wszystkie generowane teksty, opisy (shortDescription), uwagi techniczne (importantNotes, notes), nazwy wtyczek (connectorName) i tabel (tableName) oraz wypunktowane podsumowanie (summaryBullets) MUSZĄ BYĆ BEZWZGLĘDNIE W 100% W JĘZYKU POLSKIM. Nigdy nie używaj języka angielskiego w opisach!

BARDZO WAŻNA ZASADA DLA STEROWNIKÓW SILNIKA (ECU, UAEFI, STANDALONE, EMU BLACK, MAXXECU, SPEEDUINO) Z WIELOMA WTYCZKAMI / ZŁĄCZAMI:
2. Jeśli urządzenie (np. standalone ECU UAEFI, EMU Black, Ecumaster, Speeduino, MaxxECU, moduł PDM, skrzynka przekaźników) posiada WIĘCEJ NIŻ JEDNĄ WTYCZKĘ:
   - NP. W PRZYPADKU STEROWNIKA UAEFI: JEST TAM AŻ 5 WTYCZEK MOLEX:
     * Wtyczka A: 8 pinów (Zasilanie, masy główne, przekaźnik główny)
     * Wtyczka B: 18 pinów (Wtryskiwacze 1..6, Cewki zapłonowe 1..6, wyjścia Low-Side)
     * Wtyczka C: 20 pinów (Czujniki analogowe: CLT, IAT, TPS, MAP, Trigger VR/Hall wału i wałka, VREF +5V, Sensor GND)
     * Wtyczka D: 16 pinów (Przepustnica DBW H-Bridge Motor +/-, Pedał gazu PPS, magistrala CAN-Bus High/Low, wejścia cyfrowe)
     * Wtyczka E: 6 pinów (Sonda szerokopasmowa Wideband Lambda LSU 4.9: IP, VM, H-, H+, IA/RT, UN/RE)
   - BEZWZGLĘDNY NAKAZ: MUSISZ ODCZYTAĆ I ZWRÓCIĆ PINY DLA WSZYSTKICH 5 WTYCZEK BEZ WYJĄTKU!
   - NIGDY NIE ZATRZYMUJ SIĘ PO PIERWSZEJ WTYCZCE! Jeśli na stronie/schemacie/zdjęciu jest 5 wtyczek, w tablicy "pins" lub "connectors" MUSZĄ znaleźć się piny dla DOKŁADNIE KAŻDEJ Z TYCH 5 WTYCZEK!
   - W KAŻDYM obiekcie w tablicy "pins" MUSISZ podać pole "connectorName" z nazwą wtyczki, do której pin należy (np. "connectorName": "Wtyczka A", "connectorName": "Wtyczka B", "connectorName": "Wtyczka C", "connectorName": "Wtyczka D", "connectorName": "Wtyczka E" lub "Wtyczka 1", "Wtyczka 2", itd.).
   - Numery pinów ("pinNumber") numeruj zgodnie z numeracją danej wtyczki (np. 1..8, 1..18, 1..20 itd. lub A1..A8, B1..B18 itd.).
   - Prawidłowo przypisuj "type" dla każdego pinu:
     * POWER (+12V, zasilanie po stacyjce, bateria)
     * GROUND (masa silnika, masa zasilania)
     * SENSOR_GND (masa czujników analogowych z ECU)
     * VREF (+5V referencyjne dla czujników TPS, MAP, ciśnienia)
     * INJECTOR (wyjścia sterujące wtryskiwaczami Low-Side)
     * IGNITION (wyjścia zapłonowe / wyzwalające cewki)
     * TPS (wejścia położenia przepustnicy TPS1 / TPS2 lub pedału PPS)
     * TRIGGER (czujnik wału korbowego Crank VR/Hall lub wałka Cam)
     * CAN_BUS (magistrala CAN High / CAN Low)
     * SIGNAL_IN (wejścia czujników temperatury IAT/CLT, wejścia analogowe)
     * SIGNAL_OUT (wyjścia zaworów Boost, VVT, pompy paliwa, obrotomierza)
     * MOTOR (silnik przepustnicy DBW H-Bridge)
     * HEATER (grzałka sondy Lambda PWM)
     * OTHER (pozostałe sygnały)

BARDZO WAŻNA ZASADA DLA WTRYSKIWACZY I TABEL DEAD TIME (BATERIA CIŚNIEŃ):
3. Jeśli zdjęcie lub dokument zawiera tabelę czasów martwych (Dead Time / Battery Voltage Offset / Latency / Czas martwy) dla WIELU CIŚNIEŃ (np. od 2.5 bar do 6.0 bar co 0.5 bar – 8 wierszy lub kolumn):
   - MUSISZ WYODRĘBNIĆ WSZYSTKIE POZIOMY CIŚNIENIA! Każdy poziom ciśnienia to OSOBNY ELEMENT w tablicy "deadTimeTables".
   - Jeśli na zdjęciu jest 8 wierszy z ciśnieniami, tablica "deadTimeTables" MUSI zawierać DOKŁADNIE 8 OBIEKTÓW!

ZASADY DLA POZOSTAŁYCH CZUJNIKÓW:
4. Dla czujników MAP i ciśnienia: odczytaj całą charakterystykę kalibracyjną (kPa/bar vs V) do "calibrationPoints".
5. Dla czujników temperatury (NTC): odczytaj pełną tabelę oporności (°C vs Ω) do "resistancePoints" oraz zaznacz pullupRequired = true.
6. Dla przepustnic (DBW): odczytaj średnice (wewnętrzna i zewnętrzna), rozstawy śrub montażowych oraz schemat pinoutu DBW.

STRUKTURA ZWRACANEGO JSON:
Możesz zwrócić piny płasko w "pins" (z polem "connectorName") LUB pogrupowane w tablicy "connectors":
{
  "name": "Pełna nazwa podzespołu w języku polskim (np. UAEFI Standalone ECU lub Wtryskiwacze Bosch EV14 630cc)",
  "partNumber": "Numer katalogowy / OE (lub nazwa modelu)",
  "category": "Jeden z: ECU, MODULES, INJECTORS, MAP_SENSOR, TEMP_SENSOR, THROTTLE, COILS, FUEL_PRESSURE, OIL_PRESSURE, LAMBDA, OTHER",
  "shortDescription": "Zwięzły i dokładny opis podzespołu w języku polskim",
  "flowRateCc": null,
  "flowPressureBar": null,
  "deadTimeTables": [],
  "calibrationPoints": [],
  "calInputUnit": "kPa",
  "calOutputUnit": "V",
  "charTitle": "Charakterystyka kalibracyjna",
  "includeResistanceChar": false,
  "resInputUnit": "°C",
  "resOutputUnit": "Ω",
  "resCharTitle": "Charakterystyka opornościowa NTC",
  "resistancePoints": [],
  "pullupRequired": false,
  "pullupValue": "",
  "innerDiameterMm": null,
  "outerDiameterMm": null,
  "boltSpacingWidthMm": null,
  "boltSpacingHeightMm": null,
  "boltDiagonal1Mm": null,
  "boltDiagonal2Mm": null,
  "pins": [
    {
      "pinNumber": "1",
      "connectorName": "Wtyczka A",
      "name": "Masa główna Power GND",
      "description": "Masa zasilająca sterownik",
      "wireColor": "Czarny",
      "type": "GROUND"
    },
    {
      "pinNumber": "1",
      "connectorName": "Wtyczka B",
      "name": "Wtryskiwacz 1 (Inj 1)",
      "description": "Wyjście wtryskiwacza cylindra 1 Low-Side",
      "wireColor": "Niebieski",
      "type": "INJECTOR"
    },
    {
      "pinNumber": "1",
      "connectorName": "Wtyczka C",
      "name": "+5V VREF",
      "description": "Zasilanie referencyjne czujników TPS, MAP",
      "wireColor": "Czerwony",
      "type": "VREF"
    },
    {
      "pinNumber": "1",
      "connectorName": "Wtyczka D",
      "name": "DBW Motor +",
      "description": "Sterowanie silnikiem przepustnicy",
      "wireColor": "Żółty",
      "type": "MOTOR"
    },
    {
      "pinNumber": "1",
      "connectorName": "Wtyczka E",
      "name": "LSU 4.9 IP",
      "description": "Prąd pompy sondy szerokopasmowej",
      "wireColor": "Czerwony",
      "type": "SIGNAL_IN"
    }
  ],
  "connectors": [
    {
      "name": "Wtyczka A",
      "pins": [ ... ]
    },
    {
      "name": "Wtyczka B",
      "pins": [ ... ]
    },
    {
      "name": "Wtyczka C",
      "pins": [ ... ]
    },
    {
      "name": "Wtyczka D",
      "pins": [ ... ]
    },
    {
      "name": "Wtyczka E",
      "pins": [ ... ]
    }
  ],
  "importantNotes": "Ważne uwagi instalacyjne, zalecenia tunera w języku polskim",
  "summaryBullets": [
    "Rozpoznano urządzenie i wszystkie złącza",
    "Odczytano pinout ze wszystkich wtyczek"
  ]
}

Zwróć wyłącznie poprawny obiekt JSON, bez znaczników markdown ```json ani zbędnego tekstu.
""".trimIndent()
  }

  private fun buildGeminiRequestWithInlineData(prompt: String, mimeType: String, base64Data: String): String {
    val root = JSONObject()
    val contents = JSONArray()
    val contentObj = JSONObject()
    val parts = JSONArray()

    val inlineDataPart = JSONObject().put("inlineData", JSONObject().apply {
      put("mimeType", mimeType)
      put("data", base64Data.trim())
    })
    val textPart = JSONObject().put("text", prompt)

    parts.put(inlineDataPart)
    parts.put(textPart)
    contentObj.put("parts", parts)
    contents.put(contentObj)
    root.put("contents", contents)

    val genConfig = JSONObject().apply {
      put("responseMimeType", "application/json")
      put("temperature", 0.0)
      put("maxOutputTokens", 16384)
    }
    root.put("generationConfig", genConfig)

    return root.toString()
  }

  private fun buildGeminiTextOnlyRequest(prompt: String): String {
    val root = JSONObject()
    val contents = JSONArray()
    val contentObj = JSONObject()
    val parts = JSONArray()

    val textPart = JSONObject().put("text", prompt)
    parts.put(textPart)
    contentObj.put("parts", parts)
    contents.put(contentObj)
    root.put("contents", contents)

    val genConfig = JSONObject().apply {
      put("responseMimeType", "application/json")
      put("temperature", 0.0)
      put("maxOutputTokens", 16384)
    }
    root.put("generationConfig", genConfig)

    return root.toString()
  }

  /**
   * Szybki test połączenia z Google Gemini API (używany w oknie dialogowym).
   */
  /**
   * Szybki test połączenia z Google Gemini API (używany w oknie dialogowym).
   */
  suspend fun testApiKey(context: Context, candidateKey: String? = null): Result<String> = withContext(Dispatchers.IO) {
    val sanitizedCandidate = sanitizeApiKey(candidateKey)
    val key = if (sanitizedCandidate.isNotBlank()) sanitizedCandidate else sanitizeApiKey(getApiKey(context))

    if (key.isBlank() || key == "MY_GEMINI_API_KEY" || key == "null") {
      return@withContext Result.failure(IllegalStateException("Brak skonfigurowanego klucza API w aplikacji ani w panelu Secrets."))
    }

    // Obsługiwane nowoczesne i szybkie modele z gemini-api skill (flash-lite priorytetowo)
    val models = listOf("gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-flash-latest")
    val pingJson = JSONObject().apply {
      put("contents", JSONArray().put(JSONObject().apply {
        put("parts", JSONArray().put(JSONObject().apply {
          put("text", "Odpowiedz słowem: OK")
        }))
      }))
    }.toString()

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = pingJson.toRequestBody(mediaType)

    var lastErr = ""
    for (model in models) {
      val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
      val request = Request.Builder()
        .url(url)
        .header("x-goog-api-key", key)
        .post(body)
        .build()

      try {
        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
          return@withContext Result.success("Sukces! Połączenie z Google Gemini AI jest aktywne i w 100% sprawne (model: $model).")
        } else {
          val errBody = response.body?.string() ?: ""
          val message = try {
            JSONObject(errBody).optJSONObject("error")?.optString("message") ?: errBody
          } catch (_: Exception) {
            errBody
          }

          if (response.code == 400 && (message.contains("API key", ignoreCase = true) || message.contains("API_KEY_INVALID", ignoreCase = true) || message.contains("INVALID_ARGUMENT", ignoreCase = true))) {
            return@withContext Result.failure(IllegalStateException(
              "Google odrzuciło ten klucz (kod 400 - Nieprawidłowy klucz API):\n" +
              "• Klucz musi pochodzić z Google AI Studio (aistudio.google.com/apikey).\n" +
              "• Upewnij się, że usługa 'Generative Language API' jest aktywna dla konta Google.\n" +
              "• Domyślnie aplikacja korzysta z wbudowanego połączenia Google AI dla zalogowanych użytkowników."
            ))
          }
          if (response.code == 403) {
            return@withContext Result.failure(IllegalStateException(
              "Google odmówiło dostępu (kod 403: Permission Denied):\n" +
              "Ten klucz nie ma uprawnień do Generative Language API lub projekt ma nałożone ograniczenia."
            ))
          }
          if (response.code == 503) {
            lastErr = "Serwery Gemini są chwilowo mocno obciążone (kod 503)."
            continue
          }
          lastErr = "Błąd ($model, kod ${response.code}): $message"
        }
      } catch (e: Exception) {
        lastErr = e.message ?: "Błąd połączenia z internetem."
      }
    }

    Result.failure(IllegalStateException(lastErr))
  }

  private fun callGeminiApi(
    context: Context?,
    primaryApiKey: String,
    requestJson: String
  ): Result<ExtractedComponentData> {
    val buildKey = sanitizeApiKey(getBuildConfigKey())
    val customKey = sanitizeApiKey(if (context != null) getCustomKeyOnly(context) else "")
    val primary = sanitizeApiKey(primaryApiKey)

    val keysToTry = mutableListOf<String>()
    fun addKeyIfValid(k: String) {
      val s = sanitizeApiKey(k)
      if (isValidKey(s) && !keysToTry.contains(s)) {
        keysToTry.add(s)
      }
    }

    // Kolejność prób: najpierw klucz z Secrets (BuildConfig), potem podany klucz, potem klucz zapisany w telefonie
    if (isValidKey(buildKey)) addKeyIfValid(buildKey)
    if (isValidKey(primary)) addKeyIfValid(primary)
    if (isValidKey(customKey)) addKeyIfValid(customKey)

    if (keysToTry.isEmpty()) {
      return Result.failure(IllegalStateException("Brak aktywnego klucza Gemini API. Wprowadź darmowy klucz Gemini w ikonie klucza lub skonfiguruj GEMINI_API_KEY w panelu Secrets w AI Studio."))
    }

    // Najszybszy i najbardziej stabilny model gemini-3.5-flash-lite jako pierwszy
    val modelsToTry = listOf(
      "gemini-3.5-flash-lite",
      "gemini-3.5-flash",
      "gemini-flash-latest"
    )
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = requestJson.toRequestBody(mediaType)
    var lastError = "Nieznany błąd zapytania AI"
    var apiKeyWasRejected = false

    for (currentKey in keysToTry) {
      for (model in modelsToTry) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$currentKey"
        val request = Request.Builder()
          .url(url)
          .header("x-goog-api-key", currentKey)
          .post(requestBody)
          .build()

        try {
          val response = httpClient.newCall(request).execute()
          if (response.isSuccessful) {
            val resString = response.body?.string() ?: continue
            val parsed = parseGeminiResponse(resString)
            if (parsed.isSuccess) {
              if (context != null && currentKey != customKey && customKey.isNotBlank()) {
                // Skoro sprawdzony klucz zadziałał, a wprowadzony klucz custom był problematyczny, wyczyść wadliwy klucz
                clearCustomApiKey(context)
              }
              return parsed
            }
          } else {
            val errBody = response.body?.string() ?: ""
            val parsedError = try {
              JSONObject(errBody).optJSONObject("error")?.optString("message") ?: errBody
            } catch (_: Exception) {
              errBody
            }

            // Sprawdź czy to błąd klucza API
            if (response.code == 400 && (parsedError.contains("API key", ignoreCase = true) || parsedError.contains("API_KEY_INVALID", ignoreCase = true))) {
              Log.w(TAG, "Klucz zwrócił błąd 400 API_KEY_INVALID dla $model: $parsedError")
              apiKeyWasRejected = true
              lastError = "Klucz API nie został zaakceptowany przez Google (kod 400): $parsedError"
              break // przejdź do kolejnego klucza w keysToTry
            }

            if (response.code == 403) {
              Log.w(TAG, "Klucz zwrócił błąd 403 dla $model: $parsedError")
              lastError = "Klucz API nie posiada uprawnień do Generative Language API (kod 403)."
              break // przejdź do kolejnego klucza
            }

            lastError = "Błąd ($model, kod ${response.code}): $parsedError"
            // Przy 503, 429, 404 lub innych chwilowych błędach modelu spróbuj następnego modelu z listy
            continue
          }
        } catch (e: Exception) {
          lastError = e.message ?: "Błąd połączenia sieciowego"
          Log.w(TAG, "Wywołanie modelu $model nie powiodło się: $e")
        }
      }
    }

    val finalMessage = if (apiKeyWasRejected && keysToTry.size == 1) {
      "Klucz Gemini API jest nieprawidłowy lub odrzucony przez Google (kod 400). Przywrócono domyślne połączenie Google AI."
    } else {
      lastError
    }

    return Result.failure(IllegalStateException(finalMessage))
  }

  private fun parseGeminiResponse(jsonResponseString: String): Result<ExtractedComponentData> {
    return try {
      val root = JSONObject(jsonResponseString)
      val candidates = root.optJSONArray("candidates")
      if (candidates == null || candidates.length() == 0) {
        return Result.failure(IllegalStateException("Model Gemini nie zwrócił żadnej odpowiedzi."))
      }

      val content = candidates.getJSONObject(0).optJSONObject("content")
      val parts = content?.optJSONArray("parts")
      if (parts == null || parts.length() == 0) {
        return Result.failure(IllegalStateException("Brak treści w odpowiedzi modelu."))
      }

      val rawText = parts.getJSONObject(0).optString("text", "").trim()
      val cleanJson = rawText
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

      val json = JSONObject(cleanJson)
      val data = parseExtractedData(json)
      Result.success(data)
    } catch (e: Exception) {
      Log.e(TAG, "parseGeminiResponse failed", e)
      Result.failure(IllegalArgumentException("Nie udało się sparsować odpowiedzi JSON: ${e.message}"))
    }
  }

  private fun parseExtractedData(json: JSONObject): ExtractedComponentData {
    val categoryStr = json.optString("category", "")
    val category = try {
      ComponentCategory.valueOf(categoryStr.uppercase())
    } catch (_: Exception) {
      null
    }

    // Dead Time Tables - obsługa wielu ciśnień (np. 2.5b, 3.0b, ..., 6.0b)
    val deadTimeTablesList = mutableListOf<DeadTimeTableDraft>()
    val dtArr = json.optJSONArray("deadTimeTables") 
      ?: json.optJSONArray("deadTimes") 
      ?: json.optJSONArray("tables")
      ?: json.optJSONArray("dead_time_tables")

    if (dtArr != null) {
      for (i in 0 until dtArr.length()) {
        val tObj = dtArr.optJSONObject(i) ?: continue
        var tableName = tObj.optString("tableName", "").ifBlank {
          tObj.optString("name", "").ifBlank {
            tObj.optString("pressure", "").ifBlank {
              tObj.optString("pressureBar", "")
            }
          }
        }
        if (tableName.isNotBlank() && tableName.toDoubleOrNull() != null) {
          tableName = "$tableName bar"
        }
        if (tableName.isBlank()) {
          tableName = "Tabela ${i + 1}"
        }

        val notes = tObj.optString("notes", tObj.optString("description", ""))
        val ptsList = mutableListOf<DeadTimePointDraft>()
        val ptsArr = tObj.optJSONArray("points") 
          ?: tObj.optJSONArray("data") 
          ?: tObj.optJSONArray("values")

        if (ptsArr != null) {
          for (j in 0 until ptsArr.length()) {
            val pObj = ptsArr.optJSONObject(j) ?: continue
            val v = pObj.optString("voltage", pObj.optString("v", pObj.optString("volt", ""))).trim()
            val ms = pObj.optString("timeMs", pObj.optString("ms", pObj.optString("time", pObj.optString("deadTime", "")))).trim()
            if (v.isNotBlank() && ms.isNotBlank()) {
              ptsList.add(DeadTimePointDraft(voltage = v, timeMs = ms))
            }
          }
        }
        if (ptsList.isNotEmpty()) {
          deadTimeTablesList.add(
            DeadTimeTableDraft(
              tableName = tableName,
              notes = notes,
              points = androidx.compose.runtime.mutableStateListOf<DeadTimePointDraft>().apply { addAll(ptsList) }
            )
          )
        }
      }
    }

    // Wypunktowane podsumowanie
    val bulletsList = mutableListOf<String>()
    val bulletsArr = json.optJSONArray("summaryBullets")
    if (bulletsArr != null) {
      for (i in 0 until bulletsArr.length()) {
        val b = bulletsArr.optString(i, "")
        if (b.isNotBlank()) bulletsList.add(b)
      }
    }
    if (deadTimeTablesList.size > 1 && !bulletsList.any { it.contains("Dead Time", ignoreCase = true) }) {
      bulletsList.add("Wyodrębniono ${deadTimeTablesList.size} tabel Dead Time dla różnych ciśnień")
    }

    // Calibration points
    val calPointsList = mutableListOf<CalPointDraft>()
    val calArr = json.optJSONArray("calibrationPoints")
    val calInputUnit = json.optString("calInputUnit", "kPa").ifBlank { "kPa" }
    val calOutputUnit = json.optString("calOutputUnit", "V").ifBlank { "V" }
    if (calArr != null) {
      for (i in 0 until calArr.length()) {
        val pObj = calArr.optJSONObject(i) ?: continue
        val inVal = pObj.optString("inputValue", "")
        val inUnit = pObj.optString("inputUnit", calInputUnit).ifBlank { calInputUnit }
        val outVal = pObj.optString("outputValue", "")
        val outUnit = pObj.optString("outputUnit", calOutputUnit).ifBlank { calOutputUnit }
        if (inVal.isNotBlank() && outVal.isNotBlank()) {
          calPointsList.add(CalPointDraft(inVal = inVal, inUnit = inUnit, outVal = outVal, outUnit = outUnit))
        }
      }
    }

    // Resistance points
    val resPointsList = mutableListOf<CalPointDraft>()
    val resArr = json.optJSONArray("resistancePoints")
    val resInputUnit = json.optString("resInputUnit", "°C").ifBlank { "°C" }
    val resOutputUnit = json.optString("resOutputUnit", "Ω").ifBlank { "Ω" }
    if (resArr != null) {
      for (i in 0 until resArr.length()) {
        val pObj = resArr.optJSONObject(i) ?: continue
        val inVal = pObj.optString("inputValue", "")
        val inUnit = pObj.optString("inputUnit", resInputUnit).ifBlank { resInputUnit }
        val outVal = pObj.optString("outputValue", "")
        val outUnit = pObj.optString("outputUnit", resOutputUnit).ifBlank { resOutputUnit }
        if (inVal.isNotBlank() && outVal.isNotBlank()) {
          resPointsList.add(CalPointDraft(inVal = inVal, inUnit = inUnit, outVal = outVal, outUnit = outUnit))
        }
      }
    }

    // Pins and Multi-Connectors parsing
    val pinsList = mutableListOf<PinDraft>()

    fun parsePinObject(pObj: JSONObject, defaultConnector: String): PinDraft {
      val rawNum = pObj.optString("pinNumber", "").replace("Pin", "", ignoreCase = true).trim()
      val pName = pObj.optString("name", "").trim()
      val pDesc = pObj.optString("description", "").trim()
      val pColor = pObj.optString("wireColor", "").trim()
      val typeStr = pObj.optString("type", "OTHER").uppercase().trim()
      val pinType = when (typeStr) {
        "CAN", "CAN_BUS", "CANBUS", "SERIAL", "COMM", "COMMUNICATION" -> PinType.COMMUNICATION
        "TRIGGER", "CRANK", "CAM", "VR", "HALL" -> PinType.SIGNAL_IN
        else -> try {
          PinType.valueOf(typeStr)
        } catch (_: Exception) {
          PinType.OTHER
        }
      }

      var connName = pObj.optString("connectorName", pObj.optString("connector", defaultConnector)).trim()
      var cleanPinNum = rawNum

      val letterPrefixMatch = Regex("""^([A-Ea-e])[-_]?(\d+)$""").find(rawNum)
      if (letterPrefixMatch != null) {
        val letter = letterPrefixMatch.groupValues[1].uppercase()
        cleanPinNum = letterPrefixMatch.groupValues[2]
        if (connName.isBlank() || connName == "Wtyczka A" || connName == "Wtyczka 1") {
          connName = "Wtyczka $letter"
        }
      } else if (connName.isBlank() || connName == "Wtyczka A") {
        val textSearch = "$rawNum $pName $pDesc"
        val namedMatch = Regex("""(?:wtyczka|connector|złącze|header|plug)\s*([A-Za-z0-9\-_]+)""", RegexOption.IGNORE_CASE).find(textSearch)
        if (namedMatch != null) {
          connName = "Wtyczka ${namedMatch.groupValues[1].uppercase()}"
        }
      }

      if (cleanPinNum.isBlank()) {
        cleanPinNum = "${pinsList.size + 1}"
      }

      return PinDraft(
        pinNumber = cleanPinNum,
        name = pName,
        description = pDesc,
        wireColor = pColor,
        type = pinType,
        connectorName = connName.ifBlank { "Wtyczka A" }
      )
    }

    // 1. Direct pins array
    val directPinsArr = json.optJSONArray("pins")
    if (directPinsArr != null) {
      for (i in 0 until directPinsArr.length()) {
        val pObj = directPinsArr.optJSONObject(i) ?: continue
        pinsList.add(parsePinObject(pObj, "Wtyczka A"))
      }
    }

    // 2. Connectors array: "connectors": [ { "name": "Wtyczka A", "pins": [...] }, ... ]
    val connArr = json.optJSONArray("connectors") ?: json.optJSONArray("wtyczki") ?: json.optJSONArray("plugs")
    if (connArr != null) {
      for (c in 0 until connArr.length()) {
        val cObj = connArr.optJSONObject(c) ?: continue
        val cName = cObj.optString("name", cObj.optString("connectorName", "Wtyczka ${('A'.code + c).toChar()}")).trim()
        val cPins = cObj.optJSONArray("pins")
        if (cPins != null) {
          for (p in 0 until cPins.length()) {
            val pObj = cPins.optJSONObject(p) ?: continue
            pinsList.add(parsePinObject(pObj, cName))
          }
        }
      }
    }

    // 3. Connectors object: "connectors": { "Wtyczka A": [...], "Wtyczka B": [...] }
    val connObj = json.optJSONObject("connectors") ?: json.optJSONObject("wtyczki")
    if (connObj != null) {
      val keys = connObj.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val arr = connObj.optJSONArray(key)
        if (arr != null) {
          for (p in 0 until arr.length()) {
            val pObj = arr.optJSONObject(p) ?: continue
            pinsList.add(parsePinObject(pObj, key))
          }
        }
      }
    }

    // Aktualizacja podsumowania o odczytane wtyczki
    val distinctConnectors = pinsList.map { it.connectorName.ifBlank { "Wtyczka A" } }.distinct()
    if (distinctConnectors.size > 1 && !bulletsList.any { it.contains("wtycz", ignoreCase = true) }) {
      bulletsList.add("✔ Odczytano pinout ze wszystkich ${distinctConnectors.size} wtyczek (${distinctConnectors.joinToString()}) — łącznie ${pinsList.size} pinów")
    }

    val isThrottle = category == ComponentCategory.THROTTLE
    val isInjectors = category == ComponentCategory.INJECTORS
    val isCoils = category == ComponentCategory.COILS
    val isTemp = category == ComponentCategory.TEMP_SENSOR
    val isMap = category == ComponentCategory.MAP_SENSOR
    val isPress = category == ComponentCategory.FUEL_PRESSURE || category == ComponentCategory.OIL_PRESSURE
    val isLambda = category == ComponentCategory.LAMBDA
    val isEcuOrMod = category == ComponentCategory.ECU || category == ComponentCategory.MODULES || distinctConnectors.size >= 2

    val resolvedCategory = if (category == null && distinctConnectors.size >= 2) ComponentCategory.ECU else category

    return ExtractedComponentData(
      name = json.optString("name").takeIf { it.isNotBlank() },
      partNumber = json.optString("partNumber").takeIf { it.isNotBlank() },
      category = resolvedCategory,
      shortDescription = json.optString("shortDescription").takeIf { it.isNotBlank() },
      flowRateCc = if (isInjectors) json.optDouble("flowRateCc").takeIf { !it.isNaN() && it > 0 } else null,
      flowPressureBar = if (isInjectors) json.optDouble("flowPressureBar").takeIf { !it.isNaN() && it > 0 } else null,
      deadTimeTables = if (isInjectors) deadTimeTablesList else emptyList(),
      calibrationPoints = if (isCoils || isMap || isPress || category == ComponentCategory.OTHER) calPointsList else emptyList(),
      calInputUnit = calInputUnit,
      calOutputUnit = calOutputUnit,
      charTitle = json.optString("charTitle").takeIf { it.isNotBlank() },
      includeResistanceChar = if (isTemp) true else if (isThrottle || isInjectors || isCoils || isLambda || isEcuOrMod) false else json.optBoolean("includeResistanceChar", resPointsList.isNotEmpty()),
      resInputUnit = resInputUnit,
      resOutputUnit = resOutputUnit,
      resCharTitle = json.optString("resCharTitle").takeIf { it.isNotBlank() },
      resistancePoints = if (isThrottle || isInjectors || isCoils || isLambda || isEcuOrMod) emptyList() else resPointsList,
      pullupRequired = if (isTemp) true else if (isThrottle || isInjectors || isCoils || isLambda || isEcuOrMod) false else json.optBoolean("pullupRequired", false),
      pullupRecommendedValue = if (isThrottle || isInjectors || isCoils || isLambda || isEcuOrMod) null else json.optString("pullupValue").takeIf { it.isNotBlank() },
      innerDiameterMm = if (isThrottle) json.optDouble("innerDiameterMm").takeIf { !it.isNaN() && it > 0 } else null,
      outerDiameterMm = if (isThrottle) json.optDouble("outerDiameterMm").takeIf { !it.isNaN() && it > 0 } else null,
      boltSpacingWidthMm = if (isThrottle) json.optDouble("boltSpacingWidthMm").takeIf { !it.isNaN() && it > 0 } else null,
      boltSpacingHeightMm = if (isThrottle) json.optDouble("boltSpacingHeightMm").takeIf { !it.isNaN() && it > 0 } else null,
      boltDiagonal1Mm = if (isThrottle) json.optDouble("boltDiagonal1Mm").takeIf { !it.isNaN() && it > 0 } else null,
      boltDiagonal2Mm = if (isThrottle) json.optDouble("boltDiagonal2Mm").takeIf { !it.isNaN() && it > 0 } else null,
      pins = pinsList,
      importantNotes = json.optString("importantNotes").takeIf { it.isNotBlank() },
      summaryBullets = bulletsList
    )
  }

  private fun compressImageToBase64(context: Context, uri: Uri): String? {
    return try {
      val resolver = context.contentResolver
      val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

      // Odczytaj orientację EXIF zdjęcia (aparat w pionie/poziomie)
      var rotationDegrees = 0
      try {
        val exifInterface = ExifInterface(ByteArrayInputStream(bytes))
        val orientation = exifInterface.getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL
        )
        rotationDegrees = when (orientation) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90
          ExifInterface.ORIENTATION_ROTATE_180 -> 180
          ExifInterface.ORIENTATION_ROTATE_270 -> 270
          else -> 0
        }
      } catch (_: Exception) { }

      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

      // Optymalny wymiar dla OCR w Gemini Vision (do 2048px, ultra czytelne, szybki upload bez błędów pamięci)
      val maxDim = 2048
      var sampleSize = 1
      while (bounds.outWidth / (sampleSize * 2) >= maxDim || bounds.outHeight / (sampleSize * 2) >= maxDim) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

      if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
          bitmap.recycle()
          bitmap = rotated
        }
      }

      val baos = ByteArrayOutputStream()
      // Zawsze kompresujemy do czystego JPEG (jakość 90%), co gwarantuje 100% zgodności z mimeType "image/jpeg"
      bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
      val compressedBytes = baos.toByteArray()
      bitmap.recycle()
      Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
    } catch (e: Exception) {
      Log.e(TAG, "compressImageToBase64 error", e)
      null
    }
  }

  internal fun extractRichContentFromHtml(html: String): String {
    // 1. Sprawdź, czy w tagach <script> znajduje się bezpośredni obiekt JSON z pinoutem (np. strony rusEFI / Hellen / interactive pinout)
    val scriptMatches = Regex("""<script[^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE).findAll(html)
    val embeddedJsonSnippets = mutableListOf<String>()
    for (match in scriptMatches) {
      val code = match.groupValues[1]
      if (code.contains("pins", ignoreCase = true) || code.contains("pinout", ignoreCase = true) || (code.contains("pin", ignoreCase = true) && (code.contains("name", ignoreCase = true) || code.contains("type", ignoreCase = true)))) {
        // Wyodrębnij bloki zawierające dane pinoutu (do 40 000 znaków)
        val candidate = code.take(40000)
        embeddedJsonSnippets.add(candidate)
      }
    }

    // 2. Zamień tabele HTML i nagłówki na czytelną strukturę wierszy z podziałem na wtyczki
    var formatted = html
      .replace(Regex("<tr[^>]*>", RegexOption.IGNORE_CASE), "\n")
      .replace(Regex("</t[dh]>", RegexOption.IGNORE_CASE), " | ")
      .replace(Regex("<h[1-6][^>]*>", RegexOption.IGNORE_CASE), "\n\n### ")
      .replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), "\n")
      .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
      .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
      .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
      .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
      .replace(Regex("<[^>]+>"), " ")
      .replace(Regex("[ \t]+"), " ")
      .replace(Regex("\n{3,}"), "\n\n")
      .trim()

    if (embeddedJsonSnippets.isNotEmpty()) {
      formatted += "\n\n--- SUROWE DANE PINOUTU Z WYKRYTYCH SKRYPTÓW STRONY ---\n" + embeddedJsonSnippets.joinToString("\n---\n")
    }

    return formatted
  }

  private fun stripHtml(html: String): String {
    return extractRichContentFromHtml(html)
  }
}
