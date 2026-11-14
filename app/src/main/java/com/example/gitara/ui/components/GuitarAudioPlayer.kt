package com.example.gitara.ui.components // Или com.example.gitara.ui.screens, зависит от вашей структуры

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun GuitarAudioPlayer(
    mediaPlayer: MediaPlayer,
    currentPosition: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Безопасное получение длительности (защита от краша при поврежденном файле)
    val duration = remember(mediaPlayer) {
        try {
            mediaPlayer.duration.coerceAtLeast(0).toLong()
        } catch (e: Exception) {
            0L
        }
    }

    // Локальное состояние для иконки Play/Pause
    var isPlaying by remember { mutableStateOf(mediaPlayer.isPlaying) }

    // Периодическая проверка статуса проигрывания для обновления UI
    LaunchedEffect(mediaPlayer) {
        while (true) {
            try {
                isPlaying = mediaPlayer.isPlaying
            } catch (e: Exception) {
                isPlaying = false
            }
            delay(200)
        }
    }

    // Функция форматирования времени (ММ:СС)
    fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(16.dp)
    ) {
        // Слайдер прогресса
        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // Контролы (Время - Кнопка - Время)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodyMedium
            )

            // Кнопка Play/Pause
            FilledIconButton(
                onClick = {
                    try {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                            isPlaying = false
                        } else {
                            mediaPlayer.start()
                            isPlaying = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
