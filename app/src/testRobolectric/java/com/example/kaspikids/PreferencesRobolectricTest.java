package com.example.kaspikids;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PreferencesRobolectricTest {
    @Test public void preferencesRoundTrip() {
        Context context = ApplicationProvider.getApplicationContext();
        assertTrue(context.getSharedPreferences("lab7", Context.MODE_PRIVATE)
                .edit().putString("test_session", "local-test-token").commit());
        assertEquals("local-test-token", context.getSharedPreferences("lab7", Context.MODE_PRIVATE)
                .getString("test_session", ""));
    }
}
