package com.example.gitara.ui.screens

import android.content.Context

// === ТИПЫ ДАННЫХ ===
enum class LineType { TITLE, SECTION, CHORDS, LYRICS, EMPTY }

data class TimedLine(val text: String, val type: LineType, val timestamp: Long)

// === ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ===

fun isChordLine(line: String): Boolean {
    val trimmed = line.trim()
    // Лимит длины для защиты от длинных текстовых строк, которые могут случайно совпасть с паттерном
    if (trimmed.isBlank() || trimmed.length > 200) return false

    // Разбиваем по пробелам
    val parts = trimmed.split(Regex("\\s+"))

    // Паттерн для аккордов (ловит Am, G/B, Csus4, D7 и т.д.)
    val chordPattern = Regex("^([A-G][b#]?(m|min|maj|dim|aug|sus|add)?[0-9]?(sus)?[0-9]?(/[A-G][b#]?)?)$")

    val chordCount = parts.count { it.matches(chordPattern) }
    val wordCount = parts.size

    // Если хотя бы половина "слов" в строке похожа на аккорды - это строка аккордов
    return chordCount > 0 && chordCount.toDouble() / wordCount >= 0.5
}

fun isSectionHeader(line: String): Boolean {
    val upper = line.uppercase()
    return upper.startsWith("INTRO") ||
            upper.startsWith("VERSE") ||
            upper.startsWith("CHORUS") ||
            upper.startsWith("BRIDGE") ||
            upper.startsWith("PRE-CHORUS") ||
            upper.startsWith("PRECHORUS") ||
            upper.startsWith("SOLO") ||
            upper.startsWith("OUTRO") ||
            upper.startsWith("INSTRUMENTAL") ||
            upper.startsWith("ВСТУПЛЕНИЕ") ||
            upper.startsWith("КУПЛЕТ") ||
            upper.startsWith("ПРИПЕВ")
}

// === ПАРСИНГ LRC ===

fun parseLrcFile(context: Context, lrcResId: Int): List<TimedLine> {
    val lines = mutableListOf<TimedLine>()
    // Регулярное выражение для формата [mm:ss.xx]Текст
    val timestampRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

    context.resources.openRawResource(lrcResId).bufferedReader().useLines { fileLines ->
        fileLines.forEach { raw ->
            val match = timestampRegex.find(raw.trim()) ?: return@forEach

            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val millisRaw = match.groupValues[3].toLong()

            // Обработка двухзначных (0.10 = 100ms) или трехзначных (0.100 = 100ms) миллисекунд
            val millis = if (match.groupValues[3].length == 2) millisRaw * 10 else millisRaw
            val timestamp = minutes * 60_000 + seconds * 1_000 + millis

            // Захватываем текст после временной метки, заменяя табы на пробелы
            val content = match.groupValues[4].replace("\t", " ").trim()

            if (content.isNotBlank()) {
                val type = when {
                    isSectionHeader(content) -> LineType.SECTION
                    isChordLine(content) -> LineType.CHORDS
                    else -> LineType.LYRICS
                }
                lines.add(TimedLine(content, type, timestamp))
            }
        }
    }
    return lines
}
