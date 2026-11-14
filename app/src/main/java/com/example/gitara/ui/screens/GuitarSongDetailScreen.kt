package com.example.gitara.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitara.R
import com.example.gitara.data.DataStoreManager
import com.example.gitara.data.GuitarSong
import com.example.gitara.data.SongsManager
import com.example.gitara.ui.components.GuitarAudioPlayer
import kotlinx.coroutines.delay
import java.io.File

// ВРЕМЕННО: Если вы не вынесли GuitarAudioPlayer и парсеры в отдельные файлы,
// убедитесь, что они доступны. Если они в другом файле пакета ui.screens, импорты не нужны.
// Если нет - раскомментируйте или добавьте их сюда.

// === ЭКРАН ДЕТАЛЕЙ ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuitarSongDetailScreen(
    song: GuitarSong,
    onBackClick: () -> Unit = {},
    onMetronomeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val eqEnabled by dataStoreManager.equalizerEnabled.collectAsState(initial = true)
    val eqValues by dataStoreManager.equalizerValues.collectAsState(initial = listOf(50f, 50f, 50f, 50f, 50f))

    // Загрузка контента
    val songContent = remember(song) {
        runCatching {
            if (song.customLrcFile != null) {
                SongsManager.loadCustomLrc(context, song.customLrcFile)
            } else if (song.customLyricsFile != null) {
                SongsManager.loadCustomLyrics(context, song.customLyricsFile)
                    .lines().map { TimedLine(it, LineType.LYRICS, 0L) }
            } else {
                parseLrcFile(context, song.lrcResId)
            }
        }.getOrElse { emptyList() }
    }

    val listState = rememberLazyListState()
    var mediaPlayer by remember(song) { mutableStateOf<MediaPlayer?>(null) }
    var hasAudio by remember(song) { mutableStateOf(false) }

    // Инициализация плеера
    LaunchedEffect(song) {
        mediaPlayer?.release()
        hasAudio = when {
            song.customAudioFile != null -> {
                val audioFile = File(context.filesDir, "custom_audio/${song.customAudioFile}")
                if (audioFile.exists()) {
                    runCatching {
                        val mp = MediaPlayer.create(context, SongsManager.getAudioUri(context, song.customAudioFile))
                        mp.setVolume(0.2f, 0.2f) // 🔥 ТИХАЯ МУЗЫКА (чтобы слышать метроном)
                        mediaPlayer = mp
                        true
                    }.getOrElse { false }
                } else false
            }
            song.audioResId != 0 -> {
                runCatching {
                    val mp = MediaPlayer.create(context, song.audioResId)
                    mp.setVolume(0.2f, 0.2f) // 🔥 ТИХАЯ МУЗЫКА
                    mediaPlayer = mp
                    true
                }.getOrElse { false }
            }
            else -> false
        }
    }

    // Эквалайзер
    var androidEqualizer by remember { mutableStateOf<Equalizer?>(null) }
    LaunchedEffect(mediaPlayer) {
        mediaPlayer?.let { player ->
            try {
                val eq = Equalizer(0, player.audioSessionId)
                eq.enabled = eqEnabled
                androidEqualizer = eq
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    LaunchedEffect(androidEqualizer, eqEnabled, eqValues) {
        androidEqualizer?.let { eq ->
            eq.enabled = eqEnabled
            if (eqEnabled && eq.numberOfBands >= 5) {
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                eqValues.take(5).forEachIndexed { index, value ->
                    val newLevel = (minLevel + (maxLevel - minLevel) * (value / 100f)).toInt().toShort()
                    if (index < eq.numberOfBands) eq.setBandLevel(index.toShort(), newLevel)
                }
            }
        }
    }

    // Синхронизация позиции
    var currentPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(mediaPlayer) {
        while (true) {
            mediaPlayer?.let { if (it.isPlaying) currentPosition = it.currentPosition.toLong() }
            delay(100)
        }
    }

    // Очистка ресурсов
    DisposableEffect(Unit) {
        onDispose {
            androidEqualizer?.release()
            mediaPlayer?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            navigationIcon = {
                FilledIconButton(
                    onClick = onBackClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(start = 8.dp).size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                FilledIconButton(
                    onClick = onMetronomeClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(end = 8.dp).size(40.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_metronome),
                        contentDescription = "Metronome",
                        modifier = Modifier.size(25.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        GuitarSongContentView(
            songContent = songContent,
            listState = listState,
            mediaPlayer = mediaPlayer,
            currentPosition = currentPosition,
            onPositionChanged = { currentPosition = it },
            hasAudio = hasAudio,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        if (hasAudio && mediaPlayer != null) {
            GuitarAudioPlayer(
                mediaPlayer = mediaPlayer!!,
                currentPosition = currentPosition,
                onSeek = {
                    mediaPlayer?.seekTo(it.toInt())
                    currentPosition = it
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// === КОНТЕНТ ПЕСНИ (список строк) ===
@Composable
fun GuitarSongContentView(
    songContent: List<TimedLine>,
    listState: LazyListState,
    mediaPlayer: MediaPlayer?,
    currentPosition: Long,
    onPositionChanged: (Long) -> Unit,
    hasAudio: Boolean,
    modifier: Modifier = Modifier
) {
    val groupedContent = remember(songContent) { songContent.groupBy { it.timestamp } }
    val timestamps = remember(songContent) { groupedContent.keys.sorted() }
    val currentTimestampIndex = timestamps.indexOfLast { it <= currentPosition }.coerceAtLeast(0)

    LaunchedEffect(currentTimestampIndex) {
        if (!listState.isScrollInProgress) {
            val targetIndex = (currentTimestampIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        itemsIndexed(timestamps) { index, timestamp ->
            val group = groupedContent[timestamp].orEmpty()
            val isCurrent = index == currentTimestampIndex
            val isSection = group.any { it.type == LineType.SECTION }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        color = if (isCurrent && hasAudio && !isSection) MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = mediaPlayer != null && !isSection) {
                        mediaPlayer?.seekTo(timestamp.toInt())
                        mediaPlayer?.start()
                    }
                    .padding(8.dp)
            ) {
                group.forEach {
                    GuitarSongLine(
                        line = it.text,
                        type = it.type,
                        isHighlighted = isCurrent
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(200.dp)) }
    }
}

// === ОТДЕЛЬНАЯ СТРОКА ===
@Composable
fun GuitarSongLine(
    line: String?,
    type: LineType?,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (line.isNullOrBlank()) return
    val monoFont = FontFamily.Monospace
    val baseFontSize = 16.sp
    val chordColor = MaterialTheme.colorScheme.primary
    val lyricColor = if (isHighlighted) Color.White else MaterialTheme.colorScheme.onSurface

    when (type) {
        LineType.SECTION -> {
            Box(
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = line.trim().uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }
        LineType.CHORDS -> {
            Text(
                text = line,
                color = chordColor,
                fontSize = baseFontSize,
                fontFamily = monoFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                lineHeight = baseFontSize * 1.5f
            )
        }
        else -> {
            Text(
                text = line,
                color = lyricColor,
                fontSize = baseFontSize,
                fontFamily = monoFont,
                lineHeight = baseFontSize * 1.5f
            )
        }
    }
}
