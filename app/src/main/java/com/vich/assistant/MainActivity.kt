package com.vich.assistant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(15, 18, 28))
        }
        val title = TextView(this).apply {
            text = "Vich"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val desc = TextView(this).apply {
            text = "Плавающий ассистент и мини-браузер\nОкно можно перемещать и менять его размер."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 24)
        }
        val start = Button(this).apply {
            text = "Разрешить показ поверх приложений"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, FloatingService::class.java))
                    finish()
                }
            }
        }
        layout.addView(title)
        layout.addView(desc)
        layout.addView(start)
        setContentView(layout)
    }
}
