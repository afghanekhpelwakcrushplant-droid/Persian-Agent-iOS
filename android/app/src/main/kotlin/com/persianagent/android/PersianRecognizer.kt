package com.persianagent.android

import android.content.Context
import android.content.Intent
import android.os.Build
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
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onState("سرویس تشخیص گفتار روی این دستگاه در دسترس نیست.")
            return
        }

        recognizer?.destroy()
        recognizer = createRecognizer()
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onState("گوش می‌دهم…") }
            override fun onBeginningOfSpeech() { onState("در حال شنیدن…") }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { onState("در حال پردازش گفتار فارسی…") }
            override fun onError(error: Int) {
                onState(when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "خطای میکروفون؛ دسترسی میکروفون را بررسی کنید."
                    SpeechRecognizer.ERROR_CLIENT -> "سرویس تشخیص صدا متوقف شد؛ دوباره تلاش کنید."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز میکروفون داده نشده است."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "اتصال اینترنت برای تشخیص گفتار لازم است یا سرویس در دسترس نیست."
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "گفتاری تشخیص داده نشد؛ دوباره صحبت کنید."
                    else -> "تشخیص گفتار ناموفق بود (کد $error)."
                })
            }
            override fun onResults(results: Bundle?) {
                val recognized = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (recognized.isNullOrBlank()) {
                    onState("گفتار قابل تشخیصی دریافت نشد.")
                } else {
                    onText(recognized)
                    onState("آماده")
                }
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "فرمان خود را به فارسی بگویید")
        }
        recognizer?.startListening(intent)
    }

    private fun createRecognizer(): SpeechRecognizer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            try {
                return SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } catch (_: UnsupportedOperationException) {
                // Fall back to the system recognition provider below.
            }
        }
        return SpeechRecognizer.createSpeechRecognizer(context)
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}
