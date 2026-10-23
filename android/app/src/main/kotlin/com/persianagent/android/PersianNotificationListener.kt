package com.persianagent.android

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.app.PendingIntent
import android.app.RemoteInput
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
        activeListener = this
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

        val replyAction = sbn.notification.actions
            ?.firstOrNull { action -> !action.remoteInputs.isNullOrEmpty() }
        if (replyAction != null) {
            synchronized(recent) {
                latestReply = ReplyTarget(replyAction.actionIntent, replyAction.remoteInputs)
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        if (activeListener === this) activeListener = null
        super.onDestroy()
    }

    private data class ReplyTarget(
        val pendingIntent: PendingIntent,
        val remoteInputs: Array<RemoteInput>
    )

    companion object {
        @Volatile private var activeListener: PersianNotificationListener? = null
        @Volatile private var latestReply: ReplyTarget? = null

        fun replyToLatest(text: String): Boolean {
            val target = latestReply ?: return false
            return try {
                val intent = Intent()
                val results = Bundle()
                target.remoteInputs.forEach { results.putCharSequence(it.resultKey, text) }
                RemoteInput.addResultsToIntent(target.remoteInputs, intent, results)
                target.pendingIntent.send(activeListener, 0, intent)
                latestReply = null
                true
            } catch (_: Exception) {
                false
            }
        }

        private val MESSAGE_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.google.android.apps.messaging",
            "com.android.messaging",
            "com.samsung.android.messaging"
        )
    }
}