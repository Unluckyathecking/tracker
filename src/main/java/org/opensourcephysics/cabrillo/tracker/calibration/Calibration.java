package org.opensourcephysics.cabrillo.tracker.calibration;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;

/**
 * Calibration transforms between pixel coordinates and world coordinates.
 * scale = meters per pixel. origin = pixel location of world (0,0).
 * Immutable.
 */
public final class Calibration {
    private final double scale;      // meters per pixel
    private final double originX;    // pixel X where world X = 0
    private final double originY;    // pixel Y where world Y = 0 (Y grows UP in world)

    public Calibration() {
        this(1.0, 0, 0);
    }

    public Calibration(double scale, double originX, double originY) {
        this.scale = scale;
        this.originX = originX;
        this.originY = originY;
    }

    public double scale() { return scale; }
    public double originX() { return originX; }
    public double originY() { return originY; }

    /** Return a new Calibration with different scale and origin. */
    public Calibration withScale(double scale, double originX, double originY) {
        return new Calibration(scale, originX, originY);
    }

    /** Convert pixel coordinates to world coordinates. */
    public WorldPoint toWorld(double pixelX, double pixelY) {
        double wx = (pixelX - originX) * scale;
        double wy = (originY - pixelY) * scale; // Y flips: pixel Y grows down, world Y grows up
        return new WorldPoint(wx, wy);
    }

    /** Convert a pixel Point to world coordinates. */
    public WorldPoint toWorld(Point p) {
        return toWorld(p.pixelX(), p.pixelY());
    }

    /** Convert world coordinates to pixel coordinates. */
    public PixelPoint toPixel(double worldX, double worldY) {
        double px = originX + worldX / scale;
        double py = originY - worldY / scale; // Y flips back
        return new PixelPoint(px, py);
    }

    /** Convert a world Point to pixel coordinates. */
    public PixelPoint toPixel(Point p) {
        return toPixel(p.worldX(), p.worldY());
    }

    public double pixelsToMeters(double pixels) {
        return pixels * scale;
    }

    public double metersToPixels(double meters) {
        return meters / scale;
    }

    /** World coordinate record. */
    public static record WorldPoint(double x, double y) {}

    /** Pixel coordinate record. */
    public static record PixelPoint(double x, double y) {}
}
