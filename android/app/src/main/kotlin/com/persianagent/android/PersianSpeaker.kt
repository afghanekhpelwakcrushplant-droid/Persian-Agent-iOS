package com.persianagent.android
import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
class PersianSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale("fa", "IR")
            tts.setSpeechRate(0.95f)
        }
    }
    fun speak(text: String) { if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "persian-agent") }
    fun stop() { tts.stop() }
    fun shutdown() { tts.stop(); tts.shutdown() }
}
