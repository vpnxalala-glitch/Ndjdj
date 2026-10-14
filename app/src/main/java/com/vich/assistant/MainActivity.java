package com.vich.assistant;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 12, 25);
    private static final int PANEL = Color.rgb(17, 25, 43);
    private static final int CYAN = Color.rgb(44, 218, 220);
    private static final int WHITE = Color.rgb(239, 245, 255);
    private static final int MUTED = Color.rgb(151, 170, 197);

    private Client client;
    private LinearLayout root;
    private LinearLayout content;
    private TextView status;
    private EditText apiIdField, apiHashField, phoneField, codeField, passwordField;
    private String apiId = "", apiHash = "";
    private boolean parametersSent;
    private TdApi.Chat selectedChat;
    private EditText messageField;
    private final List<TdApi.Chat> chats = new ArrayList<>();
    private final List<TdApi.Message> messages = new ArrayList<>();

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(16));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildShell();
        showWelcome();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(12), dp(18), dp(12));
        header.setBackgroundColor(PANEL);
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("VichGram", 23, WHITE, true);
        TextView subtitle = text("PRIVATE MESSENGER  /  TDLib", 10, CYAN, true);
        brand.addView(title);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView dot = text("●", 13, CYAN, true);
        header.addView(dot);
        root.addView(header);

        status = text("Подготовка Telegram-клиента…", 12, MUTED, false);
        status.setPadding(dp(18), dp(10), dp(18), dp(10));
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(24));
        scroll.setFillViewport(true);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = text("VichGram • Telegram через официальный TDLib", 10, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(8), dp(10), dp(8), dp(10));
        root.addView(footer);
        setContentView(root);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private EditText field(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setTextColor(WHITE);
        e.setHintTextColor(MUTED);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setPadding(dp(14), dp(8), dp(14), dp(8));
        e.setBackground(rounded(Color.rgb(23, 34, 55), Color.rgb(44, 63, 91)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.bottomMargin = dp(10);
        content.addView(e, lp);
        return e;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(BG);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(CYAN, 0));
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
        lp.bottomMargin = dp(12);
        content.addView(b, lp);
        return b;
    }

    private void heading(String title, String body) {
        TextView h = text(title, 22, WHITE, true);
        h.setPadding(0, dp(8), 0, dp(6));
        content.addView(h);
        TextView p = text(body, 13, MUTED, false);
        p.setLineSpacing(dp(3), 1f);
        p.setPadding(0, 0, 0, dp(18));
        content.addView(p);
    }

    private void showWelcome() {
        content.removeAllViews();
        heading("Твой Telegram, по-новому", "Нативный интерфейс VichGram. Для входа нужны API credentials Telegram — они вводятся только на устройстве.");
        apiIdField = field("API ID (число с my.telegram.org)", InputType.TYPE_CLASS_NUMBER);
        apiHashField = field("API Hash", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        phoneField = field("Номер телефона с кодом страны", InputType.TYPE_CLASS_PHONE);
        button("Подключить Telegram", this::startAuthorization);
        TextView note = text("API ID и API Hash можно получить на my.telegram.org. Не отправляй их в чат и не добавляй в публичный репозиторий.", 12, MUTED, false);
        note.setLineSpacing(dp(3), 1f);
        content.addView(note);
        setStatus("Ожидание данных для подключения");
    }

    private void startAuthorization() {
        if (client == null) {
            toast("TDLib не загрузилась. Проверь сборку приложения.");
            return;
        }
        apiId = apiIdField.getText().toString().trim();
        apiHash = apiHashField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        if (apiId.isEmpty() || apiHash.isEmpty() || phone.isEmpty()) {
            toast("Заполни API ID, API Hash и номер телефона");
            return;
        }
        try {
            Integer.parseInt(apiId);
        } catch (NumberFormatException e) {
            toast("API ID должен быть числом");
            return;
        }
        pendingPhone = phone;
        if (client == null) {
            try {
                Client.setLogMessageHandler(1, (level, message) -> android.util.Log.d("VichGram-TDLib", message));
                client = Client.create(this::onTdUpdate, e -> runOnUiThread(() -> setStatus("Ошибка TDLib: " + e.getMessage())), e -> runOnUiThread(() -> setStatus("Ошибка: " + e.getMessage())));
                setStatus("Подключаем TDLib и готовим базу…");
            } catch (Throwable e) {
                setStatus("Не удалось загрузить TDLib: " + e.getClass().getSimpleName());
                toast("Ошибка загрузки TDLib: " + e.getMessage());
            }
        } else if (parametersSent) {
            client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), this::handleResponse);
        }
    }

    private String pendingPhone = "";

    private void onTdUpdate(TdApi.Object update) {
        if (update instanceof TdApi.UpdateAuthorizationState) {
            TdApi.AuthorizationState state = ((TdApi.UpdateAuthorizationState) update).authorizationState;
            if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
                sendParameters();
            } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
                runOnUiThread(() -> showPhoneEntry());
            } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
                runOnUiThread(() -> showCodeEntry());
            } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
                runOnUiThread(() -> showPasswordEntry());
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                runOnUiThread(this::showChats);
                client.send(new TdApi.GetChats(new TdApi.ChatListMain(), 100), this::handleResponse);
            } else if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation) {
                runOnUiThread(() -> setStatus("Подтверди вход по ссылке, показанной Telegram."));
            } else if (state instanceof TdApi.AuthorizationStateClosed) {
                runOnUiThread(() -> setStatus("Соединение с TDLib закрыто."));
            }
        } else if (update instanceof TdApi.UpdateNewChat) {
            TdApi.Chat chat = ((TdApi.UpdateNewChat) update).chat;
            synchronized (chats) {
                int existing = indexOfChat(chat.id);
                if (existing >= 0) chats.set(existing, chat); else chats.add(chat);
            }
            runOnUiThread(this::renderChats);
        } else if (update instanceof TdApi.UpdateChatTitle) {
            long id = ((TdApi.UpdateChatTitle) update).chatId;
            synchronized (chats) {
                int i = indexOfChat(id);
                if (i >= 0) chats.get(i).title = ((TdApi.UpdateChatTitle) update).title;
            }
            runOnUiThread(this::renderChats);
        } else if (update instanceof TdApi.UpdateNewMessage) {
            TdApi.Message message = ((TdApi.UpdateNewMessage) update).message;
            if (selectedChat != null && selectedChat.id == message.chatId) {
                synchronized (messages) { messages.add(0, message); }
                runOnUiThread(this::renderMessages);
            }
        }
    }

    private void sendParameters() {
        if (parametersSent) return;
        try {
            File files = getFilesDir();
            TdApi.SetTdlibParameters p = new TdApi.SetTdlibParameters();
            p.databaseDirectory = new File(files, "tdlib").getAbsolutePath();
            p.filesDirectory = new File(getFilesDir(), "tdfiles").getAbsolutePath();
            p.useFileDatabase = true;
            p.useChatInfoDatabase = true;
            p.useMessageDatabase = true;
            p.useSecretChats = false;
            p.apiId = Integer.parseInt(apiId);
            p.apiHash = apiHash;
            p.systemLanguageCode = Locale.getDefault().getLanguage();
            p.deviceModel = android.os.Build.MODEL;
            p.systemVersion = android.os.Build.VERSION.RELEASE;
            p.applicationVersion = "0.3.0";
            p.enableStorageOptimizer = true;
            p.ignoreFileNames = false;
            parametersSent = true;
            client.send(p, this::handleResponse);
            if (!pendingPhone.isEmpty()) {
                String phone = pendingPhone;
                pendingPhone = "";
                client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), this::handleResponse);
            }
        } catch (Throwable e) {
            runOnUiThread(() -> toast("Не удалось настроить TDLib: " + e.getMessage()));
        }
    }

    private void showPhoneEntry() {
        content.removeAllViews();
        heading("Вход в Telegram", "Введи номер телефона, на который придёт код подтверждения.");
        phoneField = field("Номер телефона с кодом страны", InputType.TYPE_CLASS_PHONE);
        button("Отправить код", () -> {
            String phone = phoneField.getText().toString().trim();
            if (phone.isEmpty()) { toast("Введи номер телефона"); return; }
            client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), this::handleResponse);
            setStatus("Запрашиваем код подтверждения…");
        });
        setStatus("Ожидается номер телефона");
    }

    private void showCodeEntry() {
        content.removeAllViews();
        heading("Код подтверждения", "Введи код, который Telegram отправил в приложение или SMS.");
        codeField = field("Код Telegram", InputType.TYPE_CLASS_NUMBER);
        button("Подтвердить код", () -> {
            String code = codeField.getText().toString().trim();
            if (code.isEmpty()) { toast("Введи код"); return; }
            client.send(new TdApi.CheckAuthenticationCode(code), this::handleResponse);
            setStatus("Проверяем код…");
        });
        setStatus("Ожидается код Telegram");
    }

    private void showPasswordEntry() {
        content.removeAllViews();
        heading("Облачный пароль", "Для аккаунта включена двухэтапная проверка. Введи пароль Telegram.");
        passwordField = field("Пароль двухэтапной проверки", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        button("Войти", () -> {
            String password = passwordField.getText().toString();
            if (password.isEmpty()) { toast("Введи пароль"); return; }
            client.send(new TdApi.CheckAuthenticationPassword(password), this::handleResponse);
            setStatus("Проверяем пароль…");
        });
        setStatus("Ожидается пароль Telegram");
    }

    private void handleResponse(TdApi.Object object) {
        if (object instanceof TdApi.Error) {
            TdApi.Error error = (TdApi.Error) object;
            runOnUiThread(() -> {
                setStatus("Telegram: " + error.code + " — " + error.message);
                toast(error.message);
            });
        } else if (object instanceof TdApi.Ok) {
            runOnUiThread(() -> setStatus("Запрос принят"));
        } else if (object instanceof TdApi.Chats) {
            long[] ids = ((TdApi.Chats) object).chatIds;
            for (long id : ids) client.send(new TdApi.GetChat(id), result -> {
                if (result instanceof TdApi.Chat) {
                    synchronized (chats) {
                        TdApi.Chat c = (TdApi.Chat) result;
                        int i = indexOfChat(c.id);
                        if (i >= 0) chats.set(i, c); else chats.add(c);
                    }
                    runOnUiThread(this::renderChats);
                }
            });
        }
    }

    private int indexOfChat(long id) {
        for (int i = 0; i < chats.size(); i++) if (chats.get(i).id == id) return i;
        return -1;
    }

    private void showChats() {
        content.removeAllViews();
        heading("Сообщения", "Синхронизация чатов через TDLib.");
        renderChats();
        setStatus("Telegram подключён");
    }

    private void renderChats() {
        if (content == null || isFinishing()) return;
        if (content.getChildCount() == 0) heading("Сообщения", "Загружаем список диалогов…");
        synchronized (chats) {
            for (int i = 0; i < chats.size(); i++) {
                final TdApi.Chat chat = chats.get(i);
                String label = chat.title == null || chat.title.isEmpty() ? "Диалог" : chat.title;
                TextView row = text(label + "\n" + (chat.lastMessage == null ? "Открыть диалог" : messagePreview(chat.lastMessage)), 15, WHITE, true);
                row.setPadding(dp(15), dp(14), dp(15), dp(14));
                row.setBackground(rounded(PANEL, Color.rgb(34, 51, 76)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(9);
                content.addView(row, lp);
            }
        }
    }

    private String messagePreview(TdApi.Message message) {
        if (message.content instanceof TdApi.MessageText) return ((TdApi.MessageText) message.content).text.text;
        if (message.content instanceof TdApi.MessagePhoto) return "📷 Фото";
        if (message.content instanceof TdApi.MessageVideo) return "▶ Видео";
        if (message.content instanceof TdApi.MessageDocument) return "📎 Файл";
        return "Сообщение";
    }

    private void setStatus(String value) {
        if (status != null) status.setText(value);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
    }
}
