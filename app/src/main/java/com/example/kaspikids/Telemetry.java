package com.example.kaspikids;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Lab 8, task 13: Firebase Crashlytics (crashes, non-fatal errors, breadcrumbs) and
 * Google Analytics for Firebase (usage events).
 * Builds without google-services.json (debug, forks, CI without the secret) simply skip telemetry.
 * No personal data (name, e-mail, phone, login) is ever sent — only event names and counters.
 */
final class Telemetry {
    private static FirebaseAnalytics analytics;
    private static FirebaseCrashlytics crashlytics;

    private Telemetry() {}

    static void init(Context context) {
        if (FirebaseApp.getApps(context).isEmpty()) return;
        analytics = FirebaseAnalytics.getInstance(context);
        crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCustomKey("environment", BuildConfig.ENVIRONMENT);
        crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE);
    }

    static boolean enabled() {
        return crashlytics != null;
    }

    /** Analytics event + Crashlytics breadcrumb. */
    static void event(String name, String key, long value) {
        if (analytics == null) return;
        Bundle b = new Bundle();
        if (key != null) b.putLong(key, value);
        analytics.logEvent(name, b);
        crashlytics.log(name + (key == null ? "" : " " + key + "=" + value));
    }

    static void screen(String screen) {
        if (analytics == null) return;
        Bundle b = new Bundle();
        b.putString(FirebaseAnalytics.Param.SCREEN_NAME, screen);
        b.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "CinemaActivity");
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, b);
        crashlytics.log("screen " + screen);
    }

    static void nonFatal(Throwable t) {
        if (crashlytics != null) crashlytics.recordException(t);
    }
}
