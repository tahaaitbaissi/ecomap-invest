package com.example.backend.overpass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BoundingBoxTest {

    @Test
    void validBox_isAccepted() {
        BoundingBox b = new BoundingBox(33.0, -8.0, 33.5, -7.5);
        assertEquals(33.0, b.south());
        assertEquals(-8.0, b.west());
        assertEquals(33.5, b.north());
        assertEquals(-7.5, b.east());
    }

    @Test
    void invalidLatitude_throws() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(91, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(0, 0, 91, 0));
    }

    @Test
    void invalidLongitude_throws() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(0, -181, 1, 0));
    }

    @Test
    void southGreaterThanNorth_throws() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(34, 0, 33, 1));
    }

    @Test
    void westGreaterThanEast_throws() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(0, 1, 1, 0));
    }
}
