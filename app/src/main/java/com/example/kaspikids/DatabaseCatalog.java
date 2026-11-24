package com.example.kaspikids;

import java.util.*;
import static com.example.kaspikids.CinemaRules.require;
import com.example.kaspikids.CinemaDb.Row;

/** Read-only, authenticated database inspection. Identifiers come only from SQLite metadata. */
public final class DatabaseCatalog {
    public static final int PAGE_SIZE = 50;
    private final CinemaDb db;
    private final String token;
    public DatabaseCatalog(CinemaDb db, String token) { this.db=db; this.token=token; }
    private Row user() { Row u=db.currentUser(token); require(u!=null,"Сессия истекла. Войдите снова"); return u; }
    public boolean isAdmin() { return user().s("role").equals("ADMIN"); }
    private void admin() { require(isAdmin(),"Просмотр записей доступен только администратору"); }
    private static String quote(String s) { return "\""+s.replace("\"","\"\"")+"\""; }
    public List<Row> tables() {
        user();
        List<Row> result=db.query("SELECT name,sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name<>'android_metadata' ORDER BY name");
        boolean privileged=isAdmin();
        for(Row t:result) {
            t.put("columns",""+columns(t.s("name")).size());
            t.put("count",privileged?db.query("SELECT COUNT(*) n FROM "+quote(t.s("name"))).get(0).s("n"):"—");
        }
        return result;
    }
    private void table(String name) {
        user(); require(!db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=? AND name NOT LIKE 'sqlite_%' AND name<>'android_metadata'",name).isEmpty(),"Таблица не найдена");
    }
    public List<Row> columns(String name) { table(name); return db.query("PRAGMA table_info("+quote(name)+")"); }
    public List<Row> foreignKeys(String name) { table(name); return db.query("PRAGMA foreign_key_list("+quote(name)+")"); }
    public List<Row> indexes(String name) { table(name); return db.query("PRAGMA index_list("+quote(name)+")"); }
    public String indexColumns(String index) {
        user(); require(!db.query("SELECT name FROM sqlite_master WHERE type='index' AND name=?",index).isEmpty(),"Индекс не найден");
        List<String> fields=new ArrayList<>();for(Row r:db.query("PRAGMA index_info("+quote(index)+")"))fields.add(r.s("name"));return CinemaRules.join(", ",fields);
    }
    public String ddl(String name) { table(name);return db.query("SELECT sql FROM sqlite_master WHERE name=? AND type='table'",name).get(0).s("sql"); }
    public String schemaSql() {
        user();StringBuilder b=new StringBuilder("-- KADR SQLite schema\nPRAGMA foreign_keys = ON;\n\n");
        for(Row r:db.query("SELECT sql FROM sqlite_master WHERE type IN ('table','index') AND name NOT LIKE 'sqlite_%' AND name<>'android_metadata' AND sql IS NOT NULL ORDER BY type DESC,name"))b.append(r.s("sql")).append(";\n\n");return b.toString();
    }
    public static boolean secret(String table,String column) {
        return (table.equals("users")&&(column.equals("password_hash")||column.equals("salt"))) || (table.equals("sessions")&&column.equals("token_hash"));
    }
    public static final class Page {
        public final List<Row> rows; public final List<String> columns; public final int total,number;
        Page(List<Row> r,List<String> c,int total,int number){this.rows=r;this.columns=c;this.total=total;this.number=number;}
    }
    public Page rows(String name,String term,String order,boolean descending,int page) {
        admin();table(name);require(page>=0&&page<=100000,"Некорректная страница");
        List<String> fields=new ArrayList<>(),select=new ArrayList<>(),where=new ArrayList<>(),args=new ArrayList<>();
        for(Row c:columns(name)) {
            String field=c.s("name");fields.add(field);
            select.add(secret(name,field)?"'[скрыто]' AS "+quote(field):quote(field));
            if(!secret(name,field)&&!term.trim().isEmpty()) {where.add("CAST("+quote(field)+" AS TEXT) LIKE ? ESCAPE '\\'");args.add("%"+term.trim().replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%");}
        }
        if(order==null||order.isEmpty())for(String f:fields)if(!secret(name,f)){order=f;break;}
        require(fields.contains(order)&&!secret(name,order),"Недопустимый столбец сортировки");
        String condition=where.isEmpty()?"":" WHERE ("+CinemaRules.join(" OR ",where)+")";
        String[] parameters=args.toArray(new String[0]);
        int total=db.query("SELECT COUNT(*) n FROM "+quote(name)+condition,parameters).get(0).i("n");
        List<Row> records=db.query("SELECT "+CinemaRules.join(",",select)+" FROM "+quote(name)+condition+" ORDER BY "+quote(order)+(descending?" DESC":" ASC")+" LIMIT "+PAGE_SIZE+" OFFSET "+(page*PAGE_SIZE),parameters);
        return new Page(records,fields,total,page);
    }
    public String checks() {
        user();StringBuilder result=new StringBuilder();
        List<Row> quick=db.query("PRAGMA quick_check");boolean ok=quick.size()==1&&quick.get(0).values().contains("ok");
        result.append("Целостность SQLite: ").append(ok?"OK":"обнаружены ошибки").append("\n");
        result.append("Нарушения внешних ключей: ").append(db.query("PRAGMA foreign_key_check").size()).append("\n");
        result.append("Контроль внешних ключей: ").append(db.query("PRAGMA foreign_keys").get(0).values().contains("1")?"включён":"выключен").append("\n");
        result.append("Версия схемы: ").append(db.getReadableDatabase().getVersion()).append("\n");
        result.append("Файл: ").append(CinemaDb.NAME).append("\nРазмер: ").append(new java.io.File(db.getReadableDatabase().getPath()).length()).append(" байт\n");
        return result.toString();
    }
    public static String csv(Page p) {
        StringBuilder b=new StringBuilder("\ufeff");csvRow(b,p.columns);
        for(Row r:p.rows){List<String> line=new ArrayList<>();for(String c:p.columns)line.add(r.s(c));csvRow(b,line);}return b.toString();
    }
    private static void csvRow(StringBuilder b,List<String> values) {
        for(int i=0;i<values.size();i++) {if(i>0)b.append(',');String v=values.get(i);if(!v.isEmpty()&&"=+@-\t\r".indexOf(v.charAt(0))>=0)v="'"+v;b.append('"').append(v.replace("\"","\"\"")).append('"');}b.append("\r\n");
    }
}
