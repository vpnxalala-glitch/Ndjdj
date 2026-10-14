package com.vich.assistant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(9, 12, 25), card = Color.rgb(20, 28, 48), accent = Color.rgb(44, 218, 220), purple = Color.rgb(151, 91, 255);
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{color, Color.rgb(30, 24, 57)}); d.setCornerRadius(dp(20)); d.setStroke(dp(1), Color.rgb(58, 89, 133)); return d; }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20),dp(28),dp(20),dp(28)); page.setBackgroundColor(bg); scroll.addView(page);
        TextView eyebrow = new TextView(this); eyebrow.setText("NEON MESSENGER PROJECT"); eyebrow.setTextSize(11); eyebrow.setLetterSpacing(.16f); eyebrow.setTextColor(accent); page.addView(eyebrow);
        TextView title = new TextView(this); title.setText("VichGram"); title.setTextSize(34); title.setTypeface(null,Typeface.BOLD); title.setTextColor(Color.WHITE); page.addView(title);
        TextView sub = new TextView(this); sub.setText("Твой киберпанк-интерфейс для Android"); sub.setTextSize(16); sub.setTextColor(Color.rgb(184,197,220)); sub.setPadding(0,dp(4),0,dp(22)); page.addView(sub);
        TextView status = new TextView(this); status.setText("●  NEON MODE   •   FLOATING UI"); status.setTextSize(12); status.setTextColor(accent); status.setPadding(dp(12),dp(12),dp(12),dp(12)); status.setBackground(shape(card)); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.bottomMargin=dp(16);page.addView(status,sp);
        page.addView(card("🤖","ИИ-ассистент","Открыть веб-помощника в отдельном плавающем окне.", "Открыть ИИ", () -> launchTab("ai")));
        page.addView(card("🎯","Командный помощник","Текстовые и голосовые команды для приложений и настроек.", "Ввести команду", () -> launchTab("commands")));
        page.addView(card("📱","Приложения","Поиск среди установленных приложений и запуск одним нажатием.", "Выбрать приложение", () -> launchTab("apps")));
        page.addView(card("🌐","Браузер","Встроенный браузер в окне с настраиваемым размером.", "Открыть браузер", () -> launchTab("browser")));
        page.addView(card("📐","Размеры окна","Настрой размер плавающего окна и пузыря.", "Настроить размеры", () -> launchTab("sizes")));
        page.addView(card("🧰","Системные инструменты","Быстрый переход к настройкам Android.", "Инструменты", () -> launchTab("tools")));
        TextView note = new TextView(this); note.setText("Следующий этап: интеграция Telegram через TDLib. Авторизация и синхронизация чатов ещё не реализованы."); note.setTextSize(12); note.setTextColor(Color.rgb(160,174,199)); note.setPadding(dp(4),dp(8),dp(4),dp(14)); page.addView(note);
        Button start = new Button(this); start.setText("ЗАПУСТИТЬ ПЛАВАЮЩЕЕ МЕНЮ"); start.setTextColor(Color.rgb(7,14,28)); start.setTypeface(null,Typeface.BOLD); start.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1,dp(56)); bp.topMargin=dp(8); page.addView(start,bp);
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));
            else { startService(new Intent(this,FloatingService.class)); Toast.makeText(this,"VichGram запущен",Toast.LENGTH_SHORT).show(); }
        });
        setContentView(scroll);
    }
    private LinearLayout card(String icon,String heading,String desc,String button,Runnable action) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(15),dp(16),dp(14)); box.setBackground(shape(card));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(12); box.setLayoutParams(lp);
        TextView h=new TextView(this); h.setText(icon+"  "+heading); h.setTextSize(18); h.setTypeface(null,Typeface.BOLD); h.setTextColor(Color.WHITE); box.addView(h);
        TextView d=new TextView(this); d.setText(desc); d.setTextSize(14); d.setTextColor(Color.rgb(185,198,220)); d.setPadding(0,dp(7),0,dp(10)); box.addView(d);
        Button b=new Button(this); b.setText(button); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(purple)); box.addView(b); b.setOnClickListener(v -> action.run());
        box.setOnClickListener(v -> action.run()); return box;
    }
    private void launchTab(String tab) {
        if (!Settings.canDrawOverlays(this)) { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))); Toast.makeText(this,"Разреши показ поверх других приложений, затем снова нажми кнопку",Toast.LENGTH_LONG).show(); return; }
        Intent i=new Intent(this,FloatingService.class); i.putExtra("tab",tab); startService(i);
    }
}
