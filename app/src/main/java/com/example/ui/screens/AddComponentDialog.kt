package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import com.example.ui.components.resolvePinTextColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.components.ComponentThumbnail
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PhotoLibrary
import java.io.File
import com.example.ui.components.ComponentHeroImage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Straighten
import com.example.ui.components.ThrottleDimensionsCard
import com.example.ui.components.AutoFillDatasheetCard
import kotlin.math.hypot
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalibrationPoint
import com.example.model.CharType
import com.example.model.CharacteristicInfo
import com.example.model.ComponentCategory
import com.example.model.PinInfo
import com.example.model.PinType
import com.example.model.TuningComponent
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
import androidx.compose.material.icons.filled.Cable
import java.util.UUID

data class PinDraft(
  val id: String = UUID.randomUUID().toString(),
  val pinNumber: String = "",
  val name: String = "",
  val description: String = "",
  val wireColor: String = "",
  val type: PinType = PinType.OTHER,
  val connectorName: String = "Wtyczka A"
)

data class CalPointDraft(
  val id: String = UUID.randomUUID().toString(),
  val inVal: String = "",
  val inUnit: String = "",
  val outVal: String = "",
  val outUnit: String = ""
)

data class DeadTimePointDraft(
  val id: String = UUID.randomUUID().toString(),
  val voltage: String = "",
  val timeMs: String = ""
)

