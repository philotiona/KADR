package com.example.kaspikids;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public final class LabActivity extends Activity {
    private static final String TAG = "KadrLab";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Runnable CLEAR_LEAK = () -> retainedForDemo = null;
    private static Activity retainedForDemo;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(4, TimeUnit.SECONDS).build();
    private final AnnouncementClient client = new AnnouncementClient(http,
            HttpUrl.get("http://127.0.0.1:8765/announcement.json"));
    private AnnouncementDb cache;
    private TextView result;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        cache = AnnouncementDb.open(getApplicationContext(), "kadr-announcements.db");
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int padding = Ui.dp(this, 20);
        page.setPadding(padding, padding, padding, padding);
        scroll.addView(page);
        setContentView(scroll);
        Ui.applyInsets(scroll);
        setTitle("Лабораторная работа №7");
        result = new TextView(this);
        result.setText("Лабораторная работа №7 · диагностика");
        result.setTextSize(20);
        page.addView(result);

        addButton(page, "Показать Logcat D / I / W / E", () -> {
            Log.d(TAG, "Debug sample: two selected seats");
            Log.i(TAG, "Info sample: booking flow reached");
            Log.w(TAG, "Warning sample: promo rejected");
            Log.e(TAG, "Error sample: simulated failure");
            result.setText("В Logcat найдите тег KadrLab");
        });
        addButton(page, "Расчёт с ошибкой", () -> priceProbe(true));
        addButton(page, "Исправленный расчёт", () -> priceProbe(false));
        addButton(page, "Сбой · NullPointerException", () -> {
            String missing = null;
            result.setText(Integer.toString(missing.length()));
        });
        addButton(page, "Сбой · IndexOutOfBoundsException", () ->
                result.setText(Collections.<String>emptyList().get(0)));
        addButton(page, "Сбой · NetworkOnMainThreadException", this::networkOnMain);
        addButton(page, "Проверить null и пустой список", () -> {
            String missing = null;
            java.util.List<String> empty = Collections.emptyList();
            int length = missing == null ? 0 : missing.length();
            String first = empty.isEmpty() ? "нет элементов" : empty.get(0);
            result.setText("Длина: " + length + ", " + first);
        });
        addButton(page, "Обновить объявление в фоне", () ->
                runAsync(new AnnouncementService(client, message ->
                        cache.announcements().save(new AnnouncementDb.Entry(
                                1, message, System.currentTimeMillis())))::refresh));
        addButton(page, "Показать объявление из Room", () -> runAsync(() -> {
            AnnouncementDb.Entry entry = cache.announcements().latest();
            return entry == null ? "Кэш пуст" : entry.message;
        }));
        addButton(page, "Переключить StrictMode", () -> result.setText(
                LabHooks.toggleStrictMode() ? "StrictMode включён" : "StrictMode выключен"));
        addButton(page, "Удержать Activity и закрыть экран", () -> {
            MAIN.removeCallbacks(CLEAR_LEAK);
            retainedForDemo = this;
            MAIN.postDelayed(CLEAR_LEAK, 120_000);
            finish();
        });
        addButton(page, "Освободить удержанную Activity", () -> {
            MAIN.removeCallbacks(CLEAR_LEAK);
            CLEAR_LEAK.run();
            result.setText("Ссылка освобождена");
        });

        FrameLayout sample = new FrameLayout(this);
        addButton(page, "Вёрстка · показать наложение", () -> layoutSample(sample, true));
        addButton(page, "Вёрстка · убрать наложение", () -> layoutSample(sample, false));
        page.addView(sample);
    }

    private void addButton(LinearLayout page, String title, Runnable action) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setOnClickListener(view -> action.run());
        page.addView(button, new LinearLayout.LayoutParams(-1, -2));
    }

    private void priceProbe(boolean injectFault) {
        int price = 2500;
        int count = 2;
        int discount = 15;
        int total = injectFault ? price * count * (100 + discount) / 100
                : CinemaRules.total(price, count, discount);
        result.setText("Итого: " + CinemaRules.money(total));
    }

    private void networkOnMain() {
        StrictMode.ThreadPolicy previous = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(previous)
                .detectNetwork().penaltyDeathOnNetwork().build());
        try {
            result.setText(client.fetch());
        } catch (IOException e) {
            result.setText(e.getMessage());
        } finally {
            StrictMode.setThreadPolicy(previous);
        }
    }

    private void runAsync(Callable<String> action) {
        result.setText("Загрузка…");
        worker.execute(() -> {
            String value;
            try {
                value = action.call();
                Log.i(TAG, "Announcement operation succeeded");
            } catch (Exception e) {
                Log.e(TAG, "Announcement operation failed", e);
                value = "Ошибка: " + e.getMessage();
            }
            String message = value;
            runOnUiThread(() -> {
                if (!isDestroyed()) result.setText(message);
            });
        });
    }

    private void layoutSample(FrameLayout frame, boolean overlap) {
        frame.removeAllViews();
        for (int i = 0; i < 2; i++) {
            TextView label = new TextView(this);
            label.setText(i == 0 ? "Место A1" : "Место A2");
            label.setGravity(Gravity.CENTER);
            label.setBackgroundColor(i == 0 ? CinemaActivity.YELLOW : CinemaActivity.ACCENT);
            FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(
                    Ui.dp(this, 112), Ui.dp(this, 44), Gravity.TOP | Gravity.START);
            position.setMarginStart(i == 0 ? 0 : Ui.dp(this, overlap ? 35 : 120));
            frame.addView(label, position);
        }
    }

    @Override protected void onDestroy() {
        worker.execute(cache::close);
        worker.shutdown();
        super.onDestroy();
    }
}
