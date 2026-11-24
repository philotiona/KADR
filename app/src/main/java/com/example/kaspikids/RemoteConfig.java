package com.example.kaspikids;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.json.JSONObject;

/**
 * Lab 8, tasks 12 and 14: remote configuration served as JSON next to the API
 * ({@code BuildConfig.API_BASE_URL + "config.json"}, GitHub Pages in release).
 *
 * <ul>
 *   <li>staged rollout — {@code rollout_percent} decides which installs are told about {@code latest_version_code};</li>
 *   <li>forced update — installs below {@code min_supported_version_code} must update;</li>
 *   <li>feature flags / kill switch — {@code flags} turn features on and off without a new build.</li>
 * </ul>
 * The last good config is cached, so the app works offline and with defaults on first start.
 */
final class RemoteConfig {
    static final String FLAG_NEAREST_SHOW = "nearest_show_card";

    private static final String PREFS = "kadr_remote_config";
    private final SharedPreferences prefs;
    private JSONObject config;

    RemoteConfig(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        config = parse(prefs.getString("json", "{}"));
    }

    /** Blocking network call; run on a background thread. */
    boolean refresh() {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(BuildConfig.API_BASE_URL + "config.json").openConnection();
            c.setConnectTimeout(4000);
            c.setReadTimeout(4000);
            c.setUseCaches(false);
            if (c.getResponseCode() != 200) return false;
            String body = read(c.getInputStream());
            JSONObject fresh = new JSONObject(body);
            config = fresh;
            prefs.edit().putString("json", body).putLong("fetched_at", System.currentTimeMillis()).apply();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Stable 0..99 bucket of this install; rollout_percent = 10 reaches buckets 0..9. */
    int bucket() {
        String id = prefs.getString("install_id", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString("install_id", id).apply();
        }
        return Math.floorMod(id.hashCode(), 100);
    }

    int latestVersionCode() {
        return config.optInt("latest_version_code", BuildConfig.VERSION_CODE);
    }

    String latestVersionName() {
        return config.optString("latest_version_name", BuildConfig.VERSION_NAME);
    }

    int rolloutPercent() {
        return config.optInt("rollout_percent", 0);
    }

    String downloadUrl() {
        return config.optString("download_url", "https://github.com/philotiona/KADR/releases/latest");
    }

    /** A newer version exists and this install is inside the current rollout percentage. */
    boolean updateAvailable() {
        return latestVersionCode() > BuildConfig.VERSION_CODE && bucket() < rolloutPercent();
    }

    /** This build is no longer supported (critical bug, security fix): block until updated. */
    boolean updateRequired() {
        return BuildConfig.VERSION_CODE < config.optInt("min_supported_version_code", 0);
    }

    boolean flag(String name, boolean fallback) {
        JSONObject flags = config.optJSONObject("flags");
        return flags == null ? fallback : flags.optBoolean(name, fallback);
    }

    String message() {
        return config.optString("message", "");
    }

    private static JSONObject parse(String s) {
        try {
            return new JSONObject(s);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static String read(InputStream in) throws java.io.IOException {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) > 0) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
