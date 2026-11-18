package com.rash.music.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SplashBackground = Color(0xFF07070C)
private val Blue = Color(0xFF48B6FF)
private val Violet = Color(0xFF8955F6)
private val WaveTop = Color(0xFF14122B)
private val WaveBottom = Color(0xFF050505)

/**
 * The launch animation: the orb scales in, the three waves draw themselves one
 * after another, then the wordmark slides in. One [Animatable] drives all of
 * it and is only read inside the draw lambda, so the animation costs no
 * recomposition — each frame is a single Canvas pass.
 *
 * Coordinates follow assets/splash.svg (a 1000 x 300 artboard).
 */
@Composable
fun RashSplash(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val measurer = rememberTextMeasurer()
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 1700, easing = androidx.compose.animation.core.LinearEasing))
        exit.animateTo(1f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - exit.value }
            .background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.82f)
                .aspectRatio(1000f / 300f),
        ) {
            drawSplash(progress.value, measurer)
        }
    }
}

private fun seg(p: Float, start: Float, end: Float): Float = ((p - start) / (end - start)).coerceIn(0f, 1f)
private val easeOut = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
private val easeInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private fun DrawScope.drawSplash(p: Float, measurer: TextMeasurer) {
    val s = size.width / 1000f
    fun x(v: Float) = v * s
    // ---- glow behind the orb
    val glowT = seg(p, 0.05f, 0.5f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Violet.copy(alpha = 0.45f * glowT), Color.Transparent),
            center = Offset(x(150f), x(150f)),
            radius = x(150f),
        ),
        radius = x(150f),
        center = Offset(x(150f), x(150f)),
    )
    // ---- orb
    val orbT = easeOut.transform(seg(p, 0f, 0.45f))
    val orbScale = 0.4f + 0.6f * orbT
    withTransform({ scale(orbScale, orbScale, pivot = Offset(x(150f), x(150f))) }) {
        val orbBrush = Brush.linearGradient(
            listOf(Blue, Violet),
            start = Offset(x(25f), x(25f)),
            end = Offset(x(275f), x(275f)),
        )
        drawCircle(orbBrush, radius = x(125f), center = Offset(x(150f), x(150f)), alpha = orbT)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(x(110f), x(95f)),
                radius = x(190f),
            ),
            radius = x(125f),
            center = Offset(x(150f), x(150f)),
            alpha = orbT,
        )
        drawCircle(
            Color.White.copy(alpha = 0.3f * orbT),
            radius = x(123f),
            center = Offset(x(150f), x(150f)),
            style = Stroke(width = x(2f)),
        )
    }
    // ---- waves, each drawn along its own path
    val waves = listOf(
        Triple(Offset(75f, 112f), Offset(150f, 80f), Offset(225f, 112f)),
        Triple(Offset(85f, 150f), Offset(150f, 125f), Offset(215f, 150f)),
        Triple(Offset(98f, 187f), Offset(150f, 169f), Offset(202f, 187f)),
    )
    val waveBrush = Brush.verticalGradient(listOf(WaveTop, WaveBottom), startY = x(100f), endY = x(200f))
    waves.forEachIndexed { i, (a, c, b) ->
        val t = easeInOut.transform(seg(p, 0.30f + i * 0.08f, 0.52f + i * 0.08f))
        if (t <= 0f) return@forEachIndexed
        val path = Path().apply {
            moveTo(x(a.x), x(a.y))
            quadraticTo(x(c.x), x(c.y), x(b.x), x(b.y))
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val out = Path()
        measure.getSegment(0f, measure.length * t, out, true)
        drawPath(out, waveBrush, style = Stroke(width = x(17f), cap = StrokeCap.Round))
    }
    // ---- wordmark
    val style = TextStyle(fontSize = (105f * s).toSp(), fontWeight = FontWeight.Bold)
    fun word(text: String, left: Float, from: Float, brush: Brush?) {
        val t = easeOut.transform(seg(p, from, from + 0.28f))
        if (t <= 0f) return
        val layout = measurer.measure(text, style)
        val top = x(180f) - layout.firstBaseline
        val dx = x(-26f) * (1f - t)
        withTransform({ translate(x(left) + dx, top) }) {
            if (brush != null) {
                drawText(layout, brush = brush, alpha = t)
            } else {
                drawText(layout, color = Color.White, alpha = t)
            }
        }
    }
    word("Rash", 320f, 0.55f, null)
    word(
        "Music", 590f, 0.64f,
        Brush.linearGradient(listOf(Blue, Violet), start = Offset(x(590f), x(90f)), end = Offset(x(900f), x(190f))),
    )
}
