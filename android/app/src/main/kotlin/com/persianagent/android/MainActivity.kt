package com.persianagent.android

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var speaker: PersianSpeaker
    private lateinit var recognizer: PersianRecognizer
    private lateinit var engine: AssistantEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaker = PersianSpeaker(this)
        engine = AssistantEngine(this)
        recognizer = PersianRecognizer(
            this,
            onState = { state -> runOnUiThread { status.text = state } },
            onText = { text -> runOnUiThread { input.setText(text); execute(text) } }
        )
        buildUi()
        requestCorePermissions()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(28, 32, 28, 28)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val title = TextView(this).apply { text = "دستیار فارسی"; textSize = 28f; gravity = Gravity.RIGHT }
        val subtitle = TextView(this).apply { text = "Agentic AI — Android"; textSize = 16f; gravity = Gravity.RIGHT }
        input = EditText(this).apply {
            hint = "مثلاً: به علی زنگ بزن"
            textSize = 18f
            minLines = 3
            gravity = Gravity.TOP or Gravity.RIGHT
        }
        val run = Button(this).apply { text = "اجرا"; setOnClickListener { execute(input.text.toString()) } }
        val listen = Button(this).apply { text = "🎙 صحبت فارسی"; setOnClickListener { recognizer.start() } }
        status = TextView(this).apply { text = "آماده"; textSize = 15f }
        output = TextView(this).apply {
            text = "فرمان خود را بگویید یا بنویسید."
            textSize = 18f
            gravity = Gravity.RIGHT
            setPadding(0, 24, 0, 24)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))
        root.addView(input, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(run, LinearLayout.LayoutParams(-1, -2))
        root.addView(listen, LinearLayout.LayoutParams(-1, -2))
        root.addView(status, LinearLayout.LayoutParams(-1, -2))
        root.addView(output, LinearLayout.LayoutParams(-1, -2))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun execute(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        status.text = "در حال اجرا…"
        val answer = engine.handle(clean)
        output.text = answer
        status.text = "آماده"
        speaker.speak(answer)
    }

    private fun requestCorePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (android.os.Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        requestPermissions(permissions.toTypedArray(), 100)
    }

    override fun onDestroy() {
        recognizer.destroy()
        speaker.shutdown()
        super.onDestroy()
    }
}
