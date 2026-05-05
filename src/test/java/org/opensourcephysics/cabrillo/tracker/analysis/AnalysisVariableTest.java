package org.opensourcephysics.cabrillo.tracker.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisVariableTest {

    private static TrackAnalyzer.KinematicResult buildResult(
            List<Double> times, List<Double> xs, List<Double> ys,
            List<Double> vx, List<Double> vy, List<Double> ax, List<Double> ay) {
        return new TrackAnalyzer.KinematicResult(null, null, null, null,
                times, xs, ys, vx, vy, ax, ay);
    }

    @Test
    void extractAtFirstIndex() {
        TrackAnalyzer.KinematicResult r = buildResult(
                List.of(0.0, 0.1, 0.2, 0.3, 0.4),
                List.of((Double) 0.0, 1.0, 2.0, 3.0, 4.0),
                List.of((Double) 0.0, 0.0, 0.0, 0.0, 0.0),
                List.of(10.0, 10.0, 10.0, 10.0, 10.0),
                List.of((Double) 0.0, 0.0, 0.0, 0.0, 0.0),
                List.of((Double) 0.0, 0.0, 0.0, 0.0, 0.0),
                List.of(-9.8, -9.8, -9.8, -9.8, -9.8)
        );

        assertThat(AnalysisVariable.TIME.valueAt(r, 0)).isEqualTo(0.0);
        assertThat(AnalysisVariable.WORLD_X.valueAt(r, 2)).isEqualTo(2.0);
        assertThat(AnalysisVariable.VX.valueAt(r, 3)).isEqualTo(10.0);
        assertThat(AnalysisVariable.AY.valueAt(r, 1)).isEqualTo(-9.8);
    }

    @Test
    void speedIsHypot() {
        TrackAnalyzer.KinematicResult r = buildResult(
                List.of(0.0),
                List.of(0.0),
                List.of(0.0),
                List.of(3.0),
                List.of(4.0),
                List.of(0.0),
                List.of(0.0)
        );

        assertThat(AnalysisVariable.SPEED.valueAt(r, 0)).isCloseTo(5.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void accelMagIsHypot() {
        TrackAnalyzer.KinematicResult r = buildResult(
                List.of(0.0),
                List.of(0.0),
                List.of(0.0),
                List.of(0.0),
                List.of(0.0),
                List.of(0.0),
                List.of(-9.8)
        );

        assertThat(AnalysisVariable.ACCEL_MAG.valueAt(r, 0)).isCloseTo(9.8, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void outOfRangeReturnsNaN() {
        TrackAnalyzer.KinematicResult r = buildResult(
                List.of(0.0, 0.1, 0.2, 0.3, 0.4),
                List.of((Double) 0.0, 1.0, 2.0, 3.0, 4.0),
                List.of((Double) 0.0, 0.0, 0.0, 0.0, 0.0),
                List.of(10.0, 10.0, 10.0, 10.0, 10.0),
                List.of((Double) 0.0, 0.0, 0.0, 0.0, 0.0),
                List.of((Double) 0.0, 0.0, 0.0, 0.0, 0.0),
                List.of(-9.8, -9.8, -9.8, -9.8, -9.8)
        );

        for (AnalysisVariable variable : AnalysisVariable.values()) {
            assertThat(Double.isNaN(variable.valueAt(r, 100)))
                    .as("Expected NaN for %s at out-of-range index", variable)
                    .isTrue();
        }
    }

    @Test
    void emptyResultReturnsNaNAndZeroSampleCount() {
        TrackAnalyzer.KinematicResult r = TrackAnalyzer.KinematicResult.empty();

        assertThat(AnalysisVariable.sampleCount(r)).isEqualTo(0);
        assertThat(Double.isNaN(AnalysisVariable.TIME.valueAt(r, 0))).isTrue();
    }
}
