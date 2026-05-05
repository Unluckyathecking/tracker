package org.opensourcephysics.cabrillo.tracker.analysis;

import org.junit.jupiter.api.Test;
import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;

import static org.junit.jupiter.api.Assertions.*;

public class TrackAnalyzerTest {

    @Test
    public void testProjectileMotion() {
        // Create a simple projectile motion track (10 fps, gravity -9.8 m/s^2)
        // Assuming 1 pixel = 1 meter for simplicity
        Track track = Track.create("Ball", TrackType.POINT_MASS);
        
        // Simulate: y = 0.5 * g * t^2
        // g = -9.8, fps = 10, dt = 0.1
        for (int i = 0; i < 10; i++) {
            double t = i * 0.1;
            double x = 10 * t;      // constant horizontal velocity
            double y = 0.5 * (-9.8) * t * t;
            track = track.addPoint(Point.atPixel(i, x, y));
        }

        // dt is 0.1 seconds (10 fps)
        TrackAnalyzer analyzer = new TrackAnalyzer(0.1);
        TrackAnalyzer.KinematicResult result = analyzer.analyze(track);
        
        // Verify we got results
        assertTrue(result.hasData());
        
        // Check average acceleration (should be close to -9.8 in Y direction)
        double avgAy = result.averageAy();
        assertTrue(Math.abs(avgAy - (-9.8)) < 2.0);
    }

    @Test
    public void testEmptyTrack() {
        Track track = Track.create("Empty", TrackType.POINT_MASS);
        TrackAnalyzer analyzer = new TrackAnalyzer(0.1);
        TrackAnalyzer.KinematicResult result = analyzer.analyze(track);
        
        // Empty track should return empty result
        assertFalse(result.hasData());
    }
}
