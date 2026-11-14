package com.example.gitara.ui.screens

import android.app.Activity
import android.net.Uri
import android.util.Base64
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitara.data.DataStoreManager
import com.example.gitara.data.GuitarSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()

    // === ДАННЫЕ ИЗ DATASTORE ===
    val isDarkTheme by dataStoreManager.isDarkTheme.collectAsState(initial = true)
    val keepScreenOn by dataStoreManager.keepScreenOn.collectAsState(initial = false)
    val vibrationEnabled by dataStoreManager.metronomeVibration.collectAsState(initial = false)

    // Получаем список песен для Экспорта
    val customSongs by dataStoreManager.getCustomSongsFlow().collectAsState(initial = emptyList())

    // === ЭКСПОРТ ===
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val songsToExport = customSongs.map { song ->
                        val lyricsContent = if (song.customLyricsFile != null) {
                            runCatching { File(context.filesDir, "custom_lyrics/${song.customLyricsFile}").readText() }.getOrNull()
                        } else if (song.customLrcFile != null) {
                            runCatching { File(context.filesDir, "custom_lrc/${song.customLrcFile}").readText() }.getOrNull()
                        } else null

                        val audioContent = if (song.customAudioFile != null) {
                            runCatching {
                                val file = File(context.filesDir, "custom_audio/${song.customAudioFile}")
                                if (file.exists()) Base64.encodeToString(file.readBytes(), Base64.DEFAULT) else null
                            }.getOrNull()
                        } else null

                        song.copy(exportedLyricsContent = lyricsContent, exportedAudioBase64 = audioContent)
                    }

                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val jsonString = Json { prettyPrint = true }.encodeToString(songsToExport)
                        outputStream.write(jsonString.toByteArray())
                    }
                    launch(Dispatchers.Main) { Toast.makeText(context, "Резервная копия создана успешно.", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) { Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    // === ИМПОРТ ===
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val jsonString = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() } ?: ""
                    val importedListRaw = Json { ignoreUnknownKeys = true }.decodeFromString<List<GuitarSong>>(jsonString)

                    val restoredSongs = importedListRaw.map { song ->
                        var updatedSong = song
                        if (song.exportedLyricsContent != null) {
                            val isLrc = song.customLrcFile != null
                            val dirName = if (isLrc) "custom_lrc" else "custom_lyrics"
                            val ext = if (isLrc) ".lrc" else ".txt"
                            val fileName = "imported_${UUID.randomUUID()}$ext"
                            File(context.filesDir, dirName).apply { mkdirs() }.resolve(fileName).writeText(song.exportedLyricsContent!!)
                            updatedSong = if (isLrc) updatedSong.copy(customLrcFile = fileName, customLyricsFile = null)
                            else updatedSong.copy(customLyricsFile = fileName, customLrcFile = null)
                        }
                        if (song.exportedAudioBase64 != null) {
                            val fileName = "imported_audio_${UUID.randomUUID()}.mp3"
                            File(context.filesDir, "custom_audio").apply { mkdirs() }.resolve(fileName).writeBytes(Base64.decode(song.exportedAudioBase64, Base64.DEFAULT))
                            updatedSong = updatedSong.copy(customAudioFile = fileName)
                        }
                        updatedSong.copy(exportedLyricsContent = null, exportedAudioBase64 = null)
                    }
                    dataStoreManager.restoreCustomSongs(restoredSongs)
                    launch(Dispatchers.Main) { Toast.makeText(context, "Восстановлено ${restoredSongs.size} композиций.", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) { Toast.makeText(context, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    // Локальные стейты для диалогов
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) } // 🔥 НОВЫЙ ДИАЛОГ
    var showLicensesDialog by remember { mutableStateOf(false) }
    var equalizerSheetOpen by remember { mutableStateOf(false) }
    var versionClickCount by remember { mutableIntStateOf(0) }

    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Настройки",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // === ВНЕШНИЙ ВИД ===
        SettingsSection(title = "ВНЕШНИЙ ВИД") {
            SettingsSwitchItem(
                title = "Тёмная тема",
                subtitle = "Переключает интерфейс в ночной режим",
                checked = isDarkTheme,
                onCheckedChange = { scope.launch { dataStoreManager.setDarkTheme(it) } }
            )
        }

        // === ИНСТРУМЕНТЫ ===
        SettingsSection(title = "ИНСТРУМЕНТЫ") {
            SettingsNavigationItem(
                title = "Эквалайзер",
                subtitle = "Настройка частот воспроизведения",
                onClick = { equalizerSheetOpen = true }
            )
            Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

            SettingsSwitchItem(
                title = "Не выключать экран",
                subtitle = "Предотвращает блокировку устройства",
                checked = keepScreenOn,
                onCheckedChange = { scope.launch { dataStoreManager.setKeepScreenOn(it) } }
            )
            Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

            SettingsSwitchItem(
                title = "Вибрация метронома",
                subtitle = "Тактильный отклик при каждом ударе",
                checked = vibrationEnabled,
                onCheckedChange = { scope.launch { dataStoreManager.setMetronomeVibration(it) } }
            )
        }

        // === ДАННЫЕ ===
        SettingsSection(title = "ДАННЫЕ") {
            SettingsNavigationItem(
                icon = Icons.Default.Upload,
                title = "Экспорт резервной копии",
                subtitle = "Сохранить библиотеку в JSON файл",
                onClick = {
                    if (customSongs.isNotEmpty()) exportLauncher.launch("guitarchordify_backup.json")
                    else Toast.makeText(context, "Библиотека пуста", Toast.LENGTH_SHORT).show()
                }
            )
            Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

            SettingsNavigationItem(
                icon = Icons.Default.Download,
                title = "Импорт резервной копии",
                subtitle = "Восстановить библиотеку из файла",
                onClick = { importLauncher.launch(arrayOf("application/json")) }
            )
        }

        // === О ПРИЛОЖЕНИИ ===
        SettingsSection(title = "О ПРИЛОЖЕНИИ") {
            SettingsNavigationItem(
                icon = Icons.Default.Info,
                title = "Версия",
                subtitle = "v1.0 (release)",
                onClick = {
                    versionClickCount++
                    if (versionClickCount == 27) {
                        Toast.makeText(context, "Максим жирный", Toast.LENGTH_SHORT).show()
                        versionClickCount = 0
                    }
                }
            )
            Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

            SettingsNavigationItem(
                icon = Icons.Default.Gavel,
                title = "Условия использования",
                subtitle = "Terms of Use",
                onClick = { showTermsDialog = true }
            )
            Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

            SettingsNavigationItem(
                icon = Icons.Default.Description,
                title = "Политика конфиденциальности",
                subtitle = "Privacy Policy",
                onClick = { showPrivacyDialog = true }
            )
            Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

            SettingsNavigationItem(
                icon = Icons.Default.Code,
                title = "Лицензии Open Source",
                subtitle = "Библиотеки Android Jetpack",
                onClick = { showLicensesDialog = true }
            )
        }

        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                "How guitaring? for Android\n© 2026 How guitaring? Team. All Rights Reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // === ДИАЛОГИ ===
    if (equalizerSheetOpen) EqualizerBottomSheet(onDismiss = { equalizerSheetOpen = false })
    if (showPrivacyDialog) PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    if (showTermsDialog) TermsOfUseDialog(onDismiss = { showTermsDialog = false })
    if (showLicensesDialog) LicensesDialog(onDismiss = { showLicensesDialog = false })
}

// 🔥 ОГРОМНАЯ ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ
@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Политика конфиденциальности") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    text = """
                        ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ ПРИЛОЖЕНИЯ HOW GUITARING?

                        1. ОБЩИЕ ПОЛОЖЕНИЯ
                        Настоящая Политика конфиденциальности описывает принципы обработки данных при использовании мобильного приложения How Guitaring? ("Приложение"). Мы с уважением относимся к правам пользователей и гарантируем максимальную прозрачность в вопросах хранения информации.
                        
                        2. СБОР И ХРАНЕНИЕ ДАННЫХ
                        2.1. Приложение работает по принципу "Local First" (Локальное хранение). Все созданные вами данные (тексты песен, аккорды, аудиозаписи, настройки метронома, пресеты эквалайзера) хранятся исключительно во внутренней памяти вашего мобильного устройства.
                        2.2. Мы НЕ собираем, НЕ храним, НЕ передаем и НЕ анализируем ваши личные данные, включая, но не ограничиваясь: ФИО, email, телефон, геолокацию, список контактов.
                        2.3. Приложение не имеет серверной части (Backend) для синхронизации пользовательского контента. Удаление Приложения с устройства приведет к безвозвратной потере всех созданных данных, если вы предварительно не создали резервную копию.

                        3. РАЗРЕШЕНИЯ ANDROID
                        Для корректной работы Приложение запрашивает следующие системные разрешения:
                        - READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE: Требуется исключительно для функций "Импорт" и "Экспорт" резервных копий базы данных (файлы .json), а также для загрузки пользовательских аудиофайлов (MP3) в библиотеку Приложения.
                        - VIBRATE: Используется для обеспечения тактильной обратной связи (Haptic Feedback) при работе с функцией "Метроном".
                        - FOREGROUND_SERVICE: Может использоваться для воспроизведения аудио в фоновом режиме, когда экран устройства выключен.

                        4. АНАЛИТИКА И ТРЕТЬИ ЛИЦА
                        4.1. Приложение не использует сторонние трекеры (Google Analytics, Firebase Crashlytics, Facebook Pixel) для отслеживания поведения пользователя.
                        4.2. Приложение построено с использованием открытых библиотек (Open Source), включая Android Jetpack и Kotlin Coroutines, которые могут собирать обезличенные технические данные о сбоях (Stack Traces) на уровне операционной системы, отправляемые в Google Play Console только с вашего явного согласия через настройки Android.

                        5. БЕЗОПАСНОСТЬ
                        Поскольку данные хранятся локально, ответственность за их сохранность от физического доступа третьих лиц несет владелец устройства. Мы рекомендуем использовать системные средства защиты (пароль, биометрия) для блокировки устройства.

                        6. ИЗМЕНЕНИЯ В ПОЛИТИКЕ
                        Разработчик оставляет за собой право вносить изменения в настоящую Политику. Обновленная версия будет доступна в меню "Настройки" Приложения.

                        Контакты разработчика: Не пишите нам
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Принимаю") } }
    )
}

