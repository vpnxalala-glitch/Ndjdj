package com.vich.assistant;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
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

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

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
        TextView dot = text("↻", 22, CYAN, true);
        dot.setContentDescription("Проверить обновления");
        dot.setPadding(dp(12), dp(4), dp(2), dp(4));
        dot.setOnClickListener(v -> checkForUpdates());
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
        button("Проверить обновления", this::checkForUpdates);
        button("Подключить Telegram", this::startAuthorization);
        TextView note = text("API ID и API Hash можно получить на my.telegram.org. Не отправляй их в чат и не добавляй в публичный репозиторий.", 12, MUTED, false);
        note.setLineSpacing(dp(3), 1f);
        content.addView(note);
        setStatus("Ожидание данных для подключения");
    }


    private static final String RELEASES_API = "https://api.github.com/repos/vpnxalala-glitch/Ndjdj/releases?per_page=10";

    private void checkForUpdates() {
        setStatus("Проверяем обновления…");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(RELEASES_API).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "VichGram-Android");
                int code = connection.getResponseCode();
                if (code != 200) throw new IllegalStateException("GitHub вернул HTTP " + code);
                StringBuilder json = new StringBuilder();
                try (InputStream in = connection.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) != -1) json.append(new String(buffer, 0, n, java.nio.charset.StandardCharsets.UTF_8));
                }
                JSONArray releases = new JSONArray(json.toString());
                JSONObject release = null;
                String apkUrl = null;
                for (int r = 0; r < releases.length(); r++) {
                    JSONObject candidate = releases.getJSONObject(r);
                    JSONArray assets = candidate.optJSONArray("assets");
                    if (assets == null) continue;
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        if (asset.optString("name").toLowerCase(Locale.ROOT).endsWith(".apk")) {
                            release = candidate;
                            apkUrl = asset.optString("browser_download_url");
                            break;
                        }
                    }
                    if (release != null) break;
                }
                if (release == null || apkUrl == null || apkUrl.isEmpty()) throw new IllegalStateException("В релизах пока нет APK");
                PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                long installedCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
                long latestCode = parseReleaseVersionCode(release.optString("tag_name"));
                String latestName = release.optString("tag_name", "новая версия");
                if (latestCode > 0 && latestCode <= installedCode) {
                    runOnUiThread(() -> {
                        setStatus("Установлена актуальная версия");
                        toast("Обновление не требуется");
                    });
                    return;
                }
                final String downloadUrl = apkUrl;
                runOnUiThread(() -> new android.app.AlertDialog.Builder(this)
                    .setTitle("Доступно обновление")
                    .setMessage("Найдена версия " + latestName + ". Скачать и установить обновление? Приложение и данные Telegram будут сохранены.")
                    .setNegativeButton("Позже", (d, w) -> setStatus("Обновление отложено"))
                    .setPositiveButton("Обновить", (d, w) -> downloadAndInstall(downloadUrl))
                    .show());
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setStatus("Не удалось проверить обновление");
                    toast("Ошибка обновления: " + e.getMessage());
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private long parseReleaseVersionCode(String tag) {
        try {
            String digits = tag.replaceFirst("^.*\\.", "").replaceAll("[^0-9]", "");
            return Long.parseLong(digits);
        } catch (Exception e) {
            return -1;
        }
    }

    private void downloadAndInstall(String url) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getPackageName()));
            startActivity(permission);
            toast("Разреши установку неизвестных приложений, затем снова нажми «Проверить обновления».");
            return;
        }
        setStatus("Скачиваем APK обновления…");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "VichGram-Android");
                File apk = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "vichgram-update.apk");
                File parent = apk.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Не удалось создать папку загрузки");
                try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(apk)) {
                    byte[] buffer = new byte[16384];
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                }
                if (apk.length() < 100000) throw new IllegalStateException("APK слишком маленький или скачался не полностью");
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
                runOnUiThread(() -> {
                    try {
                        Intent install = new Intent(Intent.ACTION_VIEW);
                        install.setDataAndType(uri, "application/vnd.android.package-archive");
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(install);
                        setStatus("APK скачан. Подтверди установку в системном окне.");
                    } catch (Exception e) {
                        setStatus("Не удалось открыть установщик");
                        toast("Ошибка установки: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setStatus("Ошибка загрузки обновления");
                    toast("Не удалось скачать APK: " + e.getMessage());
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void startAuthorization() {
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
            parametersSent = true;
            client.send(p, this::handleResponse);
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
            if (parametersSent && !pendingPhone.isEmpty()) {
                String phone = pendingPhone;
                pendingPhone = "";
                client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), this::handleResponse);
                runOnUiThread(() -> setStatus("Запрашиваем код подтверждения…"));
            } else {
                runOnUiThread(() -> setStatus("Запрос принят"));
            }
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
        if (content == null || isFinishing() || selectedChat != null) return;
        content.removeAllViews();
        heading("Сообщения", chats.isEmpty() ? "Ожидаем синхронизацию диалогов…" : "Выбери диалог, чтобы открыть историю.");
        synchronized (chats) {
            for (int i = 0; i < chats.size(); i++) {
                final TdApi.Chat chat = chats.get(i);
                String label = chat.title == null || chat.title.isEmpty() ? "Диалог" : chat.title;
                TextView row = text(label + "\\n" + (chat.lastMessage == null ? "Открыть диалог" : messagePreview(chat.lastMessage)), 15, WHITE, true);
                row.setPadding(dp(15), dp(14), dp(15), dp(14));
                row.setBackground(rounded(PANEL, Color.rgb(34, 51, 76)));
                row.setOnClickListener(v -> openChat(chat));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(9);
                content.addView(row, lp);
            }
        }
    }

    private void openChat(TdApi.Chat chat) {
        selectedChat = chat;
        synchronized (messages) { messages.clear(); }
        renderMessages();
        TdApi.GetChatHistory history = new TdApi.GetChatHistory();
        history.chatId = chat.id;
        history.fromMessageId = 0;
        history.offset = 0;
        history.limit = 50;
        history.onlyLocal = false;
        client.send(history, result -> {
            if (result instanceof TdApi.Messages) {
                TdApi.Message[] items = ((TdApi.Messages) result).messages;
                synchronized (messages) {
                    messages.clear();
                    for (int i = items.length - 1; i >= 0; i--) messages.add(items[i]);
                }
                runOnUiThread(this::renderMessages);
            } else if (result instanceof TdApi.Error) {
                runOnUiThread(() -> setStatus("Не удалось загрузить историю: " + ((TdApi.Error) result).message));
            }
        });
    }

    private void renderMessages() {
        if (selectedChat == null || content == null) return;
        content.removeAllViews();
        TextView back = text("‹  Все чаты", 15, CYAN, true);
        back.setPadding(0, dp(8), 0, dp(12));
        back.setOnClickListener(v -> {
            selectedChat = null;
            messageField = null;
            showChats();
        });
        content.addView(back);
        heading(selectedChat.title == null ? "Диалог" : selectedChat.title, "История и отправка сообщений через TDLib.");
        synchronized (messages) {
            for (TdApi.Message m : messages) {
                TextView bubble = text((m.isOutgoing ? "Вы" : "Сообщение") + "\\n" + messagePreview(m), 14, WHITE, false);
                bubble.setPadding(dp(13), dp(10), dp(13), dp(10));
                bubble.setBackground(rounded(m.isOutgoing ? Color.rgb(24, 69, 79) : PANEL, 0));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(7);
                content.addView(bubble, lp);
            }
        }
        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        messageField = new EditText(this);
        messageField.setHint("Сообщение…");
        messageField.setTextColor(WHITE);
        messageField.setHintTextColor(MUTED);
        messageField.setTextSize(14);
        messageField.setPadding(dp(12), dp(4), dp(12), dp(4));
        messageField.setBackground(rounded(PANEL, Color.rgb(44, 63, 91)));
        composer.addView(messageField, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button send = new Button(this);
        send.setText("➤");
        send.setAllCaps(false);
        send.setTextColor(BG);
        send.setBackground(rounded(CYAN, 0));
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(dp(54), dp(50));
        sendLp.leftMargin = dp(8);
        composer.addView(send, sendLp);
        send.setOnClickListener(v -> sendMessage());
        LinearLayout.LayoutParams composerLp = new LinearLayout.LayoutParams(-1, -2);
        composerLp.topMargin = dp(10);
        content.addView(composer, composerLp);
    }

    private void sendMessage() {
        if (client == null || selectedChat == null || messageField == null) return;
        String value = messageField.getText().toString().trim();
        if (value.isEmpty()) return;
        TdApi.SendMessage request = new TdApi.SendMessage();
        request.chatId = selectedChat.id;
        request.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(value, null), null, true);
        client.send(request, result -> {
            if (result instanceof TdApi.Error) runOnUiThread(() -> toast(((TdApi.Error) result).message));
        });
        messageField.setText("");
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
