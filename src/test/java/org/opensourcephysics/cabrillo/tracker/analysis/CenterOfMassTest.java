package org.opensourcephysics.cabrillo.tracker.analysis;

import org.junit.jupiter.api.Test;
import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

class CenterOfMassTest {

    @Test
    void equalMassesMidpoint() {
        Track a = Track.create("A", TrackType.POINT_MASS)
                .withPoint(0, Point.atPixel(0,  0, 0))
                .withPoint(1, Point.atPixel(1, 10, 0));

        Track b = Track.create("B", TrackType.POINT_MASS)
                .withPoint(0, Point.atPixel(0, 10, 0))
                .withPoint(1, Point.atPixel(1, 20, 0));

        Track com = CenterOfMass.compute("CoM", List.of(a, b), new double[]{1.0, 1.0});

        assertThat(com.pointCount()).isEqualTo(2);
        assertThat(com.point(0).orElseThrow().pixelX()).isCloseTo(5.0,  within(1e-9));
        assertThat(com.point(0).orElseThrow().pixelY()).isCloseTo(0.0,  within(1e-9));
        assertThat(com.point(1).orElseThrow().pixelX()).isCloseTo(15.0, within(1e-9));
        assertThat(com.point(1).orElseThrow().pixelY()).isCloseTo(0.0,  within(1e-9));
    }

    @Test
    void weightedAverage() {
        // masses 3:1  →  CoM x = (3*0 + 1*4) / 4 = 1.0
        Track a = Track.create("A", TrackType.POINT_MASS)
                .withPoint(0, Point.atPixel(0, 0, 0));

        Track b = Track.create("B", TrackType.POINT_MASS)
                .withPoint(0, Point.atPixel(0, 4, 0));

        Track com = CenterOfMass.compute("CoM", List.of(a, b), new double[]{3.0, 1.0});

        assertThat(com.pointCount()).isEqualTo(1);
        assertThat(com.point(0).orElseThrow().pixelX()).isCloseTo(1.0, within(1e-9));
        assertThat(com.point(0).orElseThrow().pixelY()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void intersectionOnlyOnSharedFrames() {
        // A: frames 0,1   B: frames 1,2  →  CoM should only contain frame 1
        Track a = Track.create("A", TrackType.POINT_MASS)
                .withPoint(0, Point.atPixel(0,  0, 0))
                .withPoint(1, Point.atPixel(1, 10, 0));

        Track b = Track.create("B", TrackType.POINT_MASS)
                .withPoint(1, Point.atPixel(1, 20, 0))
                .withPoint(2, Point.atPixel(2, 30, 0));

        Track com = CenterOfMass.compute("CoM", List.of(a, b), new double[]{1.0, 1.0});

        assertThat(com.pointCount()).isEqualTo(1);
        assertThat(com.frames()).containsExactly(1);
        assertThat(com.point(1).orElseThrow().pixelX()).isCloseTo(15.0, within(1e-9));
    }
}
