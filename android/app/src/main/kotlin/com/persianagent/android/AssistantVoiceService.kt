package com.persianagent.android

import android.os.Bundle
import android.service.voice.VoiceInteractionService

class AssistantVoiceService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        setDisabledShowContext(0)
    }

    override fun onShutdown() {
        super.onShutdown()
    }
}
