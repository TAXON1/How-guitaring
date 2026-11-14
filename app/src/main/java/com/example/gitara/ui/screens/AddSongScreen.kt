package com.example.gitara.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitara.data.DataStoreManager
import com.example.gitara.data.GuitarSong
import com.example.gitara.data.SongsManager
import com.example.gitara.data.containsLrcTimings
import kotlinx.coroutines.launch

@Composable
fun AddSongScreen(
    songToEdit: GuitarSong? = null,
    onBackClick: () -> Unit,
    onSongSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }

    var title by remember(songToEdit) {
        mutableStateOf(songToEdit?.title ?: "")
    }
    var difficulty by remember(songToEdit) {
        mutableStateOf(songToEdit?.difficulty ?: 3)
    }

    // 🔥 LRC редактор
    var songText by remember(songToEdit) { mutableStateOf(
        when {
            songToEdit?.customLrcFile != null -> {
                SongsManager.loadCustomLrc(context, songToEdit.customLrcFile)
                    .joinToString("\n") { timedLine ->
                        val minutes = (timedLine.timestamp / 60000).toString().padStart(2, '0')
                        val seconds = ((timedLine.timestamp / 1000) % 60).toString().padStart(2, '0')
                        val millis = (timedLine.timestamp % 1000).toString().padStart(3, '0')
                        "[$minutes:$seconds.$millis]${timedLine.text}"
                    }
            }
            songToEdit?.customLyricsFile != null -> {
                SongsManager.loadCustomLyrics(context, songToEdit.customLyricsFile)
            }
            else -> ""
        }
    ) }

    var audioFileName by remember(songToEdit) {
        mutableStateOf(songToEdit?.customAudioFile)
    }
    var audioDuration by remember(audioFileName) {
        mutableStateOf(
            if (audioFileName != null) SongsManager.getAudioDuration(context, audioFileName)
            else "???"
        )
    }
    var removeAudio by remember { mutableStateOf(false) }

    // 🔥 Explicit флаг
    var isExplicit by remember(songToEdit) {
        mutableStateOf(songToEdit?.isExplicit ?: false)
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            SongsManager.copyAudioToApp(context, it)?.let { newFileName ->
                audioFileName = newFileName
                audioDuration = SongsManager.getAudioDuration(context, newFileName)
                removeAudio = false
            }
        }
    }

    val isLrc by remember(songText) {
        mutableStateOf(containsLrcTimings(songText))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Верхняя панель
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onBackClick),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (songToEdit == null) "Новая песня" else "Редактировать",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название песни") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Column {
                    Text("Сложность", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    GuitarStarRatingPicker(stars = difficulty, onStarsChange = { difficulty = it })
                }
            }

            // 🔥 Explicit переключатель (Стиль: Secondary/Pink)
            item {
                // Анимация цвета фона значка (Серый -> Розовый)
                val explicitColor by animateColorAsState(
                    targetValue = if (isExplicit) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "explicitColor"
                )

                // Анимация цвета буквы (Серый -> Белый/Контрастный)
                val iconColor by animateColorAsState(
                    targetValue = if (isExplicit) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    label = "iconColor"
                )

                // Анимация обводки всей карточки (Прозрачная -> Розовая)
                val borderColor by animateColorAsState(
                    targetValue = if (isExplicit) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else Color.Transparent,
                    label = "borderColor"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { isExplicit = !isExplicit }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Имитация значка "E"
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(explicitColor, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "E",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = iconColor
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 2. Текст
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Explicit Content",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isExplicit) "Содержит нецензурную лексику" else "Без возрастных ограничений",
                            fontSize = 13.sp,
                            // Цвет текста описания тоже меняем на розовый при активации
                            color = if (isExplicit) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3. Переключатель (Розовый)
                    Switch(
                        checked = isExplicit,
                        onCheckedChange = { isExplicit = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary, // Розовый трек
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }


            // 🔥 LRC редактор
            item {
                OutlinedTextField(
                    value = songText,
                    onValueChange = { songText = it },
                    label = { Text(if (isLrc) "LRC с таймингами" else "Аккорды + текст") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp),
                    minLines = 11,
                    maxLines = 20,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            item {
                Text(
                    text = if (isLrc) "LRC файл" else "Аккорды + текст",
                    color = if (isLrc) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    fontSize = 14.sp
                )
            }

            // Аудио
            item {
                Button(
                    onClick = { audioPicker.launch("audio/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AudioFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(when {
                        audioFileName == null && !removeAudio -> "Выбрать аудио"
                        removeAudio -> "Удалить аудио"
                        else -> audioDuration
                    })
                }

                if (audioFileName != null) {
                    TextButton(
                        onClick = {
                            removeAudio = true
                            audioFileName = null
                            audioDuration = "???"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("❌ Удалить аудио")
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            if (title.isBlank() || songText.isBlank()) return@launch

                            if (songToEdit == null) {
                                SongsManager.addCustomSong(
                                    context = context,
                                    title = title.trim(),
                                    difficulty = difficulty,
                                    lyricsWithChords = if (containsLrcTimings(songText)) songText else songText,
                                    audioFileName = audioFileName,
                                    lrcText = if (containsLrcTimings(songText)) songText else null,
                                    isExplicit = isExplicit  // 🔥
                                )
                            } else {
                                SongsManager.updateCustomSong(
                                    context = context,
                                    originalSong = songToEdit,
                                    title = title,
                                    difficulty = difficulty,
                                    lyricsText = songText,
                                    audioFileName = audioFileName,
                                    removeAudio = removeAudio,
                                    isExplicit = isExplicit  // 🔥
                                )
                            }
                            onSongSaved()
                        }
                    },
                    enabled = title.isNotBlank() && songText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (songToEdit == null) "Сохранить новую песню" else "Обновить песню")
                }
            }
        }
    }
}

@Composable
fun GuitarStarRatingPicker(stars: Int, onStarsChange: (Int) -> Unit) {
    Row {
        repeat(5) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (index < stars)
                    MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onStarsChange(index + 1) }
            )
        }
    }
}
