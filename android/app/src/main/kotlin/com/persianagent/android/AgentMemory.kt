package com.persianagent.android

import android.content.Context

class AgentMemory(context: Context) {
    private val prefs = context.getSharedPreferences("agent_memory", Context.MODE_PRIVATE)

    @Synchronized
    fun put(key: String, value: String) {
        val editor = prefs.edit().putString(key, value)
        val memoryKeys = prefs.all.keys
            .filter { it.startsWith("memory_") }
            .sortedBy { it.removePrefix("memory_").toLongOrNull() ?: Long.MAX_VALUE }
        if (memoryKeys.size >= MAX_MEMORIES) {
            memoryKeys.take(memoryKeys.size - MAX_MEMORIES + 1).forEach(editor::remove)
        }
        editor.apply()
    }

    fun all(): Map<String, *> = prefs.all

    private companion object {
        const val MAX_MEMORIES = 100
    }
}
