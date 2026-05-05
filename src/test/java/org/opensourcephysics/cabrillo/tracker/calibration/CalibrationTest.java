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
}
