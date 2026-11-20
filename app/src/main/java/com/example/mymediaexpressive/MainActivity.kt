package com.example.mymediaexpressive

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val albumId: Long,
    val duration: Long,
)

fun albumArtUri(albumId: Long): Uri =
    android.content.ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart"),
        albumId,
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressivePlayerApp() {
    val context = LocalContext.current
    val activity = context as? MainActivity ?: return

    var permissionGranted by remember { mutableStateOf(activity.hasAudioPermission()) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (granted) activity.loadTracks()
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted && activity.tracks.isEmpty()) activity.loadTracks()
    }

    ExpressiveMediaTheme {
        if (!permissionGranted) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Нужен доступ к аудиофайлам",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = {
                        launcher.launch(activity.audioPermission())
                    }) {
                        Text("Разрешить")
                    }
                }
            }
            return@ExpressiveMediaTheme
        }
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Text("Музыка", style = MaterialTheme.typography.titleLarge)
                })
            },
            bottomBar = {
                if (activity.currentTrackIndex >= 0) {
                    MiniPlayer(activity)
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(padding),
            ) {
                TabRow(selectedTabIndex = activity.selectedTab) {
                    Tab(
                        selected = activity.selectedTab == 0,
                        onClick = { activity.selectedTab = 0 },
                        text = { Text("Вся музыка") },
                    )
                    Tab(
                        selected = activity.selectedTab == 1,
                        onClick = { activity.selectedTab = 1 },
                        text = { Text("Плейлисты") },
                    )
                }
                when (activity.selectedTab) {
                    0 -> MusicList(activity)
                    1 -> PlaylistsTab(activity)
                }
            }
        }

        if (activity.showPlayer) {
            FullPlayer(activity)
        }
    }
}

@Composable
fun MusicList(activity: MainActivity) {
    val tracks = activity.tracks
    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "В Download и Music ничего не найдено",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    tracks.forEachIndexed { index, track ->
                        TrackRow(
                            track = track,
                            index = index,
                            isCurrent = index == activity.currentTrackIndex,
                            onClick = { activity.playTrack(index) },
                        )
                        if (index < tracks.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackRow(track: Track, index: Int, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = albumArtUri(track.albumId),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatTime(track.duration),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PlaylistsTab(activity: MainActivity) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Плейлисты скоро появятся",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun MiniPlayer(activity: MainActivity) {
    val track = activity.tracks.getOrNull(activity.currentTrackIndex) ?: return
    val fraction = if (activity.durationMs > 0)
        activity.positionMs.toFloat() / activity.durationMs.toFloat() else 0f
    Card(
        onClick = { activity.showPlayer = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = albumArtUri(track.albumId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledTonalButton(onClick = { activity.playPause() }) {
                    Text(if (activity.isPlaying) "Pause" else "Play")
                }
            }
            Slider(
                state = rememberSliderState(
                    value = fraction,
                    steps = 0,
                    trackRange = 0f..1f,
                ),
                onValueChange = { activity.seekTo(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
fun FullPlayer(activity: MainActivity) {
    val track = activity.tracks.getOrNull(activity.currentTrackIndex) ?: return
    val fraction = if (activity.durationMs > 0)
        activity.positionMs.toFloat() / activity.durationMs.toFloat() else 0f

    val sliderState = rememberSliderState(
        value = fraction,
        steps = 0,
        trackRange = 0f..1f,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            AsyncImage(
                model = albumArtUri(track.albumId),
                contentDescription = null,
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(24.dp))
            Slider(
                state = sliderState,
                onValueChange = { f ->
                    sliderState.value = f
                    activity.seekTo(f)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTime(activity.positionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatTime(activity.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = { activity.prevTrack() }) {
                    Text("Prev")
                }
                FilledTonalButton(onClick = { activity.playPause() }) {
                    Text(if (activity.isPlaying) "Pause" else "Play")
                }
                FilledTonalButton(onClick = { activity.nextTrack() }) {
                    Text("Next")
                }
            }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(onClick = { activity.showPlayer = false }) {
                Text("Закрыть")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveMediaTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
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
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var tracks by mutableStateOf(emptyList<Track>())
    var selectedTab by mutableStateOf(0)
    var currentTrackIndex by mutableStateOf(-1)
    var isPlaying by mutableStateOf(false)
    var positionMs by mutableStateOf(0L)
    var durationMs by mutableStateOf(0L)
    var showPlayer by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) loadTracks()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playbackScope.launch {
            while (isActive) {
                delay(500)
                positionMs = (player?.currentPosition ?: 0).toLong()
            }
        }
        setContent {
            ExpressivePlayerApp()
        }
        if (hasAudioPermission()) loadTracks() else requestAudioPermission()
    }

    override fun onDestroy() {
        playbackScope.cancel()
        player?.release()
        player = null
        super.onDestroy()
    }

    fun hasAudioPermission(): Boolean {
        val perm = audioPermission()
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            android.Manifest.permission.READ_MEDIA_AUDIO
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE

    fun requestAudioPermission() {
        permissionLauncher.launch(audioPermission())
    }

    fun loadTracks() {
        val list = mutableListOf<Track>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.RELATIVE_PATH,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "(${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?)"
        val args = arrayOf("%Download%", "%Music%")
        val sort = "${MediaStore.Audio.Media.TITLE} ASC"
        contentResolver.query(uri, projection, selection, args, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val title = c.getString(titleCol) ?: "Без названия"
                val artist = c.getString(artistCol) ?: "Неизвестный исполнитель"
                val albumId = c.getLong(albumCol)
                val duration = c.getLong(durCol)
                val trackUri = android.content.ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id,
                )
                list.add(Track(id, trackUri, title, artist, albumId, duration))
            }
        }
        tracks = list
    }

    fun playTrack(index: Int) {
        if (index < 0 || index >= tracks.size) return
        currentTrackIndex = index
        val track = tracks[index]
        player?.release()
        val p = MediaPlayer()
        p.setDataSource(this, track.uri)
        p.setOnPreparedListener { mp ->
            durationMs = mp.duration.toLong()
            mp.start()
            isPlaying = true
        }
        p.setOnCompletionListener {
            nextTrack()
        }
        p.prepareAsync()
        player = p
    }

    fun playPause() {
        val p = player ?: return
        if (isPlaying) {
            p.pause()
            isPlaying = false
        } else {
            p.start()
            isPlaying = true
        }
    }

    fun nextTrack() {
        if (tracks.isEmpty()) return
        val next = (currentTrackIndex + 1) % tracks.size
        playTrack(next)
    }

    fun prevTrack() {
        if (tracks.isEmpty()) return
        val prev = if (currentTrackIndex <= 0) tracks.size - 1 else currentTrackIndex - 1
        playTrack(prev)
    }

    fun seekTo(fraction: Float) {
        val p = player ?: return
        val dur = durationMs
        if (dur > 0) {
            p.seekTo((fraction * dur).toInt())
        }
    }
}
