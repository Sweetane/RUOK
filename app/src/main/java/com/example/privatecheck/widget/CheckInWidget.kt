package com.example.privatecheck.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privatecheck.data.DataStoreRepository
import com.example.privatecheck.logic.CheckInManager
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

// 1. The Widget Layout
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.state.updateAppWidgetState

import com.example.privatecheck.data.Quotes

// Helper to Render Custom Font to Bitmap
fun createTextBitmap(context: Context, text: String, fontSizeSp: Float): android.graphics.Bitmap {
    val paint = android.text.TextPaint().apply {
        isAntiAlias = true
        textSize = fontSizeSp * context.resources.displayMetrics.scaledDensity
        color = android.graphics.Color.BLACK
        try {
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.privatecheck.R.font.huangkaihua_lawyer_font)
        } catch (e: Exception) {
            e.printStackTrace()
            typeface = android.graphics.Typeface.SERIF 
        }
        letterSpacing = 0.1f 
    }

    // Use a large bounds for layout calculation, but we will crop later
    val tempMaxWidth = (1000 * context.resources.displayMetrics.density).toInt()
    
    val alignment = android.text.Layout.Alignment.ALIGN_CENTER
    val spacingMult = 1.0f
    val spacingAdd = 0f
    val includePad = false

    val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        android.text.StaticLayout.Builder.obtain(text, 0, text.length, paint, tempMaxWidth)
            .setAlignment(alignment)
            .setLineSpacing(spacingAdd, spacingMult)
            .setIncludePad(includePad)
            .build()
    } else {
        @Suppress("DEPRECATION")
        android.text.StaticLayout(text, paint, tempMaxWidth, alignment, spacingMult, spacingAdd, includePad)
    }

    // CRITICAL FIX: Calculate ACTUAL text width, don't use container width
    var maxLineWidth = 0f
    for (i in 0 until staticLayout.lineCount) {
        maxLineWidth = java.lang.Math.max(maxLineWidth, staticLayout.getLineWidth(i))
    }
    
    // Create Bitmap with Tight Bounds
    // Add small padding (e.g., 2px) to prevent clipping
    val finalWidth = java.lang.Math.max(1, kotlin.math.ceil(maxLineWidth).toInt() + 2) 
    val finalHeight = java.lang.Math.max(1, staticLayout.height)
    
    val bitmap = android.graphics.Bitmap.createBitmap(finalWidth, finalHeight, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // We must translate the canvas because ALIGN_CENTER in a wide layout
    // might have drawn text in the middle of 1000dp.
    // Actually, StaticLayout with ALIGN_CENTER draws relative to the width?
    // Let's simplify: Use ALIGN_NORMAL (Left) for the StaticLayout to draw at 0,0, 
    // since we are calculating max width ourselves, it essentially becomes the bound.
    // BUT we want multiline centered relative to each other.
    
    // BETTER APPROACH:
    // 1. Draw translated.
    // StaticLayout aligns based on the outer width.
    // If we want tight crop, we might need to verify where it draws.
    
    // Alternative: Just use ALIGN_NORMAL. 
    // If multiline, "ALIGN_NORMAL" aligns left.
    // We want visually centered lines.
    // If we use ALIGN_CENTER, it centers within `tempMaxWidth`.
    // That means text is at X = tempMaxWidth/2.
    // That's hard to crop.
    
    // SOLUTION: Use Alignment.ALIGN_NORMAL.
    // For single line: perfect.
    // For multiline: lines are left-aligned.
    // If we want CENTERED multiline structure (pyramid shape), we need ALIGN_CENTER 
    // AND to draw onto a canvas that is centered?
    
    // Let's try this:
    // 1. Use ALIGN_CENTER.
    // 2. Translate Canvas by negative X offset?
    // No, easier way:
    // Create layout with `finalWidth`.

    
    // Re-create layout with the TIGHT width to force correct alignment
    val tightLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        android.text.StaticLayout.Builder.obtain(text, 0, text.length, paint, finalWidth)
            .setAlignment(alignment)
            .setLineSpacing(spacingAdd, spacingMult)
            .setIncludePad(includePad)
            .build()
    } else {
        @Suppress("DEPRECATION")
        android.text.StaticLayout(text, paint, finalWidth, alignment, spacingMult, spacingAdd, includePad)
    }

    tightLayout.draw(canvas)
    
    return bitmap
}

class CheckInWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = DataStoreRepository(context)
        val lastDateString = runBlocking { repository.lastCheckInDate.first() }
        val today = java.time.LocalDate.now().toString()
        val dataStoreCheckedIn = lastDateString == today
        
        provideContent {
            val prefs = currentState<Preferences>()
            val currentQuote = prefs[stringPreferencesKey("current_quote")]
            val prefCheckedIn = prefs[androidx.datastore.preferences.core.booleanPreferencesKey("is_checked_in")] ?: false
            
            // OPTIMIZATION: Read from Widget State (Fast), pushed from Settings
            val bgType = prefs[stringPreferencesKey("key_widget_bg_type")] ?: "color"
            val bgColorInt = prefs[androidx.datastore.preferences.core.intPreferencesKey("key_widget_bg_color")] ?: -7357297 // Default Green
            val bgImageUri = prefs[stringPreferencesKey("key_widget_bg_image_uri")]

            // Raw Text Logic
            val isCheckedIn = (lastDateString == today) || prefCheckedIn
            
            val rawText = when {
                !isCheckedIn -> "我还好"
                currentQuote != null -> currentQuote
                else -> "你好就行"
            }
            
            val isQuote = rawText.contains("\n") || rawText.length > 5
            
            val buttonAction = if (!isCheckedIn) {
                actionRunCallback<CheckInActionCallback>()
            } else {
                actionRunCallback<ShowQuoteActionCallback>()
            }
    
            // Outer Box (Centering in Widget Area)
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                 // Background Logic
                 // If Image: Render Image, then Content on top
                 if (bgType == "image" && bgImageUri != null) {
                     val bitmapFile = java.io.File(bgImageUri)
                     if (bitmapFile.exists()) {
                         // Image Layer
                         androidx.glance.Image(
                             provider = androidx.glance.ImageProvider(android.graphics.BitmapFactory.decodeFile(bitmapFile.absolutePath)),
                             contentDescription = null,
                             contentScale = androidx.glance.layout.ContentScale.Crop,
                             modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp).clickable(buttonAction)
                         )
                     } else {
                         // Fallback to Color if file missing
                         Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(bgColorInt))).cornerRadius(16.dp).clickable(buttonAction)) {}
                     }
                 } else {
                     // Color Layer
                     Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .background(ColorProvider(Color(bgColorInt)))
                            .appWidgetBackground() 
                            .cornerRadius(16.dp)
                            .clickable(buttonAction)
                     ) {}
                 }

                 // Text Layer (Overlay)
                 // Re-use the click action here too just in case
                 Box(
                     modifier = GlanceModifier.fillMaxSize().padding(4.dp).clickable(buttonAction),
                     contentAlignment = Alignment.Center
                 ) {
                     // Unified Rendering: Always use Bitmap to ensure View Structure stability
                     // This allows Android 12+ to perform automatic crossfade animations on content change
                     val fontSize = if (isQuote) 36f else 28f // Slightly larger for status to match bold feel
                     val fontBitmap = createTextBitmap(context, rawText, fontSize)

                     androidx.glance.Image(
                         provider = androidx.glance.ImageProvider(fontBitmap),
                         contentDescription = rawText,
                         contentScale = androidx.glance.layout.ContentScale.Fit,
                         modifier = GlanceModifier.padding(horizontal = 8.dp)
                     )
                 }
            }
        }
    }
}

// 2. The Receiver
class CheckInWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CheckInWidget()
}

// 3. Actions

class CheckInActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val repository = DataStoreRepository(context)
        val manager = CheckInManager(repository)
        manager.performCheckIn(context) 
        
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[androidx.datastore.preferences.core.booleanPreferencesKey("is_checked_in")] = true
            prefs.remove(stringPreferencesKey("current_quote"))
        }
        
        CheckInWidget().update(context, glanceId)
    }
}

class ShowQuoteActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val newQuote = Quotes.getRandomQuote()
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[stringPreferencesKey("current_quote")] = newQuote
            prefs[androidx.datastore.preferences.core.booleanPreferencesKey("is_checked_in")] = true
        }
        CheckInWidget().update(context, glanceId)
    }
}
