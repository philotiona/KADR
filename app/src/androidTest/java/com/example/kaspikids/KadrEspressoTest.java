package com.example.kaspikids;

import static androidx.test.espresso.intent.Intents.*;
import static androidx.test.espresso.intent.matcher.IntentMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class KadrEspressoTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private final SharedPreferences preferences = context.getSharedPreferences("kadr", Context.MODE_PRIVATE);
    private String previousSession;
    private String fixtureSession;
    private long fixtureBooking;

    @Before public void resetLoginAndRegisterIdlingResource() {
        previousSession = preferences.getString("session", null);
        assertTrue(preferences.edit().remove("session").commit());
        IdlingRegistry.getInstance().register(LabHooks.WORK);
    }

    @After public void restoreSessionAndCleanFixture() {
        IdlingRegistry.getInstance().unregister(LabHooks.WORK);
        preferences.edit().putString("session", previousSession).commit();
        if (fixtureSession != null) {
            try (CinemaDb db = new CinemaDb(context)) {
                if (fixtureBooking != 0) {
                    db.getWritableDatabase().execSQL("DELETE FROM booking_seats WHERE booking_id=?",
                            new Object[]{fixtureBooking});
                    db.getWritableDatabase().execSQL("DELETE FROM bookings WHERE id=?",
                            new Object[]{fixtureBooking});
                }
                db.logout(fixtureSession);
            }
        }
    }

    @Test public void viewerCanSignInAndReachTheCatalog() {
        try (ActivityScenario<CinemaActivity> scenario = ActivityScenario.launch(CinemaActivity.class)) {
            LoginPage login = new LoginPage();
            login.signIn("viewer", "Cinema123!");
            login.assertCatalogVisible();
            fixtureSession = preferences.getString("session", null);
        }
    }

    @Test public void savingTicketLaunchesThePngDocumentPicker() {
        String code;
        try (CinemaDb db = new CinemaDb(context)) {
            fixtureSession = db.loginUser("viewer", "Cinema123!");
            long screening = db.shows(0, System.currentTimeMillis(), Long.MAX_VALUE, false).get(0).l("id");
            fixtureBooking = db.book(fixtureSession, screening, Collections.singletonList("1:1"), "");
            code = db.booking(fixtureSession, fixtureBooking).s("code");
        }
        assertTrue(preferences.edit().putString("session", fixtureSession).commit());
        init();
        try (ActivityScenario<CinemaActivity> scenario = ActivityScenario.launch(CinemaActivity.class)) {
            intending(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT), hasType("image/png")))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null));
            TicketPage ticket = new TicketPage();
            ticket.open(code);
            ticket.savePng();
            intended(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT), hasType("image/png")));
        } finally {
            release();
        }
    }
}
