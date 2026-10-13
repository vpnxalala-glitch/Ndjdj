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
    private final int bg = Color.rgb(13,18,32), card = Color.rgb(25,34,54), accent = Color.rgb(35,190,170);
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(18)); return d; }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20),dp(28),dp(20),dp(28)); page.setBackgroundColor(bg); scroll.addView(page);
        TextView title = new TextView(this); title.setText("VICH"); title.setTextSize(32); title.setTypeface(null,Typeface.BOLD); title.setTextColor(Color.WHITE); page.addView(title);
        TextView sub = new TextView(this); sub.setText("Твой помощник для Android"); sub.setTextSize(17); sub.setTextColor(Color.LTGRAY); sub.setPadding(0,dp(4),0,dp(22)); page.addView(sub);
        page.addView(card("🤖","ИИ-ассистент","Открой ChatGPT и задавай вопросы.", "Открыть ИИ", () -> launchTab("ai")));
        page.addView(card("🎯","Командный помощник","Напиши: «открой YouTube», «найди погоду», «открой настройки».", "Ввести команду", () -> launchTab("commands")));
        page.addView(card("📱","Приложения","Поиск среди установленных приложений и запуск одним нажатием.", "Выбрать приложение", () -> launchTab("apps")));
        page.addView(card("🌐","Браузер и поиск","Открывай сайты и ищи информацию в интернете.", "Открыть браузер", () -> launchTab("browser")));
        page.addView(card("🧰","Быстрые инструменты","Настройки Wi-Fi, Bluetooth, звук и системные настройки Android.", "Инструменты", () -> launchTab("tools")));
        Button start = new Button(this); start.setText("ЗАПУСТИТЬ ПЛАВАЮЩЕЕ МЕНЮ"); start.setTextColor(Color.WHITE); start.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1,dp(54)); bp.topMargin=dp(12); page.addView(start,bp);
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));
            else { startService(new Intent(this,FloatingService.class)); Toast.makeText(this,"Vich запущен",Toast.LENGTH_SHORT).show(); }
        });
        setContentView(scroll);
    }
    private LinearLayout card(String icon,String heading,String desc,String button, Runnable action) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(15),dp(16),dp(14)); box.setBackground(shape(card));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(12); box.setLayoutParams(lp);
        TextView h=new TextView(this); h.setText(icon+"  "+heading); h.setTextSize(18); h.setTypeface(null,Typeface.BOLD); h.setTextColor(Color.WHITE); box.addView(h);
        TextView d=new TextView(this); d.setText(desc); d.setTextSize(14); d.setTextColor(Color.LTGRAY); d.setPadding(0,dp(7),0,dp(10)); box.addView(d);
        Button b=new Button(this); b.setText(button); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent)); box.addView(b); b.setOnClickListener(v -> action.run());
        box.setOnClickListener(v -> action.run()); return box;
    }
    private void launchTab(String tab) {
        if (!Settings.canDrawOverlays(this)) { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))); Toast.makeText(this,"Разреши показ поверх других приложений, затем снова нажми кнопку",Toast.LENGTH_LONG).show(); return; }
        Intent i=new Intent(this,FloatingService.class); i.putExtra("tab",tab); startService(i);
    }
}
