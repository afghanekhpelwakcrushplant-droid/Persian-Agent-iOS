package com.persianagent.android

import android.content.Context

class FinalAssistantEngine(context: Context) {
    private val memory = AgentMemory(context)
    private val launcher = AppLauncher(context)
    private val router = ToolRouter(AssistantEngine(context), launcher)

    fun handle(command: String): String {
        val n = command.trim()

        if (n.startsWith("یادت باشه ")) {
            val value = n.removePrefix("یادت باشه ").trim()
            if (value.isBlank()) return "متنی برای ذخیره نگفتی."
            memory.put("memory_" + System.currentTimeMillis(), value)
            return "به خاطر سپردم."
        }

        if (n == "چی یادت هست" || n == "یادت هست چی گفتم") {
            val recent = memory.all().entries
                .filter { it.key.startsWith("memory_") && it.value is String }
                .sortedByDescending { it.key.removePrefix("memory_").toLongOrNull() ?: 0L }
                .take(10)
                .sortedBy { it.key.removePrefix("memory_").toLongOrNull() ?: 0L }
                .mapNotNull { it.value as? String }
            return if (recent.isEmpty()) "چیزی در حافظه ندارم." else recent.joinToString("، ")
        }

        return router.run(n)
    }
}