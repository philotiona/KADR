package com.example.kaspikids;

import android.app.Activity;

final class LabHooks {
    private LabHooks() {}

    static void beginWork() {}
    static void endWork() {}
    static void open(Activity activity) {}
}
