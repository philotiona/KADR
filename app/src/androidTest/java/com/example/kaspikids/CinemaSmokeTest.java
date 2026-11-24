package com.example.kaspikids;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.example.kaspikids.CinemaDb.Row;
/** Standalone Android instrumentation: no downloaded testing libraries. Uses an isolated database. */
public class CinemaSmokeTest extends Instrumentation {
    private int checks;
    private boolean scheduleOnly;
    private boolean databaseOnly;
    private Activity activity;
    private CinemaDb db;
    private final String testName="cinema-verification.db";
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        scheduleOnly="true".equals(args.getString("scheduleOnly"));
        databaseOnly="true".equals(args.getString("databaseOnly"));
        start();
    }
    private void check(boolean b,String name) {
        checks++;
        if(!b)throw new AssertionError(name);
    }
    private void reject(Runnable r,String name) {
        try {
            r.run();
            throw new AssertionError("Accepted "+name);
        }
        catch(IllegalArgumentException e) {
            checks++;
        }
    }
    private View find(View v,String label) {
        if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;
        if(v.getContentDescription()!=null&&v.getContentDescription().toString().equals(label))return v;
        if(v instanceof ViewGroup)for(int i=0; i<((ViewGroup)v).getChildCount(); i++) {
            View found=find(((ViewGroup)v).getChildAt(i),label);
            if(found!=null)return found;
        }
        return null;
    }
    private View findPoster(View v) {
        if(v instanceof FilmArt)return v;
        if(v instanceof ViewGroup)for(int i=0; i<((ViewGroup)v).getChildCount(); i++) {
            View found=findPoster(((ViewGroup)v).getChildAt(i));
            if(found!=null)return found;
        }
        return null;
    }
    private View labeled(String label) {
        return find(activity.getWindow().getDecorView(),label);
    }
    private void click(String label) {
        runOnMainSync(()-> {
            View v=labeled(label);
            if(v==null)throw new AssertionError("Missing UI: "+label);
            v.performClick();
        }
        );
        waitForIdleSync();
    }
    private void waitVisible(String text)throws Exception {
        for(int i=0; i<100; i++) {
            waitForIdleSync();
            if(labeled(text)!=null)return;
            Thread.sleep(100);
        }
        throw new AssertionError("Timeout UI: "+text);
    }
    private void screenshot(String name)throws Exception {
        waitForIdleSync();
        android.view.accessibility.AccessibilityNodeInfo root=null;
        for(int i=0; i<50; i++) {
            root=getUiAutomation().getRootInActiveWindow();
            if(root!=null&&"com.example.kaspikids".contentEquals(root.getPackageName()))break;
            Thread.sleep(100);
        }
        Thread.sleep(200);
        android.graphics.Bitmap image=getUiAutomation().takeScreenshot();
        if(image!=null) {
            try(FileOutputStream f=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name+".png"))) {
                image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);
            }
            image.recycle();
        }
        check(root!=null&&"com.example.kaspikids".contentEquals(root.getPackageName()),"App unobscured for "+name+" (active="+(root==null?"null":root.getPackageName())+")");
    }
    private void verifySchedule() {
        Bundle result = new Bundle();
        CinemaDb real = new CinemaDb(getTargetContext());
        try {
            long now=System.currentTimeMillis();
            int count=real.query("SELECT COUNT(*) n FROM shows WHERE starts_at>=?", ""+now).get(0).i("n");
            check(real.shows(0,now,Long.MAX_VALUE,true).size()==count,"All-movie sentinel returns every show");
            check(count>0,"Seeded future schedule exists");
            activity=startActivitySync(new Intent(getTargetContext(),CinemaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitVisible("Управление →"); click("Управление →"); click("04  /  Расписание и цены");
            List<Row> today=real.shows(0,CinemaRules.day(now,0),CinemaRules.day(now,1),true);
            check(!today.isEmpty()&&labeled(today.get(0).s("title"))!=null,"Administrator schedule displays saved movie");
            screenshot("08-schedule");
            result.putString("stream","PASS: "+checks+" schedule regression checks\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable e) {result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
        finally {real.close();if(activity!=null)runOnMainSync(()->activity.finish());}
    }
    private View findGraph(View v) {
        if(v instanceof ErDiagramView)return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View match=findGraph(((ViewGroup)v).getChildAt(i));if(match!=null)return match;}
        return null;
    }
    private void verifyDatabase() {
        Bundle out=new Bundle();Context c=getTargetContext();String fixture="explorer-verification.db";
        String previous=c.getSharedPreferences("kadr",0).getString("session","");CinemaDb local=null,real=null;String temporary="";
        try {
            c.deleteDatabase(fixture);local=new CinemaDb(c,fixture);local.getWritableDatabase();
            String a=local.loginUser("admin","Admin123!"),u=local.loginUser("viewer","Cinema123!");
            DatabaseCatalog admin=new DatabaseCatalog(local,a),viewer=new DatabaseCatalog(local,u);
            check(admin.tables().size()==10,"catalog reads ten live tables");int fks=0,cols=0;for(Row t:admin.tables()){fks+=admin.foreignKeys(t.s("name")).size();cols+=admin.columns(t.s("name")).size();}
            check(fks==11&&cols==66,"all live columns and foreign keys");
            reject(()->viewer.rows("users","","",false,0),"viewer cannot inspect records");
            final CinemaDb testDatabase=local;
            reject(()->new DatabaseCatalog(testDatabase,"invalid").tables(),"invalid session cannot inspect schema");
            reject(()->admin.columns("users; DROP TABLE users"),"table injection rejected");
            reject(()->admin.rows("users","","id; DROP TABLE users",false,0),"sort injection rejected");
            reject(()->admin.rows("users","","password_hash",false,0),"secret ordering rejected");
            DatabaseCatalog.Page users=admin.rows("users","","id",false,0);
            check(users.rows.size()==2,"record browse");for(Row row:users.rows)check(row.s("password_hash").equals("[скрыто]")&&row.s("salt").equals("[скрыто]"),"credential hashes and salt masked");
            for(Row row:admin.rows("sessions","","user_id",false,0).rows)check(row.s("token_hash").equals("[скрыто]"),"session hashes masked");
            check(admin.rows("users","viewer","id",false,0).rows.size()==1,"bound search");
            check(admin.rows("users","' OR 1=1 --","id",false,0).rows.isEmpty(),"search injection treated literally");
            check(admin.rows("users","%","id",false,0).rows.isEmpty(),"search wildcard escaped");
            check(admin.rows("users","","id",true,0).rows.get(0).l("id")==2,"descending sort");
            check(viewer.schemaSql().contains("CREATE TABLE booking_seats")&&!viewer.schemaSql().contains("INSERT"),"schema export contains DDL only");
            check(admin.indexes("booking_seats").size()==1,"seat uniqueness index introspected");
            check(admin.indexColumns(admin.indexes("booking_seats").get(0).s("name")).contains("show_id"),"index columns introspected");
            check(viewer.checks().contains("Целостность SQLite: OK")&&viewer.checks().contains("Нарушения внешних ключей: 0"),"integrity and FK checks");
            for(int i=0;i<55;i++)local.getWritableDatabase().execSQL("INSERT INTO users(username,full_name,password_hash,salt,role) VALUES(?,?,?,?,?)",new Object[]{"paging"+i,"Paging "+i,"h","s","USER"});
            check(admin.rows("users","","id",false,0).rows.size()==50&&admin.rows("users","","id",false,1).rows.size()==7,"bounded pagination");
            String csv=DatabaseCatalog.csv(admin.rows("users","viewer","id",false,0));check(csv.contains("viewer")&&!csv.contains("Admin123!")&&csv.contains("[скрыто]"),"CSV redaction");
            real=new CinemaDb(c);List<Row> admins=real.query("SELECT id FROM users WHERE role='ADMIN' AND active=1 LIMIT 1");check(!admins.isEmpty(),"administrator fixture available");
            temporary="explorer-"+UUID.randomUUID();real.getWritableDatabase().execSQL("INSERT INTO sessions VALUES(?,?,?)",new Object[]{CinemaRules.digest(temporary),admins.get(0).l("id"),System.currentTimeMillis()+600000});
            c.getSharedPreferences("kadr",0).edit().putString("session",temporary).commit();
            activity=startActivitySync(new Intent(c,CinemaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitVisible("База");click("База");waitVisible("База данных");
            ErDiagramView graph=(ErDiagramView)findGraph(activity.getWindow().getDecorView());check(graph!=null&&graph.tableCount()==10&&graph.relationshipCount()==11,"navbar opens real ER view");
            screenshot("10-database-er");runOnMainSync(()->graph.focusTable("movies"));screenshot("11-database-zoom");android.os.Bundle viewport=graph.saveState();runOnMainSync(()->{graph.fit();graph.restoreState(viewport);});check(graph.saveState().getFloat("scale")==viewport.getFloat("scale"),"diagram viewport restored");
            click("Таблицы");check(labeled("Показать CREATE TABLE")!=null,"schema details available");click("Записи");check(labeled("CSV страницы")!=null,"admin data browser and CSV available");screenshot("12-database-rows");
            click("Экспорт SQL");waitForIdleSync();File exported=new File(c.getCacheDir(),"explorer-schema.sql");
            runOnMainSync(()->((CinemaActivity)activity).onActivityResult(DatabaseScreen.EXPORT_REQUEST,Activity.RESULT_OK,new Intent().setData(android.net.Uri.fromFile(exported))));
            check(exported.length()>2000,"actual schema file export");sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);waitForIdleSync();
            click("Проверки");click("Проверить базу");Thread.sleep(500);waitForIdleSync();check(labeled("Проверяем…")==null,"integrity check completes");screenshot("13-database-checks");
            runOnMainSync(()->activity.finish());activity=null;
            Row ordinary=real.query("SELECT id FROM users WHERE role='USER' AND active=1 LIMIT 1").get(0);real.logout(temporary);
            temporary="explorer-"+UUID.randomUUID();real.getWritableDatabase().execSQL("INSERT INTO sessions VALUES(?,?,?)",new Object[]{CinemaRules.digest(temporary),ordinary.l("id"),System.currentTimeMillis()+600000});c.getSharedPreferences("kadr",0).edit().putString("session",temporary).commit();
            activity=startActivitySync(new Intent(c,CinemaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitVisible("База");click("База");click("Таблицы");check(!labeled("Записи").isEnabled(),"viewer UI cannot open records");screenshot("14-database-viewer");
            out.putString("stream","PASS: "+checks+" database explorer, authorization, export and navbar checks\n");finish(Activity.RESULT_OK,out);
        } catch(Throwable e){out.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}
        finally{if(activity!=null)runOnMainSync(()->activity.finish());if(real!=null){if(!temporary.isEmpty())real.logout(temporary);real.close();}c.getSharedPreferences("kadr",0).edit().putString("session",previous).commit();if(local!=null)local.close();c.deleteDatabase(fixture);}
    }
    @Override public void onStart() {
        if(databaseOnly){verifyDatabase();return;}
        if(scheduleOnly){verifySchedule();return;}

        Bundle out=new Bundle();
        try {
            Context c=getTargetContext();
            c.deleteDatabase(testName);
            db=new CinemaDb(c,testName);
            db.getWritableDatabase();
            String admin=db.loginUser("ADMIN","Admin123!"),viewer=db.loginUser(" viewer ","Cinema123!");
            check(db.currentUser(admin).s("role").equals("ADMIN"),"administrator authentication");
            check(db.currentUser(viewer).s("role").equals("USER"),"viewer authentication");
            reject(()->db.loginUser("viewer","wrong"),"wrong password");
            reject(()->db.loginUser("missing","Cinema123!"),"unknown account");
            reject(()->db.register(viewer,"newuser","New User","NewPass123!","ADMIN"),"registration role boundary");
            db.register(admin,"newuser","Новый зритель","NewPass123!","USER");
            reject(()->db.register(admin,"NEWUSER","Duplicate","NewPass123!","USER"),"duplicate login case insensitive");
            String other=db.loginUser("newuser","NewPass123!");
            long otherId=db.currentUser(other).l("id");
            db.profile(other,"Олжас Тест","test@example.com","+7 777 123 45 67");
            check(db.currentUser(other).s("full_name").equals("Олжас Тест"),"saved profile");
            reject(()->db.profile(other,"Олжас","bad email",""),"invalid email");
            reject(()->db.userAccess(admin,1,"USER",true),"last administrator protected");
            db.userAccess(admin,otherId,"USER",false);
            check(db.currentUser(other)==null,"disabled account loses session");
            reject(()->db.loginUser("newuser","NewPass123!"),"disabled login");
            db.userAccess(admin,otherId,"USER",true);
            String restored=db.loginUser("newuser","NewPass123!");
            db.changePassword(restored,"NewPass123!","Changed123!");
            reject(()->db.loginUser("newuser","NewPass123!"),"old password rejected");
            String changed=db.loginUser("newuser","Changed123!");
            db.resetPassword(admin,otherId,"Reset123!");
            check(db.currentUser(changed)==null,"reset revokes sessions");
            check(db.currentUser(db.loginUser("newuser","Reset123!"))!=null,"reset login");
            reject(()->db.saveHall(viewer,0,"Forbidden",4,6),"hall role check");
            db.saveHall(admin,0,"TEST HALL",4,6);
            Row hall=db.halls(false).get(3);
            long hid=hall.l("id");
            reject(()->db.saveHall(admin,0,"test hall",4,6),"duplicate hall");
            reject(()->db.saveHall(admin,hid,"TEST HALL",1,40),"invalid dimensions");
            db.saveMovie(admin,0,"Тестовый фильм","Драма",100,12,"Описание фильма для проверки.","Режиссёр",2026,5);
            long mid=db.movies(true).get(6).l("id");
            db.saveMovie(admin,mid,"Тестовый фильм 2","Драма",100,12,"Описание фильма для проверки.","Режиссёр",2026,4);
            check(db.movie(mid).s("title").endsWith("2"),"movie edit");
            long start=System.currentTimeMillis()+30*60000L;
            long sid=db.saveShow(admin,0,mid,hid,start,2500,"2D","Русский");
            check(db.show(sid).i("free")==24,"show capacity");
            reject(()->db.saveShow(admin,0,mid,hid,start+60*60000L,2500,"2D","Русский"),"overlapping sessions rejected");
            reject(()->db.saveShow(admin,0,mid,hid,start+110*60000L,2500,"2D","Русский"),"cleaning buffer protected");
            long next=db.saveShow(admin,0,mid,hid,start+120*60000L,2500,"2D","Русский");
            check(next>sid,"adjacent show after cleaning");
            reject(()->db.saveShow(admin,0,mid,hid,System.currentTimeMillis()-1,1000,"2D","Русский"),"past show rejected");
            db.toggleFavorite(viewer,mid);
            check(db.favorite(viewer,mid)&&db.favorites(viewer).size()==1,"favorite saved");
            db.toggleFavorite(viewer,mid);
            check(!db.favorite(viewer,mid),"favorite removed");
            db.review(viewer,mid,5,"Отличный фильм");
            db.review(viewer,mid,4,"Обновлённый отзыв");
            check(db.reviews(mid).size()==1&&db.reviews(mid).get(0).i("score")==4,"review upsert");
            reject(()->db.deleteReview(db.loginUser("newuser","Reset123!"),mid,2),"review ownership");
            db.deleteReview(admin,mid,2);
            check(db.reviews(mid).isEmpty(),"admin review moderation");
            check(db.discount("student")==15,"case insensitive promo");
            db.savePromo(admin,"TEST",20,true);
            db.savePromo(admin,"TEST",20,false);
            reject(()->db.discount("TEST"),"disabled promo");
            long b=db.book(viewer,sid,Arrays.asList("1:1","1:2"),"STUDENT");
            check(db.booking(viewer,b).i("total")==4250,"booking total");
            check(db.show(sid).i("free")==22,"reserved seats counted");
            reject(()->db.book(admin,sid,Arrays.asList("1:1"),""),"double booking blocked");
            check(db.bookings(admin,true).size()==1,"failed transaction leaves no booking");
            reject(()->db.book(viewer,sid,Arrays.asList("1:1","1:1"),""),"duplicate requested seats");
            reject(()->db.book(viewer,sid,Arrays.asList("0:1"),""),"out of range seat");
            reject(()->db.book(viewer,sid,Arrays.asList("1:3"),"INVALID"),"invalid promo");
            reject(()->db.saveShow(admin,sid,mid,hid,start,3000,"2D","Русский"),"booked show protected");
            reject(()->db.booking(db.loginUser("newuser","Reset123!"),b),"ticket ownership");
            reject(()->db.bookingStatus(viewer,b,"PAID"),"payment role restriction");
            reject(()->db.bookingStatus(admin,b,"CHECKED_IN"),"unpaid entry rejected");
            db.bookingStatus(admin,b,"PAID");
            check(db.stats(admin).i("revenue")==4250,"cash revenue");
            db.bookingStatus(admin,b,"CHECKED_IN");
            reject(()->db.bookingStatus(admin,b,"CHECKED_IN"),"ticket cannot be reused");
            reject(()->db.bookingStatus(viewer,b,"CANCELED"),"used ticket cannot cancel");
            long canceled=db.book(viewer,sid,Arrays.asList("2:1"),"");
            db.bookingStatus(viewer,canceled,"CANCELED");
            check(!db.occupied(sid).contains("2:1"),"cancel releases seat");
            check(db.booking(viewer,canceled).s("seats").equals("2:1"),"canceled ticket history");
            CinemaDb second=new CinemaDb(c,testName);
            AtomicInteger successful=new AtomicInteger(),failed=new AtomicInteger();
            CountDownLatch go=new CountDownLatch(1);
            Thread t1=new Thread(()-> {
                try {
                    go.await();
                    db.book(viewer,next,Arrays.asList("1:1"),"");
                    successful.incrementAndGet();
                }
                catch(Exception e) {
                    failed.incrementAndGet();
                }
            }
            ),t2=new Thread(()-> {
                try {
                    go.await();
                    second.book(admin,next,Arrays.asList("1:1"),"");
                    successful.incrementAndGet();
                }
                catch(Exception e) {
                    failed.incrementAndGet();
                }
            }
            );
            t1.start();
            t2.start();
            go.countDown();
            t1.join();
            t2.join();
            check(successful.get()==1&&failed.get()==1,"concurrent connections reserve exactly once");
            second.close();
            db.cancelShow(admin,next);
            check(db.show(next).s("status").equals("CANCELED")&&db.occupied(next).isEmpty(),"show cancellation releases seats");
            reject(()->db.book(viewer,next,Arrays.asList("1:2"),""),"canceled show booking");
            db.saveHall(admin,hid,"TEST HALL",8,8);
            check(db.show(sid).i("rows_count")==4,"existing show seat snapshot");
            db.close();
            db=new CinemaDb(c,testName);
            check(db.booking(viewer,b).s("status").equals("CHECKED_IN"),"reopen database preserves booking and session");
            db.logout(viewer);
            check(db.currentUser(viewer)==null,"logout revokes token");
            // Launch the actual UI and verify role-specific controls and rendering, keeping the real demo data intact.
            c.getSharedPreferences("kadr",0).edit().remove("session").commit();
            activity=startActivitySync(new Intent(c,CinemaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitVisible("Войти  →");
            screenshot("01-login");
            click("Зритель");
            click("Войти  →");
            waitVisible("Время кино.");
            check(labeled("Личный кабинет")!=null&&labeled("Выйти")!=null,"main required buttons");
            check(labeled("Регистрация пользователей")==null,"registration hidden for viewer");
            screenshot("02-home");
            click("Кабинет");
            check(labeled("Сохранить профиль")!=null,"profile reachable");
            screenshot("06-profile");
            CinemaDb real=new CinemaDb(c);
            String uiToken=c.getSharedPreferences("kadr",0).getString("session","");
            List<Row> shows=real.shows(1,System.currentTimeMillis(),Long.MAX_VALUE,false);
            Row uiShow=shows.get(0);
            long uiBooking=real.book(uiToken,uiShow.l("id"),Arrays.asList("3:3","3:4"),"FIRST");
            click("Билеты");
            String code=real.booking(uiToken,uiBooking).s("code");
            click(code+"  →");
            check(labeled("Сохранить билет PNG")!=null,"ticket export available");
            android.graphics.Bitmap ticketImage=TicketImage.create(code,"Очень длинное название фильма\n"+String.join(" ",Collections.nCopies(40,"описание")));
            check(ticketImage.getHeight()>1000&&ticketImage.getWidth()==1000,"ticket image handles long text");
            try(FileOutputStream png=new FileOutputStream(new File(c.getExternalFilesDir(null),"sample-ticket.png"))) {
                check(ticketImage.compress(android.graphics.Bitmap.CompressFormat.PNG,100,png),"real PNG export");
            }
            ticketImage.recycle();
            screenshot("05-ticket");
            // Direct UI navigation uses the same public controls a viewer sees.
            click("Афиша");
            runOnMainSync(()-> {
                View v=findPoster(activity.getWindow().getDecorView());
                if(v==null)throw new AssertionError("Poster missing");
                v.performClick();
            }
            );
            waitForIdleSync();
            screenshot("03-movie");
            click("Выбрать места →");
            waitForIdleSync();
            check(activity.getWindow().getDecorView().findViewWithTag("seat-1:1")!=null,"seat map renders");
            runOnMainSync(()->activity.getWindow().getDecorView().findViewWithTag("seat-1:1").performClick());
            screenshot("04-seats");
            real.bookingStatus(uiToken,uiBooking,"CANCELED");
            real.logout(uiToken);
            String adminUi=real.loginUser("admin","Admin123!");
            c.getSharedPreferences("kadr",0).edit().putString("session",adminUi).commit();
            runOnMainSync(()->activity.finish());
            activity=startActivitySync(new Intent(c,CinemaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitVisible("Регистрация пользователей");
            check(labeled("Регистрация пользователей")!=null,"admin registration visible");
            click("Регистрация пользователей");
            screenshot("09-registration");
            getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            waitForIdleSync();
            click("Управление →");
            screenshot("07-admin");
            click("04  /  Расписание и цены");
            check(labeled("+ Создать сеанс")!=null,"admin schedule reachable");
            screenshot("08-schedule");
            real.close();
            out.putString("stream","PASS: "+checks+" Android database, concurrency, authorization and UI checks\n");
            finish(Activity.RESULT_OK,out);
        }
        catch(Throwable e) {
            out.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));
            finish(Activity.RESULT_CANCELED,out);
        }
        finally {
            if(activity!=null)runOnMainSync(()->activity.finish());
            if(db!=null)db.close();
            getTargetContext().deleteDatabase(testName);
        }
    }
}
