package org.opensourcephysics.cabrillo.tracker.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;
import org.opensourcephysics.cabrillo.tracker.calibration.Calibration;
import org.opensourcephysics.cabrillo.tracker.project.ProjectConfiguration;
import org.opensourcephysics.cabrillo.tracker.project.TrackerProject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Gson-based implementation of ProjectSerializer for serializing/deserializing
 * TrackerProject objects to/from JSON format.
 */
public class GsonProjectSerializer implements ProjectSerializer {
    
    private static final String FIELD_NAME = "name";
    private static final String FIELD_CONFIGURATION = "configuration";
    private static final String FIELD_TRACKS = "tracks";
    private static final String FIELD_CALIBRATION = "calibration";
    
    private Gson gson;
    
    public GsonProjectSerializer() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Point.class, new PointTypeAdapter())
                .registerTypeAdapter(TrackType.class, new TrackTypeAdapter())
                .setPrettyPrinting()
                .create();
    }
    
    @Override
    public void serialize(TrackerProject project, Writer writer) throws IOException {
        ProjectData data = new ProjectData();
        data.name = project.getName();
        data.configuration = project.getConfiguration();
        data.tracks = new ArrayList<>();
        for (Track track : project.getTracks()) {
            data.tracks.add(new TrackData(track));
        }
        data.calibration = project.getCalibration();
        gson.toJson(data, writer);
    }
    
    @Override
    public TrackerProject deserialize(Reader reader) throws IOException {
        ProjectData data = gson.fromJson(reader, ProjectData.class);
        
        TrackerProject project = new TrackerProject(data.name);
        
        if (data.configuration != null) {
            project.setConfiguration(data.configuration);
        }
        
        if (data.tracks != null) {
            for (TrackData trackData : data.tracks) {
                Track track = trackData.toTrack();
                project.getTracks().add(track);
            }
        }
        
        if (data.calibration != null) {
            project.setCalibration(data.calibration);
        }
        
        return project;
    }
    
    /**
     * Inner class to represent serialized project data with Gson annotations.
     */
    private static class ProjectData {
        @SerializedName("name")
        String name;
        
        @SerializedName("configuration")
        ProjectConfiguration configuration;
        
        @SerializedName("tracks")
        List<TrackData> tracks;
        
        @SerializedName("calibration")
        Calibration calibration;
    }
    
    /**
     * Inner class to represent serialized track data.
     */
    private static class TrackData {
        @SerializedName("name")
        String name;
        
        @SerializedName("type")
        TrackType type;
        
        @SerializedName("points")
        List<Point> points;
        
        TrackData() {
            this.points = new ArrayList<>();
        }
        
        TrackData(Track track) {
            this.name = track.name();
            this.type = track.getType();
            this.points = new ArrayList<>(track.getPoints());
        }
        
        Track toTrack() {
            Track track = Track.create(name != null ? name : "Untitled", type != null ? type : TrackType.DEFAULT);
            for (Point point : points) {
                track = track.addPoint(point);
            }
            return track;
        }
    }
    
    /**
     * TypeAdapter for Point serialization.
     */
    private static class PointTypeAdapter extends TypeAdapter<Point> {
        @SerializedName("frame")
        private int frame;
        
        @SerializedName("x")
        private double x;
        
        @SerializedName("y")
        private double y;
        
        @Override
        public void write(JsonWriter out, Point value) throws IOException {
            out.beginObject();
            out.name("frame").value(value.getFrame());
            out.name("pixelX").value(value.pixelX());
            out.name("pixelY").value(value.pixelY());
            out.name("worldX").value(value.worldX());
            out.name("worldY").value(value.worldY());
            out.endObject();
        }
        
        @Override
        public Point read(JsonReader in) throws IOException {
            in.beginObject();
            int frame = 0;
            double pixelX = Double.NaN;
            double pixelY = Double.NaN;
            double worldX = Double.NaN;
            double worldY = Double.NaN;
            
            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case "frame":
                        frame = in.nextInt();
                        break;
                    case "pixelX":
                        pixelX = in.nextDouble();
                        break;
                    case "pixelY":
                        pixelY = in.nextDouble();
                        break;
                    case "worldX":
                        worldX = in.nextDouble();
                        break;
                    case "worldY":
                        worldY = in.nextDouble();
                        break;
                    case "x": // Backward compatibility
                        pixelX = in.nextDouble();
                        break;
                    case "y": // Backward compatibility
                        pixelY = in.nextDouble();
                        break;
                    default:
                        in.skipValue();
                }
            }
            in.endObject();
            return Point.of(frame, pixelX, pixelY, worldX, worldY);
        }
    }
    
    /**
     * TypeAdapter for TrackType serialization.
     */
    private static class TrackTypeAdapter extends TypeAdapter<TrackType> {
        @Override
        public void write(JsonWriter out, TrackType value) throws IOException {
            out.value(value.name());
        }
        
        @Override
        public TrackType read(JsonReader in) throws IOException {
            String name = in.nextString();
            try {
                return TrackType.valueOf(name);
            } catch (IllegalArgumentException e) {
                return TrackType.DEFAULT;
            }
        }
    }
}
