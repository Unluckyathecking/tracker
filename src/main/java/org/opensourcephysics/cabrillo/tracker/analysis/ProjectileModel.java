package org.opensourcephysics.cabrillo.tracker.analysis;

import java.util.List;

/**
 * Closed-form parametric 2D projectile model.
 *
 * <p>x(t) = x0 + vx0·t
 * <p>y(t) = y0 + vy0·t + 0.5·g·t²
 */
public final class ProjectileModel {

    private ProjectileModel() {}

    /**
     * Physical parameters of a projectile trajectory.
     *
     * @param x0  initial x position
     * @param y0  initial y position
     * @param vx0 initial x velocity
     * @param vy0 initial y velocity
     * @param g   gravitational acceleration (negative for downward, e.g. -9.8)
     */
    public record Parameters(double x0, double y0, double vx0, double vy0, double g) {}

    /**
     * Result of fitting a projectile model to a track.
     *
     * @param parameters   recovered model parameters
     * @param rmseX        root-mean-square error in x
     * @param rmseY        root-mean-square error in y
     * @param predictedX   model-predicted x values at each sample time
     * @param predictedY   model-predicted y values at each sample time
     * @param residualsX   observed x minus predicted x at each sample time
     * @param residualsY   observed y minus predicted y at each sample time
     */
    public record Fit(
            Parameters parameters,
            double rmseX,
            double rmseY,
            List<Double> predictedX,
            List<Double> predictedY,
            List<Double> residualsX,
            List<Double> residualsY) {}

    /** Predict x at time {@code t} given {@code p}. */
    public static double predictX(Parameters p, double t) {
        return p.x0() + p.vx0() * t;
    }

    /** Predict y at time {@code t} given {@code p}. */
    public static double predictY(Parameters p, double t) {
        return p.y0() + p.vy0() * t + 0.5 * p.g() * t * t;
    }
}
