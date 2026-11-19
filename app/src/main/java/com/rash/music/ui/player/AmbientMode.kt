package com.rash.music.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rash.music.R
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import com.rash.music.data.lyrics.LyricLine
import com.rash.music.data.lyrics.LyricsSource
import com.rash.music.data.model.Song
import com.rash.music.data.model.artworkAt
import com.rash.music.playback.PlaybackPosition
import com.rash.music.ui.LyricsProviderState
import com.rash.music.ui.icons.SfIcons

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Ambient Mode: the artwork on the left, the synced lyrics on the right, over
 * the same artwork-tinted mesh the player uses, so it follows the app's theme.
 *
 * The screen turns itself to landscape while it is open (and back when it is
 * closed), and the system bars are hidden so it reads like a lock-screen clock
 * on a bedside stand.
 */
@Composable
fun AmbientModeScreen(
    song: Song,
    isPlaying: Boolean,
    position: PlaybackPosition,
    lyrics: List<LyricLine>?,
    lyricsSource: LyricsSource?,
    lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    onSelectLyricsProvider: (LyricsSource) -> Unit,
    lyricsUnavailable: Boolean,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    BackHandler(onBack = onClose)

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = previous
        }
    }

    val palette = rememberArtworkColors(song.thumbnailUrl)
    val scheme = MaterialTheme.colorScheme
    // The lyrics are drawn in white, so the surface behind them is always the
    // artwork's own mesh pulled dark: each song tints it differently, and the
    // text stays readable in any theme.
    val ink = Color.White
    val glassBorder = Color.White.copy(alpha = 0.22f)

    // The header (close button + label) fades out after a few seconds and a
    // tap anywhere brings it back.
    var controlsVisible by remember { mutableStateOf(true) }
    var tapCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(controlsVisible, tapCount) {
        if (controlsVisible) {
            delay(3500)
            controlsVisible = false
        }
    }
    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(350),
        label = "ambientControls",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    controlsVisible = true
                    tapCount++
                }
            },
    ) {
        MeshGradientBackground(
            palette = palette,
            modifier = Modifier.fillMaxSize(),
            trackKey = song.videoId,
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                ),
            ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(48.dp).graphicsLayer { alpha = controlsAlpha },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f))
                        .border(1.dp, glassBorder, CircleShape)
                        .then(if (controlsAlpha > 0.05f) Modifier.clickable(onClick = onClose) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        SfIcons.Close,
                        contentDescription = stringResource(R.string.ambient_close),
                        tint = ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.ambient_mode),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = ink.copy(alpha = 0.75f),
                )
            }
            Spacer(Modifier.height(8.dp))

            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val titleBlock = 64.dp
                val artSize = minOf(maxHeight - titleBlock, maxWidth * 0.36f).coerceAtLeast(120.dp)
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        Modifier.width(artSize),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl?.artworkAt(900),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(artSize)
                                .shadow(28.dp, RoundedCornerShape(28.dp), clip = false)
                                .clip(RoundedCornerShape(28.dp))
                                .background(scheme.surfaceVariant)
                                .border(1.dp, glassBorder, RoundedCornerShape(28.dp)),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ink.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // No card behind the lyrics: they sit straight on the tinted mesh. The
                    // globe / translate / source row hides with the close button.
                    LyricsSidePanel(
                        song = song,
                        isPlaying = isPlaying,
                        position = position,
                        lyrics = lyrics,
                        lyricsSource = lyricsSource,
                        lyricsProviderStates = lyricsProviderStates,
                        onSelectLyricsProvider = onSelectLyricsProvider,
                        lyricsUnavailable = lyricsUnavailable,
                        onSeek = onSeek,
                        controlsAlpha = controlsAlpha,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}
