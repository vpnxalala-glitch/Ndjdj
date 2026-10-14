package com.vich.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Minimal launcher placeholder for the clean VichGram base.
 *
 * The previous hand-built chat UI has been removed from this branch so a
 * proper Telegram Android client implementation can be integrated without
 * carrying over the old mock interface. Replace this Activity as part of the
 * planned client integration; do not treat this placeholder as a messenger.
 */
public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView status = new TextView(this);
        status.setText("VichGram\nClean Android base ready for client integration");
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(32, 32, 32, 32);
        setContentView(status);
    }
}
