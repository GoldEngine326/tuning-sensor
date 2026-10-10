package com.example.model

enum class ComponentCategory(val displayName: String) {
  ALL("Wszystkie"),
  MAP_SENSOR("MAP Sensory"),
  COILS("Cewki zapłonowe"),
  TEMP_SENSOR("Czujniki temp. (IAT/CLT)"),
  ECU("Sterowniki ECU"),
  MODULES("Moduły"),
  INJECTORS("Wtryskiwacze"),
  LAMBDA("Sondy Lambda"),
  PRESSURE_TEMP("Ciśnienie / Olej"),
  OTHER("Inne")
}

enum class PinType(val label: String, val colorHex: Long) {
  POWER("+12V / Zasilanie", 0xFFFF3B30),
  GROUND("Masa (GND)", 0xFF8E8E93),
  SENSOR_GND("Masa czujników (Sensor GND)", 0xFF636366),
  VREF("+5V Ref / Zasilanie czujnika", 0xFFFF9500),
  SIGNAL_OUT("Sygnał wyjściowy (Analog/PWM)", 0xFF00C7BE),
  SIGNAL_IN("Wejście sygnału / Trigger", 0xFF30B0C7),
  COMMUNICATION("Magistrala (CAN / Serial)", 0xFFAF52DE),
  INJECTOR("Sterowanie wtryskiem (Low Side)", 0xFFFF2D55),
  IGNITION("Sterowanie zapłonem (Trigger 5V)", 0xFFFF9F0A),
  HEATER("Grzałka", 0xFFFF453A),
  OTHER("Inny", 0xFF6C757D)
}

data class PinInfo(
  val pinNumber: String,
  val name: String,
  val description: String,
  val wireColor: String = "",
  val type: PinType = PinType.OTHER,
  val connectorName: String = ""
)

enum class CharType(val displayName: String) {
  VOLTAGE_PRESSURE("Napięcie vs Ciśnienie"),
  RESISTANCE_TEMP("Rezystancja vs Temperatura (NTC)"),
  DWELL_TIME("Czas ładowania cewki (Dwell) vs Napięcie"),
  ECU_SPEC("Specyfikacja wejść / wyjść ECU"),
  OTHER("Charakterystyka ogólna")
}

data class CalibrationPoint(
  val inputValue: String,
  val inputUnit: String,
  val outputValue: String,
  val outputUnit: String
)

data class CharacteristicInfo(
  val type: CharType,
  val title: String,
  val inputLabel: String,
  val outputLabel: String,
  val points: List<CalibrationPoint>,
  val formulaOrNotes: String = ""
)

data class DeadTimePoint(
  val voltage: String,
  val timeMs: String
)

data class DeadTimeTable(
  val tableName: String, // np. "3.0 bar (Diff)", "4.0 bar (Diff)"
  val pressureUnit: String = "bar",
  val points: List<DeadTimePoint> = emptyList(),
  val notes: String = ""
)

data class TuningComponent(
  val id: String,
  val name: String,
  val partNumber: String,
  val category: ComponentCategory,
  val shortDescription: String,
  val imageUrl: String = "",
  val images: List<String> = emptyList(),
  val vectorIllustrationKey: String = "", // e.g. "vw_coils", "map_3b", "uaefi", "bosch_iat", "lsu_49"
  val pinout: List<PinInfo>,
  val characteristic: CharacteristicInfo,
  val resistanceCharacteristic: CharacteristicInfo? = null,
  val pullupRequired: Boolean = false,
  val pullupRecommendedValue: String = "2.49 kΩ do +5V VREF",
  val pullupNote: String = "",
  val importantNotes: String = "",
  val authorNick: String = "Społeczność Tunerów",
  val authorId: String = "community",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val lastEditedByNick: String? = null,
  val lastEditedAtMillis: Long? = null,
  val rating: Float = 5.0f,
  val ratingVotesCount: Int = 1,
  val userVote: Int? = null, // 1 to 5 if current user voted
  val isUserAdded: Boolean = false,
  val connectorImageUrl: String = "",
  val deadTimeTables: List<DeadTimeTable> = emptyList()
) {
  fun allImages(): List<String> {
    return if (images.isNotEmpty()) images else if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList()
  }

  val primaryImage: String
    get() = images.firstOrNull() ?: imageUrl
}
