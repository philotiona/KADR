package com.example.kaspikids;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/** Pure Java rules shared by the database, screens and host tests. Money is integer KZT. */
public final class CinemaRules {
    private CinemaRules() {
    }
    public static void require(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException(message);
    }
    public static String login(String s) {
        s = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        require(s.matches("[a-z0-9_.-]{3,32}"), "Логин: 3–32 латинские буквы, цифры или . _ -");
        return s;
    }
    public static void password(String s) {
        require(s != null && s.length() >= 8 && s.length() <= 128, "Пароль должен содержать от 8 до 128 символов");
    }
    public static String name(String s) {
        s = s == null ? "" : s.trim();
        require(s.length() >= 2 && s.length() <= 80, "Имя: от 2 до 80 символов");
        return s;
    }
    public static String salt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return hex(b);
    }
    public static String hash(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(java.nio.charset.StandardCharsets.UTF_8), 120000, 256);
            byte[] bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return hex(bytes);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public static String digest(String s) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public static boolean matches(String password, String salt, String expected) {
        return MessageDigest.isEqual(hash(password, salt).getBytes(java.nio.charset.StandardCharsets.UTF_8), expected.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static String hex(byte[] a) {
        StringBuilder b = new StringBuilder();
        for (byte v : a) b.append(String.format(Locale.ROOT, "%02x", v & 255));
        return b.toString();
    }
    public static int total(int price, int count, int discount) {
        require(price >= 0 && price <= 100000 && count >= 1 && count <= 6 && discount >= 0 && discount <= 90, "Некорректная стоимость");
        return (int) (((long)price * count * (100 - discount) + 50) / 100);
    }
    public static boolean overlaps(long a, long b, long c, long d) {
        return a < d && c < b;
    }
    public static String join(String separator, Iterable<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(separator);
            result.append(value);
        }
        return result.toString();
    }
    public static String money(long amount) {
        return String.format(new Locale("ru"), "%,d ₸", amount);
    }
    public static String date(long time, String pattern) {
        return new SimpleDateFormat(pattern, new Locale("ru")).format(new Date(time));
    }
    public static long day(long time, int offset) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        c.add(Calendar.DAY_OF_YEAR,offset);
        return c.getTimeInMillis();
    }
}
