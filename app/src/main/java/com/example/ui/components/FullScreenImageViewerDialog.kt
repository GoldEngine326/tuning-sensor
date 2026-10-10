package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TunerCyan
import com.example.ui.theme.TuningRed
import com.example.util.ImageUtils
import java.util.Locale

@Composable
fun FullScreenImageViewerDialog(
  imageUrl: String? = null,
  images: List<String> = emptyList(),
  initialIndex: Int = 0,
  vectorKey: String,
  title: String,
  partNumber: String,
  onDismiss: () -> Unit
) {
  val allImages = remember(images, imageUrl) {
    if (images.isNotEmpty()) images else listOfNotNull(imageUrl?.ifBlank { null })
  }
  var currentIndex by remember(allImages, initialIndex) {
    mutableIntStateOf(initialIndex.coerceIn(0, maxOf(0, allImages.size - 1)))
  }
  var scale by remember(currentIndex) { mutableFloatStateOf(1f) }
  var offset by remember(currentIndex) { mutableStateOf(Offset.Zero) }

  val activeImage = allImages.getOrNull(currentIndex) ?: imageUrl
  val context = LocalContext.current

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = false
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
      // Zoomable Image Canvas
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(currentIndex) {
            detectTapGestures(
              onDoubleTap = {
                if (scale > 1.2f) {
                  scale = 1f
                  offset = Offset.Zero
                } else {
                  scale = 2.5f
                }
              },
              onTap = {
                if (scale <= 1.05f) {
                  onDismiss()
                }
              }
            )
          }
          .pointerInput(currentIndex) {
            detectTransformGestures { _, pan, zoom, _ ->
              scale = (scale * zoom).coerceIn(1f, 5f)
              if (scale > 1f) {
                offset += pan
              } else {
                offset = Offset.Zero
              }
            }
          },
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
              scaleX = scale,
              scaleY = scale,
              translationX = offset.x,
              translationY = offset.y
            ),
          contentAlignment = Alignment.Center
        ) {
          val imageModel = ImageUtils.getImageModel(context, activeImage)
          if (imageModel != null) {
            SubcomposeAsyncImage(
              model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .build(),
              contentDescription = "Powiększone zdjęcie $title",
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
              loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(color = TuningRed, strokeWidth = 3.dp)
                }
              },
              error = {
                VectorIllustration(
                  vectorKey = vectorKey,
                  modifier = Modifier.fillMaxSize()
                )
              }
            )
          } else {
            VectorIllustration(
              vectorKey = vectorKey,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }

      // Next / Previous Navigation arrows when multiple photos exist
      if (allImages.size > 1 && scale <= 1.2f) {
        if (currentIndex > 0) {
          IconButton(
            onClick = {
              currentIndex--
              scale = 1f
              offset = Offset.Zero
            },
            modifier = Modifier
              .align(Alignment.CenterStart)
              .padding(start = 12.dp)
              .background(Color.Black.copy(alpha = 0.65f), CircleShape)
              .size(44.dp)
              .testTag("prev_image_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Poprzednie zdjęcie",
              tint = Color.White
            )
          }
        }

        if (currentIndex < allImages.size - 1) {
          IconButton(
            onClick = {
              currentIndex++
              scale = 1f
              offset = Offset.Zero
            },
            modifier = Modifier
              .align(Alignment.CenterEnd)
              .padding(end = 12.dp)
              .background(Color.Black.copy(alpha = 0.65f), CircleShape)
              .size(44.dp)
              .testTag("next_image_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Następne zdjęcie",
              tint = Color.White
            )
          }
        }
      }

      // Top Overlay Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .background(Color.Black.copy(alpha = 0.82f))
          .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (partNumber.isNotBlank()) {
              Text(
                text = partNumber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TunerCyan,
                maxLines = 1
              )
            }
            if (allImages.size > 1) {
              if (partNumber.isNotBlank()) Spacer(Modifier.width(8.dp))
              Text(
                text = "• Zdjęcie ${currentIndex + 1} z ${allImages.size}",
                fontSize = 12.sp,
                color = TunerAmber,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (scale > 1.05f) {
            IconButton(
              onClick = {
                scale = 1f
                offset = Offset.Zero
              },
              modifier = Modifier
                .background(DarkSurfaceElevated, CircleShape)
                .size(38.dp)
                .testTag("reset_zoom_button")
            ) {
              Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Zresetuj powiększenie (1x)",
                tint = TunerAmber,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(Modifier.width(8.dp))
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .background(DarkSurfaceElevated, CircleShape)
              .size(38.dp)
              .testTag("close_fullscreen_image_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Zamknij podgląd zdjęcia",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // Bottom Control & Zoom Panel
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Zoom Buttons pill: - / + / %
        Row(
          modifier = Modifier
            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Zoom out button
          IconButton(
            onClick = {
              scale = (scale - 0.5f).coerceAtLeast(1f)
              if (scale <= 1.05f) {
                offset = Offset.Zero
              }
            },
            enabled = scale > 1.05f,
            modifier = Modifier
              .size(36.dp)
              .background(if (scale > 1.05f) DarkSurfaceVariant else Color.Transparent, CircleShape)
              .testTag("zoom_out_button")
          ) {
            Icon(
              imageVector = Icons.Default.Remove,
              contentDescription = "Pomniejsz",
              tint = if (scale > 1.05f) TextPrimary else TextSecondary.copy(alpha = 0.5f),
              modifier = Modifier.size(18.dp)
            )
          }

          // Percentage badge
          Text(
            text = "${String.format(Locale.US, "%.0f", scale * 100)}%",
            color = TunerCyan,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
          )

          // Zoom in button
          IconButton(
            onClick = {
              scale = (scale + 0.5f).coerceAtMost(5f)
            },
            enabled = scale < 4.95f,
            modifier = Modifier
              .size(36.dp)
              .background(if (scale < 4.95f) TuningRed else DarkSurfaceVariant, CircleShape)
              .testTag("zoom_in_button")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Powiększ",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(Modifier.height(8.dp))

        // Info subtitle
        Text(
          text = if (allImages.size > 1) "Przełączaj strzałkami • Rozsuń palce lub użyj +/-" else "Dotknij dwukrotnie, rozsuń palce lub użyj +/-",
          color = TextSecondary,
          fontSize = 11.sp
        )
      }
    }
  }
}
