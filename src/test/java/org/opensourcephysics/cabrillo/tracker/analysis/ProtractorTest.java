package org.opensourcephysics.cabrillo.tracker.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProtractorTest {

    @Test
    void rightAngle() {
        // V=(0,0), A=(1,0), B=(0,1) → 90°
        Protractor.Reading r = Protractor.angle(0, 0, 1, 0, 0, 1);
        assertThat(r.degrees()).isCloseTo(90.0, within(1e-9));
        assertThat(r.radians()).isCloseTo(Math.PI / 2, within(1e-9));
    }

    @Test
    void straightAngle() {
        // V=(0,0), A=(-1,0), B=(1,0) → 180°
        Protractor.Reading r = Protractor.angle(0, 0, -1, 0, 1, 0);
        assertThat(r.degrees()).isCloseTo(180.0, within(1e-9));
        assertThat(r.radians()).isCloseTo(Math.PI, within(1e-9));
    }

    @Test
    void acuteAngleSymmetric() {
        // V=(0,0), A=(1,0), B=(1,1) → 45°
        Protractor.Reading r = Protractor.angle(0, 0, 1, 0, 1, 1);
        assertThat(r.degrees()).isCloseTo(45.0, within(1e-9));

        // Swapping A and B must give the same unsigned reading
        Protractor.Reading swapped = Protractor.angle(0, 0, 1, 1, 1, 0);
        assertThat(swapped.degrees()).isCloseTo(r.degrees(), within(1e-9));
        assertThat(swapped.radians()).isCloseTo(r.radians(), within(1e-9));
    }

    @Test
    void zeroAngleSamePoint() {
        // V=(0,0), A=(1,0), B=(2,0) → 0° (collinear, same direction)
        Protractor.Reading r = Protractor.angle(0, 0, 1, 0, 2, 0);
        assertThat(r.degrees()).isCloseTo(0.0, within(1e-9));
        assertThat(r.radians()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void zeroLengthArmThrows() {
        // V=(0,0), A=(0,0) — arm VA has zero length
        assertThatThrownBy(() -> Protractor.angle(0, 0, 0, 0, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
