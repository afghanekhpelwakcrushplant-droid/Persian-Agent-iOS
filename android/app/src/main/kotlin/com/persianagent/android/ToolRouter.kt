package com.persianagent.android

class ToolRouter(
    private val engine: AssistantEngine,
    private val launcher: AppLauncher
) {
    fun run(command: String): String {
        val n = command.trim()
        if (n.startsWith("باز کن ")) {
            val name = n.removePrefix("باز کن ").trim()
            return if (launcher.openByName(name)) {
                "برنامه «$name» را باز کردم."
            } else {
                "برنامه «$name» را پیدا نکردم."
            }
        }
        return engine.handle(n)
    }
}
