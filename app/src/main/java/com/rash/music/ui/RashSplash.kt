package com.rash.music.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rash.music.R

private val SplashBackground = Color(0xFF07070C)
private val Blue = Color(0xFF48B6FF)
private val Violet = Color(0xFF8955F6)

/** A layer's placement inside the logo, as fractions of the logo's width/height. */
private class SplashBox(val x: Float, val y: Float, val w: Float, val h: Float)

private const val LOGO_ASPECT = 3.06519f
private val StaffBox = SplashBox(0.00000f, 0.09339f, 0.49688f, 0.68488f)
private val NotesBox = SplashBox(0.02344f, 0.00000f, 0.45312f, 0.99379f)
private val RashBox = SplashBox(0.45078f, 0.10058f, 0.54922f, 0.67769f)
private val MusicBox = SplashBox(0.64922f, 0.84532f, 0.29219f, 0.15326f)

private val easeOut = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
private val easeInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private fun seg(p: Float, start: Float, end: Float): Float = ((p - start) / (end - start)).coerceIn(0f, 1f)

/**
 * The launch animation, built from four pre-rendered layers of the Rash Music
 * wordmark (staff, notes + clef, "RASH", "MUSIC"). The staff and the notes are
 * revealed left to right, then the name slides in. A single [Animatable] drives
 * everything and is only read inside the draw lambda, so there is no
 * recomposition and each frame is a handful of bitmap draws on the GPU — the
 * same cost on a low-end phone as on a flagship.
 */
@Composable
fun RashSplash(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val staff = ImageBitmap.imageResource(R.drawable.splash_staff)
    val notes = ImageBitmap.imageResource(R.drawable.splash_notes)
    val rash = ImageBitmap.imageResource(R.drawable.splash_rash)
    val music = ImageBitmap.imageResource(R.drawable.splash_music)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 2100, easing = LinearEasing))
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
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.84f)
                .aspectRatio(LOGO_ASPECT),
        ) {
            drawLogo(progress.value, staff, notes, rash, music)
        }
    }
}

private fun DrawScope.drawLayer(
    image: ImageBitmap,
    box: SplashBox,
    alpha: Float,
    dx: Float = 0f,
    dy: Float = 0f,
    reveal: Float = 1f,
) {
    if (alpha <= 0f || reveal <= 0f) return
    val cw = size.width
    val ch = size.height
    val left = box.x * cw + dx
    val top = box.y * ch + dy
    val w = box.w * cw
    val h = box.h * ch
    clipRect(left = left, top = top, right = left + w * reveal, bottom = top + h) {
        drawImage(
            image = image,
            dstOffset = IntOffset(left.toInt(), top.toInt()),
            dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)),
            alpha = alpha,
            filterQuality = FilterQuality.High,
        )
    }
}

private fun DrawScope.drawLogo(p: Float, staff: ImageBitmap, notes: ImageBitmap, rash: ImageBitmap, music: ImageBitmap) {
    val cw = size.width
    val ch = size.height
    // soft brand glow behind the staff
    val glow = seg(p, 0f, 0.45f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Violet.copy(alpha = 0.32f * glow), Blue.copy(alpha = 0.10f * glow), Color.Transparent),
            center = Offset(cw * 0.26f, ch * 0.5f),
            radius = cw * 0.42f,
        ),
        radius = cw * 0.42f,
        center = Offset(cw * 0.26f, ch * 0.5f),
    )
    // staff: drawn left to right
    val staffT = easeInOut.transform(seg(p, 0.04f, 0.42f))
    drawLayer(staff, StaffBox, alpha = seg(p, 0.04f, 0.14f), reveal = staffT)
    // clef and notes: revealed behind the staff line, rising slightly into place
    val notesT = easeOut.transform(seg(p, 0.26f, 0.66f))
    drawLayer(notes, NotesBox, alpha = seg(p, 0.26f, 0.40f), dy = ch * 0.04f * (1f - notesT), reveal = notesT)
    // RASH slides in from the right
    val rashT = easeOut.transform(seg(p, 0.50f, 0.78f))
    drawLayer(rash, RashBox, alpha = rashT, dx = cw * 0.07f * (1f - rashT))
    // MUSIC rises last
    val musicT = easeOut.transform(seg(p, 0.70f, 0.96f))
    drawLayer(music, MusicBox, alpha = musicT, dy = ch * 0.05f * (1f - musicT))
}
