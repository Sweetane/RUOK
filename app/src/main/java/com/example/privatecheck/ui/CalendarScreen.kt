package com.example.privatecheck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privatecheck.data.DataStoreRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    repository: DataStoreRepository,
    backgroundColor: Color,
    onBack: () -> Unit
) {
    val history by repository.checkInHistory.collectAsState(initial = emptySet())
    val appThemeColorInt by repository.appThemeColor.collectAsState(initial = -7357297)
    val themeColor = Color(appThemeColorInt)

    // Year State
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }
    val currentYear = LocalDate.now().year
    val minYear = 2023

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("打卡历程", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Year Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                IconButton(
                    onClick = { if (selectedYear > minYear) selectedYear-- },
                    enabled = selectedYear > minYear
                ) {
                    Icon(
                        Icons.Default.ChevronLeft, 
                        contentDescription = "Previous Year",
                        tint = if (selectedYear > minYear) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                
                Text(
                    text = "$selectedYear",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                IconButton(
                    onClick = { if (selectedYear < currentYear) selectedYear++ },
                    enabled = selectedYear < currentYear
                ) {
                    Icon(
                        Icons.Default.ChevronRight, 
                        contentDescription = "Next Year",
                        tint = if (selectedYear < currentYear) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            HeatmapCalendar(
                history = history,
                themeColor = themeColor,
                year = selectedYear,
                isDark = MaterialTheme.colorScheme.brightness == Brightness.Dark
            )
        }
    }
}

private enum class Brightness { Light, Dark }
private val ColorScheme.brightness: Brightness
    get() = if (surface.luminance() < 0.5f) Brightness.Dark else Brightness.Light

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

@Composable
fun HeatmapCalendar(
    history: Set<String>,
    themeColor: Color,
    year: Int,
    isDark: Boolean
) {
    val startDate = LocalDate.of(year, 1, 1)
    val totalDays = startDate.lengthOfYear() // 365 or 366
    
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Pre-process history: "YYYY-MM-DD|HH:mm" -> Map<"YYYY-MM-DD", "HH:mm">
    // If multiple entries exist for same day (shouldn't happen with set + logic), take latest.
    val historyMap = remember(history) {
        history.associate { entry ->
            if (entry.contains("|")) {
                val parts = entry.split("|")
                parts[0] to parts[1]
            } else {
                entry to "" // Old data format
            }
        }
    }

    Column {
        LazyVerticalGrid(
            columns = GridCells.Fixed(21),
            contentPadding = PaddingValues(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(totalDays) { index ->
                val date = startDate.plusDays(index.toLong())
                val dateStr = date.format(formatter)
                val isCheckedIn = historyMap.containsKey(dateStr)
                
                // Color Logic
                val boxColor = if (isCheckedIn) {
                    val timeStr = historyMap[dateStr]
                    if (!timeStr.isNullOrEmpty()) {
                        calculateCheckInColor(timeStr)
                    } else {
                        // Fallback for old data or missing time: Use standard Green or Theme Color?
                        // User said "remove theme control", but for old data maybe a neutral positive is best.
                        // Let's use a nice default "Day" color (Green) to be safe, or just keep Theme for legacy.
                        // Given the request, let's stick to a safe default like the "Morning Green".
                        Color(0xFF4CAF50) 
                    }
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(
                            color = boxColor,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

// Time-based Gradient Logic
private fun calculateCheckInColor(timeStr: String): Color {
    try {
        val parts = timeStr.split(":")
        val hour = parts[0].toInt() + parts[1].toInt() / 60f
        
        // Gradient Key Points
        // 00:00 - 05:00: Deep Purple (0xFF673AB7)
        // 05:00 - 11:00: -> Green (0xFF4CAF50)
        // 11:00 - 13:00: -> Red (0xFFF44336)
        // 13:00 - 17:00: -> Orange (0xFFFF9800)
        // 17:00 - 20:00: -> Blue (0xFF2196F3)
        // 20:00 - 24:00: -> Deep Blue (0xFF3F51B5) -> Deep Purple
        
        return when {
            hour < 5 -> lerpColor(Color(0xFF673AB7), Color(0xFF4CAF50), hour / 5f) // Deep Purple -> Green (Early Morning) - Wait, concept says 0-5 is Deep Purple. 5-11 is Green.
            // Let's follow the user's description exactly:
            // Midnight(0) Deep Purple -> Morning(5-11) Green -> Noon(11-13) Red -> Afternoon(13-17) Orange -> Evening(17-20) Blue -> Late(20-24) Deep Blue -> Midnight
            
            // Refined Interpolation:
            hour < 5 -> Color(0xFF673AB7) // 0-5: Static Deep Purple (or slight gradient?) Let's keep it steady or strictly interpolation.
            // Actually "Morning gradient into Green". So 0 -> 5 transition? 
            // Let's do linear interpolation between keyframes.
            
            // Keyframes:
            // 0.0: 0xFF4527A0 (Deep Purple)
            // 5.0: 0xFF4CAF50 (Green) ?? No, 5am is too early for bright green. 
            // User: "Midnight Deep Purple, Morning gradient to Green, Noon Red..."
            
            // Let's interpret:
            // 00:00 : Deep Purple 
            // 05:00 : Start of Morning (Maybe Teal/Cyan?) -> 11:00 Green? No "Morning gradient to Green" usually means 5->11 is the transition.
            
            // 0  -> 5 : Deep Purple -> Green (Transition 1)
            // 5  -> 11: Green -> Green (Steady?) or Green -> Red? 
            // "Morning gradient to Green" -> implies end state is green.
            // "Noon become Red" -> 11->13 transition.
            
            // Let's try this mapping:
            // 00:00 : Deep Purple (0xFF4527A0)
            // 06:00 : Green (0xFF4CAF50)
            // 11:00 : Red (0xFFD32F2F)
            // 12:00 : Red (Peak)
            // 13:00 : Red -> Orange
            // 15:00 : Orange (0xFFFF9800)
            // 17:00 : Blue (0xFF2196F3)
            // 20:00 : Deep Blue (0xFF1A237E)
            // 24:00 : Deep Purple (0xFF4527A0)
            
            hour < 6 -> lerpColor(Color(0xFF4527A0), Color(0xFF4CAF50), hour / 6f)
            hour < 11 -> lerpColor(Color(0xFF4CAF50), Color(0xFFD32F2F), (hour - 6) / 5f) // Green -> Red
            hour < 13 -> Color(0xFFD32F2F) // Noon Red (Steady)
            hour < 17 -> lerpColor(Color(0xFFD32F2F), Color(0xFFFF9800), (hour - 13) / 4f) // Red -> Orange
            hour < 19 -> lerpColor(Color(0xFFFF9800), Color(0xFF2196F3), (hour - 17) / 2f) // Orange -> Blue
            hour < 22 -> lerpColor(Color(0xFF2196F3), Color(0xFF1A237E), (hour - 19) / 3f) // Blue -> Deep Blue
            else -> lerpColor(Color(0xFF1A237E), Color(0xFF4527A0), (hour - 22) / 2f) // Deep Blue -> Deep Purple
        }
    } catch (e: Exception) {
        return Color(0xFF4CAF50) // Default Green
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return androidx.compose.ui.graphics.lerp(start, end, f)
}
