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
import com.example.ui.screens.AddComponentDialog
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NicknameDialog
import androidx.compose.runtime.DisposableEffect
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
    boltDiagonal2Mm: Double? = null
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
      boltDiagonal2Mm = boltDiagonal2Mm
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
    boltDiagonal2Mm: Double? = null
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
      boltDiagonal2Mm = boltDiagonal2Mm
    )
  }

  fun deleteComponent(id: String): Boolean {
    return repository.deleteComponent(id)
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

  var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
  var showNickDialog by remember { mutableStateOf(false) }

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
        onCheckUpdate = {
          coroutineScope.launch {
            android.widget.Toast.makeText(context, "Sprawdzanie aktualizacji na GitHubie...", android.widget.Toast.LENGTH_SHORT).show()
            val result = GitHubUpdateManager.checkForGitHubUpdate()
            result.onSuccess { (hasUpdate, releaseInfo) ->
              if (hasUpdate && releaseInfo != null) {
                gitHubRelease = releaseInfo
                showGitHubUpdateDialog = true
              } else {
                android.widget.Toast.makeText(context, "Masz zainstalowaną najnowszą wersję!", android.widget.Toast.LENGTH_SHORT).show()
              }
            }.onFailure { err ->
              android.widget.Toast.makeText(context, "Błąd sprawdzania aktualizacji: ${err.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
          }
        },
        onSignOut = onSignOut,
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
          onDelete = { comp ->
            viewModel.deleteComponent(comp.id)
            android.widget.Toast.makeText(
              context,
              "Usunięto czujnik z bazy!",
              android.widget.Toast.LENGTH_SHORT
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
          onDismiss = { currentScreen = AppScreen.Home },
          onSave = { name, partNumber, category, shortDesc, imgUrl, imgs, pins, char, resChar, pullupReq, pullupVal, pullupNote, notes, connImg, deadTimeTables, flowRateCc, flowPressureBar, innerDia, outerDia, boltW, boltH, boltD1, boltD2 ->
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
              boltDiagonal2Mm = boltD2
            )
            android.widget.Toast.makeText(
              context,
              "Pomyślnie zapisano w chmurze Firestore!",
              android.widget.Toast.LENGTH_SHORT
            ).show()
            currentScreen = AppScreen.Detail(created.id)
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
            onDismiss = { currentScreen = AppScreen.Detail(component.id) },
            onSave = { name, partNumber, category, shortDesc, imgUrl, imgs, pins, char, resChar, pullupReq, pullupVal, pullupNote, notes, connImg, deadTimeTables, flowRateCc, flowPressureBar, innerDia, outerDia, boltW, boltH, boltD1, boltD2 ->
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
                boltDiagonal2Mm = boltD2
              )
              android.widget.Toast.makeText(
                context,
                "Zaktualizowano w chmurze Firestore!",
                android.widget.Toast.LENGTH_SHORT
              ).show()
              currentScreen = AppScreen.Detail(component.id)
            },
            onDelete = {
              viewModel.deleteComponent(component.id)
              android.widget.Toast.makeText(
                context,
                "Wpis został usunięty z chmury!",
                android.widget.Toast.LENGTH_SHORT
              ).show()
              currentScreen = AppScreen.Home
            },
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
}
