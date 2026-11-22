package com.serma.pubify.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset

/**
 * Apple HIG / iOS-inspired fluid motion curves and GPU-composited spring helpers.
 * All transform animations read state strictly inside `graphicsLayer` so there are zero
 * layout passes or composition-phase jitters during 60Hz–120Hz frame updates.
 */
object IosMotion {
    /** Iconic fluid deceleration curve used across iOS sheets, tabs, and fades. */
    val EaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Smooth symmetric ease-in-out for subtle opacity transitions. */
    val StandardEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /** Fluid spring for sliding indicators, progress bars, and hero entrances. */
    fun <T> fluidSpring(
        dampingRatio: Float = 0.82f,
        stiffness: Float = 420f
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    /** Critically tuned spring for IntOffset slides & LazyColumn item placement. */
    fun offsetSpring(
        dampingRatio: Float = 0.86f,
        stiffness: Float = 420f
    ): SpringSpec<IntOffset> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    /** Crisp micro-bouncy spring for selected icons and interactive badges. */
    fun <T> tactileSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = 480f
    )

    /** Critically tuned, zero-jitter spring for panels, bars, and entrances. */
    fun <T> smoothSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.86f,
        stiffness = 400f
    )

    /** Crisp micro-bouncy spring for sliding pill indicators. */
    fun <T> pillSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = 440f
    )

    /** Snappy tactile spring for immediate press-down and release response. */
    fun <T> pressSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = 560f
    )

    /** Smooth iOS ease-out tween for opacity and color crossfades. */
    fun <T> silkyTween(durationMillis: Int = 220): TweenSpec<T> = tween(
        durationMillis = durationMillis,
        easing = EaseOut
    )

    /** Alias for silkyTween for fast opacity fades. */
    fun <T> fadeTween(durationMillis: Int = 200): TweenSpec<T> = silkyTween(durationMillis)
}

/**
 * Applies tactile iOS-style press compression (`scaleX`/`scaleY` + subtle `alpha`)
 * bound to a Compose [InteractionSource]. Automatically cancels on scroll gestures
 * and runs strictly on the GPU render layer (`graphicsLayer`) for 120Hz smoothness.
 */
fun Modifier.iosPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.965f,
    pressedAlpha: Float = 0.92f,
    enabled: Boolean = true
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = IosMotion.pressSpring(),
        label = "ios_press_scale"
    )
    val alphaState = animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedAlpha else 1f,
        animationSpec = IosMotion.silkyTween(durationMillis = 120),
        label = "ios_press_alpha"
    )
    this.graphicsLayer {
        val s = scaleState.value
        scaleX = s
        scaleY = s
        alpha = alphaState.value
    }
}

/**
 * Non-consuming GPU press-bounce modifier for any clickable component (`FilterChip`,
 * `Button`, `OutlinedButton`, `FloatingActionButton`, `Surface`) without intercepting
 * clicks, ripples, or parent scrolling.
 */
fun Modifier.iosTapBounce(
    pressedScale: Float = 0.955f,
    pressedAlpha: Float = 0.93f,
    enabled: Boolean = true
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scaleState = animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = IosMotion.pressSpring(),
        label = "ios_tap_bounce_scale"
    )
    val alphaState = animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedAlpha else 1f,
        animationSpec = IosMotion.silkyTween(durationMillis = 110),
        label = "ios_tap_bounce_alpha"
    )

    this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                isPressed = true
                try {
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                } finally {
                    isPressed = false
                }
            }
        }
        .graphicsLayer {
            val s = scaleState.value
            scaleX = s
            scaleY = s
            alpha = alphaState.value
        }
}
