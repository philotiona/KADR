package com.example.kaspikids;

/**
 * Lab 8, task 13: test crash and test ANR for debug/staging builds (BuildConfig.DIAGNOSTICS).
 * The call chain has real method names so the obfuscated stack trace can be decoded with mapping.txt.
 */
final class Diagnostics {
    private Diagnostics() {}

    static void testCrash() {
        new SeatAudit().verifyHall("Зал 01");
    }

    /** Blocks the main thread for 20 s; Android reports an ANR when input waits longer than its dispatch timeout. */
    static void testAnr() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static final class SeatAudit {
        void verifyHall(String hall) {
            countFreeSeats(hall, null);
        }

        int countFreeSeats(String hall, int[] seats) {
            if (seats == null) {
                throw new IllegalStateException("KADR test crash (Lab 8): seat map for " + hall + " is not loaded");
            }
            return seats.length;
        }
    }
}
