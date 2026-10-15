package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TunerCyan
import com.example.ui.theme.TuningRed

import com.example.util.ImageUtils

@Composable
fun ComponentHeroImage(
  imageUrl: String?,
  vectorKey: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(210.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(DarkSurfaceVariant),
    contentAlignment = Alignment.Center
  ) {
    val context = LocalContext.current
    val imageModel = ImageUtils.getImageModel(context, imageUrl)
    if (imageModel != null) {
      SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
          .data(imageModel)
          .crossfade(true)
          .build(),
        contentDescription = "Zdjęcie podzespołu",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        loading = {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TuningRed, strokeWidth = 2.dp)
          }
        },
        error = {
          // If internet or photo fails to load, gracefully display the custom vector artwork!
          VectorIllustration(vectorKey = vectorKey)
        }
      )
    } else {
      VectorIllustration(vectorKey = vectorKey)
    }
  }
}

@Composable
fun ComponentThumbnail(
  imageUrl: String?,
  vectorKey: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val imageModel = remember(imageUrl) {
    ImageUtils.getImageModel(context, imageUrl)
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(DarkSurfaceVariant),
    contentAlignment = Alignment.Center
  ) {
    if (imageModel == null) {
      VectorIllustration(vectorKey = vectorKey)
    } else {
      var isLoaded by remember(imageUrl) { mutableStateOf(false) }
      var isError by remember(imageUrl) { mutableStateOf(false) }

      // Immediate vector illustration as lightweight placeholder without spinning progress bar jank
      if (!isLoaded || isError) {
        VectorIllustration(vectorKey = vectorKey)
      }

      if (!isError) {
        val imageRequest = remember(imageModel, context) {
          ImageRequest.Builder(context)
            .data(imageModel)
            .size(240, 240) // Target downsampled thumbnail resolution to save memory and avoid GC stalls
            .crossfade(100)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .listener(
              onSuccess = { _, _ -> isLoaded = true },
              onError = { _, _ -> isError = true }
            )
            .build()
        }

        AsyncImage(
          model = imageRequest,
          contentDescription = "Zdjęcie podzespołu",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

@Composable
fun VectorIllustration(vectorKey: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(DarkSurfaceElevated, CarbonBlack)
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val cx = w / 2f
      val cy = h / 2f

      when (vectorKey) {
        "vw_coils" -> drawVwCoilIllustration(cx, cy)
        "map_3b" -> drawMapSensorIllustration(cx, cy)
        "uaefi" -> drawEcuIllustration(cx, cy)
        "bosch_iat" -> drawIatSensorIllustration(cx, cy)
        "lsu_49" -> drawLambdaIllustration(cx, cy)
        "throttle" -> drawThrottleIllustration(cx, cy)
        else -> drawGenericSensorIllustration(cx, cy)
      }
    }
  }
}

// Vector graphic for VW R8 Red Ignition Coil
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVwCoilIllustration(cx: Float, cy: Float) {
  // Glow
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(TuningRed.copy(alpha = 0.25f), Color.Transparent),
      center = Offset(cx, cy),
      radius = 120f
    )
  )

  // Coil Red Head
  drawRoundRect(
    color = TuningRed,
    topLeft = Offset(cx - 50f, cy - 70f),
    size = Size(100f, 45f),
    cornerRadius = CornerRadius(8f, 8f)
  )
  // Head accent groove
  drawRoundRect(
    color = Color(0xFFB71C1C),
    topLeft = Offset(cx - 40f, cy - 65f),
    size = Size(80f, 12f),
    cornerRadius = CornerRadius(4f, 4f)
  )

  // 4-pin Connector housing
  drawRoundRect(
    color = Color(0xFF212529),
    topLeft = Offset(cx + 35f, cy - 68f),
    size = Size(35f, 30f),
    cornerRadius = CornerRadius(4f, 4f)
  )
  // 4 gold pins in connector
  for (i in 0..3) {
    drawRect(
      color = TunerAmber,
      topLeft = Offset(cx + 42f + (i * 6f), cy - 62f),
      size = Size(3f, 16f)
    )
  }

  // Metal / Rubber Stalk Shaft
  drawRoundRect(
    color = Color(0xFF495057),
    topLeft = Offset(cx - 22f, cy - 25f),
    size = Size(44f, 65f),
    cornerRadius = CornerRadius(4f, 4f)
  )

  // Stainless steel shield band
  drawRect(
    color = Color(0xFFCED4DA),
    topLeft = Offset(cx - 24f, cy - 10f),
    size = Size(48f, 28f)
  )

  // Lower Rubber Spark Plug Boot
  val bootPath = Path().apply {
    moveTo(cx - 20f, cy + 40f)
    lineTo(cx + 20f, cy + 40f)
    lineTo(cx + 15f, cy + 85f)
    lineTo(cx - 15f, cy + 85f)
    close()
  }
  drawPath(bootPath, color = Color(0xFF1E2124))

  // High Voltage Spark Pulse
  drawCircle(color = TunerCyan, radius = 6f, center = Offset(cx, cy + 88f))
}

// Vector graphic for Bosch 3 Bar MAP Sensor
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMapSensorIllustration(cx: Float, cy: Float) {
  // Pressure wave aura
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(TunerCyan.copy(alpha = 0.2f), Color.Transparent),
      center = Offset(cx, cy),
      radius = 110f
    )
  )

  // Main Black Body
  drawRoundRect(
    color = Color(0xFF1C1E24),
    topLeft = Offset(cx - 55f, cy - 40f),
    size = Size(110f, 60f),
    cornerRadius = CornerRadius(10f, 10f)
  )

  // Mounting Ear with Brass Bushing
  drawRoundRect(
    color = Color(0xFF2B303C),
    topLeft = Offset(cx + 35f, cy - 25f),
    size = Size(35f, 30f),
    cornerRadius = CornerRadius(6f, 6f)
  )
  drawCircle(color = Color(0xFFE5A93C), radius = 8f, center = Offset(cx + 52f, cy - 10f))
  drawCircle(color = CarbonBlack, radius = 5f, center = Offset(cx + 52f, cy - 10f))

  // Pressure Spigot / Probe (Bottom)
  drawRoundRect(
    color = Color(0xFF2B303C),
    topLeft = Offset(cx - 16f, cy + 20f),
    size = Size(32f, 45f),
    cornerRadius = CornerRadius(4f, 4f)
  )
  // Green Viton O-Ring
  drawRoundRect(
    color = Color(0xFF00E676),
    topLeft = Offset(cx - 19f, cy + 32f),
    size = Size(38f, 8f),
    cornerRadius = CornerRadius(3f, 3f)
  )

  // 4 Connector Pins at Left
  drawRoundRect(
    color = Color(0xFF12141A),
    topLeft = Offset(cx - 85f, cy - 25f),
    size = Size(32f, 32f),
    cornerRadius = CornerRadius(4f, 4f)
  )
  for (i in 0..3) {
    drawRect(
      color = Color(0xFFFFD54F),
      topLeft = Offset(cx - 78f, cy - 20f + (i * 7f)),
      size = Size(16f, 3f)
    )
  }

  // Bosch Label Plate
  drawRoundRect(
    color = TuningRed,
    topLeft = Offset(cx - 30f, cy - 25f),
    size = Size(45f, 20f),
    cornerRadius = CornerRadius(3f, 3f)
  )
}

