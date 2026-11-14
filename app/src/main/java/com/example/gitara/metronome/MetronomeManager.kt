package com.example.gitara.metronome

import android.content.Context

object MetronomeManager {
    private var currentMetronome: Metronome? = null

    fun create(context: Context, onBeat: (Int) -> Unit): Metronome {
        stop() // Очищаем старый
        val metronome = Metronome(context.applicationContext, onBeat)
        currentMetronome = metronome
        return metronome
    }

    fun stop() {
        currentMetronome?.stop()
        currentMetronome?.release()
        currentMetronome = null
    }

    fun getCurrent(): Metronome? = currentMetronome
}