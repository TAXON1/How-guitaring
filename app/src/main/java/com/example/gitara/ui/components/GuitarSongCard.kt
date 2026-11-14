package com.example.gitara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitara.data.GuitarSong

// Расширение для проверки, кастомная ли песня (нужно для отображения кнопок ред./удаления)
val GuitarSong.isCustom: Boolean
    get() = customLyricsFile != null ||
            customAudioFile != null ||
            customLrcFile != null

@Composable
fun GuitarSongCard(
    song: GuitarSong,
    onClick: () -> Unit,
    onEdit: (GuitarSong) -> Unit,
    onDelete: (GuitarSong) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ЛЕВАЯ ЧАСТЬ: Название + Длительность
            Column(modifier = Modifier.weight(1f)) {

                // Ряд для Названия + Значка E
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // fill = false, чтобы текст занимал только нужное место, и значок E был рядом
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Значок E (Только если explicit)
                    if (song.isExplicit) {
                        Spacer(modifier = Modifier.width(6.dp))
                        ExplicitBadgeSmall()
                    }
                }

                Text(
                    text = song.duration,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // ПРАВАЯ ЧАСТЬ: Кнопки (только для кастомных песен)
            if (song.isCustom) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onEdit(song) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(song) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // НИЖНЯЯ ЧАСТЬ: Сложность
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сложность", fontSize = 14.sp)
            GuitarStarRating(stars = song.difficulty)
        }
    }
}

// 🔥 Красивый и компактный значок Explicit
@Composable
fun ExplicitBadgeSmall() {
    Box(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "E",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// === ЗВЁЗДЫ ===
@Composable
fun GuitarStarRating(stars: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            val color = if (index < stars)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.outline
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
