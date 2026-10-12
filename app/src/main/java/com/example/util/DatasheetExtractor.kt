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
   * Retrieves active Gemini API key from BuildConfig or SharedPreferences.
   */
  fun getApiKey(context: Context): String {
    // 1. Sprawdź najpierw klucz wbudowany z panelu Secrets w AI Studio
    val buildKey = try {
      BuildConfig.GEMINI_API_KEY.trim()
    } catch (_: Exception) {
      ""
    }
    if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
      return buildKey
    }

    // 2. Jeśli brak, sprawdź czy użytkownik zapisał własny klucz w SharedPreferences
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val customKey = prefs.getString(KEY_CUSTOM_API_KEY, "")?.trim() ?: ""
    if (customKey.isNotBlank()) return customKey

    return ""
  }

  /**
   * Sprawdza, czy klucz API pochodzi bezpośrednio z panelu Secrets projektu w AI Studio.
   */
  fun isKeyConfiguredInProject(): Boolean {
    val buildKey = try {
      BuildConfig.GEMINI_API_KEY.trim()
    } catch (_: Exception) {
      ""
    }
    return buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY"
  }

  /**
   * Saves a user-provided custom Gemini API key.
   */
  fun saveApiKey(context: Context, key: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
  }

  /**
   * Checks if an API key is available (either from Secrets or entered manually).
   */
  fun hasApiKey(context: Context): Boolean {
    return getApiKey(context).isNotBlank()
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

        callGeminiApi(apiKey, requestJson)
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
          return@withContext callGeminiApi(apiKey, requestJson)
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

        callGeminiApi(apiKey, requestJson)
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
          return@withContext callGeminiApi(apiKey, requestJson)
        }

        if (contentType.contains("image/") || trimmedUrl.endsWith(".jpg", ignoreCase = true) || trimmedUrl.endsWith(".png", ignoreCase = true)) {
          val imgBytes = body.bytes()
          val base64Data = Base64.encodeToString(imgBytes, Base64.NO_WRAP)
          val requestJson = buildGeminiRequestWithInlineData(
            prompt = "$prompt\n\nLink źródłowy do zdjęcia: $trimmedUrl",
            mimeType = if (contentType.contains("png")) "image/png" else "image/jpeg",
            base64Data = base64Data
          )
          return@withContext callGeminiApi(apiKey, requestJson)
        }

        // HTML or text webpage
        val textBody = body.string()
        // Strip excessive HTML scripts and styles to save tokens
        val cleanText = stripHtml(textBody).take(45000)

        val fullPrompt = "$prompt\n\n--- ZAWARTOŚĆ STRONY Z LINKU ($trimmedUrl) ---\n$cleanText"
        val requestJson = buildGeminiTextOnlyRequest(fullPrompt)
        callGeminiApi(apiKey, requestJson)
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

    for (line in lines) {
      // Wykrywanie definicji pinów, np. "Pin 1: GND - Masa", "1 - Sygnał MAP"
      val pinMatch = Regex("""^(?:Pin\s*)?(\d+)[\s:\-–—]+([^\-,:;]+)(?:[\-–—:;](.+))?""", RegexOption.IGNORE_CASE).find(line)
      if (pinMatch != null && !line.matches(Regex("""^[\d.,\s\t;|\-]+$"""))) {
        val pinNum = pinMatch.groupValues[1]
        val pinName = pinMatch.groupValues[2].trim()
        val pinDesc = pinMatch.groupValues.getOrNull(3)?.trim() ?: ""
        val pinType = when {
          pinName.contains("gnd", ignoreCase = true) || pinName.contains("masa", ignoreCase = true) ->
            if (pinName.contains("sensor", ignoreCase = true)) PinType.SENSOR_GND else PinType.GROUND
          pinName.contains("5v", ignoreCase = true) || pinName.contains("vref", ignoreCase = true) -> PinType.VREF
          pinName.contains("12v", ignoreCase = true) || pinName.contains("power", ignoreCase = true) || pinName.contains("zasil", ignoreCase = true) -> PinType.POWER
          pinName.contains("tps", ignoreCase = true) -> PinType.TPS
          pinName.contains("inj", ignoreCase = true) || pinName.contains("wtrysk", ignoreCase = true) -> PinType.INJECTOR
          pinName.contains("motor", ignoreCase = true) || pinName.contains("silnik", ignoreCase = true) -> PinType.MOTOR
          pinName.contains("out", ignoreCase = true) || pinName.contains("sygnał wy", ignoreCase = true) -> PinType.SIGNAL_OUT
          pinName.contains("in", ignoreCase = true) || pinName.contains("sygnał we", ignoreCase = true) -> PinType.SIGNAL_IN
          else -> PinType.OTHER
        }
        pinsList.add(
          PinDraft(
            pinNumber = pinNum,
            name = pinName,
            description = pinDesc,
            type = pinType,
            connectorName = "Wtyczka A"
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

    val inferredCategory = when {
      dtPoints.isNotEmpty() -> ComponentCategory.INJECTORS
      resPoints.isNotEmpty() -> ComponentCategory.TEMP_SENSOR
      calPoints.isNotEmpty() -> ComponentCategory.MAP_SENSOR
      pinsList.size >= 4 -> ComponentCategory.THROTTLE
      else -> null
    }

    val baseName = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
    val bullets = mutableListOf<String>()
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
Jesteś ekspertem tuningu motorsportu, elektroniki samochodowej i systemów Standalone ECU (EMU Black, Ecumaster, TunerStudio, Megasquirt, MaxxECU, Haltech, Link ECU).
Twoim zadaniem jest dokładny odczyt i ekstrakcja danych z załączonej karty katalogowej (datasheet), zdjęcia tabeli, schematu lub dokumentu technicznego podzespołu silnikowego.

Zwróć wynik WYŁĄCZNIE jako poprawny obiekt JSON o następującej strukturze:
{
  "name": "Pełna nazwa podzespołu (np. Bosch EV14 630cc, MAP Sensor 3 Bar)",
  "partNumber": "Numer katalogowy / OE (np. 0 280 158 117)",
  "category": "Jeden z: MAP_SENSOR, INJECTORS, TEMP_SENSOR, THROTTLE, COILS, FUEL_PRESSURE, OIL_PRESSURE, LAMBDA, ECU, MODULES, OTHER",
  "shortDescription": "Zwięzłe podsumowanie zastosowania",
  "flowRateCc": 630.0, // Tylko dla wtryskiwaczy (Wydajność w cc/min). Jeśli brak, null.
  "flowPressureBar": 3.0, // Ciśnienie bazowe testu w bar (np. 3.0). Jeśli brak, null.
  "deadTimeTables": [ // Tylko dla wtryskiwaczy: Tabele Dead Time (czas martwy vs napięcie)
    {
      "tableName": "3.0 bar (Diff)",
      "notes": "Dead time przy 3.0 bar",
      "points": [
        {"voltage": "8.0", "timeMs": "2.30"},
        {"voltage": "10.0", "timeMs": "1.65"},
        {"voltage": "12.0", "timeMs": "1.25"},
        {"voltage": "14.0", "timeMs": "1.00"},
        {"voltage": "16.0", "timeMs": "0.85"}
      ]
    }
  ],
  "calibrationPoints": [ // Dla czujników MAP, ciśnienia, TPS lub ogólnych (wartość wejściowa vs napięcie wyjściowe V)
    {"inputValue": "20.0", "inputUnit": "kPa", "outputValue": "0.40", "outputUnit": "V"},
    {"inputValue": "100.0", "inputUnit": "kPa", "outputValue": "1.60", "outputUnit": "V"},
    {"inputValue": "300.0", "inputUnit": "kPa", "outputValue": "4.65", "outputUnit": "V"}
  ],
  "calInputUnit": "kPa", // np. kPa, bar
  "calOutputUnit": "V", // zazwyczaj V
  "charTitle": "Tytuł charakterystyki",
  "includeResistanceChar": false, // true dla czujników temp. NTC
  "resInputUnit": "°C",
  "resOutputUnit": "Ω",
  "resCharTitle": "Charakterystyka opornościowa NTC",
  "resistancePoints": [ // Punkty rezystancji dla termistorów NTC
    {"inputValue": "-20", "inputUnit": "°C", "outputValue": "15000", "outputUnit": "Ω"},
    {"inputValue": "20", "inputUnit": "°C", "outputValue": "2500", "outputUnit": "Ω"},
    {"inputValue": "80", "inputUnit": "°C", "outputValue": "330", "outputUnit": "Ω"}
  ],
  "pullupRequired": false, // true jeśli czujnik wymaga rezystora podciągającego (np. NTC)
  "pullupValue": "2.49 kΩ do +5V VREF",
  "innerDiameterMm": null, // Dla przepustnic: średnica wewnętrzna w mm (np. 68.0)
  "outerDiameterMm": null, // Dla przepustnic: średnica zewnętrzna w mm (np. 76.0)
  "boltSpacingWidthMm": null, // Dla przepustnic: rozstaw śrub bok 1 (szerokość) w mm (np. 60.0)
  "boltSpacingHeightMm": null, // Dla przepustnic: rozstaw śrub bok 2 (wysokość) w mm (np. 60.0)
  "boltDiagonal1Mm": null, // Dla przepustnic: przekątna 1 w mm (np. 84.8)
  "boltDiagonal2Mm": null, // Dla przepustnic: przekątna 2 w mm (np. 84.8)
  "pins": [ // Wykaz pinów złącza
    {
      "pinNumber": "1", // TYLKO numer (np. 1, 2, 3)
      "name": "Krótka funkcja pinu (np. Sensor GND, +5V VREF, Sygnał MAP)",
      "description": "Szczegółowy opis lub uwagi",
      "wireColor": "Czerwony/Brązowy/Biały/Niebieski",
      "type": "Jeden z: POWER, GROUND, SENSOR_GND, VREF, SIGNAL_OUT, SIGNAL_IN, INJECTOR, IGNITION, MOTOR, TPS, OTHER"
    }
  ],
  "importantNotes": "Ważne uwagi instalacyjne, momenty dokręcania, zalecenia dla tunera",
  "summaryBullets": [ // 2-5 punktów podsumowujących co zostało odczytane w języku polskim
    "Rozpoznano model i numer katalogowy",
    "Wyodrębniono tabelę..."
  ]
}

ZASADY:
1. Jeśli kategoria to Przepustnica (THROTTLE), nie twórz charakterystyki napięciowej ani opornościowej, ale skup się na średnicy wewnętrznej/zewnętrznej, rozstawie śrub i przekątnych oraz pinoucie DBW/TPS.
2. Jeśli kategoria to Wtryskiwacze (INJECTORS), koniecznie odczytaj wydajność (cc/min), ciśnienie oraz pełną tabelę Dead Time (wartości dla 8V, 10V, 12V, 14V, 16V).
3. Dla czujników MAP: odczytaj tabelę kPa vs V lub bar vs V.
4. Dla czujników temperatury (TEMP_SENSOR): odczytaj tabelę opornościową NTC (°C vs Ω) i zaznacz pullupRequired = true.
5. Numery pinów to ZAWSZE same cyfry ("1", "2", "3"), bez słowa "Pin".
6. Zwróć wyłącznie poprawny JSON, bez znaczników markdown ```json ani zbędnego tekstu.
""".trimIndent()
  }

  private fun buildGeminiRequestWithInlineData(prompt: String, mimeType: String, base64Data: String): String {
    val root = JSONObject()
    val contents = JSONArray()
    val contentObj = JSONObject()
    val parts = JSONArray()

    val textPart = JSONObject().put("text", prompt)
    val inlineDataPart = JSONObject().put("inlineData", JSONObject().apply {
      put("mimeType", mimeType)
      put("data", base64Data)
    })

    parts.put(textPart)
    parts.put(inlineDataPart)
    contentObj.put("parts", parts)
    contents.put(contentObj)
    root.put("contents", contents)

    val genConfig = JSONObject().apply {
      put("responseMimeType", "application/json")
      put("temperature", 0.1)
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
      put("temperature", 0.1)
    }
    root.put("generationConfig", genConfig)

    return root.toString()
  }

  private fun callGeminiApi(apiKey: String, requestJson: String): Result<ExtractedComponentData> {
    val modelsToTry = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.8-flash")
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = requestJson.toRequestBody(mediaType)
    var lastError = "Nieznany błąd zapytania AI"

    for (model in modelsToTry) {
      val urlWithKey = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
      val request = Request.Builder()
        .url(urlWithKey)
        .post(requestBody)
        .build()

      try {
        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
          val resString = response.body?.string() ?: continue
          val parsed = parseGeminiResponse(resString)
          if (parsed.isSuccess) {
            return parsed
          }
        } else {
          val errBody = response.body?.string() ?: ""
          val parsedError = try {
            JSONObject(errBody).optJSONObject("error")?.optString("message") ?: errBody
          } catch (_: Exception) {
            errBody
          }
          lastError = "Błąd ($model, kod ${response.code}): $parsedError"
          // Jeśli model jest chwilowo przeciążony (503), ma rate-limit (429) lub nieobsługiwany (404), próbujemy kolejnego
          if (response.code == 503 || response.code == 404 || response.code == 429) {
            Log.w(TAG, "Model $model zwrócił ${response.code}, próba alternatywnego modelu...")
            continue
          } else {
            return Result.failure(IllegalStateException(lastError))
          }
        }
      } catch (e: Exception) {
        lastError = e.message ?: "Błąd połączenia sieciowego"
        Log.w(TAG, "Wywołanie modelu $model nie powiodło się: $e")
      }
    }

    return Result.failure(IllegalStateException(lastError))
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

    // Dead Time Tables
    val deadTimeTablesList = mutableListOf<DeadTimeTableDraft>()
    val dtArr = json.optJSONArray("deadTimeTables")
    if (dtArr != null) {
      for (i in 0 until dtArr.length()) {
        val tObj = dtArr.optJSONObject(i) ?: continue
        val tableName = tObj.optString("tableName", "Tabela ${i + 1}")
        val notes = tObj.optString("notes", "")
        val ptsList = mutableListOf<DeadTimePointDraft>()
        val ptsArr = tObj.optJSONArray("points")
        if (ptsArr != null) {
          for (j in 0 until ptsArr.length()) {
            val pObj = ptsArr.optJSONObject(j) ?: continue
            val v = pObj.optString("voltage", "")
            val ms = pObj.optString("timeMs", "")
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

    // Pins
    val pinsList = mutableListOf<PinDraft>()
    val pinsArr = json.optJSONArray("pins")
    if (pinsArr != null) {
      for (i in 0 until pinsArr.length()) {
        val pObj = pinsArr.optJSONObject(i) ?: continue
        val rawNum = pObj.optString("pinNumber", "${i + 1}").replace("Pin", "", ignoreCase = true).trim()
        val pinNum = rawNum.ifBlank { "${i + 1}" }
        val pName = pObj.optString("name", "")
        val pDesc = pObj.optString("description", "")
        val pColor = pObj.optString("wireColor", "")
        val typeStr = pObj.optString("type", "OTHER")
        val pinType = try {
          PinType.valueOf(typeStr.uppercase())
        } catch (_: Exception) {
          PinType.OTHER
        }
        pinsList.add(
          PinDraft(
            pinNumber = pinNum,
            name = pName,
            description = pDesc,
            wireColor = pColor,
            type = pinType,
            connectorName = "Wtyczka A"
          )
        )
      }
    }

    // Summary bullets
    val bulletsList = mutableListOf<String>()
    val bulletsArr = json.optJSONArray("summaryBullets")
    if (bulletsArr != null) {
      for (i in 0 until bulletsArr.length()) {
        val b = bulletsArr.optString(i, "")
        if (b.isNotBlank()) bulletsList.add(b)
      }
    }

    return ExtractedComponentData(
      name = json.optString("name").takeIf { it.isNotBlank() },
      partNumber = json.optString("partNumber").takeIf { it.isNotBlank() },
      category = category,
      shortDescription = json.optString("shortDescription").takeIf { it.isNotBlank() },
      flowRateCc = json.optDouble("flowRateCc").takeIf { !it.isNaN() && it > 0 },
      flowPressureBar = json.optDouble("flowPressureBar").takeIf { !it.isNaN() && it > 0 },
      deadTimeTables = deadTimeTablesList,
      calibrationPoints = calPointsList,
      calInputUnit = calInputUnit,
      calOutputUnit = calOutputUnit,
      charTitle = json.optString("charTitle").takeIf { it.isNotBlank() },
      includeResistanceChar = json.optBoolean("includeResistanceChar", resPointsList.isNotEmpty()),
      resInputUnit = resInputUnit,
      resOutputUnit = resOutputUnit,
      resCharTitle = json.optString("resCharTitle").takeIf { it.isNotBlank() },
      resistancePoints = resPointsList,
      pullupRequired = json.optBoolean("pullupRequired", false),
      pullupRecommendedValue = json.optString("pullupValue").takeIf { it.isNotBlank() },
      innerDiameterMm = json.optDouble("innerDiameterMm").takeIf { !it.isNaN() && it > 0 },
      outerDiameterMm = json.optDouble("outerDiameterMm").takeIf { !it.isNaN() && it > 0 },
      boltSpacingWidthMm = json.optDouble("boltSpacingWidthMm").takeIf { !it.isNaN() && it > 0 },
      boltSpacingHeightMm = json.optDouble("boltSpacingHeightMm").takeIf { !it.isNaN() && it > 0 },
      boltDiagonal1Mm = json.optDouble("boltDiagonal1Mm").takeIf { !it.isNaN() && it > 0 },
      boltDiagonal2Mm = json.optDouble("boltDiagonal2Mm").takeIf { !it.isNaN() && it > 0 },
      pins = pinsList,
      importantNotes = json.optString("importantNotes").takeIf { it.isNotBlank() },
      summaryBullets = bulletsList
    )
  }

  private fun compressImageToBase64(context: Context, uri: Uri): String? {
    return try {
      val resolver = context.contentResolver
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

      var sampleSize = 1
      val maxDim = 1200
      while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        ?: return null

      val baos = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos)
      val bytes = baos.toByteArray()
      Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
      Log.e(TAG, "compressImageToBase64 error", e)
      null
    }
  }

  private fun stripHtml(html: String): String {
    return html
      .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
      .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
      .replace(Regex("<[^>]+>"), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
  }
}
