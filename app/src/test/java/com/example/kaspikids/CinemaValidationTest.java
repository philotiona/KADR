package com.example.kaspikids;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;
import org.junit.Test;

public class CinemaValidationTest {
    @Test public void loginTrimsAndNormalizesCase() {
        assertEquals("viewer", CinemaRules.login("  ViEwEr "));
    }

    @Test public void missingAndInvalidLoginsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CinemaRules.login(null));
        assertThrows(IllegalArgumentException.class, () -> CinemaRules.login("ab"));
        assertThrows(IllegalArgumentException.class, () -> CinemaRules.login("Зритель"));
    }

    @Test public void passwordBoundaries() {
        CinemaRules.password("a".repeat(8));
        CinemaRules.password("a".repeat(128));
        assertThrows(IllegalArgumentException.class, () -> CinemaRules.password(null));
        assertThrows(IllegalArgumentException.class, () -> CinemaRules.password("a".repeat(7)));
        assertThrows(IllegalArgumentException.class, () -> CinemaRules.password("a".repeat(129)));
    }

    @Test public void namesAreTrimmedAndValidated() {
        assertEquals("Олжас", CinemaRules.name("  Олжас  "));
        assertThrows(IllegalArgumentException.class, () -> CinemaRules.name(null));
        assertThrows(IllegalArgumentException.class, () -> CinemaRules.name("a".repeat(81)));
    }

    @Test public void adjacentScreeningsDoNotOverlap() {
        assertFalse(CinemaRules.overlaps(0, 10, 10, 20));
        assertFalse(CinemaRules.overlaps(10, 20, 0, 10));
    }

    @Test public void overlappingScreeningsAreDetected() {
        assertTrue(CinemaRules.overlaps(0, 11, 10, 20));
    }

    @Test public void passwordHashMatchesOnlyTheOriginalPassword() {
        String salt = CinemaRules.salt();
        String hash = CinemaRules.hash("Cinema123!", salt);
        assertEquals(32, salt.length());
        assertEquals(64, hash.length());
        assertTrue(CinemaRules.matches("Cinema123!", salt, hash));
        assertFalse(CinemaRules.matches("Incorrect123!", salt, hash));
    }

    @Test public void digestIsStable() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                CinemaRules.digest("abc"));
    }

    @Test public void seatsAreSeparatedWithoutTrailingPunctuation() {
        assertEquals("1:1, 1:2", CinemaRules.join(", ", Arrays.asList("1:1", "1:2")));
    }

    @Test public void ticketAmountsUseKztAndGroupThousands() {
        assertEquals("2 500 ₸", CinemaRules.money(2500).replaceAll("[\\p{Zs}]+", " "));
    }

    @Test public void nextScreeningDayStartsAtLocalMidnight() {
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(2026, Calendar.JANUARY, 10, 14, 5);

        long tomorrow = CinemaRules.day(local.getTimeInMillis(), 1);

        assertEquals("2026-01-11 00:00", CinemaRules.date(tomorrow, "yyyy-MM-dd HH:mm"));
    }
}
