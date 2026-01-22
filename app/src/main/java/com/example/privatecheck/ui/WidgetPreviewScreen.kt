package com.example.privatecheck.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.privatecheck.data.DataStoreRepository
import com.example.privatecheck.widget.CheckInWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.File
import java.io.FileInputStream
import kotlin.math.roundToInt

@Composable
fun WidgetPreviewScreen(
    imageUri: String,
    repository: DataStoreRepository,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- State for Transformations ---
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // --- State for Adjustments ---
    var contrast by remember { mutableFloatStateOf(1f) }   // 0.5 to 1.5
    var whiteScrim by remember { mutableFloatStateOf(0f) } // 0.0 to 1.0 (Brightness)

    // Load original bitmap size for calculations
    var originalBitmapSize by remember { mutableStateOf<android.util.Size?>(null) }
    var previewBoxWidth by remember { mutableFloatStateOf(1f) } 
    
    // Restore Saved State
    LaunchedEffect(Unit) {
        val savedScale = repository.widgetScale.firstOrNull() ?: 1f
        val savedRotation = repository.widgetRotation.firstOrNull() ?: 0f
        val savedOffsetX = repository.widgetOffsetX.firstOrNull() ?: 0f
        val savedOffsetY = repository.widgetOffsetY.firstOrNull() ?: 0f
        val savedContrast = repository.widgetContrast.firstOrNull() ?: 1f
        val savedWhiteScrim = repository.widgetWhiteScrim.firstOrNull() ?: 0f
        
        scale = savedScale
        rotation = savedRotation
        offset = Offset(savedOffsetX, savedOffsetY)
        contrast = savedContrast
        whiteScrim = savedWhiteScrim
    }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                 val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                 val uri = Uri.parse(imageUri)
                 val inputStream = if (uri.scheme == "content" || uri.scheme == "file") {
                     context.contentResolver.openInputStream(uri)
                 } else {
                     // Assume raw path
                     FileInputStream(java.io.File(imageUri))
                 }
                 
                 inputStream?.use { 
                     BitmapFactory.decodeStream(it, null, options)
                 }
                 originalBitmapSize = android.util.Size(options.outWidth, options.outHeight)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ColorMatrix for Brightness
    // ColorMatrix for Adjustments
    val colorMatrix = remember(contrast, whiteScrim) {
        val contrastMatrix = android.graphics.ColorMatrix()
        // 1. Contrast
        val t = (1f - contrast) * 128f
        contrastMatrix.set(floatArrayOf(
            contrast, 0f, 0f, 0f, t,
            0f, contrast, 0f, 0f, t,
            0f, 0f, contrast, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // 2. White Scrim (Brightness Add)
        // Adds white offset: pixel + (scrim * 100)
        // Range 0..1 -> 0..100 offset (Or 255 for full white?)
        // User wants "White Curtain", maybe 100 is enough for readability without washing out completely
        // Let's try 0..150 range for effect.
        val b = whiteScrim * 150f 
        val brightnessMatrix = android.graphics.ColorMatrix(floatArrayOf(
             1f, 0f, 0f, 0f, b,
             0f, 1f, 0f, 0f, b,
             0f, 0f, 1f, 0f, b,
             0f, 0f, 0f, 1f, 0f
        ))
        
        // Apply: Contrast first, then Brightness
        val combined = android.graphics.ColorMatrix()
        combined.setConcat(brightnessMatrix, contrastMatrix)
        
        ColorMatrix(combined.array)
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
             // Controls Area (Bottom-fixed)
             Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RotationRuler(
                        rotation = rotation,
                        onRotationChange = { rotation = it },
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = { rotation = 0f }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Rotation",
                            tint = Color.White
                        )
                    }
                }

                AdjustmentRow(
                    icon = Icons.Default.Exposure,
                    label = "对比度",
                    value = contrast,
                    onValueChange = { contrast = it },
                    valueRange = 0.5f..1.5f
                )

                AdjustmentRow(
                    icon = Icons.Default.BrightnessLow,
                    label = "白色幕布",
                    value = whiteScrim,
                    onValueChange = { whiteScrim = it },
                    valueRange = 0f..1f
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val processedPath = withContext(Dispatchers.IO) {
                                        // 1. Decode Source
                                        val sourceBitamp = try {
                                            val uri = Uri.parse(imageUri)
                                            val inputStream = if (uri.scheme == "content" || uri.scheme == "file") {
                                                context.contentResolver.openInputStream(uri)
                                            } else {
                                                FileInputStream(java.io.File(imageUri))
                                            }
                                            val bitmap = BitmapFactory.decodeStream(inputStream)
                                            inputStream?.close()
                                            bitmap
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            null
                                        }

                                        if (sourceBitamp != null) {
                                            // 2. Create Target Bitmap (2:1 Ratio, e.g. 1000x500)
                                            val targetWidth = 1000
                                            val targetHeight = 500
                                            val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                                            val canvas = Canvas(resultBitmap)
                                            val paint = Paint()

                                            // 3. Setup Color Filter
                                            val cm = android.graphics.ColorMatrix()
                                            
                                            // A. Contrast
                                            val t = (1f - contrast) * 128f
                                            val contrastMatrix = android.graphics.ColorMatrix(floatArrayOf(
                                                contrast, 0f, 0f, 0f, t,
                                                0f, contrast, 0f, 0f, t,
                                                0f, 0f, contrast, 0f, t,
                                                0f, 0f, 0f, 1f, 0f
                                            ))
                                            
                                            // B. Brightness (White Scrim)
                                            val b = whiteScrim * 150f
                                            val brightnessMatrix = android.graphics.ColorMatrix(floatArrayOf(
                                                1f, 0f, 0f, 0f, b,
                                                0f, 1f, 0f, 0f, b,
                                                0f, 0f, 1f, 0f, b,
                                                0f, 0f, 0f, 1f, 0f
                                            ))
                                            
                                            // Combine
                                            val combined = android.graphics.ColorMatrix()
                                            combined.setConcat(brightnessMatrix, contrastMatrix)

                                            paint.colorFilter = android.graphics.ColorMatrixColorFilter(combined)
                                            // paint.alpha is default 255 (opaque)
                                            
                                            // 4. Calculate Matrix
                                            // The logic here attempts to replicate the Compose transforms (offset, scale, rotation)
                                            // onto the canvas. 
                                            // NB: Compose transforms are applied to the Image, which is fitted/centered in the Box.
                                            // The Box is 2:1. The Image is 'Fit' or 'Crop'?
                                            // In UI below: we use ContentScale.Fit inside the Box, but we allow unlimited Scaling/Panning.
                                            // So we need to map: "Center of Source" -> "Center of Target" + Transforms.
                                            
                                            // A. Calculate Base Scale (ContentScale.Fit)
                                            // We fit source into 1000x500
                                            val viewRatio = targetWidth.toFloat() / targetHeight
                                            val sourceRatio = sourceBitamp.width.toFloat() / sourceBitamp.height
                                            
                                            // Scale to fit
                                            val baseScale = if (sourceRatio > viewRatio) {
                                                targetWidth.toFloat() / sourceBitamp.width
                                            } else {
                                                targetHeight.toFloat() / sourceBitamp.height
                                            }
                                            
                                            // Center offsets
                                            val finalW = sourceBitamp.width * baseScale
                                            val finalH = sourceBitamp.height * baseScale
                                            val dx = (targetWidth - finalW) / 2f
                                            val dy = (targetHeight - finalH) / 2f

                                            val matrix = android.graphics.Matrix()
                                            
                                            // 1. Apply Base Fit
                                            matrix.postScale(baseScale, baseScale)
                                            matrix.postTranslate(dx, dy)
                                            
                                            // 2. Apply User Transforms (Scale/Rotate around center, Pan)
                                            val cx = targetWidth / 2f
                                            val cy = targetHeight / 2f
                                            
                                            matrix.postScale(scale, scale, cx, cy)
                                            matrix.postRotate(rotation, cx, cy)
                                            
                                            // 3. Pan
                                            // Note: User offset is in Screen Pixels. 
                                            // We assume roughly 1:1 density mapping for simplicity or we could normalize.
                                            // For a robust widget, this approximation is usually sufficient.
                                            
                                            // Scaling offset from Screen Coords -> Bitmap Coords
                                            val scaleFactor = targetWidth.toFloat() / previewBoxWidth
                                            matrix.postTranslate(offset.x * scaleFactor, offset.y * scaleFactor)
                                            
                                            // 5. Draw
                                            canvas.drawColor(android.graphics.Color.BLACK) // Background
                                            canvas.drawBitmap(sourceBitamp, matrix, paint)
                                            
                                            // 6. Save
                                            val file = java.io.File(context.filesDir, "widget_bg_final.jpg")
                                            val fos = FileOutputStream(file)
                                            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                                            fos.close()
                                            file.absolutePath
                                        } else {
                                            null
                                        }
                                    }

                                    if (processedPath != null) {
                                        repository.saveWidgetImageAdjustments(
                                            scale = scale,
                                            rotation = rotation,
                                            offsetX = offset.x,
                                            offsetY = offset.y,
                                            contrast = contrast,
                                            whiteScrim = whiteScrim
                                        )
                                        repository.saveWidgetSettings("image", -1, processedPath)

                                        val manager = GlanceAppWidgetManager(context)
                                        val glanceIds = manager.getGlanceIds(CheckInWidget::class.java)
                                        glanceIds.forEach { glanceId ->
                                            updateAppWidgetState(context, glanceId) { prefs ->
                                                prefs[stringPreferencesKey("key_widget_bg_type")] = "image"
                                                prefs[stringPreferencesKey("key_widget_bg_image_uri")] = processedPath
                                            }
                                            CheckInWidget().update(context, glanceId)
                                        }
                                        Toast.makeText(context, "设置已更新", Toast.LENGTH_SHORT).show()
                                        onSave()
                                    } else {
                                        Toast.makeText(context, "处理失败", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "保存失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale *= zoom
                        offset += pan
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Mask / Crop Frame (2:1 Ratio)
            // The user manipulates the image *inside/behind* this frame.
            // Or actually, commonly, the user moves the image *relative* to the fixed frame.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .onGloballyPositioned { coordinates ->
                        previewBoxWidth = coordinates.size.width.toFloat()
                    }
                    .border(2.dp, Color.White)
                    .clipToBounds() // Visual clip
            ) {
                // The Image
                if (originalBitmapSize != null) {
                    val model = if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) 
                                    Uri.parse(imageUri) 
                                else java.io.File(imageUri)
                                
                    Image(
                        painter = rememberAsyncImagePainter(model = model),
                        contentDescription = "Editing Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                                rotationZ = rotation
                            },
                        // We use Fit so users can see the whole image initially and zoom in.
                        // Or maybe None? Fit is safer start.
                        contentScale = ContentScale.Fit, 
                        colorFilter = ColorFilter.colorMatrix(colorMatrix)
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                

            }
            
            // Helper Text
            Text(
                "单指移动 • 双指缩放",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp)
            )
        }
    }
}

@Composable
fun AdjustmentRow(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 12.sp)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.Gray
                )
            )
        }
    }
}