// 🔥 ОГРОМНЫЕ УСЛОВИЯ ИСПОЛЬЗОВАНИЯ
@Composable
fun TermsOfUseDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Условия использования") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    text = """
                        УСЛОВИЯ ИСПОЛЬЗОВАНИЯ (TERMS OF USE)
                        
                        ВНИМАТЕЛЬНО ПРОЧИТАЙТЕ ПЕРЕД ИСПОЛЬЗОВАНИЕМ ПРИЛОЖЕНИЯ HOW GUITARING?

                        1. ПРЕДМЕТ СОГЛАШЕНИЯ
                        Устанавливая, копируя или иным образом используя приложение How Guitaring? ("ПО"), вы подтверждаете свое полное и безоговорочное согласие с данными условиями. Если вы не согласны с каким-либо пунктом, вы обязаны немедленно удалить ПО.

                        2. ЛИЦЕНЗИЯ
                        Разработчик предоставляет вам ограниченную, неисключительную, непередаваемую лицензию на использование ПО в личных некоммерческих целях. Запрещается декомпилировать, модифицировать, взламывать или создавать производные продукты на основе данного ПО.

                        3. ПОЛЬЗОВАТЕЛЬСКИЙ КОНТЕНТ И АВТОРСКИЕ ПРАВА
                        3.1. Приложение является инструментом для организации музыкального материала. Пользователь несет единоличную ответственность за любой контент (тексты песен, аккорды, аудиофайлы), добавляемый в Приложение.
                        3.2. Пользователь гарантирует, что использование им материалов, защищенных авторским правом (например, текстов известных песен), осуществляется в рамках законодательства (например, для личного ознакомления или обучения) и не нарушает прав третьих лиц.
                        3.3. Разработчик не осуществляет премодерацию контента и не несет ответственности за наличие в базе данных пользователя материалов, нарушающих чьи-либо права.

                        4. ОТКАЗ ОТ ГАРАНТИЙ (AS IS)
                        ПРИЛОЖЕНИЕ ПРЕДОСТАВЛЯЕТСЯ "КАК ЕСТЬ", БЕЗ КАКИХ-ЛИБО ЯВНЫХ ИЛИ ПОДРАЗУМЕВАЕМЫХ ГАРАНТИЙ. РАЗРАБОТЧИК НЕ ГАРАНТИРУЕТ, ЧТО:
                        - ФУНКЦИОНАЛ ПРИЛОЖЕНИЯ БУДЕТ ОТВЕЧАТЬ ВАШИМ ОЖИДАНИЯМ;
                        - РАБОТА ПРИЛОЖЕНИЯ БУДЕТ БЕСПЕРЕБОЙНОЙ ИЛИ БЕЗОШИБОЧНОЙ;
                        - ТОЧНОСТЬ РАБОТЫ МЕТРОНОМА ИЛИ ЭКВАЛАЙЗЕРА БУДЕТ ДОСТАТОЧНОЙ ДЛЯ ПРОФЕССИОНАЛЬНОЙ СТУДИЙНОЙ ЗАПИСИ.

                        5. ОГРАНИЧЕНИЕ ОТВЕТСТВЕННОСТИ
                        НИ ПРИ КАКИХ ОБСТОЯТЕЛЬСТВАХ РАЗРАБОТЧИК НЕ НЕСЕТ ОТВЕТСТВЕННОСТИ ЗА ЛЮБОЙ ПРЯМОЙ ИЛИ КОСВЕННЫЙ УЩЕРБ, ВКЛЮЧАЯ:
                        - ПОТЕРЮ ДАННЫХ ИЛИ СБОЙ УСТРОЙСТВА;
                        - СРЫВ ПУБЛИЧНЫХ ВЫСТУПЛЕНИЙ ИЗ-ЗА ТЕХНИЧЕСКИХ НЕПОЛАДОК;
                        - ПОВРЕЖДЕНИЕ МУЗЫКАЛЬНОГО ОБОРУДОВАНИЯ ВСЛЕДСТВИЕ ИСПОЛЬЗОВАНИЯ ЭКВАЛАЙЗЕРА С ЭКСТРЕМАЛЬНЫМИ НАСТРОЙКАМИ.

                        6. БЕЗОПАСНОСТЬ ИСПОЛЬЗОВАНИЯ
                        Пользователь обязуется не использовать Приложение в ситуациях, когда отвлечение внимания может создать угрозу жизни или здоровью (например, при управлении транспортным средством).

                        7. ЗАКЛЮЧИТЕЛЬНЫЕ ПОЛОЖЕНИЯ
                        Данное соглашение регулируется законодательством страны пребывания пользователя. Разработчик оставляет за собой право прекратить поддержку ПО в любой момент без предварительного уведомления.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Согласен") } }
    )
}

