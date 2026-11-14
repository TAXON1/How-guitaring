package com.example.gitara.data

import com.example.gitara.R
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class GuitarSong(
    val id: String = UUID.randomUUID().toString(),
    val audioResId: Int = 0,
    val title: String,
    val duration: String,
    val difficulty: Int,
    val lrcResId: Int = 0,

    // Пути к файлам (работают локально)
    val customAudioFile: String? = null,
    val customLrcFile: String? = null,
    val customLyricsFile: String? = null,

    val artist: String = "",
    val isExplicit: Boolean = false,

    // 🔥 ПОЛЯ ДЛЯ ЭКСПОРТА (хранят контент файлов внутри JSON)
    var exportedLyricsContent: String? = null,
    var exportedAudioBase64: String? = null // Для MP3 файлов
)

val sampleSongs = listOf(
    GuitarSong(
        id = "sample_perfect", // 🔥 ФИКСИРОВАННЫЙ ID
        title = "Perfect",
        artist = "Ed Sheeran",
        duration = "4:23",
        difficulty = 3,
        audioResId = R.raw.perfect,
        lrcResId = R.raw.perfect_text
    ),
    GuitarSong(
        id = "sample_videli_noch", // 🔥 ФИКСИРОВАННЫЙ ID
        title = "Видели ночь",
        artist = "Кино",
        duration = "3:08",
        difficulty = 3,
        audioResId = R.raw.videli_noch,
        lrcResId = R.raw.videli_noch_text
    ),
    GuitarSong(
        id = "sample_believer", // 🔥 ФИКСИРОВАННЫЙ ID
        title = "Believer",
        artist = "Imagine Dragons",
        duration = "3:23",
        difficulty = 4,
        audioResId = R.raw.believer,
        lrcResId = R.raw.believer_text
    ),
    GuitarSong(
        id = "sample_bohemian", // 🔥 ФИКСИРОВАННЫЙ ID
        title = "Bohemian Rhapsody",
        artist = "Queen",
        duration = "5:55",
        difficulty = 5,
        audioResId = R.raw.bohemian_rhapsody,
        lrcResId = R.raw.bohemian_rhapsody_text
    ),
    GuitarSong(
        id = "sample_cupsize",
        title = "Прыгай, дура!",
        artist = "CUPSIZE",
        duration = "1:58", // из твоего MP3 файла
        difficulty = 4,
        audioResId = R.raw.cupsize_prygai_dura, // добавь этот файл в res/raw/
        lrcResId = R.raw.cupsizeprygaidura_lrc, // добавь этот LRC в res/raw/
        isExplicit = true
)
)