// Vector graphic for UAEFI Standalone ECU
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEcuIllustration(cx: Float, cy: Float) {
  // Anodized ECU Enclosure
  drawRoundRect(
    color = Color(0xFF1A1F2C),
    topLeft = Offset(cx - 80f, cy - 50f),
    size = Size(160f, 100f),
    cornerRadius = CornerRadius(8f, 8f)
  )

  // Enclosure Bevel Outline
  drawRoundRect(
    color = TuningRed,
    topLeft = Offset(cx - 78f, cy - 48f),
    size = Size(156f, 96f),
    cornerRadius = CornerRadius(7f, 7f),
    style = Stroke(width = 2.5f)
  )

  // UAEFI Laser Engraving Emblem
  drawRoundRect(
    color = Color(0xFF262D3D),
    topLeft = Offset(cx - 50f, cy - 35f),
    size = Size(100f, 26f),
    cornerRadius = CornerRadius(4f, 4f)
  )
  // Circuit traces on ECU
  drawLine(
    color = TunerCyan.copy(alpha = 0.7f),
    start = Offset(cx - 40f, cy + 5f),
    end = Offset(cx + 40f, cy + 5f),
    strokeWidth = 2f
  )
  drawLine(
    color = TunerCyan.copy(alpha = 0.7f),
    start = Offset(cx - 20f, cy + 5f),
    end = Offset(cx - 20f, cy + 25f),
    strokeWidth = 2f
  )

  // Multi-pin Main Header Connector (Bottom)
  drawRoundRect(
    color = Color(0xFF0F1117),
    topLeft = Offset(cx - 65f, cy + 30f),
    size = Size(130f, 32f),
    cornerRadius = CornerRadius(4f, 4f)
  )
  // Pin Array Grid
  for (col in 0..11) {
    for (row in 0..1) {
      drawCircle(
        color = Color(0xFFFFC107),
        radius = 2f,
        center = Offset(cx - 55f + (col * 10f), cy + 38f + (row * 10f))
      )
    }
  }

  // Power & Status LEDs
  drawCircle(color = Color(0xFF00E676), radius = 3.5f, center = Offset(cx + 60f, cy - 35f))
  drawCircle(color = TunerAmber, radius = 3.5f, center = Offset(cx + 60f, cy - 25f))
}

