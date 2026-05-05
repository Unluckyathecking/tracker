package org.opensourcephysics.cabrillo.tracker.data.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TrackTest {

    @Test
    public void testCreateTrack() {
        Track track = Track.create("TestTrack", TrackType.POINT_MASS);
        assertEquals("TestTrack", track.name());
        assertEquals(TrackType.POINT_MASS, track.getType());
        assertTrue(track.getPoints().isEmpty());
    }

    @Test
    public void testAddPointReturnsNewInstance() {
        Track t1 = Track.create("Track", TrackType.DEFAULT);
        Point p = Point.atPixel(0, 10.0, 20.0);
        Track t2 = t1.addPoint(p);
        
        assertNotSame(t1, t2);
        assertTrue(t1.getPoints().isEmpty());
        assertEquals(1, t2.getPoints().size());
    }

    @Test
    public void testMultipleAddPoints() {
        Track track = Track.create("Track", TrackType.POINT_MASS);
        track = track.addPoint(Point.atPixel(0, 10.0, 20.0));
        track = track.addPoint(Point.atPixel(1, 15.0, 25.0));
        track = track.addPoint(Point.atPixel(2, 20.0, 30.0));
        
        List<Point> points = track.getPoints();
        assertEquals(3, points.size());
        assertEquals(0, points.get(0).getFrame());
        assertEquals(1, points.get(1).getFrame());
        assertEquals(2, points.get(2).getFrame());
    }

    @Test
    public void testGetPointByIndex() {
        Track track = Track.create("Track", TrackType.DEFAULT);
        track = track.addPoint(Point.atPixel(5, 10.0, 20.0));
        
        Point p = track.getPoint(0);
        assertNotNull(p);
        assertEquals(5, p.getFrame());
    }

    @Test
    public void testGetPointNotFound() {
        Track track = Track.create("Track", TrackType.DEFAULT);
        assertNull(track.getPoint(99));
    }
}
