package org.opensourcephysics.cabrillo.tracker.tracking;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateMatcherTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Create a 100x100 solid-gray (50,50,50) image. */
    private static BufferedImage grayImage() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(50, 50, 50));
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        return img;
    }

    /** Draw a 9x9 white square whose top-left corner is at (ox, oy). */
    private static void drawWhiteSquare(BufferedImage img, int ox, int oy) {
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(ox, oy, 9, 9);
        g.dispose();
    }

    // -----------------------------------------------------------------------
    // Test 1: integer translation is recovered within 1 pixel
    // -----------------------------------------------------------------------

    @Test
    void findsExactTranslation() {
        // prev: white square at (40, 40) → center at (44, 44)
        BufferedImage prev = grayImage();
        drawWhiteSquare(prev, 40, 40);

        // next: same square shifted +7 in x, +3 in y → top-left (47, 43), center (51, 47)
        BufferedImage next = grayImage();
        drawWhiteSquare(next, 47, 43);

        double[] patch = TemplateMatcher.extractPatch(prev, 44.0, 44.0, 8);
        assertThat(patch).isNotNull();

        TemplateMatcher.Match m = TemplateMatcher.match(next, 44.0, 44.0, patch, 8, 15);
        assertThat(m).isNotNull();
        assertThat(Math.abs(m.pixelX() - 51.0)).isLessThanOrEqualTo(1.0);
        assertThat(Math.abs(m.pixelY() - 47.0)).isLessThanOrEqualTo(1.0);
    }

    // -----------------------------------------------------------------------
    // Test 2: convenience wrapper recovers the same shift
    // -----------------------------------------------------------------------

    @Test
    void convenienceFindNextRecoversShift() {
        BufferedImage prev = grayImage();
        drawWhiteSquare(prev, 40, 40);

        BufferedImage next = grayImage();
        drawWhiteSquare(next, 47, 43);

        TemplateMatcher.Match m = TemplateMatcher.findNext(prev, next, 44.0, 44.0);
        assertThat(m).isNotNull();
        assertThat(Math.abs(m.pixelX() - 51.0)).isLessThanOrEqualTo(1.0);
        assertThat(Math.abs(m.pixelY() - 47.0)).isLessThanOrEqualTo(1.0);
    }

    // -----------------------------------------------------------------------
    // Test 3: extractPatch returns null when patch extends out of bounds
    // -----------------------------------------------------------------------

    @Test
    void nullPatchOutOfBounds() {
        // 20x20 image; patchRadius=15 → half-side = 15
        // center at (1,1): left edge = 1-15 = -14 < 0 → must return null
        BufferedImage small = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        double[] patch = TemplateMatcher.extractPatch(small, 1.0, 1.0, 15);
        assertThat(patch).isNull();
    }
}
