package com.vich.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceCommandActivity extends Activity {
    private static final int REQUEST_SPEECH = 71;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи команду для Vich");
            startActivityForResult(intent, REQUEST_SPEECH);
        } catch (Exception e) {
            Toast.makeText(this, "Голосовой ввод недоступен. Проверь приложение распознавания речи.", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPEECH) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    Intent service = new Intent(this, FloatingService.class);
                    service.setAction("com.vich.assistant.EXECUTE_COMMAND");
                    service.putExtra("command", results.get(0));
                    startService(service);
                }
            }
            finish();
        }
    }
}
