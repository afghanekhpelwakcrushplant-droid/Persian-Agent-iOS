package com.persianagent.android

import android.content.Context
import android.content.Intent

class AppLauncher(private val context: Context) {
    fun openByName(name: String): Boolean {
        val pm = context.packageManager
        val wanted = name.trim()
        val app = pm.getInstalledApplications(0).firstOrNull { info ->
            val label = pm.getApplicationLabel(info).toString()
            label.equals(wanted, ignoreCase = true) || label.contains(wanted, ignoreCase = true)
        } ?: return false

        val intent = pm.getLaunchIntentForPackage(app.packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
