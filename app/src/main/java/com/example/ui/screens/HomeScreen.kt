package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ComponentCategory
import com.example.model.TuningComponent
import com.example.ui.components.ComponentThumbnail
import com.example.ui.components.VectorIllustration
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TunerAmber
import com.example.ui.theme.TunerCyan
import com.example.ui.theme.TuningRed
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  components: List<TuningComponent>,
  userNick: String,
  onComponentClick: (TuningComponent) -> Unit,
  onAddClick: () -> Unit,
  onOpenNickDialog: () -> Unit,
  onCheckUpdate: () -> Unit = {},
  onSignOut: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(ComponentCategory.ALL) }

  // Filter components based on search query (name or part number) and category
  val filteredComponents = remember(components, searchQuery, selectedCategory) {
    val q = searchQuery.trim().lowercase()
    components.filter { comp ->
      val matchesCategory = selectedCategory == ComponentCategory.ALL || comp.category == selectedCategory
      val matchesQuery = q.isEmpty() ||
        comp.name.lowercase().contains(q) ||
        comp.partNumber.lowercase().contains(q) ||
        comp.shortDescription.lowercase().contains(q) ||
        comp.authorNick.lowercase().contains(q)
      matchesCategory && matchesQuery
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = CarbonBlack,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DarkSurface,
          titleContentColor = TextPrimary
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .background(TuningRed, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(Modifier.width(10.dp))
            Column {
              Text(
                text = "TUNING SENSOR",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextPrimary
              )
              Text(
                text = "Baza wiedzy & pinoutów ECU",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }
        },
        actions = {
          // Nickname indicator button
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(DarkSurfaceVariant)
              .clickable { onOpenNickDialog() }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("nick_button"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Zmień nick",
              tint = TunerAmber,
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = userNick,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Spacer(Modifier.width(4.dp))

          // Przycisk sprawdzania aktualizacji po prawej stronie obok nicku
          IconButton(
            onClick = onCheckUpdate,
            modifier = Modifier
              .size(36.dp)
              .testTag("check_update_button")
          ) {
            Icon(
              imageVector = Icons.Default.SystemUpdate,
              contentDescription = "Sprawdź aktualizacje",
              tint = TunerCyan,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(Modifier.width(2.dp))

          IconButton(
            onClick = onSignOut,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Wyloguj",
              tint = TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = onAddClick,
        containerColor = TuningRed,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_add_sensor")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Dodaj czujnik lub sterownik")
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 1. TOP SEARCH BAR
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sensor_search_field"),
          placeholder = {
            Text(
              text = "Wpisz numer (np. 06E 905 115, 0 281 002) lub nazwę...",
              fontSize = 13.sp,
              color = TextMuted
            )
          },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Szukaj", tint = TunerCyan)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Wyczyść", tint = TextSecondary)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TuningRed,
            unfocusedBorderColor = BorderStroke,
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        Spacer(Modifier.height(10.dp))

        // Category filter chips row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ComponentCategory.values().forEach { cat ->
            val isSelected = selectedCategory == cat
            FilterChip(
              selected = isSelected,
              onClick = { selectedCategory = cat },
              label = {
                Text(
                  text = cat.displayName,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TuningRed,
                selectedLabelColor = Color.White,
                containerColor = DarkSurfaceElevated,
                labelColor = TextSecondary
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isSelected,
                borderColor = BorderStroke,
                selectedBorderColor = TuningRed
              )
            )
          }
        }
      }

      // 2. Results Header / Counter
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (searchQuery.isBlank()) "Dostępne pozycje w bazie (${filteredComponents.size})" else "Znalezione wyniki (${filteredComponents.size})",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )

        if (filteredComponents.size > 1 && searchQuery.isNotBlank()) {
          Text(
            text = "Wybierz podzespół z listy poniżej",
            fontSize = 12.sp,
            color = TunerCyan
          )
        }
      }

      // 3. Components List
      if (filteredComponents.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = TextMuted,
              modifier = Modifier.size(54.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
              text = "Nie znaleziono takiego czujnika ani sterownika",
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              fontSize = 16.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
              text = "Sprawdź wpisany numer części lub dodaj własny wpis do bazy klikając '+' w prawym górnym rogu!",
              color = TextSecondary,
              fontSize = 13.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(filteredComponents, key = { it.id }) { component ->
            ComponentListCard(
              component = component,
              onClick = { onComponentClick(component) }
            )
          }
          item {
            Spacer(Modifier.height(72.dp)) // padding for FAB
          }
        }
      }
    }
  }
}

@Composable
fun ComponentListCard(
  component: TuningComponent,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("component_card_${component.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Mini visual thumbnail using the first photo from images (or imageUrl)
      Box(modifier = Modifier.size(76.dp)) {
        ComponentThumbnail(
          imageUrl = component.primaryImage,
          vectorKey = component.vectorIllustrationKey,
          modifier = Modifier.size(76.dp)
        )
        if (component.allImages().size > 1) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(3.dp)
              .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
              .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Text(
              text = "📷 ${component.allImages().size}",
              fontSize = 9.sp,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Category badge
          Box(
            modifier = Modifier
              .background(TuningRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = component.category.displayName,
              color = TuningRed,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp
            )
          }

          // Rating badge
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = TunerAmber, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text(
              text = String.format(Locale.US, "%.1f", component.rating),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = TextPrimary
            )
          }
        }

        Spacer(Modifier.height(4.dp))

        // Component Name
        Text(
          text = component.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        // Part Number (Monospace)
        if (component.partNumber.isNotBlank()) {
          Text(
            text = component.partNumber,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TunerCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(Modifier.height(6.dp))

        // Meta info chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Number of pins
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ElectricalServices, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(3.dp))
            Text(
              text = "${component.pinout.size} pin${if (component.pinout.size == 1) "" else if (component.pinout.size in 2..4) "y" else "ów"}",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }

          // Pullup badge if required
          if (component.pullupRequired) {
            Spacer(Modifier.width(8.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .background(TunerAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = TunerAmber, modifier = Modifier.size(11.dp))
              Spacer(Modifier.width(3.dp))
              Text("Pull-up", fontSize = 10.sp, color = TunerAmber, fontWeight = FontWeight.SemiBold)
            }
          }

          Spacer(Modifier.weight(1f))

          // Author Nick
          Text(
            text = "@${component.authorNick}",
            fontSize = 11.sp,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}
