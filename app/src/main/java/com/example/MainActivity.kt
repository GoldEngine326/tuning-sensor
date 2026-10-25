package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TuningRepository
import com.example.model.CharacteristicInfo
import com.example.model.ComponentCategory
import com.example.model.DeadTimeTable
import com.example.model.PinInfo
import com.example.model.TuningComponent
import com.example.model.ModerationRequest
import com.example.ui.screens.AddComponentDialog
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModerationScreen
import com.example.ui.screens.MySubmissionsDialog
import com.example.ui.screens.NicknameDialog
import com.example.util.NotificationHelper
import android.app.Activity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.auth.AuthManager
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.components.GitHubUpdateDialog
import com.example.util.GitHubReleaseInfo
import com.example.util.GitHubUpdateManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

sealed class AppScreen {
  data object Home : AppScreen()
  data class Detail(val componentId: String) : AppScreen()
  data object Add : AppScreen()
  data class Edit(val componentId: String) : AppScreen()
  data object Moderation : AppScreen()
}

class TuningViewModel(private val repository: TuningRepository) : ViewModel() {
  val components = repository.components
  val userNick = repository.userNick

  fun updateNick(newNick: String) {
    repository.setUserNick(newNick)
  }

  fun vote(componentId: String, score: Int) {
    repository.voteComponent(componentId, score)
  }

  fun addComponent(
    name: String,
    partNumber: String,
    category: ComponentCategory,
    shortDescription: String,
    imageUrl: String,
    images: List<String> = emptyList(),
    pins: List<PinInfo>,
    characteristic: CharacteristicInfo,
    resistanceCharacteristic: CharacteristicInfo?,
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
    return repository.addComponent(
      name = name,
      partNumber = partNumber,
      category = category,
      shortDescription = shortDescription,
      imageUrl = imageUrl,
      images = images,
      pinout = pins,
      characteristic = characteristic,
      resistanceCharacteristic = resistanceCharacteristic,
      pullupRequired = pullupRequired,
      pullupRecommendedValue = pullupRecommendedValue,
      pullupNote = pullupNote,
      importantNotes = importantNotes,
      connectorImageUrl = connectorImageUrl,
      deadTimeTables = deadTimeTables,
      flowRateCc = flowRateCc,
      flowPressureBar = flowPressureBar,
      innerDiameterMm = innerDiameterMm,
      outerDiameterMm = outerDiameterMm,
      boltSpacingWidthMm = boltSpacingWidthMm,
      boltSpacingHeightMm = boltSpacingHeightMm,
      boltDiagonal1Mm = boltDiagonal1Mm,
      boltDiagonal2Mm = boltDiagonal2Mm,
      injectorImpedance = injectorImpedance,
      ecuOutputsCount = ecuOutputsCount,
      ecuBoardDimensions = ecuBoardDimensions
    )
  }

  fun updateComponent(
    id: String,
    name: String,
    partNumber: String,
    category: ComponentCategory,
    shortDescription: String,
    imageUrl: String,
    images: List<String> = emptyList(),
    pins: List<PinInfo>,
    characteristic: CharacteristicInfo,
    resistanceCharacteristic: CharacteristicInfo?,
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
    return repository.updateComponent(
      id = id,
      name = name,
      partNumber = partNumber,
      category = category,
      shortDescription = shortDescription,
      imageUrl = imageUrl,
      images = images,
      pinout = pins,
      characteristic = characteristic,
      resistanceCharacteristic = resistanceCharacteristic,
      pullupRequired = pullupRequired,
      pullupRecommendedValue = pullupRecommendedValue,
      pullupNote = pullupNote,
      importantNotes = importantNotes,
      connectorImageUrl = connectorImageUrl,
      deadTimeTables = deadTimeTables,
      flowRateCc = flowRateCc,
      flowPressureBar = flowPressureBar,
      innerDiameterMm = innerDiameterMm,
      outerDiameterMm = outerDiameterMm,
      boltSpacingWidthMm = boltSpacingWidthMm,
      boltSpacingHeightMm = boltSpacingHeightMm,
      boltDiagonal1Mm = boltDiagonal1Mm,
      boltDiagonal2Mm = boltDiagonal2Mm,
      injectorImpedance = injectorImpedance,
      ecuOutputsCount = ecuOutputsCount,
      ecuBoardDimensions = ecuBoardDimensions
    )
  }

