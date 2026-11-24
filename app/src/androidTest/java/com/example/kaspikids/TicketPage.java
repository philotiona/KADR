package com.example.kaspikids;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

final class TicketPage {
    void open(String bookingCode) {
        onView(withText("Билеты")).perform(click());
        onView(withText(bookingCode + "  →")).perform(scrollTo(), click());
    }

    void savePng() {
        onView(withText("Сохранить билет PNG")).perform(scrollTo(), click());
    }
}
