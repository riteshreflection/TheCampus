package com.reflection.thecampus.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom view that displays animated gold particles for premium UI effect
 */
class ParticleAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null
    
    // Particle configuration for twinkling effect
    private val particleCount = 80  // Increased density
    private val baseGoldColor = 0xFFFFD700.toInt()
    private val lightGoldColor = 0xFFFFF4CC.toInt()
    
    init {
        // Initialize particles with varied properties for natural twinkling
        for (i in 0 until particleCount) {
            particles.add(createRandomParticle())
        }
        
        // Setup animator with faster refresh for smooth twinkling
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000 // Faster 2 second cycle for more dynamic effect
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateParticles()
                invalidate()
            }
        }
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.start()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        particles.forEach { particle ->
            // Use varied gold colors for more natural sparkle
            val useLight = particle.phase % 120f < 60f
            paint.color = if (useLight) lightGoldColor else particle.color
            paint.alpha = (particle.currentAlpha * 255).toInt()
            
            // Draw particle with slight glow effect
            paint.style = Paint.Style.FILL
            canvas.drawCircle(particle.x, particle.y, particle.size, paint)
            
            // Add subtle outer glow for twinkle effect
            if (particle.currentAlpha > 0.5f) {
                paint.alpha = ((particle.currentAlpha - 0.5f) * 100).toInt()
                canvas.drawCircle(particle.x, particle.y, particle.size * 1.5f, paint)
            }
        }
    }
    
    private fun createRandomParticle(): Particle {
        val isDarkMode = (resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        // Create particles with varied sizes for depth
        val sizeVariation = Random.nextFloat()
        val size = when {
            sizeVariation < 0.3f -> Random.nextFloat() * 1.5f + 0.5f  // Small particles
            sizeVariation < 0.7f -> Random.nextFloat() * 2f + 1.5f    // Medium particles
            else -> Random.nextFloat() * 3f + 2.5f                     // Large particles
        }
        
        return Particle(
            x = Random.nextFloat() * width,
            y = Random.nextFloat() * height,
            size = size,
            speedX = Random.nextFloat() * 0.8f - 0.4f,  // Faster movement
            speedY = Random.nextFloat() * 0.8f - 0.4f,
            alpha = Random.nextFloat() * 0.5f + 0.3f,     // Brighter particles
            color = if (isDarkMode) {
                // Brighter gold for dark mode
                adjustColorBrightness(baseGoldColor, 1.3f)
            } else {
                // Slightly dimmer for light mode
                adjustColorBrightness(baseGoldColor, 0.9f)
            },
            phase = Random.nextFloat() * 360f,
            phaseSpeed = Random.nextFloat() * 6f + 3f  // Much faster twinkling
        )
    }
    
    private fun updateParticles() {
        if (width == 0 || height == 0) return
        
        particles.forEach { particle ->
            // Update position with slow drift
            particle.x += particle.speedX
            particle.y += particle.speedY
            
            // Update phase for pulsing twinkle effect
            particle.phase += particle.phaseSpeed
            if (particle.phase > 360f) particle.phase -= 360f
            
            // Create dramatic pulse for twinkling (like stars)
            val pulseFactor = (sin(Math.toRadians(particle.phase.toDouble())) + 1.0) / 2.0
            val twinkleFactor = pulseFactor * pulseFactor  // Square for more dramatic effect
            particle.currentAlpha = (particle.alpha * twinkleFactor * 0.8 + particle.alpha * 0.2).toFloat()
            
            // Wrap around edges for continuous effect
            if (particle.x < 0) particle.x = width.toFloat()
            if (particle.x > width) particle.x = 0f
            if (particle.y < 0) particle.y = height.toFloat()
            if (particle.y > height) particle.y = 0f
        }
    }
    
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    
    private data class Particle(
        var x: Float,
        var y: Float,
        val size: Float,
        val speedX: Float,
        val speedY: Float,
        val alpha: Float,
        val color: Int,
        var phase: Float,
        val phaseSpeed: Float,
        var currentAlpha: Float = alpha
    )
}