  fun deleteComponent(id: String): Boolean {
    return repository.deleteComponent(id)
  }

  val moderationRequests = repository.moderationRequests
  val pendingRequestsCount = repository.pendingRequestsCount

  fun isAdmin(): Boolean = repository.isAdmin()
  val currentUserEmail: String? get() = repository.currentUserEmail

  fun submitAddProposal(
    name: String,
    partNumber: String,
    category: ComponentCategory,
    shortDescription: String,
    imageUrl: String,
    images: List<String> = emptyList(),
    pins: List<PinInfo>,
    characteristic: CharacteristicInfo,
    resistanceCharacteristic: CharacteristicInfo?,
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
  ): String {
    return repository.submitAddProposal(
      name, partNumber, category, shortDescription, imageUrl, images, pins,
      characteristic, resistanceCharacteristic, pullupRequired, pullupRecommendedValue,
      pullupNote, importantNotes, connectorImageUrl, deadTimeTables, flowRateCc,
      flowPressureBar, innerDiameterMm, outerDiameterMm, boltSpacingWidthMm,
      boltSpacingHeightMm, boltDiagonal1Mm, boltDiagonal2Mm, injectorImpedance,
      ecuOutputsCount, ecuBoardDimensions
    )
  }

  fun submitEditProposal(
    id: String,
    name: String,
    partNumber: String,
    category: ComponentCategory,
    shortDescription: String,
    imageUrl: String = "",
    images: List<String> = emptyList(),
    pins: List<PinInfo>,
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
    ecuBoardDimensions: String = "",
    editReason: String = ""
  ): String {
    return repository.submitEditProposal(
      id, name, partNumber, category, shortDescription, imageUrl, images, pins,
      characteristic, resistanceCharacteristic, pullupRequired, pullupRecommendedValue,
      pullupNote, importantNotes, connectorImageUrl, deadTimeTables, flowRateCc,
      flowPressureBar, innerDiameterMm, outerDiameterMm, boltSpacingWidthMm,
      boltSpacingHeightMm, boltDiagonal1Mm, boltDiagonal2Mm, injectorImpedance,
      ecuOutputsCount, ecuBoardDimensions, editReason
    )
  }

  fun submitDeleteProposal(component: TuningComponent, reason: String): String {
    return repository.submitDeleteProposal(component, reason)
  }

  fun approveModerationRequest(request: ModerationRequest, adminNick: String, onComplete: ((Boolean) -> Unit)? = null) {
    repository.approveModerationRequest(request, adminNick, onComplete)
  }

  fun rejectModerationRequest(request: ModerationRequest, adminNick: String, reason: String, onComplete: ((Boolean) -> Unit)? = null) {
    repository.rejectModerationRequest(request, adminNick, reason, onComplete)
  }
}

class TuningViewModelFactory(private val repository: TuningRepository) :
  ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return TuningViewModel(repository) as T
  }
}

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Gwarancja inicjalizacji FirebaseApp dla aplikacji
    try {
      if (FirebaseApp.getApps(this).isEmpty()) {
        val app = FirebaseApp.initializeApp(this)
        if (app == null) {
          val options = FirebaseOptions.Builder()
            .setApplicationId("1:797791659250:android:2be4f5f10488932c989c6b")
            .setApiKey("AIzaSyBVcO4b2Li5uL13zPEFZ_gGNOE_eIPMDHc")
            .setProjectId("gen-lang-client-0918404378")
            .setStorageBucket("gen-lang-client-0918404378.firebasestorage.app")
            .build()
          FirebaseApp.initializeApp(this, options)
        }
      }
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "Błąd inicjalizacji FirebaseApp: ${e.message}", e)
    }

    setContent {
      MyApplicationTheme {
        MainRoot()
      }
    }
  }
}

