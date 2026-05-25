package org.opensourcephysics.cabrillo.tracker.calibration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CalibrationTest {

    @Test
    public void testDefaultIdentityTransform() {
        Calibration c = new Calibration();
        Calibration.WorldPoint world = c.toWorld(100.0, 200.0);
        
        assertEquals(100.0, world.x(), 0.001);
        assertEquals(-200.0, world.y(), 0.001); // Y flips direction
    }

    @Test
    public void testSetScale() {
        Calibration c = new Calibration();
        c = c.withScale(2.0, 0, 0); // 1 pixel = 2 meters
        
        Calibration.WorldPoint world = c.toWorld(100.0, 200.0);
        
        assertEquals(200.0, world.x(), 0.001);
        assertEquals(-400.0, world.y(), 0.001);
    }

    @Test
    public void testToPixel() {
        Calibration c = new Calibration();
        c = c.withScale(2.0, 0, 0);
        
        Calibration.PixelPoint pixel = c.toPixel(200.0, 400.0);
        
        assertEquals(100.0, pixel.x(), 0.001);
        assertEquals(-200.0, pixel.y(), 0.001); // Y flips back
    }

    @Test
    public void testWithOrigin() {
        Calibration c = new Calibration();
        c = c.withScale(1.0, 100.0, 200.0);

        Calibration.WorldPoint world = c.toWorld(100.0, 200.0);

        assertEquals(0.0, world.x(), 0.001);
        assertEquals(0.0, world.y(), 0.001);
    }

    @Test
    public void testIdentityRotation() {
        Calibration c = new Calibration().withAngle(0);
        Calibration.WorldPoint world = c.toWorld(50.0, -30.0);
        // Same as the default identity transform (Y flips).
        assertEquals(50.0, world.x(), 1e-9);
        assertEquals(30.0, world.y(), 1e-9);
    }

    @Test
    public void testRotation90Degrees() {
        // World +X axis aligned with pixel "up" direction (image Y decreasing).
        Calibration c = new Calibration(1.0, 0, 0, Math.PI / 2);
        // Pixel (0, -10) is 10 pixels "above" origin in display => world +X.
        Calibration.WorldPoint world = c.toWorld(0.0, -10.0);
        assertEquals(10.0, world.x(), 1e-9);
        assertEquals(0.0, world.y(), 1e-9);
    }

    @Test
    public void testRotation180Degrees() {
        Calibration c = new Calibration(1.0, 0, 0, Math.PI);
        Calibration.WorldPoint world = c.toWorld(10.0, 0.0);
        assertEquals(-10.0, world.x(), 1e-9);
        assertEquals(0.0, world.y(), 1e-9);
    }

    @Test
    public void testRotatedRoundTrip() {
        Calibration c = new Calibration(0.05, 320, 240, 0.37);
        Calibration.WorldPoint w = c.toWorld(412.5, 173.25);
        Calibration.PixelPoint p = c.toPixel(w.x(), w.y());
        assertEquals(412.5, p.x(), 1e-9);
        assertEquals(173.25, p.y(), 1e-9);
    }




    @Test
    public void testDegenerateScale() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> new Calibration(0.0, 0, 0, 0));
        assertTrue(ex1.getMessage().contains("scale"));
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> new Calibration(Double.NaN, 0, 0, 0));
        assertTrue(ex2.getMessage().contains("scale"));
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> new Calibration(Double.POSITIVE_INFINITY, 0, 0, 0));
        assertTrue(ex3.getMessage().contains("scale"));
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () -> new Calibration(Double.NEGATIVE_INFINITY, 0, 0, 0));
        assertTrue(ex4.getMessage().contains("scale"));
    }

    @Test
    public void testDegenerateOrigin() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, Double.NaN, 0, 0));
        assertTrue(ex1.getMessage().contains("originX"));
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, Double.NaN, 0));
        assertTrue(ex2.getMessage().contains("originY"));
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, Double.POSITIVE_INFINITY, 0, 0));
        assertTrue(ex3.getMessage().contains("originX"));
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, Double.NEGATIVE_INFINITY, 0, 0));
        assertTrue(ex4.getMessage().contains("originX"));
        IllegalArgumentException ex5 = assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, Double.POSITIVE_INFINITY, 0));
        assertTrue(ex5.getMessage().contains("originY"));
        IllegalArgumentException ex6 = assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, Double.NEGATIVE_INFINITY, 0));
        assertTrue(ex6.getMessage().contains("originY"));
    }

    @Test
    public void testDegenerateAngle() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, 0, Double.NaN));
        assertTrue(ex1.getMessage().contains("angle"));
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, 0, Double.POSITIVE_INFINITY));
        assertTrue(ex2.getMessage().contains("angle"));
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, 0, Double.NEGATIVE_INFINITY));
        assertTrue(ex3.getMessage().contains("angle"));
    }
}
