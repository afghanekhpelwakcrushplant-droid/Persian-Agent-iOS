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
            memory.put("memory_" + System.currentTimeMillis(), value)
            return "به خاطر سپردم."
        }

        if (n == "چی یادت هست" || n == "یادت هست چی گفتم") {
            val values = memory.all().values.filterIsInstance<String>().takeLast(10)
            return if (values.isEmpty()) "چیزی در حافظه ندارم." else values.joinToString("، ")
        }

        return router.run(n)
    }
}