// Vector graphic for Bosch IAT Open-Element Sensor
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIatSensorIllustration(cx: Float, cy: Float) {
  // Brass Hex Nut Body
  drawRoundRect(
    color = Color(0xFFD4AF37),
    topLeft = Offset(cx - 35f, cy - 15f),
    size = Size(70f, 30f),
    cornerRadius = CornerRadius(4f, 4f)
  )

  // M12 Threaded Shank
  drawRoundRect(
    color = Color(0xFFB8860B),
    topLeft = Offset(cx - 25f, cy + 15f),
    size = Size(50f, 35f),
    cornerRadius = CornerRadius(2f, 2f)
  )
  // Thread grooves
  for (i in 0..3) {
    drawLine(
      color = Color(0xFF996515),
      start = Offset(cx - 25f, cy + 22f + (i * 7f)),
      end = Offset(cx + 25f, cy + 22f + (i * 7f)),
      strokeWidth = 2f
    )
  }

  // Open Element Cage & Glass Bead NTC Thermistor
  drawRoundRect(
    color = Color(0xFF212529),
    topLeft = Offset(cx - 15f, cy + 50f),
    size = Size(30f, 30f),
    cornerRadius = CornerRadius(6f, 6f),
    style = Stroke(width = 3f)
  )
  // Exposed Red NTC Thermistor bead
  drawCircle(color = TuningRed, radius = 5f, center = Offset(cx, cy + 65f))

  // EV1 Connector Plug (Top)
  drawRoundRect(
    color = Color(0xFF263238),
    topLeft = Offset(cx - 28f, cy - 55f),
    size = Size(56f, 40f),
    cornerRadius = CornerRadius(6f, 6f)
  )
  // 2 Pins
  drawRect(color = Color(0xFFFFD54F), topLeft = Offset(cx - 12f, cy - 65f), size = Size(5f, 15f))
  drawRect(color = Color(0xFFFFD54F), topLeft = Offset(cx + 7f, cy - 65f), size = Size(5f, 15f))
}

