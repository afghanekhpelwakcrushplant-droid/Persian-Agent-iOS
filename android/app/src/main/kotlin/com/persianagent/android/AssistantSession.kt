package com.persianagent.android

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession

class AssistantSession(context: android.content.Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("launched_as_assistant", true)
        }
        applicationContext.startActivity(intent)
    }
}
