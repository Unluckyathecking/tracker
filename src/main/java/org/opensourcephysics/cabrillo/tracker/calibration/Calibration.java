package org.opensourcephysics.cabrillo.tracker.calibration;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;

/**
 * Calibration transforms between pixel coordinates and world coordinates.
 * scale = meters per pixel. origin = pixel location of world (0,0).
 * angle = counter-clockwise rotation (radians) of the world +X axis relative to
 * the pixel +X axis (in display orientation, with pixel Y flipped to be "up").
 * Immutable.
 */
public final class Calibration {
    private final double scale;      // meters per pixel
    private final double originX;    // pixel X where world X = 0
    private final double originY;    // pixel Y where world Y = 0 (Y grows UP in world)
    private final double angle;      // radians, CCW

    public Calibration() {
        this(1.0, 0, 0, 0);
    }

    public Calibration(double scale, double originX, double originY) {
        this(scale, originX, originY, 0);
    }

    public Calibration(double scale, double originX, double originY, double angle) {
        if (!Double.isFinite(scale) || scale == 0.0) {
            throw new IllegalArgumentException("Invalid scale: " + scale);
        }
        if (!Double.isFinite(originX)) {
            throw new IllegalArgumentException("Invalid originX: " + originX);
        }
        if (!Double.isFinite(originY)) {
            throw new IllegalArgumentException("Invalid originY: " + originY);
        }
        if (!Double.isFinite(angle)) {
            throw new IllegalArgumentException("Invalid angle: " + angle);
        }
        this.scale = scale;
        this.originX = originX;
        this.originY = originY;
        this.angle = angle;
    }

    public double scale() { return scale; }
    public double originX() { return originX; }
    public double originY() { return originY; }
    public double angle() { return angle; }

    /** Return a new Calibration with different scale and origin (preserving angle). */
    public Calibration withScale(double scale, double originX, double originY) {
        return new Calibration(scale, originX, originY, this.angle);
    }

    /** Return a new Calibration with a different rotation. */
    public Calibration withAngle(double angle) {
        return new Calibration(scale, originX, originY, angle);
    }

    /** Return a new Calibration with a different origin. */
    public Calibration withOrigin(double originX, double originY) {
        return new Calibration(scale, originX, originY, angle);
    }

    /** Convert pixel coordinates to world coordinates. */
    public WorldPoint toWorld(double pixelX, double pixelY) {
        // Translate so origin is at (0,0); flip Y so "up" is positive.
        double dx = pixelX - originX;
        double dy = originY - pixelY;
        // Rotate by -angle to align with world axes.
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double rx =  dx * cos + dy * sin;
        double ry = -dx * sin + dy * cos;
        return new WorldPoint(rx * scale, ry * scale);
    }

    /** Convert a pixel Point to world coordinates. */
    public WorldPoint toWorld(Point p) {
        return toWorld(p.pixelX(), p.pixelY());
    }

    /** Convert world coordinates to pixel coordinates. */
    public PixelPoint toPixel(double worldX, double worldY) {
        double wx = worldX / scale;
        double wy = worldY / scale;
        // Rotate by +angle back into pixel-aligned axes.
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dx = wx * cos - wy * sin;
        double dy = wx * sin + wy * cos;
        return new PixelPoint(originX + dx, originY - dy);
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
