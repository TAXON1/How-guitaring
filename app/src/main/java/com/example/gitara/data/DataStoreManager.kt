package com.example.gitara.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    private val dataStore = context.dataStore

    // Конфигурация JSON
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true // Чтобы файл бэкапа был читаемым
    }

    companion object {
        private val IS_DARK_THEME_KEY = booleanPreferencesKey("is_dark_theme")
        private val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        // 🔥 Новый ключ для вибрации
        private val METRONOME_VIBRATION_KEY = booleanPreferencesKey("metronome_vibration")

        private val EQUALIZER_VALUES_KEY = stringPreferencesKey("equalizer_values")
        private val EQUALIZER_ENABLED_KEY = booleanPreferencesKey("equalizer_enabled")
        private val CUSTOM_SONGS_KEY = stringPreferencesKey("custom_songs")

        private fun getExplicitKey(songId: String) = booleanPreferencesKey("explicit_$songId")
    }

    // === ОБЩИЕ НАСТРОЙКИ ===
    val isDarkTheme: Flow<Boolean> = dataStore.data.map { it[IS_DARK_THEME_KEY] ?: true }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[IS_DARK_THEME_KEY] = enabled }
    }

    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[KEEP_SCREEN_ON_KEY] ?: false }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_ON_KEY] = enabled }
    }

    // === МЕТРОНОМ ===
    // 🔥 Чтение настройки вибрации
    val metronomeVibration: Flow<Boolean> = dataStore.data.map { it[METRONOME_VIBRATION_KEY] ?: false }

    // 🔥 Сохранение настройки вибрации
    suspend fun setMetronomeVibration(enabled: Boolean) {
        dataStore.edit { it[METRONOME_VIBRATION_KEY] = enabled }
    }

    // === ЭКВАЛАЙЗЕР ===
    val equalizerValues: Flow<List<Float>> = dataStore.data.map { prefs ->
        prefs[EQUALIZER_VALUES_KEY]?.let { json ->
            runCatching { jsonConfig.decodeFromString<List<Float>>(json) }.getOrElse { List(5) { 50f } }
        } ?: List(5) { 50f }
    }

    suspend fun saveEqualizerValues(values: List<Float>) {
        dataStore.edit { it[EQUALIZER_VALUES_KEY] = jsonConfig.encodeToString(values) }
    }

    val equalizerEnabled: Flow<Boolean> = dataStore.data.map { it[EQUALIZER_ENABLED_KEY] ?: true }

    suspend fun saveEqualizerEnabled(enabled: Boolean) {
        dataStore.edit { it[EQUALIZER_ENABLED_KEY] = enabled }
    }

    // === КАСТОМНЫЕ ПЕСНИ ===
    fun getCustomSongsFlow(): Flow<List<GuitarSong>> = dataStore.data.map { prefs ->
        val json = prefs[CUSTOM_SONGS_KEY] ?: "[]"
        val songs = runCatching { jsonConfig.decodeFromString<List<GuitarSong>>(json) }.getOrElse { emptyList() }
        songs.map { it.copy(isExplicit = prefs[getExplicitKey(it.id)] ?: it.isExplicit) }
    }

    suspend fun addCustomSong(song: GuitarSong) {
        val currentJson = dataStore.data.first()[CUSTOM_SONGS_KEY] ?: "[]"
        val list = runCatching { jsonConfig.decodeFromString<MutableList<GuitarSong>>(currentJson) }.getOrElse { mutableListOf() }
        list.add(song)
        dataStore.edit {
            it[CUSTOM_SONGS_KEY] = jsonConfig.encodeToString(list)
            it[getExplicitKey(song.id)] = song.isExplicit
        }
    }

    suspend fun deleteCustomSong(songId: String) {
        val currentJson = dataStore.data.first()[CUSTOM_SONGS_KEY] ?: "[]"
        val list = runCatching { jsonConfig.decodeFromString<List<GuitarSong>>(currentJson) }.getOrElse { emptyList() }
        val newList = list.filter { it.id != songId }
        dataStore.edit {
            it[CUSTOM_SONGS_KEY] = jsonConfig.encodeToString(newList)
            it.remove(getExplicitKey(songId))
        }
    }

    suspend fun updateCustomSong(updatedSong: GuitarSong) {
        val currentJson = dataStore.data.first()[CUSTOM_SONGS_KEY] ?: "[]"
        val list = runCatching { jsonConfig.decodeFromString<List<GuitarSong>>(currentJson) }.getOrElse { emptyList() }
        val newList = list.map { if (it.id == updatedSong.id) updatedSong else it }
        dataStore.edit {
            it[CUSTOM_SONGS_KEY] = jsonConfig.encodeToString(newList)
            it[getExplicitKey(updatedSong.id)] = updatedSong.isExplicit
        }
    }

    // 🔥 Метод для восстановления песен из бэкапа (объединяет существующие с новыми)
    suspend fun restoreCustomSongs(importedSongs: List<GuitarSong>) {
        val currentJson = dataStore.data.first()[CUSTOM_SONGS_KEY] ?: "[]"
        val currentList = runCatching { jsonConfig.decodeFromString<MutableList<GuitarSong>>(currentJson) }.getOrElse { mutableListOf() }

        // Добавляем только те, которых нет (проверка по ID)
        val newSongs = importedSongs.filter { imported -> currentList.none { it.id == imported.id } }
        currentList.addAll(newSongs)

        dataStore.edit { prefs ->
            prefs[CUSTOM_SONGS_KEY] = jsonConfig.encodeToString(currentList)
            // Восстанавливаем флаги explicit для новых песен
            newSongs.forEach { song ->
                prefs[getExplicitKey(song.id)] = song.isExplicit
            }
        }
    }

    suspend fun getCustomSongById(songId: String): GuitarSong? {
        val songs = getCustomSongsFlow().first()
        return songs.find { it.id == songId }
    }
}
