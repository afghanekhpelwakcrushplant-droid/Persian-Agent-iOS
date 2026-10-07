package com.persianagent.android
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
class PersianRecognizer(
    private val context: Context,
    private val onState: (String) -> Unit,
    private val onText: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) { onState("تشخیص گفتار روی این دستگاه در دسترس نیست."); return }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onState("گوش می‌دهم…") }
            override fun onBeginningOfSpeech() { onState("در حال شنیدن…") }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { onState("در حال پردازش…") }
            override fun onError(error: Int) { onState("تشخیص صدا انجام نشد.") }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onText)
                onState("آماده")
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }
    fun destroy() { recognizer?.destroy(); recognizer = null }
}
