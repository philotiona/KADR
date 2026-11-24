package com.example.kaspikids;

import android.app.Activity;
import android.content.Intent;
import android.os.StrictMode;
import androidx.test.espresso.idling.CountingIdlingResource;

final class LabHooks {
    static final CountingIdlingResource WORK = new CountingIdlingResource("KADR background work");
    private static StrictMode.ThreadPolicy previousPolicy;

    private LabHooks() {}

    static void beginWork() {
        WORK.increment();
    }

    static void endWork() {
        WORK.decrement();
    }

    static void open(Activity activity) {
        activity.startActivity(new Intent(activity, LabActivity.class));
    }

    static boolean toggleStrictMode() {
        if (previousPolicy != null) {
            StrictMode.setThreadPolicy(previousPolicy);
            previousPolicy = null;
            return false;
        }
        previousPolicy = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(previousPolicy)
                .detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
        return true;
    }
}