// ... ОСТАВЬ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ БЕЗ ИЗМЕНЕНИЙ (SettingsSection, SwitchItem, etc) ...
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))) { content() }
    }
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
fun SettingsNavigationItem(icon: ImageVector? = null, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(16.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) { Spacer(modifier = Modifier.height(2.dp)); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
@Composable
fun LicensesDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Лицензии Open Source") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Нажмите на библиотеку, чтобы открыть её исходный код:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // === KOTLIN & SERIALIZATION ===
                LicenseItem("Kotlin Standard Library", "JetBrains", "Apache 2.0", "https://kotlinlang.org/") { uriHandler.openUri(it) }
                LicenseItem("Kotlin Coroutines", "JetBrains", "Apache 2.0", "https://github.com/Kotlin/kotlinx.coroutines") { uriHandler.openUri(it) }
                LicenseItem("Kotlinx Serialization", "JetBrains", "Apache 2.0", "https://github.com/Kotlin/kotlinx.serialization") { uriHandler.openUri(it) }

                // === UI: COMPOSE ===
                LicenseItem("Jetpack Compose UI", "Google LLC", "Apache 2.0", "https://developer.android.com/jetpack/compose") { uriHandler.openUri(it) }
                LicenseItem("Compose Material 3", "Google LLC", "Apache 2.0", "https://m3.material.io/develop/android/jetpack-compose") { uriHandler.openUri(it) }
                LicenseItem("Compose UI Graphics", "Google LLC", "Apache 2.0", "https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/package-summary") { uriHandler.openUri(it) }
                LicenseItem("Compose Tooling Preview", "Google LLC", "Apache 2.0", "https://developer.android.com/jetpack/compose/tooling") { uriHandler.openUri(it) }

                // === ARCHITECTURE ===
                LicenseItem("AndroidX Activity Compose", "Google LLC", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/activity") { uriHandler.openUri(it) }
                LicenseItem("AndroidX Navigation Compose", "Google LLC", "Apache 2.0", "https://developer.android.com/guide/navigation") { uriHandler.openUri(it) }
                LicenseItem("AndroidX Lifecycle Runtime", "Google LLC", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/lifecycle") { uriHandler.openUri(it) }
                LicenseItem("AndroidX DataStore Prefs", "Google LLC", "Apache 2.0", "https://developer.android.com/topic/libraries/architecture/datastore") { uriHandler.openUri(it) }
                LicenseItem("AndroidX Core KTX", "Google LLC", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/core") { uriHandler.openUri(it) }

                // === RESOURCES ===
                LicenseItem("Material Icons Extended", "Google LLC", "Apache 2.0", "https://fonts.google.com/icons") { uriHandler.openUri(it) }

                // === LEGACY / UTILS ===
                LicenseItem("Google Gson", "Google LLC", "Apache 2.0", "https://github.com/google/gson") { uriHandler.openUri(it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
fun LicenseItem(
    library: String,
    author: String,
    license: String,
    url: String,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(url) } // Делаем всю строку кликабельной
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = library,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary // Цвет ссылки
            )
            Row {
                Text(author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(license, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        // Иконка "Открыть ссылку"
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline
        )
    }
    Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
}