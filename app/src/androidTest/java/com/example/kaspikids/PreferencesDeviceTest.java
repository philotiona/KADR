package com.example.kaspikids;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PreferencesDeviceTest {
    @Test public void preferencesRoundTrip() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences preferences = context.getSharedPreferences("lab7", Context.MODE_PRIVATE);
        try {
            assertTrue(preferences.edit().putString("test_session", "local-test-token").commit());
            assertEquals("local-test-token", preferences.getString("test_session", ""));
        } finally {
            preferences.edit().remove("test_session").commit();
        }
    }
}