data class DeadTimeTableDraft(
  val id: String = UUID.randomUUID().toString(),
  val tableName: String = "3.0 bar (Diff)",
  val notes: String = "",
  val points: androidx.compose.runtime.snapshots.SnapshotStateList<DeadTimePointDraft> = androidx.compose.runtime.mutableStateListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddComponentDialog(
  userNick: String,
  existingComponent: TuningComponent? = null,
  onDismiss: () -> Unit,
  onSave: (
    name: String,
    partNumber: String,
    category: ComponentCategory,
    shortDescription: String,
    imageUrl: String,
    images: List<String>,
    pins: List<PinInfo>,
    characteristic: CharacteristicInfo,
    resistanceCharacteristic: CharacteristicInfo?,
    pullupRequired: Boolean,
    pullupValue: String,
    pullupNote: String,
    importantNotes: String,
    connectorImageUrl: String,
    deadTimeTables: List<com.example.model.DeadTimeTable>,
    flowRateCc: Double?,
    flowPressureBar: Double?,
    innerDiameterMm: Double?,
    outerDiameterMm: Double?,
    boltSpacingWidthMm: Double?,
    boltSpacingHeightMm: Double?,
    boltDiagonal1Mm: Double?,
    boltDiagonal2Mm: Double?,
    injectorImpedance: String,
    ecuOutputsCount: String,
    ecuBoardDimensions: String
  ) -> Unit,
  onDelete: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val isEditing = existingComponent != null
  val compKey = existingComponent?.id ?: "new_component"

  var name by remember(compKey) { mutableStateOf(existingComponent?.name ?: "") }
  var partNumber by remember(compKey) { mutableStateOf(existingComponent?.partNumber ?: "") }
  var category by remember(compKey) { mutableStateOf(existingComponent?.category ?: ComponentCategory.MAP_SENSOR) }
  var categoryDropdownExpanded by remember(compKey) { mutableStateOf(false) }

  var shortDescription by remember(compKey) { mutableStateOf(existingComponent?.shortDescription ?: "") }

  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  val initialImages = remember(compKey) {
    if (existingComponent != null) {
      if (existingComponent.images.isNotEmpty()) {
        existingComponent.images
      } else if (existingComponent.imageUrl.isNotBlank()) {
        listOf(existingComponent.imageUrl)
      } else {
        emptyList()
      }
    } else {
      emptyList()
    }
  }
  val imagesList = remember(compKey) { mutableStateListOf<String>().apply { addAll(initialImages) } }
  var isCompressingPhotos by remember(compKey) { mutableStateOf(false) }
  var urlInputText by remember(compKey) { mutableStateOf("") }

  val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      isCompressingPhotos = true
      coroutineScope.launch(Dispatchers.IO) {
        val convertedList = mutableListOf<String>()
        for (u in uris) {
          val b64 = ImageUtils.compressImageUriToBase64(context, u)
          if (b64 != null) convertedList.add(b64)
        }
        withContext(Dispatchers.Main) {
          for (c in convertedList) {
            if (imagesList.size < 8 && !imagesList.contains(c)) {
              imagesList.add(c)
            }
          }
          isCompressingPhotos = false
        }
      }
    }
  }

  val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      isCompressingPhotos = true
      coroutineScope.launch(Dispatchers.IO) {
        val b64 = ImageUtils.compressImageUriToBase64(context, uri)
        withContext(Dispatchers.Main) {
          if (b64 != null && imagesList.size < 8 && !imagesList.contains(b64)) {
            imagesList.add(b64)
          }
          isCompressingPhotos = false
        }
      }
    }
  }

  var connectorImageUrl by remember(compKey) { mutableStateOf(existingComponent?.connectorImageUrl ?: "") }

  val connectorPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      isCompressingPhotos = true
      coroutineScope.launch(Dispatchers.IO) {
        val b64 = ImageUtils.compressImageUriToBase64(context, uri)
        withContext(Dispatchers.Main) {
          if (b64 != null) {
            connectorImageUrl = b64
          }
          isCompressingPhotos = false
        }
      }
    }
  }

  var pullupRequired by remember(compKey) { mutableStateOf(existingComponent?.pullupRequired ?: false) }
  var pullupValue by remember(compKey) { mutableStateOf(existingComponent?.pullupRecommendedValue ?: "2.49 kΩ do +5V VREF") }
  var pullupNote by remember(compKey) { mutableStateOf(existingComponent?.pullupNote ?: "") }

  // Connectors and Draft pins
  val initialConnectors = remember(compKey) {
    if (existingComponent != null && existingComponent.pinout.isNotEmpty()) {
      existingComponent.pinout.map { it.connectorName.ifBlank { "Wtyczka A" } }.distinct()
    } else {
      listOf("Wtyczka A")
    }
  }
  val connectorsList = remember(compKey) {
    mutableStateListOf<String>().apply { addAll(initialConnectors) }
  }

  val pinsList = remember(compKey) {
    mutableStateListOf<PinDraft>().apply {
      if (existingComponent != null && existingComponent.pinout.isNotEmpty()) {
        addAll(existingComponent.pinout.map {
          PinDraft(
            pinNumber = it.pinNumber,
            name = it.name,
            description = it.description,
            wireColor = it.wireColor,
            type = it.type,
            connectorName = it.connectorName.ifBlank { "Wtyczka A" }
          )
        })
      } else {
        addAll(
          listOf(
            PinDraft(pinNumber = "1", name = "", description = "", wireColor = "", type = PinType.OTHER, connectorName = "Wtyczka A"),
            PinDraft(pinNumber = "2", name = "", description = "", wireColor = "", type = PinType.OTHER, connectorName = "Wtyczka A"),
            PinDraft(pinNumber = "3", name = "", description = "", wireColor = "", type = PinType.OTHER, connectorName = "Wtyczka A")
          )
        )
      }
    }
  }

  // Characteristics draft
  var charTitle by remember(compKey) {
    mutableStateOf(existingComponent?.characteristic?.title ?: "Charakterystyka napięciowa / kalibracja")
  }
  var charType by remember(compKey) {
    mutableStateOf(existingComponent?.characteristic?.type ?: CharType.VOLTAGE_PRESSURE)
  }
  var charInputLabel by remember(compKey) {
    mutableStateOf(
      existingComponent?.characteristic?.inputLabel?.let {
        if (it.contains("Wartość wejściowa", ignoreCase = true) || it.equals("Wejście", ignoreCase = true)) "kPa" else it
      } ?: "kPa"
    )
  }
  var charOutputLabel by remember(compKey) {
    mutableStateOf(
      existingComponent?.characteristic?.outputLabel?.let {
        if (it.contains("Wartość wyjściowa", ignoreCase = true) || it.equals("Wyjście", ignoreCase = true)) "V" else it
      } ?: "V"
    )
  }
  var charNotes by remember(compKey) {
    mutableStateOf(existingComponent?.characteristic?.formulaOrNotes ?: "")
  }

  // Globalne jednostki dla całej tabeli kalibracyjnej
  var tableInputUnit by remember(compKey) {
    mutableStateOf(
      existingComponent?.characteristic?.points?.firstOrNull()?.inputUnit?.ifBlank { null }
        ?: if (existingComponent?.characteristic?.type == CharType.RESISTANCE_TEMP) "°C" else "kPa"
    )
  }
  var tableOutputUnit by remember(compKey) {
    mutableStateOf(
      existingComponent?.characteristic?.points?.firstOrNull()?.outputUnit?.ifBlank { null }
        ?: if (existingComponent?.characteristic?.type == CharType.RESISTANCE_TEMP) "Ω" else "V"
    )
  }

  val calPointsList = remember(compKey) {
    mutableStateListOf<CalPointDraft>().apply {
      if (existingComponent != null && existingComponent.characteristic.points.isNotEmpty()) {
        addAll(existingComponent.characteristic.points.map {
          CalPointDraft(inVal = it.inputValue, inUnit = it.inputUnit, outVal = it.outputValue, outUnit = it.outputUnit)
        })
      } else {
        addAll(
          listOf(
            CalPointDraft(inVal = "20", inUnit = "kPa", outVal = "0.40", outUnit = "V"),
            CalPointDraft(inVal = "100", inUnit = "kPa", outVal = "1.80", outUnit = "V"),
            CalPointDraft(inVal = "300", inUnit = "kPa", outVal = "4.65", outUnit = "V")
          )
        )
      }
    }
  }

  // Optional resistance characteristics (charakterystyka opornościowa)
  var includeResistanceChar by remember(compKey) {
    mutableStateOf(existingComponent?.resistanceCharacteristic != null || category == ComponentCategory.TEMP_SENSOR)
  }
  var resCharTitle by remember(compKey) {
    mutableStateOf(existingComponent?.resistanceCharacteristic?.title ?: "Charakterystyka opornościowa NTC (Temp vs Rezystancja)")
  }
  var resTableInputUnit by remember(compKey) {
    mutableStateOf(
      existingComponent?.resistanceCharacteristic?.points?.firstOrNull()?.inputUnit?.ifBlank { null } ?: "°C"
    )
  }
  var resTableOutputUnit by remember(compKey) {
    mutableStateOf(
      existingComponent?.resistanceCharacteristic?.points?.firstOrNull()?.outputUnit?.ifBlank { null } ?: "Ω"
    )
  }
  val resPointsList = remember(compKey) {
    mutableStateListOf<CalPointDraft>().apply {
      if (existingComponent?.resistanceCharacteristic != null && existingComponent.resistanceCharacteristic.points.isNotEmpty()) {
        addAll(existingComponent.resistanceCharacteristic.points.map {
          CalPointDraft(inVal = it.inputValue, inUnit = it.inputUnit, outVal = it.outputValue, outUnit = it.outputUnit)
        })
      } else {
        addAll(
          listOf(
            CalPointDraft(inVal = "-20", inUnit = "°C", outVal = "15000", outUnit = "Ω"),
            CalPointDraft(inVal = "0", inUnit = "°C", outVal = "5800", outUnit = "Ω"),
            CalPointDraft(inVal = "20", inUnit = "°C", outVal = "2500", outUnit = "Ω"),
            CalPointDraft(inVal = "80", inUnit = "°C", outVal = "320", outUnit = "Ω"),
            CalPointDraft(inVal = "100", inUnit = "°C", outVal = "180", outUnit = "Ω")
          )
        )
      }
    }
  }
  var resCharNotes by remember(compKey) {
    mutableStateOf(existingComponent?.resistanceCharacteristic?.formulaOrNotes ?: "")
  }

  // Tabela / tabele Dead Time wtryskiwacza
  val deadTimeTablesDraft = remember(compKey) {
    mutableStateListOf<DeadTimeTableDraft>().apply {
      if (existingComponent != null && existingComponent.deadTimeTables.isNotEmpty()) {
        addAll(existingComponent.deadTimeTables.map { tbl ->
          val ptsList = androidx.compose.runtime.mutableStateListOf<DeadTimePointDraft>()
          ptsList.addAll(tbl.points.map { DeadTimePointDraft(voltage = it.voltage, timeMs = it.timeMs) })
          DeadTimeTableDraft(
            tableName = tbl.tableName,
            notes = tbl.notes,
            points = ptsList
          )
        })
      } else {
        // Domyślna tabela dead time dla wtryskiwacza
        val defaultPts = androidx.compose.runtime.mutableStateListOf<DeadTimePointDraft>()
        defaultPts.addAll(
          listOf(
            DeadTimePointDraft(voltage = "8.0", timeMs = "2.10"),
            DeadTimePointDraft(voltage = "10.0", timeMs = "1.52"),
            DeadTimePointDraft(voltage = "11.0", timeMs = "1.30"),
            DeadTimePointDraft(voltage = "12.0", timeMs = "1.14"),
            DeadTimePointDraft(voltage = "13.0", timeMs = "1.01"),
            DeadTimePointDraft(voltage = "14.0", timeMs = "0.91"),
            DeadTimePointDraft(voltage = "15.0", timeMs = "0.82"),
            DeadTimePointDraft(voltage = "16.0", timeMs = "0.75")
          )
        )
        add(DeadTimeTableDraft(tableName = "3.0 bar (Diff)", notes = "", points = defaultPts))
      }
    }
  }

  // Wydajność wtryskiwacza i ciśnienie bazowe
  var flowRateCcInput by remember(compKey) {
    mutableStateOf(existingComponent?.flowRateCc?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
  }
  var flowPressureBarInput by remember(compKey) {
    mutableStateOf(existingComponent?.flowPressureBar?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "3.0")
  }

  // Wymiary przepustnicy (Throttle Body)
  var innerDiameterInput by remember(compKey) {
    mutableStateOf(existingComponent?.innerDiameterMm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
  }
  var outerDiameterInput by remember(compKey) {
    mutableStateOf(existingComponent?.outerDiameterMm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
  }
  var boltSpacingWidthInput by remember(compKey) {
    mutableStateOf(existingComponent?.boltSpacingWidthMm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
  }
  var boltSpacingHeightInput by remember(compKey) {
    mutableStateOf(existingComponent?.boltSpacingHeightMm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
  }
  var boltDiagonal1Input by remember(compKey) {
    mutableStateOf(existingComponent?.boltDiagonal1Mm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
  }
  var boltDiagonal2Input by remember(compKey) {
    mutableStateOf(existingComponent?.boltDiagonal2Mm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
  }

  // Rezystancja wtryskiwacza (High-Z vs Low-Z)
  var injectorImpedance by remember(compKey) {
    mutableStateOf(existingComponent?.injectorImpedance ?: "")
  }

  // Spis wejść/wyjść sterownika ECU i opcjonalne wymiary płytki PCB
  var ecuOutputsCount by remember(compKey) {
    mutableStateOf(existingComponent?.ecuOutputsCount ?: "")
  }
  var ecuBoardDimensions by remember(compKey) {
    mutableStateOf(existingComponent?.ecuBoardDimensions ?: "")
  }

  // Flagi dla czujników zespolonych (np. T-MAP lub kombi-sensor ciśnienia i temperatury paliwa / oleju)
  var isComboTmapSensor by remember(compKey) {
    mutableStateOf(existingComponent?.category == ComponentCategory.MAP_SENSOR && existingComponent.resistanceCharacteristic != null)
  }
  var isComboFuelPressureTempSensor by remember(compKey) {
    mutableStateOf(existingComponent?.category == ComponentCategory.FUEL_PRESSURE && existingComponent.resistanceCharacteristic != null)
  }
  var isComboOilPressureTempSensor by remember(compKey) {
    mutableStateOf(existingComponent?.category == ComponentCategory.OIL_PRESSURE && existingComponent.resistanceCharacteristic != null)
  }
  var showGenericOtherCalibration by remember(compKey) {
    mutableStateOf(existingComponent?.category == ComponentCategory.OTHER && existingComponent.characteristic.points.isNotEmpty())
  }
  var showGenericOtherResistance by remember(compKey) {
    mutableStateOf(existingComponent?.category == ComponentCategory.OTHER && existingComponent.resistanceCharacteristic != null)
  }
  var showGenericOtherPullup by remember(compKey) {
    mutableStateOf(existingComponent?.category == ComponentCategory.OTHER && existingComponent.pullupRequired)
  }

  var importantNotes by remember(compKey) { mutableStateOf(existingComponent?.importantNotes ?: "") }
  var errorMessage by remember(compKey) { mutableStateOf<String?>(null) }
  var showDeleteConfirmDialog by remember(compKey) { mutableStateOf(false) }

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
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_sensor_button")) {
            Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = Color.White)
          }
        },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
              contentDescription = null,
              tint = if (isEditing) TunerAmber else TuningRed,
              modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = if (isEditing) "Edytuj wpis" else "Dodaj nowy czujnik / ECU",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        },
        actions = {
          if (isEditing && onDelete != null) {
            IconButton(
              onClick = { showDeleteConfirmDialog = true },
              modifier = Modifier.testTag("delete_component_topbar_button")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Usuń wpis",
                tint = TuningRed
              )
            }
          }
          Button(
            onClick = {
              if (name.isBlank()) {
                errorMessage = "Wpisz nazwę czujnika lub sterownika!"
                return@Button
              }
              val finalPins = pinsList.mapIndexed { idx, p ->
                PinInfo(
                  pinNumber = p.pinNumber.trim().ifBlank { "${idx + 1}" },
                  name = p.name.trim(),
                  description = p.description.trim(),
                  wireColor = p.wireColor,
                  type = p.type,
                  connectorName = p.connectorName.ifBlank { "Wtyczka A" }
                )
              }
              // 1. Główna charakterystyka - tylko dla kategorii, które jej potrzebują
              // 1. Główna charakterystyka - tylko dla kategorii, które jej potrzebują
              val finalCharacteristic = when (category) {
                ComponentCategory.COILS -> {
                  CharacteristicInfo(
                    type = CharType.DWELL_TIME,
                    title = "Tabela Dwell Time do napięcia",
                    inputLabel = "Napięcie ($tableInputUnit)",
                    outputLabel = "Dwell Time ($tableOutputUnit)",
                    points = calPointsList.map {
                      CalibrationPoint(
                        inputValue = it.inVal.trim(),
                        inputUnit = tableInputUnit.trim(),
                        outputValue = it.outVal.trim(),
                        outputUnit = tableOutputUnit.trim()
                      )
                    },
                    formulaOrNotes = charNotes
                  )
                }
                ComponentCategory.MAP_SENSOR, ComponentCategory.FUEL_PRESSURE, ComponentCategory.OIL_PRESSURE -> {
                  CharacteristicInfo(
                    type = CharType.VOLTAGE_PRESSURE,
                    title = "Kalibracja liniowa ciśnienia (kPa vs V)",
                    inputLabel = "Ciśnienie ($tableInputUnit)",
                    outputLabel = "Napięcie ($tableOutputUnit)",
                    points = calPointsList.map {
                      CalibrationPoint(
                        inputValue = it.inVal.trim(),
                        inputUnit = tableInputUnit.trim(),
                        outputValue = it.outVal.trim(),
                        outputUnit = tableOutputUnit.trim()
                      )
                    },
                    formulaOrNotes = charNotes
                  )
                }
                ComponentCategory.TEMP_SENSOR -> {
                  CharacteristicInfo(
                    type = CharType.RESISTANCE_TEMP,
                    title = "Charakterystyka temperatury do oporności",
                    inputLabel = "Temperatura (℃)",
                    outputLabel = "Rezystancja ($tableOutputUnit)",
                    points = calPointsList.map {
                      CalibrationPoint(
                        inputValue = it.inVal.trim(),
                        inputUnit = "°C",
                        outputValue = it.outVal.trim(),
                        outputUnit = tableOutputUnit.trim()
                      )
                    },
                    formulaOrNotes = ""
                  )
                }
                ComponentCategory.MODULES -> {
                  CharacteristicInfo(
                    type = CharType.VOLTAGE_PRESSURE,
                    title = "Charakterystyka napięciowa modułu",
                    inputLabel = "Wejście ($tableInputUnit)",
                    outputLabel = "Napięcie ($tableOutputUnit)",
                    points = calPointsList.map {
                      CalibrationPoint(
                        inputValue = it.inVal.trim(),
                        inputUnit = tableInputUnit.trim(),
                        outputValue = it.outVal.trim(),
                        outputUnit = tableOutputUnit.trim()
                      )
                    },
                    formulaOrNotes = charNotes
                  )
                }
                ComponentCategory.OTHER -> {
                  if (showGenericOtherCalibration) {
                    CharacteristicInfo(
                      type = charType,
                      title = charTitle.ifBlank { "Charakterystyka ogólna" },
                      inputLabel = "Wejście ($tableInputUnit)",
                      outputLabel = "Wyjście ($tableOutputUnit)",
                      points = calPointsList.map {
                        CalibrationPoint(
                          inputValue = it.inVal.trim(),
                          inputUnit = tableInputUnit.trim(),
                          outputValue = it.outVal.trim(),
                          outputUnit = tableOutputUnit.trim()
                        )
                      },
                      formulaOrNotes = charNotes
                    )
                  } else {
                    CharacteristicInfo(
                      type = CharType.OTHER,
                      title = "",
                      inputLabel = "",
                      outputLabel = "",
                      points = emptyList(),
                      formulaOrNotes = ""
                    )
                  }
                }
                else -> {
                  CharacteristicInfo(
                    type = CharType.OTHER,
                    title = "",
                    inputLabel = "",
                    outputLabel = "",
                    points = emptyList(),
                    formulaOrNotes = ""
                  )
                }
              }

              // 2. Charakterystyka opornościowa - dla czujników z wbudowanym pomiarem temp oraz modułów
              val finalResistanceChar = when {
                category == ComponentCategory.MAP_SENSOR && isComboTmapSensor -> {
                  CharacteristicInfo(
                    type = CharType.RESISTANCE_TEMP,
                    title = "Charakterystyka opornościowa IAT (NTC)",
                    inputLabel = "Temperatura ($resTableInputUnit)",
                    outputLabel = "Opór ($resTableOutputUnit)",
                    points = resPointsList.map {
                      CalibrationPoint(
                        inputValue = it.inVal.trim(),
                        inputUnit = resTableInputUnit.trim(),
                        outputValue = it.outVal.trim(),
                        outputUnit = resTableOutputUnit.trim()
                      )
                    },
                    formulaOrNotes = ""
                  )
                }
                category == ComponentCategory.FUEL_PRESSURE && isComboFuelPressureTempSensor -> {
                  CharacteristicInfo(
                    type = CharType.RESISTANCE_TEMP,
                    title = "Charakterystyka temperatury paliwa (NTC)",
                    inputLabel = "Temperatura ($resTableInputUnit)",
                    outputLabel = "Opór ($resTableOutputUnit)",
                    points = resPointsList.map {
                      CalibrationPoint(
                        inputValue = it.inVal.trim(),
                        inputUnit = resTableInputUnit.trim(),
                        outputValue = it.outVal.trim(),
                        outputUnit = resTableOutputUnit.trim()
                      )
                    },
                    formulaOrNotes = ""
                  )
                }
                category == ComponentCategory.OIL_PRESSURE && isComboOilPressureTempSensor -> {
                  CharacteristicInfo(
                    type = CharType.RESISTANCE_TEMP,
                    title = "Charakterystyka temperatury oleju (NTC)",
                    inputLabel = "Temperatura ($resTableInputUnit)",
                    outputLabel = "Opór ($resTableOutputUnit)",
                    points = resPointsList.map {
                      CalibrationPoint(
                        inputValue = it.inVal.trim(),
                        inputUnit = resTableInputUnit.trim(),
                        outputValue = it.outVal.trim(),
                        outputUnit = resTableOutputUnit.trim()
                      )
                    },
                    formulaOrNotes = ""
                  )
                }
                category == ComponentCategory.MODULES -> {
                  CharacteristicInfo(
                    type = CharType.RESISTANCE_TEMP,
                    title = "Charakterystyka opornościowa modułu",
                    inputLabel = "Wejście ($resTableInputUnit)",
                    outputLabel = "Oporność ($resTableOutputUnit)",
                    points = resPointsList.map {
                      CalibrationPoint(
                        inputValue = it.inVal.trim(),
                        inputUnit = resTableInputUnit.trim(),
                        outputValue = it.outVal.trim(),
                        outputUnit = resTableOutputUnit.trim()
                      )
                    },
                    formulaOrNotes = ""
                  )
                }
                category == ComponentCategory.OTHER && showGenericOtherResistance -> {
                  CharacteristicInfo(
                    type = CharType.RESISTANCE_TEMP,
                    title = resCharTitle.ifBlank { "Charakterystyka opornościowa" },
                    inputLabel = "Temperatura ($resTableInputUnit)",
                    outputLabel = "Opór ($resTableOutputUnit)",
                    points = resPointsList.map {
                      CalibrationPoint(
                        inputValue = it.inVal.trim(),
                        inputUnit = resTableInputUnit.trim(),
                        outputValue = it.outVal.trim(),
                        outputUnit = resTableOutputUnit.trim()
                      )
                    },
                    formulaOrNotes = resCharNotes
                  )
                }
                else -> null
              }

              // 3. Rezystor Pull-Up: TYLKO dla czujników temp lub kombi T-MAP/kombi olej
              val finalPullupRequired = when {
                category == ComponentCategory.TEMP_SENSOR -> pullupRequired
                category == ComponentCategory.MAP_SENSOR -> isComboTmapSensor && pullupRequired
                category == ComponentCategory.OIL_PRESSURE -> isComboOilPressureTempSensor && pullupRequired
                category == ComponentCategory.OTHER -> showGenericOtherPullup && pullupRequired
                else -> false
              }

              // 4. Tabele Dead Time - TYLKO dla INJECTORS
              val finalDeadTimeTables = if (category == ComponentCategory.INJECTORS) {
                deadTimeTablesDraft.map { tbl ->
                  com.example.model.DeadTimeTable(
                    tableName = tbl.tableName.trim().ifBlank { "Tabela ciśnienia" },
                    pressureUnit = "bar",
                    notes = tbl.notes.trim(),
                    points = tbl.points.map { pt ->
                      com.example.model.DeadTimePoint(
                        voltage = pt.voltage.trim(),
                        timeMs = pt.timeMs.trim()
                      )
                    }
                  )
                }
              } else {
                emptyList()
              }

              val finalImages = imagesList.toList()
              val primaryImg = finalImages.firstOrNull() ?: ""

              // 5. Wydajność wtryskiwacza - TYLKO dla INJECTORS
              val parsedFlowRate = if (category == ComponentCategory.INJECTORS) {
                flowRateCcInput.replace(",", ".").trim().toDoubleOrNull()
              } else null

              val parsedFlowPressure = if (category == ComponentCategory.INJECTORS) {
                flowPressureBarInput.replace(",", ".").trim().toDoubleOrNull()
              } else null

              // 6. Wymiary przepustnicy - TYLKO dla THROTTLE
              val parsedInnerDiameter = if (category == ComponentCategory.THROTTLE) {
                innerDiameterInput.replace(",", ".").trim().toDoubleOrNull()
              } else null

              val parsedOuterDiameter = if (category == ComponentCategory.THROTTLE) {
                outerDiameterInput.replace(",", ".").trim().toDoubleOrNull()
              } else null

              val parsedBoltWidth = if (category == ComponentCategory.THROTTLE) {
                boltSpacingWidthInput.replace(",", ".").trim().toDoubleOrNull()
              } else null

              val parsedBoltHeight = if (category == ComponentCategory.THROTTLE) {
                boltSpacingHeightInput.replace(",", ".").trim().toDoubleOrNull()
              } else null

              val parsedDiag1 = if (category == ComponentCategory.THROTTLE) {
                boltDiagonal1Input.replace(",", ".").trim().toDoubleOrNull()
              } else null

              val parsedDiag2 = if (category == ComponentCategory.THROTTLE) {
                boltDiagonal2Input.replace(",", ".").trim().toDoubleOrNull()
              } else null

              onSave(
                name,
                partNumber,
                category,
                shortDescription,
                primaryImg,
                finalImages,
                finalPins,
                finalCharacteristic,
                finalResistanceChar,
                finalPullupRequired,
                pullupValue,
                pullupNote,
                importantNotes,
                connectorImageUrl,
                finalDeadTimeTables,
                parsedFlowRate,
                parsedFlowPressure,
                parsedInnerDiameter,
                parsedOuterDiameter,
                parsedBoltWidth,
                parsedBoltHeight,
                parsedDiag1,
                parsedDiag2,
                injectorImpedance.trim(),
                ecuOutputsCount.trim(),
                ecuBoardDimensions.trim()
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = if (isEditing) TunerAmber else TuningRed),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .padding(end = 8.dp)
              .testTag("submit_add_sensor_button")
          ) {
            Icon(
              imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Publish,
              contentDescription = null,
              tint = if (isEditing) Color.Black else Color.White,
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = if (isEditing) "Zapisz zmiany" else "Opublikuj",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (isEditing) Color.Black else Color.White
            )
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
      if (errorMessage != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF441818), RoundedCornerShape(8.dp))
            .border(1.dp, TuningRed, RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Text(text = errorMessage!!, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
      }

      if (isEditing) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, TunerAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(10.dp)
        ) {
          Text(
            text = "Edytujesz wpis w bazie społecznościowej jako @$userNick. Twoje poprawki zostaną natychmiast zapisane dla wszystkich użytkowników.",
            fontSize = 12.sp,
            color = TunerAmber
          )
        }
        Spacer(Modifier.height(14.dp))
      }

      // Asystent automatycznego zaczerpnięcia danych (AI / OCR z dokumentacji)
      AutoFillDatasheetCard(
        currentCategory = category,
        onDataExtracted = { data ->
          data.name?.let { name = it }
          data.partNumber?.let { partNumber = it }
          data.category?.let { cat -> category = cat }
          data.shortDescription?.let { shortDescription = it }
          data.flowRateCc?.let { flowRateCcInput = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
          data.flowPressureBar?.let { flowPressureBarInput = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
          if (data.deadTimeTables.isNotEmpty()) {
            deadTimeTablesDraft.clear()
            deadTimeTablesDraft.addAll(data.deadTimeTables)
          }
          if (data.calibrationPoints.isNotEmpty()) {
            calPointsList.clear()
            calPointsList.addAll(data.calibrationPoints)
          }
          data.calInputUnit?.let { tableInputUnit = it }
          data.calOutputUnit?.let { tableOutputUnit = it }
          data.charTitle?.let { charTitle = it }
          data.charType?.let { charType = it }
          if (data.resistancePoints.isNotEmpty()) {
            resPointsList.clear()
            resPointsList.addAll(data.resistancePoints)
            includeResistanceChar = true
          }
          data.resInputUnit?.let { resTableInputUnit = it }
          data.resOutputUnit?.let { resTableOutputUnit = it }
          data.resCharTitle?.let { resCharTitle = it }
          data.pullupRequired?.let { pullupRequired = it }
          data.pullupRecommendedValue?.let { pullupValue = it }
          data.innerDiameterMm?.let { innerDiameterInput = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
          data.outerDiameterMm?.let { outerDiameterInput = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
          data.boltSpacingWidthMm?.let { boltSpacingWidthInput = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
          data.boltSpacingHeightMm?.let { boltSpacingHeightInput = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
          data.boltDiagonal1Mm?.let { boltDiagonal1Input = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
          data.boltDiagonal2Mm?.let { boltDiagonal2Input = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
          if (data.pins.isNotEmpty()) {
            pinsList.clear()
            pinsList.addAll(data.pins)

            // Automatycznie zaktualizuj listę wtyczek (np. gdy sterownik ECU posiada 5 wtyczek)
            val extractedConnectors = data.pins.map { it.connectorName.ifBlank { "Wtyczka A" } }.distinct()
            if (extractedConnectors.isNotEmpty()) {
              connectorsList.clear()
              connectorsList.addAll(extractedConnectors)
            }
          }
          data.importantNotes?.let {
            if (it.isNotBlank()) {
              importantNotes = if (importantNotes.isBlank()) it else "$importantNotes\n$it"
            }
          }

          // Dostosowanie dozwolonych pól pod kątem kategorii po ekstrakcji
          if (category == ComponentCategory.THROTTLE ||
              category == ComponentCategory.INJECTORS ||
              category == ComponentCategory.COILS ||
              category == ComponentCategory.LAMBDA ||
              category == ComponentCategory.ECU ||
              category == ComponentCategory.MODULES) {
            pullupRequired = false
            includeResistanceChar = false
            isComboTmapSensor = false
            isComboOilPressureTempSensor = false
          }
        },
        modifier = Modifier.padding(bottom = 16.dp)
      )

      // 1. Podstawowe informacje
      SectionHeader(title = "1. Podstawowe informacje", icon = Icons.Default.Info)
      Spacer(Modifier.height(8.dp))

      OutlinedTextField(
        value = name,
        onValueChange = { name = it; errorMessage = null },
        label = { Text("Nazwa czujnika / sterownika (wymagana)") },
        placeholder = { Text("np. Czujnik ciśnienia MAP 4 Bar, Cewki zapłonowe R8") },
        modifier = Modifier.fillMaxWidth().testTag("input_sensor_name"),
        colors = customFieldColors(),
        singleLine = true
      )

      Spacer(Modifier.height(10.dp))

      OutlinedTextField(
        value = partNumber,
        onValueChange = { partNumber = it },
        label = { Text("Numer katalogowy / części OEM (Bosch, VAG, BMW itp.)") },
        placeholder = { Text("np. 0 281 002 401, 06E 905 115 E") },
        modifier = Modifier.fillMaxWidth().testTag("input_sensor_part_number"),
        colors = customFieldColors(),
        singleLine = true
      )

      Spacer(Modifier.height(10.dp))

      // Category Selector
      ExposedDropdownMenuBox(
        expanded = categoryDropdownExpanded,
        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
      ) {
        OutlinedTextField(
          value = category.displayName,
          onValueChange = {},
          readOnly = true,
          label = { Text("Kategoria podzespołu") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
          colors = customFieldColors(),
          modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
          expanded = categoryDropdownExpanded,
          onDismissRequest = { categoryDropdownExpanded = false },
          modifier = Modifier.background(DarkSurfaceElevated)
        ) {
          ComponentCategory.values().filter { it != ComponentCategory.ALL }.forEach { cat ->
            DropdownMenuItem(
              text = { Text(cat.displayName, color = TextPrimary) },
              onClick = {
                category = cat
                when (cat) {
                  ComponentCategory.THROTTLE -> {
                    pullupRequired = false
                    includeResistanceChar = false
                    isComboTmapSensor = false
                    isComboOilPressureTempSensor = false
                    if (pinsList.size <= 3 && pinsList.all { it.name.isBlank() && it.description.isBlank() }) {
                      pinsList.clear()
                      pinsList.addAll(
                        listOf(
                          PinDraft(pinNumber = "1", name = "Silnik DBW Motor +", description = "Sterowanie silnikiem przepustnicy (H-Bridge A)", wireColor = "Żółty", type = PinType.MOTOR, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "2", name = "Silnik DBW Motor -", description = "Sterowanie silnikiem przepustnicy (H-Bridge B)", wireColor = "Niebieski", type = PinType.MOTOR, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "3", name = "Sensor GND", description = "Masa sensoryczna czujników TPS z ECU", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "4", name = "TPS 1 Sygnał", description = "Potencjometr główny 0.5V - 4.5V", wireColor = "Biały", type = PinType.TPS, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "5", name = "+5V VREF", description = "Zasilanie referencyjne z ECU", wireColor = "Czerwony", type = PinType.VREF, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "6", name = "TPS 2 Sygnał", description = "Potencjometr redundantny 4.5V - 0.5V", wireColor = "Zielony", type = PinType.TPS, connectorName = "Wtyczka A")
                        )
                      )
                    }
                  }
                  ComponentCategory.INJECTORS -> {
                    pullupRequired = false
                    includeResistanceChar = false
                    isComboTmapSensor = false
                    isComboOilPressureTempSensor = false
                    if (pinsList.size <= 3 && pinsList.all { it.name.isBlank() && it.description.isBlank() }) {
                      pinsList.clear()
                      pinsList.addAll(
                        listOf(
                          PinDraft(pinNumber = "1", name = "+12V Zasilanie", description = "Zasilanie po stacyjce / przekaźnik pompy", wireColor = "Czerwony", type = PinType.POWER, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "2", name = "Sterowanie wtryskiem (ECU)", description = "Kluczowanie masą z wyjścia wtryskiwacza Low-Side", wireColor = "Niebieski", type = PinType.INJECTOR, connectorName = "Wtyczka A")
                        )
                      )
                    }
                  }
                  ComponentCategory.COILS -> {
                    pullupRequired = false
                    includeResistanceChar = false
                    isComboTmapSensor = false
                    isComboOilPressureTempSensor = false
                    charTitle = "Tabela Dwell Time (Czas ładowania cewki)"
                    tableInputUnit = "ms"
                    tableOutputUnit = "V"
                    charType = CharType.DWELL_TIME
                    if (pinsList.size <= 3 && pinsList.all { it.name.isBlank() && it.description.isBlank() }) {
                      pinsList.clear()
                      pinsList.addAll(
                        listOf(
                          PinDraft(pinNumber = "1", name = "+12V Zasilanie główne", description = "Zasilanie z przekaźnika po stacyjce", wireColor = "Czerwony", type = PinType.POWER, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "2", name = "Masa iskry / głowicy (Power GND)", description = "Bezpośrednio do głowicy silnika", wireColor = "Brązowy", type = PinType.GROUND, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "3", name = "Sygnał wyzwalający (Trigger 5V)", description = "Logika 5V TTL z wyjścia zapłonu ECU", wireColor = "Szary", type = PinType.IGNITION, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "4", name = "Masa logiczna (Logic GND)", description = "Masa sterująca z ECU lub silnika", wireColor = "Czarny", type = PinType.GROUND, connectorName = "Wtyczka A")
                        )
                      )
                    }
                    if (calPointsList.isEmpty() || calPointsList.all { it.inVal.isBlank() }) {
                      calPointsList.clear()
                      calPointsList.addAll(
                        listOf(
                          CalPointDraft(inVal = "3.20", inUnit = "ms", outVal = "10.0", outUnit = "V"),
                          CalPointDraft(inVal = "2.80", inUnit = "ms", outVal = "11.0", outUnit = "V"),
                          CalPointDraft(inVal = "2.50", inUnit = "ms", outVal = "12.0", outUnit = "V"),
                          CalPointDraft(inVal = "2.30", inUnit = "ms", outVal = "13.0", outUnit = "V"),
                          CalPointDraft(inVal = "2.10", inUnit = "ms", outVal = "14.0", outUnit = "V"),
                          CalPointDraft(inVal = "1.90", inUnit = "ms", outVal = "15.0", outUnit = "V")
                        )
                      )
                    }
                  }
                  ComponentCategory.TEMP_SENSOR -> {
                    pullupRequired = true
                    includeResistanceChar = true
                    isComboTmapSensor = false
                    isComboOilPressureTempSensor = false
                    resCharTitle = "Charakterystyka opornościowa NTC (Temp vs Rezystancja)"
                    resTableInputUnit = "°C"
                    resTableOutputUnit = "Ω"
                    if (pinsList.size <= 3 && pinsList.all { it.name.isBlank() && it.description.isBlank() }) {
                      pinsList.clear()
                      pinsList.addAll(
                        listOf(
                          PinDraft(pinNumber = "1", name = "Sensor GND", description = "Masa sensoryczna z ECU", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "2", name = "Sygnał temperatury (NTC)", description = "Wejście analogowe z rezystorem pull-up", wireColor = "Biały", type = PinType.SIGNAL_IN, connectorName = "Wtyczka A")
                        )
                      )
                    }
                  }
                  ComponentCategory.MAP_SENSOR -> {
                    pullupRequired = false
                    includeResistanceChar = false
                    isComboTmapSensor = false
                    charTitle = "Kalibracja liniowa MAP (kPa vs V)"
                    tableInputUnit = "kPa"
                    tableOutputUnit = "V"
                    charType = CharType.VOLTAGE_PRESSURE
                    if (pinsList.size <= 3 && pinsList.all { it.name.isBlank() && it.description.isBlank() }) {
                      pinsList.clear()
                      pinsList.addAll(
                        listOf(
                          PinDraft(pinNumber = "1", name = "Sensor GND", description = "Masa sensoryczna z ECU", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "2", name = "+5V VREF", description = "Zasilanie referencyjne 5V z ECU", wireColor = "Czerwony", type = PinType.VREF, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "3", name = "Sygnał ciśnienia MAP", description = "Sygnał 0.5V - 4.5V do ECU", wireColor = "Biały", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka A")
                        )
                      )
                    }
                  }
                  ComponentCategory.FUEL_PRESSURE, ComponentCategory.OIL_PRESSURE -> {
                    pullupRequired = false
                    includeResistanceChar = false
                    isComboOilPressureTempSensor = false
                    charTitle = "Kalibracja liniowa ciśnienia (bar vs V)"
                    tableInputUnit = "bar"
                    tableOutputUnit = "V"
                    charType = CharType.VOLTAGE_PRESSURE
                    if (pinsList.size <= 3 && pinsList.all { it.name.isBlank() && it.description.isBlank() }) {
                      pinsList.clear()
                      pinsList.addAll(
                        listOf(
                          PinDraft(pinNumber = "1", name = "Sensor GND", description = "Masa sensoryczna z ECU", wireColor = "Brązowy", type = PinType.SENSOR_GND, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "2", name = "+5V VREF", description = "Zasilanie referencyjne 5V z ECU", wireColor = "Czerwony", type = PinType.VREF, connectorName = "Wtyczka A"),
                          PinDraft(pinNumber = "3", name = "Sygnał ciśnienia", description = "Sygnał analogowy 0.5V - 4.5V", wireColor = "Biały", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka A")
                        )
                      )
                    }
                  }
                  ComponentCategory.LAMBDA -> {
                    pullupRequired = false
                    includeResistanceChar = false
                    isComboTmapSensor = false
                    isComboOilPressureTempSensor = false
                    if (pinsList.size <= 3 && pinsList.all { it.name.isBlank() && it.description.isBlank() }) {
                      pinsList.clear()
                      pinsList.addAll(
                        listOf(
                          PinDraft(pinNumber = "1", name = "IP (Pump Current)", description = "Prąd pompowania tlenu z kontrolera", wireColor = "Czerwony", type = PinType.SIGNAL_OUT, connectorName = "Wtyczka LSU"),
                          PinDraft(pinNumber = "2", name = "VM (Virtual Ground)", description = "Masa wirtualna z kontrolera", wireColor = "Żółty", type = PinType.SENSOR_GND, connectorName = "Wtyczka LSU"),
                          PinDraft(pinNumber = "3", name = "H- (Grzałka -)", description = "Sterowanie masą grzałki PWM z kontrolera/ECU", wireColor = "Biały", type = PinType.HEATER, connectorName = "Wtyczka LSU"),
                          PinDraft(pinNumber = "4", name = "H+ (Grzałka +12V)", description = "Zasilanie grzałki +12V po przekaźniku", wireColor = "Szary", type = PinType.POWER, connectorName = "Wtyczka LSU"),
                          PinDraft(pinNumber = "5", name = "IA / RT (Trim Resistor)", description = "Rezystor kalibracyjny we wtyczce", wireColor = "Zielony", type = PinType.SIGNAL_IN, connectorName = "Wtyczka LSU"),
                          PinDraft(pinNumber = "6", name = "UN / RE (Nernst Voltage)", description = "Napięcie celi Nernsta 450mV", wireColor = "Czarny", type = PinType.SIGNAL_IN, connectorName = "Wtyczka LSU")
                        )
                      )
                    }
                  }
                  ComponentCategory.ECU, ComponentCategory.MODULES -> {
                    pullupRequired = false
                    includeResistanceChar = false
                    isComboTmapSensor = false
                    isComboOilPressureTempSensor = false
                  }
                  ComponentCategory.OTHER, ComponentCategory.ALL -> {
                    // Pozostaw bez zmian
                  }
                }
                categoryDropdownExpanded = false
              }
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      OutlinedTextField(
        value = shortDescription,
        onValueChange = { shortDescription = it },
        label = { Text("Krótki opis / zastosowanie") },
        placeholder = { Text("Do jakich silników, aut, projektów standalone pasuje...") },
        modifier = Modifier.fillMaxWidth(),
        colors = customFieldColors(),
        minLines = 2,
        maxLines = 4
      )

      // Zdjęcia podzespołu: Galeria telefonu (multi-photo) lub URL z internetu
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Zdjęcia czujnika (zapis w chmurze)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary
              )
            }
            if (imagesList.isNotEmpty()) {
              Text(
                text = "${imagesList.size} / 8 zdjęć",
                fontSize = 11.sp,
                color = TunerAmber,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(Modifier.height(10.dp))

          // PRZYCISKI DODAWANIA ZDJĘĆ
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                multiPhotoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("pick_from_gallery_button"),
              colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Galeria",
                tint = TunerAmber,
                modifier = Modifier.size(18.dp)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = if (imagesList.isEmpty()) "Wybierz z galerii" else "+ Dodaj z galerii",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
              )
            }

            if (imagesList.isNotEmpty() && imagesList.size < 8) {
              OutlinedButton(
                onClick = {
                  singlePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TunerCyan)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Jedno foto", color = TunerCyan, fontSize = 12.sp)
              }
            }
          }

          if (isCompressingPhotos) {
            Spacer(Modifier.height(10.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                color = TunerCyan,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
              )
              Spacer(Modifier.width(10.dp))
              Text(
                text = "Optymalizowanie zdjęć do chmury Firestore...",
                fontSize = 12.sp,
                color = TunerCyan,
                fontWeight = FontWeight.Medium
              )
            }
          }

          // LISTA DODANYCH ZDJĘĆ
          if (imagesList.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
              text = "Dodane zdjęcia (pierwsze zdjęcie to miniatura na liście):",
              fontSize = 12.sp,
              color = TextSecondary,
              fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))

            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              itemsIndexed(imagesList) { index, photoItem ->
                Card(
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                  border = androidx.compose.foundation.BorderStroke(
                    width = if (index == 0) 2.dp else 1.dp,
                    color = if (index == 0) TunerCyan else BorderStroke
                  ),
                  modifier = Modifier.width(140.dp)
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    // Badge: Główne vs Kolejne
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      if (index == 0) {
                        Box(
                          modifier = Modifier
                            .background(TunerCyan, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text(
                            text = "★ GŁÓWNE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                          )
                        }
                      } else {
                        Text(
                          text = "#${index + 1}",
                          fontSize = 11.sp,
                          color = TextSecondary,
                          fontWeight = FontWeight.Bold
                        )
                      }

                      IconButton(
                        onClick = {
                          imagesList.removeAt(index)
                        },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "Usuń zdjęcie",
                          tint = TuningRed,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Thumbnail preview
                    ComponentThumbnail(
                      imageUrl = photoItem,
                      vectorKey = "map_3b",
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                    )

                    Spacer(Modifier.height(6.dp))

                    // Controls: Reorder or Set as primary
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      if (index > 0) {
                        TextButton(
                          onClick = {
                            val item = imagesList.removeAt(index)
                            imagesList.add(0, item)
                          },
                          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                          modifier = Modifier.height(26.dp)
                        ) {
                          Text("Na 1. miejsce", fontSize = 10.sp, color = TunerAmber, fontWeight = FontWeight.Bold)
                        }
                      } else {
                        Text(
                          text = "Karta w bazie",
                          fontSize = 10.sp,
                          color = TunerCyan,
                          fontWeight = FontWeight.Medium
                        )
                      }

                      Row {
                        if (index > 0) {
                          IconButton(
                            onClick = {
                              val item = imagesList.removeAt(index)
                              imagesList.add(index - 1, item)
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Przesuń w lewo", tint = TextPrimary, modifier = Modifier.size(14.dp))
                          }
                        }
                        if (index < imagesList.size - 1) {
                          IconButton(
                            onClick = {
                              val item = imagesList.removeAt(index)
                              imagesList.add(index + 1, item)
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Przesuń w prawo", tint = TextPrimary, modifier = Modifier.size(14.dp))
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(Modifier.height(12.dp))

          // ALTERNATYWA: WPISZ LINK Z INTERNETU
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = urlInputText,
              onValueChange = { urlInputText = it },
              label = { Text("Lub dodaj link URL do zdjęcia") },
              placeholder = { Text("https://...") },
              modifier = Modifier
                .weight(1f)
                .testTag("input_sensor_image_url"),
              colors = customFieldColors(),
              leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = TextMuted) },
              singleLine = true
            )

            Button(
              onClick = {
                if (urlInputText.isNotBlank()) {
                  imagesList.add(urlInputText.trim())
                  urlInputText = ""
                }
              },
              enabled = urlInputText.isNotBlank(),
              colors = ButtonDefaults.buttonColors(containerColor = TunerCyan),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(54.dp)
            ) {
              Text("Dodaj", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }

          Spacer(Modifier.height(10.dp))
          Text(
            text = "✓ Zdjęcia są zapisywane w chmurze Firestore — będą widoczne dla wszystkich i po ponownej instalacji aplikacji. Pierwsze zdjęcie jest wyświetlane jako główne na liście w bazie.",
            fontSize = 11.sp,
            color = TunerCyan,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(Modifier.height(20.dp))

      // 2. Zdjęcie wtyczki (Connector / Plug) nad układem pinów
      SectionHeader(title = "2. Zdjęcie wtyczki (Złącza)", icon = Icons.Default.Cable)
      Spacer(Modifier.height(8.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Zdjęcie wtyczki / gniazda czujnika",
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Dodaj zdjęcie wtyczki (widok od strony pinów lub zapinki). Będzie ono wyświetlane nad rozkładem pinów.",
            fontSize = 12.sp,
            color = TextSecondary
          )

          Spacer(Modifier.height(10.dp))

          if (connectorImageUrl.isNotBlank()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CarbonBlack)
            ) {
              ComponentThumbnail(
                imageUrl = connectorImageUrl,
                vectorKey = "vw_coils",
                modifier = Modifier.fillMaxSize()
              )
              IconButton(
                onClick = { connectorImageUrl = "" },
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(6.dp)
                  .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                  .size(32.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń zdjęcie wtyczki", tint = TuningRed, modifier = Modifier.size(18.dp))
              }
            }
            Spacer(Modifier.height(10.dp))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                connectorPhotoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = TunerCyan),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                text = if (connectorImageUrl.isNotBlank()) "Zmień z galerii" else "Wybierz z galerii",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
            if (connectorImageUrl.isNotBlank()) {
              OutlinedButton(
                onClick = { connectorImageUrl = "" },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TuningRed),
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Usuń", fontSize = 12.sp)
              }
            }
          }
        }
      }

      Spacer(Modifier.height(20.dp))

      // 3. Pinout Builder with Multi-Connector support
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        SectionHeader(title = "3. Układ pinów (Pinout)", icon = Icons.Default.ElectricalServices)
        OutlinedButton(
          onClick = {
            val nextLetter = ('A'.code + connectorsList.size).toChar().toString()
            val newConnectorName = "Wtyczka $nextLetter"
            connectorsList.add(newConnectorName)
            pinsList.add(
              PinDraft(
                pinNumber = "1",
                name = "",
                description = "",
                wireColor = "",
                type = PinType.OTHER,
                connectorName = newConnectorName
              )
            )
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Dodaj nową wtyczkę", fontSize = 12.sp)
        }
      }

      Spacer(Modifier.height(10.dp))

      connectorsList.forEachIndexed { connIdx, connName ->
        Card(
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            // Header for connector: Name input + Delete connector
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = connName,
                onValueChange = { newName ->
                  val oldName = connName
                  connectorsList[connIdx] = newName
                  pinsList.forEachIndexed { pIdx, p ->
                    if (p.connectorName == oldName) {
                      pinsList[pIdx] = p.copy(connectorName = newName)
                    }
                  }
                },
                label = { Text("Nazwa wtyczki (np. Wtyczka A / Czarna)") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true,
                leadingIcon = {
                  Icon(
                    Icons.Default.Cable,
                    contentDescription = null,
                    tint = TunerAmber,
                    modifier = Modifier.size(18.dp)
                  )
                }
              )

              if (connectorsList.size > 1) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                  onClick = {
                    val nameToRemove = connName
                    connectorsList.removeAt(connIdx)
                    pinsList.removeAll { it.connectorName == nameToRemove }
                  }
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Usuń całą wtyczkę", tint = TuningRed)
                }
              }
            }

            Spacer(Modifier.height(10.dp))

            val pinsForThisConn = pinsList.filter { it.connectorName == connName }

            Text(
              text = "Piny w tej wtyczce (${pinsForThisConn.size}):",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextSecondary
            )

            Spacer(Modifier.height(8.dp))

            pinsForThisConn.forEach { pinDraft ->
              key(pinDraft.id) {
                val globalIndex = pinsList.indexOfFirst { it.id == pinDraft.id }
                val pinColor = resolvePinTextColor(pinDraft.wireColor)
                Card(
                  modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderStroke)
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    // Wiersz 1: Po lewo Nr pinu, od środka do prawej Opis pinu w wybranym kolorze
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedTextField(
                        value = pinDraft.pinNumber,
                        onValueChange = { newVal ->
                          if (globalIndex >= 0) {
                            pinsList[globalIndex] = pinDraft.copy(pinNumber = newVal)
                          }
                        },
                        label = { Text("Nr pinu") },
                        modifier = Modifier.width(90.dp),
                        colors = customFieldColors(),
                        singleLine = true
                      )
                      Spacer(Modifier.width(8.dp))
                      OutlinedTextField(
                        value = pinDraft.name,
                        onValueChange = { newVal ->
                          if (globalIndex >= 0) {
                            pinsList[globalIndex] = pinDraft.copy(name = newVal)
                          }
                        },
                        label = { Text("Opis / funkcja pinu") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                          focusedTextColor = pinColor,
                          unfocusedTextColor = pinColor,
                          focusedBorderColor = pinColor.copy(alpha = 0.8f),
                          unfocusedBorderColor = BorderStroke,
                          focusedLabelColor = pinColor,
                          cursorColor = pinColor,
                          focusedContainerColor = DarkSurface,
                          unfocusedContainerColor = DarkSurface
                        ),
                        singleLine = true
                      )
                      // Przyciski przesuwania pinu w górę i w dół oraz usuwanie
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        val currentLocalIdx = pinsForThisConn.indexOfFirst { it.id == pinDraft.id }
                        IconButton(
                          onClick = {
                            if (currentLocalIdx > 0) {
                              val prevPin = pinsForThisConn[currentLocalIdx - 1]
                              val idx1 = pinsList.indexOfFirst { it.id == pinDraft.id }
                              val idx2 = pinsList.indexOfFirst { it.id == prevPin.id }
                              if (idx1 >= 0 && idx2 >= 0) {
                                val temp = pinsList[idx1]
                                pinsList[idx1] = pinsList[idx2]
                                pinsList[idx2] = temp
                              }
                            }
                          },
                          enabled = currentLocalIdx > 0,
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Przesuń pin w górę",
                            tint = if (currentLocalIdx > 0) TunerCyan else TextMuted,
                            modifier = Modifier.size(18.dp)
                          )
                        }

                        IconButton(
                          onClick = {
                            if (currentLocalIdx < pinsForThisConn.size - 1) {
                              val nextPin = pinsForThisConn[currentLocalIdx + 1]
                              val idx1 = pinsList.indexOfFirst { it.id == pinDraft.id }
                              val idx2 = pinsList.indexOfFirst { it.id == nextPin.id }
                              if (idx1 >= 0 && idx2 >= 0) {
                                val temp = pinsList[idx1]
                                pinsList[idx1] = pinsList[idx2]
                                pinsList[idx2] = temp
                              }
                            }
                          },
                          enabled = currentLocalIdx < pinsForThisConn.size - 1,
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Przesuń pin w dół",
                            tint = if (currentLocalIdx < pinsForThisConn.size - 1) TunerCyan else TextMuted,
                            modifier = Modifier.size(18.dp)
                          )
                        }

                        IconButton(
                          onClick = {
                            if (pinsList.size > 1) {
                              pinsList.removeIf { it.id == pinDraft.id }
                            }
                          },
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(Icons.Default.Delete, contentDescription = "Usuń pin", tint = TuningRed, modifier = Modifier.size(18.dp))
                        }
                      }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Wiersz 2: Wybór koloru (Czerwony, Brązowy, Biały, Niebieski)
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text("Kolor:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                      val colorOptions = listOf(
                        "Czerwony" to Color(0xFFFF453A),
                        "Brązowy" to Color(0xFFC68A4C),
                        "Biały" to Color(0xFFF5F5F7),
                        "Niebieski" to Color(0xFF38BDF8)
                      )
                      colorOptions.forEach { (cName, cVal) ->
                        val isSelected = pinDraft.wireColor.equals(cName, ignoreCase = true) ||
                          (cName == "Biały" && pinDraft.wireColor.isBlank())
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) cVal.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                              width = if (isSelected) 1.5.dp else 0.5.dp,
                              color = if (isSelected) cVal else BorderStroke,
                              shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                              if (globalIndex >= 0) {
                                pinsList[globalIndex] = pinDraft.copy(wireColor = cName)
                              }
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                              modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(cVal)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                              text = cName,
                              fontSize = 11.sp,
                              color = if (isSelected) Color.White else TextMuted,
                              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                          }
                        }
                      }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Wiersz 3: Uwagi do pinu równo od lewej do prawej pod nr pinu i opisem
                    OutlinedTextField(
                      value = pinDraft.description,
                      onValueChange = { newVal ->
                        if (globalIndex >= 0) {
                          pinsList[globalIndex] = pinDraft.copy(description = newVal)
                        }
                      },
                      label = { Text("Uwagi do pinu (opcjonalnie)") },
                      placeholder = { Text("np. VREF +5V z ECU, minimalny przekrój 0.5mm²") },
                      modifier = Modifier.fillMaxWidth(),
                      colors = customFieldColors(),
                      singleLine = true
                    )
                  }
                }
              }
            }

            // Przycisk "Dodaj pin" umieszczony POD pinami
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
              onClick = {
                val existingNumbers = pinsForThisConn.mapNotNull { p ->
                  Regex("""\d+""").find(p.pinNumber)?.value?.toIntOrNull()
                }
                val nextNum = if (existingNumbers.isEmpty()) 1 else (existingNumbers.maxOrNull() ?: 0) + 1
                val newPin = PinDraft(
                  pinNumber = "$nextNum",
                  name = "",
                  description = "",
                  wireColor = "",
                  type = PinType.OTHER,
                  connectorName = connName
                )
                val lastIndexInList = pinsList.indexOfLast { it.connectorName == connName }
                if (lastIndexInList >= 0 && lastIndexInList < pinsList.size) {
                  pinsList.add(lastIndexInList + 1, newPin)
                } else {
                  pinsList.add(newPin)
                }
              },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerAmber),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Dodaj pin ($connName)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
          }
        }
      }

      var secCounter = 4

      // ==========================================
      // KATEGORIA: THROTTLE (Przepustnice)
      // ==========================================
      if (category == ComponentCategory.THROTTLE) {
        SectionHeader(title = "${secCounter++}. Wymiary gardzieli i kołnierza montażowego", icon = Icons.Default.Architecture)
        Spacer(Modifier.height(8.dp))

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, TunerCyan.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .background(TunerCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Architecture,
                  contentDescription = null,
                  tint = TunerCyan,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Wymiary gardzieli i kołnierza montażowego",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = TextPrimary
              )
            }

            Spacer(Modifier.height(8.dp))
            Text(
              text = "Podaj średnicę wewnętrzną i zewnętrzną oraz wymiary rozstawu otworów na śruby montażowe (od środka do środka śruby dla boku 1 i boku 2 oraz przekątne).",
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 16.sp
            )

            Spacer(Modifier.height(14.dp))

            // Średnice
            Text(
              text = "Średnice przepustnicy [mm]:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TunerCyan
            )
            Spacer(Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = innerDiameterInput,
                onValueChange = { innerDiameterInput = it },
                label = { Text("Średnica wewn. (gardziel)") },
                placeholder = { Text("np. 68") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                  keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
              )

              OutlinedTextField(
                value = outerDiameterInput,
                onValueChange = { outerDiameterInput = it },
                label = { Text("Średnica zewn. (dolot)") },
                placeholder = { Text("np. 76") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                  keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
              )
            }

            Spacer(Modifier.height(14.dp))

            // Rozmieszczenie otworów montażowych
            Text(
              text = "Rozstaw otworów na śruby (od środka do środka śruby) [mm]:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TunerAmber
            )
            Spacer(Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = boltSpacingWidthInput,
                onValueChange = { boltSpacingWidthInput = it },
                label = { Text("Bok 1 (szerokość)") },
                placeholder = { Text("np. 60") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                  keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
              )

              OutlinedTextField(
                value = boltSpacingHeightInput,
                onValueChange = { boltSpacingHeightInput = it },
                label = { Text("Bok 2 (wysokość)") },
                placeholder = { Text("np. 60") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                  keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
              )
            }

            Spacer(Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = boltDiagonal1Input,
                onValueChange = { boltDiagonal1Input = it },
                label = { Text("Przekątna 1 (D1)") },
                placeholder = { Text("np. 84.8") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                  keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
              )

              OutlinedTextField(
                value = boltDiagonal2Input,
                onValueChange = { boltDiagonal2Input = it },
                label = { Text("Przekątna 2 (D2)") },
                placeholder = { Text("np. 84.8") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                  keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
              )
            }

            Spacer(Modifier.height(10.dp))

            // Przycisk wyliczenia przekątnej z boków
            FilledTonalButton(
              onClick = {
                val w = boltSpacingWidthInput.replace(",", ".").trim().toDoubleOrNull()
                val h = boltSpacingHeightInput.replace(",", ".").trim().toDoubleOrNull()
                if (w != null && h != null && w > 0.0 && h > 0.0) {
                  val diag = hypot(w, h)
                  val diagStr = String.format(java.util.Locale.US, "%.1f", diag)
                  boltDiagonal1Input = diagStr
                  boltDiagonal2Input = diagStr
                }
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = DarkSurfaceVariant,
                contentColor = TunerCyan
              )
            ) {
              Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(8.dp))
              Text("Oblicz przekątne z wymiarów boków: √(a² + b²)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Podgląd schematu na żywo
            val pInner = innerDiameterInput.replace(",", ".").trim().toDoubleOrNull()
            val pOuter = outerDiameterInput.replace(",", ".").trim().toDoubleOrNull()
            val pWidth = boltSpacingWidthInput.replace(",", ".").trim().toDoubleOrNull()
            val pHeight = boltSpacingHeightInput.replace(",", ".").trim().toDoubleOrNull()
            val pDiag1 = boltDiagonal1Input.replace(",", ".").trim().toDoubleOrNull()
            val pDiag2 = boltDiagonal2Input.replace(",", ".").trim().toDoubleOrNull()

            if (pInner != null || pOuter != null || pWidth != null || pHeight != null) {
              Spacer(Modifier.height(14.dp))
              Text(
                text = "Podgląd schematu technicznego:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
              Spacer(Modifier.height(8.dp))
              ThrottleDimensionsCard(
                innerDiameterMm = pInner,
                outerDiameterMm = pOuter,
                boltSpacingWidthMm = pWidth,
                boltSpacingHeightMm = pHeight,
                boltDiagonal1Mm = pDiag1,
                boltDiagonal2Mm = pDiag2
              )
            }
          }
        }
      }

      // ==========================================
      // KATEGORIA: INJECTORS (Wtryskiwacze)
      // ==========================================
      else if (category == ComponentCategory.INJECTORS) {
        SectionHeader(title = "${secCounter++}. Wydajność nominalna i ciśnienie testowe", icon = Icons.Default.Speed)
        Spacer(Modifier.height(8.dp))

        // Karta deklaracji wydajności bazowej
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, TunerCyan.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .background(TunerCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Speed,
                  contentDescription = null,
                  tint = TunerCyan,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Wydajność bazowa wtryskiwacza",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = TextPrimary
              )
            }

            Spacer(Modifier.height(8.dp))
            Text(
              text = "Podaj nominalną wydajność wtryskiwacza (np. 630 cc/min) oraz ciśnienie testowe (np. 3.0 bar). W widoku szczegółów tuner będzie mógł wpisać ciśnienie swojego układu i przeliczyć wydajność!",
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = flowRateCcInput,
                onValueChange = { flowRateCcInput = it },
                label = { Text("Wydajność [cc/min]") },
                placeholder = { Text("np. 630") },
                modifier = Modifier.weight(1.2f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = TunerCyan,
                  unfocusedTextColor = TunerCyan,
                  focusedBorderColor = TunerCyan,
                  cursorColor = TunerCyan,
                  focusedContainerColor = DarkSurface,
                  unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
              )

              OutlinedTextField(
                value = flowPressureBarInput,
                onValueChange = { flowPressureBarInput = it },
                label = { Text("Przy ciśnieniu [bar]") },
                placeholder = { Text("np. 3.0") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = TunerAmber,
                  unfocusedTextColor = TunerAmber,
                  focusedBorderColor = TunerAmber,
                  cursorColor = TunerAmber,
                  focusedContainerColor = DarkSurface,
                  unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
              )
            }
          }
        }

        SectionHeader(title = "${secCounter++}. Tabele Dead Time wtryskiwacza (Napięcie vs Czas [ms])", icon = Icons.Default.ElectricalServices)
        Spacer(Modifier.height(8.dp))

        Text(
          text = "Czas martwy (Dead Time / Battery Voltage Compensation) określa opóźnienie otwarcia wtryskiwacza w zależności od napięcia. Możesz dodać tabele dla każdego ciśnienia (np. 2.5 bar, 3.0 bar, 4.0 bar).",
          fontSize = 12.sp,
          color = TextSecondary,
          lineHeight = 17.sp
        )

        Spacer(Modifier.height(10.dp))

        deadTimeTablesDraft.forEachIndexed { tIdx, tableDraft ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
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
                OutlinedTextField(
                  value = tableDraft.tableName,
                  onValueChange = { newName ->
                    deadTimeTablesDraft[tIdx] = tableDraft.copy(tableName = newName)
                  },
                  label = { Text("Nazwa tabeli / Ciśnienie paliwa") },
                  placeholder = { Text("np. 3.0 bar (Diff), 4.0 bar (Diff)") },
                  modifier = Modifier.weight(1f),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TunerCyan,
                    unfocusedTextColor = TunerCyan,
                    focusedLabelColor = TunerCyan,
                    focusedBorderColor = TunerCyan,
                    cursorColor = TunerCyan,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                  ),
                  singleLine = true
                )

                if (deadTimeTablesDraft.size > 1) {
                  Spacer(Modifier.width(8.dp))
                  IconButton(
                    onClick = { deadTimeTablesDraft.removeAt(tIdx) },
                    modifier = Modifier.size(40.dp)
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń tabelę", tint = TuningRed)
                  }
                }
              }

              Spacer(Modifier.height(10.dp))

              // Nagłówek tabeli Dead Time
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                  .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Napięcie [V]",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = TunerCyan,
                  modifier = Modifier.weight(1f)
                )
                Text(
                  text = "Dead Time [ms]",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = TunerAmber,
                  modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(36.dp))
              }

              Spacer(Modifier.height(6.dp))

              // Wiersze punktów Dead Time
              tableDraft.points.forEachIndexed { pIdx, pt ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  OutlinedTextField(
                    value = pt.voltage,
                    onValueChange = { newVal ->
                      tableDraft.points[pIdx] = pt.copy(voltage = newVal)
                    },
                    placeholder = { Text("np. 12.0") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedTextColor = TunerCyan,
                      unfocusedTextColor = TunerCyan,
                      focusedBorderColor = TunerCyan,
                      cursorColor = TunerCyan,
                      focusedContainerColor = DarkSurface,
                      unfocusedContainerColor = DarkSurface
                    ),
                    singleLine = true
                  )
                  OutlinedTextField(
                    value = pt.timeMs,
                    onValueChange = { newVal ->
                      tableDraft.points[pIdx] = pt.copy(timeMs = newVal)
                    },
                    placeholder = { Text("np. 1.14") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedTextColor = TunerAmber,
                      unfocusedTextColor = TunerAmber,
                      focusedBorderColor = TunerAmber,
                      cursorColor = TunerAmber,
                      focusedContainerColor = DarkSurface,
                      unfocusedContainerColor = DarkSurface
                    ),
                    singleLine = true
                  )
                  IconButton(
                    onClick = { if (tableDraft.points.size > 1) tableDraft.points.removeAt(pIdx) },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń punkt", tint = TuningRed, modifier = Modifier.size(18.dp))
                  }
                }
              }

              Spacer(Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    tableDraft.points.add(DeadTimePointDraft(voltage = "", timeMs = ""))
                  },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerAmber),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Dodaj napięcie i czas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }

              Spacer(Modifier.height(8.dp))

              OutlinedTextField(
                value = tableDraft.notes,
                onValueChange = { newNotes ->
                  deadTimeTablesDraft[tIdx] = tableDraft.copy(notes = newNotes)
                },
                label = { Text("Notatki do tabeli (np. przepływ ${tableDraft.tableName})") },
                placeholder = { Text("np. Przepływ 630cc/min przy 3 bar, testowane w 20°C") },
                modifier = Modifier.fillMaxWidth(),
                colors = customFieldColors(),
                singleLine = true
              )
            }
          }
        }

        OutlinedButton(
          onClick = {
            val nextPressureBar = (deadTimeTablesDraft.size + 3).toString()
            val newPts = androidx.compose.runtime.mutableStateListOf<DeadTimePointDraft>()
            val prevTable = deadTimeTablesDraft.lastOrNull()
            if (prevTable != null && prevTable.points.isNotEmpty()) {
              newPts.addAll(prevTable.points.map { DeadTimePointDraft(voltage = it.voltage, timeMs = it.timeMs) })
            } else {
              newPts.addAll(
                listOf(
                  DeadTimePointDraft(voltage = "8.0", timeMs = "2.30"),
                  DeadTimePointDraft(voltage = "10.0", timeMs = "1.65"),
                  DeadTimePointDraft(voltage = "12.0", timeMs = "1.25"),
                  DeadTimePointDraft(voltage = "14.0", timeMs = "1.00"),
                  DeadTimePointDraft(voltage = "16.0", timeMs = "0.85")
                )
              )
            }
            deadTimeTablesDraft.add(
              DeadTimeTableDraft(
                tableName = "$nextPressureBar.0 bar (Diff)",
                notes = "",
                points = newPts
              )
            )
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Dodaj kolejną tabelę ciśnienia (Dead Time)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }

      // ==========================================
      // KATEGORIA: COILS (Cewki zapłonowe)
      // ==========================================
      else if (category == ComponentCategory.COILS) {
        SectionHeader(title = "${secCounter++}. Tabela Dwell Time (Czas ładowania cewki)", icon = Icons.Default.ElectricalServices)
        Spacer(Modifier.height(8.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Tabela czasu ładowania cewki (Dwell Time vs Napięcie)",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "Określa wymagany czas nasycenia cewki energią w milisekundach (ms) w zależności od napięcia zasilania baterii w woltach (V). Wpisz punkty z datasheetu producenta.",
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            // Nagłówek tabeli Dwell
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Napięcie [$tableOutputUnit]",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TunerCyan,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "Dwell Time [$tableInputUnit]",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TunerAmber,
                modifier = Modifier.weight(1f)
              )
              Spacer(Modifier.width(36.dp))
            }

            Spacer(Modifier.height(6.dp))

            // Wiersze punktów Dwell
            calPointsList.forEachIndexed { cIdx, pt ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                OutlinedTextField(
                  value = pt.outVal,
                  onValueChange = { newVal ->
                    calPointsList[cIdx] = pt.copy(outVal = newVal)
                  },
                  placeholder = { Text("np. 12.0") },
                  modifier = Modifier.weight(1f),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TunerCyan,
                    unfocusedTextColor = TunerCyan,
                    focusedBorderColor = TunerCyan,
                    cursorColor = TunerCyan,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                  ),
                  singleLine = true
                )
                OutlinedTextField(
                  value = pt.inVal,
                  onValueChange = { newVal ->
                    calPointsList[cIdx] = pt.copy(inVal = newVal)
                  },
                  placeholder = { Text("np. 2.50") },
                  modifier = Modifier.weight(1f),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TunerAmber,
                    unfocusedTextColor = TunerAmber,
                    focusedBorderColor = TunerAmber,
                    cursorColor = TunerAmber,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                  ),
                  singleLine = true
                )
                IconButton(
                  onClick = { if (calPointsList.size > 1) calPointsList.removeAt(cIdx) },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Usuń punkt", tint = TuningRed, modifier = Modifier.size(18.dp))
                }
              }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
              onClick = { calPointsList.add(CalPointDraft(inVal = "", inUnit = tableInputUnit, outVal = "", outUnit = tableOutputUnit)) },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerAmber),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(6.dp))
              Text("Dodaj napięcie i czas dwell", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
              value = charNotes,
              onValueChange = { charNotes = it },
              label = { Text("Wskazówki cewki dla tunera (np. typ i max dwell)") },
              placeholder = { Text("np. Cewka aktywna z wbudowanym tranzystorem (trigger 5V). Max dwell 2.6 ms przy 14V.") },
              modifier = Modifier.fillMaxWidth(),
              colors = customFieldColors(),
              minLines = 2
            )
          }
        }
      }

      // ==========================================
      // KATEGORIA: TEMP_SENSOR (Czujniki temperatury: IAT, CLT, Olej itp.)
      // ==========================================
      else if (category == ComponentCategory.TEMP_SENSOR) {
        SectionHeader(title = "${secCounter++}. Rezystor PULL-UP (Podciągający do +5V)", icon = Icons.Default.ElectricalServices)
        Spacer(Modifier.height(8.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Wymagany rezystor PULL-UP w sterowniku",
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Termistory NTC/PTC wymagają podciągnięcia sygnału rezystorem do +5V VREF na wejściu analogowym ECU.",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
              }
              Switch(
                checked = pullupRequired,
                onCheckedChange = { pullupRequired = it },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = TunerCyan,
                  checkedTrackColor = TunerCyan.copy(alpha = 0.4f)
                )
              )
            }

            if (pullupRequired) {
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = pullupValue,
                onValueChange = { pullupValue = it },
                label = { Text("Wartość rezystora pull-up (np. 2490 Ω lub 2.49 kΩ)") },
                modifier = Modifier.fillMaxWidth(),
                colors = customFieldColors(),
                singleLine = true
              )

              Spacer(Modifier.height(8.dp))

              OutlinedTextField(
                value = pullupNote,
                onValueChange = { pullupNote = it },
                label = { Text("Uwagi dot. podłączenia do ECU (opcjonalne)") },
                placeholder = { Text("np. Wbudowany pull-up w EMU Black (wejście IAT/CLT)") },
                modifier = Modifier.fillMaxWidth(),
                colors = customFieldColors(),
                singleLine = true
              )
            }
          }
        }

        Spacer(Modifier.height(16.dp))

        SectionHeader(title = "${secCounter++}. Charakterystyka opornościowa NTC (Temperatura vs Rezystancja)", icon = Icons.Default.Thermostat)
        Spacer(Modifier.height(8.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Krzywa rezystancji czujnika (°C vs Ω)",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "Wpisz punkty kalibracji temperatury i odpowiadającej jej oporności w omach. Te dane są niezbędne do wpisania w tabeli analogowej TunerStudio, EMU Black lub Link ECU.",
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            // Ustalanie jednostek
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = resTableInputUnit,
                onValueChange = { resTableInputUnit = it },
                label = { Text("Jedn. temp.") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true
              )
              OutlinedTextField(
                value = resTableOutputUnit,
                onValueChange = { resTableOutputUnit = it },
                label = { Text("Jedn. oporu") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true
              )
            }

            Spacer(Modifier.height(10.dp))

            // Nagłówek tabeli
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Temperatura [${resTableInputUnit.ifBlank { "°C" }}]",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TunerCyan,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "Rezystancja [${resTableOutputUnit.ifBlank { "Ω" }}]",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TunerAmber,
                modifier = Modifier.weight(1.2f)
              )
              Spacer(Modifier.width(36.dp))
            }

            Spacer(Modifier.height(6.dp))

            resPointsList.forEachIndexed { rIdx, rpt ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                OutlinedTextField(
                  value = rpt.inVal,
                  onValueChange = { newVal ->
                    resPointsList[rIdx] = rpt.copy(inVal = newVal)
                  },
                  placeholder = { Text("np. 20") },
                  modifier = Modifier.weight(1f),
                  colors = customFieldColors(),
                  singleLine = true
                )
                OutlinedTextField(
                  value = rpt.outVal,
                  onValueChange = { newVal ->
                    resPointsList[rIdx] = rpt.copy(outVal = newVal)
                  },
                  placeholder = { Text("np. 2500") },
                  modifier = Modifier.weight(1.2f),
                  colors = customFieldColors(),
                  singleLine = true
                )
                IconButton(
                  onClick = { if (resPointsList.size > 1) resPointsList.removeAt(rIdx) },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = TuningRed, modifier = Modifier.size(18.dp))
                }
              }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
              onClick = { resPointsList.add(CalPointDraft(inVal = "", inUnit = resTableInputUnit, outVal = "", outUnit = resTableOutputUnit)) },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(6.dp))
              Text("Dodaj kolejny wiersz temperatury i oporu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
              value = resCharNotes,
              onValueChange = { resCharNotes = it },
              label = { Text("Wskazówka / wzór / model termistora dla tunera") },
              placeholder = { Text("np. Bosch NTC M12, opór w 20°C: 2500 Ω, stała B25/85 = 3977K") },
              modifier = Modifier.fillMaxWidth(),
              colors = customFieldColors(),
              minLines = 2
            )
          }
        }
      }

      // ==========================================
      // KATEGORIA: MAP_SENSOR (Czujniki ciśnienia w dolocie)
      // ==========================================
      else if (category == ComponentCategory.MAP_SENSOR) {
        SectionHeader(title = "${secCounter++}. Kalibracja liniowa MAP (Ciśnienie vs Napięcie)", icon = Icons.Default.ShowChart)
        Spacer(Modifier.height(8.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Kalibracja liniowa MAP (kPa vs V)",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "Podaj punkty kalibracji ciśnienia absolutnego (np. 20 kPa i 300 kPa) oraz odpowiadające im napięcia wyjściowe w woltach (V).",
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            // Ustalanie jednostek
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = tableInputUnit,
                onValueChange = { tableInputUnit = it },
                label = { Text("Jednostka ciśnienia (np. kPa)") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true
              )
              OutlinedTextField(
                value = tableOutputUnit,
                onValueChange = { tableOutputUnit = it },
                label = { Text("Jednostka napięcia (np. V)") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true
              )
            }

            Spacer(Modifier.height(10.dp))

            // Nagłówek tabeli
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Ciśnienie [${tableInputUnit.ifBlank { "kPa" }}]",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TunerCyan,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "Napięcie [${tableOutputUnit.ifBlank { "V" }}]",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TunerAmber,
                modifier = Modifier.weight(1f)
              )
              Spacer(Modifier.width(36.dp))
            }

            Spacer(Modifier.height(6.dp))

            calPointsList.forEachIndexed { cIdx, pt ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                OutlinedTextField(
                  value = pt.inVal,
                  onValueChange = { newVal ->
                    calPointsList[cIdx] = pt.copy(inVal = newVal)
                  },
                  placeholder = { Text("np. 100") },
                  modifier = Modifier.weight(1f),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TunerCyan,
                    unfocusedTextColor = TunerCyan,
                    focusedBorderColor = TunerCyan,
                    cursorColor = TunerCyan,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                  ),
                  singleLine = true
                )
                OutlinedTextField(
                  value = pt.outVal,
                  onValueChange = { newVal ->
                    calPointsList[cIdx] = pt.copy(outVal = newVal)
                  },
                  placeholder = { Text("np. 1.60") },
                  modifier = Modifier.weight(1f),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TunerAmber,
                    unfocusedTextColor = TunerAmber,
                    focusedBorderColor = TunerAmber,
                    cursorColor = TunerAmber,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                  ),
                  singleLine = true
                )
                IconButton(
                  onClick = { if (calPointsList.size > 1) calPointsList.removeAt(cIdx) },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Usuń punkt", tint = TuningRed, modifier = Modifier.size(18.dp))
                }
              }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
              onClick = { calPointsList.add(CalPointDraft(inVal = "", inUnit = tableInputUnit, outVal = "", outUnit = tableOutputUnit)) },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(6.dp))
              Text("Dodaj kolejny wiersz kalibracji", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
              value = charNotes,
              onValueChange = { charNotes = it },
              label = { Text("Wzór matematyczny / zakres sensora MAP") },
              placeholder = { Text("np. MAP 3-bar (300 kPa), V = 0.0152 * p + 0.1, zasilanie 5.0V VREF") },
              modifier = Modifier.fillMaxWidth(),
              colors = customFieldColors(),
              minLines = 2
            )
          }
        }

        Spacer(Modifier.height(16.dp))

        // Opcja czujnika zespolonego T-MAP
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Czujnik zespolony T-MAP (MAP + IAT 4-pin)",
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Zaznacz, jeśli czujnik zawiera w jednej obudowie zarówno pomiar ciśnienia MAP, jak i termistor temperatury powietrza dolotowego IAT.",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
              }
              Switch(
                checked = isComboTmapSensor,
                onCheckedChange = { checked ->
                  isComboTmapSensor = checked
                  includeResistanceChar = checked
                  pullupRequired = checked
                  if (checked && pinsList.none { it.name.contains("IAT", ignoreCase = true) || it.type == PinType.SIGNAL_IN }) {
                    pinsList.add(
                      PinDraft(
                        pinNumber = (pinsList.size + 1).toString(),
                        name = "IAT Sygnał (NTC)",
                        description = "Sygnał temperatury powietrza dolotowego do wejścia analogowego z pull-up 2490Ω",
                        wireColor = "Szary",
                        type = PinType.SIGNAL_IN,
                        connectorName = "Wtyczka A"
                      )
                    )
                  }
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = TunerCyan,
                  checkedTrackColor = TunerCyan.copy(alpha = 0.4f)
                )
              )
            }

            if (isComboTmapSensor) {
              Spacer(Modifier.height(14.dp))
              Text(
                text = "Zintegrowana krzywa termistora IAT (NTC):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TunerCyan
              )
              Spacer(Modifier.height(6.dp))

              OutlinedTextField(
                value = pullupValue,
                onValueChange = { pullupValue = it },
                label = { Text("Rezystor pull-up dla zintegrowanego IAT (np. 2490 Ω do +5V)") },
                modifier = Modifier.fillMaxWidth(),
                colors = customFieldColors(),
                singleLine = true
              )

              Spacer(Modifier.height(8.dp))

              resPointsList.forEachIndexed { rIdx, rpt ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  OutlinedTextField(
                    value = rpt.inVal,
                    onValueChange = { newVal -> resPointsList[rIdx] = rpt.copy(inVal = newVal) },
                    placeholder = { Text("np. 20") },
                    modifier = Modifier.weight(1f),
                    colors = customFieldColors(),
                    singleLine = true
                  )
                  OutlinedTextField(
                    value = rpt.outVal,
                    onValueChange = { newVal -> resPointsList[rIdx] = rpt.copy(outVal = newVal) },
                    placeholder = { Text("np. 2500") },
                    modifier = Modifier.weight(1.2f),
                    colors = customFieldColors(),
                    singleLine = true
                  )
                  IconButton(
                    onClick = { if (resPointsList.size > 1) resPointsList.removeAt(rIdx) },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = TuningRed, modifier = Modifier.size(18.dp))
                  }
                }
              }

              Spacer(Modifier.height(6.dp))

              OutlinedButton(
                onClick = { resPointsList.add(CalPointDraft(inVal = "", inUnit = "°C", outVal = "", outUnit = "Ω")) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dodaj punkt IAT (°C vs Ω)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }

      // ==========================================
      // KATEGORIA: FUEL_PRESSURE / OIL_PRESSURE
      // ==========================================
      else if (category == ComponentCategory.FUEL_PRESSURE || category == ComponentCategory.OIL_PRESSURE) {
        val pressType = if (category == ComponentCategory.FUEL_PRESSURE) "paliwa" else "oleju"
        SectionHeader(title = "${secCounter++}. Kalibracja liniowa ciśnienia $pressType (Bar vs V)", icon = Icons.Default.ShowChart)
        Spacer(Modifier.height(8.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Kalibracja liniowa ciśnienia $pressType (bar vs V)",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "Wpisz punkty charakterystyki ciśnienia w barach i odpowiadające im napięcia w woltach (np. 0.0 bar = 0.5V, 10.0 bar = 4.5V).",
              fontSize = 11.5.sp,
              color = TextSecondary,
              lineHeight = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            // Ustalanie jednostek
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = tableInputUnit,
                onValueChange = { tableInputUnit = it },
                label = { Text("Jedn. ciśnienia (np. bar)") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true
              )
              OutlinedTextField(
                value = tableOutputUnit,
                onValueChange = { tableOutputUnit = it },
                label = { Text("Jedn. napięcia (np. V)") },
                modifier = Modifier.weight(1f),
                colors = customFieldColors(),
                singleLine = true
              )
            }

            Spacer(Modifier.height(10.dp))

            // Nagłówek tabeli
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Ciśnienie [${tableInputUnit.ifBlank { "bar" }}]",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TunerCyan,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "Napięcie [${tableOutputUnit.ifBlank { "V" }}]",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TunerAmber,
                modifier = Modifier.weight(1f)
              )
              Spacer(Modifier.width(36.dp))
            }

            Spacer(Modifier.height(6.dp))

            calPointsList.forEachIndexed { cIdx, pt ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                OutlinedTextField(
                  value = pt.inVal,
                  onValueChange = { newVal -> calPointsList[cIdx] = pt.copy(inVal = newVal) },
                  placeholder = { Text("np. 0.0") },
                  modifier = Modifier.weight(1f),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TunerCyan,
                    unfocusedTextColor = TunerCyan,
                    focusedBorderColor = TunerCyan,
                    cursorColor = TunerCyan,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                  ),
                  singleLine = true
                )
                OutlinedTextField(
                  value = pt.outVal,
                  onValueChange = { newVal -> calPointsList[cIdx] = pt.copy(outVal = newVal) },
                  placeholder = { Text("np. 0.50") },
                  modifier = Modifier.weight(1f),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TunerAmber,
                    unfocusedTextColor = TunerAmber,
                    focusedBorderColor = TunerAmber,
                    cursorColor = TunerAmber,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                  ),
                  singleLine = true
                )
                IconButton(
                  onClick = { if (calPointsList.size > 1) calPointsList.removeAt(cIdx) },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Usuń punkt", tint = TuningRed, modifier = Modifier.size(18.dp))
                }
              }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
              onClick = { calPointsList.add(CalPointDraft(inVal = "", inUnit = tableInputUnit, outVal = "", outUnit = tableOutputUnit)) },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(6.dp))
              Text("Dodaj punkt kalibracji", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
              value = charNotes,
              onValueChange = { charNotes = it },
              label = { Text("Wzór kalibracji / zakres czujnika") },
              placeholder = { Text("np. Zakres 0-10 bar, 0.5V = 0 bar, 4.5V = 10 bar, gwint 1/8 NPT") },
              modifier = Modifier.fillMaxWidth(),
              colors = customFieldColors(),
              minLines = 2
            )
          }
        }

        // Opcja 2-w-1 ciśnienie + temperatura dla oleju
        if (category == ComponentCategory.OIL_PRESSURE) {
          Spacer(Modifier.height(16.dp))
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Czujnik zespolony 2-w-1 (Ciśnienie + Temp. oleju)",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                  Text(
                    text = "Zaznacz, jeśli czujnik oleju posiada dodatkowy pin z termistorem temperatury oleju (np. Bosch 0 261 230 340).",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                  )
                }
                Switch(
                  checked = isComboOilPressureTempSensor,
                  onCheckedChange = { checked ->
                    isComboOilPressureTempSensor = checked
                    includeResistanceChar = checked
                    pullupRequired = checked
                    if (checked && pinsList.none { it.name.contains("Temp", ignoreCase = true) || it.type == PinType.SIGNAL_IN }) {
                      pinsList.add(
                        PinDraft(
                          pinNumber = (pinsList.size + 1).toString(),
                          name = "Temp. oleju (NTC)",
                          description = "Sygnał temperatury oleju do wejścia analogowego z pull-up 2490Ω",
                          wireColor = "Żółty",
                          type = PinType.SIGNAL_IN,
                          connectorName = "Wtyczka A"
                        )
                      )
                    }
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = TunerCyan,
                    checkedTrackColor = TunerCyan.copy(alpha = 0.4f)
                  )
                )
              }

              if (isComboOilPressureTempSensor) {
                Spacer(Modifier.height(14.dp))
                Text(
                  text = "Zintegrowana krzywa temperatury oleju (NTC):",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = TunerCyan
                )
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                  value = pullupValue,
                  onValueChange = { pullupValue = it },
                  label = { Text("Rezystor pull-up dla temperatury oleju (np. 2490 Ω)") },
                  modifier = Modifier.fillMaxWidth(),
                  colors = customFieldColors(),
                  singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                resPointsList.forEachIndexed { rIdx, rpt ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    OutlinedTextField(
                      value = rpt.inVal,
                      onValueChange = { newVal -> resPointsList[rIdx] = rpt.copy(inVal = newVal) },
                      placeholder = { Text("np. 80") },
                      modifier = Modifier.weight(1f),
                      colors = customFieldColors(),
                      singleLine = true
                    )
                    OutlinedTextField(
                      value = rpt.outVal,
                      onValueChange = { newVal -> resPointsList[rIdx] = rpt.copy(outVal = newVal) },
                      placeholder = { Text("np. 320") },
                      modifier = Modifier.weight(1.2f),
                      colors = customFieldColors(),
                      singleLine = true
                    )
                    IconButton(
                      onClick = { if (resPointsList.size > 1) resPointsList.removeAt(rIdx) },
                      modifier = Modifier.size(36.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = TuningRed, modifier = Modifier.size(18.dp))
                    }
                  }
                }

                Spacer(Modifier.height(6.dp))

                OutlinedButton(
                  onClick = { resPointsList.add(CalPointDraft(inVal = "", inUnit = "°C", outVal = "", outUnit = "Ω")) },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Dodaj punkt temp. oleju (°C vs Ω)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }

      // ==========================================
      // KATEGORIA: LAMBDA (Sondy szerokopasmowe)
      // ==========================================
      else if (category == ComponentCategory.LAMBDA) {
        SectionHeader(title = "${secCounter++}. Kontroler szerokopasmowy i złącze LSU", icon = Icons.Default.Speed)
        Spacer(Modifier.height(8.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Speed, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Specyfika sondy szerokopasmowej LSU",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = TextPrimary
              )
            }
            Spacer(Modifier.height(8.dp))
            Text(
              text = "Sonda szerokopasmowa (np. Bosch LSU 4.9 lub 4.2) nie posiada uproszczonej krzywej napięcia 0-5V. Wymaga dedykowanego kontrolera z regulatorem prądu pompowania tlenu (Ip) oraz sterownikiem grzałki PID (np. wbudowany kontroler szerokopasmowy w EMU Black, moduł Lambda-to-CAN, Spartan 3 lub kontroler AEM). Pełny 6-pinowy układ połączeń został wygenerowany w Sekcji 3 powyżej.",
              fontSize = 12.sp,
              color = TextSecondary,
              lineHeight = 17.sp
            )
          }
        }
      }

      // ==========================================
      // KATEGORIA: ECU / MODULES (Sterowniki standalone i moduły CAN)
      // ==========================================
      else if (category == ComponentCategory.ECU || category == ComponentCategory.MODULES) {
        SectionHeader(title = "${secCounter++}. Konfiguracja magistrali i modułu", icon = Icons.Default.Info)
        Spacer(Modifier.height(8.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Zarządzanie połączeniami sterownika standalone",
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp,
              color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = "Dla sterowników silnika i modułów magistrali przypisuj dedykowane gniazda i wtyczki (np. Wtyczka A, Wtyczka B) z poszczególnymi typami pinów (zasilania, masy, wyjścia wtryskiwaczy, wyjścia zapłonu, magistrala CAN-Bus) bezpośrednio w Sekcji 3.",
              fontSize = 12.sp,
              color = TextSecondary,
              lineHeight = 17.sp
            )
          }
        }
      }

      // ==========================================
      // KATEGORIA: OTHER (Inne niestandardowe podzespoły)
      // ==========================================
      else {
        SectionHeader(title = "${secCounter++}. Opcjonalne charakterystyki podzespołu", icon = Icons.Default.ShowChart)
        Spacer(Modifier.height(8.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Dostosuj tabele do niestandardowego urządzenia:",
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))

            // Przełącznik kalibracji liniowej
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Tabela kalibracji (np. napięcie vs wyjście)", fontSize = 12.5.sp, color = TextPrimary)
              Switch(
                checked = showGenericOtherCalibration,
                onCheckedChange = { showGenericOtherCalibration = it },
                colors = SwitchDefaults.colors(checkedThumbColor = TunerCyan, checkedTrackColor = TunerCyan.copy(alpha = 0.4f))
              )
            }

            if (showGenericOtherCalibration) {
              Spacer(Modifier.height(8.dp))
              calPointsList.forEachIndexed { cIdx, pt ->
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  OutlinedTextField(
                    value = pt.inVal,
                    onValueChange = { newVal -> calPointsList[cIdx] = pt.copy(inVal = newVal) },
                    label = { Text("Wartość") },
                    modifier = Modifier.weight(1f),
                    colors = customFieldColors(),
                    singleLine = true
                  )
                  OutlinedTextField(
                    value = pt.outVal,
                    onValueChange = { newVal -> calPointsList[cIdx] = pt.copy(outVal = newVal) },
                    label = { Text("Sygnał [V]") },
                    modifier = Modifier.weight(1f),
                    colors = customFieldColors(),
                    singleLine = true
                  )
                  IconButton(onClick = { if (calPointsList.size > 1) calPointsList.removeAt(cIdx) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = TuningRed, modifier = Modifier.size(18.dp))
                  }
                }
              }
              OutlinedButton(
                onClick = { calPointsList.add(CalPointDraft(inVal = "", inUnit = "", outVal = "", outUnit = "V")) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TunerCyan),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dodaj punkt kalibracji", fontSize = 12.sp)
              }
            }

            Spacer(Modifier.height(14.dp))

            // Przełącznik NTC / Pull-up
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Rezystor Pull-up i termistor NTC", fontSize = 12.5.sp, color = TextPrimary)
              Switch(
                checked = showGenericOtherResistance,
                onCheckedChange = {
                  showGenericOtherResistance = it
                  showGenericOtherPullup = it
                  pullupRequired = it
                  includeResistanceChar = it
                },
                colors = SwitchDefaults.colors(checkedThumbColor = TunerAmber, checkedTrackColor = TunerAmber.copy(alpha = 0.4f))
              )
            }

            if (showGenericOtherResistance) {
              Spacer(Modifier.height(8.dp))
              OutlinedTextField(
                value = pullupValue,
                onValueChange = { pullupValue = it },
                label = { Text("Wartość rezystora pull-up") },
                modifier = Modifier.fillMaxWidth(),
                colors = customFieldColors(),
                singleLine = true
              )
            }
          }
        }
      }

      Spacer(Modifier.height(20.dp))

      // ==========================================
      // WAŻNE INFORMACJE I OSTRZEŻENIA DLA TUNERA
      // ==========================================
      val placeholderNotes = when (category) {
        ComponentCategory.THROTTLE -> "np. Częstotliwość PWM silnika DBW (1000 Hz w EMU), napięcia krańcowe TPS1 (0.5V-4.5V) i TPS2 (4.5V-0.5V)..."
        ComponentCategory.INJECTORS -> "np. Rezystancja cewki wtryskiwacza (High-Z 12-16 Ohm), kąt stożka rozpylania paliwa, kompatybilność z E85/metanolem..."
        ComponentCategory.COILS -> "np. Maksymalny dopuszczalny dwell time przy 14V (2.4 ms). Podłączyć masę iskry bezpośrednio do głowicy silnika!"
        ComponentCategory.TEMP_SENSOR -> "np. Gwint montażowy (M12x1.5), typ NTC, czas odpowiedzi termistora, moment dokręcenia (max 15 Nm)..."
        ComponentCategory.MAP_SENSOR -> "np. Montaż króćcem do dołu aby uniknąć zalewania olejem z dolotu, zakres ciśnienia doładowania..."
        ComponentCategory.FUEL_PRESSURE, ComponentCategory.OIL_PRESSURE -> "np. Gwint 1/8 NPT lub M10x1, moment dokręcenia, dopuszczalny zakres temperatur oleju..."
        ComponentCategory.LAMBDA -> "np. Kąt montażu w wydechu min. 10-15° ponad poziom, odległość min. 60-80cm od turbosprężarki, nie stosować paliwa z ołowiem..."
        ComponentCategory.ECU, ComponentCategory.MODULES -> "np. Wersja firmware, rezystor terminujący CAN 120Ω, zalecany bezpiecznik główny 15A..."
        else -> "Wpisz wszystko co tuner musi wiedzieć montując ten element..."
      }

      SectionHeader(title = "${secCounter++}. Ważne informacje i ostrzeżenia dla tunera", icon = Icons.Default.Info)
      Spacer(Modifier.height(8.dp))

      OutlinedTextField(
        value = importantNotes,
        onValueChange = { importantNotes = it },
        label = { Text("Uwagi montażowe / typ wtyczki / parametry pracy") },
        placeholder = { Text(placeholderNotes) },
        modifier = Modifier.fillMaxWidth(),
        colors = customFieldColors(),
        minLines = 3
      )


      Spacer(Modifier.height(14.dp))

      Text(
        text = if (isEditing) "Wpis zostanie zaktualizowany przez: @$userNick" else "Wpis zostanie dodany pod Twoim nickiem: @$userNick",
        fontSize = 12.sp,
        color = TunerCyan,
        fontWeight = FontWeight.SemiBold
      )

      if (isEditing && onDelete != null) {
        Spacer(Modifier.height(28.dp))
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1313)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B2020)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = TuningRed,
                modifier = Modifier.size(20.dp)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Strefa niebezpieczna",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TuningRed
              )
            }
            Spacer(Modifier.height(6.dp))
            Text(
              text = "Możesz trwale usunąć ten wpis z bazy danych aplikacji.",
              fontSize = 12.sp,
              color = TextMuted
            )
            Spacer(Modifier.height(12.dp))
            Button(
              onClick = { showDeleteConfirmDialog = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = TuningRed,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_component_button")
            ) {
              Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("Usuń ten wpis z bazy", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      Spacer(Modifier.height(32.dp))
    }
  }

  if (showDeleteConfirmDialog && onDelete != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      containerColor = DarkSurfaceElevated,
      icon = {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = null,
          tint = TuningRed,
          modifier = Modifier.size(32.dp)
        )
      },
      title = {
        Text(
          text = "Usunąć ten wpis?",
          color = TextPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      },
      text = {
        Text(
          text = "Czy na pewno chcesz bezpowrotnie usunąć wpis „${name.ifBlank { "Wybrany element" }}” z bazy czujników i sterowników?",
          color = TextSecondary,
          fontSize = 14.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmDialog = false
            onDelete()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = TuningRed,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("confirm_delete_button")
        ) {
          Text("Usuń trwale", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDeleteConfirmDialog = false },
          modifier = Modifier.testTag("cancel_delete_button")
        ) {
          Text("Anuluj", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = null, tint = TunerCyan, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
  }
}

@Composable
private fun customFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = TuningRed,
  unfocusedBorderColor = BorderStroke,
  focusedContainerColor = DarkSurfaceElevated,
  unfocusedContainerColor = DarkSurfaceElevated,
  focusedTextColor = TextPrimary,
  unfocusedTextColor = TextPrimary,
  focusedLabelColor = TuningRed,
  unfocusedLabelColor = TextSecondary
)
