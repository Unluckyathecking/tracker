package org.opensourcephysics.cabrillo.tracker.data.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PointTest {

    @Test
    public void testAtPixelCreatesPointWithPixelCoords() {
        Point p = Point.atPixel(5, 10.0, 20.0);
        assertEquals(5, p.getFrame());
        assertEquals(10.0, p.pixelX(), 0.001);
        assertEquals(20.0, p.pixelY(), 0.001);
        assertTrue(Double.isNaN(p.worldX()));
        assertTrue(Double.isNaN(p.worldY()));
    }

    @Test
    public void testWithWorldCreatesPointWithWorldCoords() {
        Point p = Point.withWorld(3, 100.0, 200.0);
        assertEquals(3, p.getFrame());
        assertEquals(100.0, p.worldX(), 0.001);
        assertEquals(200.0, p.worldY(), 0.001);
        assertTrue(Double.isNaN(p.pixelX()));
        assertTrue(Double.isNaN(p.pixelY()));
    }

    @Test
    public void testWithWorldReturnsNewInstance() {
        Point p1 = Point.atPixel(0, 10.0, 20.0);
        Point p2 = p1.withWorld(100.0, 200.0);
        
        assertNotSame(p1, p2);
        assertEquals(10.0, p1.pixelX(), 0.001);
        assertEquals(100.0, p2.worldX(), 0.001);
    }

    @Test
    public void testWithPixelReturnsNewInstance() {
        Point p1 = Point.withWorld(0, 100.0, 200.0);
        Point p2 = p1.withPixel(10.0, 20.0);
        
        assertNotSame(p1, p2);
        assertEquals(100.0, p1.worldX(), 0.001);
        assertEquals(10.0, p2.pixelX(), 0.001);
    }

    @Test
    public void testWithFrameReturnsNewInstance() {
        Point p1 = Point.atPixel(5, 10.0, 20.0);
        Point p2 = p1.withFrame(10);
        
        assertNotSame(p1, p2);
        assertEquals(5, p1.getFrame());
        assertEquals(10, p2.getFrame());
        assertEquals(10.0, p2.pixelX(), 0.001);
    }

    @Test
    public void testGettersPreferPixelOverWorld() {
        Point p = Point.atPixel(0, 10.0, 20.0).withWorld(100.0, 200.0);
        assertEquals(10.0, p.pixelX(), 0.001);
        assertEquals(20.0, p.pixelY(), 0.001);
    }

    @Test
    public void testGettersFallBackToWorld() {
        Point p = Point.withWorld(0, 100.0, 200.0);
        assertEquals(100.0, p.worldX(), 0.001);
        assertEquals(200.0, p.worldY(), 0.001);
    }
}
