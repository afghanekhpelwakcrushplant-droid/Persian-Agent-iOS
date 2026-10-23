package com.persianagent.android

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.app.RemoteInput
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class PersianNotificationListener : NotificationListenerService(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private val recent = LinkedHashSet<String>()

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeListener = this
    }

    override fun onListenerDisconnected() {
        if (activeListener === this) activeListener = null
        synchronized(replyLock) { latestReply = null }
        super.onListenerDisconnected()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        synchronized(replyLock) {
            if (latestReply?.notificationKey == sbn.key) latestReply = null
        }
        super.onNotificationRemoved(sbn)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val result = tts?.setLanguage(Locale("fa", "IR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ready = false
            } else {
                tts?.setSpeechRate(0.95f)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in MESSAGE_PACKAGES) return

        // Capture direct-reply capability independently of TTS readiness or message preview.
        val replyAction = sbn.notification.actions
            ?.firstOrNull { action ->
                action.remoteInputs?.any { it.allowFreeFormInput } == true
            }
        synchronized(replyLock) {
            // Never leave a previous sender's reply action active when a newer notification arrives.
            latestReply = replyAction?.let {
                ReplyTarget(it.actionIntent, it.remoteInputs, sbn.key)
            }
        }

        if (!ready || !isReadingEnabled(this)) return
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
        if (activeListener === this) activeListener = null
        super.onDestroy()
    }

    private data class ReplyTarget(
        val pendingIntent: PendingIntent,
        val remoteInputs: Array<RemoteInput>,
        val notificationKey: String
    )

    companion object {
        private val replyLock = Any()
        private const val PREFS = "notification_preferences"
        private const val KEY_READ_ALOUD = "read_aloud_enabled"

        fun isReadingEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_READ_ALOUD, false)

        fun setReadingEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_READ_ALOUD, enabled).apply()
            // Turning privacy-sensitive readout off must also stop any queued speech immediately.
            if (!enabled) activeListener?.tts?.stop()
        }

        @Volatile private var activeListener: PersianNotificationListener? = null
        @Volatile private var latestReply: ReplyTarget? = null

        fun replyToLatest(text: String): Boolean {
            if (text.isBlank()) return false
            val service = activeListener ?: return false
            val target = synchronized(replyLock) { latestReply } ?: return false
            return try {
                val intent = Intent()
                val results = Bundle()
                target.remoteInputs.forEach { results.putCharSequence(it.resultKey, text) }
                RemoteInput.addResultsToIntent(target.remoteInputs, intent, results)
                target.pendingIntent.send(service, 0, intent)
                synchronized(replyLock) {
                    if (latestReply === target) latestReply = null
                }
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