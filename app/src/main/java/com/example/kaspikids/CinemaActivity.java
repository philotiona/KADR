package com.example.kaspikids;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static com.example.kaspikids.CinemaRules.*;
import com.example.kaspikids.CinemaDb.Row;
/** Dependency-free native Android UI. The same repository is used by UI and device tests. */
public class CinemaActivity extends Activity {
    static final int PAPER=0xfff5f1e8,INK=0xff191a1b,MUTED=0xff74736b,ACCENT=0xffff7447,YELLOW=0xfff5cc55,LINE=0xffd9d4c9,WHITE=0xfffffdf8;
    private CinemaDb db;
    private RemoteConfig remote;
    private boolean updatePromptShown;
    private DatabaseScreen databaseScreen;
    private Bundle databaseState;
    private Intent pendingDatabaseResult;
    private int pendingDatabaseResultCode;
    private String token="",screen="home",search="",genre="Все жанры",formatFilter="Все форматы",sort="По названию",ticketFilter="Все";
    private long routeId=0,selectedDay=0;
    private LinearLayout root,body,content;
    private ScrollView scroll;
    private int fieldId=0x6000;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean busy;
    private final ArrayDeque<String> history=new ArrayDeque<>();
    private final LinkedHashSet<String> selected=new LinkedHashSet<>();
    private String promo="";
    private long selectionShow=0;
    private Row exportBooking;
    private String exportKind="";
    private final Map<String,String> restoredFields=new HashMap<>();
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db=new CinemaDb(this);
        remote=new RemoteConfig(this);
        Telemetry.init(this);
        token=getSharedPreferences("kadr",0).getString("session","");
        selectedDay=day(System.currentTimeMillis(),0);
        if(state!=null) {
            databaseState=state.getBundle("databaseState");
            screen=state.getString("screen","home");
            routeId=state.getLong("route");
            selectedDay=state.getLong("day",selectedDay);
            search=state.getString("search","");
            genre=state.getString("genre","Все жанры");
            formatFilter=state.getString("format","Все форматы");
            sort=state.getString("sort","По названию");
            selectionShow=state.getLong("selectionShow");
            promo=state.getString("promo","");
            ArrayList<String> seats=state.getStringArrayList("seats");
            if(seats!=null)selected.addAll(seats);
            ArrayList<String> stack=state.getStringArrayList("history");
            if(stack!=null)history.addAll(stack);
            Bundle fields=state.getBundle("fields");
            if(fields!=null)for(String key:fields.keySet())restoredFields.put(key,fields.getString(key));
            exportKind=state.getString("exportKind","");
            long bid=state.getLong("exportBooking");
            if(bid>0) {
                Row r=new Row();
                r.put("id",""+bid);
                exportBooking=r;
            }
        }
        shell(false);
        text(content,"KADR.",48,true);
        text(content,"Загружаем вашу афишу…",16,false);
        LabHooks.beginWork();
        worker.execute(()-> {
            try {
                db.getWritableDatabase();
                finishWork(()-> {
                    render();
                    restoreFields();
                    refreshRemoteConfig();
                });
            }
            catch(Exception e) {
                finishWork(()->message("Не удалось открыть базу данных",e.getMessage()));
            }
        }
        );
    }
    @Override protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        if(databaseScreen!=null)databaseState=databaseScreen.saveState();
        b.putBundle("databaseState",databaseState);
        b.putString("screen",screen);
        b.putLong("route",routeId);
        b.putLong("day",selectedDay);
        b.putString("search",search);
        b.putString("genre",genre);
        b.putString("format",formatFilter);
        b.putString("sort",sort);
        b.putLong("selectionShow",selectionShow);
        b.putString("promo",promo);
        b.putStringArrayList("seats",new ArrayList<>(selected));
        b.putStringArrayList("history",new ArrayList<>(history));
        Bundle fields=new Bundle();
        saveFields(root,fields);
        b.putBundle("fields",fields);
        b.putString("exportKind",exportKind);
        if(exportBooking!=null)b.putLong("exportBooking",exportBooking.l("id"));
    }
    private void saveFields(View v,Bundle b) {
        if(v instanceof EditText&&v.getTag() instanceof String&&v.isSaveEnabled())b.putString((String)v.getTag(),((EditText)v).getText().toString());
        if(v instanceof ViewGroup)for(int i=0; i<((ViewGroup)v).getChildCount(); i++)saveFields(((ViewGroup)v).getChildAt(i),b);
    }
    private void restoreFields() {
        for(Map.Entry<String,String> e:restoredFields.entrySet()) {
            View v=root.findViewWithTag(e.getKey());
            if(v instanceof EditText)((EditText)v).setText(e.getValue());
        }
        restoredFields.clear();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        worker.execute(db::close);
        worker.shutdown();
    }
    int dp(int n) {
        return Ui.dp(this,n);
    }
    private LinearLayout col() {
        LinearLayout v=new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }
    private LinearLayout row() {
        LinearLayout v=new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private GradientDrawable bg(int fill,int stroke,int radius) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);
        if(stroke!=0)d.setStroke(dp(1),stroke);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private TextView text(LinearLayout p,String s,int size,boolean bold) {
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextColor(INK);
        t.setTextSize(size);
        t.setTypeface(Typeface.create(bold?"sans-serif-black":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(3),1f);
        p.addView(t,new LinearLayout.LayoutParams(-1,-2));
        return t;
    }
    private TextView caption(LinearLayout p,String s) {
        TextView t=text(p,s,12,true);
        t.setTextColor(MUTED);
        t.setLetterSpacing(.10f);
        return t;
    }
    private void gap(LinearLayout p,int n) {
        View v=new View(this);
        p.addView(v,new LinearLayout.LayoutParams(1,dp(n)));
    }
    private void line(LinearLayout p) {
        View v=new View(this);
        v.setBackgroundColor(LINE);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));
        lp.setMargins(0,dp(16),0,dp(16));
        p.addView(v,lp);
    }
    private Button button(LinearLayout p,String label,int fill,Runnable action) {
        Button b=new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(INK);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setMinHeight(dp(50));
        b.setMinimumHeight(dp(50));
        b.setPadding(dp(14),dp(10),dp(14),dp(10));
        b.setStateListAnimator(null);
        b.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x22191a1b),bg(fill,INK,6),null));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(5),0,dp(5));
        p.addView(b,lp);
        b.setOnClickListener(v->safe(action));
        return b;
    }
    private Button small(LinearLayout p,String label,int fill,Runnable action) {
        Button b=button(p,label,fill,action);
        b.setTextSize(12);
        b.setMinHeight(dp(44));
        b.setMinimumHeight(dp(44));
        b.setPadding(dp(10),dp(7),dp(10),dp(7));
        return b;
    }
    private void weight(View v) {
        LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)v.getLayoutParams();
        lp.width=0;
        lp.weight=1;
        lp.leftMargin=dp(3);
        lp.rightMargin=dp(3);
        v.setLayoutParams(lp);
    }
    private LinearLayout card(LinearLayout p,int fill) {
        LinearLayout c=col();
        c.setPadding(dp(16),dp(16),dp(16),dp(16));
        c.setBackground(bg(fill,LINE,8));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=dp(12);
        p.addView(c,lp);
        return c;
    }
    private EditText field(LinearLayout p,String label,String value,int input) {
        gap(p,10);
        caption(p,label);
        gap(p,7);
        EditText e=new EditText(this);
        e.setId(fieldId++);
        e.setTag(label);
        e.setText(value);
        e.setTextSize(16);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setContentDescription(label);
        e.setInputType(input);
        e.setPadding(dp(14),dp(12),dp(14),dp(12));
        e.setBackground(bg(WHITE,LINE,6));
        e.setSelectAllOnFocus(false);
        e.setMinHeight(dp(50));
        if((input&android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)!=0) {
            e.setMinLines(3);
            e.setGravity(Gravity.TOP);
        }
        else e.setSingleLine(true);
        p.addView(e,new LinearLayout.LayoutParams(-1,-2));
        return e;
    }
    private EditText passwordField(LinearLayout p,String label) {
        EditText e=field(p,label,"",129);
        e.setTransformationMethod(PasswordTransformationMethod.getInstance());
        e.setSaveEnabled(false);
        CheckBox show=new CheckBox(this);
        show.setText("Показать пароль");
        show.setTextColor(MUTED);
        show.setTextSize(12);
        show.setOnCheckedChangeListener((v,checked)-> {
            e.setTransformationMethod(checked?null:PasswordTransformationMethod.getInstance());
            e.setSelection(e.length());
        }
        );
        p.addView(show);
        return e;
    }
    private Spinner spinner(LinearLayout p,String label,List<String> values,int index) {
        gap(p,10);
        caption(p,label);
        gap(p,6);
        Spinner s=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        s.setSelection(Math.max(0,index));
        s.setBackground(bg(WHITE,LINE,6));
        s.setPadding(dp(8),0,dp(8),0);
        s.setContentDescription(label);
        p.addView(s,new LinearLayout.LayoutParams(-1,dp(50)));
        return s;
    }
    private Spinner spinner(LinearLayout p,String label,String[] values,int index) {
        return spinner(p,label,Arrays.asList(values),index);
    }
    private String value(EditText e) {
        return e.getText().toString();
    }
    private int number(EditText e) {
        try {
            return Integer.parseInt(value(e).trim());
        }
        catch(Exception x) {
            throw new IllegalArgumentException("Введите целое число в поле «"+e.getTag()+"»");
        }
    }
    private void hideKeyboard() {
        View v=getCurrentFocus();
        if(v!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);
    }
    private void safe(Runnable task) {
        try {
            task.run();
        }
        catch(IllegalArgumentException e) {
            message("Проверьте данные",e.getMessage());
        }
        catch(Exception e) {
            if(e instanceof IllegalStateException&&String.valueOf(e.getMessage()).startsWith("KADR test crash"))throw (IllegalStateException)e;
            Telemetry.nonFatal(e);
            android.util.Log.e("KADR","Operation failed",e);
            message("Не удалось выполнить действие","Попробуйте ещё раз. "+e.getClass().getSimpleName());
        }
    }
    private void message(String title,String s) {
        if(!isFinishing())new AlertDialog.Builder(this).setTitle(title).setMessage(s==null?"Ошибка":s).setPositiveButton("Понятно",null).show();
    }
    private void confirm(String title,String s,Runnable action) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(s).setNegativeButton("Назад",null).setPositiveButton("Подтвердить",(d,w)->safe(action)).show();
    }
    private void toast(String s) {
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }
    private void finishWork(Runnable update) {
        runOnUiThread(()-> {
            try {
                if (!isDestroyed()) update.run();
            } finally {
                LabHooks.endWork();
            }
        });
    }
    private void background(Button b,Callable<String> action,Runnable done) {
        if(busy)return;
        busy=true;
        b.setEnabled(false);
        String label=b.getText().toString();
        b.setText("Подождите…");
        LabHooks.beginWork();
        worker.execute(()-> {
            try {
                String result=action.call();
                finishWork(()-> {
                    busy=false;
                    b.setEnabled(true);
                    b.setText(label);
                    if(result!=null)toast(result);
                    safe(done);
                }
                );
            }
            catch(Exception e) {
                if (BuildConfig.DEBUG && !(e instanceof IllegalArgumentException)) {
                    android.util.Log.e("KadrLab", "Background operation failed", e);
                }
                finishWork(()-> {
                    busy=false;
                    b.setEnabled(true);
                    b.setText(label);
                    message("Проверьте данные",e.getMessage());
                }
                );
            }
        }
        );
    }
    private Row me() {
        Row u=db.currentUser(token);
        require(u!=null,"Войдите в аккаунт");
        return u;
    }
    private boolean isAdmin() {
        Row u=db.currentUser(token);
        return u!=null&&u.s("role").equals("ADMIN");
    }
    private void shell(boolean nav) {
        fieldId=0x6000;
        root=col();
        root.setBackgroundColor(PAPER);
        setContentView(root);
        Ui.applyInsets(root);
        LinearLayout header=row();
        header.setPadding(dp(22),dp(14),dp(22),dp(12));
        root.addView(header);
        TextView brand=text(header,"KADR.",27,true);
        weight(brand);
        brand.setOnClickListener(v-> {
            if(db.currentUser(token)!=null)tab("home");
        }
        );
        TextView city=text(header,"КИНОТЕАТР / АЛМАТЫ",10,true);
        city.setTextColor(MUTED);
        city.setLetterSpacing(.12f);
        city.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));
        View divider=new View(this);
        divider.setBackgroundColor(INK);
        root.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        body=col();
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=col();
        content.setPadding(dp(22),dp(22),dp(22),dp(28));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        if(nav) {
            LinearLayout bar=row();
            bar.setBackground(bg(WHITE,LINE,0));
            bar.setPadding(dp(8),dp(5),dp(8),dp(5));
            root.addView(bar);
            String[] labels= {
                "Афиша","Билеты","Избранное","База","Кабинет"
            }
            ,routes= {
                "home","tickets","favorites","database","profile"
            }
            ,icons= {
                "▦","▤","♡","◇","○"
            }
            ;
            for(int i=0; i<labels.length; i++) {
                final String dest=routes[i];
                LinearLayout item=col();
                item.setGravity(Gravity.CENTER);
                bar.addView(item,new LinearLayout.LayoutParams(0,dp(58),1));
                boolean active=screen.equals(dest)||(dest.equals("home")&&(screen.equals("movie")||screen.equals("seats")))||(dest.equals("tickets")&&screen.equals("ticket"))||(dest.equals("profile")&&(screen.equals("admin")||screen.startsWith("manage")));
                item.setBackground(bg(active?YELLOW:WHITE,0,6));
                TextView icon=text(item,icons[i],22,true);
                icon.setGravity(Gravity.CENTER);
                TextView label=text(item,labels[i],10,true);
                label.setGravity(Gravity.CENTER);
                item.setContentDescription(labels[i]);
                item.setOnClickListener(v->safe(()->tab(dest)));
                item.setFocusable(true);
            }
        }
    }
    private void title(String eyebrow,String title,String detail) {
        if(!eyebrow.isEmpty()) {
            caption(content,eyebrow);
            gap(content,9);
        }
        text(content,title,32,true);
        if(!detail.isEmpty()) {
            gap(content,10);
            TextView sub=text(content,detail,14,false);
            sub.setTextColor(MUTED);
        }
        gap(content,22);
    }
    private void back() {
        small(content,"← Назад",PAPER,this::goBack);
        gap(content,12);
    }
    private void nav(String name,long id) {
        hideKeyboard();
        history.addLast(screen+"|"+routeId);
        screen=name;
        routeId=id;
        render();
    }
    private void tab(String name) {
        hideKeyboard();
        history.clear();
        screen=name;
        routeId=0;
        render();
    }
    @Override public void onBackPressed() {
        if(db.currentUser(token)==null) {
            super.onBackPressed();
            return;
        }
        goBack();
    }
    private void goBack() {
        hideKeyboard();
        if(!history.isEmpty()) {
            String[] r=history.removeLast().split("\\|");
            screen=r[0];
            routeId=Long.parseLong(r[1]);
            render();
        }
        else if(!screen.equals("home"))tab("home");
        else super.onBackPressed();
    }
    private void render() {
        if(isDestroyed())return;
        if(databaseScreen!=null){databaseState=databaseScreen.saveState();databaseScreen=null;}
        Row u=db.currentUser(token);
        if(u==null) {
            screen="login";
            history.clear();
            shell(false);
            loginScreen();
            return;
        }
        if(screen.equals("login"))screen="home";
        if((screen.equals("admin")||screen.startsWith("manage"))&&!isAdmin()) {
            screen="home";
            history.clear();
        }
        shell(true);
        Telemetry.screen(screen);
        safe(()-> {
            switch(screen) {
                case "movie":movieScreen(routeId);
                break;
                case "seats":seatScreen(routeId);
                break;
                case "tickets":ticketsScreen(false);
                break;
                case "ticket":ticketScreen(routeId);
                break;
                case "favorites":favoriteScreen();
                break;
                case "database":
                    body.removeAllViews();
                    databaseScreen=new DatabaseScreen(this,db,token,databaseState);
                    body.addView(databaseScreen,new LinearLayout.LayoutParams(-1,-1));
                    if(pendingDatabaseResult!=null){databaseScreen.onActivityResult(DatabaseScreen.EXPORT_REQUEST,pendingDatabaseResultCode,pendingDatabaseResult);pendingDatabaseResult=null;}
                    break;
                case "profile":profileScreen();
                break;
                case "admin":adminScreen();
                break;
                case "manageUsers":usersScreen();
                break;
                case "manageMovies":moviesAdmin();
                break;
                case "manageHalls":hallsAdmin();
                break;
                case "manageShows":showsAdmin();
                break;
                case "manageBookings":ticketsScreen(true);
                break;
                case "managePromos":promosAdmin();
                break;
                default:homeScreen();
            }
        }
        );
    }
    private void loginScreen() {
        title("ВРЕМЯ БОЛЬШОГО ЭКРАНА","Хорошее кино.\nВаше место.","Войдите, чтобы выбрать сеанс и забронировать билеты.");
        FilmArt art=new FilmArt(this,0,"Орбита");
        content.addView(art,new LinearLayout.LayoutParams(-1,dp(180)));
        gap(content,12);
        EditText login=field(content,"Логин","",1),pass=passwordField(content,"Пароль");
        TextView error=text(content,"",13,false);
        error.setTextColor(0xffb12d24);
        Button enter=button(content,"Войти  →",ACCENT,()-> {
        }
        );
        enter.setOnClickListener(v-> {
            error.setText("");
            String username=value(login), password=value(pass);
            if(username.trim().isEmpty()||password.isEmpty()) {
                error.setText("Введите логин и пароль");
                return;
            }
            background(enter,()-> {
                String t=db.loginUser(username,password);
                getSharedPreferences("kadr",0).edit().putString("session",t).commit();
                token=t;
                Telemetry.event("login",null,0);
                return null;
            }
            ,()-> {
                screen="home";
                history.clear();
                render();
            }
            );
        }
        );
        gap(content,14);
        TextView note=text(content,"Аккаунт выдаёт администратор кинотеатра. Если забыли пароль, обратитесь к нему для сброса.",13,false);
        note.setTextColor(MUTED);
        line(content);
        caption(content,"УЧЕБНЫЙ ДЕМО-ДОСТУП");
        LinearLayout demo=row();
        content.addView(demo);
        weight(small(demo,"Зритель",WHITE,()-> {
            login.setText("viewer");
            pass.setText("Cinema123!");
        }
        ));
        weight(small(demo,"Администратор",YELLOW,()-> {
            login.setText("admin");
            pass.setText("Admin123!");
        }
        ));
        text(content,"viewer / Cinema123!     admin / Admin123!",11,false).setTextColor(MUTED);
        addLabEntry();
        versionFooter();
    }
    /** Lab 8, tasks 12/14: remote config drives staged rollout, forced update and feature flags. */
    private void refreshRemoteConfig() {
        new Thread(()-> {
            remote.refresh();
            runOnUiThread(this::checkForUpdate);
        },"kadr-remote-config").start();
    }
    private void checkForUpdate() {
        if(isFinishing()||isDestroyed())return;
        if(remote.updateRequired()) {
            AlertDialog d=new AlertDialog.Builder(this).setTitle("Требуется обновление")
                .setMessage("Версия "+BuildConfig.VERSION_NAME+" больше не поддерживается. Установите KADR "+remote.latestVersionName()+", чтобы продолжить.\n\n"+remote.message())
                .setCancelable(false)
                .setPositiveButton("Скачать",(x,w)->{openUrl(remote.downloadUrl());finish();})
                .create();
            d.show();
            return;
        }
        if(!updatePromptShown&&remote.updateAvailable()) {
            updatePromptShown=true;
            new AlertDialog.Builder(this).setTitle("Доступна версия "+remote.latestVersionName())
                .setMessage(remote.message().isEmpty()?"Обновите приложение, чтобы получить исправления и новые функции.":remote.message())
                .setNegativeButton("Позже",null)
                .setPositiveButton("Обновить",(x,w)->openUrl(remote.downloadUrl()))
                .show();
        }
    }
    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));
        }
        catch(ActivityNotFoundException e) {
            message("Не удалось открыть ссылку",url);
        }
    }
    /** Lab 8, task 3: every build shows its version and, outside release, its environment and API. */
    private void versionFooter() {
        gap(content,18);
        String env=BuildConfig.ENVIRONMENT.equals("release")?"":"\n"+BuildConfig.ENVIRONMENT.toUpperCase(Locale.ROOT)+"  ·  "+BuildConfig.APPLICATION_ID+"\nAPI: "+BuildConfig.API_BASE_URL;
        TextView v=text(content,"Версия "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")"+env,11,false);
        v.setTextColor(MUTED);
        v.setTag("versionFooter");
    }
    private void addLabEntry() {
        if (BuildConfig.DEBUG) {
            button(content,"Лабораторная работа №7",PAPER,()->LabHooks.open(this));
        }
    }
    private void homeScreen() {
        Row u=me();
        caption(content,"Здравствуйте, "+u.s("full_name")+".");
        gap(content,10);
        text(content,"Время кино.",39,true);
        gap(content,8);
        text(content,"Выберите историю на сегодняшний вечер.",14,false).setTextColor(MUTED);
        gap(content,16);
        LinearLayout actions=row();
        content.addView(actions);
        weight(small(actions,"Личный кабинет",WHITE,()->tab("profile")));
        weight(small(actions,"Выйти",PAPER,this::logout));
        if(isAdmin()) {
            LinearLayout admin=row();
            content.addView(admin);
            weight(small(admin,"Регистрация пользователей",YELLOW,this::registerDialog));
            weight(small(admin,"Управление →",WHITE,()->nav("admin",0)));
        }
        EditText q=field(content,"Поиск фильма",search,1);
        q.setHint("Название, жанр, режиссёр");
        LinearLayout list=col();
        small(content,"Фильтры  /  "+genre+"  /  "+formatFilter,WHITE,()->filtersDialog());
        gap(content,14);
        dateStrip(content,()->drawMovieList(list,false));
        gap(content,18);
        content.addView(list);
        q.addTextChangedListener(watcher(()-> {
            search=value(q);
            drawMovieList(list,false);
        }
        ));
        drawMovieList(list,false);
    }
    private void filtersDialog() {
        LinearLayout p=col();
        LinkedHashSet<String> genres=new LinkedHashSet<>();
        genres.add("Все жанры");
        for(Row m:db.movies(false))genres.add(m.s("genre"));
        List<String> values=new ArrayList<>(genres);
        Spinner g=spinner(p,"Жанр",values,values.indexOf(genre)),f=spinner(p,"Формат",new String[] {
            "Все форматы","2D","3D","ATMOS"
        }
        ,Arrays.asList("Все форматы","2D","3D","ATMOS").indexOf(formatFilter)),o=spinner(p,"Сортировка",new String[] {
            "По названию","По рейтингу","Сначала новинки"
        }
        ,Arrays.asList("По названию","По рейтингу","Сначала новинки").indexOf(sort));
        button(p,"Сбросить фильтры",PAPER,()-> {
            g.setSelection(0);
            f.setSelection(0);
            o.setSelection(0);
        }
        );
        form("Фильтры афиши",p,()-> {
            genre=g.getSelectedItem().toString();
            formatFilter=f.getSelectedItem().toString();
            sort=o.getSelectedItem().toString();
            render();
        }
        );
    }
    private TextWatcher watcher(Runnable r) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {
            }
            public void onTextChanged(CharSequence s,int st,int before,int count) {
                r.run();
            }
            public void afterTextChanged(Editable e) {
            }
        }
        ;
    }
    private AdapterView.OnItemSelectedListener selection(Runnable r) {
        return new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id) {
                safe(r);
            }
            public void onNothingSelected(AdapterView<?> p) {
            }
        }
        ;
    }
    private void dateStrip(LinearLayout parent,Runnable refresh) {
        HorizontalScrollView h=new HorizontalScrollView(this);
        h.setHorizontalScrollBarEnabled(false);
        LinearLayout dates=row();
        h.addView(dates);
        parent.addView(h);
        long today=day(System.currentTimeMillis(),0);
        Button[] other= {
            null
        }
        ;
        Runnable highlight=()-> {
            for(int k=0; k<dates.getChildCount(); k++) {
                View child=dates.getChildAt(k);
                if(child.getTag() instanceof Long)child.setBackground(bg(((Long)child.getTag())==selectedDay?YELLOW:WHITE,INK,6));
            }
            if(other[0]!=null) {
                boolean custom=selectedDay<today||selectedDay>=day(today,8);
                other[0].setText(custom?date(selectedDay,"dd.MM.yyyy"):"Другая дата");
                other[0].setBackground(bg(custom?YELLOW:WHITE,INK,6));
            }
        }
        ;
        for(int i=0; i<8; i++) {
            final long d=day(today,i);
            Button b=small(dates,(i==0?"Сегодня":i==1?"Завтра":date(d,"EEE"))+"\n"+date(d,"d MMM"),d==selectedDay?YELLOW:WHITE,()-> {
                selectedDay=d;
                highlight.run();
                refresh.run();
            }
            );
            b.setTag(d);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(87),dp(62));
            lp.rightMargin=dp(8);
            b.setLayoutParams(lp);
        }
        other[0]=small(dates,"Другая дата",WHITE,()-> {
            Calendar c=Calendar.getInstance();
            c.setTimeInMillis(selectedDay);
            DatePickerDialog picker=new DatePickerDialog(this,(view,y,m,d)-> {
                Calendar chosen=Calendar.getInstance();
                chosen.set(y,m,d);
                selectedDay=day(chosen.getTimeInMillis(),0);
                highlight.run();
                refresh.run();
            }
            ,c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
            picker.show();
        }
        );
        other[0].setLayoutParams(new LinearLayout.LayoutParams(dp(115),dp(62)));
        highlight.run();
    }
    private void drawMovieList(LinearLayout list,boolean favorite) {
        list.removeAllViews();
        List<Row> movies=favorite?db.favorites(token):db.movies(false);
        String query=search.trim().toLowerCase(new Locale("ru"));
        if(!favorite) {
            movies.removeIf(m->!((m.s("title")+" "+m.s("genre")+" "+m.s("director")).toLowerCase(new Locale("ru")).contains(query))||(!genre.equals("Все жанры")&&!genre.equals(m.s("genre"))));
            if(sort.equals("По рейтингу"))movies.sort((a,b)->Double.compare(rating(b),rating(a)));
            else if(sort.equals("Сначала новинки"))movies.sort((a,b)->Long.compare(b.l("id"),a.l("id")));
            else movies.sort((a,b)->a.s("title").compareToIgnoreCase(b.s("title")));
        }
        int count=0;
        LinearLayout pair=null;
        for(Row m:movies) {
            List<Row> sessions=db.shows(m.l("id"),Math.max(selectedDay,System.currentTimeMillis()),day(selectedDay,1),false);
            if(!favorite&&!formatFilter.equals("Все форматы"))sessions.removeIf(s->!s.s("format").equals(formatFilter));
            if(!favorite&&!formatFilter.equals("Все форматы")&&sessions.isEmpty())continue;
            if(count%2==0) {
                pair=row();
                pair.setGravity(Gravity.TOP);
                list.addView(pair,new LinearLayout.LayoutParams(-1,-2));
            }
            posterCard(pair,m,sessions);
            count++;
        }
        if(count==0)empty(list,favorite?"Пока без избранного":"Ничего не найдено",favorite?"Откройте фильм и нажмите «В избранное».":"Измените поиск, жанр, формат или дату.");
    }
    private void posterCard(LinearLayout p,Row m,List<Row> sessions) {
        LinearLayout c=card(p,WHITE);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
        lp.setMargins(dp(3),0,dp(3),dp(12));
        c.setLayoutParams(lp);
        c.setPadding(dp(8),dp(8),dp(8),dp(10));
        FilmArt art=new FilmArt(this,m.i("art"),m.s("title"));
        c.addView(art,new LinearLayout.LayoutParams(-1,dp(196)));
        art.setOnClickListener(v->safe(()->nav("movie",m.l("id"))));
        gap(c,11);
        TextView title=text(c,m.s("title"),17,true);
        title.setMinLines(2);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        gap(c,7);
        TextView genreText=text(c,m.s("genre")+" · "+m.s("age")+"+",11,false);
        genreText.setMaxLines(1);
        genreText.setEllipsize(TextUtils.TruncateAt.END);
        genreText.setTextColor(MUTED);
        gap(c,6);
        text(c,m.s("duration")+" мин"+(m.s("rating").isEmpty()?"":" · ★ "+m.s("rating")),11,false).setTextColor(MUTED);
        gap(c,8);
        small(c,sessions.isEmpty()?"Все даты →":"От "+money(sessions.stream().mapToInt(r->r.i("price")).min().orElse(0)),YELLOW,()->nav("movie",m.l("id")));
    }
    private double rating(Row r) {
        try {
            return Double.parseDouble(r.s("rating"));
        }
        catch(Exception e) {
            return 0;
        }
    }
    private void movieCard(LinearLayout parent,Row m,List<Row> sessions) {
        LinearLayout c=card(parent,WHITE);
        FilmArt art=new FilmArt(this,m.i("art"),m.s("title"));
        c.addView(art,new LinearLayout.LayoutParams(-1,dp(270)));
        art.setOnClickListener(v->safe(()->nav("movie",m.l("id"))));
        gap(c,14);
        caption(c,m.s("genre").toUpperCase(new Locale("ru"))+"  /  "+m.s("age")+"+");
        gap(c,8);
        text(c,m.s("title"),25,true);
        gap(c,8);
        text(c,m.s("duration")+" мин  ·  "+(m.s("rating").isEmpty()?"Пока без оценок":"★ "+m.s("rating"))+"  ·  "+m.s("year"),12,false).setTextColor(MUTED);
        gap(c,12);
        if(sessions.isEmpty())text(c,"На выбранную дату сеансов нет",13,false).setTextColor(MUTED);
        else {
            LinearLayout times=row();
            c.addView(times);
            int n=0;
            for(Row s:sessions) {
                if(n++>=3)break;
                weight(small(times,date(s.l("starts_at"),"HH:mm")+"\n"+money(s.l("price")),PAPER,()->nav("seats",s.l("id"))));
            }
        }
        button(c,"О фильме и все сеансы  ↗",WHITE,()->nav("movie",m.l("id")));
    }
    private void empty(LinearLayout p,String heading,String detail) {
        LinearLayout c=card(p,WHITE);
        text(c,heading,23,true);
        gap(c,12);
        text(c,detail,14,false).setTextColor(MUTED);
    }
    private void favoriteScreen() {
        title("ВАША КОЛЛЕКЦИЯ","Избранное","Фильмы, к которым хочется вернуться.");
        drawMovieList(content,true);
    }
    private void movieScreen(long id) {
        back();
        Row m=db.movie(id);
        FilmArt art=new FilmArt(this,m.i("art"),m.s("title"));
        content.addView(art,new LinearLayout.LayoutParams(-1,dp(340)));
        gap(content,22);
        title(m.s("genre").toUpperCase(new Locale("ru"))+" / "+m.s("age")+"+",m.s("title"),m.s("year")+"  ·  "+m.s("duration")+" мин  ·  "+m.s("director"));
        text(content,m.s("synopsis"),16,false);
        gap(content,14);
        button(content,db.favorite(token,id)?"♥ В избранном · убрать":"♡ В избранное",YELLOW,()-> {
            db.toggleFavorite(token,id);
            render();
        }
        );
        line(content);
        text(content,"Выберите сеанс",23,true);
        gap(content,15);
        LinearLayout sessions=col();
        dateStrip(content,()->showSessions(sessions,id));
        gap(content,14);
        content.addView(sessions);
        showSessions(sessions,id);
        line(content);
        text(content,"Отзывы зрителей",23,true);
        gap(content,8);
        text(content,m.s("review_count")+" отзывов"+(m.s("rating").isEmpty()?"":"  ·  ★ "+m.s("rating")),13,false);
        button(content,"Оценить фильм",WHITE,()->reviewDialog(id));
        for(Row r:db.reviews(id)) {
            LinearLayout c=card(content,WHITE);
            text(c,r.s("full_name")+"  ·  "+r.s("score")+" / 5",15,true);
            gap(c,8);
            text(c,r.s("comment"),14,false);
            gap(c,8);
            caption(c,date(r.l("updated_at"),"d MMM yyyy"));
            if(isAdmin()||r.l("user_id")==me().l("id"))small(c,"Удалить отзыв",PAPER,()->confirm("Удалить отзыв?","Оценка также будет удалена.",()-> {
                db.deleteReview(token,id,r.l("user_id"));
                render();
            }
            ));
        }
    }
    private void showSessions(LinearLayout p,long id) {
        p.removeAllViews();
        List<Row> sessions=db.shows(id,Math.max(selectedDay,System.currentTimeMillis()),day(selectedDay,1),false);
        if(sessions.isEmpty())empty(p,"Пока нет сеансов","Выберите другую дату.");
        for(Row s:sessions) {
            LinearLayout c=card(p,WHITE);
            text(c,date(s.l("starts_at"),"HH:mm")+"  /  "+money(s.l("price")),23,true);
            gap(c,7);
            text(c,s.s("hall_name")+" · "+s.s("format")+" · "+s.s("language"),12,false);
            gap(c,7);
            text(c,"Свободно "+s.s("free")+" мест",12,false).setTextColor(MUTED);
            Button b=button(c,s.i("free")==0?"Мест нет":"Выбрать места →",s.i("free")==0?LINE:YELLOW,()->nav("seats",s.l("id")));
            b.setEnabled(s.i("free")>0);
        }
    }
    private void reviewDialog(long id) {
        LinearLayout p=col();
        Row mine=null;
        for(Row r:db.reviews(id))if(r.l("user_id")==me().l("id"))mine=r;
        Spinner rating=spinner(p,"Ваша оценка",new String[] {
            "1 — плохо","2 — ниже ожиданий","3 — неплохо","4 — понравилось","5 — отлично"
        }
        ,mine==null?4:mine.i("score")-1);
        EditText review=field(p,"Ваш отзыв",mine==null?"":mine.s("comment"),android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form("Ваше мнение",p,()-> {
            db.review(token,id,rating.getSelectedItemPosition()+1,value(review));
            render();
        }
        );
    }
    private void seatScreen(long id) {
        back();
        Row s=db.show(id);
        title("ВЫБОР МЕСТ / "+s.s("format"),s.s("title"),date(s.l("starts_at"),"d MMMM, HH:mm")+"  ·  "+s.s("hall_name")+"\n"+s.s("language")+"  ·  "+s.s("age")+"+");
        require(s.s("status").equals("ACTIVE")&&s.l("starts_at")>System.currentTimeMillis(),"Бронирование сеанса закрыто");
        if(selectionShow!=id) {
            selected.clear();
            promo="";
            selectionShow=id;
        }
        Set<String> occupied=db.occupied(id);
        selected.removeAll(occupied);
        LinearLayout screenCard=card(content,WHITE);
        TextView screenLabel=text(screenCard,"Э К Р А Н",11,true);
        screenLabel.setGravity(Gravity.CENTER);
        gap(screenCard,8);
        View beam=new View(this);
        beam.setBackground(bg(YELLOW,INK,3));
        screenCard.addView(beam,new LinearLayout.LayoutParams(-1,dp(5)));
        gap(screenCard,18);
        HorizontalScrollView h=new HorizontalScrollView(this);
        h.setHorizontalScrollBarEnabled(true);
        LinearLayout grid=col();
        h.addView(grid);
        screenCard.addView(h);
        Map<String,Button> buttons=new HashMap<>();
        TextView selection=text(content,"",14,true);
        TextView amount=text(content,"",29,true);
        EditText promoField=field(content,"Промокод",promo,1);
        TextView promoInfo=text(content,"",13,false);
        promoInfo.setTextColor(MUTED);
        Button pay=button(content,"",ACCENT,()-> {
        }
        );
        Runnable update=()-> {
            int discount=0;
            if (BuildConfig.DEBUG) {
                android.util.Log.d("KadrLab", "Selected seats: " + selected.size());
            }
            try {
                discount=db.discount(value(promoField));
                promoInfo.setText(discount==0?"Есть промокод? Введите его перед бронированием.":"Скидка "+discount+"% применена");
                promoInfo.setTextColor(MUTED);
            }
            catch(IllegalArgumentException e) {
                if (BuildConfig.DEBUG) android.util.Log.w("KadrLab", "Promo rejected");
                promoInfo.setText(e.getMessage());
                promoInfo.setTextColor(0xffb12d24);
            }
            selection.setText(selected.isEmpty()?"Выберите до 6 мест":"Ряд:место  "+CinemaRules.join(", ",selected));
            amount.setText(selected.isEmpty()?"0 ₸":money(total(s.i("price"),selected.size(),discount)));
            pay.setText(selected.isEmpty()?"Выберите места":"Забронировать · "+money(total(s.i("price"),selected.size(),discount)));
            pay.setEnabled(!selected.isEmpty());
            for(Map.Entry<String,Button> b:buttons.entrySet()) {
                b.getValue().setBackground(bg(occupied.contains(b.getKey())?LINE:selected.contains(b.getKey())?ACCENT:WHITE,occupied.contains(b.getKey())?LINE:INK,5));
                b.getValue().setSelected(selected.contains(b.getKey()));
                String[] seat=b.getKey().split(":");
                b.getValue().setContentDescription("Ряд "+seat[0]+", место "+seat[1]+(occupied.contains(b.getKey())?", занято":selected.contains(b.getKey())?", выбрано":", свободно"));
            }
        }
        ;
        for(int r=1; r<=s.i("rows_count"); r++) {
            LinearLayout seats=row();
            grid.addView(seats);
            TextView label=text(seats,""+r,11,true);
            label.setTextColor(MUTED);
            label.setGravity(Gravity.CENTER);
            label.setLayoutParams(new LinearLayout.LayoutParams(dp(24),dp(44)));
            for(int c=1; c<=s.i("cols_count"); c++) {
                String key=r+":"+c;
                Button seat=small(seats,occupied.contains(key)?"×":""+c,WHITE,()-> {
                    if(selected.contains(key))selected.remove(key);
                    else {
                        require(selected.size()<6,"В одной брони может быть до 6 мест");
                        selected.add(key);
                    }
                    update.run();
                }
                );
                seat.setTag("seat-"+key);
                seat.setContentDescription("Ряд "+r+", место "+c+(occupied.contains(key)?", занято":", свободно"));
                seat.setEnabled(!occupied.contains(key));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(44),dp(44));
                lp.setMargins(dp(2),dp(3),dp(c==s.i("cols_count")/2?10:2),dp(3));
                seat.setLayoutParams(lp);
                buttons.put(key,seat);
            }
        }
        gap(screenCard,14);
        text(screenCard,"□ Свободно     ■ Выбрано     × Занято",11,false).setTextColor(MUTED);
        gap(screenCard,8);
        text(screenCard,"↔ Проведите по схеме, чтобы увидеть все места",11,false).setTextColor(MUTED);
        gap(content,12);
        text(content,"Оплата в кассе кинотеатра. Покажите код бронирования кассиру. Отмена доступна до начала сеанса.",13,false).setTextColor(MUTED);
        promoField.addTextChangedListener(watcher(()-> {
            promo=value(promoField);
            update.run();
        }
        ));
        pay.setOnClickListener(v-> {
            List<String> seats = new ArrayList<>(selected);
            String code = value(promoField);
            java.util.concurrent.atomic.AtomicLong booking = new java.util.concurrent.atomic.AtomicLong();
            background(pay,()-> {
                android.os.Trace.beginSection("KADR book seats");
                try {
                    booking.set(db.book(token,id,seats,code));
                    if (BuildConfig.DEBUG) android.util.Log.i("KadrLab", "Booking saved");
                    Telemetry.event("booking_created","seats",seats.size());
                    return "Места забронированы";
                } finally {
                    android.os.Trace.endSection();
                }
            },()-> {
                selected.clear();
                promo="";
                history.clear();
                history.add("tickets|0");
                screen="ticket";
                routeId=booking.get();
                render();
            });
        });
        update.run();
    }
    private String status(Row b) {
        switch(b.s("status")) {
            case "PAID":return b.l("starts_at")<System.currentTimeMillis()?"Сеанс прошёл · оплачен":"Оплачен";
            case "CHECKED_IN":return "Использован";
            case "CANCELED":return "Отменён";
            default:return b.l("starts_at")<System.currentTimeMillis()?"Сеанс прошёл · не оплачен":"Бронь · оплата в кассе";
        }
    }
    private void ticketsScreen(boolean all) {
        if(all)back();
        title(all?"КАССА И КОНТРОЛЬ":"ВАШИ ПОХОДЫ В КИНО",all?"Бронирования":"Мои билеты",all?"Найдите бронь по коду, фильму или имени зрителя.":"Предстоящие сеансы и история бронирований.");
        EditText q=field(content,"Найти билет","",1);
        Spinner filter=spinner(content,"Статус",new String[] {
            "Все","Предстоящие","Прошедшие","Отменённые"
        }
        ,0);
        gap(content,20);
        LinearLayout list=col();
        content.addView(list);
        Runnable refresh=()-> {
            list.removeAllViews();
            String term=value(q).trim().toLowerCase(new Locale("ru"));
            String selectedFilter=filter.getSelectedItem().toString();
            int n=0;
            for(Row b:db.bookings(token,all)) {
                if(!(b.s("code")+b.s("movie_title")+b.s("full_name")+b.s("username")).toLowerCase(new Locale("ru")).contains(term))continue;
                boolean canceled=b.s("status").equals("CANCELED"),past=b.l("starts_at")<=System.currentTimeMillis();
                if(selectedFilter.equals("Предстоящие")&&(canceled||past))continue;
                if(selectedFilter.equals("Прошедшие")&&(canceled||!past))continue;
                if(selectedFilter.equals("Отменённые")&&!canceled)continue;
                n++;
                LinearLayout c=card(list,canceled?PAPER:WHITE);
                caption(c,status(b).toUpperCase(new Locale("ru")));
                gap(c,10);
                text(c,b.s("movie_title"),23,true);
                gap(c,9);
                text(c,date(b.l("starts_at"),"d MMM, HH:mm")+" · "+b.s("hall_name"),13,false);
                gap(c,7);
                text(c,"Места "+b.s("seats")+"  /  "+money(b.l("total")),14,true);
                if(all) {
                    gap(c,7);
                    text(c,b.s("full_name")+" · @"+b.s("username"),12,false);
                }
                button(c,b.s("code")+"  →",canceled?PAPER:YELLOW,()->nav("ticket",b.l("id")));
            }
            if(n==0)empty(list,"Билетов пока нет",all?"Новые бронирования появятся здесь.":"Выберите фильм в афише и забронируйте места.");
        }
        ;
        q.addTextChangedListener(watcher(refresh));
        filter.setOnItemSelectedListener(selection(refresh));
        refresh.run();
    }
    private void ticketScreen(long id) {
        back();
        Row b=db.booking(token,id);
        boolean canceled=b.s("status").equals("CANCELED");
        title("ВАШ БИЛЕТ",canceled?"Бронь отменена":"Увидимся в кино.",status(b));
        LinearLayout ticket=card(content,WHITE);
        caption(ticket,"KADR. / "+b.s("format"));
        gap(ticket,14);
        text(ticket,b.s("movie_title"),30,true);
        gap(ticket,18);
        text(ticket,date(b.l("starts_at"),"d MMMM yyyy"),16,true);
        text(ticket,date(b.l("starts_at"),"HH:mm")+"  ·  "+b.s("hall_name"),18,true);
        line(ticket);
        caption(ticket,"РЯД : МЕСТО");
        gap(ticket,8);
        text(ticket,b.s("seats"),23,true);
        gap(ticket,12);
        text(ticket,money(b.l("total")),28,true);
        if(b.i("discount")>0)text(ticket,"Промокод "+b.s("promo")+" · −"+b.s("discount")+"%",12,false);
        line(ticket);
        TextView code=text(ticket,b.s("code"),23,true);
        code.setTypeface(Typeface.MONOSPACE);
        code.setGravity(Gravity.CENTER);
        if(!canceled) {
            ImageView qr=new ImageView(this);
            qr.setImageBitmap(qrBitmap(b.s("code"),290));
            qr.setContentDescription("QR-код бронирования "+b.s("code"));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(200));
            lp.setMargins(0,dp(12),0,dp(12));
            ticket.addView(qr,lp);
        }
        text(ticket,b.s("full_name"),13,false).setGravity(Gravity.CENTER);
        gap(content,8);
        if(b.s("status").equals("BOOKED"))text(content,"Оплатите "+money(b.l("total"))+" в кассе до начала сеанса. Этот QR-код содержит номер бронирования.",14,false);
        if(b.s("status").equals("CANCELED"))text(content,"Места освобождены. Для оплаченного билета возврат оформляется в кассе.",14,false);
        if(!canceled) {
            button(content,"Сохранить билет PNG",YELLOW,()->export(b,"png"));
            button(content,"Добавить в календарь · ICS",WHITE,()->export(b,"ics"));
        }
        button(content,"Поделиться деталями",WHITE,()-> {
            Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,ticketText(b));
            startActivity(Intent.createChooser(share,"Поделиться билетом"));
        }
        );
        if(!canceled&&!b.s("status").equals("CHECKED_IN")&&b.l("starts_at")>System.currentTimeMillis())button(content,"Отменить бронирование",PAPER,()->confirm("Отменить бронирование?","Все места этой брони снова станут доступны. Для оплаченного билета возврат оформляется в кассе.",()-> {
            db.bookingStatus(token,id,"CANCELED");
            render();
        }
        ));
        if(isAdmin()) {
            line(content);
            caption(content,"ДЕЙСТВИЯ КАССИРА");
            if(b.s("status").equals("BOOKED"))button(content,"Подтвердить оплату в кассе",ACCENT,()->confirm("Подтвердить оплату?","Получено в кассе: "+money(b.l("total")),()-> {
                db.bookingStatus(token,id,"PAID");
                render();
            }
            ));
            if(b.s("status").equals("PAID"))button(content,"Проверить и погасить билет",YELLOW,()->confirm("Пропустить зрителя?","Повторное использование билета будет заблокировано.",()-> {
                db.bookingStatus(token,id,"CHECKED_IN");
                render();
            }
            ));
        }
    }
    private String ticketText(Row b) {
        return "KADR. · "+b.s("movie_title")+"\n"+date(b.l("starts_at"),"dd.MM.yyyy HH:mm")+"\n"+b.s("hall_name")+" · "+b.s("format")+"\nРяд:место "+b.s("seats")+"\n"+money(b.l("total"))+"\n"+b.s("code")+"\n"+status(b);
    }
    private Bitmap qrBitmap(String code,int size) {
        return TicketImage.qr(code,size);
    }
    private void export(Row b,String kind) {
        exportBooking=b;
        exportKind=kind;
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(kind.equals("png")?"image/png":"text/calendar").putExtra(Intent.EXTRA_TITLE,b.s("code")+"."+kind);
        startActivityForResult(i,801);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==DatabaseScreen.EXPORT_REQUEST){if(databaseScreen!=null)databaseScreen.onActivityResult(request,result,data);else if(data!=null){pendingDatabaseResult=data;pendingDatabaseResultCode=result;}return;}
        if(request==801&&result==RESULT_OK&&data!=null&&data.getData()!=null&&exportBooking!=null)safe(()-> {
            Row b=db.booking(token,exportBooking.l("id"));
            require(!b.s("status").equals("CANCELED"),"Бронь уже отменена");
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())) {
                require(out!=null,"Не удалось открыть файл");
                if(exportKind.equals("ics")) {
                    Row show=db.show(b.l("show_id"));
                    String ics=CalendarFile.create(b.s("code"),b.s("movie_title"),b.s("hall_name"),ticketText(b),b.l("starts_at"),show.l("ends_at")-20*60000L);
                    out.write(ics.getBytes(StandardCharsets.UTF_8));
                }
                else {
                    Bitmap image=TicketImage.create(b.s("code"),ticketText(b));
                    require(image.compress(Bitmap.CompressFormat.PNG,100,out),"Не удалось записать PNG");
                    image.recycle();
                }
                toast("Файл сохранён");
            }
            catch(Exception e) {
                throw new IllegalArgumentException("Не удалось сохранить файл: "+e.getMessage());
            }
        }
        );
    }
    private void profileScreen() {
        Row u=me();
        title("ВАШ АККАУНТ","Личный кабинет","@"+u.s("username")+"  /  "+(isAdmin()?"Администратор":"Зритель"));
        EditText name=field(content,"Имя и фамилия",u.s("full_name"),1),email=field(content,"Электронная почта",u.s("email"),33),phone=field(content,"Телефон",u.s("phone"),3);
        gap(content,14);
        button(content,"Сохранить профиль",YELLOW,()-> {
            db.profile(token,value(name),value(email),value(phone));
            toast("Профиль сохранён");
            render();
        }
        );
        button(content,"Изменить пароль",WHITE,this::passwordDialog);
        if(isAdmin()) {
            button(content,"Регистрация пользователей",YELLOW,this::registerDialog);
            button(content,"Управление кинотеатром →",WHITE,()->nav("admin",0));
        }
        line(content);
        button(content,"О приложении",WHITE,()->message("KADR. · Кинотеатр","Бронирование билетов и управление кинотеатром. Данные хранятся в SQLite на этом устройстве. Оплата выполняется в кассе.\n\nФильмы и афиши в начальном каталоге — авторские демонстрационные материалы. Промокоды: FIRST −10%, STUDENT −15%.\n\nAndroid 7.0+ · Java · SQLite\nВерсия "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")"));
        button(content,"Что нового в "+BuildConfig.VERSION_NAME,WHITE,()->message("Что нового",getString(R.string.whats_new)));
        button(content,"Сообщить о проблеме",WHITE,()->openUrl("https://github.com/philotiona/KADR/issues/new/choose"));
        button(content,"Политика конфиденциальности",WHITE,()->openUrl("https://philotiona.github.io/KADR/privacy-policy.html"));
        button(content,"Проверить обновления",WHITE,()->{updatePromptShown=false;toast("Проверяем…");refreshRemoteConfig();});
        addLabEntry();
        if(BuildConfig.DIAGNOSTICS) {
            line(content);
            caption(content,"ДИАГНОСТИКА · "+BuildConfig.ENVIRONMENT.toUpperCase(Locale.ROOT)+(Telemetry.enabled()?" · FIREBASE ON":" · FIREBASE OFF"));
            button(content,"Тестовый сбой (Crashlytics)",WHITE,Diagnostics::testCrash);
            button(content,"Тестовый ANR (20 секунд)",WHITE,Diagnostics::testAnr);
        }
        button(content,"Выйти",PAPER,this::logout);
        versionFooter();
    }
    private void logout() {
        confirm("Выйти из аккаунта?","Сохранённые бронирования останутся в личном кабинете.",()-> {
            db.logout(token);
            getSharedPreferences("kadr",0).edit().remove("session").commit();
            databaseState=null;
            token="";
            selected.clear();
            selectionShow=0;
            promo="";
            search="";
            genre="Все жанры";
            formatFilter="Все форматы";
            history.clear();
            render();
        }
        );
    }
    private void form(String title,LinearLayout p,Runnable save) {
        ScrollView s=new ScrollView(this);
        p.setPadding(dp(22),dp(10),dp(22),dp(22));
        s.addView(p);
        TextView error=text(p,"",13,false);
        error.setTextColor(0xffb12d24);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(s).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",null).create();
        d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w-> {
            try {
                save.run();
                d.dismiss();
                hideKeyboard();
            }
            catch(IllegalArgumentException e) {
                error.setText(e.getMessage());
                s.post(()->s.fullScroll(View.FOCUS_DOWN));
            }
        }
        ));
        d.show();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void asyncForm(String title,LinearLayout p,Callable<String> action,Runnable done) {
        ScrollView s=new ScrollView(this);
        p.setPadding(dp(22),dp(10),dp(22),dp(22));
        s.addView(p);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(s).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",null).create();
        d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->background(d.getButton(-1),action,()-> {
            d.dismiss();
            done.run();
        }
        )));
        d.show();
    }
    private void passwordDialog() {
        LinearLayout p=col();
        EditText old=passwordField(p,"Текущий пароль"),next=passwordField(p,"Новый пароль"),repeat=passwordField(p,"Повтор нового пароля");
        asyncForm("Изменить пароль",p,()-> {
            require(value(next).equals(value(repeat)),"Пароли не совпадают");
            db.changePassword(token,value(old),value(next));
            return "Пароль изменён";
        }
        ,this::render);
    }
    private void registerDialog() {
        require(isAdmin(),"Доступно администратору");
        LinearLayout p=col();
        EditText login=field(p,"Логин нового пользователя","",1),name=field(p,"Имя и фамилия","",1),pass=passwordField(p,"Первоначальный пароль");
        Spinner role=spinner(p,"Роль",new String[] {
            "Пользователь","Администратор"
        }
        ,0);
        asyncForm("Регистрация пользователей",p,()-> {
            db.register(token,value(login),value(name),value(pass),role.getSelectedItemPosition()==0?"USER":"ADMIN");
            return "Пользователь зарегистрирован";
        }
        ,this::render);
    }
    private void adminScreen() {
        back();
        title("ПАНЕЛЬ АДМИНИСТРАТОРА","За кадром.","Расписание, зрители и работа кассы.");
        Row stats=db.stats(token);
        LinearLayout c=card(content,YELLOW);
        text(c,stats.s("shows")+" сеансов  /  "+stats.s("bookings")+" броней",23,true);
        gap(c,10);
        text(c,"Оплачено в кассе: "+money(stats.l("revenue"))+"\nАктивных аккаунтов: "+stats.s("users"),14,false);
        button(content,"01  /  Пользователи",WHITE,()->nav("manageUsers",0));
        button(content,"02  /  Фильмы и афиша",WHITE,()->nav("manageMovies",0));
        button(content,"03  /  Залы и места",WHITE,()->nav("manageHalls",0));
        button(content,"04  /  Расписание и цены",WHITE,()->nav("manageShows",0));
        button(content,"05  /  Бронирования и касса",YELLOW,()->nav("manageBookings",0));
        button(content,"06  /  Промокоды",WHITE,()->nav("managePromos",0));
    }
    private void usersScreen() {
        back();
        title("АДМИНИСТРИРОВАНИЕ","Пользователи","Регистрация, роли, доступ и восстановление паролей.");
        button(content,"+ Регистрация пользователей",YELLOW,this::registerDialog);
        EditText q=field(content,"Поиск пользователя","",1);
        gap(content,16);
        LinearLayout list=col();
        content.addView(list);
        Runnable draw=()-> {
            list.removeAllViews();
            for(Row u:db.users(token)) {
                if(!(u.s("full_name")+u.s("username")).toLowerCase(new Locale("ru")).contains(value(q).toLowerCase(new Locale("ru"))))continue;
                LinearLayout c=card(list,WHITE);
                text(c,u.s("full_name"),21,true);
                gap(c,9);
                text(c,"@"+u.s("username")+" · "+u.s("role")+" · "+(u.i("active")==1?"Активен":"Отключён"),12,false);
                button(c,"Роль и доступ",PAPER,()->accessDialog(u));
                button(c,"Сбросить пароль",WHITE,()->resetDialog(u));
            }
            if(list.getChildCount()==0)empty(list,"Ничего не найдено","Измените имя или логин.");
        }
        ;
        q.addTextChangedListener(watcher(draw));
        draw.run();
    }
    private void accessDialog(Row u) {
        LinearLayout p=col();
        Spinner role=spinner(p,"Роль",new String[] {
            "Пользователь","Администратор"
        }
        ,u.s("role").equals("ADMIN")?1:0);
        CheckBox enabled=new CheckBox(this);
        enabled.setText("Аккаунт активен");
        enabled.setChecked(u.i("active")==1);
        p.addView(enabled);
        form(u.s("full_name"),p,()-> {
            db.userAccess(token,u.l("id"),role.getSelectedItemPosition()==0?"USER":"ADMIN",enabled.isChecked());
            render();
        }
        );
    }
    private void resetDialog(Row u) {
        LinearLayout p=col();
        text(p,"Все текущие сессии этого пользователя будут завершены.",14,false);
        EditText pass=passwordField(p,"Новый пароль");
        asyncForm("Сброс пароля @"+u.s("username"),p,()-> {
            db.resetPassword(token,u.l("id"),value(pass));
            return "Пароль сброшен";
        }
        ,this::render);
    }
    private void moviesAdmin() {
        back();
        title("УПРАВЛЕНИЕ КАТАЛОГОМ","Фильмы","Архив сохраняет историю билетов.");
        button(content,"+ Добавить фильм",YELLOW,()->movieDialog(null));
        for(Row m:db.movies(true)) {
            LinearLayout c=card(content,WHITE);
            text(c,m.s("title"),22,true);
            gap(c,7);
            text(c,m.s("genre")+" · "+m.s("duration")+" мин · "+m.s("age")+"+ · "+(m.i("active")==1?"В афише":"Архив"),12,false);
            button(c,"Редактировать",WHITE,()->movieDialog(m));
            button(c,m.i("active")==1?"В архив":"Вернуть в афишу",PAPER,()-> {
                db.archive(token,"movies",m.l("id"),m.i("active")==1);
                render();
            }
            );
        }
    }
    private void movieDialog(Row m) {
        LinearLayout p=col();
        EditText title=field(p,"Название фильма",m==null?"":m.s("title"),1),genre=field(p,"Жанр",m==null?"Драма":m.s("genre"),1),duration=field(p,"Длительность в минутах",m==null?"120":m.s("duration"),2);
        Spinner age=spinner(p,"Возраст",new String[] {
            "0+","6+","12+","16+","18+"
        }
        ,m==null?2:Arrays.asList(0,6,12,16,18).indexOf(m.i("age")));
        EditText synopsis=field(p,"Описание",m==null?"":m.s("synopsis"),android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE),director=field(p,"Режиссёр",m==null?"":m.s("director"),1),year=field(p,"Год",m==null?"2026":m.s("year"),2);
        Spinner art=spinner(p,"Стиль афиши",new String[] {
            "Космос","Ночной город","Кассета","Океан","Лис","Горы"
        }
        ,m==null?0:m.i("art"));
        form(m==null?"Новый фильм":"Редактировать фильм",p,()-> {
            db.saveMovie(token,m==null?0:m.l("id"),value(title),value(genre),number(duration),new int[] {
                0,6,12,16,18
            }
            [age.getSelectedItemPosition()],value(synopsis),value(director),number(year),art.getSelectedItemPosition());
            render();
        }
        );
    }
    private void hallsAdmin() {
        back();
        title("ПРОСТРАНСТВА КИНОТЕАТРА","Залы и места","Каждый сеанс сохраняет собственную схему зала.");
        button(content,"+ Добавить зал",YELLOW,()->hallDialog(null));
        for(Row h:db.halls(true)) {
            LinearLayout c=card(content,WHITE);
            text(c,h.s("name"),22,true);
            gap(c,8);
            text(c,h.s("rows_count")+" рядов × "+h.s("cols_count")+" мест = "+(h.i("rows_count")*h.i("cols_count"))+" мест\n"+(h.i("active")==1?"Активен":"Архив"),14,false);
            button(c,"Изменить зал",WHITE,()->hallDialog(h));
            button(c,h.i("active")==1?"В архив":"Восстановить",PAPER,()-> {
                db.archive(token,"halls",h.l("id"),h.i("active")==1);
                render();
            }
            );
        }
    }
    private void hallDialog(Row h) {
        LinearLayout p=col();
        EditText name=field(p,"Название зала",h==null?"":h.s("name"),1),rows=field(p,"Количество рядов",h==null?"6":h.s("rows_count"),2),cols=field(p,"Мест в ряду",h==null?"8":h.s("cols_count"),2);
        text(p,"Изменение размеров применяется к новым и редактируемым сеансам. Схемы существующих сеансов сохраняются.",13,false);
        form(h==null?"Новый зал":"Редактировать зал",p,()-> {
            db.saveHall(token,h==null?0:h.l("id"),value(name),number(rows),number(cols));
            render();
        }
        );
    }
    private void showsAdmin() {
        back();
        title("РАСПИСАНИЕ И ЦЕНЫ","Сеансы","Пересечения в одном зале автоматически блокируются.");
        button(content,"+ Создать сеанс",YELLOW,()->showDialog(null));
        LinearLayout list=col();
        dateStrip(content,()->drawAdminShows(list));
        gap(content,18);
        content.addView(list);
        drawAdminShows(list);
    }
    private void drawAdminShows(LinearLayout list) {
        list.removeAllViews();
        List<Row> sessions=db.shows(0,selectedDay,day(selectedDay,1),true);
        for(Row s:sessions) {
            LinearLayout c=card(list,WHITE);
            caption(c,date(s.l("starts_at"),"HH:mm")+" / "+s.s("status"));
            gap(c,8);
            text(c,s.s("title"),23,true);
            gap(c,9);
            text(c,s.s("hall_name")+" · "+s.s("format")+"\n"+s.s("language")+" · "+money(s.l("price"))+" · свободно "+s.s("free"),13,false);
            if(s.s("status").equals("ACTIVE")&&s.l("starts_at")>System.currentTimeMillis()) {
                button(c,"Изменить время / цену",WHITE,()->showDialog(s));
                button(c,"Отменить сеанс",PAPER,()->confirm("Отменить весь сеанс?","Все бронирования будут отменены, места освобождены. Оплаченные билеты возвращаются через кассу.",()-> {
                    db.cancelShow(token,s.l("id"));
                    render();
                }
                ));
            }
        }
        if(sessions.isEmpty())empty(list,"Расписание свободно","Создайте сеанс или выберите другую дату.");
    }
    private void showDialog(Row s) {
        List<Row> movies=db.movies(false),halls=db.halls(false);
        require(!movies.isEmpty()&&!halls.isEmpty(),"Сначала добавьте активный фильм и зал");
        LinearLayout p=col();
        List<String> titles=new ArrayList<>(),names=new ArrayList<>();
        int mi=0,hi=0;
        for(int i=0; i<movies.size(); i++) {
            titles.add(movies.get(i).s("title"));
            if(s!=null&&movies.get(i).l("id")==s.l("movie_id"))mi=i;
        }
        for(int i=0; i<halls.size(); i++) {
            names.add(halls.get(i).s("name"));
            if(s!=null&&halls.get(i).l("id")==s.l("hall_id"))hi=i;
        }
        Spinner movie=spinner(p,"Фильм",titles,mi),hall=spinner(p,"Зал",names,hi);
        Calendar time=Calendar.getInstance();
        time.setTimeInMillis(s==null?Math.max(day(selectedDay,0)+18*3600000L,System.currentTimeMillis()+3600000L):s.l("starts_at"));
        time.set(Calendar.SECOND,0);
        time.set(Calendar.MILLISECOND,0);
        Button dateButton=button(p,date(time.getTimeInMillis(),"dd.MM.yyyy"),WHITE,()-> {
        }
        ),timeButton=button(p,date(time.getTimeInMillis(),"HH:mm"),WHITE,()-> {
        }
        );
        dateButton.setOnClickListener(v-> {
            DatePickerDialog d=new DatePickerDialog(this,(picker,y,m,day)-> {
                time.set(y,m,day);
                dateButton.setText(date(time.getTimeInMillis(),"dd.MM.yyyy"));
            }
            ,time.get(Calendar.YEAR),time.get(Calendar.MONTH),time.get(Calendar.DAY_OF_MONTH));
            d.getDatePicker().setMinDate(day(System.currentTimeMillis(),0));
            d.show();
        }
        );
        timeButton.setOnClickListener(v->new TimePickerDialog(this,(picker,h,m)-> {
            time.set(Calendar.HOUR_OF_DAY,h);
            time.set(Calendar.MINUTE,m);
            timeButton.setText(date(time.getTimeInMillis(),"HH:mm"));
        }
        ,time.get(Calendar.HOUR_OF_DAY),time.get(Calendar.MINUTE),true).show());
        EditText price=field(p,"Цена одного места в тенге",s==null?"2500":s.s("price"),2);
        Spinner format=spinner(p,"Формат сеанса",new String[] {
            "2D","3D","ATMOS"
        }
        ,s==null?0:Arrays.asList("2D","3D","ATMOS").indexOf(s.s("format")));
        EditText language=field(p,"Язык сеанса",s==null?"Русский":s.s("language"),1);
        text(p,"Между показами предусмотрено 20 минут на уборку. Сеанс с действующими бронированиями нельзя переносить.",13,false);
        form(s==null?"Создать сеанс":"Изменить сеанс",p,()-> {
            db.saveShow(token,s==null?0:s.l("id"),movies.get(movie.getSelectedItemPosition()).l("id"),halls.get(hall.getSelectedItemPosition()).l("id"),time.getTimeInMillis(),number(price),format.getSelectedItem().toString(),value(language));
            selectedDay=day(time.getTimeInMillis(),0);
            render();
        }
        );
    }
    private void promosAdmin() {
        back();
        title("СПЕЦИАЛЬНЫЕ ПРЕДЛОЖЕНИЯ","Промокоды","Скидка применяется к полной стоимости брони.");
        button(content,"+ Создать промокод",YELLOW,()->promoDialog(null));
        for(Row r:db.promos(token)) {
            LinearLayout c=card(content,WHITE);
            text(c,r.s("code")+" / −"+r.s("percent")+"%",24,true);
            gap(c,8);
            caption(c,r.i("active")==1?"АКТИВЕН":"ОТКЛЮЧЁН");
            button(c,"Редактировать",WHITE,()->promoDialog(r));
            button(c,r.i("active")==1?"Отключить":"Включить",PAPER,()-> {
                db.savePromo(token,r.s("code"),r.i("percent"),r.i("active")!=1);
                render();
            }
            );
        }
    }
    private void promoDialog(Row r) {
        LinearLayout p=col();
        EditText code=field(p,"Код",r==null?"":r.s("code"),1),percent=field(p,"Скидка в процентах",r==null?"10":r.s("percent"),2);
        if(r!=null)code.setEnabled(false);
        form(r==null?"Новый промокод":"Изменить промокод",p,()-> {
            db.savePromo(token,value(code),number(percent),r==null||r.i("active")==1);
            render();
        }
        );
    }
}
