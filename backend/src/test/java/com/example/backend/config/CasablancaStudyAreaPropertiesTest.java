package com.example.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CasablancaStudyAreaPropertiesTest {

    @Test
    void valid_sw_ne_order_ok() {
        var p = new CasablancaStudyAreaProperties(-7.751238, 33.493342, -7.4574165, 33.6409103);
        assertEquals(-7.751238, p.swLng(), 1e-6);
    }

    @Test
    void invalid_order_throws() {
        assertThrows(IllegalArgumentException.class, () -> new CasablancaStudyAreaProperties(0, 0, -1, 1));
    }
}
