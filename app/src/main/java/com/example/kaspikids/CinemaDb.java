package com.example.kaspikids;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
import static com.example.kaspikids.CinemaRules.*;
/** SQLite is the data boundary: protected operations validate the persisted session and role. */
public final class CinemaDb extends SQLiteOpenHelper {
    public static final String NAME="kadr_cinema.db";
    public static final class Row extends HashMap<String,String> {
        public String s(String k) {
            String v=get(k);
            return v==null?"":v;
        }
        public long l(String k) {
            try {
                return Long.parseLong(s(k));
            }
            catch(Exception e) {
                return 0;
            }
        }
        public int i(String k) {
            return (int)l(k);
        }
    }
    public CinemaDb(Context c) {
        this(c,NAME);
    }
    public CinemaDb(Context c,String n) {
        super(c,n,null,1);
    }
    @Override public void onConfigure(SQLiteDatabase d) {
        d.setForeignKeyConstraintsEnabled(true);
    }
    @Override public void onUpgrade(SQLiteDatabase d,int a,int b) {
        throw new IllegalStateException("Unsupported migration");
    }
    @Override public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY,username TEXT UNIQUE NOT NULL COLLATE NOCASE,full_name TEXT NOT NULL,password_hash TEXT NOT NULL,salt TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN ('ADMIN','USER')),active INTEGER NOT NULL DEFAULT 1,email TEXT NOT NULL DEFAULT '',phone TEXT NOT NULL DEFAULT '')");
        d.execSQL("CREATE TABLE sessions(token_hash TEXT PRIMARY KEY,user_id INTEGER NOT NULL REFERENCES users(id),expires_at INTEGER NOT NULL)");
        d.execSQL("CREATE TABLE movies(id INTEGER PRIMARY KEY,title TEXT NOT NULL,genre TEXT NOT NULL,duration INTEGER NOT NULL CHECK(duration BETWEEN 30 AND 300),age INTEGER NOT NULL,synopsis TEXT NOT NULL,director TEXT NOT NULL,year INTEGER NOT NULL,art INTEGER NOT NULL DEFAULT 0,active INTEGER NOT NULL DEFAULT 1)");
        d.execSQL("CREATE TABLE halls(id INTEGER PRIMARY KEY,name TEXT UNIQUE NOT NULL COLLATE NOCASE,rows_count INTEGER NOT NULL CHECK(rows_count BETWEEN 3 AND 12),cols_count INTEGER NOT NULL CHECK(cols_count BETWEEN 4 AND 12),active INTEGER NOT NULL DEFAULT 1)");
        d.execSQL("CREATE TABLE shows(id INTEGER PRIMARY KEY,movie_id INTEGER NOT NULL REFERENCES movies(id),hall_id INTEGER NOT NULL REFERENCES halls(id),starts_at INTEGER NOT NULL,ends_at INTEGER NOT NULL,price INTEGER NOT NULL CHECK(price BETWEEN 0 AND 100000),format TEXT NOT NULL,language TEXT NOT NULL,rows_count INTEGER NOT NULL,cols_count INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'ACTIVE')");
        d.execSQL("CREATE INDEX shows_time ON shows(starts_at,status)");
        d.execSQL("CREATE TABLE bookings(id INTEGER PRIMARY KEY,code TEXT NOT NULL UNIQUE,user_id INTEGER NOT NULL REFERENCES users(id),show_id INTEGER NOT NULL REFERENCES shows(id),created_at INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'BOOKED',movie_title TEXT NOT NULL,hall_name TEXT NOT NULL,starts_at INTEGER NOT NULL,seats TEXT NOT NULL,total INTEGER NOT NULL,format TEXT NOT NULL,promo TEXT NOT NULL DEFAULT '',discount INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE INDEX bookings_user ON bookings(user_id,created_at)");
        d.execSQL("CREATE TABLE booking_seats(booking_id INTEGER NOT NULL REFERENCES bookings(id),show_id INTEGER NOT NULL REFERENCES shows(id),row_no INTEGER NOT NULL,seat_no INTEGER NOT NULL,UNIQUE(show_id,row_no,seat_no))");
        d.execSQL("CREATE TABLE favorites(user_id INTEGER NOT NULL REFERENCES users(id),movie_id INTEGER NOT NULL REFERENCES movies(id),PRIMARY KEY(user_id,movie_id))");
        d.execSQL("CREATE TABLE reviews(user_id INTEGER NOT NULL REFERENCES users(id),movie_id INTEGER NOT NULL REFERENCES movies(id),score INTEGER NOT NULL CHECK(score BETWEEN 1 AND 5),comment TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(user_id,movie_id))");
        d.execSQL("CREATE TABLE promos(code TEXT PRIMARY KEY COLLATE NOCASE,percent INTEGER NOT NULL CHECK(percent BETWEEN 1 AND 90),active INTEGER NOT NULL DEFAULT 1)");
        addUser(d,"admin","Администратор","Admin123!","ADMIN");
        addUser(d,"viewer","Александр","Cinema123!","USER");
        String[][] films= {
            {
                "Орбита","Фантастика","142","12","Экипаж исследовательской станции получает сигнал из области, где не должно быть жизни. Чтобы вернуться домой, им придётся изменить представление о времени.","Анна Миронова","2026"
            }
            , {
                "Город после полуночи","Триллер","118","16","Ночной курьер находит плёнку с записью завтрашнего преступления. До рассвета он должен найти незнакомку из кадра и решить, кому можно доверять.","Тимур Асанов","2026"
            }
            , {
                "Лето на кассете","Комедия","104","12","Трое друзей едут к морю на старом микроавтобусе. У них одна кассета, сломанный навигатор и обещание впервые ничего не планировать.","Мария Волкова","2026"
            }
            , {
                "Тихий океан","Драма","126","12","Пианист возвращается в родной приморский город. Письма матери и знакомый звук прибоя помогают ему услышать себя заново.","Данияр Ким","2026"
            }
            , {
                "Лис и облако","Анимация","92","0","Маленький лис отправляется за исчезнувшим дождём вместе с облаком, которое боится высоты. Тёплая история о смелости, дружбе и доме.","Алина Соколова","2026"
            }
            , {
                "Последний маршрут","Приключения","132","12","Картограф находит незавершённый маршрут своего отца в горах Тянь-Шаня. Экспедиция становится путешествием к ответам, которые он давно перестал искать.","Руслан Сериков","2026"
            }
        }
        ;
        for(int i=0; i<films.length; i++) {
            String[] f=films[i];
            d.execSQL("INSERT INTO movies(title,genre,duration,age,synopsis,director,year,art) VALUES(?,?,?,?,?,?,?,?)",new Object[] {
                f[0],f[1],f[2],f[3],f[4],f[5],f[6],i
            }
            );
        }
        d.execSQL("INSERT INTO halls(name,rows_count,cols_count) VALUES('Зал 01 · STANDARD',6,8),('Зал 02 · ATMOS',7,8),('Зал 03 · CLUB',4,6)");
        d.execSQL("INSERT INTO promos(code,percent) VALUES('STUDENT',15),('FIRST',10)");
        long now=System.currentTimeMillis();
        for(int offset=0; offset<8; offset++)for(int hall=1; hall<=3; hall++)for(int slot=0; slot<4; slot++) {
            int m=(offset+hall+slot)%6;
            long start=day(now,offset)+(10+slot*4)*3600000L;
            if(start<=now)continue;
            d.execSQL("INSERT INTO shows(movie_id,hall_id,starts_at,ends_at,price,format,language,rows_count,cols_count) VALUES(?,?,?,?,?,?,?,?,?)",new Object[] {
                m+1,hall,start,start+(Integer.parseInt(films[m][2])+20)*60000L,hall==3?3500:2200+slot*200,hall==2?"ATMOS":"2D","Русский",hall==3?4:hall==2?7:6,hall==3?6:8
            }
            );
        }
    }
    private static void addUser(SQLiteDatabase d,String login,String name,String pass,String role) {
        String salt=salt();
        d.execSQL("INSERT INTO users(username,full_name,password_hash,salt,role) VALUES(?,?,?,?,?)",new Object[] {
            login,name,hash(pass,salt),salt,role
        }
        );
    }
    public List<Row> query(String sql,String...args) {
        List<Row> rows=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery(sql,args)) {
            while(c.moveToNext()) {
                Row r=new Row();
                for(int i=0; i<c.getColumnCount(); i++)r.put(c.getColumnName(i),c.isNull(i)?"":c.getString(i));
                rows.add(r);
            }
        }
        return rows;
    }
    private Row one(String sql,String...args) {
        List<Row> r=query(sql,args);
        return r.isEmpty()?null:r.get(0);
    }
    public synchronized String loginUser(String username,String pass) {
        Row r=one("SELECT * FROM users WHERE username=?",login(username));
        boolean match=matches(pass,r==null?"00000000000000000000000000000000":r.s("salt"),r==null?"0":r.s("password_hash"));
        require(r!=null&&match&&r.i("active")==1,"Неверный логин или пароль, либо аккаунт отключён");
        String token=UUID.randomUUID().toString()+UUID.randomUUID();
        long now=System.currentTimeMillis();
        getWritableDatabase().delete("sessions","expires_at<?",new String[] {
            ""+now
        }
        );
        getWritableDatabase().execSQL("INSERT INTO sessions VALUES(?,?,?)",new Object[] {
            digest(token),r.l("id"),now+30L*86400000
        }
        );
        return token;
    }
    public Row currentUser(String t) {
        if(t==null||t.isEmpty())return null;
        return one("SELECT u.id,u.username,u.full_name,u.role,u.active,u.email,u.phone FROM users u JOIN sessions s ON s.user_id=u.id WHERE s.token_hash=? AND s.expires_at>? AND u.active=1",digest(t),""+System.currentTimeMillis());
    }
    private Row user(String t) {
        Row u=currentUser(t);
        require(u!=null,"Сессия истекла. Войдите снова");
        return u;
    }
    private Row admin(String t) {
        Row u=user(t);
        require(u.s("role").equals("ADMIN"),"Доступно только администратору");
        return u;
    }
    public void logout(String t) {
        getWritableDatabase().delete("sessions","token_hash=?",new String[] {
            digest(t)
        }
        );
    }
    public synchronized void register(String t,String username,String fullName,String pass,String role) {
        admin(t);
        username=login(username);
        fullName=name(fullName);
        password(pass);
        require(role.equals("ADMIN")||role.equals("USER"),"Некорректная роль");
        try {
            addUser(getWritableDatabase(),username,fullName,pass,role);
        }
        catch(SQLiteConstraintException e) {
            throw new IllegalArgumentException("Этот логин уже занят");
        }
    }
    public void profile(String t,String fullName,String email,String phone) {
        Row u=user(t);
        fullName=name(fullName);
        email=email.trim();
        phone=phone.trim();
        require(email.isEmpty()||email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"),"Проверьте адрес электронной почты");
        require(email.length()<=120&&phone.length()<=30,"Слишком длинные контактные данные");
        require(phone.isEmpty()||phone.matches("[+0-9() \\-]{5,30}"),"Проверьте номер телефона");
        getWritableDatabase().execSQL("UPDATE users SET full_name=?,email=?,phone=? WHERE id=?",new Object[] {
            fullName,email,phone,u.l("id")
        }
        );
    }
    public synchronized void changePassword(String t,String old,String next) {
        Row u=user(t),r=one("SELECT * FROM users WHERE id=?",u.s("id"));
        require(matches(old,r.s("salt"),r.s("password_hash")),"Текущий пароль неверен");
        password(next);
        setPassword(u.l("id"),next);
        getWritableDatabase().execSQL("DELETE FROM sessions WHERE user_id=? AND token_hash<>?",new Object[] {
            u.l("id"),digest(t)
        }
        );
    }
    private void setPassword(long id,String pass) {
        String s=salt();
        getWritableDatabase().execSQL("UPDATE users SET salt=?,password_hash=? WHERE id=?",new Object[] {
            s,hash(pass,s),id
        }
        );
    }
    public synchronized void resetPassword(String t,long id,String pass) {
        admin(t);
        password(pass);
        require(one("SELECT id FROM users WHERE id=?",""+id)!=null,"Пользователь не найден");
        setPassword(id,pass);
        getWritableDatabase().execSQL("DELETE FROM sessions WHERE user_id=?",new Object[] {
            id
        }
        );
    }
    public List<Row> users(String t) {
        admin(t);
        return query("SELECT id,username,full_name,role,active,email,phone FROM users ORDER BY id");
    }
    public synchronized void userAccess(String t,long id,String role,boolean active) {
        admin(t);
        require(role.equals("ADMIN")||role.equals("USER"),"Некорректная роль");
        Row target=one("SELECT * FROM users WHERE id=?",""+id);
        require(target!=null,"Пользователь не найден");
        if(target.s("role").equals("ADMIN")&&target.i("active")==1&&(!role.equals("ADMIN")||!active))require(one("SELECT COUNT(*) n FROM users WHERE role='ADMIN' AND active=1").i("n")>1,"Нельзя отключить или понизить последнего администратора");
        getWritableDatabase().execSQL("UPDATE users SET role=?,active=? WHERE id=?",new Object[] {
            role,active?1:0,id
        }
        );
        if(!active)getWritableDatabase().execSQL("DELETE FROM sessions WHERE user_id=?",new Object[] {
            id
        }
        );
    }
    public List<Row> movies(boolean all) {
        return query("SELECT m.*,(SELECT ROUND(AVG(score),1) FROM reviews WHERE movie_id=m.id) rating,(SELECT COUNT(*) FROM reviews WHERE movie_id=m.id) review_count FROM movies m "+(all?"":"WHERE active=1 ")+"ORDER BY id");
    }
    public Row movie(long id) {
        Row m=one("SELECT m.*,(SELECT ROUND(AVG(score),1) FROM reviews WHERE movie_id=m.id) rating,(SELECT COUNT(*) FROM reviews WHERE movie_id=m.id) review_count FROM movies m WHERE id=?",""+id);
        require(m!=null,"Фильм не найден");
        return m;
    }
    public synchronized void saveMovie(String t,long id,String title,String genre,int duration,int age,String synopsis,String director,int year,int art) {
        admin(t);
        require(title.trim().length()>=2&&title.length()<=100,"Название: от 2 до 100 символов");
        require(!genre.trim().isEmpty()&&genre.length()<=40,"Укажите жанр до 40 символов");
        require(duration>=30&&duration<=300,"Длительность: от 30 до 300 минут");
        require(Arrays.asList(0,6,12,16,18).contains(age),"Возраст: 0, 6, 12, 16 или 18");
        require(!synopsis.trim().isEmpty()&&synopsis.length()<=3000,"Добавьте описание до 3000 символов");
        require(director.length()<=100&&year>=1900&&year<=2100,"Проверьте режиссёра и год");
        ContentValues v=new ContentValues();
        v.put("title",title.trim());
        v.put("genre",genre.trim());
        v.put("duration",duration);
        v.put("age",age);
        v.put("synopsis",synopsis.trim());
        v.put("director",director.trim());
        v.put("year",year);
        v.put("art",Math.floorMod(art,6));
        if(id==0)getWritableDatabase().insertOrThrow("movies",null,v);
        else require(getWritableDatabase().update("movies",v,"id=?",new String[] {
            ""+id
        }
        )==1,"Фильм не найден");
    }
    public List<Row> halls(boolean all) {
        return query("SELECT * FROM halls "+(all?"":"WHERE active=1 ")+"ORDER BY id");
    }
    public synchronized void saveHall(String t,long id,String n,int rows,int cols) {
        admin(t);
        require(n.trim().length()>=2&&n.length()<=60,"Название зала: от 2 до 60 символов");
        require(rows>=3&&rows<=12&&cols>=4&&cols<=12,"Зал: 3–12 рядов и 4–12 мест в ряду");
        try {
            ContentValues v=new ContentValues();
            v.put("name",n.trim());
            v.put("rows_count",rows);
            v.put("cols_count",cols);
            if(id==0)getWritableDatabase().insertOrThrow("halls",null,v);
            else require(getWritableDatabase().update("halls",v,"id=?",new String[] {
                ""+id
            }
            )==1,"Зал не найден");
        }
        catch(SQLiteConstraintException e) {
            throw new IllegalArgumentException("Зал с таким названием уже существует");
        }
    }
    public synchronized void archive(String t,String table,long id,boolean archived) {
        admin(t);
        require(table.equals("movies")||table.equals("halls"),"Некорректная сущность");
        String field=table.equals("movies")?"movie_id":"hall_id";
        if(archived)require(one("SELECT COUNT(*) n FROM shows WHERE "+field+"=? AND status='ACTIVE' AND ends_at>?",""+id,""+System.currentTimeMillis()).i("n")==0,"Сначала отмените будущие сеансы этой записи");
        getWritableDatabase().execSQL("UPDATE "+table+" SET active=? WHERE id=?",new Object[] {
            archived?0:1,id
        }
        );
    }
    private static final String SHOW_SELECT="SELECT s.*,m.title,m.genre,m.duration,m.age,m.art,h.name hall_name,(s.rows_count*s.cols_count-(SELECT COUNT(*) FROM booking_seats bs WHERE bs.show_id=s.id)) free FROM shows s JOIN movies m ON m.id=s.movie_id JOIN halls h ON h.id=s.hall_id ";
    public List<Row> shows(long movie,long start,long end,boolean all) {
        return query(SHOW_SELECT+"WHERE (CAST(? AS INTEGER)=0 OR s.movie_id=?) AND s.starts_at>=? AND s.starts_at<? "+(all?"":"AND s.status='ACTIVE' AND m.active=1 ")+"ORDER BY s.starts_at,h.name",""+movie,""+movie,""+start,""+end);
    }
    public Row show(long id) {
        Row s=one(SHOW_SELECT+"WHERE s.id=?",""+id);
        require(s!=null,"Сеанс не найден");
        return s;
    }
    public synchronized long saveShow(String t,long id,long mid,long hid,long start,int price,String format,String language) {
        admin(t);
        require(start>System.currentTimeMillis(),"Начало сеанса должно быть в будущем");
        require(price>=0&&price<=100000,"Цена: от 0 до 100 000 ₸");
        require(Arrays.asList("2D","3D","ATMOS").contains(format),"Выберите формат");
        require(!language.trim().isEmpty()&&language.length()<=40,"Укажите язык сеанса");
        Row m=movie(mid),h=one("SELECT * FROM halls WHERE id=?",""+hid);
        require(m.i("active")==1&&h!=null&&h.i("active")==1,"Фильм и зал должны быть активны");
        if(id!=0) {
            require(show(id).s("status").equals("ACTIVE"),"Отменённый сеанс нельзя изменить");
            require(one("SELECT COUNT(*) n FROM bookings WHERE show_id=? AND status<>'CANCELED'",""+id).i("n")==0,"Сеанс уже забронирован. Отмените его и создайте новый");
        }
        long end=start+(m.i("duration")+20)*60000L;
        SQLiteDatabase d=getWritableDatabase();
        d.beginTransaction();
        try {
            require(one("SELECT COUNT(*) n FROM shows WHERE hall_id=? AND id<>? AND status='ACTIVE' AND starts_at<? AND ends_at>?",""+hid,""+id,""+end,""+start).i("n")==0,"Зал занят. Учтите 20 минут на уборку");
            ContentValues v=new ContentValues();
            v.put("movie_id",mid);
            v.put("hall_id",hid);
            v.put("starts_at",start);
            v.put("ends_at",end);
            v.put("price",price);
            v.put("format",format);
            v.put("language",language.trim());
            v.put("rows_count",h.i("rows_count"));
            v.put("cols_count",h.i("cols_count"));
            if(id==0)id=d.insertOrThrow("shows",null,v);
            else d.update("shows",v,"id=?",new String[] {
                ""+id
            }
            );
            d.setTransactionSuccessful();
            return id;
        }
        finally {
            d.endTransaction();
        }
    }
    public Set<String> occupied(long show) {
        Set<String> seats=new HashSet<>();
        for(Row r:query("SELECT row_no,seat_no FROM booking_seats WHERE show_id=?",""+show))seats.add(r.s("row_no")+":"+r.s("seat_no"));
        return seats;
    }
    public boolean favorite(String t,long m) {
        return one("SELECT * FROM favorites WHERE user_id=? AND movie_id=?",user(t).s("id"),""+m)!=null;
    }
    public void toggleFavorite(String t,long m) {
        Row u=user(t);
        movie(m);
        if(favorite(t,m))getWritableDatabase().delete("favorites","user_id=? AND movie_id=?",new String[] {
            u.s("id"),""+m
        }
        );
        else getWritableDatabase().execSQL("INSERT INTO favorites VALUES(?,?)",new Object[] {
            u.l("id"),m
        }
        );
    }
    public List<Row> favorites(String t) {
        return query("SELECT m.* FROM movies m JOIN favorites f ON f.movie_id=m.id WHERE f.user_id=? AND m.active=1 ORDER BY m.id",user(t).s("id"));
    }
    public List<Row> reviews(long m) {
        return query("SELECT r.*,u.full_name FROM reviews r JOIN users u ON u.id=r.user_id WHERE movie_id=? ORDER BY updated_at DESC",""+m);
    }
    public void review(String t,long m,int score,String comment) {
        Row u=user(t);
        movie(m);
        require(score>=1&&score<=5,"Оценка: от 1 до 5");
        require(comment.trim().length()>=3&&comment.length()<=500,"Отзыв: от 3 до 500 символов");
        getWritableDatabase().execSQL("INSERT OR REPLACE INTO reviews VALUES(?,?,?,?,?)",new Object[] {
            u.l("id"),m,score,comment.trim(),System.currentTimeMillis()
        }
        );
    }
    public void deleteReview(String t,long m,long author) {
        Row u=user(t);
        require(u.l("id")==author||u.s("role").equals("ADMIN"),"Нельзя удалить чужой отзыв");
        getWritableDatabase().delete("reviews","movie_id=? AND user_id=?",new String[] {
            ""+m,""+author
        }
        );
    }
    public int discount(String code) {
        if(code.trim().isEmpty())return 0;
        Row r=one("SELECT * FROM promos WHERE code=? AND active=1",code.trim().toUpperCase(Locale.ROOT));
        require(r!=null,"Промокод не найден или отключён");
        return r.i("percent");
    }
    public List<Row> promos(String t) {
        admin(t);
        return query("SELECT * FROM promos ORDER BY code");
    }
    public void savePromo(String t,String code,int percent,boolean active) {
        admin(t);
        code=code.trim().toUpperCase(Locale.ROOT);
        require(code.matches("[A-Z0-9]{3,20}"),"Промокод: 3–20 латинских букв или цифр");
        require(percent>=1&&percent<=90,"Скидка: от 1 до 90%");
        getWritableDatabase().execSQL("INSERT OR REPLACE INTO promos VALUES(?,?,?)",new Object[] {
            code,percent,active?1:0
        }
        );
    }
    public synchronized long book(String t,long sid,Collection<String> requested,String promo) {
        Row u=user(t);
        require(requested!=null&&requested.size()>=1&&requested.size()<=6,"Выберите от 1 до 6 мест");
        TreeSet<String> seats=new TreeSet<>((a,b)-> {
            String[] aa=a.split(":"),bb=b.split(":");
            int c=Integer.compare(Integer.parseInt(aa[0]),Integer.parseInt(bb[0]));
            return c!=0?c:Integer.compare(Integer.parseInt(aa[1]),Integer.parseInt(bb[1]));
        }
        );
        for(String seat:requested) {
            require(seat!=null&&seat.matches("[0-9]{1,2}:[0-9]{1,2}"),"Некорректное место");
            require(seats.add(seat),"Место выбрано дважды");
        }
        SQLiteDatabase d=getWritableDatabase();
        d.beginTransaction();
        try {
            Row s=show(sid);
            require(s.s("status").equals("ACTIVE")&&s.l("starts_at")>System.currentTimeMillis()&&movie(s.l("movie_id")).i("active")==1,"Бронирование этого сеанса закрыто");
            for(String seat:seats) {
                String[] p=seat.split(":");
                require(Integer.parseInt(p[0])>=1&&Integer.parseInt(p[0])<=s.i("rows_count")&&Integer.parseInt(p[1])>=1&&Integer.parseInt(p[1])<=s.i("cols_count"),"Место отсутствует в зале");
            }
            int off=discount(promo),cost=total(s.i("price"),seats.size(),off);
            String code="KDR-"+UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase(Locale.ROOT);
            ContentValues v=new ContentValues();
            v.put("code",code);
            v.put("user_id",u.l("id"));
            v.put("show_id",sid);
            v.put("created_at",System.currentTimeMillis());
            v.put("movie_title",s.s("title"));
            v.put("hall_name",s.s("hall_name"));
            v.put("starts_at",s.l("starts_at"));
            v.put("seats",CinemaRules.join(", ",seats));
            v.put("total",cost);
            v.put("format",s.s("format"));
            v.put("promo",promo.trim().toUpperCase(Locale.ROOT));
            v.put("discount",off);
            long id=d.insertOrThrow("bookings",null,v);
            for(String seat:seats) {
                String[] p=seat.split(":");
                d.execSQL("INSERT INTO booking_seats VALUES(?,?,?,?)",new Object[] {
                    id,sid,Integer.parseInt(p[0]),Integer.parseInt(p[1])
                }
                );
            }
            d.setTransactionSuccessful();
            return id;
        }
        catch(SQLiteConstraintException e) {
            throw new IllegalArgumentException("Одно из мест уже занято. Обновите схему и выберите другое");
        }
        finally {
            d.endTransaction();
        }
    }
    public List<Row> bookings(String t,boolean all) {
        Row u=all?admin(t):user(t);
        return query("SELECT b.*,u.full_name,u.username FROM bookings b JOIN users u ON u.id=b.user_id "+(all?"":"WHERE b.user_id=? ")+"ORDER BY b.created_at DESC,b.id DESC",all?new String[] {
        }
        :new String[] {
            u.s("id")
        }
        );
    }
    public Row booking(String t,long id) {
        Row u=user(t),b=one("SELECT b.*,u.full_name,u.username FROM bookings b JOIN users u ON u.id=b.user_id WHERE b.id=?",""+id);
        require(b!=null,"Билет не найден");
        require(b.l("user_id")==u.l("id")||u.s("role").equals("ADMIN"),"Этот билет принадлежит другому пользователю");
        return b;
    }
    public synchronized void bookingStatus(String t,long id,String status) {
        SQLiteDatabase d=getWritableDatabase();
        d.beginTransaction();
        try {
            Row b=booking(t,id);
            if(status.equals("CANCELED")) {
                require(b.s("status").equals("BOOKED")||b.s("status").equals("PAID"),"Билет уже использован или отменён");
                require(b.l("starts_at")>System.currentTimeMillis(),"Сеанс уже начался. Отмена закрыта");
            }
            else {
                admin(t);
                if(status.equals("PAID"))require(b.s("status").equals("BOOKED"),"Оплатить можно только действующую бронь");
                else if(status.equals("CHECKED_IN")) {
                    require(b.s("status").equals("PAID"),"Сначала отметьте оплату");
                    Row s=show(b.l("show_id"));
                    require(System.currentTimeMillis()>=s.l("starts_at")-3600000L&&System.currentTimeMillis()<s.l("ends_at")-20*60000L,"Вход открыт за час до сеанса и до его окончания");
                }
                else throw new IllegalArgumentException("Некорректный статус");
            }
            d.execSQL("UPDATE bookings SET status=? WHERE id=?",new Object[] {
                status,id
            }
            );
            if(status.equals("CANCELED"))d.delete("booking_seats","booking_id=?",new String[] {
                ""+id
            }
            );
            d.setTransactionSuccessful();
        }
        finally {
            d.endTransaction();
        }
    }
    public synchronized void cancelShow(String t,long id) {
        admin(t);
        Row s=show(id);
        require(s.l("starts_at")>System.currentTimeMillis(),"Начавшийся сеанс нельзя отменить");
        SQLiteDatabase d=getWritableDatabase();
        d.beginTransaction();
        try {
            d.execSQL("UPDATE shows SET status='CANCELED' WHERE id=?",new Object[] {
                id
            }
            );
            d.execSQL("UPDATE bookings SET status='CANCELED' WHERE show_id=?",new Object[] {
                id
            }
            );
            d.delete("booking_seats","show_id=?",new String[] {
                ""+id
            }
            );
            d.setTransactionSuccessful();
        }
        finally {
            d.endTransaction();
        }
    }
    public Row stats(String t) {
        admin(t);
        return one("SELECT (SELECT COUNT(*) FROM users WHERE active=1) users,(SELECT COUNT(*) FROM shows WHERE status='ACTIVE' AND starts_at>"+System.currentTimeMillis()+") shows,(SELECT COUNT(*) FROM bookings WHERE status<>'CANCELED') bookings,(SELECT COALESCE(SUM(total),0) FROM bookings WHERE status IN ('PAID','CHECKED_IN')) revenue");
    }
}
