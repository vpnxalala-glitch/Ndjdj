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
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import kotlin.math.max
import kotlin.math.min

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var panel: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private var lastX = 0f
    private var lastY = 0f
    private var screenW = 0
    private var screenH = 0
    private var activeTab = "browser"
    private var webView: WebView? = null
    private var content: FrameLayout? = null
    private val bg = Color.rgb(17, 23, 39)
    private val surface = Color.rgb(31, 41, 61)
    private val accent = Color.rgb(35, 190, 170)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Float, stroke: Int = Color.TRANSPARENT) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius.toInt()).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (panel == null) showPanel()
        return START_STICKY
    }

    private fun showPanel() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        screenW = resources.displayMetrics.widthPixels
        screenH = resources.displayMetrics.heightPixels
        params = WindowManager.LayoutParams(
            (screenW * .88f).toInt(), (screenH * .68f).toInt(),
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenW - width) / 2
            y = (screenH - height) / 5
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(bg, 22f, Color.rgb(65, 83, 112))
            setPadding(dp(8), dp(8), dp(8), dp(6))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(surface, 15f)
            setPadding(dp(10), dp(4), dp(4), dp(4))
        }
        header.addView(TextView(this).apply {
            text = "VICH  •  ПОМОЩНИК"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(actionButton("—") { collapse() }, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(actionButton("×") { stopSelf() }, LinearLayout.LayoutParams(dp(42), dp(42)))
        enableDrag(header)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
        }
        listOf("🌐 Браузер" to "browser", "🤖 ИИ" to "ai", "📱 Приложения" to "apps").forEach { (label, key) ->
            tabs.addView(actionButton(label) { showTab(key) }, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        content = FrameLayout(this).apply {
            background = rounded(surface, 15f)
            clipToOutline = true
        }
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        root.addView(tabs, LinearLayout.LayoutParams(-1, -2))
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "Перемещай за верхнюю панель • меняй размер за угол"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(24)))
        root.addView(TextView(this).apply {
            text = "◢"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY; true }
                    MotionEvent.ACTION_MOVE -> {
                        params.width = max(dp(260), min((screenW * .97f).toInt(), params.width + (event.rawX-lastX).toInt()))
                        params.height = max(dp(260), min((screenH * .9f).toInt(), params.height + (event.rawY-lastY).toInt()))
                        lastX = event.rawX; lastY = event.rawY; safeUpdate(); true
                    }
                    else -> true
                }
            }
        }, LinearLayout.LayoutParams(-1, dp(22)))
        panel = root
        wm.addView(root, params)
        showTab(activeTab)
    }

    private fun actionButton(label: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(surface, 12f)
            setPadding(dp(4), 0, dp(4), 0)
            setOnClickListener { action() }
        }

    private fun showTab(tab: String) {
        activeTab = tab
        webView?.let { try { it.stopLoading(); it.destroy() } catch (_: Exception) {} }
        webView = null
        val host = content ?: return
        host.removeAllViews()
        when (tab) {
            "browser" -> showBrowser(host, false)
            "ai" -> showBrowser(host, true)
            else -> showApps(host)
        }
    }

    private fun showBrowser(host: FrameLayout, ai: Boolean) {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val address = EditText(this).apply {
            hint = if (ai) "Спроси ИИ или введи запрос" else "Адрес сайта или поисковый запрос"
            textSize = 14f
            singleLine = true
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            imeOptions = EditorInfo.IME_ACTION_GO
            background = rounded(bg, 12f)
            setPadding(dp(12), 0, dp(12), 0)
        }
        val open = actionButton(if (ai) "Спросить" else "Открыть") { navigate(address.text.toString(), ai) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(address, LinearLayout.LayoutParams(0, dp(46), 1f))
        row.addView(open, LinearLayout.LayoutParams(dp(82), dp(46)))
        column.addView(row)
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.setBackgroundColor(Color.WHITE)
        webView = web
        column.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        host.addView(column, FrameLayout.LayoutParams(-1, -1))
        address.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                navigate(address.text.toString(), ai)
                true
            } else false
        }
        if (ai) {
            address.setText("https://chatgpt.com")
            web.loadUrl("https://chatgpt.com")
        } else web.loadUrl("https://www.google.com")
    }

    private fun navigate(input: String, ai: Boolean) {
        val text = input.trim()
        if (text.isEmpty()) return
        val url = when {
            text.startsWith("https://") || text.startsWith("http://") -> text
            text.contains(".") && !text.contains(" ") -> "https://$text"
            ai -> "https://chatgpt.com/?q=" + java.net.URLEncoder.encode(text, "UTF-8")
            else -> "https://www.google.com/search?q=" + java.net.URLEncoder.encode(text, "UTF-8")
        }
        webView?.loadUrl(url)
    }

    private fun showApps(host: FrameLayout) {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(8))
        }
        val search = EditText(this).apply {
            hint = "Введи название приложения…"
            textSize = 14f
            singleLine = true
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            background = rounded(bg, 12f)
            setPadding(dp(12), 0, dp(12), 0)
        }
        column.addView(search, LinearLayout.LayoutParams(-1, dp(46)))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        host.addView(column, FrameLayout.LayoutParams(-1, -1))
        val apps = try {
            packageManager.getInstalledApplications(0)
                .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null && it.packageName != packageName }
                .map { it.packageName to packageManager.getApplicationLabel(it).toString() }
                .sortedBy { it.second.lowercase() }
        } catch (_: Exception) { emptyList() }

        fun render(query: String) {
            list.removeAllViews()
            val matches = apps.filter { it.second.contains(query, true) || it.first.contains(query, true) }.take(100)
            if (matches.isEmpty()) list.addView(TextView(this).apply {
                text = if (apps.isEmpty()) "Не удалось получить список приложений." else "Ничего не найдено."
                setTextColor(Color.LTGRAY); textSize = 14f
                setPadding(dp(8), dp(16), dp(8), dp(16))
            })
            matches.forEach { (pkg, label) ->
                val item = TextView(this).apply {
                    text = "📲  $label\n      $pkg"
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setPadding(dp(12), dp(12), dp(8), dp(12))
                    background = rounded(bg, 12f)
                    setOnClickListener {
                        val launch = packageManager.getLaunchIntentForPackage(pkg)
                        if (launch != null) {
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try { startActivity(launch) } catch (_: Exception) {
                                Toast.makeText(this@FloatingService, "Не удалось открыть $label", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.topMargin = dp(6)
                list.addView(item, lp)
            }
        }
        render("")
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun enableDrag(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x += (event.rawX-lastX).toInt()
                    params.y += (event.rawY-lastY).toInt()
                    lastX = event.rawX; lastY = event.rawY; safeUpdate(); true
                }
                else -> false
            }
        }
    }

    private fun safeUpdate() { try { panel?.let { wm.updateViewLayout(it, params) } } catch (_: Exception) {} }

    private fun collapse() {
        val old = panel ?: return
        try { wm.removeView(old) } catch (_: Exception) {}
        webView?.let { try { it.destroy() } catch (_: Exception) {} }
        webView = null
        var downX = 0f
        var downY = 0f
        val bubble = TextView(this).apply {
            text = "V"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(accent, 100f)
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; lastX = event.rawX; lastY = event.rawY; true }
                    MotionEvent.ACTION_MOVE -> {
                        params.x += (event.rawX-lastX).toInt()
                        params.y += (event.rawY-lastY).toInt()
                        lastX = event.rawX; lastY = event.rawY; safeUpdate(); true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (kotlin.math.abs(event.rawX-downX) < dp(8) && kotlin.math.abs(event.rawY-downY) < dp(8)) {
                            try { wm.removeView(view) } catch (_: Exception) {}
                            panel = null
                            showPanel()
                        }
                        true
                    }
                    else -> true
                }
            }
        }
        params.width = dp(58); params.height = dp(58); params.x = dp(16); params.y = dp(200)
        panel = bubble
        wm.addView(bubble, params)
    }

    override fun onDestroy() {
        webView?.let { try { it.stopLoading(); it.destroy() } catch (_: Exception) {} }
        try { panel?.let { wm.removeView(it) } } catch (_: Exception) {}
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
