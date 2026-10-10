package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TunerCyan
import com.example.ui.theme.TuningRed
import com.example.util.GitHubReleaseInfo

@Composable
fun GitHubUpdateDialog(
  releaseInfo: GitHubReleaseInfo?,
  isDownloading: Boolean,
  downloadProgress: Int,
  downloadError: String?,
  onDismiss: () -> Unit,
  onStartUpdate: () -> Unit
) {
  if (releaseInfo == null) return

  AlertDialog(
    onDismissRequest = {
      if (!isDownloading) {
        onDismiss()
      }
    },
    containerColor = DarkSurfaceElevated,
    shape = RoundedCornerShape(16.dp),
    icon = {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(TunerCyan.copy(alpha = 0.15f))
          .border(1.dp, TunerCyan, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.SystemUpdate,
          contentDescription = null,
          tint = TunerCyan,
          modifier = Modifier.size(30.dp)
        )
      }
    },
    title = {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "Dostępna nowa wersja z GitHuba!",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = releaseInfo.tagName,
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp,
          color = TunerAmber,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = releaseInfo.bodyNotes.ifBlank { "Nowe usprawnienia bazy danych, wtryskiwaczy oraz funkcji tunerskich." },
          fontSize = 13.sp,
          color = TextSecondary,
          lineHeight = 18.sp
        )

        Spacer(Modifier.height(14.dp))

        if (isDownloading) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(CarbonBlack, RoundedCornerShape(10.dp))
              .padding(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Pobieranie APK z GitHuba...",
                fontSize = 12.sp,
                color = TunerCyan,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "$downloadProgress%",
                fontSize = 12.sp,
                color = TunerAmber,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { downloadProgress / 100f },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = TunerCyan,
              trackColor = DarkSurfaceVariant
            )
          }
        }

        if (downloadError != null) {
          Spacer(Modifier.height(8.dp))
          Text(
            text = downloadError,
            color = TuningRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onStartUpdate,
        enabled = !isDownloading,
        colors = ButtonDefaults.buttonColors(containerColor = TunerCyan),
        shape = RoundedCornerShape(8.dp)
      ) {
        if (isDownloading) {
          CircularProgressIndicator(
            color = Color.Black,
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
          )
          Spacer(Modifier.width(6.dp))
          Text("Pobieranie...", color = Color.Black, fontWeight = FontWeight.Bold)
        } else {
          Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Zaktualizuj teraz", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      if (!isDownloading) {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
          Text("Później")
        }
      }
    }
  )
}
