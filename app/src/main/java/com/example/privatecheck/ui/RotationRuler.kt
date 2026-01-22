package com.example.privatecheck.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun RotationRuler(
    rotation: Float,
    onRotationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRotation by rememberUpdatedState(rotation)
    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Sensitivity: 2.5 pixels = 1 degree (Faster)
                    val delta = dragAmount.x / 2.5f 
                    // Clamp to -180..180 range (Hard Limit)
                    // Inverted direction: Dragging left should rotate right (or vice versa depending on preference, user asked to "invert").
                    // If previously it was +, now -
                    val newAngle = (currentRotation - delta).coerceIn(-180f, 180f)
                    onRotationChange(newAngle) 
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Draw Ticks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            
            val centerY = height / 2f
            
            
            // Draw Scrollable Ticks
            
            // Draw Scrollable Ticks
            // We draw ticks relative to the current rotation
            // Range visible: +/- 20 degrees maybe? ~40 degrees total width?
            // If 1 degree = 5px (from drag logic above), then screen width controls visible range.
            
            // Config
            val pixelsPerDegree = 2.dp.toPx() // Narrower spacing (Half length)
            
            val currentAngle = rotation
            
            // Draw Range
            val halfWidthDeg = (width / 2f) / pixelsPerDegree
            val startDeg = (currentAngle - halfWidthDeg).roundToInt()
            val endDeg = (currentAngle + halfWidthDeg).roundToInt()
            
            for (i in startDeg..endDeg) {
                // Range Limit
                if (i < -180 || i > 180) continue

                // Density: Draw every 5 degrees (Cleaner)
                if (i % 5 == 0) {
                     val offsetFromCenter = (i - currentAngle) * pixelsPerDegree
                     val x = centerX + offsetFromCenter
                     
                     // Style
                     val isBigTick = (i == 0 || i == 180 || i == -180)
                     
                     // Dynamic Height Logic
                     // 1. Base height based on distance from center (Fisheye effect)
                     val dist = kotlin.math.abs(x - centerX)
                     val maxDist = width / 2f
                     val proximity = (1f - (dist / maxDist)).coerceIn(0f, 1f)
                     // Quadratic falloff for "sharper" peak near center
                     val heightFactor = proximity * proximity 
                     
                     // Dynamic Height Logic
                     val baseH = height * 0.1f // Shortest at edges (User req: "shorter")
                     val maxH = height * 0.75f   // Tallest at center (User req: "just a bit shorter than 0deg line")
                     
                     // Quadratic falloff for "sharper" peak near center
                     // maxH corresponds to proximity = 1.0 (Center)
                     val rawHeight = baseH + (maxH - baseH) * heightFactor
                     
                     var tickHeight = rawHeight
                     
                     // 2. Big vs Small Ticks
                     if (isBigTick) {
                         tickHeight = maxH // Always max height, no scaling
                     } else {
                         tickHeight *= 0.5f // Small ticks are half the height of big ticks at the same position
                     }

                     val tickColor = if (isBigTick) Color.White else Color.Gray.copy(alpha = 0.6f + 0.4f * heightFactor)
                     val stroke = if (isBigTick) 2.dp.toPx() else 1.dp.toPx()

                     drawLine(
                        color = tickColor,
                        start = Offset(x, centerY - tickHeight / 2),
                        end = Offset(x, centerY + tickHeight / 2),
                        strokeWidth = stroke
                     )
                }
            }
            
            // Draw Center Indicator (Fixed) - Drawn LAST to overlap ticks
            drawLine(
                color = primaryColor, // Active Color
                start = Offset(centerX, centerY - height * 0.4f),
                end = Offset(centerX, centerY + height * 0.4f),
                strokeWidth = 3.dp.toPx()
            )
        }
        
        // Value Text Overlay 
        Text(
            text = "${rotation.roundToInt()}°",
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            style = TextStyle(fontSize = 12.sp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )
    }
}
