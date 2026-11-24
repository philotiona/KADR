package com.example.kaspikids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CinemaPriceTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
                {"full price", 2500, 2, 0, 5000},
                {"student discount", 2500, 2, 15, 4250},
                {"round half up", 1, 1, 50, 1},
                {"maximum order", 100000, 6, 0, 600000},
                {"free screening", 0, 1, 0, 0},
                {"maximum discount", 2500, 2, 90, 500},
                {"negative price", -1, 1, 0, null},
                {"price above maximum", 100001, 1, 0, null},
                {"no seats", 2500, 0, 0, null},
                {"too many seats", 2500, 7, 0, null},
                {"negative discount", 2500, 1, -1, null},
                {"discount above maximum", 2500, 1, 91, null}
        });
    }

    private final int price;
    private final int seats;
    private final int discount;
    private final Integer expected;

    public CinemaPriceTest(String name, int price, int seats, int discount, Integer expected) {
        this.price = price;
        this.seats = seats;
        this.discount = discount;
        this.expected = expected;
    }

    @Test public void calculatesOrRejectsOrder() {
        if (expected == null) {
            assertThrows(IllegalArgumentException.class, () -> CinemaRules.total(price, seats, discount));
        } else {
            int actual = CinemaRules.total(price, seats, discount);
            assertEquals(expected.intValue(), actual);
        }
    }
}
