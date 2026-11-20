package com.example.mymediaexpressive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.media.session.MediaSessionCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

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

    val darkTheme = when (activity.themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    ExpressiveMediaTheme(darkTheme = darkTheme) {
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
                TopAppBar(
                    title = { Text("Музыка", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        TextButton(onClick = { activity.showQueue = true }) {
                            Text("Очередь")
                        }
                        TextButton(onClick = { activity.showSettings = true }) {
                            Text("Настр.")
                        }
                    },
                )
            },
            bottomBar = {
                if (activity.currentTrack() != null) {
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
                ScrollableTabRow(selectedTabIndex = activity.selectedTab) {
                    Tab(selected = activity.selectedTab == 0, onClick = { activity.selectedTab = 0 }, text = { Text("Вся музыка") })
                    Tab(selected = activity.selectedTab == 1, onClick = { activity.selectedTab = 1 }, text = { Text("Плейлисты") })
                    Tab(selected = activity.selectedTab == 2, onClick = { activity.selectedTab = 2 }, text = { Text("Избранное") })
                    Tab(selected = activity.selectedTab == 3, onClick = { activity.selectedTab = 3 }, text = { Text("Недавние") })
                    Tab(selected = activity.selectedTab == 4, onClick = { activity.selectedTab = 4 }, text = { Text("Альбомы") })
                }
                when (activity.selectedTab) {
                    0 -> MusicList(activity)
                    1 -> PlaylistsTab(activity)
                    2 -> FavoritesTab(activity)
                    3 -> RecentTab(activity)
                    4 -> AlbumsTab(activity)
                }
            }
        }

        if (activity.showPlayer) {
            FullPlayer(activity)
        }
        if (activity.showSettings) {
            SettingsSheet(activity)
        }
        if (activity.showQueue) {
            QueueSheet(activity)
        }
        if (activity.contextTrack != null) {
            ContextMenu(activity)
        }
    }
}

