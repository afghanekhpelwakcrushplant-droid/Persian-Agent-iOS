package com.persianagent.android

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class PersianNotificationListener : NotificationListenerService(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val recent = LinkedHashSet<String>()

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale("fa", "IR")
            tts?.setSpeechRate(0.95f)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!ready || sbn.packageName !in MESSAGE_PACKAGES) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()
        if (body.isBlank() || body == "۱ پیام جدید" || body == "New message") return

        val key = "${sbn.packageName}:${sbn.key}:$body"
        synchronized(recent) {
            if (!recent.add(key)) return
            while (recent.size > 40) recent.remove(recent.first())
        }
        val message = if (title.isBlank()) body else "$title می‌گوید: $body"
        tts?.speak(message, TextToSpeech.QUEUE_ADD, Bundle(), key.take(100))
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    companion object {
        private val MESSAGE_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.google.android.apps.messaging",
            "com.android.messaging",
            "com.samsung.android.messaging"
        )
    }
}