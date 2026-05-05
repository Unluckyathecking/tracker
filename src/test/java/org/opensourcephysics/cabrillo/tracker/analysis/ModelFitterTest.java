package org.opensourcephysics.cabrillo.tracker.analysis;

import org.junit.jupiter.api.Test;
import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ModelFitterTest {

    private static final double X0  =  0.0;
    private static final double Y0  = 100.0;
    private static final double VX0 =  5.0;
    private static final double VY0 =  2.0;
    private static final double G   = -9.8;
    private static final double DT  =  0.1;
    private static final int    N   = 30;

    /** Build a Track whose points are sampled exactly from the known parametric trajectory. */
    private static Track buildSyntheticTrack() {
        Track track = Track.create("synthetic", TrackType.POINT_MASS);
        for (int i = 0; i < N; i++) {
            double t = i * DT;
            double x = X0 + VX0 * t;
            double y = Y0 + VY0 * t + 0.5 * G * t * t;
            // Use Point.of(frame, pixelX=NaN, pixelY=NaN, worldX, worldY) so hasWorld() == true
            track = track.withPoint(i, Point.of(i, Double.NaN, Double.NaN, x, y));
        }
        return track;
    }

    @Test
    void recoversKnownParameters() {
        Track track = buildSyntheticTrack();
        ProjectileModel.Fit fit = ModelFitter.fit(track, DT);
        ProjectileModel.Parameters p = fit.parameters();

        assertThat(p.g()).isCloseTo(G, within(0.01));
        assertThat(p.vx0()).isCloseTo(VX0, within(0.001));
        assertThat(p.vy0()).isCloseTo(VY0, within(0.001));
        assertThat(p.x0()).isCloseTo(X0, within(0.001));
        assertThat(p.y0()).isCloseTo(Y0, within(0.001));

        // RMSE should be essentially zero for noise-free synthetic data
        assertThat(fit.rmseX()).isCloseTo(0.0, within(1e-9));
        assertThat(fit.rmseY()).isCloseTo(0.0, within(1e-9));

        // Lists must have the same length as the number of frames
        assertThat(fit.predictedX()).hasSize(N);
        assertThat(fit.predictedY()).hasSize(N);
        assertThat(fit.residualsX()).hasSize(N);
        assertThat(fit.residualsY()).hasSize(N);
    }

    @Test
    void tooFewPointsThrows() {
        Track track = Track.create("tiny", TrackType.POINT_MASS);
        track = track.withPoint(0, Point.withWorld(0, 0.0, 0.0));
        track = track.withPoint(1, Point.withWorld(1, 1.0, 1.0));

        final Track twoPointTrack = track;
        assertThatThrownBy(() -> ModelFitter.fit(twoPointTrack, DT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3");
    }
}