@Composable
fun MusicList(activity: MainActivity) {
    val tracks = activity.visibleTracks()
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = activity.searchQuery,
            onValueChange = { activity.searchQuery = it },
            label = { Text("Поиск") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { activity.sortMode = 0 }) {
                Text(if (activity.sortMode == 0) "✓ Имя" else "Имя")
            }
            TextButton(onClick = { activity.sortMode = 1 }) {
                Text(if (activity.sortMode == 1) "✓ Исполнитель" else "Исполнитель")
            }
            TextButton(onClick = { activity.sortMode = 2 }) {
                Text(if (activity.sortMode == 2) "✓ Длительность" else "Длительность")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { activity.shuffleAll() }) {
                Text("Перемешать")
            }
        }
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Ничего не найдено",
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
                                isCurrent = track.id == activity.currentTrack()?.id,
                                isFavorite = activity.isFavorite(track.id),
                                onClick = { activity.playTrack(track) },
                                onLongClick = { activity.contextTrack = track },
                                onFavorite = { activity.toggleFavorite(track.id) },
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
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongClick() }) }
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
        TextButton(onClick = onFavorite) {
            Text(if (isFavorite) "♥" else "♡")
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = activity.newPlaylistName,
                onValueChange = { activity.newPlaylistName = it },
                label = { Text("Имя плейлиста") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            FilledTonalButton(onClick = { activity.createPlaylist() }) {
                Text("Создать")
            }
        }
        Spacer(Modifier.height(12.dp))
        val playlists = activity.playlists
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Плейлистов пока нет",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }
        LazyColumn {
            itemsIndexed(playlists) { index, pl ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { activity.openPlaylist(index) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${pl.trackIds.size} треков",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { activity.deletePlaylist(index) }) {
                            Text("Удалить")
                        }
                    }
                }
                if (activity.openedPlaylistIndex == index) {
                    val plTracks = activity.tracks.filter { it.id in pl.trackIds }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        plTracks.forEach { track ->
                            TrackRow(
                                track = track,
                                isCurrent = track.id == activity.currentTrack()?.id,
                                isFavorite = activity.isFavorite(track.id),
                                onClick = { activity.playTrack(track) },
                                onLongClick = { activity.contextTrack = track },
                                onFavorite = { activity.toggleFavorite(track.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTab(activity: MainActivity) {
    val favTracks = activity.tracks.filter { activity.isFavorite(it.id) }
    if (favTracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Избранных пока нет",
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
                    favTracks.forEachIndexed { index, track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == activity.currentTrack()?.id,
                            isFavorite = true,
                            onClick = { activity.playTrack(track) },
                            onLongClick = { activity.contextTrack = track },
                            onFavorite = { activity.toggleFavorite(track.id) },
                        )
                        if (index < favTracks.lastIndex) {
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
fun RecentTab(activity: MainActivity) {
    val recentTracks = activity.recentTrackObjects()
    if (recentTracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Недавно воспроизведённых нет",
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
                    recentTracks.forEachIndexed { index, track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == activity.currentTrack()?.id,
                            isFavorite = activity.isFavorite(track.id),
                            onClick = { activity.playTrack(track) },
                            onLongClick = { activity.contextTrack = track },
                            onFavorite = { activity.toggleFavorite(track.id) },
                        )
                        if (index < recentTracks.lastIndex) {
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
fun AlbumsTab(activity: MainActivity) {
    val albums = activity.tracks.groupBy { it.albumId }
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Альбомов нет",
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
        items(albums.entries.toList()) { (albumId, tracks) ->
            val first = tracks.first()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { activity.playTrack(first) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = albumArtUri(albumId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = first.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = first.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${tracks.size} треков",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(activity: MainActivity) {
    val track = activity.currentTrack() ?: return
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
    val track = activity.currentTrack() ?: return
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
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = { activity.toggleShuffle() }) {
                    Text(if (activity.shuffleEnabled) "Shuffle ON" else "Shuffle")
                }
                FilledTonalButton(onClick = { activity.cycleRepeat() }) {
                    Text(
                        when (activity.repeatMode) {
                            1 -> "Repeat All"
                            2 -> "Repeat One"
                            else -> "Repeat Off"
                        },
                    )
                }
                FilledTonalButton(onClick = { activity.toggleFavorite(track.id) }) {
                    Text(if (activity.isFavorite(track.id)) "♥" else "♡")
                }
            }
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = { activity.cycleSpeed() }) {
                    Text("Speed ${activity.playbackSpeed}x")
                }
                FilledTonalButton(onClick = { activity.cycleSleepTimer() }) {
                    Text(
                        if (activity.sleepTimerMinutes == 0) "Sleep off"
                        else "Sleep ${activity.sleepTimerMinutes}m",
                    )
                }
                FilledTonalButton(onClick = { activity.showQueue = true }) {
                    Text("Очередь")
                }
            }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(onClick = { activity.showPlayer = false }) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
fun QueueSheet(activity: MainActivity) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(
                text = "Очередь воспроизведения",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            if (activity.queueIds.isEmpty()) {
                Text(
                    text = "Очередь пуста",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn {
                    items(activity.queueIds) { id ->
                        val t = activity.tracks.firstOrNull { it.id == id }
                        if (t != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = t.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { activity.removeFromQueue(id) }) {
                                    Text("Убрать")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { activity.clearQueue() }) {
                    Text("Очистить")
                }
                FilledTonalButton(onClick = { activity.showQueue = false }) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
fun ContextMenu(activity: MainActivity) {
    val track = activity.contextTrack ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = { activity.playTrack(track); activity.contextTrack = null }) {
                Text("Воспроизвести")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { activity.playNext(track); activity.contextTrack = null }) {
                Text("Воспроизвести следующим")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { activity.addToQueue(track); activity.contextTrack = null }) {
                Text("Добавить в очередь")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { activity.toggleFavorite(track.id); activity.contextTrack = null }) {
                Text(if (activity.isFavorite(track.id)) "Убрать из избранного" else "В избранное")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { activity.contextTrack = null }) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
fun SettingsSheet(activity: MainActivity) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Тёмная тема", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = activity.themeMode == 2,
                    onCheckedChange = { activity.themeMode = if (it) 2 else 1 },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Следовать системе", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = activity.themeMode == 0,
                    onCheckedChange = { if (it) activity.themeMode = 0 },
                )
            }
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = { activity.loadTracks() }) {
                Text("Пересканировать музыку")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { activity.clearRecent() }) {
                Text("Очистить недавние")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { activity.showSettings = false }) {
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

data class Playlist(val name: String, val trackIds: MutableList<Long> = mutableListOf())

class MainActivity : ComponentActivity() {

    private var player: MediaPlayer? = null
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val notificationChannelId = "expressive_player"
    private val notificationId = 1

    var tracks by mutableStateOf(emptyList<Track>())
    var selectedTab by mutableStateOf(0)
    var currentTrackIndex by mutableStateOf(-1)
    var isPlaying by mutableStateOf(false)
    var positionMs by mutableStateOf(0L)
    var durationMs by mutableStateOf(0L)
    var showPlayer by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showQueue by mutableStateOf(false)
    var contextTrack by mutableStateOf<Track?>(null)
    var searchQuery by mutableStateOf("")
    var sortMode by mutableStateOf(0)
    var shuffleEnabled by mutableStateOf(false)
    var repeatMode by mutableStateOf(0)
    var playbackSpeed by mutableStateOf(1.0f)
    var sleepTimerMinutes by mutableStateOf(0)
    var themeMode by mutableStateOf(0)
    var newPlaylistName by mutableStateOf("")
    var openedPlaylistIndex by mutableStateOf(-1)
    var playlists by mutableStateOf(emptyList<Playlist>())
    var favorites by mutableStateOf(emptySet<Long>())
    var queueIds by mutableStateOf(emptyList<Long>())
    var recentIds by mutableStateOf(emptyList<Long>())

    private val prefs by lazy { getSharedPreferences("player", Context.MODE_PRIVATE) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) loadTracks()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPrefs()
        setupNotificationChannel()
        setupMediaSession()
        requestAudioFocus()
        playbackScope.launch {
            while (isActive) {
                delay(500)
                positionMs = (player?.currentPosition ?: 0).toLong()
                checkSleepTimer()
            }
        }
        setContent {
            ExpressivePlayerApp()
        }
        if (hasAudioPermission()) {
            loadTracks()
            restoreLastPlayback()
        } else requestAudioPermission()
    }

    override fun onDestroy() {
        saveLastPlayback()
        mediaSession.release()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
        playbackScope.cancel()
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Музыкальный плеер",
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.description = "Управление воспроизведением"
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "ExpressivePlayer")
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { playPause() }
            override fun onPause() { playPause() }
            override fun onSkipToNext() { nextTrack() }
            override fun onSkipToPrevious() { prevTrack() }
            override fun onSeekTo(pos: Long) {
                val dur = durationMs.takeIf { it > 0 } ?: 1L
                seekTo(pos.toFloat() / dur.toFloat())
            }
        })
        mediaSession.isActive = true
    }

    private fun requestAudioFocus() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { change ->
                    when (change) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            player?.pause()
                            isPlaying = false
                            updateNotification()
                        }
                    }
                }
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        }
    }

    fun updateNotification() {
        val track = currentTrack() ?: return
        val nm = getSystemService(NotificationManager::class.java)
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .setOngoing(isPlaying)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .addAction(android.R.drawable.ic_media_previous, "Prev", null)
            .addAction(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, if (isPlaying) "Pause" else "Play", null)
            .addAction(android.R.drawable.ic_media_next, "Next", null)
            .build()
        nm.notify(notificationId, notification)
        mediaSession.setMetadata(
            android.media.MediaMetadata.Builder()
                .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, track.duration)
                .build(),
        )
        mediaSession.setPlaybackState(
            android.media.session.PlaybackState.Builder()
                .setState(
                    if (isPlaying) android.media.session.PlaybackState.STATE_PLAYING
                    else android.media.session.PlaybackState.STATE_PAUSED,
                    positionMs,
                    playbackSpeed,
                )
                .setActions(
                    android.media.session.PlaybackState.ACTION_PLAY or
                        android.media.session.PlaybackState.ACTION_PAUSE or
                        android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT or
                        android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        android.media.session.PlaybackState.ACTION_SEEK_TO,
                )
                .build(),
        )
    }

    private fun restoreLastPlayback() {
        val lastId = prefs.getLong("lastTrackId", -1L)
        if (lastId < 0) return
        val idx = tracks.indexOfFirst { it.id == lastId }
        if (idx < 0) return
        currentTrackIndex = idx
        val track = tracks[idx]
        player?.release()
        val p = MediaPlayer()
        p.setDataSource(this, track.uri)
        p.setOnPreparedListener { mp ->
            durationMs = mp.duration.toLong()
            val savedPos = prefs.getLong("lastPosition", 0L).toInt()
            if (savedPos > 0) mp.seekTo(savedPos)
            positionMs = savedPos.toLong()
            applySpeed(p)
            isPlaying = false
        }
        p.prepareAsync()
        player = p
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

    private fun saveLastPlayback() {
        val track = currentTrack() ?: return
        prefs.edit().apply {
            putLong("lastTrackId", track.id)
            putLong("lastPosition", positionMs)
            apply()
        }
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

    fun visibleTracks(): List<Track> {
        val q = searchQuery.trim().lowercase()
        val filtered = if (q.isEmpty()) tracks else tracks.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
        }
        return when (sortMode) {
            1 -> filtered.sortedBy { it.artist }
            2 -> filtered.sortedByDescending { it.duration }
            else -> filtered.sortedBy { it.title }
        }
    }

    fun currentTrack(): Track? {
        // find by index in tracks
        return tracks.getOrNull(currentTrackIndex)
    }

    fun playTrack(track: Track) {
        val idx = tracks.indexOfFirst { it.id == track.id }
        if (idx < 0) return
        currentTrackIndex = idx
        startPlayback(track)
        recordRecent(track.id)
    }

    private fun startPlayback(track: Track) {
        player?.release()
        val p = MediaPlayer()
        p.setDataSource(this, track.uri)
        p.setOnPreparedListener { mp ->
            durationMs = mp.duration.toLong()
            applySpeed(p)
            mp.start()
            isPlaying = true
            updateNotification()
            saveLastPlayback()
        }
        p.setOnCompletionListener {
            onTrackCompleted()
        }
        p.prepareAsync()
        player = p
    }

    private fun onTrackCompleted() {
        when (repeatMode) {
            2 -> {
                currentTrack()?.let { startPlayback(it) }
            }
            else -> {
                if (shuffleEnabled) {
                    if (tracks.isNotEmpty()) {
                        val next = (0 until tracks.size).random()
                        playTrack(tracks[next])
                    }
                } else {
                    nextTrack()
                }
            }
        }
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
        updateNotification()
        saveLastPlayback()
    }

    fun nextTrack() {
        if (tracks.isEmpty()) return
        val next = if (shuffleEnabled) (0 until tracks.size).random()
        else (currentTrackIndex + 1) % tracks.size
        playTrack(tracks[next])
    }

    fun prevTrack() {
        if (tracks.isEmpty()) return
        val pos = positionMs
        if (pos > 3000) {
            player?.seekTo(0)
            positionMs = 0
            return
        }
        val prev = if (currentTrackIndex <= 0) tracks.size - 1 else currentTrackIndex - 1
        playTrack(tracks[prev])
    }

    fun seekTo(fraction: Float) {
        val p = player ?: return
        val dur = durationMs
        if (dur > 0) {
            p.seekTo((fraction * dur).toInt())
        }
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
    }

    fun cycleRepeat() {
        repeatMode = (repeatMode + 1) % 3
    }

    fun toggleFavorite(id: Long) {
        favorites = if (id in favorites) favorites - id else favorites + id
        savePrefs()
    }

    fun isFavorite(id: Long): Boolean = id in favorites

    fun cycleSpeed() {
        playbackSpeed = when (playbackSpeed) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        player?.let { applySpeed(it) }
    }

    private fun applySpeed(p: MediaPlayer) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                p.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
            }
        } catch (_: Exception) {
        }
    }

    fun cycleSleepTimer() {
        sleepTimerMinutes = when (sleepTimerMinutes) {
            0 -> 15
            15 -> 30
            30 -> 45
            45 -> 60
            else -> 0
        }
        if (sleepTimerMinutes == 0) sleepTimerEnd = 0L
        else sleepTimerEnd = System.currentTimeMillis() + sleepTimerMinutes * 60_000L
    }

    private var sleepTimerEnd: Long = 0L

    private fun checkSleepTimer() {
        if (sleepTimerMinutes == 0) return
        if (sleepTimerEnd == 0L) {
            sleepTimerEnd = System.currentTimeMillis() + sleepTimerMinutes * 60_000L
        }
        if (System.currentTimeMillis() >= sleepTimerEnd) {
            player?.pause()
            isPlaying = false
            sleepTimerMinutes = 0
            sleepTimerEnd = 0L
        }
    }

    fun shuffleAll() {
        if (tracks.isEmpty()) return
        val shuffled = tracks.shuffled()
        // play first of shuffled by reordering? Just play random
        playTrack(shuffled.first())
    }

    // ---- Queue ----
    fun playNext(track: Track) {
        queueIds = listOf(track.id) + queueIds
    }

    fun addToQueue(track: Track) {
        queueIds = queueIds + track.id
    }

    fun removeFromQueue(id: Long) {
        queueIds = queueIds.filter { it != id }
    }

    fun clearQueue() {
        queueIds = emptyList()
    }

    // ---- Recent ----
    private fun recordRecent(id: Long) {
        recentIds = (listOf(id) + recentIds.filter { it != id }).take(30)
        savePrefs()
    }

    fun recentTrackObjects(): List<Track> =
        recentIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }

    fun clearRecent() {
        recentIds = emptyList()
        savePrefs()
    }

    // ---- Playlists ----
    data class Playlist(val name: String, val trackIds: MutableList<Long> = mutableListOf())

    fun createPlaylist() {
        val name = newPlaylistName.trim()
        if (name.isEmpty()) return
        playlists = playlists + Playlist(name)
        newPlaylistName = ""
        savePrefs()
    }

    fun deletePlaylist(index: Int) {
        if (index !in playlists.indices) return
        playlists = playlists.filterIndexed { i, _ -> i != index }
        if (openedPlaylistIndex == index) openedPlaylistIndex = -1
        savePrefs()
    }

    fun openPlaylist(index: Int) {
        openedPlaylistIndex = if (openedPlaylistIndex == index) -1 else index
    }

    // ---- prefs ----
    private fun loadPrefs() {
        themeMode = prefs.getInt("themeMode", 0)
        favorites = prefs.getStringSet("favorites", emptySet())?.map { it.toLong() }?.toSet() ?: emptySet()
        val plJson = prefs.getString("playlists", "[]") ?: "[]"
        val arr = JSONArray(plJson)
        val list = mutableListOf<Playlist>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.getString("name")
            val ids = o.getJSONArray("ids")
            val pl = Playlist(name)
            for (j in 0 until ids.length()) pl.trackIds.add(ids.getLong(j))
            list.add(pl)
        }
        playlists = list
        val rq = prefs.getString("recent", "[]") ?: "[]"
        val rarr = JSONArray(rq)
        val rlist = mutableListOf<Long>()
        for (i in 0 until rarr.length()) rlist.add(rarr.getLong(i))
        recentIds = rlist
    }

    private fun savePrefs() {
        prefs.edit().apply {
            putInt("themeMode", themeMode)
            putStringSet("favorites", favorites.map { it.toString() }.toSet())
            val arr = JSONArray()
            playlists.forEach { pl ->
                val o = JSONObject()
                o.put("name", pl.name)
                val ids = JSONArray()
                pl.trackIds.forEach { ids.put(it) }
                o.put("ids", ids)
                arr.put(o)
            }
            putString("playlists", arr.toString())
            val rarr = JSONArray()
            recentIds.forEach { rarr.put(it) }
            putString("recent", rarr.toString())
            apply()
        }
    }
}
