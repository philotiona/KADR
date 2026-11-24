package com.example.kaspikids;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.example.kaspikids.CinemaDb.Row;

/** Embedded database workspace. Schema is public to signed-in users; rows are admin-only. */
public final class DatabaseScreen extends LinearLayout {
    public static final int EXPORT_REQUEST=802;
    private final Activity host;
    private final DatabaseCatalog catalog;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final LinearLayout main;
    private LinearLayout results;
    private String section="ER",table="movies",term="",order="",pendingText="",pendingKind="";
    private boolean records=false,descending=false;
    private int page=0;
    private ErDiagramView graph;
    private Bundle graphState;
    private final Map<String,Button> tabButtons=new HashMap<>();
    private final int PAPER=0xfff5f1e8,INK=0xff191a1b,WHITE=0xfffffdf8,YELLOW=0xfff5cc55;
    public DatabaseScreen(Activity host,CinemaDb db,String token,Bundle state) {
        super(host);this.host=host;catalog=new DatabaseCatalog(db,token);setOrientation(VERTICAL);setBackgroundColor(PAPER);
        if(state!=null){graphState=state.getBundle("graph");section=state.getString("section","ER");table=state.getString("table","movies");term=state.getString("term","");order=state.getString("order","");records=state.getBoolean("records");descending=state.getBoolean("descending");page=state.getInt("page");pendingText=state.getString("pendingText","");pendingKind=state.getString("pendingKind","");}
        LinearLayout header=col();header.setPadding(d(16),d(14),d(16),d(8));addView(header);label(header,"База данных",25,true);label(header,CinemaDb.NAME+" · живая схема SQLite",12,false);
        LinearLayout tabs=row();header.addView(tabs);for(String name:new String[]{"ER","Таблицы","Проверки"})tabButtons.put(name,action(tabs,name,name.equals(section)?YELLOW:WHITE,()->{section=name;render();},true));
        main=col();addView(main,new LayoutParams(-1,0,1));render();
    }
    public Bundle saveState(){Bundle b=new Bundle();if(graph!=null)graphState=graph.saveState();b.putBundle("graph",graphState);b.putString("section",section);b.putString("table",table);b.putString("term",term);b.putString("order",order);b.putBoolean("records",records);b.putBoolean("descending",descending);b.putInt("page",page);b.putString("pendingText",pendingText);b.putString("pendingKind",pendingKind);return b;}
    private int d(int n){return Ui.dp(host,n);}
    private LinearLayout col(){LinearLayout l=new LinearLayout(host);l.setOrientation(VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(host);l.setOrientation(HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView label(LinearLayout p,String s,int size,boolean bold){TextView t=new TextView(host);t.setText(s);t.setTextSize(size);t.setTextColor(INK);t.setTypeface(null,bold?Typeface.BOLD:Typeface.NORMAL);t.setPadding(0,d(5),0,d(5));p.addView(t,new LayoutParams(-1,-2));return t;}
    private GradientDrawable background(int color){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(d(6));b.setStroke(d(1),0xffb7b3aa);return b;}
    private Button action(LinearLayout p,String text,int color,Runnable work,boolean weighted){Button b=new Button(host);b.setText(text);b.setTextSize(12);b.setAllCaps(false);b.setMinHeight(d(44));b.setMinimumHeight(d(44));b.setPadding(d(7),d(5),d(7),d(5));b.setTextColor(INK);b.setBackground(background(color));LayoutParams lp=new LayoutParams(weighted?0:-1,-2,weighted?1:0);lp.setMargins(d(2),d(5),d(2),d(5));p.addView(b,lp);b.setOnClickListener(v->safe(work));return b;}
    private EditText search(LinearLayout p,String hint,String initial){EditText input=new EditText(host);input.setSingleLine(true);input.setTextSize(14);input.setTextColor(INK);input.setHint(hint);input.setContentDescription(hint);input.setText(initial);input.setPadding(d(12),d(8),d(12),d(8));input.setBackground(background(WHITE));p.addView(input,new LayoutParams(-1,d(44)));return input;}
    private void change(EditText e,Runnable work){e.addTextChangedListener(new TextWatcher(){Runnable pending;public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){if(pending!=null)handler.removeCallbacks(pending);pending=()->safe(work);handler.postDelayed(pending,180);}public void afterTextChanged(Editable e){}});}
    private void safe(Runnable r){try{r.run();}catch(Exception e){new AlertDialog.Builder(host).setTitle("База данных").setMessage(e.getMessage()==null?"Не удалось прочитать данные":e.getMessage()).setPositiveButton("Понятно",null).show();}}
    private void copy(String text){((android.content.ClipboardManager)host.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("KADR schema",text));Toast.makeText(host,"Скопировано",Toast.LENGTH_SHORT).show();}
    private void render(){handler.removeCallbacksAndMessages(null);safe(()->{for(Map.Entry<String,Button> t:tabButtons.entrySet())t.getValue().setBackground(background(t.getKey().equals(section)?YELLOW:WHITE));if(graph!=null){graphState=graph.saveState();graph=null;}main.removeAllViews();if(section.equals("ER"))diagram();else if(section.equals("Таблицы"))tables();else checks();});}
    private void toolbar(LinearLayout p){LinearLayout tools=row();p.addView(tools);action(tools,"Обновить",WHITE,this::render,true);action(tools,"Экспорт SQL",YELLOW,()->export("sql",catalog.schemaSql()),true);}
    private void diagram(){
        LinearLayout tools=col();tools.setPadding(d(16),0,d(16),d(5));main.addView(tools);
        EditText find=search(tools,"Найти таблицу или столбец","");
        LinearLayout controls=row();tools.addView(controls);action(controls,"−",WHITE,()->graph.zoom(.8f),true);action(controls,"Вместить",YELLOW,()->graph.fit(),true);action(controls,"+",WHITE,()->graph.zoom(1.25f),true);
        List<String> names=new ArrayList<>();names.add("Перейти к таблице…");for(Row t:catalog.tables())names.add(t.s("name"));
        Spinner jump=new Spinner(host);jump.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,names));tools.addView(jump,new LayoutParams(-1,d(42)));jump.setContentDescription("Перейти к таблице");
        graph=new ErDiagramView(host,catalog,this::tableDialog);graph.restoreState(graphState);main.addView(graph,new LayoutParams(-1,0,1));
        jump.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(pos>0)graph.focusTable(names.get(pos));}});
        change(find,()->graph.search(find.getText().toString()));
        LinearLayout footer=col();footer.setPadding(d(16),0,d(16),0);main.addView(footer);label(footer,"Два пальца — масштаб · Проведите — перемещение\nНажмите таблицу для схемы и записей. PK / FK; связи 1 : 0..N.",11,false);toolbar(footer);
    }
    private void tableDialog(String name){table=name;StringBuilder s=new StringBuilder();s.append(catalog.columns(name).size()).append(" столбцов · ").append(catalog.foreignKeys(name).size()).append(" внешних ключей\n\n");for(Row f:catalog.foreignKeys(name))s.append(f.s("from")).append(" → ").append(f.s("table")).append('.').append(f.s("to")).append("\n");if(catalog.foreignKeys(name).isEmpty())s.append("Исходящих внешних ключей нет.");AlertDialog.Builder b=new AlertDialog.Builder(host).setTitle(name).setMessage(s.toString()).setNegativeButton("Закрыть",null).setPositiveButton("Схема",(v,w)->{records=false;section="Таблицы";page=0;order="";render();});if(catalog.isAdmin())b.setNeutralButton("Записи",(v,w)->{records=true;section="Таблицы";page=0;term="";order="";render();});b.show();}
    private void tables(){
        LinearLayout tools=col();tools.setPadding(d(16),0,d(16),0);main.addView(tools);
        List<Row> ts=catalog.tables();List<String> names=new ArrayList<>();for(Row r:ts)names.add(r.s("name"));if(!names.contains(table))table=names.get(0);
        Spinner picker=new Spinner(host);picker.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,names));picker.setSelection(names.indexOf(table));picker.setContentDescription("Выбор таблицы");tools.addView(picker,new LayoutParams(-1,d(44)));
        picker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(!table.equals(names.get(pos))){table=names.get(pos);page=0;order="";term="";render();}}});
        LinearLayout modes=row();tools.addView(modes);action(modes,"Схема",records?WHITE:YELLOW,()->{records=false;render();},true);Button data=action(modes,"Записи",records?YELLOW:WHITE,()->{records=true;page=0;render();},true);data.setEnabled(catalog.isAdmin());
        if(!catalog.isAdmin())label(tools,"Схема доступна всем. Записи — только администратору.",11,false);
        if(records&&catalog.isAdmin()) {
            EditText input=search(tools,"Поиск по значениям",term);change(input,()->{term=input.getText().toString();page=0;drawRows();});
            List<String> columns=new ArrayList<>();for(Row c:catalog.columns(table))if(!DatabaseCatalog.secret(table,c.s("name")))columns.add(c.s("name"));if(!columns.contains(order))order=columns.get(0);
            LinearLayout sorting=row();tools.addView(sorting);Spinner sort=new Spinner(host);sort.setContentDescription("Столбец сортировки");sort.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,columns));sort.setSelection(columns.indexOf(order));sorting.addView(sort,new LayoutParams(0,d(44),2));
            action(sorting,descending?"↓ DESC":"↑ ASC",WHITE,()->{descending=!descending;render();},true);
            sort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(!order.equals(columns.get(pos))){order=columns.get(pos);page=0;drawRows();}}});
            results=col();main.addView(results,new LayoutParams(-1,0,1));drawRows();
        } else {records=false;ScrollView scroll=new ScrollView(host);LinearLayout schema=col();schema.setPadding(d(16),d(5),d(16),d(12));scroll.addView(schema);main.addView(scroll,new LayoutParams(-1,0,1));schema(schema);}
        toolbar(tools);
    }
    private void schema(LinearLayout box){
        label(box,table+" · "+catalog.columns(table).size()+" столбцов",20,true);
        for(Row c:catalog.columns(table)){String title=c.s("name")+" : "+c.s("type")+(c.i("pk")>0?"  [PK "+c.s("pk")+"]":"");label(box,title,14,true);label(box,(c.i("pk")>0?"PRIMARY KEY · позиция "+c.s("pk"):(c.i("notnull")==1?"NOT NULL":"NULL допустим"))+(c.s("dflt_value").isEmpty()?"":" · DEFAULT "+c.s("dflt_value")),12,false);}
        label(box,"Внешние ключи · 1 : 0..N",18,true);List<Row> fks=catalog.foreignKeys(table);if(fks.isEmpty())label(box,"Исходящих FK нет",13,false);for(Row f:fks){String parent=f.s("table");action(box,f.s("from")+" → "+parent+"."+f.s("to"),WHITE,()->{table=parent;page=0;order="";render();},false);}
        label(box,"Индексы и уникальность",18,true);for(Row idx:catalog.indexes(table))label(box,(idx.i("unique")==1?"UNIQUE ":"INDEX ")+idx.s("name")+"\n("+catalog.indexColumns(idx.s("name"))+")",12,false);
        action(box,"Показать CREATE TABLE",YELLOW,()->new AlertDialog.Builder(host).setTitle(table).setMessage(catalog.ddl(table)).setNeutralButton("Скопировать",(v,w)->copy(catalog.ddl(table))).setPositiveButton("Закрыть",null).show(),false);
    }
    private void drawRows(){safe(()->{
        if(results==null||!section.equals("Таблицы")||!records)return;DatabaseCatalog.Page data=catalog.rows(table,term,order,descending,page);results.removeAllViews();
        LinearLayout top=col();top.setPadding(d(16),0,d(16),0);results.addView(top);label(top,table+" · найдено "+data.total+" · страница "+(page+1),13,true);label(top,"Прокрутите по горизонтали. Нажмите строку для деталей.\nХеши паролей, соли и токены скрыты.",11,false);
        LinearLayout paging=row();top.addView(paging);action(paging,"←",WHITE,()->{page--;drawRows();},true).setEnabled(page>0);action(paging,"CSV страницы",YELLOW,()->export("csv",DatabaseCatalog.csv(data)),true);action(paging,"→",WHITE,()->{page++;drawRows();},true).setEnabled((page+1)*DatabaseCatalog.PAGE_SIZE<data.total);
        HorizontalScrollView horizontal=new HorizontalScrollView(host);ScrollView vertical=new ScrollView(host);LinearLayout grid=col();vertical.addView(grid);horizontal.addView(vertical,new HorizontalScrollView.LayoutParams(-2,-1));results.addView(horizontal,new LayoutParams(-1,0,1));
        LinearLayout headers=row();headers.setBackgroundColor(YELLOW);grid.addView(headers);for(String field:data.columns)cell(headers,field,true);
        if(data.rows.isEmpty())label(grid,"Записей нет. Измените поиск или таблицу.",14,false);
        for(Row r:data.rows){LinearLayout line=row();line.setBackgroundColor(grid.getChildCount()%2==0?PAPER:WHITE);grid.addView(line);for(String field:data.columns)cell(line,r.s(field),false);line.setOnClickListener(v->{StringBuilder b=new StringBuilder();for(String f:data.columns)b.append(f).append("\n").append(r.s(f)).append("\n\n");new AlertDialog.Builder(host).setTitle(table+" · запись").setMessage(b.toString()).setPositiveButton("Закрыть",null).show();});}
    });}
    private void cell(LinearLayout p,String text,boolean bold){TextView t=new TextView(host);t.setText(text);t.setTextColor(INK);t.setTextSize(12);t.setTypeface(Typeface.MONOSPACE,bold?Typeface.BOLD:Typeface.NORMAL);t.setPadding(d(10),d(10),d(10),d(10));t.setMaxLines(3);t.setEllipsize(TextUtils.TruncateAt.END);p.addView(t,new LayoutParams(d(152),d(bold?48:76)));}
    private void checks(){LinearLayout box=col();box.setPadding(d(16),d(8),d(16),d(12));ScrollView s=new ScrollView(host);s.addView(box);main.addView(s,new LayoutParams(-1,0,1));label(box,"Проверки SQLite",22,true);label(box,"Проверка целостности и внешних ключей читает текущую БД. Изменения записей выполняются через обычные разделы приложения.",13,false);TextView result=label(box,"Нажмите «Проверить базу».",15,false);Button check=action(box,"Проверить базу",YELLOW,()->{},false);check.setOnClickListener(v->{check.setEnabled(false);result.setText("Проверяем…");new Thread(()->{try{String text=catalog.checks();host.runOnUiThread(()->{if(!host.isDestroyed()){result.setText(text);check.setEnabled(true);}});}catch(Exception e){host.runOnUiThread(()->{check.setEnabled(true);result.setText(e.getMessage());});}},"kadr-db-check").start();});
        label(box,"Связи и объём",18,true);for(Row t:catalog.tables()){label(box,t.s("name")+" · "+t.s("columns")+" столбцов · "+t.s("count")+" записей",14,true);for(Row fk:catalog.foreignKeys(t.s("name")))label(box,"  "+fk.s("from")+" → "+fk.s("table")+"."+fk.s("to"),12,false);}toolbar(box);
    }
    private void export(String kind,String text){if(kind.equals("csv")&&!catalog.isAdmin())throw new IllegalArgumentException("Только администратор");pendingKind=kind;pendingText=text;host.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(kind.equals("csv")?"text/csv":"application/sql").putExtra(Intent.EXTRA_TITLE,kind.equals("csv")?"KADR-"+table+"-page-"+(page+1)+".csv":"KADR-schema.sql"),EXPORT_REQUEST);}
    public boolean onActivityResult(int request,int result,Intent data){if(request!=EXPORT_REQUEST)return false;if(result==Activity.RESULT_OK&&data!=null&&data.getData()!=null)safe(()->{catalog.tables();if(pendingKind.equals("csv")&&!catalog.isAdmin())throw new IllegalArgumentException("Доступ к записям закрыт");try(OutputStream out=host.getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IllegalArgumentException("Не удалось открыть файл");out.write(pendingText.getBytes(StandardCharsets.UTF_8));Toast.makeText(host,"Файл сохранён",Toast.LENGTH_SHORT).show();}catch(java.io.IOException e){throw new IllegalArgumentException("Ошибка сохранения файла");}});pendingText="";pendingKind="";return true;}
    @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();handler.removeCallbacksAndMessages(null);}
}
