package com.example.privatecheck.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import androidx.compose.runtime.withFrameNanos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ------------------------------------------------------------------------
// Data Structures
// ------------------------------------------------------------------------

data class ParticleV2(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var alpha: Float = 1f,
    val size: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    var lifeTime: Float, // Current age in seconds
    val maxLifeTime: Float, // Total lifespan
    val shapeObj: Path // Pre-calculated shape path (normalized at 0,0)
)

// ------------------------------------------------------------------------
// Physics Constants
// ------------------------------------------------------------------------
private const val GRAVITY = 1000f // px/s^2 (Reduced slightly for floatier feel)
private const val DRAG = 0.99f 

@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current.density
        
        // State for frame loop
        val particles = remember { mutableStateListOf<ParticleV2>() }
        var frameTrigger by remember { mutableStateOf(0L) } // FORCE REDRAW TRIGGER
        
        // One-shot initialization
        LaunchedEffect(widthPx, heightPx) {
            if (widthPx > 0 && heightPx > 0) {
                // ... (Generation logic remains same, omitted for brevity if possible, but replace_file_content needs context)
                // Wait, I need to match the block. I will just replace the whole LaunchedEffect block or inner loop.
                
                // Let's replace the whole LaunchedEffect block for safety.
                // Generate Particles from two sides (OFF-SCREEN)
                val totalParticles = 150
                
                // Left Side
                repeat(totalParticles / 2) {
                    particles.add(
                        createFireworkParticle(
                            density = density,
                            emitterXRange = -100f..-10f, 
                            emitterYRange = (heightPx * 0.4f)..(heightPx * 0.9f), 
                            angleRange = -70.0..-20.0, 
                            isRightSide = false
                        )
                    )
                }
                
                // Right Side
                repeat(totalParticles / 2) {
                    particles.add(
                        createFireworkParticle(
                            density = density,
                            emitterXRange = (widthPx + 10f)..(widthPx + 100f),
                            emitterYRange = (heightPx * 0.4f)..(heightPx * 0.9f),
                            angleRange = -160.0..-110.0,
                            isRightSide = true
                        )
                    )
                }

                // Physics Loop (High Precision)
                var lastTimeNanos = withFrameNanos { it }
                
                while (isActive && particles.isNotEmpty()) {
                    withFrameNanos { frameTimeNanos ->
                        // Calculate precise dt
                        val dtNano = frameTimeNanos - lastTimeNanos
                        lastTimeNanos = frameTimeNanos
                        
                        // Convert to seconds
                        var dt = dtNano / 1_000_000_000f
                        if (dt > 0.064f) dt = 0.064f 

                        // Update Simulation
                        val iterator = particles.iterator()
                        while (iterator.hasNext()) {
                            val p = iterator.next()
                            
                            p.lifeTime += dt
                            if (p.lifeTime >= p.maxLifeTime) {
                                iterator.remove()
                                continue
                            }
                            
                            p.vy += GRAVITY * dt
                            p.x += p.vx * dt
                            p.y += p.vy * dt
                            p.rotation += p.rotationSpeed * dt
                            
                            val lifeProgress = p.lifeTime / p.maxLifeTime
                            if (lifeProgress > 0.8f) {
                                p.alpha = (1f - lifeProgress) / 0.2f
                            }
                            
                            if (p.y > heightPx + 300f && p.vy > 0) {
                                iterator.remove()
                            }
                        }
                        
                        // Force Redraw
                        frameTrigger = frameTimeNanos
                    }
                }
                
                onFinished()
            }
        }

        // Draw Phase
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read Trigger to ensure Invalidations happen every frame
            val tick = frameTrigger 
            
            particles.forEach { p ->
                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    translate(left = p.x, top = p.y) {
                        drawPath(
                            path = p.shapeObj,
                            color = p.color.copy(alpha = p.alpha)
                        )
                    }
                }
            }
        }
    }
}

fun createFireworkParticle(
    density: Float,
    emitterXRange: ClosedFloatingPointRange<Float>,
    emitterYRange: ClosedFloatingPointRange<Float>,
    angleRange: ClosedFloatingPointRange<Double>,
    isRightSide: Boolean
): ParticleV2 {
    // 1. Random Start Position
    val startX = Random.nextDouble(emitterXRange.start.toDouble(), emitterXRange.endInclusive.toDouble()).toFloat()
    val startY = Random.nextDouble(emitterYRange.start.toDouble(), emitterYRange.endInclusive.toDouble()).toFloat()

    // 2. Random Velocity
    // Increase speed slightly to clear the screen edge
    val baseSpeedMin = 500f * density * 0.6f 
    val baseSpeedMax = 1500f * density * 0.6f
    
    val speed = Random.nextDouble(baseSpeedMin.toDouble(), baseSpeedMax.toDouble()).toFloat()
    val angle = Math.toRadians(Random.nextDouble(angleRange.start, angleRange.endInclusive))
    
    val vx = (cos(angle) * speed).toFloat()
    // Vy must be negative (Up). Angle ranges (-70..-20) are naturally Up.
    val vy = (sin(angle) * speed).toFloat()
    
    // 3. Random Lifespan (2s to 4s)
    val life = Random.nextDouble(2.0, 4.0).toFloat()
    
    // 4. Parameters
    val size = (Random.nextFloat() * 20f + 10f) * density // Slightly larger
    val rotSpeed = (Random.nextFloat() - 0.5f) * 500f 
    
    // 5. Shape Generation (Random Polygon / "Random Triangle")
    val path = Path()
    // Generate a random triangle (3 points around 0,0)
    // To make it irregular, vary the radius and angle step
    val r = size / 2f
    path.moveTo(
        (Random.nextFloat() - 0.5f) * r, 
        -r * (0.8f + Random.nextFloat() * 0.4f)
    ) // Point 1 (Top-ish)
    
    path.lineTo(
        r * (0.5f + Random.nextFloat() * 0.5f), 
        r * (0.5f + Random.nextFloat() * 0.5f)
    ) // Point 2 (Bottom Right-ish)
    
    path.lineTo(
        -r * (0.5f + Random.nextFloat() * 0.5f), 
        r * (0.5f + Random.nextFloat() * 0.5f)
    ) // Point 3 (Bottom Left-ish)
    
    path.close()
    
    val colors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFF4500), // OrangeRed
        Color(0xFF00CED1), // DarkTurquoise
        Color(0xFF7FFF00), // Chartreuse
        Color(0xFFFF00FF), // Magenta
        Color(0xFFFFFFFF), // White
        Color(0xFF1E90FF)  // DodgerBlue
    )

    return ParticleV2(
        x = startX,
        y = startY,
        vx = vx,
        vy = vy,
        color = colors.random(),
        size = size,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = rotSpeed,
        lifeTime = 0f,
        maxLifeTime = life,
        shapeObj = path
    )
}
