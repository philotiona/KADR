package com.example.kaspikids.benchmark;

import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.FrameTimingMetric;
import androidx.benchmark.macro.MacrobenchmarkScope;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.StartupTimingMetric;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.util.Collections;
import kotlin.Unit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class KadrBenchmark {
    private static final String PACKAGE = "com.example.kaspikids.benchmark";
    private static final int ITERATIONS = 5;
    @Rule public final MacrobenchmarkRule benchmark = new MacrobenchmarkRule();

    @Test public void coldStartup() {
        benchmark.measureRepeated(PACKAGE, Collections.singletonList(new StartupTimingMetric()),
                new CompilationMode.None(), StartupMode.COLD, ITERATIONS, scope -> {
                    scope.pressHome(0);
                    return Unit.INSTANCE;
                }, scope -> {
                    launch(scope);
                    return Unit.INSTANCE;
                });
    }

    @Test public void catalogScroll() {
        benchmark.measureRepeated(PACKAGE, Collections.singletonList(new FrameTimingMetric()),
                new CompilationMode.None(), StartupMode.WARM, ITERATIONS, scope -> {
                    launch(scope);
                    openCatalog(scope.getDevice());
                    catalog(scope.getDevice()).scroll(Direction.UP, 1.0f);
                    return Unit.INSTANCE;
                }, scope -> {
                    UiObject2 list = catalog(scope.getDevice());
                    list.scroll(Direction.DOWN, 0.75f);
                    list.scroll(Direction.UP, 0.75f);
                    return Unit.INSTANCE;
                });
    }

    private static void launch(MacrobenchmarkScope scope) {
        scope.startActivityAndWait(new Intent(Intent.ACTION_MAIN)
                .setClassName(PACKAGE, "com.example.kaspikids.CinemaActivity"));
    }

    private static void openCatalog(UiDevice device) {
        if (device.wait(Until.hasObject(By.text("Афиша")), 1500)) return;
        UiObject2 demo = device.findObject(By.text("Зритель"));
        if (demo == null) {
            catalog(device).scroll(Direction.DOWN, 0.9f);
            demo = device.wait(Until.findObject(By.text("Зритель")), 5000);
        }
        assertNotNull("Demo login button", demo);
        demo.click();
        UiObject2 signIn = device.wait(Until.findObject(By.textStartsWith("Войти")), 5000);
        assertNotNull("Sign-in button", signIn);
        signIn.click();
        assertNotNull("Catalog after sign-in", device.wait(Until.findObject(By.text("Афиша")), 10000));
    }

    private static UiObject2 catalog(UiDevice device) {
        UiObject2 list = device.wait(Until.findObject(By.clazz("android.widget.ScrollView")
                .scrollable(true)), 5000);
        assertNotNull("Scrollable KADR content", list);
        return list;
    }
}
