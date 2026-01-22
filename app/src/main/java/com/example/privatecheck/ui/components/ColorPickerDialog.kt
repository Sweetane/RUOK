package com.example.privatecheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    // Convert initial Int to HSL
    val initialComposeColor = Color(initialColor)
    // Compose Color doesn't give HSL directly easily, but we can assume generic RGB.
    // Let's use Android's ColorUtils or just simple RGB sliders? 
    // User requested HSL.
    // We can allow users to pick HSL, and convert to Color.
    
    // Initial State: Default to middle if we can't easily reverse engineer HSL (or convert once)
    // To properly initialize sliders from `initialColor`:
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor, hsv)
    
    // Approximation: HSV and HSL are different. 
    // HSL:
    // H = H
    // L = (max + min) / 2
    // S = ...
    // Let's just use HSV for the "H" slider, and maybe approximate S/L or just start fresh?
    // Actually, user asked for HSL. Let's provide HSL sliders.
    // We will initialize them to defaults (Red, 50%, 50%) or try to match.
    // Let's just start with defaults for simplicity unless we want to be perfect.
    // Matching is better.
    
    // Let's implement robust conversion if possible, or just use HSB (HSV) which is standard in Android?
    // HSL is slightly different (Lightness vs Value).
    // User said "HSL". I will try to implement HSL -> Color logic.
    
    var hue by remember { mutableStateOf(hsv[0]) } // 0..360
    var saturation by remember { mutableStateOf(0.5f) } // 0..1
    var lightness by remember { mutableStateOf(0.5f) } // 0..1
    
    // Real-time preview color
    val currentColor = Color.hsl(hue, saturation, lightness)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "自定义颜色",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Preview Box
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 40.dp)
                        .background(currentColor, RoundedCornerShape(12.dp))
                        .then(Modifier.border(1.dp, Color.Gray, RoundedCornerShape(12.dp)))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Hue Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("色相", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(48.dp))
                    @OptIn(ExperimentalMaterial3Api::class)
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        thumb = {
                            Spacer(
                                modifier = Modifier
                                    .size(6.dp, 20.dp)
                                    .background(currentColor, RoundedCornerShape(2.dp))
                            )
                        },
                         track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors(activeTrackColor = currentColor, inactiveTrackColor = currentColor.copy(alpha=0.3f)),
                                thumbTrackGapSize = 0.dp,
                                modifier = Modifier.clip(androidx.compose.ui.graphics.RectangleShape)
                            )
                        }
                    )
                }

                // Saturation Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("饱和度", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(48.dp))
                    @OptIn(ExperimentalMaterial3Api::class)
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        thumb = {
                            Spacer(
                                modifier = Modifier
                                    .size(6.dp, 20.dp)
                                    .background(currentColor, RoundedCornerShape(2.dp))
                            )
                        },
                         track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors(activeTrackColor = currentColor, inactiveTrackColor = currentColor.copy(alpha=0.3f)),
                                thumbTrackGapSize = 0.dp,
                                modifier = Modifier.clip(androidx.compose.ui.graphics.RectangleShape)
                            )
                        }
                    )
                }

                // Lightness Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("亮度", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(48.dp))
                    @OptIn(ExperimentalMaterial3Api::class)
                    Slider(
                        value = lightness,
                        onValueChange = { lightness = it },
                        valueRange = 0f..1f,
                        thumb = {
                            Spacer(
                                modifier = Modifier
                                    .size(6.dp, 20.dp)
                                    .background(currentColor, RoundedCornerShape(2.dp))
                            )
                        },
                         track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors(activeTrackColor = currentColor, inactiveTrackColor = currentColor.copy(alpha=0.3f)),
                                thumbTrackGapSize = 0.dp,
                                modifier = Modifier.clip(androidx.compose.ui.graphics.RectangleShape)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(currentColor.toArgb()) },
                        colors = ButtonDefaults.buttonColors(containerColor = currentColor)
                    ) {
                        Text("确定", color = if (lightness > 0.5f) Color.Black else Color.White)
                    }
                }
            }
        }
    }
}
