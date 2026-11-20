package com.example.mymediaexpressive

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressivePlayerApp(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    positionMs: Long,
    durationMs: Long,
    trackName: String,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onRepeatChange: (Boolean) -> Unit,
) {
    val progress: () -> Float = remember(positionMs, durationMs) {
        {
            if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else Float.NaN
        }
    }

    val sliderState = rememberSliderState(
        value = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
        steps = 0,
        trackRange = 0f..1f,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = trackName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = Alignment.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        CircularWavyProgressIndicator(
            progress = { progress() },
            modifier = Modifier.size(260.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Slider(
            state = sliderState,
            onValueChange = { fraction ->
                sliderState.value = fraction
                onSeek(fraction)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTime(positionMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatTime(durationMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = shuffleEnabled,
                onCheckedChange = onShuffleChange,
                modifier = Modifier.padding(end = 16.dp),
            )
            FilledTonalButton(
                onClick = onPlayPause,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = repeatEnabled,
                onCheckedChange = onRepeatChange,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveMediaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
        typography = Typography(
            displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, lineHeight = 64.sp),
            headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
            titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
        ),
    ) {
        content()
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

class MainActivity : ComponentActivity() {

    private var player: MediaPlayer? = null

    private val playingState = mutableStateOf(false)
    private val shuffleState = mutableStateOf(false)
    private val repeatState = mutableStateOf(false)
    private val positionState = mutableStateOf(0L)
    private val durationState = mutableStateOf(0L)

    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var isPlaying: Boolean
        get() = playingState.value
        set(value) { playingState.value = value }

    var shuffleEnabled: Boolean
        get() = shuffleState.value
        set(value) { shuffleState.value = value }

    var repeatEnabled: Boolean
        get() = repeatState.value
        set(value) { repeatState.value = value }

    var positionMs: Long
        get() = positionState.value
        set(value) { positionState.value = value }

    var durationMs: Long
        get() = durationState.value
        set(value) { durationState.value = value }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playbackScope.launch {
            while (isActive) {
                delay(500)
                positionState.value = player?.currentPosition ?: 0L
            }
        }
        setContent {
            ExpressivePlayerApp(
                isPlaying = isPlaying,
                shuffleEnabled = shuffleEnabled,
                repeatEnabled = repeatEnabled,
                positionMs = positionMs,
                durationMs = durationMs,
                trackName = "Material Expressive Mix",
                onPlayPause = ::onPlayPauseClicked,
                onSeek = ::seekTo,
                onShuffleChange = ::setShuffleEnabled,
                onRepeatChange = ::setRepeatEnabled,
            )
        }
    }

    override fun onDestroy() {
        playbackScope.cancel()
        player?.release()
        player = null
        super.onDestroy()
    }

    fun onPlayPauseClicked() {
        val player = player ?: return
        if (isPlaying) player.pause() else player.start()
        isPlaying = !isPlaying
    }

    fun seekTo(fraction: Float) {
        val player = player ?: return
        val duration = durationMs
        if (duration > 0) {
            player.seekTo((fraction * duration).toInt())
        }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled = enabled
    }

    fun setRepeatEnabled(enabled: Boolean) {
        repeatEnabled = enabled
    }
}
