package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.DefaultTuningData
import com.example.data.TuningRepository
import com.example.model.CalibrationPoint
import com.example.model.CharType
import com.example.model.CharacteristicInfo
import com.example.model.ComponentCategory
import com.example.model.PinInfo
import com.example.model.PinType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest : FirestoreEmulatorTestBase() {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tuning Sensor", appName)
  }

  @Test
  fun `verify default sensors loaded including VW coils and UAEFI`() = runBlocking {
    signInTestUser("test1@example.com")
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TuningRepository(firestore, context)
    val items = DefaultTuningData.getInitialComponents()

    assertTrue("Powinno być załadowane co najmniej 5 wstępnych czujników", items.size >= 5)

    val vwCoils = items.find { it.id == "vw_red_coils_r8" }
    assertNotNull("Cewki czerwone z VW powinny być w bazie", vwCoils)
    assertEquals(4, vwCoils?.pinout?.size)

    val mapSensor = items.find { it.id == "vw_mapsensor_3bar" }
    assertNotNull("MAP sensor 3 bar powinien być w bazie", mapSensor)
    assertTrue("MAP sensor ma zintegrowany czujnik temp IAT wymagający pullupa", mapSensor?.pullupRequired == true)
    assertNotNull("MAP sensor powinien mieć charakterystykę opornościową NTC", mapSensor?.resistanceCharacteristic)

    val uaefi = items.find { it.id == "ecu_uaefi_standalone" }
    assertNotNull("Pinout sterownika UAEFI powinien być w bazie", uaefi)
    assertTrue(uaefi?.pinout?.size ?: 0 >= 20)

    repo.stopListening()
  }

  @Test
  fun `add custom component and rate it`() = runBlocking {
    signInTestUser("test2@example.com")
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TuningRepository(firestore, context)

    repo.setUserNick("Bartek_18T")
    assertEquals("Bartek_18T", repo.userNick.value)

    val added = repo.addComponent(
      name = "Czujnik Halla BMW",
      partNumber = "12141703277",
      category = ComponentCategory.OTHER,
      shortDescription = "Czujnik wałka rozrządu",
      imageUrl = "",
      pinout = listOf(
        PinInfo("Pin 1", "Zasilanie +12V", "Zasilanie po stacyjce", "Czerwony", PinType.POWER),
        PinInfo("Pin 2", "Sygnał Hall", "Sygnał prostokątny 0-5V", "Żółty", PinType.SIGNAL_OUT),
        PinInfo("Pin 3", "Masa GND", "Masa sensorów", "Brązowy", PinType.SENSOR_GND)
      ),
      characteristic = CharacteristicInfo(
        type = CharType.OTHER,
        title = "Sygnał prostokątny Hall",
        inputLabel = "Ząb",
        outputLabel = "Napięcie",
        points = listOf(CalibrationPoint("Metal", "", "0.2", "V"), CalibrationPoint("Szczelina", "", "5.0", "V")),
        formulaOrNotes = "Sygnał logiczny"
      ),
      resistanceCharacteristic = null,
      pullupRequired = true,
      pullupRecommendedValue = "1.0 kΩ",
      pullupNote = "Wymagany rezystor podciągający do 5V",
      importantNotes = "Moment 10Nm"
    )

    assertEquals("Bartek_18T", added.authorNick)
    assertEquals(ComponentCategory.OTHER, added.category)

    repo.stopListening()
  }
}
