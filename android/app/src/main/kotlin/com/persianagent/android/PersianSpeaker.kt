package com.persianagent.android

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class PersianSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ready = false
            return
        }
        val languageStatus = tts.setLanguage(Locale("fa", "IR"))
        ready = languageStatus >= TextToSpeech.LANG_AVAILABLE
        if (ready) tts.setSpeechRate(0.95f)
    }

    fun speak(text: String) {
        if (ready && text.isNotBlank()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "persian-agent")
        }
    }

    fun stop() {
        tts.stop()
    }

    fun shutdown() {
        ready = false
        tts.stop()
        tts.shutdown()
    }
}