@Composable
fun MainRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var currentUser by remember {
    mutableStateOf(
      try {
        Firebase.auth.currentUser
      } catch (e: Exception) {
        null
      }
    )
  }

  DisposableEffect(Unit) {
    var listener: FirebaseAuth.AuthStateListener? = null
    try {
      listener = FirebaseAuth.AuthStateListener { auth ->
        currentUser = auth.currentUser
      }
      Firebase.auth.addAuthStateListener(listener)
    } catch (e: Exception) {
      android.util.Log.e("MainRoot", "Błąd rejestracji AuthStateListener: ${e.message}")
    }
    onDispose {
      try {
        listener?.let { Firebase.auth.removeAuthStateListener(it) }
      } catch (e: Exception) {
        // ignore
      }
    }
  }

  if (currentUser == null) {
    SignInScreen(
      onAuthSuccess = {
        currentUser = Firebase.auth.currentUser
      }
    )
  } else {
    val repository = remember { TuningRepository(context.applicationContext) }
    val viewModel: TuningViewModel = viewModel(
      factory = TuningViewModelFactory(repository)
    )

    TuningAppContent(
      viewModel = viewModel,
      onSignOut = {
        val credentialManager = CredentialManager.create(context)
        AuthManager.signOut(
          context = context,
          credentialManager = credentialManager,
          onSignOutComplete = {
            repository.stopListening()
            currentUser = null
          },
          scope = scope
        )
      }
    )
  }
}

