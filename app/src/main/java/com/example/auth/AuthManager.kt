package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object AuthManager {
  private const val TAG = "AuthManager"
  private const val PREFS_NAME = "tuning_auth_prefs"
  private const val KEY_PREVIOUSLY_LOGGED_IN = "user_previously_logged_in"
  private const val KEY_SAVED_UID = "saved_user_uid"
  private const val KEY_SAVED_EMAIL = "saved_user_email"

  fun isPreviouslyLoggedIn(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_PREVIOUSLY_LOGGED_IN, false)
  }

  fun setPreviouslyLoggedIn(context: Context, loggedIn: Boolean, uid: String? = null, email: String? = null) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean(KEY_PREVIOUSLY_LOGGED_IN, loggedIn)
      .putString(KEY_SAVED_UID, uid)
      .putString(KEY_SAVED_EMAIL, email)
      .apply()
  }

  fun attemptAutoSignIn(
    activity: Activity,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
  ) {
    val currentUser = Firebase.auth.currentUser
    if (currentUser != null) {
      setPreviouslyLoggedIn(activity, true, currentUser.uid, currentUser.email)
      onAuthSuccess()
      return
    }

    val clientId = try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.w(TAG, "Web client ID not found: ${e.message}")
      onUnauthenticated()
      return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          val signedInUser = Firebase.auth.currentUser
          setPreviouslyLoggedIn(activity, true, signedInUser?.uid, signedInUser?.email)
          onAuthSuccess()
        } else {
          onUnauthenticated()
        }
      } catch (e: Exception) {
        Log.d(TAG, "Silent auto sign-in skipped: ${e.message}")
        onUnauthenticated()
      }
    }
  }

  fun onGoogleSignInClicked(
    activity: Activity,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
  ) {
    val clientId = try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          val signedInUser = Firebase.auth.currentUser
          setPreviouslyLoggedIn(activity, true, signedInUser?.uid, signedInUser?.email)
          onAuthSuccess()
        } else {
          onAuthError("Nieprawidłowy typ poświadczeń")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        val rawMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
        val friendlyMessage = when {
          rawMsg.contains("10:") || rawMsg.contains("DEVELOPER_ERROR") || rawMsg.contains("12500") || rawMsg.contains("ApiException: 10") -> {
            "Błąd podpisu aplikacji (DEVELOPER_ERROR 10): Odcisk klucza SHA-1 zainstalowanego pliku APK nie pasuje do zarejestrowanego w Firebase. Aby logowanie Google działało, zainstaluj wersję Debug zbudowaną w Android Studio (z dołączonym kluczem debug.keystore) lub pobierz gotowy plik APK bezpośrednio z menu AI Studio."
          }
          rawMsg.contains("16:") || rawMsg.contains("Cannot find a matching credential") -> {
            "Nie wybrano konta Google lub brak dostępnych poświadczeń na urządzeniu."
          }
          rawMsg.contains("7:") || rawMsg.contains("NETWORK_ERROR") -> {
            "Błąd sieci: Brak połączenia z serwerami Google."
          }
          else -> "Błąd logowania przez Google: $rawMsg"
        }
        onAuthError(friendlyMessage)
      }
    }
  }

  fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
  ) {
    Firebase.auth.signOut()
    setPreviouslyLoggedIn(context, false)
    scope.launch {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.e(TAG, "Failed to clear credential state", e)
      } finally {
        onSignOutComplete()
      }
    }
  }
}
