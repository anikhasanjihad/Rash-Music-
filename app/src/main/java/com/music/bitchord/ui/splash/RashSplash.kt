package com.music.bitchord.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R

private val Blue = Color(0xFF48B6FF)
private val Violet = Color(0xFF8955F6)
private val Ink = Color(0xFF050505)
private val Backdrop = Color(0xFF050505)

/** Timeline, in milliseconds. Everything is derived from one clock so the whole thing stays in step. */
private const val APP_READY_AT = 1250f
private const val END_AT = 1500f

private val Smooth = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private fun seg(t: Float, start: Float, end: Float): Float =
    ((t - start) / (end - start)).coerceIn(0f, 1f)

/** The three arcs of the mark, in the logo's own 512 x 512 space. */
private fun arcPaths(): List<Path> = listOf(
    Path().apply { moveTo(105f, 190f); quadraticTo(256f, 125f, 407f, 190f) },
    Path().apply { moveTo(125f, 256f); quadraticTo(256f, 205f, 387f, 256f) },
    Path().apply { moveTo(150f, 320f); quadraticTo(256f, 280f, 362f, 320f) },
)

/**
 * Rash Music's launch animation: the gradient disc settles in, the three arcs
 * draw themselves one after another, a light sweeps across, and the wordmark
 * rises underneath.
 *
 * Built to stay smooth: one [Animatable] clock is read only while drawing (and
 * inside graphicsLayer), so nothing recomposes per frame, and no objects are
 * allocated in the draw path. The app underneath is only composed once
 * [onAppReady] fires, so its first-frame work never lands in the middle of the
 * logo animation — it lands in the short fade-out instead.
 */
@Composable
fun RashSplash(onAppReady: () -> Unit, onFinished: () -> Unit) {
    val clock = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        clock.animateTo(APP_READY_AT, tween((APP_READY_AT).toInt(), easing = LinearEasing))
        onAppReady()
        clock.animateTo(END_AT, tween((END_AT - APP_READY_AT).toInt(), easing = LinearEasing))
        onFinished()
    }

    val arcs = remember { arcPaths() }
    val measures = remember { arcs.map { PathMeasure().apply { setPath(it, false) } } }
    val segment = remember { Path() }
    val disc = remember { Path() }

    val font = remember { FontFamily(Font(R.font.sf_pro_display_bold, FontWeight.Bold)) }
    val wordmark = remember {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White)) { append("Rash ") }
            withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(Blue, Violet)))) { append("Music") }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val f = Smooth.transform(seg(clock.value, APP_READY_AT, END_AT))
                alpha = 1f - f
                val s = 1f + 0.04f * f
                scaleX = s
                scaleY = s
            }
            .background(Backdrop)
            // Swallow touches while it is up: the app underneath is already live during the fade.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(300.dp)) {
            val t = clock.value
            val logoPx = 180.dp.toPx()
            val s = logoPx / 512f
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = 240f * s

            // 1. Glow, then the disc settling in.
            val c = Smooth.transform(seg(t, 0f, 480f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Violet.copy(alpha = 0.42f * c), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.52f,
                ),
                radius = size.minDimension * 0.52f,
                center = center,
            )
            val discScale = 0.8f + 0.2f * c
            withTransform({ scale(discScale, discScale, center) }) {
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Blue, Violet),
                        start = Offset(center.x - r, center.y - r),
                        end = Offset(center.x + r, center.y + r),
                    ),
                    radius = r,
                    center = center,
                    alpha = c,
                )

                // 2. A soft light sweeping across the disc.
                val sweep = seg(t, 520f, 1050f)
                if (sweep > 0f && sweep < 1f) {
                    disc.rewind()
                    disc.addOval(Rect(center, r))
                    clipPath(disc) {
                        rotate(-32f, center) {
                            val x = center.x - r * 1.6f + (r * 3.2f) * Smooth.transform(sweep)
                            val half = r * 0.35f
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                                    startX = x - half,
                                    endX = x + half,
                                ),
                            )
                        }
                    }
                }

                // 3. The arcs draw themselves, one after the other.
                withTransform({
                    translate(center.x - 256f * s, center.y - 239f * s)
                    scale(s, s, Offset.Zero)
                }) {
                    for (i in arcs.indices) {
                        val p = Smooth.transform(seg(t, 150f + 150f * i, 670f + 150f * i))
                        if (p <= 0f) continue
                        val m = measures[i]
                        segment.rewind()
                        m.getSegment(0f, m.length * p, segment, true)
                        drawPath(
                            path = segment,
                            color = Ink,
                            style = Stroke(width = 30f, cap = StrokeCap.Round),
                        )
                        // fine highlight along the upper edge
                        withTransform({ translate(0f, -7f) }) {
                            drawPath(
                                path = segment,
                                color = Color.White.copy(alpha = 0.16f),
                                style = Stroke(width = 4f, cap = StrokeCap.Round),
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = wordmark,
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            letterSpacing = 0.4.sp,
            modifier = Modifier
                .offset(y = (-52).dp)
                .graphicsLayer {
                    val a = Smooth.transform(seg(clock.value, 800f, 1200f))
                    alpha = a
                    translationY = (1f - a) * 18.dp.toPx()
                },
        )
    }
}
