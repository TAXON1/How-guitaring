package com.example.gitara.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.example.gitara.data.DataStoreManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()

    val eqEnabled by dataStore.equalizerEnabled.collectAsState(initial = true)
    val values by dataStore.equalizerValues.collectAsState(initial = listOf(50f, 50f, 50f, 50f, 50f))

    val presets = mapOf(
        "Flat" to listOf(50f, 50f, 50f, 50f, 50f),
        "Rock" to listOf(68f, 62f, 50f, 58f, 70f),
        "Bass" to listOf(80f, 70f, 55f, 45f, 40f),
        "Treble" to listOf(40f, 45f, 55f, 70f, 80f),
        "Jazz" to listOf(65f, 58f, 55f, 62f, 68f)
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // ЗАГОЛОВОК И ТОГГЛ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "EQUALIZER",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )

                Switch(
                    checked = eqEnabled,
                    onCheckedChange = {
                        scope.launch { dataStore.saveEqualizerEnabled(it) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ПРЕСЕТЫ (ЧИПЫ)
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (name, presetValues) ->
                    val isSelected = values == presetValues
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (eqEnabled) scope.launch { dataStore.saveEqualizerValues(presetValues) }
                        },
                        label = { Text(name) },
                        enabled = eqEnabled,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        // ИСПРАВЛЕНИЕ ЗДЕСЬ:
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if(isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                            enabled = eqEnabled,     // <--- Добавлено
                            selected = isSelected    // <--- Добавлено
                        )
                    )
                }
            }


            Spacer(modifier = Modifier.height(32.dp))

            // СЛАЙДЕРЫ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // dB Маркировка слева (необязательно, можно убрать для минимализма)
                Column(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("+15", fontSize=10.sp, color=MaterialTheme.colorScheme.outline)
                    Text("0", fontSize=10.sp, color=MaterialTheme.colorScheme.outline)
                    Text("-15", fontSize=10.sp, color=MaterialTheme.colorScheme.outline)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Сами ползунки
                listOf("60", "230", "910", "4k", "14k").forEachIndexed { i, label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        StudioVerticalSlider(
                            value = values.getOrElse(i) { 50f },
                            onValueChange = { newValue ->
                                val newList = values.toMutableList().apply {
                                    this[i] = newValue.coerceIn(0f, 100f)
                                }
                                scope.launch { dataStore.saveEqualizerValues(newList) }
                            },
                            enabled = eqEnabled
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // КНОПКА СБРОСА
            TextButton(
                onClick = { scope.launch { dataStore.saveEqualizerValues(listOf(50f, 50f, 50f, 50f, 50f)) } },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = eqEnabled
            ) {
                Text("СБРОСИТЬ", letterSpacing = 1.sp)
            }
        }
    }
}

// === НОВЫЙ ДИЗАЙН СЛАЙДЕРА (STUDIO STYLE) ===
@Composable
private fun StudioVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 200.dp) // Чуть уже
            .drawBehind {
                val centerX = size.width / 2f
                val startY = 0f
                val endY = size.height

                // 1. Центральная линия (желоб)
                drawLine(
                    color = outlineColor.copy(alpha = 0.2f),
                    start = Offset(centerX, startY),
                    end = Offset(centerX, endY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 2. Горизонтальные засечки (шкала)
                for (i in 0..10) {
                    val y = size.height * (i / 10f)
                    val width = if (i == 5) 12.dp.toPx() else 6.dp.toPx()
                    drawLine(
                        color = outlineColor.copy(alpha = 0.3f),
                        start = Offset(centerX - width/2, y),
                        end = Offset(centerX + width/2, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 3. Заполненная часть (снизу до ручки) - опционально, для стиля "уровень"
                // val thumbY = (50f - value) / 50f * (size.height / 2) + size.height/2
                // ... можно нарисовать линию
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectVerticalDragGestures { change, _ ->
                        val y = change.position.y.coerceIn(0f, size.height.toFloat())
                        // Преобразование Y в значение 0..100 (где 0 внизу, 100 вверху для слайдера, но тут логика +/-)
                        // Логика вашего старого кода: 50 - центр.
                        // Верх (y=0) -> 100? Низ (y=h) -> 0?
                        // Ваша формула была: onValueChange(50f - ((y - size.height / 2f) / half) * 50f)
                        // Она верна для диапазона +/- относительно центра.
                        val halfHeight = size.height / 2f
                        val rawVal = 50f - ((y - halfHeight) / halfHeight) * 50f
                        onValueChange(rawVal.coerceIn(0f, 100f))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // РУЧКА (ФЕЙДЕР)
        // Рассчитываем смещение по Y
        // value 100 -> top (-100dp relative to center?)
        // value 50 -> center (0dp)
        // value 0 -> bottom (+100dp)
        // Высота трека ~200dp. Половина ~100dp.
        val offsetY = ((50f - value) / 50f) * 100.dp // Приблизительно, лучше использовать проценты от высоты BoxWithConstraints, но так проще.

        Box(
            modifier = Modifier
                .offset(y = offsetY)
                .size(width = 32.dp, height = 20.dp) // Прямоугольная ручка
                .background(
                    color = MaterialTheme.colorScheme.surface, // Внутри цвет фона
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 2.dp,
                    color = if(enabled) primaryColor else outlineColor,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            // Полоска на ручке
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .background(if(enabled) primaryColor else outlineColor)
            )
        }
    }
}
