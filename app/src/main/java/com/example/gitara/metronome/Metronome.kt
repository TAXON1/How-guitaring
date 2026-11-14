package com.example.gitara.metronome

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.core.content.getSystemService
import com.example.gitara.R
import kotlinx.coroutines.*
import kotlin.math.roundToInt

class Metronome(
    private val context: Context,
    private val onBeat: (Int) -> Unit
) {
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Свойство для управления вибрацией (меняется на лету)
    var isVibrationEnabled: Boolean = false

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var tickSoundId: Int = 0
    private var accentSoundId: Int = 0
    private val vibrator: Vibrator? = context.getSystemService()

    init {
        // Убедитесь, что файлы raw существуют
        tickSoundId = soundPool.load(context, R.raw.metronome_tick, 1)
        accentSoundId = soundPool.load(context, R.raw.metronome_accent, 1)
    }

    fun start(bpm: Int, beatsPerBar: Int) {
        stop()
        if (bpm <= 0) return

        job = scope.launch {
            val intervalMs = 60_000 / bpm.toDouble()
            var beatIndex = 0
            var nextTime = System.currentTimeMillis()

            while (isActive) {
                val now = System.currentTimeMillis()
                val delayTime = nextTime - now

                if (delayTime <= 0) {
                    val isAccent = beatIndex % beatsPerBar == 0
                    val soundId = if (isAccent) accentSoundId else tickSoundId

                    // 🔥 МАКСИМАЛЬНАЯ ГРОМКОСТЬ
                    val volume = 2.0f

                    // Звук
                    soundPool.play(soundId, volume, volume, 1, 0, 1f)

                    // ВИБРАЦИЯ (Только если включена)
                    if (isVibrationEnabled) {
                        vibrator?.let { vib ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                // Короткий четкий удар
                                val effect = if (isAccent)
                                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                                else
                                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                                vib.vibrate(effect)
                            } else {
                                @Suppress("DEPRECATION")
                                vib.vibrate(if (isAccent) 80 else 40)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        onBeat(beatIndex % beatsPerBar)
                    }

                    beatIndex++
                    nextTime += intervalMs.roundToInt()
                } else {
                    delay(delayTime.coerceAtMost(20))
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun release() {
        stop()
        soundPool.release()
        scope.cancel()
    }
}
