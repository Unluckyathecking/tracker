package org.opensourcephysics.cabrillo.tracker.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ProjectileModelTest {

    /** Parameters: x0=0, y0=100, vx0=5, vy0=0, g=-9.8 */
    private static final ProjectileModel.Parameters PARAMS =
            new ProjectileModel.Parameters(0, 100, 5, 0, -9.8);

    @Test
    void predictsExpectedValues() {
        // x(t=2) = 0 + 5*2 = 10
        assertThat(ProjectileModel.predictX(PARAMS, 2.0)).isCloseTo(10.0, within(1e-9));

        // y(t=1) = 100 + 0*1 + 0.5*(-9.8)*1^2 = 100 - 4.9 = 95.1
        assertThat(ProjectileModel.predictY(PARAMS, 1.0)).isCloseTo(95.1, within(1e-9));
    }

    @Test
    void recordEqualityAndAccessors() {
        ProjectileModel.Parameters p1 = new ProjectileModel.Parameters(1, 2, 3, 4, -9.8);
        ProjectileModel.Parameters p2 = new ProjectileModel.Parameters(1, 2, 3, 4, -9.8);

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.x0()).isEqualTo(1.0);
        assertThat(p1.y0()).isEqualTo(2.0);
        assertThat(p1.vx0()).isEqualTo(3.0);
        assertThat(p1.vy0()).isEqualTo(4.0);
        assertThat(p1.g()).isEqualTo(-9.8);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }
}
