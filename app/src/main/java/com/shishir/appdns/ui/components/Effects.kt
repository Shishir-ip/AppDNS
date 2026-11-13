package com.shishir.appdns.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.shishir.appdns.ui.theme.LocalHapticsOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Signature toggle: spring physics with a rubbery overshoot, squish on press,
 * cross-fading track and a haptic tick. Monochrome by design.
 */
@Composable
fun RubberSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsOn.current
    val trackW = 56.dp
    val trackH = 32.dp
    val thumbSize = 24.dp
    val pad = 4.dp
    val travel = with(LocalDensity.current) { (trackW - thumbSize - pad * 2).toPx() }

    val offset by animateFloatAsState(
        targetValue = if (checked) travel else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "thumbOffset"
    )
    val trackColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(220),
        label = "trackColor"
    )
    val thumbColor = if (checked) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    var pressed by remember { mutableStateOf(false) }
    val squish by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "squish"
    )

    Box(
        modifier = modifier
            .size(trackW, trackH)
            .clip(CircleShape)
            .background(trackColor)
            .semantics {
                role = Role.Switch
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) {
                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
    ) {
        Box(
            Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .padding(pad)
                .size(thumbSize)
                .graphicsLayer { scaleX = squish; scaleY = squish }
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

/** Press-anywhere scale feedback with spring release — the "high-end software" feel. */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressScale: Float = 0.97f,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "pressScale"
    )
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interaction, ripple(), enabled = enabled, onClick = onClick)
    ) { content() }
}

/** Staggered fade + rise entrance for list items. */
@Composable
fun StaggerIn(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(12) * 35L)
        shown = true
    }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(260), label = "staggerA")
    val rise by animateFloatAsState(
        if (shown) 0f else 26f,
        spring(dampingRatio = 0.8f, stiffness = 380f),
        label = "staggerY"
    )
    Box(modifier.graphicsLayer { this.alpha = alpha; translationY = rise }) { content() }
}

/** Check mark that pops in with overshoot — used for save confirmations. */
@Composable
fun PopIn(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scale by animateFloatAsState(
        if (visible) 1f else 0f,
        spring(dampingRatio = 0.45f, stiffness = 420f),
        label = "pop"
    )
    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale }) { content() }
}

/**
 * The AppDNS shield. Breathes gently while protection is active and emits a
 * soft radar pulse; idle state shows a calm outline with a center dot.
 */
@Composable
fun ShieldMark(
    active: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    val transition = rememberInfiniteTransition(label = "shield")
    val breath by transition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "breath"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Restart),
        label = "pulse"
    )
    Canvas(modifier.graphicsLayer {
        if (active) { scaleX = breath; scaleY = breath }
    }) {
        val w = size.width
        val h = size.height
        if (active) {
            drawCircle(
                color = color.copy(alpha = (1f - pulse) * 0.22f),
                radius = w * 0.44f + pulse * w * 0.16f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.92f, h * 0.21f)
            lineTo(w * 0.92f, h * 0.5f)
            cubicTo(w * 0.92f, h * 0.76f, w * 0.74f, h * 0.9f, w * 0.5f, h * 0.97f)
            cubicTo(w * 0.26f, h * 0.9f, w * 0.08f, h * 0.76f, w * 0.08f, h * 0.5f)
            lineTo(w * 0.08f, h * 0.21f)
            close()
        }
        drawPath(
            shield, color,
            style = Stroke(width = w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (active) {
            val check = Path().apply {
                moveTo(w * 0.33f, h * 0.5f)
                lineTo(w * 0.45f, h * 0.62f)
                lineTo(w * 0.68f, h * 0.37f)
            }
            drawPath(
                check, color,
                style = Stroke(width = w * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else {
            drawCircle(color, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.5f))
        }
    }
}

/**
 * Custom rubber-band overscroll: the list stretches like elastic when you
 * pull past its edge and springs back on release. Works on every Android
 * version, not just the Android 12+ system stretch.
 */
@OptIn(ExperimentalFoundationApi::class)
class RubberOverscrollEffect(private val scope: CoroutineScope) :
    androidx.compose.foundation.OverscrollEffect {

    private val offset = Animatable(0f)
    private val maxPx = 220f

    override val effectModifier: Modifier = Modifier.graphicsLayer {
        translationY = offset.value
        scaleY = 1f + (abs(offset.value) / 6000f).coerceAtMost(0.035f)
    }

    override val isInProgress: Boolean get() = offset.value != 0f

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        sideEffect: (Offset) -> Offset
    ): Offset {
        val consumed = sideEffect(delta)
        val remaining = delta - consumed
        if (remaining.y != 0f) {
            scope.launch {
                offset.snapTo((offset.value + remaining.y * 0.35f).coerceIn(-maxPx, maxPx))
            }
        }
        return delta
    }

    override suspend fun applyToFling(
        velocity: androidx.compose.ui.unit.Velocity,
        performFling: suspend (androidx.compose.ui.unit.Velocity) -> androidx.compose.ui.unit.Velocity
    ) {
        performFling(velocity)
        offset.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberRubberOverscroll(): androidx.compose.foundation.OverscrollEffect {
    val scope = rememberCoroutineScope()
    return remember { RubberOverscrollEffect(scope) }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.rubberOverscroll(effect: androidx.compose.foundation.OverscrollEffect): Modifier =
    this.overscroll(effect)