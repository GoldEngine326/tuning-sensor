package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.R
import com.example.model.CalibrationPoint
import com.example.model.CharType
import com.example.model.CharacteristicInfo
import com.example.model.ComponentCategory
import com.example.model.DeadTimePoint
import com.example.model.DeadTimeTable
import com.example.model.PinInfo
import com.example.model.PinType
import com.example.model.TuningComponent
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import com.example.util.ImageUtils
import java.util.UUID

class TuningRepository(
  private val db: FirebaseFirestore,
  private val context: Context
) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    context.applicationContext
  )

  private val prefs: SharedPreferences =
    context.getSharedPreferences("tuning_sensor_prefs", Context.MODE_PRIVATE)

  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  private val _components = MutableStateFlow<List<TuningComponent>>(emptyList())
  val components: StateFlow<List<TuningComponent>> = _components.asStateFlow()

  private val _userNick = MutableStateFlow(getUserNickInternal())
  val userNick: StateFlow<String> = _userNick.asStateFlow()

  private var snapshotListener: ListenerRegistration? = null

  init {
    startListeningToSensors()
  }

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun startListeningToSensors() {
    snapshotListener?.remove()
    val uid = auth.currentUser?.uid
    if (uid != null) {
      syncUserNickFromCloud(uid)
    }

    val sensorsPath = "sensors"
    snapshotListener = db.collection(sensorsPath)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, sensorsPath)
          return@addSnapshotListener
        }

        if (snapshot != null) {
          if (snapshot.isEmpty) {
            // First time seeding default tuning components
            seedInitialComponentsIfEmpty()
          } else {
            val votesJson = prefs.getString("user_votes_json", "{}") ?: "{}"
            val votesObj = try { JSONObject(votesJson) } catch (_: Exception) { JSONObject() }

            val list = snapshot.documents.mapNotNull { doc ->
              try {
                val data = doc.data ?: return@mapNotNull null
                val userVote = if (votesObj.has(doc.id)) votesObj.getInt(doc.id) else null
                parseComponentFromFirestore(doc.id, data, userVote)
              } catch (e: Exception) {
                Log.e("TuningRepository", "Error parsing sensor ${doc.id}", e)
                null
              }
            }
            _components.value = list
          }
        }
      }
  }

  fun stopListening() {
    snapshotListener?.remove()
    snapshotListener = null
  }

  private fun seedInitialComponentsIfEmpty() {
    val uid = auth.currentUser?.uid ?: return
    val initialList = DefaultTuningData.getInitialComponents()
    val sensorsRef = db.collection("sensors")

    for (comp in initialList) {
      val payload = createFirestorePayload(
        component = comp.copy(authorId = uid),
        authorId = uid,
        isCreate = true
      )
      sensorsRef.document(comp.id).set(payload)
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "sensors/${comp.id}")
        }
    }
  }

  private fun getUserNickInternal(): String {
    val saved = prefs.getString("user_nick", "") ?: ""
    if (saved.isNotBlank()) return saved
    val defaultNick = "Tuner_${(100..999).random()}"
    prefs.edit().putString("user_nick", defaultNick).apply()
    return defaultNick
  }

  /**
   * Fetches the user's custom nickname from Firestore users/{uid}.
   * If found, sets it locally so that upon logging in with Google,
   * changing devices, or reinstalling the app, the nickname is always preserved.
   */
  fun syncUserNickFromCloud(uid: String) {
    db.collection("users").document(uid).get()
      .addOnSuccessListener { doc ->
        if (doc != null && doc.exists()) {
          val cloudNick = doc.getString("nick")
          if (!cloudNick.isNullOrBlank()) {
            _userNick.value = cloudNick
            prefs.edit().putString("user_nick", cloudNick).apply()
            Log.d("TuningRepository", "Synchronized nick from cloud: $cloudNick")
            return@addOnSuccessListener
          }
        }
        // If not in cloud yet, persist current nick or Google display name to cloud
        val current = _userNick.value.ifBlank {
          auth.currentUser?.displayName?.take(24)?.trim() ?: getUserNickInternal()
        }
        setUserNick(current)
      }
      .addOnFailureListener { e ->
        Log.w("TuningRepository", "Failed to sync user nick from cloud", e)
      }
  }

  /**
   * Sets user nickname locally in SharedPreferences AND saves it in Firestore under users/{uid}.
   */
  fun setUserNick(newNick: String) {
    val cleaned = newNick.trim().take(24).ifBlank { "Tuner_${(100..999).random()}" }
    prefs.edit().putString("user_nick", cleaned).apply()
    _userNick.value = cleaned

    val uid = auth.currentUser?.uid
    if (uid != null) {
      val userPayload = mapOf(
        "userId" to uid,
        "nick" to cleaned,
        "email" to (auth.currentUser?.email ?: ""),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      db.collection("users").document(uid).set(userPayload, SetOptions.merge())
        .addOnSuccessListener {
          Log.d("TuningRepository", "Successfully persisted nickname @$cleaned to users/$uid in Firestore")
        }
        .addOnFailureListener { e ->
          Log.w("TuningRepository", "Failed to persist nick to Firestore users/$uid", e)
        }
    }
  }

  fun addComponent(
    name: String,
    partNumber: String,
    category: ComponentCategory,
    shortDescription: String,
    imageUrl: String = "",
    images: List<String> = emptyList(),
    pinout: List<PinInfo>,
    characteristic: CharacteristicInfo,
    resistanceCharacteristic: CharacteristicInfo? = null,
    pullupRequired: Boolean,
    pullupRecommendedValue: String,
    pullupNote: String,
    importantNotes: String,
    connectorImageUrl: String = "",
    deadTimeTables: List<DeadTimeTable> = emptyList(),
    flowRateCc: Double? = null,
    flowPressureBar: Double? = null,
    innerDiameterMm: Double? = null,
    outerDiameterMm: Double? = null,
    boltSpacingWidthMm: Double? = null,
    boltSpacingHeightMm: Double? = null,
    boltDiagonal1Mm: Double? = null,
    boltDiagonal2Mm: Double? = null,
    injectorImpedance: String = "",
    ecuOutputsCount: String = "",
    ecuBoardDimensions: String = ""
  ): TuningComponent {
    val uid = requireUserId()
    val newId = "sensor_" + UUID.randomUUID().toString().replace("-", "").take(12)
    val author = _userNick.value

    val finalImages = if (images.isNotEmpty()) images else if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList()
    val finalPrimaryImage = finalImages.firstOrNull() ?: imageUrl.trim()

    val newComp = TuningComponent(
      id = newId,
      name = name.trim(),
      partNumber = partNumber.trim(),
      category = category,
      shortDescription = shortDescription.trim(),
      imageUrl = finalPrimaryImage,
      images = finalImages,
      connectorImageUrl = connectorImageUrl.trim(),
      vectorIllustrationKey = when (category) {
        ComponentCategory.COILS -> "vw_coils"
        ComponentCategory.MAP_SENSOR -> "map_3b"
        ComponentCategory.ECU -> "uaefi"
        ComponentCategory.MODULES -> "uaefi"
        ComponentCategory.INJECTORS -> "vw_coils"
        ComponentCategory.TEMP_SENSOR -> "bosch_iat"
        ComponentCategory.LAMBDA -> "lsu_49"
        ComponentCategory.THROTTLE -> "throttle"
        ComponentCategory.FUEL_PRESSURE -> "map_3b"
        ComponentCategory.OIL_PRESSURE -> "map_3b"
        else -> "map_3b"
      },
      pinout = pinout,
      characteristic = characteristic,
      resistanceCharacteristic = resistanceCharacteristic,
      pullupRequired = pullupRequired,
      pullupRecommendedValue = pullupRecommendedValue.trim(),
      pullupNote = pullupNote.trim(),
      importantNotes = importantNotes.trim(),
      authorNick = author,
      authorId = uid,
      createdAtMillis = System.currentTimeMillis(),
      rating = 5.0f,
      ratingVotesCount = 1,
      userVote = 5,
      isUserAdded = true,
      deadTimeTables = deadTimeTables,
      flowRateCc = flowRateCc,
      flowPressureBar = flowPressureBar,
      innerDiameterMm = innerDiameterMm,
      outerDiameterMm = outerDiameterMm,
      boltSpacingWidthMm = boltSpacingWidthMm,
      boltSpacingHeightMm = boltSpacingHeightMm,
      boltDiagonal1Mm = boltDiagonal1Mm,
      boltDiagonal2Mm = boltDiagonal2Mm,
      injectorImpedance = injectorImpedance.trim(),
      ecuOutputsCount = ecuOutputsCount.trim(),
      ecuBoardDimensions = ecuBoardDimensions.trim()
    )

    val payload = createFirestorePayload(newComp, uid, isCreate = true)
    val path = "sensors/$newId"

    db.collection("sensors").document(newId).set(payload)
      .addOnFailureListener { exception ->
        handleFirestoreError(exception, OperationType.CREATE, path)
      }

    _components.value = listOf(newComp) + _components.value.filter { it.id != newId }
    recordVote(newId, 5)
    return newComp
  }

  fun updateComponent(
    id: String,
    name: String,
    partNumber: String,
    category: ComponentCategory,
    shortDescription: String,
    imageUrl: String = "",
    images: List<String> = emptyList(),
    pinout: List<PinInfo>,
    characteristic: CharacteristicInfo,
    resistanceCharacteristic: CharacteristicInfo? = null,
    pullupRequired: Boolean,
    pullupRecommendedValue: String,
    pullupNote: String,
    importantNotes: String,
    connectorImageUrl: String = "",
    deadTimeTables: List<DeadTimeTable> = emptyList(),
    flowRateCc: Double? = null,
    flowPressureBar: Double? = null,
    innerDiameterMm: Double? = null,
    outerDiameterMm: Double? = null,
    boltSpacingWidthMm: Double? = null,
    boltSpacingHeightMm: Double? = null,
    boltDiagonal1Mm: Double? = null,
    boltDiagonal2Mm: Double? = null,
    injectorImpedance: String = "",
    ecuOutputsCount: String = "",
    ecuBoardDimensions: String = ""
  ): TuningComponent? {
    requireUserId()
    val existing = _components.value.find { it.id == id } ?: return null
    val editorNick = _userNick.value

    val finalImages = if (images.isNotEmpty()) images else if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList()
    val finalPrimaryImage = finalImages.firstOrNull() ?: imageUrl.trim()

    val updatedComp = existing.copy(
      name = name.trim(),
      partNumber = partNumber.trim(),
      category = category,
      shortDescription = shortDescription.trim(),
      imageUrl = finalPrimaryImage,
      images = finalImages,
      connectorImageUrl = connectorImageUrl.trim(),
      pinout = pinout,
      characteristic = characteristic,
      resistanceCharacteristic = resistanceCharacteristic,
      pullupRequired = pullupRequired,
      pullupRecommendedValue = pullupRecommendedValue.trim(),
      pullupNote = pullupNote.trim(),
      importantNotes = importantNotes.trim(),
      lastEditedByNick = editorNick,
      lastEditedAtMillis = System.currentTimeMillis(),
      deadTimeTables = deadTimeTables,
      flowRateCc = flowRateCc,
      flowPressureBar = flowPressureBar,
      innerDiameterMm = innerDiameterMm,
      outerDiameterMm = outerDiameterMm,
      boltSpacingWidthMm = boltSpacingWidthMm,
      boltSpacingHeightMm = boltSpacingHeightMm,
      boltDiagonal1Mm = boltDiagonal1Mm,
      boltDiagonal2Mm = boltDiagonal2Mm,
      injectorImpedance = injectorImpedance.trim(),
      ecuOutputsCount = ecuOutputsCount.trim(),
      ecuBoardDimensions = ecuBoardDimensions.trim()
    )

    _components.value = _components.value.map {
      if (it.id == id) updatedComp else it
    }

    val path = "sensors/$id"
    val updatePayload = mutableMapOf<String, Any>(
      "name" to updatedComp.name,
      "partNumber" to updatedComp.partNumber,
      "category" to updatedComp.category.name,
      "shortDescription" to updatedComp.shortDescription,
      "imageUrl" to updatedComp.imageUrl,
      "images" to updatedComp.images,
      "connectorImageUrl" to updatedComp.connectorImageUrl,
      "vectorIllustrationKey" to updatedComp.vectorIllustrationKey,
      "pins" to serializePins(updatedComp.pinout),
      "characteristic" to serializeCharacteristic(updatedComp.characteristic),
      "pullupRequired" to updatedComp.pullupRequired,
      "pullupRecommendedValue" to updatedComp.pullupRecommendedValue,
      "pullupNote" to updatedComp.pullupNote,
      "importantNotes" to updatedComp.importantNotes,
      "authorNick" to updatedComp.authorNick,
      "lastEditedByNick" to editorNick,
      "lastEditedAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
      "rating" to updatedComp.rating.toDouble(),
      "ratingVotesCount" to updatedComp.ratingVotesCount,
      "isUserAdded" to updatedComp.isUserAdded
    )

    if (resistanceCharacteristic != null) {
      updatePayload["resistanceCharacteristic"] = serializeCharacteristic(resistanceCharacteristic)
    }

    if (deadTimeTables.isNotEmpty()) {
      updatePayload["deadTimeTables"] = serializeDeadTimeTables(deadTimeTables)
    }

    if (updatedComp.flowRateCc != null) {
      updatePayload["flowRateCc"] = updatedComp.flowRateCc
    }
    if (updatedComp.flowPressureBar != null) {
      updatePayload["flowPressureBar"] = updatedComp.flowPressureBar
    }
    if (updatedComp.innerDiameterMm != null) {
      updatePayload["innerDiameterMm"] = updatedComp.innerDiameterMm
    }
    if (updatedComp.outerDiameterMm != null) {
      updatePayload["outerDiameterMm"] = updatedComp.outerDiameterMm
    }
    if (updatedComp.boltSpacingWidthMm != null) {
      updatePayload["boltSpacingWidthMm"] = updatedComp.boltSpacingWidthMm
    }
    if (updatedComp.boltSpacingHeightMm != null) {
      updatePayload["boltSpacingHeightMm"] = updatedComp.boltSpacingHeightMm
    }
    if (updatedComp.boltDiagonal1Mm != null) {
      updatePayload["boltDiagonal1Mm"] = updatedComp.boltDiagonal1Mm
    }
    if (updatedComp.boltDiagonal2Mm != null) {
      updatePayload["boltDiagonal2Mm"] = updatedComp.boltDiagonal2Mm
    }
    if (updatedComp.injectorImpedance.isNotBlank()) {
      updatePayload["injectorImpedance"] = updatedComp.injectorImpedance
    }
    if (updatedComp.ecuOutputsCount.isNotBlank()) {
      updatePayload["ecuOutputsCount"] = updatedComp.ecuOutputsCount
    }
    if (updatedComp.ecuBoardDimensions.isNotBlank()) {
      updatePayload["ecuBoardDimensions"] = updatedComp.ecuBoardDimensions
    }

    db.collection("sensors").document(id).update(updatePayload)
      .addOnFailureListener { exception ->
        handleFirestoreError(exception, OperationType.UPDATE, path)
      }

    return updatedComp
  }

  fun deleteComponent(id: String): Boolean {
    requireUserId()
    val path = "sensors/$id"
    _components.value = _components.value.filter { it.id != id }
    db.collection("sensors").document(id).delete()
      .addOnFailureListener { exception ->
        handleFirestoreError(exception, OperationType.DELETE, path)
      }
    return true
  }

  fun voteComponent(componentId: String, score: Int) {
    requireUserId()
    val safeScore = score.coerceIn(1, 5)
    val comp = _components.value.find { it.id == componentId } ?: return

    val previousVote = comp.userVote
    val newVoteCount = if (previousVote == null) comp.ratingVotesCount + 1 else comp.ratingVotesCount
    val totalScore = if (previousVote == null) {
      (comp.rating * comp.ratingVotesCount) + safeScore
    } else {
      (comp.rating * comp.ratingVotesCount) - previousVote + safeScore
    }
    val newAvg = (totalScore / newVoteCount).coerceIn(1.0f, 5.0f)

    recordVote(componentId, safeScore)

    val path = "sensors/$componentId"
    db.collection("sensors").document(componentId).update(
      mapOf(
        "rating" to newAvg.toDouble(),
        "ratingVotesCount" to newVoteCount,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).addOnFailureListener { exception ->
      handleFirestoreError(exception, OperationType.UPDATE, path)
    }
  }

  private fun recordVote(componentId: String, score: Int) {
    val votesJson = prefs.getString("user_votes_json", "{}") ?: "{}"
    val votesObj = try { JSONObject(votesJson) } catch (_: Exception) { JSONObject() }
    votesObj.put(componentId, score)
    prefs.edit().putString("user_votes_json", votesObj.toString()).apply()
  }

  private fun createFirestorePayload(
    component: TuningComponent,
    authorId: String,
    isCreate: Boolean
  ): Map<String, Any> {
    val allImagesList = if (component.images.isNotEmpty()) {
      component.images
    } else if (component.imageUrl.isNotBlank()) {
      listOf(component.imageUrl)
    } else {
      emptyList()
    }

    val primaryImg = allImagesList.firstOrNull() ?: component.imageUrl

    val map = mutableMapOf<String, Any>(
      "id" to component.id,
      "name" to component.name,
      "partNumber" to component.partNumber,
      "category" to component.category.name,
      "shortDescription" to component.shortDescription,
      "imageUrl" to primaryImg,
      "images" to allImagesList,
      "vectorIllustrationKey" to component.vectorIllustrationKey,
      "authorNick" to component.authorNick,
      "authorId" to authorId,
      "rating" to component.rating.toDouble(),
      "ratingVotesCount" to component.ratingVotesCount,
      "pullupRequired" to component.pullupRequired,
      "pullupRecommendedValue" to component.pullupRecommendedValue,
      "pullupNote" to component.pullupNote,
      "importantNotes" to component.importantNotes,
      "connectorImageUrl" to component.connectorImageUrl,
      "pins" to serializePins(component.pinout),
      "characteristic" to serializeCharacteristic(component.characteristic),
      "isUserAdded" to component.isUserAdded,
      "updatedAt" to FieldValue.serverTimestamp()
    )

    if (isCreate) {
      map["createdAt"] = FieldValue.serverTimestamp()
    }

    if (component.resistanceCharacteristic != null) {
      map["resistanceCharacteristic"] = serializeCharacteristic(component.resistanceCharacteristic)
    }

    if (component.deadTimeTables.isNotEmpty()) {
      map["deadTimeTables"] = serializeDeadTimeTables(component.deadTimeTables)
    }

    if (component.flowRateCc != null) {
      map["flowRateCc"] = component.flowRateCc
    }
    if (component.flowPressureBar != null) {
      map["flowPressureBar"] = component.flowPressureBar
    }
    if (component.innerDiameterMm != null) {
      map["innerDiameterMm"] = component.innerDiameterMm
    }
    if (component.outerDiameterMm != null) {
      map["outerDiameterMm"] = component.outerDiameterMm
    }
    if (component.boltSpacingWidthMm != null) {
      map["boltSpacingWidthMm"] = component.boltSpacingWidthMm
    }
    if (component.boltSpacingHeightMm != null) {
      map["boltSpacingHeightMm"] = component.boltSpacingHeightMm
    }
    if (component.boltDiagonal1Mm != null) {
      map["boltDiagonal1Mm"] = component.boltDiagonal1Mm
    }
    if (component.boltDiagonal2Mm != null) {
      map["boltDiagonal2Mm"] = component.boltDiagonal2Mm
    }
    if (component.injectorImpedance.isNotBlank()) {
      map["injectorImpedance"] = component.injectorImpedance
    }
    if (component.ecuOutputsCount.isNotBlank()) {
      map["ecuOutputsCount"] = component.ecuOutputsCount
    }
    if (component.ecuBoardDimensions.isNotBlank()) {
      map["ecuBoardDimensions"] = component.ecuBoardDimensions
    }

    return map
  }

  private fun serializeDeadTimeTables(tables: List<DeadTimeTable>): List<Map<String, Any>> {
    return tables.map { tbl ->
      mapOf(
        "tableName" to tbl.tableName,
        "pressureUnit" to tbl.pressureUnit,
        "notes" to tbl.notes,
        "points" to tbl.points.map { pt ->
          mapOf(
            "voltage" to pt.voltage,
            "timeMs" to pt.timeMs
          )
        }
      )
    }
  }

  private fun serializePins(pins: List<PinInfo>): List<Map<String, Any>> {
    return pins.map { pin ->
      mapOf(
        "pinNumber" to pin.pinNumber,
        "name" to pin.name,
        "description" to pin.description,
        "wireColor" to pin.wireColor,
        "type" to pin.type.name,
        "connectorName" to pin.connectorName
      )
    }
  }

  private fun serializeCharacteristic(char: CharacteristicInfo): Map<String, Any> {
    return mapOf(
      "type" to char.type.name,
      "title" to char.title,
      "inputLabel" to char.inputLabel,
      "outputLabel" to char.outputLabel,
      "formulaOrNotes" to char.formulaOrNotes,
      "points" to char.points.map { pt ->
        mapOf(
          "inVal" to pt.inputValue,
          "inUnit" to pt.inputUnit,
          "outVal" to pt.outputValue,
          "outUnit" to pt.outputUnit
        )
      }
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseComponentFromFirestore(
    docId: String,
    data: Map<String, Any>,
    userVote: Int?
  ): TuningComponent {
    val pinsRaw = data["pins"] as? List<Map<String, Any>> ?: emptyList()
    val pins = pinsRaw.map { pMap ->
      PinInfo(
        pinNumber = pMap["pinNumber"] as? String ?: "",
        name = pMap["name"] as? String ?: "",
        description = pMap["description"] as? String ?: "",
        wireColor = pMap["wireColor"] as? String ?: "",
        type = try {
          PinType.valueOf(pMap["type"] as? String ?: "OTHER")
        } catch (_: Exception) {
          PinType.OTHER
        },
        connectorName = pMap["connectorName"] as? String ?: ""
      )
    }

    val charRaw = data["characteristic"] as? Map<String, Any>
    val characteristic = parseCharacteristicFromMap(charRaw)

    val resCharRaw = data["resistanceCharacteristic"] as? Map<String, Any>
    val resistanceCharacteristic = if (resCharRaw != null) parseCharacteristicFromMap(resCharRaw) else null

    val createdAtTimestamp = data["createdAt"] as? Timestamp
    val lastEditedAtTimestamp = data["lastEditedAt"] as? Timestamp

    val docImageUrlRaw = data["imageUrl"] as? String ?: ""
    val imagesRaw = data["images"] as? List<*> ?: emptyList<Any>()
    val imagesListRaw = imagesRaw.filterIsInstance<String>()

    val docImageUrl = ImageUtils.preCacheBase64Image(context, docImageUrlRaw)
    val imagesList = imagesListRaw.map { ImageUtils.preCacheBase64Image(context, it) }

    val finalImages = if (imagesList.isNotEmpty()) {
      imagesList
    } else if (docImageUrl.isNotBlank()) {
      listOf(docImageUrl)
    } else {
      emptyList()
    }
    val finalPrimaryImage = finalImages.firstOrNull() ?: docImageUrl

    return TuningComponent(
      id = data["id"] as? String ?: docId,
      name = data["name"] as? String ?: "",
      partNumber = data["partNumber"] as? String ?: "",
      category = try {
        val catStr = data["category"] as? String ?: "OTHER"
        when (catStr) {
          "PRESSURE_TEMP" -> ComponentCategory.OIL_PRESSURE
          else -> ComponentCategory.valueOf(catStr)
        }
      } catch (_: Exception) {
        ComponentCategory.OTHER
      },
      shortDescription = data["shortDescription"] as? String ?: "",
      imageUrl = finalPrimaryImage,
      images = finalImages,
      vectorIllustrationKey = data["vectorIllustrationKey"] as? String ?: "map_3b",
      pinout = pins,
      characteristic = characteristic,
      resistanceCharacteristic = resistanceCharacteristic,
      pullupRequired = data["pullupRequired"] as? Boolean ?: false,
      pullupRecommendedValue = data["pullupRecommendedValue"] as? String ?: "",
      pullupNote = data["pullupNote"] as? String ?: "",
      importantNotes = data["importantNotes"] as? String ?: "",
      authorNick = data["authorNick"] as? String ?: "Tuner",
      authorId = data["authorId"] as? String ?: "community",
      connectorImageUrl = data["connectorImageUrl"] as? String ?: "",
      createdAtMillis = createdAtTimestamp?.toDate()?.time ?: System.currentTimeMillis(),
      lastEditedByNick = data["lastEditedByNick"] as? String,
      lastEditedAtMillis = lastEditedAtTimestamp?.toDate()?.time,
      rating = (data["rating"] as? Number)?.toFloat() ?: 5.0f,
      ratingVotesCount = (data["ratingVotesCount"] as? Number)?.toInt() ?: 1,
      userVote = userVote,
      isUserAdded = data["isUserAdded"] as? Boolean ?: false,
      deadTimeTables = parseDeadTimeTables(data["deadTimeTables"]),
      flowRateCc = (data["flowRateCc"] as? Number)?.toDouble(),
      flowPressureBar = (data["flowPressureBar"] as? Number)?.toDouble(),
      innerDiameterMm = (data["innerDiameterMm"] as? Number)?.toDouble(),
      outerDiameterMm = (data["outerDiameterMm"] as? Number)?.toDouble(),
      boltSpacingWidthMm = (data["boltSpacingWidthMm"] as? Number)?.toDouble(),
      boltSpacingHeightMm = (data["boltSpacingHeightMm"] as? Number)?.toDouble(),
      boltDiagonal1Mm = (data["boltDiagonal1Mm"] as? Number)?.toDouble(),
      boltDiagonal2Mm = (data["boltDiagonal2Mm"] as? Number)?.toDouble(),
      injectorImpedance = data["injectorImpedance"] as? String ?: "",
      ecuOutputsCount = data["ecuOutputsCount"] as? String ?: "",
      ecuBoardDimensions = data["ecuBoardDimensions"] as? String ?: ""
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseDeadTimeTables(raw: Any?): List<DeadTimeTable> {
    val list = raw as? List<Map<String, Any>> ?: return emptyList()
    return list.map { tblMap ->
      val pointsRaw = tblMap["points"] as? List<Map<String, Any>> ?: emptyList()
      val points = pointsRaw.map { ptMap ->
        DeadTimePoint(
          voltage = ptMap["voltage"] as? String ?: "",
          timeMs = ptMap["timeMs"] as? String ?: ""
        )
      }
      DeadTimeTable(
        tableName = tblMap["tableName"] as? String ?: "Tabela Dead Time",
        pressureUnit = tblMap["pressureUnit"] as? String ?: "bar",
        points = points,
        notes = tblMap["notes"] as? String ?: ""
      )
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseCharacteristicFromMap(map: Map<String, Any>?): CharacteristicInfo {
    if (map == null) {
      return CharacteristicInfo(
        type = CharType.OTHER,
        title = "Charakterystyka",
        inputLabel = "Wejście",
        outputLabel = "Wyjście",
        points = emptyList()
      )
    }

    val ptsRaw = map["points"] as? List<Map<String, Any>> ?: emptyList()
    val points = ptsRaw.map { pt ->
      CalibrationPoint(
        inputValue = pt["inVal"] as? String ?: "",
        inputUnit = pt["inUnit"] as? String ?: "",
        outputValue = pt["outVal"] as? String ?: "",
        outputUnit = pt["outUnit"] as? String ?: ""
      )
    }

    return CharacteristicInfo(
      type = try {
        CharType.valueOf(map["type"] as? String ?: "OTHER")
      } catch (_: Exception) {
        CharType.OTHER
      },
      title = map["title"] as? String ?: "Charakterystyka",
      inputLabel = map["inputLabel"] as? String ?: "Wejście",
      outputLabel = map["outputLabel"] as? String ?: "Wyjście",
      points = points,
      formulaOrNotes = map["formulaOrNotes"] as? String ?: ""
    )
  }
}
