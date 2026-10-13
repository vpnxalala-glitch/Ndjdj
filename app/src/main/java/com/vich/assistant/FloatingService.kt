package com.vich.assistant

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import android.text.TextUtils
import kotlin.math.max
import kotlin.math.min

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var panel: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private var savedWidth = 0
    private var savedHeight = 0
    private var lastX = 0f
    private var lastY = 0f

    private fun rounded(color: Int, radius: Float, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (stroke != Color.TRANSPARENT) setStroke(2, stroke)
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (panel == null) showPanel()
        return START_STICKY
    }

    private fun showPanel() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels
        savedWidth = (screenW * 0.72f).toInt()
        savedHeight = (screenH * 0.52f).toInt()
        params = WindowManager.LayoutParams(
            savedWidth, savedHeight,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenW - width) / 2
            y = (screenH - height) / 3
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.rgb(20, 24, 36), 34f, Color.rgb(62, 74, 100))
            clipToOutline = true
            setPadding(12, 8, 12, 10)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.rgb(31, 37, 54), 26f)
            setPadding(12, 6, 8, 6)
        }
        val handle = TextView(this).apply {
            text = "⠿  Vich  ·  Браузер"
            setTextColor(Color.WHITE); textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            ellipsize = TextUtils.TruncateAt.END; maxLines = 1
        }
        header.addView(handle, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(Button(this).apply { text = "—"; setOnClickListener { collapse() } }, LinearLayout.LayoutParams(42, 42))
        header.addView(Button(this).apply { text = "×"; setOnClickListener { stopSelf() } }, LinearLayout.LayoutParams(42, 42))
        enableDrag(header)

        val presets = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 6, 2, 6)
        }
        presets.addView(TextView(this).apply {
            text = "Размер окна"; setTextColor(Color.LTGRAY); textSize = 12f
        }, LinearLayout.LayoutParams(0, -2, 1f))
        listOf(40, 60, 80).forEach { percent ->
            presets.addView(Button(this).apply {
                text = "$percent%"; textSize = 11f
                setOnClickListener { resizeTo(percent, screenW, screenH) }
            }, LinearLayout.LayoutParams(0, 40, 1f))
        }

        val address = EditText(this).apply {
            hint = "Введите адрес сайта"; setSingleLine(true); textSize = 14f
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setPadding(14, 0, 14, 0)
            background = rounded(Color.rgb(35, 42, 60), 24f)
        }
        lateinit var web: WebView
        val go = Button(this).apply {
            text = "Открыть"
            setOnClickListener {
                var url = address.text.toString().trim()
                if (url.isNotEmpty()) {
                    if (!url.contains("://")) url = if (url.contains(".") && !url.contains(" ")) "https://$url" else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(url, "UTF-8")
                    web.loadUrl(url)
                }
            }
        }
        val addressRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        addressRow.addView(address, LinearLayout.LayoutParams(0, 48, 1f))
        addressRow.addView(go, LinearLayout.LayoutParams(88, 48))

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.setBackgroundColor(Color.WHITE)
        web.loadUrl("https://www.google.com")
        val webHolder = FrameLayout(this).apply {
            background = rounded(Color.WHITE, 24f); clipToOutline = true
            addView(web, FrameLayout.LayoutParams(-1, -1))
        }
        outer.addView(header, LinearLayout.LayoutParams(-1, -2))
        outer.addView(presets, LinearLayout.LayoutParams(-1, -2))
        outer.addView(addressRow, LinearLayout.LayoutParams(-1, -2))
        outer.addView(webHolder, LinearLayout.LayoutParams(-1, 0, 1f))
        val resize = TextView(this).apply {
            text = "◢  Потяни за угол, чтобы изменить размер"
            textSize = 11f; gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setTextColor(Color.LTGRAY); setPadding(0, 6, 4, 0)
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY; true }
                    MotionEvent.ACTION_MOVE -> {
                        params.width = max((screenW * 0.35f).toInt(), min((screenW * 0.96f).toInt(), params.width + (event.rawX - lastX).toInt()))
                        params.height = max((screenH * 0.25f).toInt(), min((screenH * 0.88f).toInt(), params.height + (event.rawY - lastY).toInt()))
                        lastX = event.rawX; lastY = event.rawY
                        safeUpdate(); true
                    }
                    else -> false
                }
            }
        }
        outer.addView(resize, LinearLayout.LayoutParams(-1, 26))
        panel = outer
        wm.addView(outer, params)
    }

    private fun resizeTo(percent: Int, screenW: Int, screenH: Int) {
        params.width = (screenW * percent / 100f).toInt()
        params.height = (screenH * percent / 100f).toInt()
        safeUpdate()
    }

    private fun enableDrag(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x += (event.rawX - lastX).toInt()
                    params.y += (event.rawY - lastY).toInt()
                    lastX = event.rawX; lastY = event.rawY
                    safeUpdate(); true
                }
                else -> false
            }
        }
    }

    private fun safeUpdate() { try { panel?.let { wm.updateViewLayout(it, params) } } catch (_: Exception) {} }

    private fun collapse() {
        val v = panel ?: return
        wm.removeView(v)
        val bubble = TextView(this).apply {
            text = "V"; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); background = rounded(Color.rgb(20, 184, 166), 100f)
            setOnClickListener {
                try { wm.removeView(this) } catch (_: Exception) {}
                panel = null
                showPanel()
            }
        }
        params.width = 58; params.height = 58; params.x = 20; params.y = 240
        panel = bubble
        wm.addView(bubble, params)
    }

    override fun onDestroy() {
        try { panel?.let { wm.removeView(it) } } catch (_: Exception) {}
        panel = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