@Composable
fun TuningAppContent(
  viewModel: TuningViewModel,
  onSignOut: () -> Unit = {}
) {
  val context = LocalContext.current
  val components by viewModel.components.collectAsState()
  val userNick by viewModel.userNick.collectAsState()
  val moderationRequests by viewModel.moderationRequests.collectAsState()
  val pendingRequestsCount by viewModel.pendingRequestsCount.collectAsState()
  val isAdmin = viewModel.isAdmin()
  var showMySubmissionsDialog by remember { mutableStateOf(false) }

  var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
  var showNickDialog by remember { mutableStateOf(false) }

  val activity = context as? Activity
  val openModerationFromIntent = remember {
    activity?.intent?.getBooleanExtra(NotificationHelper.EXTRA_NAVIGATE_TO, false) == true
  }
  LaunchedEffect(openModerationFromIntent, isAdmin) {
    if (openModerationFromIntent && isAdmin) {
      currentScreen = AppScreen.Moderation
    }
  }

  // GitHub Auto-Update States
  val coroutineScope = rememberCoroutineScope()
  var gitHubRelease by remember { mutableStateOf<GitHubReleaseInfo?>(null) }
  var showGitHubUpdateDialog by remember { mutableStateOf(false) }
  var isDownloadingGitHubUpdate by remember { mutableStateOf(false) }
  var gitHubDownloadProgress by remember { mutableStateOf(0) }
  var gitHubDownloadError by remember { mutableStateOf<String?>(null) }

  DisposableEffect(Unit) {
    coroutineScope.launch {
      val result = GitHubUpdateManager.checkForGitHubUpdate()
      result.onSuccess { (hasUpdate, releaseInfo) ->
        if (hasUpdate && releaseInfo != null) {
          gitHubRelease = releaseInfo
          showGitHubUpdateDialog = true
        }
      }
    }
    onDispose { }
  }

  when (val screen = currentScreen) {
    is AppScreen.Home -> {
      HomeScreen(
        components = components,
        userNick = userNick,
        onComponentClick = { comp ->
          currentScreen = AppScreen.Detail(comp.id)
        },
        onAddClick = {
          currentScreen = AppScreen.Add
        },
        onOpenNickDialog = {
          showNickDialog = true
        },
        onSignOut = onSignOut,
        isAdmin = isAdmin,
        pendingRequestsCount = pendingRequestsCount,
        onOpenModeration = { currentScreen = AppScreen.Moderation },
        onOpenMySubmissions = { showMySubmissionsDialog = true },
        modifier = Modifier.fillMaxSize()
      )
    }

    is AppScreen.Moderation -> {
      ModerationScreen(
        requests = moderationRequests,
        adminNick = userNick,
        onApprove = { req ->
          viewModel.approveModerationRequest(req, userNick) { success ->
            android.widget.Toast.makeText(
              context,
              if (success) "Zatwierdzono i zastosowano zmiany w bazie Firestore!" else "Błąd podczas zatwierdzania wniosku",
              android.widget.Toast.LENGTH_SHORT
            ).show()
          }
        },
        onReject = { req, reason ->
          viewModel.rejectModerationRequest(req, userNick, reason) { success ->
            android.widget.Toast.makeText(
              context,
              if (success) "Odrzucono wniosek użytkownika" else "Błąd podczas odrzucania wniosku",
              android.widget.Toast.LENGTH_SHORT
            ).show()
          }
        },
        onBack = { currentScreen = AppScreen.Home },
        modifier = Modifier.fillMaxSize()
      )
    }

    is AppScreen.Detail -> {
      val component = components.find { it.id == screen.componentId }
      if (component != null) {
        DetailScreen(
          component = component,
          onBack = { currentScreen = AppScreen.Home },
          onEditClick = { comp ->
            currentScreen = AppScreen.Edit(comp.id)
          },
          onDelete = if (isAdmin) { comp ->
            viewModel.deleteComponent(comp.id)
            android.widget.Toast.makeText(
              context,
              "Usunięto czujnik z bazy!",
              android.widget.Toast.LENGTH_SHORT
            ).show()
            currentScreen = AppScreen.Home
          } else null,
          isAdmin = isAdmin,
          onRequestDelete = { comp, reason ->
            viewModel.submitDeleteProposal(comp, reason)
            android.widget.Toast.makeText(
              context,
              "Wniosek o usunięcie został przesłany do Administratora (yetigiusz@gmail.com)!",
              android.widget.Toast.LENGTH_LONG
            ).show()
            currentScreen = AppScreen.Home
          },
          onVote = { score ->
            viewModel.vote(component.id, score)
            android.widget.Toast.makeText(
              context,
              "Ocena zapisana w chmurze!",
              android.widget.Toast.LENGTH_SHORT
            ).show()
          },
          modifier = Modifier.fillMaxSize()
        )
      } else {
        currentScreen = AppScreen.Home
      }
    }

    is AppScreen.Add -> {
      key("add_new_component") {
        AddComponentDialog(
          userNick = userNick,
          existingComponent = null,
          isAdmin = isAdmin,
          onDismiss = { currentScreen = AppScreen.Home },
          onSave = { name, partNumber, category, shortDesc, imgUrl, imgs, pins, char, resChar, pullupReq, pullupVal, pullupNote, notes, connImg, deadTimeTables, flowRateCc, flowPressureBar, innerDia, outerDia, boltW, boltH, boltD1, boltD2, injImp, ecuOuts, ecuDim ->
            if (isAdmin) {
              val created = viewModel.addComponent(
                name = name,
                partNumber = partNumber,
                category = category,
                shortDescription = shortDesc,
                imageUrl = imgUrl,
                images = imgs,
                pins = pins,
                characteristic = char,
                resistanceCharacteristic = resChar,
                pullupRequired = pullupReq,
                pullupRecommendedValue = pullupVal,
                pullupNote = pullupNote,
                importantNotes = notes,
                connectorImageUrl = connImg,
                deadTimeTables = deadTimeTables,
                flowRateCc = flowRateCc,
                flowPressureBar = flowPressureBar,
                innerDiameterMm = innerDia,
                outerDiameterMm = outerDia,
                boltSpacingWidthMm = boltW,
                boltSpacingHeightMm = boltH,
                boltDiagonal1Mm = boltD1,
                boltDiagonal2Mm = boltD2,
                injectorImpedance = injImp,
                ecuOutputsCount = ecuOuts,
                ecuBoardDimensions = ecuDim
              )
              android.widget.Toast.makeText(
                context,
                "Pomyślnie zapisano w chmurze Firestore!",
                android.widget.Toast.LENGTH_SHORT
              ).show()
              currentScreen = AppScreen.Detail(created.id)
            } else {
              viewModel.submitAddProposal(
                name = name,
                partNumber = partNumber,
                category = category,
                shortDescription = shortDesc,
                imageUrl = imgUrl,
                images = imgs,
                pins = pins,
                characteristic = char,
                resistanceCharacteristic = resChar,
                pullupRequired = pullupReq,
                pullupRecommendedValue = pullupVal,
                pullupNote = pullupNote,
                importantNotes = notes,
                connectorImageUrl = connImg,
                deadTimeTables = deadTimeTables,
                flowRateCc = flowRateCc,
                flowPressureBar = flowPressureBar,
                innerDiameterMm = innerDia,
                outerDiameterMm = outerDia,
                boltSpacingWidthMm = boltW,
                boltSpacingHeightMm = boltH,
                boltDiagonal1Mm = boltD1,
                boltDiagonal2Mm = boltD2,
                injectorImpedance = injImp,
                ecuOutputsCount = ecuOuts,
                ecuBoardDimensions = ecuDim
              )
              android.widget.Toast.makeText(
                context,
                "Propozycja czujnika została przesłana do zatwierdzenia przez Administratora (yetigiusz@gmail.com)!",
                android.widget.Toast.LENGTH_LONG
              ).show()
              currentScreen = AppScreen.Home
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    is AppScreen.Edit -> {
      val component = components.find { it.id == screen.componentId }
      if (component != null) {
        key(component.id) {
          AddComponentDialog(
            userNick = userNick,
            existingComponent = component,
            isAdmin = isAdmin,
            onDismiss = { currentScreen = AppScreen.Detail(component.id) },
            onSave = { name, partNumber, category, shortDesc, imgUrl, imgs, pins, char, resChar, pullupReq, pullupVal, pullupNote, notes, connImg, deadTimeTables, flowRateCc, flowPressureBar, innerDia, outerDia, boltW, boltH, boltD1, boltD2, injImp, ecuOuts, ecuDim ->
              if (isAdmin) {
                viewModel.updateComponent(
                  id = component.id,
                  name = name,
                  partNumber = partNumber,
                  category = category,
                  shortDescription = shortDesc,
                  imageUrl = imgUrl,
                  images = imgs,
                  pins = pins,
                  characteristic = char,
                  resistanceCharacteristic = resChar,
                  pullupRequired = pullupReq,
                  pullupRecommendedValue = pullupVal,
                  pullupNote = pullupNote,
                  importantNotes = notes,
                  connectorImageUrl = connImg,
                  deadTimeTables = deadTimeTables,
                  flowRateCc = flowRateCc,
                  flowPressureBar = flowPressureBar,
                  innerDiameterMm = innerDia,
                  outerDiameterMm = outerDia,
                  boltSpacingWidthMm = boltW,
                  boltSpacingHeightMm = boltH,
                  boltDiagonal1Mm = boltD1,
                  boltDiagonal2Mm = boltD2,
                  injectorImpedance = injImp,
                  ecuOutputsCount = ecuOuts,
                  ecuBoardDimensions = ecuDim
                )
                android.widget.Toast.makeText(
                  context,
                  "Zaktualizowano w chmurze Firestore!",
                  android.widget.Toast.LENGTH_SHORT
                ).show()
                currentScreen = AppScreen.Detail(component.id)
              } else {
                viewModel.submitEditProposal(
                  id = component.id,
                  name = name,
                  partNumber = partNumber,
                  category = category,
                  shortDescription = shortDesc,
                  imageUrl = imgUrl,
                  images = imgs,
                  pins = pins,
                  characteristic = char,
                  resistanceCharacteristic = resChar,
                  pullupRequired = pullupReq,
                  pullupRecommendedValue = pullupVal,
                  pullupNote = pullupNote,
                  importantNotes = notes,
                  connectorImageUrl = connImg,
                  deadTimeTables = deadTimeTables,
                  flowRateCc = flowRateCc,
                  flowPressureBar = flowPressureBar,
                  innerDiameterMm = innerDia,
                  outerDiameterMm = outerDia,
                  boltSpacingWidthMm = boltW,
                  boltSpacingHeightMm = boltH,
                  boltDiagonal1Mm = boltD1,
                  boltDiagonal2Mm = boltD2,
                  injectorImpedance = injImp,
                  ecuOutputsCount = ecuOuts,
                  ecuBoardDimensions = ecuDim,
                  editReason = "Zaproponowano aktualizację parametrów czujnika"
                )
                android.widget.Toast.makeText(
                  context,
                  "Propozycja zmian została przesłana do zatwierdzenia przez Administratora (yetigiusz@gmail.com)!",
                  android.widget.Toast.LENGTH_LONG
                ).show()
                currentScreen = AppScreen.Detail(component.id)
              }
            },
            onDelete = if (isAdmin) {
              {
                viewModel.deleteComponent(component.id)
                android.widget.Toast.makeText(
                  context,
                  "Wpis został usunięty z chmury!",
                  android.widget.Toast.LENGTH_SHORT
                ).show()
                currentScreen = AppScreen.Home
              }
            } else null,
            modifier = Modifier.fillMaxSize()
          )
        }
      } else {
        currentScreen = AppScreen.Home
      }
    }
  }

  // Global Dialogs
  if (showNickDialog) {
    NicknameDialog(
      currentNick = userNick,
      onDismiss = { showNickDialog = false },
      onSave = { newNick ->
        viewModel.updateNick(newNick)
        showNickDialog = false
      }
    )
  }

  if (showGitHubUpdateDialog && gitHubRelease != null) {
    GitHubUpdateDialog(
      releaseInfo = gitHubRelease,
      isDownloading = isDownloadingGitHubUpdate,
      downloadProgress = gitHubDownloadProgress,
      downloadError = gitHubDownloadError,
      onDismiss = { showGitHubUpdateDialog = false },
      onStartUpdate = {
        val url = gitHubRelease?.apkDownloadUrl ?: ""
        if (url.isNotBlank()) {
          isDownloadingGitHubUpdate = true
          gitHubDownloadError = null
          gitHubDownloadProgress = 0
          coroutineScope.launch {
            GitHubUpdateManager.downloadAndInstallApk(
              context = context,
              apkUrl = url,
              onProgress = { p -> gitHubDownloadProgress = p },
              onError = { err ->
                isDownloadingGitHubUpdate = false
                gitHubDownloadError = err
              }
            )
            isDownloadingGitHubUpdate = false
          }
        }
      },
      onOpenInBrowser = {
        try {
          val browserIntent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://github.com/GoldEngine326/tuning-sensor/releases/latest")
          ).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(browserIntent)
        } catch (e: Exception) {
          android.widget.Toast.makeText(context, "Nie można otworzyć przeglądarki: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  if (showMySubmissionsDialog) {
    MySubmissionsDialog(
      requests = moderationRequests,
      currentUserId = Firebase.auth.currentUser?.uid,
      onDismiss = { showMySubmissionsDialog = false }
    )
  }
}
