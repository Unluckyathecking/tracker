package org.opensourcephysics.cabrillo.tracker.analysis;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates derived kinematic tracks (velocity, acceleration) from a position track.
 * Uses numerical differentiation with configurable methods.
 */
public class TrackAnalyzer {

    private final double dt;
    private final Differentiator.Method method;
    private final int windowSize;

    public TrackAnalyzer(double dt) {
        this(dt, Differentiator.Method.FIVE_POINT, 5);
    }

    public TrackAnalyzer(double dt, Differentiator.Method method, int windowSize) {
        this.dt = dt;
        this.method = method;
        this.windowSize = windowSize;
    }

    /**
     * Compute velocity track from position track.
     * Returns vx and vy components as separate tracks, plus a magnitude track.
     */
    public KinematicResult analyze(Track positionTrack) {
        List<Integer> frames = positionTrack.frames();
        if (frames.size() < 3) {
            return KinematicResult.empty();
        }

        List<Double> times = new ArrayList<>();
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();

        for (int f : frames) {
            times.add(f * dt);
            Point p = positionTrack.point(f).orElseThrow();
            xs.add(p.hasWorld() ? p.worldX() : p.getX());
            ys.add(p.hasWorld() ? p.worldY() : p.getY());
        }

        Differentiator diff = new Differentiator(method, windowSize);
        List<Double> vx = diff.differentiate(xs, dt);
        List<Double> vy = diff.differentiate(ys, dt);
        List<Double> ax = diff.differentiateTwice(xs, dt);
        List<Double> ay = diff.differentiateTwice(ys, dt);

        Track velocityTrack = buildDerivedTrack(
            positionTrack.name() + " (velocity)", TrackType.VECTOR, frames, vx, vy);
        Track accelerationTrack = buildDerivedTrack(
            positionTrack.name() + " (acceleration)", TrackType.VECTOR, frames, ax, ay);

        // Fit linear to x(t) and quadratic to y(t) for verification
        CurveFitter.LinearResult xFit = CurveFitter.fitLinear(times, xs);
        CurveFitter.PolynomialResult yFit = CurveFitter.fitPolynomial(times, ys, 2);

        return new KinematicResult(velocityTrack, accelerationTrack, xFit, yFit, times, xs, ys, vx, vy, ax, ay);
    }

    private Track buildDerivedTrack(String name, TrackType type, List<Integer> frames,
                                     List<Double> valsX, List<Double> valsY) {
        Track track = Track.create(name, type);
        int n = Math.min(frames.size(), Math.min(valsX.size(), valsY.size()));
        for (int i = 0; i < n; i++) {
            Point p = Point.withWorld(frames.get(i), valsX.get(i), valsY.get(i));
            track = track.withPoint(frames.get(i), p);
        }
        return track;
    }

    public record KinematicResult(
        Track velocityTrack,
        Track accelerationTrack,
        CurveFitter.LinearResult xFit,
        CurveFitter.PolynomialResult yFit,
        List<Double> times,
        List<Double> xs,
        List<Double> ys,
        List<Double> vx,
        List<Double> vy,
        List<Double> ax,
        List<Double> ay
    ) {
        public static KinematicResult empty() {
            return new KinematicResult(null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        public boolean hasData() {
            return velocityTrack != null && accelerationTrack != null;
        }

        public double averageAx() {
            return ax.stream().skip(2).limit(Math.max(0, ax.size() - 4))
                .mapToDouble(Double::doubleValue).average().orElse(0);
        }

        public double averageAy() {
            return ay.stream().skip(2).limit(Math.max(0, ay.size() - 4))
                .mapToDouble(Double::doubleValue).average().orElse(0);
        }
    }
}
