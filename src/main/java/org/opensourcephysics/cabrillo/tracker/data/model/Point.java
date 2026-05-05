package org.opensourcephysics.cabrillo.tracker.data.model;

/**
 * Immutable point representing a measurement at a specific video frame.
 * Stores both pixel and world coordinates. Either may be NaN if unknown.
 */
public final class Point {

    private final int frame;
    private final double pixelX;
    private final double pixelY;
    private final double worldX;
    private final double worldY;

    private Point(int frame, double pixelX, double pixelY, double worldX, double worldY) {
        this.frame = frame;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.worldX = worldX;
        this.worldY = worldY;
    }

    /** Factory method for creating a point with all coordinates (for Gson deserialization). */
    public static Point of(int frame, double pixelX, double pixelY, double worldX, double worldY) {
        return new Point(frame, pixelX, pixelY, worldX, worldY);
    }

    /** Create a Point with only pixel coordinates. */
    public static Point atPixel(int frame, double x, double y) {
        return new Point(frame, x, y, Double.NaN, Double.NaN);
    }

    /** Create a Point with only world coordinates. */
    public static Point withWorld(int frame, double x, double y) {
        return new Point(frame, Double.NaN, Double.NaN, x, y);
    }

    /** Convenience constructor for pixel points (used by subagent code). */
    public Point(int frame, double x, double y) {
        this(frame, x, y, Double.NaN, Double.NaN);
    }

    /** Return a new Point with world coordinates added. */
    public Point withWorld(double wx, double wy) {
        return new Point(frame, pixelX, pixelY, wx, wy);
    }

    /** Return a new Point with pixel coordinates added. */
    public Point withPixel(double px, double py) {
        return new Point(frame, px, py, worldX, worldY);
    }

    /** Return a new Point at a different frame. */
    public Point withFrame(int frame) {
        return new Point(frame, pixelX, pixelY, worldX, worldY);
    }

    public int getFrame() { return frame; }

    /** Get X coordinate. Prefers pixel, falls back to world. */
    public double getX() {
        return Double.isNaN(pixelX) ? worldX : pixelX;
    }

    /** Get Y coordinate. Prefers pixel, falls back to world. */
    public double getY() {
        return Double.isNaN(pixelY) ? worldY : pixelY;
    }

    public double pixelX() { return pixelX; }
    public double pixelY() { return pixelY; }
    public double worldX() { return worldX; }
    public double worldY() { return worldY; }

    public boolean hasPixel() { return !Double.isNaN(pixelX) && !Double.isNaN(pixelY); }
    public boolean hasWorld() { return !Double.isNaN(worldX) && !Double.isNaN(worldY); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point p = (Point) o;
        return frame == p.frame &&
               Double.compare(p.pixelX, pixelX) == 0 &&
               Double.compare(p.pixelY, pixelY) == 0 &&
               Double.compare(p.worldX, worldX) == 0 &&
               Double.compare(p.worldY, worldY) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(frame, pixelX, pixelY, worldX, worldY);
    }

    @Override
    public String toString() {
        return String.format("Point[%d|px(%.2f,%.2f)|world(%.4f,%.4f)]",
            frame, pixelX, pixelY, worldX, worldY);
    }
}
