package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.TuningRepository
import com.example.model.CalibrationPoint
import com.example.model.CharType
import com.example.model.CharacteristicInfo
import com.example.model.ComponentCategory
import com.example.model.PinInfo
import com.example.model.PinType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testCreateAndFetchSensor() = runBlocking {
    signInTestUser("alice@example.com")
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repository = TuningRepository(firestore, context)

    val added = repository.addComponent(
      name = "Test MAP Sensor 4 Bar",
      partNumber = "0281002576",
      category = ComponentCategory.MAP_SENSOR,
      shortDescription = "4 bar MAP sensor for high boost",
      imageUrl = "",
      images = emptyList(),
      pinout = listOf(
        PinInfo("Pin 1", "Masa", "GND", "Czarny", PinType.SENSOR_GND),
        PinInfo("Pin 2", "+5V", "Zasilanie", "Czerwony", PinType.VREF),
        PinInfo("Pin 3", "Signal", "Sygnał 0.5-4.5V", "Niebieski", PinType.SIGNAL_OUT)
      ),
      characteristic = CharacteristicInfo(
        type = CharType.VOLTAGE_PRESSURE,
        title = "Krzywa kalibracji",
        inputLabel = "Napięcie",
        outputLabel = "Ciśnienie",
        points = listOf(
          CalibrationPoint("0.5", "V", "20", "kPa"),
          CalibrationPoint("4.5", "V", "400", "kPa")
        ),
        formulaOrNotes = "Liniowa charakterystyka"
      ),
      resistanceCharacteristic = null,
      pullupRequired = false,
      pullupRecommendedValue = "",
      pullupNote = "",
      importantNotes = "Płaski montaż w kolektorze"
    )

    assertNotNull(added.id)
    assertEquals("Test MAP Sensor 4 Bar", added.name)
    assertEquals(ComponentCategory.MAP_SENSOR, added.category)
    assertTrue(added.isUserAdded)

    repository.stopListening()
  }

  @Test
  fun testMultiImagesAndPrimaryImage() = runBlocking {
    signInTestUser("bob@example.com")
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repository = TuningRepository(firestore, context)

    val photo1 = "data:image/jpeg;base64,/9j/photo1dummy"
    val photo2 = "data:image/jpeg;base64,/9j/photo2dummy"
    val photoList = listOf(photo1, photo2)

    val added = repository.addComponent(
      name = "Sensor with Multi Photos",
      partNumber = "BOSCH-123",
      category = ComponentCategory.COILS,
      shortDescription = "Test multi photos",
      imageUrl = photo1,
      images = photoList,
      pinout = emptyList(),
      characteristic = CharacteristicInfo(
        type = CharType.DWELL_TIME,
        title = "Dwell",
        inputLabel = "V",
        outputLabel = "ms",
        points = emptyList()
      ),
      resistanceCharacteristic = null,
      pullupRequired = false,
      pullupRecommendedValue = "",
      pullupNote = "",
      importantNotes = ""
    )

    assertEquals(2, added.images.size)
    assertEquals(photo1, added.primaryImage)
    assertEquals(photo1, added.images[0])
    assertEquals(photo2, added.images[1])

    repository.stopListening()
  }

  @Test
  fun testUserNicknamePersistence() = runBlocking {
    val uid = signInTestUser("tuner_pro@example.com")
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repository = TuningRepository(firestore, context)

    repository.setUserNick("MasterTuner99")
    assertEquals("MasterTuner99", repository.userNick.value)

    // Verify written to Firestore users collection
    val doc = firestore.collection("users").document(uid).get().await()
    assertTrue(doc.exists())
    assertEquals("MasterTuner99", doc.getString("nick"))

    repository.stopListening()
  }
}
