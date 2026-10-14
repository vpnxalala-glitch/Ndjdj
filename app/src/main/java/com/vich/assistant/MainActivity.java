package com.vich.assistant;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TELEGRAM_URL = "https://web.telegram.org/a/";
    private final int bg = Color.rgb(9, 12, 25);
    private final int accent = Color.rgb(44, 218, 220);
    private WebView web;
    private ProgressBar progress;
    private TextView status;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(14));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(6), dp(12), dp(6));
        toolbar.setBackgroundColor(Color.rgb(16, 22, 39));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("VichGram");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        TextView subtitle = new TextView(this);
        subtitle.setText("TELEGRAM CLIENT");
        subtitle.setTextColor(accent);
        subtitle.setTextSize(9);
        subtitle.setLetterSpacing(.15f);
        brand.addView(title);
        brand.addView(subtitle);
        toolbar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        Button reload = toolbarButton("↻");
        reload.setOnClickListener(v -> { if (web != null) web.reload(); });
        toolbar.addView(reload, new LinearLayout.LayoutParams(dp(44), dp(44)));

        Button floating = toolbarButton("▣");
        floating.setOnClickListener(v -> {
            try {
                startService(new Intent(this, FloatingService.class).putExtra("tab", "telegram"));
                Toast.makeText(this, "Telegram открыт в плавающем окне", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть окно", Toast.LENGTH_SHORT).show();
            }
        });
        toolbar.addView(floating, new LinearLayout.LayoutParams(dp(44), dp(44)));

        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        progress.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        FrameLayout webArea = new FrameLayout(this);
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setBackgroundColor(Color.rgb(23, 33, 43));
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
            @Override public void onReceivedTitle(WebView view, String pageTitle) {
                status.setText(pageTitle == null || pageTitle.isEmpty() ? "Telegram" : pageTitle);
            }
        });
        webArea.addView(web, new FrameLayout.LayoutParams(-1, -1));
        root.addView(webArea, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(12), dp(5), dp(12), dp(5));
        footer.setBackgroundColor(Color.rgb(16, 22, 39));
        status = new TextView(this);
        status.setText("Подключение к Telegram…");
        status.setTextColor(Color.rgb(174, 190, 214));
        status.setTextSize(11);
        footer.addView(status, new LinearLayout.LayoutParams(0, dp(28), 1));
        TextView external = new TextView(this);
        external.setText("Открыть сайт ↗");
        external.setTextColor(accent);
        external.setTextSize(12);
        external.setPadding(dp(8), dp(6), dp(4), dp(6));
        external.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(TELEGRAM_URL))); }
            catch (Exception ignored) { Toast.makeText(this, "Нет браузера для открытия ссылки", Toast.LENGTH_SHORT).show(); }
        });
        footer.addView(external);
        root.addView(footer, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
        web.loadUrl(TELEGRAM_URL);
    }

    private Button toolbarButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(33, 47, 70)));
        return button;
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
