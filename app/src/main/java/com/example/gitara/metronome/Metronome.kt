package com.example.gitara.metronome

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import com.example.gitara.R
import kotlinx.coroutines.*

class Metronome(
    private val context: Context,
    private val onBeat: (Int) -> Unit
) {
    private var job: Job? = null
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Свойство для управления вибрацией (меняется на лету)
    var isVibrationEnabled: Boolean = false

    private var soundPool: SoundPool? = null
    private var tickSoundId: Int = 0
    private var accentSoundId: Int = 0
    @Volatile
    private var isSoundPoolLoaded = false
    private var isReleased = false

    private val vibrator: Vibrator? by lazy {
        context.applicationContext.getSystemService()
    }

    init {
        initSoundPool()
    }

    @Synchronized
    private fun initSoundPool() {
        if (soundPool != null) return

        val sp = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        val loadedIds = mutableSetOf<Int>()
        sp.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedIds.add(sampleId)
                if (tickSoundId != 0 && accentSoundId != 0 &&
                    loadedIds.contains(tickSoundId) && loadedIds.contains(accentSoundId)
                ) {
                    isSoundPoolLoaded = true
                }
            }
        }

        tickSoundId = sp.load(context.applicationContext, R.raw.metronome_tick, 1)
        accentSoundId = sp.load(context.applicationContext, R.raw.metronome_accent, 1)
        soundPool = sp
    }

    @Synchronized
    private fun ensureActive() {
        if (isReleased || !scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            isReleased = false
        }
        if (soundPool == null) {
            initSoundPool()
        }
    }

    fun start(bpm: Int, beatsPerBar: Int) {
        stop()
        if (bpm <= 0 || beatsPerBar <= 0) return

        ensureActive()

        job = scope.launch {
            val intervalMs = 60_000.0 / bpm
            var beatIndex = 0
            var startTime = System.currentTimeMillis()

            while (isActive) {
                val targetTime = startTime + (beatIndex * intervalMs).toLong()
                val now = System.currentTimeMillis()

                // Если произошла существенная задержка (больше 2 интервалов), подстраиваем отсчет
                if (now - targetTime > intervalMs * 2) {
                    startTime = now - (beatIndex * intervalMs).toLong()
                }

                val delayTime = targetTime - now

                if (delayTime <= 0) {
                    val currentBeatIndex = beatIndex % beatsPerBar
                    val isAccent = currentBeatIndex == 0
                    val soundId = if (isAccent) accentSoundId else tickSoundId

                    // 1. Воспроизведение звука в фоновом потоке без задержек
                    if (soundId != 0) {
                        val priority = if (isAccent) 2 else 1
                        soundPool?.play(soundId, 1.0f, 1.0f, priority, 0, 1f)
                    }

                    // 2. Вибрация
                    if (isVibrationEnabled) {
                        try {
                            vibrator?.let { vib ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val effect = if (isAccent)
                                        VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                                    else
                                        VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                                    vib.vibrate(effect)
                                } else {
                                    @Suppress("DEPRECATION")
                                    vib.vibrate(if (isAccent) 80L else 40L)
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }

                    // 3. Асинхронный вызов обновления UI (не блокирует аудио-поток)
                    scope.launch(Dispatchers.Main) {
                        onBeat(currentBeatIndex)
                    }

                    beatIndex++
                } else {
                    delay(delayTime.coerceAtMost(10))
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    @Synchronized
    fun release() {
        stop()
        isReleased = true
        try {
            soundPool?.release()
        } catch (_: Exception) {
        }
        soundPool = null
        isSoundPoolLoaded = false
        scope.cancel()
    }
}
