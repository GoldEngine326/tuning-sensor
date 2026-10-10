package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import java.util.Locale

@Composable
fun RatingBarView(
  rating: Float,
  votesCount: Int,
  userVote: Int?,
  onVote: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = String.format(Locale.US, "%.1f", rating),
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = TextPrimary
            )
            Spacer(Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = TunerAmber,
              modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = "($votesCount ${if (votesCount == 1) "ocena" else if (votesCount in 2..4) "oceny" else "ocen"})",
              fontSize = 13.sp,
              color = TextSecondary
            )
          }

          Text(
            text = if (userVote != null) "Twoja ocena: $userVote / 5 ★ (Kliknij, aby zmienić)" else "Oceń przydatność tego wpisu:",
            fontSize = 12.sp,
            color = if (userVote != null) TunerAmber else TextMuted
          )
        }

        // 5 Star voting buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          for (starIndex in 1..5) {
            val isFilled = if (userVote != null) {
              starIndex <= userVote
            } else {
              starIndex <= rating.toInt()
            }

            Icon(
              imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = "Oceń na $starIndex gwiazdek",
              tint = if (isFilled) TunerAmber else Color.Gray,
              modifier = Modifier
                .size(32.dp)
                .clickable { onVote(starIndex) }
                .padding(2.dp)
            )
          }
        }
      }
    }
  }
}
