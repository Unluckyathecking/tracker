package org.opensourcephysics.cabrillo.tracker.ui;

import org.opensourcephysics.cabrillo.tracker.calibration.Calibration;

/**
 * Pure helper for computing a {@link Calibration} from a calibration stick
 * defined by two pixel endpoints and a known real-world length.
 * No I/O, no UI dependencies, no side effects.
 */
public final class CalibrationStickTool {

    private CalibrationStickTool() {}

    /**
     * Compute a Calibration where:
     *   - scale = realLengthMeters / pixel distance between (x1,y1) and (x2,y2)
     *   - origin is unchanged from the existing calibration (caller can move it separately)
     *   - angle is set so the world +X axis points from (x1,y1) toward (x2,y2)
     *
     * The angle convention in Calibration is CCW around an axis where pixel-Y has
     * been flipped to be "up", so:
     *   angle = atan2(-(y2 - y1), (x2 - x1))
     *
     * @throws IllegalArgumentException if realLengthMeters &lt;= 0 or the two points coincide
     */
    public static Calibration calibrate(Calibration existing,
                                        double x1, double y1,
                                        double x2, double y2,
                                        double realLengthMeters) {
        if (realLengthMeters <= 0) {
            throw new IllegalArgumentException(
                    "realLengthMeters must be positive, got: " + realLengthMeters);
        }
        double dx = x2 - x1;
        double dy = y2 - y1;
        double pixelDist = Math.hypot(dx, dy);
        if (pixelDist == 0.0) {
            throw new IllegalArgumentException(
                    "The two pixel points are identical; cannot determine scale or angle.");
        }
        double scale = realLengthMeters / pixelDist;
        double angle = Math.atan2(-dy, dx); // flip dy for Y-up convention
        return existing.withScale(scale, existing.originX(), existing.originY())
                       .withAngle(angle);
    }
}
