package com.example.kaspikids;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BookingDatabaseTest {
    private CinemaDb db;
    private String viewer;
    private CinemaDb.Row show;

    @Before public void openDatabase() {
        db = new CinemaDb(ApplicationProvider.getApplicationContext(), null);
        viewer = db.loginUser("viewer", "Cinema123!");
        show = db.shows(0, System.currentTimeMillis(), Long.MAX_VALUE, false).get(0);
    }

    @After public void closeDatabase() {
        db.close();
    }

    @Test public void bookingStoresTwoSeatsAndTheDiscountedTotal() {
        long booking = db.book(viewer, show.l("id"), Arrays.asList("1:1", "1:2"), "STUDENT");

        assertEquals(2, db.query("SELECT * FROM booking_seats WHERE booking_id=?", "" + booking).size());
        assertEquals(CinemaRules.total(show.i("price"), 2, 15), db.booking(viewer, booking).i("total"));
    }

    @Test public void aConflictingBookingDoesNotReserveAnyOfItsSeats() {
        long screening = show.l("id");
        db.book(viewer, screening, Collections.singletonList("1:2"), "");

        assertThrows(IllegalArgumentException.class, () ->
                db.book(viewer, screening, Arrays.asList("1:1", "1:2"), ""));
        assertEquals(Collections.singleton("1:2"), db.occupied(screening));
        assertEquals(1, db.bookings(viewer, false).size());
    }

    @Test public void cancelFreesTheSeatsForAnotherBooking() {
        long screening = show.l("id");
        List<String> seats = Arrays.asList("1:1", "1:2");
        long booking = db.book(viewer, screening, seats, "");

        db.bookingStatus(viewer, booking, "CANCELED");

        assertEquals("CANCELED", db.booking(viewer, booking).s("status"));
        assertTrue(db.occupied(screening).isEmpty());
        assertTrue(db.book(viewer, screening, seats, "") > booking);
    }
}
