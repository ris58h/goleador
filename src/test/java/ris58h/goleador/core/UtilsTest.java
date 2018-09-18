package ris58h.goleador.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void timestamp0() {
        assertEquals("0:00", Utils.timestamp(0));
    }

    @Test
    void timestamp333() {
        assertEquals("5:33", Utils.timestamp(333));
    }

    @Test
    void timestamp777() {
        assertEquals("12:57", Utils.timestamp(777));
    }

    @Test
    void timestamp9000() {
        assertEquals("2:30:00", Utils.timestamp(9000));
    }
}