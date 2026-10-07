package com.persianagent.android

import android.content.Context

class AgentMemory(context: Context) {
    private val prefs = context.getSharedPreferences("agent_memory", Context.MODE_PRIVATE)

    fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun all(): Map<String, *> = prefs.all
}
