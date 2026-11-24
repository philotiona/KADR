package com.example.kaspikids;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.startsWith;

final class LoginPage {
    void signIn(String username, String password) {
        onView(withContentDescription("Логин")).perform(scrollTo(), replaceText(username));
        onView(withContentDescription("Пароль")).perform(scrollTo(), replaceText(password), closeSoftKeyboard());
        onView(withText(startsWith("Войти"))).perform(scrollTo(), click());
    }

    void assertCatalogVisible() {
        onView(withText("Афиша")).check(matches(isDisplayed()));
    }
}
