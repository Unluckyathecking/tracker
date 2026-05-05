package org.opensourcephysics.cabrillo.tracker.analysis;

/**
 * Pure-function angle calculator. Computes the angle AVB at vertex V
 * given two arm endpoints A and B.
 */
public final class Protractor {

    private Protractor() {}

    /** Holds both radian and degree representations of a measured angle. */
    public record Reading(double radians, double degrees) {}

    /**
     * Angle AVB measured at vertex V, in [0, π] radians (unsigned,
     * direction-independent). Inputs are 2D coordinates in any consistent space.
     *
     * @throws IllegalArgumentException if either arm has zero length (< 1e-12)
     */
    public static Reading angle(double vx, double vy,
                                double ax, double ay,
                                double bx, double by) {
        double aX = ax - vx;
        double aY = ay - vy;
        double bX = bx - vx;
        double bY = by - vy;

        double lenA = Math.hypot(aX, aY);
        double lenB = Math.hypot(bX, bY);

        if (lenA < 1e-12) {
            throw new IllegalArgumentException("Arm VA has zero length");
        }
        if (lenB < 1e-12) {
            throw new IllegalArgumentException("Arm VB has zero length");
        }

        double cross = aX * bY - aY * bX;
        double dot   = aX * bX + aY * bY;
        double radians = Math.abs(Math.atan2(cross, dot));
        return new Reading(radians, Math.toDegrees(radians));
    }
}
