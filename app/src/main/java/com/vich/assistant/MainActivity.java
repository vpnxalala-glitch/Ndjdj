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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
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
    private EditText phoneField, codeField, passwordField, chatSearchField;
    private LinearLayout chatRows;
    private ScrollView scrollView;
    private static final int PICK_WALLPAPER = 702;
    private static final String PREFS = "vichgram_appearance";
    private static final String PREF_GLASS = "glass_chats";
    private static final String PREF_WALLPAPER = "wallpaper_uri";
    private String chatQuery = "";
    private boolean parametersSent;
    private boolean waitingForPhoneNumber;
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
        initializeTdlibIfConfigured();
    }

    private boolean hasTelegramCredentials() {
        return BuildConfig.TELEGRAM_API_ID > 0
            && BuildConfig.TELEGRAM_API_HASH != null
            && !BuildConfig.TELEGRAM_API_HASH.trim().isEmpty();
    }

    private void initializeTdlibIfConfigured() {
        if (client != null || !hasTelegramCredentials()) return;
        try {
            Client.setLogMessageHandler(1, (level, message) -> android.util.Log.d("VichGram-TDLib", message));
            client = Client.create(this::onTdUpdate,
                e -> runOnUiThread(() -> setStatus("Ошибка TDLib: " + e.getMessage())),
                e -> runOnUiThread(() -> setStatus("Ошибка: " + e.getMessage())));
            setStatus("Подключаем Telegram…");
        } catch (Throwable e) {
            client = null;
            setStatus("Не удалось загрузить TDLib: " + e.getClass().getSimpleName());
            toast("Ошибка загрузки TDLib: " + e.getMessage());
        }
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
        TextView settings = text("⚙", 22, CYAN, true);
        settings.setContentDescription("Настройки оформления");
        settings.setPadding(dp(12), dp(4), dp(8), dp(4));
        settings.setOnClickListener(v -> openAppearanceSettings());
        header.addView(settings);
        TextView dot = text("↻", 22, CYAN, true);
        dot.setContentDescription("Проверить обновления");
        dot.setPadding(dp(12), dp(4), dp(2), dp(4));
        dot.setOnClickListener(v -> checkForUpdates());
        header.addView(dot);
        root.addView(header);

        status = text("Подготовка Telegram-клиента…", 12, MUTED, false);
        status.setPadding(dp(18), dp(10), dp(18), dp(10));
        root.addView(status);

        scrollView = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(24));
        scrollView.setFillViewport(true);
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
        applyWallpaper();

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
        heading("VichGram", "Твой мессенджер в аккуратном неоновом оформлении. Введи номер телефона — дальше проведёт Telegram.");
        phoneField = field("Номер телефона с кодом страны", InputType.TYPE_CLASS_PHONE);
        button("Получить код", this::startAuthorization);
        button("Проверить обновления", this::checkForUpdates);

        featureCard("01", "ЧАТЫ И СООБЩЕНИЯ", "Открывай диалоги, читай историю и отправляй текстовые сообщения.");
        featureCard("02", "ПОИСК ПО ДИАЛОГАМ", "Быстро находи нужный чат по имени или последнему сообщению.");
        featureCard("03", "ЗАЩИЩЁННЫЙ ВХОД", "Код приходит через Telegram или SMS. Пароль 2FA потребуется только если он включён в аккаунте.");

        TextView note = text("Технические параметры приложения настраиваются при сборке и не запрашиваются у пользователей.", 12, MUTED, false);
        note.setLineSpacing(dp(3), 1f);
        note.setPadding(0, dp(4), 0, dp(8));
        content.addView(note);
        setStatus("Введи номер телефона");
    }

    private void featureCard(String number, String title, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(PANEL, Color.rgb(38, 57, 82)));

        TextView index = text(number, 15, CYAN, true);
        index.setGravity(Gravity.CENTER);
        card.addView(index, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(text(title, 12, CYAN, true));
        TextView body = text(description, 12, WHITE, false);
        body.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, -2);
        bodyLp.topMargin = dp(4);
        copy.addView(body, bodyLp);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = dp(9);
        content.addView(card, cardLp);
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
        if (!hasTelegramCredentials()) {
            setStatus("Сборка не настроена для входа в Telegram");
            toast("Разработчику нужно один раз настроить VICHGRAM_TG_API_ID и VICHGRAM_TG_API_HASH в секретах GitHub. Пользователям эти данные вводить не придётся.");
            return;
        }

        String phone = phoneField == null ? "" : phoneField.getText().toString().trim();
        String normalizedPhone = phone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
        if (!normalizedPhone.matches("\\+?[0-9]{7,15}")) {
            toast("Введи действительный номер с кодом страны, например +491234567890");
            return;
        }

        pendingPhone = normalizedPhone;
        initializeTdlibIfConfigured();
        if (client == null) {
            pendingPhone = "";
            return;
        }
        if (waitingForPhoneNumber) sendPendingPhone();
    }

    private void sendPendingPhone() {
        if (client == null || pendingPhone == null || pendingPhone.isEmpty() || !waitingForPhoneNumber) return;
        String phone = pendingPhone;
        pendingPhone = "";
        waitingForPhoneNumber = false;
        client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), this::handleResponse);
        runOnUiThread(() -> setStatus("Запрашиваем код подтверждения…"));
    }

    private String pendingPhone = "";

    private void onTdUpdate(TdApi.Object update) {
        if (update instanceof TdApi.UpdateAuthorizationState) {
            TdApi.AuthorizationState state = ((TdApi.UpdateAuthorizationState) update).authorizationState;
            if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
                sendParameters();
            } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
                waitingForPhoneNumber = true;
                if (pendingPhone != null && !pendingPhone.isEmpty()) {
                    sendPendingPhone();
                } else {
                    runOnUiThread(() -> showPhoneEntry());
                }
            } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
                waitingForPhoneNumber = false;
                runOnUiThread(() -> showCodeEntry());
            } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
                runOnUiThread(() -> showPasswordEntry());
            } else if (state instanceof TdApi.AuthorizationStateReady) {
                waitingForPhoneNumber = false;
                pendingPhone = "";
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
                synchronized (messages) { messages.add(message); }
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
            if (BuildConfig.TELEGRAM_API_ID <= 0 || BuildConfig.TELEGRAM_API_HASH == null || BuildConfig.TELEGRAM_API_HASH.trim().isEmpty()) {
                throw new IllegalStateException("Telegram API credentials are not configured in this build");
            }
            p.apiId = BuildConfig.TELEGRAM_API_ID;
            p.apiHash = BuildConfig.TELEGRAM_API_HASH;
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
            String normalizedPhone = phone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
            if (!normalizedPhone.matches("\\+?[0-9]{7,15}")) {
                toast("Введи действительный номер с кодом страны");
                return;
            }
            waitingForPhoneNumber = false;
            client.send(new TdApi.SetAuthenticationPhoneNumber(normalizedPhone, null), this::handleResponse);
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
        chatSearchField = null;
        chatRows = null;
        chatQuery = "";
        renderChats();
        setStatus("Telegram подключён");
    }

    private void renderChats() {
        if (content == null || isFinishing() || selectedChat != null) return;
        if (chatSearchField == null || chatRows == null) {
            content.removeAllViews();
            heading("Твои чаты", "Поиск по имени диалога или последнему сообщению.");

            chatSearchField = new EditText(this);
            chatSearchField.setSingleLine(true);
            chatSearchField.setTextSize(14);
            chatSearchField.setTextColor(WHITE);
            chatSearchField.setHintTextColor(MUTED);
            chatSearchField.setHint("⌕  Поиск по чатам");
            chatSearchField.setInputType(InputType.TYPE_CLASS_TEXT);
            chatSearchField.setPadding(dp(14), dp(8), dp(14), dp(8));
            chatSearchField.setBackground(rounded(Color.rgb(23, 34, 55), Color.rgb(44, 63, 91)));
            LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(50));
            searchLp.bottomMargin = dp(12);
            content.addView(chatSearchField, searchLp);

            chatRows = new LinearLayout(this);
            chatRows.setOrientation(LinearLayout.VERTICAL);
            content.addView(chatRows, new LinearLayout.LayoutParams(-1, -2));

            chatSearchField.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable s) {
                    chatQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                    renderChatRows();
                }
            });
        }
        renderChatRows();
    }

    private android.graphics.drawable.Drawable chatRowBackground() {
        GradientDrawable shape = new GradientDrawable();
        boolean glass = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_GLASS, false);
        shape.setCornerRadius(dp(glass ? 24 : 16));
        shape.setColor(glass ? Color.argb(150, 17, 25, 43) : PANEL);
        shape.setStroke(dp(glass ? 1 : 1), glass ? Color.argb(210, 44, 218, 220) : Color.rgb(34, 51, 76));
        return shape;
    }

    private void renderChatRows() {
        if (chatRows == null) return;
        chatRows.removeAllViews();
        int shown = 0;
        synchronized (chats) {
            for (int i = 0; i < chats.size(); i++) {
                final TdApi.Chat chat = chats.get(i);
                String label = chat.title == null || chat.title.trim().isEmpty() ? "Диалог" : chat.title;
                String preview = chat.lastMessage == null ? "Открыть диалог" : messagePreview(chat.lastMessage);
                if (!chatQuery.isEmpty()
                    && !label.toLowerCase(Locale.ROOT).contains(chatQuery)
                    && !preview.toLowerCase(Locale.ROOT).contains(chatQuery)) {
                    continue;
                }
                String unread = chat.unreadCount > 0 ? chat.unreadCount + " новых · " : "";
                TextView row = text(label + "\n" + unread + preview, 15, WHITE, true);
                row.setMaxLines(3);
                row.setPadding(dp(15), dp(14), dp(15), dp(14));
                row.setBackground(chatRowBackground());
                row.setOnClickListener(v -> openChat(chat));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(9);
                chatRows.addView(row, lp);
                shown++;
            }
        }
        if (shown == 0) {
            String message = chats.isEmpty()
                ? "Пока нет загруженных диалогов. Проверь соединение и обнови список."
                : "По этому запросу ничего не найдено.";
            TextView empty = text(message, 13, MUTED, false);
            empty.setLineSpacing(dp(3), 1f);
            empty.setPadding(dp(6), dp(14), dp(6), dp(14));
            chatRows.addView(empty);
            if (chats.isEmpty()) {
                Button refresh = new Button(this);
                refresh.setText("Обновить список чатов");
                refresh.setAllCaps(false);
                refresh.setTextColor(BG);
                refresh.setBackground(rounded(CYAN, 0));
                refresh.setOnClickListener(v -> {
                    if (client != null) client.send(new TdApi.GetChats(new TdApi.ChatListMain(), 100), this::handleResponse);
                    setStatus("Обновляем список чатов…");
                });
                LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(-1, dp(48));
                refreshLp.topMargin = dp(8);
                chatRows.addView(refresh, refreshLp);
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
            chatSearchField = null;
            chatRows = null;
            showChats();
        });
        content.addView(back);
        heading(selectedChat.title == null ? "Диалог" : selectedChat.title, "История и отправка сообщений через TDLib.");
        synchronized (messages) {
            for (TdApi.Message m : messages) {
                TextView bubble = text((m.isOutgoing ? "Вы" : "Сообщение") + "\n" + messagePreview(m), 14, WHITE, false);
                bubble.setPadding(dp(13), dp(10), dp(13), dp(10));
                bubble.setBackground(rounded(m.isOutgoing ? Color.rgb(24, 69, 79) : PANEL, 0));
                bubble.setGravity(m.isOutgoing ? Gravity.END : Gravity.START);
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

    private void openAppearanceSettings() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(8), dp(20), dp(4));
        Switch glassSwitch = new Switch(this);
        glassSwitch.setText("Стеклянные чаты");
        glassSwitch.setTextColor(WHITE);
        glassSwitch.setTextSize(15);
        glassSwitch.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_GLASS, false));
        glassSwitch.setPadding(0, dp(8), 0, dp(8));
        panel.addView(glassSwitch);
        TextView hint = text("Включи, чтобы карточки диалогов стали полупрозрачными, с бирюзовой обводкой и пузырчатыми углами. Выключи — вернётся обычный вид.", 12, MUTED, false);
        hint.setLineSpacing(dp(2), 1f);
        panel.addView(hint);
        new android.app.AlertDialog.Builder(this)
            .setTitle("Оформление VichGram")
            .setView(panel)
            .setPositiveButton("Обои чатов", (dialog, which) -> pickWallpaper())
            .setNeutralButton("Убрать обои", (dialog, which) -> clearWallpaper())
            .setNegativeButton("Готово", (dialog, which) -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREF_GLASS, glassSwitch.isChecked()).apply();
                renderChatRows();
                setStatus(glassSwitch.isChecked() ? "Стеклянные чаты включены" : "Обычный вид чатов");
            })
            .show();
    }

    private void pickWallpaper() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_WALLPAPER);
        } catch (Exception e) {
            toast("Не удалось открыть выбор обоев: " + e.getMessage());
        }
    }

    private void clearWallpaper() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(PREF_WALLPAPER).apply();
        if (scrollView != null) scrollView.setBackgroundColor(BG);
        setStatus("Обои убраны");
        toast("Установлен стандартный фон");
    }

    private void applyWallpaper() {
        if (scrollView == null) return;
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_WALLPAPER, "");
        if (saved == null || saved.isEmpty()) {
            scrollView.setBackgroundColor(BG);
            return;
        }
        try (InputStream in = getContentResolver().openInputStream(Uri.parse(saved))) {
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap != null) {
                BitmapDrawable wallpaper = new BitmapDrawable(getResources(), bitmap);
                wallpaper.setAlpha(105);
                wallpaper.setGravity(Gravity.FILL);
                scrollView.setBackground(wallpaper);
                content.setBackgroundColor(Color.TRANSPARENT);
            }
        } catch (Exception e) {
            android.util.Log.w("VichGram", "Не удалось применить обои", e);
            scrollView.setBackgroundColor(BG);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_WALLPAPER || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) { }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_WALLPAPER, uri.toString()).apply();
        applyWallpaper();
        setStatus("Обои применены");
        toast("Обои чатов обновлены");
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
