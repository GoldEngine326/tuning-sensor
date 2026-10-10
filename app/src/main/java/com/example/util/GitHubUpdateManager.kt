package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
  val tagName: String,
  val releaseName: String,
  val bodyNotes: String,
  val apkDownloadUrl: String,
  val publishedAt: String
)

object GitHubUpdateManager {

  private const val TAG = "GitHubUpdateManager"

  // Repozytorium na GitHubie
  const val GITHUB_OWNER = "GoldEngine326"
  const val GITHUB_REPO = "tuning-sensor"

  const val CURRENT_VERSION_TAG = "v${BuildConfig.VERSION_NAME}"
  const val CURRENT_VERSION_CODE = BuildConfig.VERSION_CODE

  /**
   * Sprawdza najnowszą wersję w GitHub Releases:
   * https://api.github.com/repos/GoldEngine326/tuning-sensor/releases/latest
   */
  suspend fun checkForGitHubUpdate(): Result<Pair<Boolean, GitHubReleaseInfo?>> = withContext(Dispatchers.IO) {
    try {
      val apiUrl = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
      val url = URL(apiUrl)
      val connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
      connection.setRequestProperty("User-Agent", "TuningSensor-AndroidApp")
      connection.connectTimeout = 10000
      connection.readTimeout = 15000

      val code = connection.responseCode
      if (code != 200) {
        Log.w(TAG, "GitHub API zwróciło kod $code")
        return@withContext Result.failure(Exception("HTTP $code z GitHub API"))
      }

      val reader = BufferedReader(InputStreamReader(connection.inputStream))
      val response = StringBuilder()
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        response.append(line)
      }
      reader.close()

      val json = JSONObject(response.toString())
      val tagName = json.optString("tag_name", "").trim()
      val name = json.optString("name", "Nowa wersja Tuning Sensor")
      val body = json.optString("body", "Aktualizacja z poprawkami i nowymi danymi.")
      val publishedAt = json.optString("published_at", "")

      // Szukanie pliku .apk w assets
      val assets = json.optJSONArray("assets")
      var apkUrl = ""
      if (assets != null) {
        for (i in 0 until assets.length()) {
          val asset = assets.getJSONObject(i)
          val assetName = asset.optString("name", "")
          if (assetName.endsWith(".apk", ignoreCase = true)) {
            apkUrl = asset.optString("browser_download_url", "")
            break
          }
        }
      }

      if (apkUrl.isBlank()) {
        // Zapasowy standardowy link do najnowszego wydania
        apkUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest/download/TuningSensor-latest.apk"
      }

      val releaseInfo = GitHubReleaseInfo(
        tagName = tagName,
        releaseName = name,
        bodyNotes = body,
        apkDownloadUrl = apkUrl,
        publishedAt = publishedAt
      )

      val hasUpdate = isNewerVersion(tagName)
      Result.success(Pair(hasUpdate, releaseInfo))
    } catch (e: Exception) {
      Log.e(TAG, "Błąd sprawdzania wydań na GitHubie: ${e.message}", e)
      Result.failure(e)
    }
  }

  /**
   * Porównuje wersję z tagu GitHuba (np. "v1.1", "1.2") z wersją aplikacji
   */
  private fun isNewerVersion(remoteTag: String): Boolean {
    val cleanRemote = remoteTag.removePrefix("v").removePrefix("V").trim()
    val cleanCurrent = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()

    if (cleanRemote.isBlank()) return false
    if (cleanRemote == cleanCurrent) return false

    val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
    val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

    for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
      val r = remoteParts.getOrElse(i) { 0 }
      val c = currentParts.getOrElse(i) { 0 }
      if (r > c) return true
      if (r < c) return false
    }
    return false
  }

  /**
   * Pobiera plik APK z GitHuba i uruchamia instalację w systemie Android
   */
  suspend fun downloadAndInstallApk(
    context: Context,
    apkUrl: String,
    onProgress: (Int) -> Unit,
    onError: (String) -> Unit
  ) = withContext(Dispatchers.IO) {
    try {
      var redirectUrl = apkUrl
      var connection: HttpURLConnection
      var redirectCount = 0

      // Obsługa przekierowań (GitHub Releases przekierowuje do AWS S3)
      while (true) {
        val url = URL(redirectUrl)
        connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "TuningSensor-AndroidApp")
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.connect()

        val status = connection.responseCode
        if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
            status == HttpURLConnection.HTTP_MOVED_PERM ||
            status == HttpURLConnection.HTTP_SEE_OTHER ||
            status == 307 || status == 308
        ) {
          redirectUrl = connection.getHeaderField("Location")
          redirectCount++
          if (redirectCount > 5) break
          continue
        }
        break
      }

      val fileLength = connection.contentLength
      val apkFile = File(context.cacheDir, "TuningSensor_update.apk")
      if (apkFile.exists()) {
        apkFile.delete()
      }

      connection.inputStream.use { input ->
        FileOutputStream(apkFile).use { output ->
          val data = ByteArray(8 * 1024)
          var total: Long = 0
          var count: Int
          var lastProgress = 0

          while (input.read(data).also { count = it } != -1) {
            total += count
            output.write(data, 0, count)
            if (fileLength > 0) {
              val currentProgress = (total * 100 / fileLength).toInt()
              if (currentProgress != lastProgress) {
                lastProgress = currentProgress
                withContext(Dispatchers.Main) {
                  onProgress(currentProgress)
                }
              }
            }
          }
          output.flush()
        }
      }

      withContext(Dispatchers.Main) {
        installApk(context, apkFile)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Błąd pobierania APK z GitHuba", e)
      withContext(Dispatchers.Main) {
        onError("Błąd pobierania z GitHuba: ${e.localizedMessage ?: "Brak połączenia"}")
      }
    }
  }

  fun installApk(context: Context, apkFile: File) {
    try {
      if (!apkFile.exists()) {
        Toast.makeText(context, "Brak pobranego pliku APK!", Toast.LENGTH_SHORT).show()
        return
      }

      val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
      )

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Błąd instalatora APK", e)
      Toast.makeText(context, "Błąd uruchomienia instalatora: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }
}
