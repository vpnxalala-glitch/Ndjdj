package com.vich.assistant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val bg = Color.rgb(13, 18, 32)
    private val card = Color.rgb(25, 34, 54)
    private val accent = Color.rgb(42, 190, 170)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { addView(page) }
        page.addView(TextView(this).apply {
            text = "VICH"
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        page.addView(TextView(this).apply {
            text = "Твои инструменты — в одном окне"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(4), 0, dp(20))
        })
        page.addView(feature("🤖", "ИИ-ассистент", "Открывает онлайн-чат с ИИ. Для ответов нужен интернет."))
        page.addView(feature("📱", "Запуск приложений", "Найди установленное приложение по названию и открой его."))
        page.addView(feature("🌐", "Мини-браузер", "Ищи в интернете или открывай сайты в плавающем окне."))
        page.addView(feature("🪟", "Плавающее окно", "Перемещай окно поверх других приложений, сворачивай его в пузырёк и меняй размер."))
        val start = Button(this).apply {
            text = if (Settings.canDrawOverlays(this@MainActivity)) "ЗАПУСТИТЬ VICH" else "РАЗРЕШИТЬ И ЗАПУСТИТЬ"
            textSize = 15f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, FloatingService::class.java))
                    android.widget.Toast.makeText(this@MainActivity, "Vich запущен. Открой пузырёк поверх приложений.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        val lp = LinearLayout.LayoutParams(-1, dp(54))
        lp.topMargin = dp(22)
        page.addView(start, lp)
        page.addView(TextView(this).apply {
            text = "После выдачи разрешения вернись в Vich и нажми кнопку ещё раз."
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(2, dp(12), 2, 0)
        })
        setContentView(scroll)
    }

    private fun feature(icon: String, title: String, description: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(card)
                cornerRadius = dp(18).toFloat()
            }
        }
        box.addView(TextView(this).apply {
            text = "$icon  $title"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        box.addView(TextView(this).apply {
            text = description
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(6), 0, 0)
        })
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(12)
        box.layoutParams = lp
        return box
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
