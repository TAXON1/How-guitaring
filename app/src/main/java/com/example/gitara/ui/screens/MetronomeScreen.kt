package com.example.gitara.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gitara.data.DataStoreManager
import com.example.gitara.metronome.MetronomeManager
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Читаем настройку вибрации
    val dataStoreManager = remember { DataStoreManager(context) }
    val vibrationEnabled by dataStoreManager.metronomeVibration.collectAsState(initial = false)

    var bpm by rememberSaveable { mutableFloatStateOf(100f) }
    var isRunning by remember { mutableStateOf(false) }
    var beatsPerBar by rememberSaveable { mutableIntStateOf(4) }
    var currentBeat by remember { mutableIntStateOf(0) }
    var showBpmDialog by remember { mutableStateOf(false) }

    val metronome = remember {
        MetronomeManager.create(context) { beat -> currentBeat = beat }
    }

    // Автоматическая остановка метронома при уходе приложения в фон или со экрана
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                isRunning = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            metronome.stop()
            MetronomeManager.stop()
        }
    }

    // Передаем настройку в метроном
    LaunchedEffect(vibrationEnabled) {
        metronome.isVibrationEnabled = vibrationEnabled
    }

    // Синхронизация UI с действиями
    LaunchedEffect(isRunning, bpm, beatsPerBar) {
        if (isRunning) {
            metronome.start(bpm.roundToInt(), beatsPerBar)
        } else {
            metronome.stop()
            currentBeat = 0
        }
    }

    if (showBpmDialog) {
        BpmInputDialog(
            bpm = bpm.roundToInt(),
            onDismiss = { showBpmDialog = false },
            onConfirm = { newBpm ->
                bpm = newBpm.toFloat()
                showBpmDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("МЕТРОНОМ", letterSpacing = 2.sp, fontWeight = FontWeight.Bold) },
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // 1. Визуализатор
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(20.dp))
            ) {
                for (i in 0 until beatsPerBar) {
                    val isActive = (i == currentBeat) && isRunning
                    val activeColor = if (i == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    BeatIndicator(isActive = isActive, activeColor = activeColor)
                }
            }

            // 2. Дисплей BPM
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${bpm.roundToInt()}",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-2).sp,
                    modifier = Modifier.clickable { showBpmDialog = true }
                )
                Text(
                    text = "уд/мин",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3. Слайдер
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Slider(
                    value = bpm,
                    onValueChange = { bpm = it },
                    valueRange = 40f..240f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("MIN 40", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                    Text("MAX 240", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                }
            }

            // 4. Выбор размера
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Размер такта:", color = MaterialTheme.colorScheme.outline)

                FilterChip(
                    selected = false,
                    onClick = { beatsPerBar = if (beatsPerBar >= 8) 2 else beatsPerBar + 1 },
                    label = {
                        Text("$beatsPerBar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    enabled = true,
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.secondary,
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Кнопка СТАРТ / СТОП
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .shadow(
                        elevation = if (isRunning) 4.dp else 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = MaterialTheme.colorScheme.primary,
                        spotColor = MaterialTheme.colorScheme.primary
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isRunning) MaterialTheme.colorScheme.error else Color.White
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (isRunning) "СТОП" else "СТАРТ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) MaterialTheme.colorScheme.error else Color.White
                )
            }
        }
    }
}

@Composable
private fun BpmInputDialog(bpm: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var textFieldValue by remember { mutableStateOf(bpm.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(280.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Темп (BPM)", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) textFieldValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Диапазон: 40 — 240", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Отмена") }
                    Button(onClick = {
                        keyboardController?.hide()
                        onConfirm(textFieldValue.toIntOrNull()?.coerceIn(40, 240) ?: bpm)
                    }, modifier = Modifier.weight(1f)) { Text("ОК") }
                }
            }
        }
    }
}

@Composable
fun BeatIndicator(isActive: Boolean, activeColor: Color) {
    val size by animateDpAsState(targetValue = if (isActive) 24.dp else 12.dp, animationSpec = tween(100))
    val color by animateColorAsState(targetValue = if (isActive) activeColor else MaterialTheme.colorScheme.outline.copy(alpha=0.3f), animationSpec = tween(50))
    Box(modifier = Modifier.padding(horizontal = 4.dp).width(16.dp).height(40.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(size).shadow(if (isActive) 10.dp else 0.dp, CircleShape, spotColor = activeColor).background(color, CircleShape))
    }
}
