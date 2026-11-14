package com.example.gitara.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitara.data.GuitarSong
import com.example.gitara.data.sampleSongs
import com.example.gitara.ui.components.GuitarSongCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuitarSongsScreen(
    customSongs: List<GuitarSong>,
    onSongClick: (GuitarSong) -> Unit,
    onAddSongClick: () -> Unit,
    onEditSong: (GuitarSong) -> Unit,
    onDeleteSong: (GuitarSong) -> Unit,
    listState: LazyListState
) {
    var search by rememberSaveable { mutableStateOf("") }

    // Объединяем встроенные и пользовательские песни
    val allSongs = remember(customSongs) { sampleSongs + customSongs }

    // Фильтрация
    val filteredSongs by remember(allSongs, search) {
        derivedStateOf {
            if (search.isBlank()) allSongs
            else allSongs.filter { it.title.contains(search, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        // Заголовок
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "How Guitaring?",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Поиск + Кнопка добавления
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuitarSearchBar(
                searchText = search,
                onSearchChange = { search = it },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilledIconButton(
                onClick = onAddSongClick,
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить песню",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Список песен
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = filteredSongs,
                key = { it.id },
                contentType = { "song" }
            ) { song ->
                // Здесь используется компонент из отдельного файла
                GuitarSongCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    onEdit = onEditSong,
                    onDelete = onDeleteSong,
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                )
            }
        }
    }
}
