package org.opensourcephysics.cabrillo.tracker.analysis;

import java.util.List;

/**
 * Names a plottable kinematic quantity and knows how to extract its value at a
 * given index of a {@link TrackAnalyzer.KinematicResult}.
 *
 * <p>Intended for use in JavaFX ChoiceBox / ComboBox "X axis" and "Y axis"
 * dropdowns: {@link #toString()} returns the human-readable label so no custom
 * cell factory is needed.
 */
public enum AnalysisVariable {

    TIME     ("t (s)"),
    WORLD_X  ("x (m)"),
    WORLD_Y  ("y (m)"),
    VX       ("vx (m/s)"),
    VY       ("vy (m/s)"),
    SPEED    ("|v| (m/s)"),
    AX       ("ax (m/s²)"),
    AY       ("ay (m/s²)"),
    ACCEL_MAG("|a| (m/s²)");

    private final String label;

    AnalysisVariable(String label) { this.label = label; }

    /** Human-readable axis label including units. */
    public String label() { return label; }

    /**
     * Returns the value of this variable at {@code index} within {@code r}.
     * Returns {@link Double#NaN} if {@code r} is null, the backing list is null
     * or empty, or {@code index} is out of range.
     */
    public double valueAt(TrackAnalyzer.KinematicResult r, int index) {
        if (r == null) return Double.NaN;
        return switch (this) {
            case TIME      -> safeGet(r.times(), index);
            case WORLD_X   -> safeGet(r.xs(),    index);
            case WORLD_Y   -> safeGet(r.ys(),    index);
            case VX        -> safeGet(r.vx(),    index);
            case VY        -> safeGet(r.vy(),    index);
            case SPEED     -> {
                double vx = safeGet(r.vx(), index);
                double vy = safeGet(r.vy(), index);
                yield (Double.isNaN(vx) || Double.isNaN(vy)) ? Double.NaN : Math.hypot(vx, vy);
            }
            case AX        -> safeGet(r.ax(),    index);
            case AY        -> safeGet(r.ay(),    index);
            case ACCEL_MAG -> {
                double ax = safeGet(r.ax(), index);
                double ay = safeGet(r.ay(), index);
                yield (Double.isNaN(ax) || Double.isNaN(ay)) ? Double.NaN : Math.hypot(ax, ay);
            }
        };
    }

    /** Number of sample indices available in {@code r} (== {@code times} list size). */
    public static int sampleCount(TrackAnalyzer.KinematicResult r) {
        if (r == null || r.times() == null) return 0;
        return r.times().size();
    }

    @Override
    public String toString() { return label; }

    // -------------------------------------------------------------------------

    private static double safeGet(List<Double> list, int index) {
        if (list == null || index < 0 || index >= list.size()) return Double.NaN;
        return list.get(index);
    }
}
