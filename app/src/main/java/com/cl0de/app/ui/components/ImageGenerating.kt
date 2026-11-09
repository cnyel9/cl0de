package com.cl0de.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl0de.app.data.SChat
import com.cl0de.app.ui.theme.Oc
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * The wait for a generated picture (Deep's flux-schnell): the bridge answers
 * in one chunk at the very end, so instead of the generic "Thinking…" the
 * chat tail shows the shape of what is coming, ChatGPT-style: a shimmering
 * stage line, a square of travelling dots and an estimate percent in the
 * corner. The percent is a time-based estimate only (the bridge gets no
 * progress signal from the generator): it climbs fast, then eases towards a
 * 94% cap and waits there; the real reply replaces this composable the
 * moment it lands. Both clocks live in LaunchedEffects of this composable
 * and die with it: no shared state, no timer can outlive the wait.
 */
@Composable
fun ImageGeneratingPlaceholder(modifier: Modifier = Modifier) {
    val c = Oc.c
    // Slow clock for the stage line and the percent (~200ms steps). Only the
    // stage index and the percent below read it, and both are computed in
    // this small subtree, so the big canvas never recomposes with time.
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var t0 = -1L
        while (true) {
            withFrameMillis { ft -> if (t0 < 0L) t0 = ft; elapsedMs = ft - t0 }
            delay(200)
        }
    }
    val stage = (elapsedMs / 2500).toInt().coerceAtMost(3)
    val pct = (94.0 * (1.0 - exp(-elapsedMs / 2600.0))).toInt()
    Column(modifier.fillMaxWidth()) {
        AnimatedContent(
            stage, label = "image-stage",
            transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 }) togetherWith (fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 2 }) },
        ) { s ->
            StageShimmer(
                when (s) {
                    0 -> SChat.imgSketching
                    1 -> SChat.imgDrafting
                    2 -> SChat.imgComposing
                    else -> SChat.imgFinishing
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(Oc.round(18.dp)).background(c.surface).border(1.dp, c.border, Oc.round(18.dp)),
        ) {
            DotWave(Modifier.fillMaxSize().padding(14.dp))
            Box(
                Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(CircleShape).background(c.surfaceHigh)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) { Txt("$pct%", size = 12.sp, color = c.text, weight = FontWeight.Medium) }
        }
    }
}

/** The stage line, shimmering like the work label does in Thinking.kt. */
@Composable
private fun StageShimmer(text: String) {
    val c = Oc.c
    val t = rememberInfiniteTransition(label = "img-shimmer")
    val x by t.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x")
    val w = 260f
    Text(
        text,
        style = TextStyle(
            fontSize = 15.sp, fontFamily = Oc.reply,
            brush = Brush.linearGradient(
                0f to c.textDim, 0.5f to c.text, 1f to c.textDim,
                start = Offset(x * w - w / 2, 0f), end = Offset(x * w + w / 2, 0f),
            ),
        ),
    )
}

/**
 * PixelWave grown to picture size: one Canvas, one phase, a 14x14 grid whose
 * diagonal wave (PixelWave's formula) breathes each dot's size and alpha.
 * The phase driver is quantized to ~33ms steps and the phase is read only
 * inside the draw block, so repaints never recompose anything: cheap enough
 * for weak phones. Plain per-dot colors, no brushes, no layout nodes.
 */
@Composable
private fun DotWave(modifier: Modifier) {
    val c = Oc.c
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var t0 = -1L
        var lastStep = -1L
        while (true) {
            withFrameMillis { ft ->
                if (t0 < 0L) t0 = ft
                val step = (ft - t0) / 33L
                if (step != lastStep) {
                    lastStep = step
                    phase.floatValue = (step * 33L % 1500L) / 1500f
                }
            }
        }
    }
    val dot = c.textDim
    Canvas(modifier) {
        val ph = phase.floatValue
        val n = 14
        val cellW = size.width / n
        val cellH = size.height / n
        val baseR = minOf(cellW, cellH) * 0.31f
        for (y in 0 until n) for (x in 0 until n) {
            val k = (sin((ph - (x + y) / 6f) * 2 * PI).toFloat() + 1f) / 2f
            drawCircle(
                dot.copy(alpha = 0.15f + 0.85f * k),
                baseR * (0.75f + 0.4f * k),
                Offset(x * cellW + cellW / 2, y * cellH + cellH / 2),
            )
        }
    }
}
