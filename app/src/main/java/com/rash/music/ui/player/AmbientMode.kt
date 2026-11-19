package com.rash.music.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        MeshGradientBackground(
            palette = palette,
            modifier = Modifier.fillMaxSize(),
            trackKey = song.videoId,
        )
        // Pulls the mesh toward the theme's own background so text stays legible
        // in both light and dark themes.
        Box(Modifier.fillMaxSize().background(scheme.background.copy(alpha = 0.38f)))

        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
            val artSize = minOf(maxHeight - 56.dp, maxWidth * 0.42f)
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.width(artSize).fillMaxHeight(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val art = song.thumbnailUrl
                    AsyncImage(
                        model = art?.artworkAt(900),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(artSize)
                            .clip(RoundedCornerShape(24.dp))
                            .background(scheme.surfaceVariant),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onBackground.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(scheme.surface.copy(alpha = 0.22f)),
                ) {
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(scheme.onBackground.copy(alpha = 0.12f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(SfIcons.Close, contentDescription = null, tint = scheme.onBackground, modifier = Modifier.size(18.dp))
            }
        }
    }
}
