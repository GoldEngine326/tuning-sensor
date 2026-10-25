package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.ModerationRequest
import com.example.model.ModerationType

object NotificationHelper {
  const val CHANNEL_ID = "tuning_moderation_channel"
  const val CHANNEL_NAME = "Powiadomienia Moderatora"
  const val EXTRA_NAVIGATE_TO = "navigate_to_moderation"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Powiadomienia o nowych zgłoszeniach dodania, edycji lub usunięcia czujników"
        enableLights(true)
        enableVibration(true)
      }
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  fun showModerationNotification(context: Context, request: ModerationRequest) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    createNotificationChannel(context)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_NAVIGATE_TO, true)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      request.id.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val actionDesc = when (request.type) {
      ModerationType.ADD -> "prosi o dodanie nowego czujnika"
      ModerationType.EDIT -> "prosi o modyfikację czujnika"
      ModerationType.DELETE -> "prosi o usunięcie czujnika"
    }

    val contentText = "${request.requesterNick} $actionDesc \"${request.targetSensorName}\""

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Tuning Sensor: Nowe zgłoszenie do zatwierdzenia")
      .setContentText(contentText)
      .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\n\nDotknij, aby przejrzeć i zatwierdzić lub odrzucić wniosek."))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)

    try {
      with(NotificationManagerCompat.from(context)) {
        notify(request.id.hashCode(), builder.build())
      }
    } catch (_: SecurityException) {
      // Permission denied or missing
    }
  }
}
