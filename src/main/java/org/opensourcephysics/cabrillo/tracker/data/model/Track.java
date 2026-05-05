package org.opensourcephysics.cabrillo.tracker.data.model;

import java.util.*;

/**
 * A Track represents measurements of a single object over time.
 * Immutable - use the builder to create modified copies.
 */
public final class Track {
    
    private final String id;
    private final String name;
    private final TrackType type;
    private final String color;
    private final boolean visible;
    private final Map<Integer, Point> points; // frame number -> point
    
    public Track() {
        this(UUID.randomUUID().toString(), "Untitled", TrackType.DEFAULT, "#FF0000", true, new HashMap<>());
    }

    public Track(TrackType type) {
        this(UUID.randomUUID().toString(), "Untitled", type, "#FF0000", true, new HashMap<>());
    }

    public Track(String id, String name, TrackType type, String color, boolean visible, 
                 Map<Integer, Point> points) {
        this.id = Objects.requireNonNull(id, "id required");
        this.name = Objects.requireNonNull(name, "name required");
        this.type = type != null ? type : TrackType.POINT_MASS;
        this.color = color != null ? color : "#FF0000";
        this.visible = visible;
        this.points = points != null ? new HashMap<>(points) : new HashMap<>();
    }
    
    public static Track create(String name, TrackType type) {
        return new Track(UUID.randomUUID().toString(), name, type, "#FF0000", true, new HashMap<>());
    }
    
    // Withers (return new modified copy)
    public Track withName(String name) {
        return new Track(id, name, type, color, visible, points);
    }
    
    public Track withColor(String color) {
        return new Track(id, name, type, color, visible, points);
    }
    
    public Track withVisible(boolean visible) {
        return new Track(id, name, type, color, visible, points);
    }
    
    public Track withPoint(int frame, Point point) {
        Map<Integer, Point> newPoints = new HashMap<>(points);
        newPoints.put(frame, point);
        return new Track(id, name, type, color, visible, newPoints);
    }
    
    public Track withoutPoint(int frame) {
        Map<Integer, Point> newPoints = new HashMap<>(points);
        newPoints.remove(frame);
        return new Track(id, name, type, color, visible, newPoints);
    }
    
    public Optional<Point> point(int frame) {
        return Optional.ofNullable(points.get(frame));
    }
    
    public List<Integer> frames() {
        List<Integer> sorted = new ArrayList<>(points.keySet());
        Collections.sort(sorted);
        return sorted;
    }
    
    public boolean hasFrame(int frame) {
        return points.containsKey(frame);
    }
    
    public int pointCount() {
        return points.size();
    }
    
    // Also provide List<Point> for compatibility
    public List<Point> getPoints() {
        List<Integer> frames = frames();
        List<Point> list = new ArrayList<>();
        for (int f : frames) {
            points.get(f);
            list.add(points.get(f));
        }
        return list;
    }
    
    public Point getPoint(int index) {
        List<Integer> frames = frames();
        if (index >= 0 && index < frames.size()) {
            return points.get(frames.get(index));
        }
        return null;
    }
    
    public Track addPoint(Point point) {
        return withPoint(point.getFrame(), point);
    }
    
    // Getters
    public String id() { return id; }
    public String name() { return name; }
    public TrackType type() { return type; }
    public TrackType getType() { return type; }
    public String color() { return color; }
    public boolean visible() { return visible; }
    public Map<Integer, Point> points() { return Collections.unmodifiableMap(points); }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof Track t && id.equals(t.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Track[%s|%s|%s|%dpts]", id.substring(0, 8), name, type, points.size());
    }
}