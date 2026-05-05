package org.opensourcephysics.cabrillo.tracker.persist;

import org.junit.jupiter.api.Test;
import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;
import org.opensourcephysics.cabrillo.tracker.project.TrackerProject;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

public class GsonProjectSerializerTest {

    @Test
    public void testSerializeDeserializeProject() throws Exception {
        // Create a project with a track
        TrackerProject project = new TrackerProject("Test Project");
        Track track = Track.create("Particle", TrackType.POINT_MASS);
        track = track.addPoint(Point.atPixel(0, 10.0, 20.0));
        track = track.addPoint(Point.atPixel(1, 15.0, 25.0));
        project.addTrack(track);

        // Serialize
        GsonProjectSerializer serializer = new GsonProjectSerializer();
        StringWriter writer = new StringWriter();
        serializer.serialize(project, writer);
        String json = writer.toString();

        // Deserialize
        StringReader reader = new StringReader(json);
        TrackerProject loaded = serializer.deserialize(reader);

        // Verify
        assertNotNull(loaded);
        assertEquals(1, loaded.getTracks().size());
        Track loadedTrack = loaded.getTracks().get(0);
        assertEquals("Particle", loadedTrack.name());
        assertEquals(2, loadedTrack.getPoints().size());
        
        // Verify points
        Point p0 = loadedTrack.getPoint(0);
        Point p1 = loadedTrack.getPoint(1);
        assertNotNull(p0);
        assertNotNull(p1);
        assertEquals(10.0, p0.pixelX(), 0.001);
        assertEquals(15.0, p1.pixelX(), 0.001);
    }
}