// Vector graphic for Bosch LSU 4.9 Lambda Sensor
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLambdaIllustration(cx: Float, cy: Float) {
  // Stainless Hexagonal Body
  drawRoundRect(
    color = Color(0xFF90A4AE),
    topLeft = Offset(cx - 25f, cy - 20f),
    size = Size(50f, 40f),
    cornerRadius = CornerRadius(4f, 4f)
  )

  // Perforated Sensing Tip
  drawRoundRect(
    color = Color(0xFF607D8B),
    topLeft = Offset(cx - 16f, cy + 20f),
    size = Size(32f, 50f),
    cornerRadius = CornerRadius(8f, 8f)
  )
  // Holes in tip
  for (i in 0..2) {
    drawCircle(color = Color(0xFF263238), radius = 3f, center = Offset(cx - 7f, cy + 30f + (i * 10f)))
    drawCircle(color = Color(0xFF263238), radius = 3f, center = Offset(cx + 7f, cy + 30f + (i * 10f)))
  }

  // Flexible Heat Sleeve & Wiring Harness (Top)
  drawRoundRect(
    color = Color(0xFF263238),
    topLeft = Offset(cx - 12f, cy - 65f),
    size = Size(24f, 45f),
    cornerRadius = CornerRadius(4f, 4f)
  )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGenericSensorIllustration(cx: Float, cy: Float) {
  drawRoundRect(
    color = Color(0xFF262C3D),
    topLeft = Offset(cx - 45f, cy - 45f),
    size = Size(90f, 90f),
    cornerRadius = CornerRadius(12f, 12f)
  )
  drawCircle(color = TunerCyan, radius = 20f, center = Offset(cx, cy))
}

// Vector graphic for Throttle Body (Przepustnica DBW)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawThrottleIllustration(cx: Float, cy: Float) {
  // Glow
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(TunerCyan.copy(alpha = 0.2f), Color.Transparent),
      center = Offset(cx, cy),
      radius = 120f
    )
  )

  // Outer Flange Body
  drawRoundRect(
    color = Color(0xFF1E2330),
    topLeft = Offset(cx - 62f, cy - 62f),
    size = Size(124f, 124f),
    cornerRadius = CornerRadius(14f, 14f)
  )
  drawRoundRect(
    color = Color(0xFF323B50),
    topLeft = Offset(cx - 62f, cy - 62f),
    size = Size(124f, 124f),
    cornerRadius = CornerRadius(14f, 14f),
    style = Stroke(width = 2f)
  )

  // 4 Corner Bolt Holes
  val boltOffsets = listOf(
    Offset(cx - 46f, cy - 46f),
    Offset(cx + 46f, cy - 46f),
    Offset(cx + 46f, cy + 46f),
    Offset(cx - 46f, cy + 46f)
  )
  boltOffsets.forEach { pt ->
    drawCircle(color = Color(0xFF0C0E12), radius = 7f, center = pt)
    drawCircle(color = Color(0xFF34C759), radius = 7f, center = pt, style = Stroke(width = 2f))
  }

  // Outer Inlet Lip (dashed)
  drawCircle(
    color = TunerAmber,
    radius = 48f,
    center = Offset(cx, cy),
    style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f))
  )

  // Inner Throttle Bore
  drawCircle(color = Color(0xFF0C0E12), radius = 38f, center = Offset(cx, cy))
  drawCircle(color = TunerCyan, radius = 38f, center = Offset(cx, cy), style = Stroke(width = 3f))

  // Butterfly Throttle Plate (Brass/Amber Axle)
  drawLine(
    color = TunerAmber,
    start = Offset(cx, cy - 36f),
    end = Offset(cx, cy + 36f),
    strokeWidth = 3.5f,
    cap = StrokeCap.Round
  )
  drawCircle(color = Color.White, radius = 2.5f, center = Offset(cx, cy - 14f))
  drawCircle(color = Color.White, radius = 2.5f, center = Offset(cx, cy + 14f))

  // Drive-by-wire servo motor housing on the right
  drawRoundRect(
    color = Color(0xFF262C3D),
    topLeft = Offset(cx + 60f, cy - 35f),
    size = Size(25f, 70f),
    cornerRadius = CornerRadius(6f, 6f)
  )
  drawRoundRect(
    color = TunerCyan,
    topLeft = Offset(cx + 60f, cy - 35f),
    size = Size(25f, 70f),
    cornerRadius = CornerRadius(6f, 6f),
    style = Stroke(width = 1.5f)
  )
}

