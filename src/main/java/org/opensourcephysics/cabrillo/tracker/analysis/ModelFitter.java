package org.opensourcephysics.cabrillo.tracker.analysis;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Fits a {@link ProjectileModel} to a {@link Track} using polynomial least squares.
 *
 * <ul>
 *   <li>x(t) is linear  → {@link CurveFitter#fitLinear} yields x0 and vx0.</li>
 *   <li>y(t) is degree-2 → {@link CurveFitter#fitPolynomial} with degree 2 yields
 *       coefficients [y0, vy0, 0.5·g], so g = 2·coeff[2].</li>
 * </ul>
 */
public final class ModelFitter {

    private ModelFitter() {}

    /**
     * Fit {@link ProjectileModel.Parameters} to the given track.
     *
     * <p>Coordinate selection: if every point in the track has world coordinates,
     * world coordinates are used; otherwise pixel coordinates are used.
     *
     * @param track the measured trajectory (must have at least 3 points)
     * @param dt    time elapsed per frame (seconds)
     * @return a {@link ProjectileModel.Fit} describing the best-fit parameters and residuals
     * @throws IllegalArgumentException if the track has fewer than 3 points
     */
    public static ProjectileModel.Fit fit(Track track, double dt) {
        List<Integer> frames = track.frames();
        if (frames.size() < 3) {
            throw new IllegalArgumentException(
                    "Track must have at least 3 points to fit a projectile model, got " + frames.size());
        }

        // Decide coordinate system: world if ALL points have world coords, else pixel.
        boolean useWorld = frames.stream()
                .map(f -> track.point(f).orElseThrow())
                .allMatch(Point::hasWorld);

        List<Double> times = new ArrayList<>(frames.size());
        List<Double> xs    = new ArrayList<>(frames.size());
        List<Double> ys    = new ArrayList<>(frames.size());

        for (int f : frames) {
            Point p = track.point(f).orElseThrow();
            times.add(f * dt);
            if (useWorld) {
                xs.add(p.worldX());
                ys.add(p.worldY());
            } else {
                xs.add(p.getX());
                ys.add(p.getY());
            }
        }

        // x(t) = x0 + vx0·t  →  linear fit
        CurveFitter.LinearResult linX = CurveFitter.fitLinear(times, xs);
        double x0  = linX.intercept;
        double vx0 = linX.slope;

        // y(t) = y0 + vy0·t + 0.5·g·t²  →  degree-2 polynomial fit
        // coefficients: [c0=y0, c1=vy0, c2=0.5·g]
        CurveFitter.PolynomialResult polyY = CurveFitter.fitPolynomial(times, ys, 2);
        double y0  = polyY.coefficients[0];
        double vy0 = polyY.coefficients[1];
        double g   = 2.0 * polyY.coefficients[2];

        ProjectileModel.Parameters params = new ProjectileModel.Parameters(x0, y0, vx0, vy0, g);

        // Build predicted lists and residuals
        List<Double> predictedX = new ArrayList<>(frames.size());
        List<Double> predictedY = new ArrayList<>(frames.size());
        List<Double> residualsX = new ArrayList<>(frames.size());
        List<Double> residualsY = new ArrayList<>(frames.size());

        double ssX = 0, ssY = 0;
        for (int i = 0; i < frames.size(); i++) {
            double t  = times.get(i);
            double px = ProjectileModel.predictX(params, t);
            double py = ProjectileModel.predictY(params, t);
            double rx = xs.get(i) - px;
            double ry = ys.get(i) - py;
            predictedX.add(px);
            predictedY.add(py);
            residualsX.add(rx);
            residualsY.add(ry);
            ssX += rx * rx;
            ssY += ry * ry;
        }

        double n     = frames.size();
        double rmseX = Math.sqrt(ssX / n);
        double rmseY = Math.sqrt(ssY / n);

        return new ProjectileModel.Fit(params, rmseX, rmseY, predictedX, predictedY, residualsX, residualsY);
    }
}
