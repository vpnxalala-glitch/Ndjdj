package com.vich.assistant;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.text.TextWatcher;
import android.text.Editable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FloatingService extends Service {
    private WindowManager wm; private View panel; private WindowManager.LayoutParams params;
    private FrameLayout content; private WebView web; private int sw,sh; private float lastX,lastY,downX,downY;
    private String activeTab="menu";
    private int bubbleDp=58, panelWidthPct=92, panelHeightPct=76;
    private final int bg=Color.rgb(17,23,39), surface=Color.rgb(31,41,61), accent=Color.rgb(35,190,170);
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null && "com.vich.assistant.EXECUTE_COMMAND".equals(intent.getAction())){
            String command=intent.getStringExtra("command");
            if(panel==null) { activeTab="commands"; showPanel(); }
            else showTab("commands");
            final String spoken=command==null?"":command;
            content.post(()->{ if("commands".equals(activeTab)) runCommand(spoken, findCommandResult()); });
            return START_STICKY;
        }
        String tab=intent==null?null:intent.getStringExtra("tab");
        if(tab!=null)activeTab=tab;
        if(panel==null)showPanel();else showTab(activeTab);
        return START_STICKY;
    }
    private TextView findCommandResult(){
        if(content==null)return text("",13,Color.LTGRAY);
        TextView result=findResultIn(content);return result==null?text("Команда получена",13,Color.LTGRAY):result;
    }
    private TextView findResultIn(android.view.ViewGroup group){
        for(int i=0;i<group.getChildCount();i++){
            View v=group.getChildAt(i);
            if(v instanceof TextView && "vichgram_command_result".equals(v.getTag()))return (TextView)v;
            if(v instanceof android.view.ViewGroup){TextView found=findResultIn((android.view.ViewGroup)v);if(found!=null)return found;}
        }
        return null;
    }
    private void showPanel(){
        wm=(WindowManager)getSystemService(WINDOW_SERVICE); sw=getResources().getDisplayMetrics().widthPixels; sh=getResources().getDisplayMetrics().heightPixels;
        android.content.SharedPreferences prefs=getSharedPreferences("vichgram_ui",MODE_PRIVATE);bubbleDp=prefs.getInt("bubble_size",58);panelWidthPct=prefs.getInt("panel_width",92);panelHeightPct=prefs.getInt("panel_height",76);
        params=new WindowManager.LayoutParams((int)(sw*panelWidthPct/100f),(int)(sh*panelHeightPct/100f),Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,
          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;params.x=(sw-params.width)/2;params.y=sh/10;params.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(8),dp(8),dp(8),dp(6));root.setBackground(shape(bg,22));root.setClipToOutline(true);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(dp(10),dp(3),dp(3),dp(3));head.setBackground(shape(surface,14));
        TextView brand=text("VichGram • NEON",15,Color.WHITE);brand.setTypeface(null,Typeface.BOLD);head.addView(brand,new LinearLayout.LayoutParams(0,dp(42),1));
        head.addView(btn("⌂",()->showTab("menu")),new LinearLayout.LayoutParams(dp(42),dp(42)));
        head.addView(btn("—",()->collapse()),new LinearLayout.LayoutParams(dp(42),dp(42)));
        head.addView(btn("×",()->stopSelf()),new LinearLayout.LayoutParams(dp(42),dp(42)));head.setOnTouchListener(this::drag);
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);
        String[][] tabs={{"Меню","menu"},{"Telegram","telegram"},{"ИИ","ai"},{"Команды","commands"},{"Веб","browser"}};
        for(String[] t:tabs){TextView b=btn(t[0],()->showTab(t[1]));nav.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));}
        content=new FrameLayout(this);content.setBackground(shape(surface,14));content.setClipToOutline(true);
        root.addView(head);root.addView(nav,new LinearLayout.LayoutParams(-1,dp(46)));root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=text("Перетаскивай за заголовок • сверни в пузырёк кнопкой —",11,Color.LTGRAY);hint.setGravity(Gravity.CENTER);root.addView(hint,new LinearLayout.LayoutParams(-1,dp(24)));
        panel=root;wm.addView(panel,params);showTab(activeTab);
    }
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private TextView btn(String s,Runnable action){TextView t=text(s,13,Color.WHITE);t.setGravity(Gravity.CENTER);t.setPadding(dp(4),dp(4),dp(4),dp(4));t.setBackground(shape(surface,11));t.setOnClickListener(v->action.run());return t;}
    private void showTab(String tab){
        activeTab=tab;if(web!=null){try{web.stopLoading();web.destroy();}catch(Exception ignored){}web=null;}
        if(content==null)return;content.removeAllViews();
        if("menu".equals(tab))showMenu();else if("sizes".equals(tab))showSizes();else if("osint".equals(tab))showOpenSearch();else if("ai".equals(tab))showWeb(true);else if("browser".equals(tab))showWeb(false);else if("telegram".equals(tab))showTelegram();else if("apps".equals(tab))showApps();else if("commands".equals(tab))showCommands();else showTools();
    }
    private void showMenu(){
        LinearLayout list=column();list.addView(text("Чем займёмся?",24,Color.WHITE));list.addView(text("Выбери функцию — браузер сам не откроется.",14,Color.LTGRAY));
        addMenuCard(list,"🤖 ИИ-ассистент","Задать вопрос через ChatGPT","ai");
        addMenuCard(list,"🎯 Командный помощник","Напиши «открой YouTube» или «найди рецепт»","commands");
        addMenuCard(list,"📱 Запуск приложений","Поиск установленных приложений","apps");
        addMenuCard(list,"✈ Telegram Web","Официальный веб-клиент Telegram: войди по QR-коду или номеру телефона","telegram");
        addMenuCard(list,"🌐 Браузер","Сайты и поиск","browser");
        addMenuCard(list,"🧰 Быстрые настройки","Wi-Fi, Bluetooth, звук и настройки Android","tools");
        addMenuCard(list,"📐 Размеры пузыря и окна","Настрой ширину и высоту окна, размер плавающего пузыря","sizes");
        addMenuCard(list,"🔎 Поиск по открытым источникам","Публичные ссылки и сведения по Telegram username или номеру","osint");
        content.addView(new ScrollView(this){{addView(list);}},new FrameLayout.LayoutParams(-1,-1));
    }
    private void showSizes(){
        LinearLayout box=column();
        box.addView(text("Размеры окна и пузыря",22,Color.WHITE));
        box.addView(text("Изменения сохраняются и применяются к следующему открытию окна.",13,Color.LTGRAY));
        addSizeControl(box,"Размер пузыря",28,100,bubbleDp,"bubble_size","dp",v->{bubbleDp=v;});
        addSizeControl(box,"Ширина окна",55,100,panelWidthPct,"panel_width","%",v->{panelWidthPct=v;});
        addSizeControl(box,"Высота окна",35,95,panelHeightPct,"panel_height","%",v->{panelHeightPct=v;});
        box.addView(btn("Применить размер окна",()->{
            getSharedPreferences("vichgram_ui",MODE_PRIVATE).edit().putInt("bubble_size",bubbleDp).putInt("panel_width",panelWidthPct).putInt("panel_height",panelHeightPct).apply();
            if(params!=null){params.width=(int)(sw*panelWidthPct/100f);params.height=(int)(sh*panelHeightPct/100f);try{wm.updateViewLayout(panel,params);}catch(Exception ignored){}}
            Toast.makeText(this,"Размеры сохранены",Toast.LENGTH_SHORT).show();
        }));
        content.addView(new ScrollView(this){{addView(box);}},new FrameLayout.LayoutParams(-1,-1));
    }
    private void showOpenSearch(){
        LinearLayout box=column();
        box.addView(text("Проверка номера телефона",21,Color.WHITE));
        box.addView(text("Страна и примерный регион по телефонному коду. Оператор показывается только как исходный диапазон: после переноса номера он может быть другим.",13,Color.LTGRAY));
        EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);input.setTextColor(Color.WHITE);input.setHintTextColor(Color.LTGRAY);input.setHint("+7 918 123-45-67");input.setBackground(shape(bg,14));input.setPadding(dp(12),0,dp(12),0);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(50));ip.topMargin=dp(12);box.addView(input,ip);
        TextView result=text("Введи номер и нажми «Проверить».",14,Color.WHITE);result.setPadding(0,dp(12),0,dp(12));box.addView(result);
        LinearLayout links=new LinearLayout(this);links.setOrientation(LinearLayout.VERTICAL);box.addView(links);
        box.addView(btn("Проверить номер",()->{
            String digits=input.getText().toString().replaceAll("[^0-9]","");
            if(digits.startsWith("00"))digits=digits.substring(2);
            if(digits.startsWith("8")&&digits.length()==11)digits="7"+digits.substring(1);
            if(digits.length()<7||digits.length()>15){result.setText("Неверный формат. Укажи от 7 до 15 цифр, желательно с кодом страны.");links.removeAllViews();return;}
            String country="Страна по коду не определена",region="Регион не определён",operator="Текущий оператор не подтверждён";
            if(digits.startsWith("7")){
                country="Россия или Казахстан (+7)";String n=digits.substring(1);
                if(n.length()>=3){String[] info=ruPrefixInfo(n.substring(0,3));if(info!=null){region=info[0];operator="Исходный диапазон: "+info[1]+" (может измениться после переноса номера)";}else if(n.startsWith("9"))region="Мобильный номер РФ; регион по этому префиксу точно не определён";}
                if(n.startsWith("6")||n.startsWith("7")){country="Казахстан (+7)";region="Регион требует проверки по актуальной базе нумерации";}
            }else if(digits.startsWith("33"))country="Франция (+33)";
            else if(digits.startsWith("1"))country="США/Канада и другие страны NANP (+1)";
            else if(digits.startsWith("44"))country="Великобритания (+44)";
            else if(digits.startsWith("49"))country="Германия (+49)";
            else if(digits.startsWith("380"))country="Украина (+380)";
            else if(digits.startsWith("375"))country="Беларусь (+375)";
            else if(digits.startsWith("998"))country="Узбекистан (+998)";
            else if(digits.startsWith("994"))country="Азербайджан (+994)";
            else if(digits.startsWith("90"))country="Турция (+90)";
            else if(digits.startsWith("971"))country="ОАЭ (+971)";
            else if(digits.startsWith("91"))country="Индия (+91)";
            else if(digits.startsWith("86"))country="Китай (+86)";
            String number="+"+digits;
            result.setText("Номер: "+number+"\\n"+country+"\\n"+region+"\\n"+operator+"\\n\\nСсылки ведут в сервисы или поиск и не гарантируют, что аккаунт существует.");
            links.removeAllViews();
            addSearchLink(links,"Telegram • открыть по номеру","https://t.me/+"+digits);
            addSearchLink(links,"WhatsApp • открыть чат","https://wa.me/"+digits);
            addSearchLink(links,"Viber • открыть чат","viber://chat?number=%2B"+digits);
            addSearchLink(links,"TikTok • поиск","https://www.tiktok.com/search?q="+Uri.encode(number));
            addSearchLink(links,"YouTube • поиск","https://www.youtube.com/results?search_query="+Uri.encode(number));
            addSearchLink(links,"Google • поиск номера","https://www.google.com/search?q="+Uri.encode(number));
            addSearchLink(links,"Bing • поиск номера","https://www.bing.com/search?q="+Uri.encode(number));
            addSearchLink(links,"DuckDuckGo • поиск номера","https://duckduckgo.com/?q="+Uri.encode(number));
        }));
        ScrollView scroll=new ScrollView(this);scroll.setBackground(shape(surface,14));scroll.setClipToOutline(true);scroll.addView(box);content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
    }
    private String[] ruPrefixInfo(String prefix){
        switch(prefix){
            case "903":case "905":return new String[]{"Москва и некоторые другие регионы","исторически Билайн"};
            case "916":return new String[]{"Москва и Московская область","исторически МТС"};
            case "925":case "926":case "936":return new String[]{"Москва и Московская область","исторически МегаФон"};
            case "918":return new String[]{"Краснодарский край и Адыгея (часть диапазона)","исторически МТС"};
            case "912":return new String[]{"Урал (часть диапазона)","исторически МТС"};
            case "913":return new String[]{"Сибирь (часть диапазона)","исторически МТС"};
            case "914":return new String[]{"Дальний Восток (часть диапазона)","исторически МТС"};
            case "915":return new String[]{"Центральная Россия (часть диапазона)","исторически МТС"};
            case "917":return new String[]{"Поволжье и некоторые другие регионы","исторически МТС"};
            case "920":return new String[]{"Центральная Россия и Поволжье","исторически МегаФон"};
            case "921":return new String[]{"Северо-Запад России","исторически МегаФон"};
            case "922":return new String[]{"Урал","исторически МегаФон"};
            case "923":return new String[]{"Сибирь","исторически МегаФон"};
            case "924":return new String[]{"Дальний Восток","исторически МегаФон"};
            case "927":return new String[]{"Поволжье","исторически МегаФон"};
            case "928":return new String[]{"Северный Кавказ и юг России","исторически МегаФон"};
            case "930":return new String[]{"Центральная Россия","исторически МегаФон"};
            case "931":return new String[]{"Северо-Запад России","исторически МегаФон"};
            case "932":return new String[]{"Урал","исторически МегаФон"};
            case "933":return new String[]{"Сибирь","исторически МегаФон"};
            case "934":return new String[]{"Дальний Восток","исторически МегаФон"};
            case "937":return new String[]{"Поволжье","исторически МегаФон"};
            case "938":return new String[]{"Юг России и Северный Кавказ","исторически МегаФон"};
            default:return null;
        }
    }
    private void addSearchLink(LinearLayout box,String title,String url){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(42));lp.topMargin=dp(6);
        box.addView(btn(title,()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){Toast.makeText(this,"Не удалось открыть ссылку",Toast.LENGTH_SHORT).show();}}),lp);
    }
    private interface IntSetter{void set(int value);}
    private void addSizeControl(LinearLayout parent,String title,int min,int max,int initial,String key,String unit,IntSetter setter){
        TextView label=text(title+": "+initial+unit,15,Color.WHITE);parent.addView(label);
        SeekBar seek=new SeekBar(this);seek.setMax(max-min);seek.setProgress(Math.max(0,Math.min(max-min,initial-min)));parent.addView(seek,new LinearLayout.LayoutParams(-1,dp(42)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){int val=min+progress;label.setText(title+": "+val+unit);setter.set(val);}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(12),dp(12),dp(12),dp(12));return l;}
    private void addMenuCard(LinearLayout parent,String title,String desc,String tab){
        LinearLayout c=column();c.setBackground(shape(bg,13));TextView h=text(title,17,Color.WHITE);h.setTypeface(null,Typeface.BOLD);c.addView(h);
        TextView d=text(desc,13,Color.LTGRAY);d.setPadding(0,dp(5),0,dp(8));c.addView(d);c.setOnClickListener(v->showTab(tab));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);parent.addView(c,lp);
    }
    private void showTelegram(){
        LinearLayout box=column();
        box.addView(text("Telegram Web",20,Color.WHITE));
        box.addView(text("Официальный веб-интерфейс Telegram. Вход выполняется внутри WebView; не сообщай коды входа другим людям.",12,Color.LTGRAY));
        web=new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.setBackgroundColor(Color.WHITE);
        box.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        content.addView(box,new FrameLayout.LayoutParams(-1,-1));
        web.loadUrl("https://web.telegram.org/a/");
    }
    private void showWeb(boolean ai){
        LinearLayout box=column();LinearLayout row=new LinearLayout(this);
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(Color.WHITE);input.setHintTextColor(Color.LTGRAY);input.setTextSize(14);
        input.setHint(ai?"Задай вопрос ИИ":"Адрес сайта или поисковый запрос");input.setBackground(shape(bg,10));input.setPadding(dp(10),0,dp(10),0);
        row.addView(input,new LinearLayout.LayoutParams(0,dp(46),1));
        row.addView(btn(ai?"Спросить":"Открыть",()->navigate(input.getText().toString(),ai)),new LinearLayout.LayoutParams(dp(82),dp(46)));box.addView(row);
        web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.setWebViewClient(new WebViewClient());web.setWebChromeClient(new WebChromeClient());web.setBackground(shape(Color.WHITE,14));web.setClipToOutline(true);
        box.addView(web,new LinearLayout.LayoutParams(-1,0,1));content.addView(box,new FrameLayout.LayoutParams(-1,-1));
        input.setOnEditorActionListener((v,id,event)->{if(id==EditorInfo.IME_ACTION_GO||id==EditorInfo.IME_ACTION_DONE){navigate(input.getText().toString(),ai);return true;}return false;});
        if(ai){input.setText("https://chatgpt.com");web.loadUrl("https://chatgpt.com");}else web.loadUrl("https://www.google.com");
    }
    private void navigate(String raw,boolean ai){
        String q=raw==null?"":raw.trim();if(q.isEmpty())return;
        try{String url;if(q.startsWith("https://")||q.startsWith("http://"))url=q;
        else if(q.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(/.*)?$"))url="https://"+q;
        else url=(ai?"https://chatgpt.com/?q=":"https://www.google.com/search?q=")+URLEncoder.encode(q,"UTF-8");
        if(web!=null)web.loadUrl(url);else startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }catch(Exception e){Toast.makeText(this,"Не удалось открыть запрос",Toast.LENGTH_SHORT).show();}
    }
    private List<String[]> getApps(){
        List<String[]> apps=new ArrayList<>();
        try{for(android.content.pm.ApplicationInfo a:getPackageManager().getInstalledApplications(0)){
            Intent launch=getPackageManager().getLaunchIntentForPackage(a.packageName);
            if(launch!=null&&!a.packageName.equals(getPackageName()))apps.add(new String[]{a.packageName,getPackageManager().getApplicationLabel(a).toString()});
        }}catch(Exception ignored){}
        Collections.sort(apps,(a,b)->a[1].compareToIgnoreCase(b[1]));return apps;
    }
    private void showApps(){
        LinearLayout box=column();EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Название приложения…");search.setTextColor(Color.WHITE);search.setHintTextColor(Color.LTGRAY);search.setBackground(shape(bg,10));box.addView(search,new LinearLayout.LayoutParams(-1,dp(46)));
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(this);scroll.addView(results);box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content.addView(box,new FrameLayout.LayoutParams(-1,-1));
        List<String[]> apps=getApps();Runnable render=()->{results.removeAllViews();String q=search.getText().toString().toLowerCase(Locale.ROOT);int count=0;
            for(String[] a:apps)if(a[1].toLowerCase(Locale.ROOT).contains(q)||a[0].toLowerCase(Locale.ROOT).contains(q)){TextView item=text("📲  "+a[1]+"\n"+a[0],14,Color.WHITE);item.setPadding(dp(12),dp(12),dp(8),dp(12));item.setBackground(shape(bg,10));item.setOnClickListener(v->openApp(a[0],a[1]));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(5);results.addView(item,lp);if(++count>=100)break;}
            if(count==0)results.addView(text(apps.isEmpty()?"Список приложений недоступен":"Ничего не найдено",14,Color.LTGRAY));};
        render.run();search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){render.run();}public void afterTextChanged(Editable e){}});
    }
    private boolean launchMatching(String[] words){
        for(String[] app:getApps())for(String word:words)if(app[1].toLowerCase(new Locale("ru")).contains(word)||app[0].toLowerCase(Locale.ROOT).contains(word)){openApp(app[0],app[1]);return true;}
        return false;
    }
    private void openApp(String pkg,String label){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}else Toast.makeText(this,"Нет команды запуска для "+label,Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Не удалось открыть "+label,Toast.LENGTH_SHORT).show();}}
    private void showCommands(){
        LinearLayout box=column();box.addView(text("Скажи помощнику, что сделать",20,Color.WHITE));
        box.addView(text("Примеры: открой YouTube • открой Telegram • найди погоду в Париже • открой настройки Wi-Fi • открой браузер",13,Color.LTGRAY));
        EditText command=new EditText(this);command.setHint("Введи команду…");command.setTextColor(Color.WHITE);command.setHintTextColor(Color.LTGRAY);command.setSingleLine(true);command.setBackground(shape(bg,10));command.setPadding(dp(12),0,dp(12),0);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(48));cp.topMargin=dp(12);box.addView(command,cp);
        TextView result=text("Команды выполняются на устройстве; для запуска приложения оно должно быть установлено.",13,Color.LTGRAY);result.setTag("vichgram_command_result");result.setPadding(0,dp(12),0,dp(12));box.addView(result);
        box.addView(btn("Выполнить команду",()->runCommand(command.getText().toString(),result)));
        LinearLayout.LayoutParams voiceLp=new LinearLayout.LayoutParams(-1,dp(44));voiceLp.topMargin=dp(7);
        box.addView(btn("🎙 Сказать команду голосом",()->{try{startActivity(new Intent(this,VoiceCommandActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){result.setText("Голосовой ввод недоступен на устройстве.");}}),voiceLp);
        String[] examples={"Открой YouTube","Открой настройки Wi-Fi","Найди погоду","Открой Bluetooth","Открой будильник","Открой календарь","Открой местоположение"};
        for(String ex:examples){TextView e=btn("↗  "+ex,()->{command.setText(ex);runCommand(ex,result);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(40));lp.topMargin=dp(7);box.addView(e,lp);}
        content.addView(new ScrollView(this){{addView(box);}},new FrameLayout.LayoutParams(-1,-1));
    }
    private void runCommand(String raw,TextView result){
        String q=raw==null?"":raw.trim();String l=q.toLowerCase(new Locale("ru"));
        if(q.isEmpty()){result.setText("Сначала введи команду.");return;}
        if(l.contains("настрой")&&(l.contains("wi")||l.contains("вай"))){openSettings(new Intent(Settings.ACTION_WIFI_SETTINGS),result,"Открываю настройки Wi-Fi");return;}
        if(l.contains("камер")){if(!launchMatching(new String[]{"камера","camera"})){result.setText("Камера не найдена среди приложений.");}return;}
        if(l.contains("калькулятор")||l.contains("калькуля")){if(!launchMatching(new String[]{"калькулятор","calculator"})){result.setText("Калькулятор не найден среди приложений.");}return;}
        if(l.contains("карты")||l.contains("навиг")){if(!launchMatching(new String[]{"карты","maps","google maps"})){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="+URLEncoder.encode(q,"UTF-8"))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){result.setText("Карты недоступны.");}}return;}
        if(l.contains("позвони")||l.contains("телефон")||l.contains("набор номера")){try{startActivity(new Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));result.setText("Открываю приложение телефона.");}catch(Exception e){result.setText("Приложение телефона недоступно.");}return;}
        if(l.contains("будильник")||l.contains("поставь будильник")||l.contains("установи будильник")){try{startActivity(new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));result.setText("Открываю будильники. Время можно подтвердить в приложении часов.");}catch(Exception e){result.setText("Приложение часов недоступно.");}return;}
        if(l.contains("календар")){try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));result.setText("Открываю календарь.");}catch(Exception e){result.setText("Календарь не найден.");}return;}
        if(l.contains("загрузк")||l.contains("скачан")){if(!launchMatching(new String[]{"загрузки","downloads","файлы","files"})){try{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.setType("*/*");pick.addCategory(Intent.CATEGORY_OPENABLE);startActivity(pick.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){result.setText("Открой приложение «Файлы», чтобы посмотреть загрузки.");}}return;}
        if(l.contains("местополож")||l.contains("геолокац")||l.contains("gps")){openSettings(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),result,"Открываю настройки местоположения");return;}
        if(l.contains("приложен")&&l.contains("настрой")){openSettings(new Intent(Settings.ACTION_APPLICATION_SETTINGS),result,"Открываю настройки приложений");return;}
        if(l.contains("bluetooth")||l.contains("блютуз")){openSettings(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),result,"Открываю настройки Bluetooth");return;}
        if(l.contains("звук")||l.contains("громк")){openSettings(new Intent(Settings.ACTION_SOUND_SETTINGS),result,"Открываю настройки звука");return;}
        if(l.contains("настрой")){openSettings(new Intent(Settings.ACTION_SETTINGS),result,"Открываю настройки Android");return;}
        if(l.contains("браузер")||l.contains("интернет")){showTab("browser");return;}
        if(l.equals("меню")||l.contains("главное меню")){showTab("menu");return;}
        if(l.contains("ии")||l.contains("чатгпт")||l.contains("chatgpt")){showTab("ai");return;}
        if(l.startsWith("найди ")||l.startsWith("поищи ")||l.startsWith("поиск ")){String term=q.replaceFirst("(?i)^(найди|поищи|поиск)\\s*","");showTab("browser");navigate(term,false);return;}
        String target=q.replaceFirst("(?i)^(пожалуйста\\s+)?(открой|запусти|включи|открыть|запустить)\\s+","").trim();
        if(target.equalsIgnoreCase(q)){target=q;}
        if(target.isEmpty()){result.setText("Напиши, например: «открой YouTube» или «найди погоду».");return;}
        List<String[]> apps=getApps();String t=target.toLowerCase(new Locale("ru"));
        for(String[] a:apps){String label=a[1].toLowerCase(new Locale("ru"));String pkg=a[0].toLowerCase(Locale.ROOT);
            if(label.equals(t)||pkg.equals(t)||pkg.endsWith("."+t)||label.contains(t)){openApp(a[0],a[1]);result.setText("Запускаю "+a[1]);return;}}
        showTab("browser");navigate(target,false);Toast.makeText(this,"Приложение не найдено — ищу в интернете",Toast.LENGTH_LONG).show();
    }
    private void openSettings(Intent i,TextView result,String message){try{i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);result.setText(message);}catch(Exception e){result.setText("Не удалось открыть системные настройки.");}}
    private void showTools(){
        LinearLayout box=column();box.addView(text("Быстрые инструменты Android",20,Color.WHITE));
        addTool(box,"Wi-Fi","Открыть настройки Wi-Fi",Settings.ACTION_WIFI_SETTINGS);
        addTool(box,"Bluetooth","Открыть настройки Bluetooth",Settings.ACTION_BLUETOOTH_SETTINGS);
        addTool(box,"Звук","Громкость и режим звука",Settings.ACTION_SOUND_SETTINGS);
        addTool(box,"Дисплей","Яркость и экран",Settings.ACTION_DISPLAY_SETTINGS);
        addTool(box,"Приложения","Управление приложениями",Settings.ACTION_APPLICATION_SETTINGS);
        addTool(box,"Все настройки","Системные настройки",Settings.ACTION_SETTINGS);
        content.addView(new ScrollView(this){{addView(box);}},new FrameLayout.LayoutParams(-1,-1));
    }
    private void addTool(LinearLayout box,String title,String desc,String action){
        LinearLayout c=column();c.setBackground(shape(bg,12));c.addView(text(title,16,Color.WHITE));c.addView(text(desc,12,Color.LTGRAY));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(7);box.addView(c,lp);c.setOnClickListener(v->{try{startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){Toast.makeText(this,"Недоступно на этом устройстве",Toast.LENGTH_SHORT).show();}});
    }
    private boolean drag(View v,MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getRawX();lastY=e.getRawY();return true;}
        if(e.getAction()==MotionEvent.ACTION_MOVE){params.x+=(int)(e.getRawX()-lastX);params.y+=(int)(e.getRawY()-lastY);lastX=e.getRawX();lastY=e.getRawY();update();return true;}
        return e.getAction()==MotionEvent.ACTION_UP;
    }
    private void update(){try{if(panel!=null)wm.updateViewLayout(panel,params);}catch(Exception ignored){}}
    private void collapse(){
        if(panel!=null)try{wm.removeView(panel);}catch(Exception ignored){}
        if(web!=null){web.destroy();web=null;}
        TextView bubble=text("V",22,Color.WHITE);bubble.setGravity(Gravity.CENTER);bubble.setTypeface(null,Typeface.BOLD);bubble.setBackground(shape(accent,50));bubble.setClipToOutline(true);
        params.width=dp(58);params.height=dp(58);params.x=dp(18);params.y=dp(220);
        bubble.setOnTouchListener((v,e)->{switch(e.getAction()){
            case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();lastX=downX;lastY=downY;return true;
            case MotionEvent.ACTION_MOVE:params.x+=(int)(e.getRawX()-lastX);params.y+=(int)(e.getRawY()-lastY);lastX=e.getRawX();lastY=e.getRawY();update();return true;
            case MotionEvent.ACTION_UP:if(Math.abs(e.getRawX()-downX)<dp(8)&&Math.abs(e.getRawY()-downY)<dp(8)){try{wm.removeView(v);}catch(Exception ignored){}panel=null;activeTab="menu";showPanel();}return true;
        }return true;});
        WindowManager.LayoutParams bubbleParams=new WindowManager.LayoutParams(dp(bubbleDp),dp(bubbleDp),Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);bubbleParams.gravity=Gravity.TOP|Gravity.START;bubbleParams.x=dp(12);bubbleParams.y=sh/3;params=bubbleParams;panel=bubble;wm.addView(panel,params);
    }
    @Override public void onDestroy(){if(web!=null){try{web.stopLoading();web.destroy();}catch(Exception ignored){}}try{if(panel!=null)wm.removeView(panel);}catch(Exception ignored){}panel=null;super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
