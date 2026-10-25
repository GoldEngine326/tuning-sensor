package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
enum class ModerationType(val label: String, val badgeColor: Long) {
  ADD("Dodanie nowego czujnika", 0xFF34C759),
  EDIT("Modyfikacja danych czujnika", 0xFFFF9500),
  DELETE("Prośba o usunięcie czujnika", 0xFFFF3B30)
}

@Immutable
enum class ModerationStatus(val label: String, val color: Long) {
  PENDING("Oczekuje na zatwierdzenie", 0xFFFF9500),
  APPROVED("Zatwierdzone przez Administratora", 0xFF34C759),
  REJECTED("Odrzucone", 0xFFFF3B30)
}

@Immutable
data class ModerationRequest(
  val id: String,
  val type: ModerationType,
  val status: ModerationStatus,
  val targetSensorId: String,
  val targetSensorName: String,
  val requesterId: String,
  val requesterNick: String,
  val requesterEmail: String,
  val requestReason: String = "",
  val sensorData: Map<String, Any?> = emptyMap(),
  val originalSensorData: Map<String, Any?> = emptyMap(),
  val createdAtMillis: Long = System.currentTimeMillis(),
  val reviewedAtMillis: Long? = null,
  val reviewerNick: String? = null,
  val reviewerEmail: String? = null,
  val reviewNotes: String? = null
)
