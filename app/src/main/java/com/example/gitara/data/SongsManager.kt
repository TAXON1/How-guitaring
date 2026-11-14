package com.example.gitara.data

import android.content.Context
import android.net.Uri
import com.example.gitara.ui.screens.LineType
import com.example.gitara.ui.screens.TimedLine
import com.example.gitara.ui.screens.isChordLine
import com.example.gitara.ui.screens.isSectionHeader
import java.io.File
import java.util.UUID
import android.media.MediaMetadataRetriever
import com.example.gitara.data.SongsManager.deleteSongFiles

// 🔥 ПЕРЕМЕСТИЛИ СЮДА из ui.screens
private val lrcRegex = Regex("""\[\d{2}:\d{2}\.\d{2,3}]""")

fun containsLrcTimings(text: String): Boolean {
    return lrcRegex.containsMatchIn(text)
}

object SongsManager {
    private const val AUDIO_DIR = "custom_audio"
    private const val LRC_DIR = "custom_lrc"
    private const val LYRICS_DIR = "custom_lyrics"

    private fun getAppFilesDir(context: Context, subDir: String): File {
        return File(context.filesDir, subDir).also { it.mkdirs() }
    }

    // 🔥 ФУНКЦИЯ ДЛЯ ДЛИТЕЛЬНОСТИ
    fun getAudioDuration(context: Context, audioFileName: String?): String {
        if (audioFileName == null) return "?:??"

        return try {
            val retriever = MediaMetadataRetriever()
            val file = File(getAppFilesDir(context, AUDIO_DIR), audioFileName)
            retriever.setDataSource(file.absolutePath)

            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L
            retriever.release()

            formatDuration(durationMs)
        } catch (e: Exception) {
            "?:??"
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val minutes = (durationMs / 1000) / 60
        val seconds = (durationMs / 1000) % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    fun copyAudioToApp(context: Context, uri: Uri): String? {
        return try {
            val fileName = "custom_${UUID.randomUUID()}.mp3"
            val destFile = File(getAppFilesDir(context, AUDIO_DIR), fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveLyricsFile(context: Context, content: String): String {
        val fileName = "lyrics_${UUID.randomUUID()}.txt"
        val file = File(getAppFilesDir(context, LYRICS_DIR), fileName)
        file.writeText(content)
        return fileName
    }

    private fun saveLrcFile(context: Context, content: String): String {
        val fileName = "lrc_${UUID.randomUUID()}.lrc"
        val file = File(getAppFilesDir(context, LRC_DIR), fileName)
        file.writeText(content)
        return fileName
    }
    suspend fun addCustomSong(
        context: Context,
        title: String,
        difficulty: Int,
        lyricsWithChords: String,
        audioFileName: String?,
        lrcText: String?,
        isExplicit: Boolean // 🔥 ДОБАВЛЕН ПАРАМЕТР
    ): GuitarSong {
        val dataStoreManager = DataStoreManager(context)
        val hasLrc = lrcText != null
        val lyricsFileName = if (!hasLrc) saveLyricsFile(context, lyricsWithChords) else null
        val lrcFileName = if (hasLrc) saveLrcFile(context, lrcText!!) else null
        val duration = getAudioDuration(context, audioFileName)

        val customSong = GuitarSong(
            audioResId = 0,
            title = title.trim(),
            duration = duration,
            difficulty = difficulty,
            lrcResId = 0,
            customAudioFile = audioFileName,
            customLrcFile = lrcFileName,
            customLyricsFile = lyricsFileName,
            artist = "",
            isExplicit = isExplicit // 🔥 Передаем в объект
        )

        dataStoreManager.addCustomSong(customSong)
        return customSong
    }

    suspend fun updateCustomSong(
        context: Context,
        originalSong: GuitarSong,
        title: String,
        difficulty: Int,
        lyricsText: String,
        audioFileName: String?,
        removeAudio: Boolean = false,
        isExplicit: Boolean // 🔥 ДОБАВЛЕН ПАРАМЕТР
    ) {
        val dataStoreManager = DataStoreManager(context)
        val hasLrc = containsLrcTimings(lyricsText)

        // Обновляем файлы lyrics/lrc
        val newLyricsFile = if (!hasLrc) {
            if (lyricsText != SongsManager.loadCustomLyrics(context, originalSong.customLyricsFile)) {
                saveLyricsFile(context, lyricsText)
            } else originalSong.customLyricsFile
        } else originalSong.customLyricsFile

        val newLrcFile = if (hasLrc) {
            if (lyricsText != SongsManager.loadCustomLrc(context, originalSong.customLrcFile).joinToString { it.text }) {
                saveLrcFile(context, lyricsText)
            } else originalSong.customLrcFile
        } else originalSong.customLrcFile

        // Обновляем аудио
        val finalAudioFile = when {
            removeAudio -> {
                originalSong.customAudioFile?.let { old ->
                    File(getAppFilesDir(context, AUDIO_DIR), old).delete()
                    null
                }
            }
            audioFileName != null && audioFileName != originalSong.customAudioFile -> audioFileName
            else -> originalSong.customAudioFile
        }

        val duration = getAudioDuration(context, finalAudioFile)
        val updatedSong = originalSong.copy(
            title = title.trim(),
            difficulty = difficulty,
            duration = duration,
            customAudioFile = finalAudioFile,
            customLyricsFile = newLyricsFile,
            customLrcFile = newLrcFile,
            isExplicit = isExplicit // 🔥 Передаем обновленный флаг
        )

        dataStoreManager.updateCustomSong(updatedSong)
    }
    suspend fun updateCustomSong(
        context: Context,
        originalSong: GuitarSong,
        title: String,
        difficulty: Int,
        lyricsText: String,
        audioFileName: String?,      // как было
        removeAudio: Boolean = false // ← НОВЫЙ флаг
    ) {
        val hasLrc = containsLrcTimings(lyricsText)

        // 🔥 1. НОВЫЕ файлы ТОЛЬКО при изменении
        val newLyricsFile = if (!hasLrc) {
            if (lyricsText != SongsManager.loadCustomLyrics(context, originalSong.customLyricsFile ?: ""))
                saveLyricsFile(context, lyricsText)  // Изменили текст
            else originalSong.customLyricsFile    // Текст тот же
        } else originalSong.customLyricsFile

        val newLrcFile = if (hasLrc) {
            if (lyricsText != SongsManager.loadCustomLrc(context, originalSong.customLrcFile ?: "").joinToString("\n") { it.text })
                saveLrcFile(context, lyricsText)
            else originalSong.customLrcFile
        } else originalSong.customLrcFile

        // 🔥 2. АУДИО: новый файл ИЛИ старый
        val finalAudioFile = when {
            removeAudio -> {
                // удаляем старый файл и не привязываем ничего
                originalSong.customAudioFile?.let { old ->
                    File(getAppFilesDir(context, AUDIO_DIR), old).delete()
                }
                null
            }
            audioFileName != null && audioFileName != originalSong.customAudioFile -> {
                // новый файл
                audioFileName
            }
            originalSong.customAudioFile != null -> {
                // старый оставляем
                originalSong.customAudioFile
            }
            else -> null
        }

        val duration = getAudioDuration(context, finalAudioFile)

        val updatedSong = originalSong.copy(
            title = title.trim(),
            difficulty = difficulty,
            duration = duration,
            customAudioFile = finalAudioFile,
            customLyricsFile = newLyricsFile,
            customLrcFile = newLrcFile
        )

        DataStoreManager(context).updateCustomSong(updatedSong)
    }



    fun getAudioUri(context: Context, fileName: String?): Uri? {
        if (fileName == null) return null
        return Uri.fromFile(File(getAppFilesDir(context, AUDIO_DIR), fileName))
    }

    fun loadCustomLyrics(context: Context, fileName: String?): String {
        if (fileName.isNullOrBlank()) return ""

        // 🔥 ПРОВЕРКА: это не папка?
        val file = File(getAppFilesDir(context, LYRICS_DIR), fileName)
        if (!file.isFile) {
            println("❌ NOT A FILE: $fileName → ${file.absolutePath}")
            return ""
        }

        return try {
            file.readText()
        } catch (e: Exception) {
            println("❌ Read failed: ${e.message}")
            ""
        }
    }


    fun loadCustomLrc(context: Context, fileName: String?): List<TimedLine> {
        if (fileName.isNullOrBlank()) return emptyList()

        val file = File(getAppFilesDir(context, LRC_DIR), fileName)
        if (!file.isFile) {
            println("❌ LRC NOT A FILE: $fileName")
            return emptyList()
        }

        return try {
            parseLrcText(file.readText())
        } catch (e: Exception) {
            println("❌ LRC read failed: ${e.message}")
            emptyList()
        }
    }


    private fun parseLrcText(lrcContent: String): List<TimedLine> {
        val lines = mutableListOf<TimedLine>()
        val timestampRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        lrcContent.lines().forEach { raw ->
            val match = timestampRegex.find(raw) ?: return@forEach
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val millisRaw = match.groupValues[3].toLong()
            val millis = if (match.groupValues[3].length == 2) millisRaw * 10 else millisRaw
            val timestamp = minutes * 60_000 + seconds * 1_000 + millis
            val content = match.groupValues[4].replace("\t", " ")
            if (content.isBlank()) return@forEach
            val type = when {
                isSectionHeader(content.trim()) -> LineType.SECTION
                isChordLine(content) -> LineType.CHORDS
                else -> LineType.LYRICS
            }
            lines.add(TimedLine(content, type, timestamp))
        }
        return lines
    }

    // 🔥 УДАЛЕНИЕ ФАЙЛОВ ПЕСНИ — ВНУТРИ object SongsManager!
    fun deleteSongFiles(context: Context, song: GuitarSong) {
        // Удаляем аудио
        song.customAudioFile?.let { fileName ->
            File(getAppFilesDir(context, AUDIO_DIR), fileName).delete()
        }
        // Удаляем LRC
        song.customLrcFile?.let { fileName ->
            File(getAppFilesDir(context, LRC_DIR), fileName).delete()
        }
        // Удаляем lyrics
        song.customLyricsFile?.let { fileName ->
            File(getAppFilesDir(context, LYRICS_DIR), fileName).delete()
        }
    }
}

