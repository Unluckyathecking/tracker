package org.opensourcephysics.cabrillo.tracker.tracking;

import java.awt.image.BufferedImage;

/**
 * Pure-Java SSD (sum of squared differences) template matcher.
 * Operates on grayscale luminance derived from BufferedImage RGB pixels.
 */
public final class TemplateMatcher {

    public static final int DEFAULT_PATCH_RADIUS  = 15; // 31x31 patch
    public static final int DEFAULT_SEARCH_RADIUS = 25; // 51x51 search window

    private TemplateMatcher() {}

    /** Result of a template-match operation. */
    public record Match(double pixelX, double pixelY, double ssdPerPixel) {}

    // -----------------------------------------------------------------------
    // Luminance helpers
    // -----------------------------------------------------------------------

    /** Convert a packed ARGB int (from BufferedImage.getRGB) to [0,255] luminance. */
    private static double luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b =  rgb        & 0xFF;
        return 0.299 * r + 0.587 * g + 0.114 * b;
    }

    /**
     * Read luminance at a fractional (x, y) coordinate using bilinear interpolation.
     * Returns Double.NaN if any of the four surrounding pixels are out of bounds.
     */
    private static double sampleBilinear(BufferedImage img, double x, double y) {
        int w = img.getWidth();
        int h = img.getHeight();
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        if (x0 < 0 || y0 < 0 || x1 >= w || y1 >= h) return Double.NaN;
        double fx = x - x0;
        double fy = y - y0;
        double v00 = luminance(img.getRGB(x0, y0));
        double v10 = luminance(img.getRGB(x1, y0));
        double v01 = luminance(img.getRGB(x0, y1));
        double v11 = luminance(img.getRGB(x1, y1));
        return (1 - fx) * (1 - fy) * v00
             +      fx  * (1 - fy) * v10
             + (1 - fx) *      fy  * v01
             +      fx  *      fy  * v11;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Extract a (2*patchRadius+1)^2 grayscale patch centered at (cx, cy) from src.
     * Returns null if the patch would extend outside src bounds.
     */
    public static double[] extractPatch(BufferedImage src, double cx, double cy, int patchRadius) {
        int w   = src.getWidth();
        int h   = src.getHeight();
        int cx0 = (int) Math.round(cx);
        int cy0 = (int) Math.round(cy);
        int lo  = -patchRadius;
        int hi  =  patchRadius;
        if (cx0 + lo < 0 || cy0 + lo < 0 || cx0 + hi >= w || cy0 + hi >= h) {
            return null;
        }
        int side = 2 * patchRadius + 1;
        double[] patch = new double[side * side];
        int idx = 0;
        for (int dy = lo; dy <= hi; dy++) {
            for (int dx = lo; dx <= hi; dx++) {
                patch[idx++] = luminance(src.getRGB(cx0 + dx, cy0 + dy));
            }
        }
        return patch;
    }

    /**
     * Search next-frame image for the patch's best match within +/-searchRadius pixels
     * of (cx, cy). Uses bilinear sampling so sub-integer center offsets are handled.
     * Returns null if no valid candidate window could be found.
     */
    public static Match match(BufferedImage next, double cx, double cy,
                              double[] patch, int patchRadius, int searchRadius) {
        int side      = 2 * patchRadius + 1;
        int patchArea = side * side;
        double bestSsd   = Double.MAX_VALUE;
        double bestX     = Double.NaN;
        double bestY     = Double.NaN;
        boolean anyValid = false;

        for (int dy = -searchRadius; dy <= searchRadius; dy++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                double candX = cx + dx;
                double candY = cy + dy;
                // Quick bounds pre-check (integer coords of patch corners)
                int cx0 = (int) Math.round(candX);
                int cy0 = (int) Math.round(candY);
                if (cx0 - patchRadius < 0 || cy0 - patchRadius < 0
                        || cx0 + patchRadius >= next.getWidth()
                        || cy0 + patchRadius >= next.getHeight()) {
                    continue;
                }
                double ssd = 0.0;
                int idx = 0;
                boolean valid = true;
                for (int py = -patchRadius; py <= patchRadius && valid; py++) {
                    for (int px = -patchRadius; px <= patchRadius && valid; px++) {
                        double lum = sampleBilinear(next, candX + px, candY + py);
                        if (Double.isNaN(lum)) { valid = false; break; }
                        double diff = lum - patch[idx++];
                        ssd += diff * diff;
                    }
                }
                if (!valid) continue;
                anyValid = true;
                if (ssd < bestSsd) {
                    bestSsd = ssd;
                    bestX   = candX;
                    bestY   = candY;
                }
            }
        }
        if (!anyValid) return null;
        return new Match(bestX, bestY, bestSsd / patchArea);
    }

    /**
     * Convenience: extract a patch from prevFrame at (cx, cy) using DEFAULT_PATCH_RADIUS,
     * then locate it in nextFrame using DEFAULT_SEARCH_RADIUS.
     * Returns null if extraction fails.
     */
    public static Match findNext(BufferedImage prevFrame, BufferedImage nextFrame,
                                 double cx, double cy) {
        double[] patch = extractPatch(prevFrame, cx, cy, DEFAULT_PATCH_RADIUS);
        if (patch == null) return null;
        return match(nextFrame, cx, cy, patch, DEFAULT_PATCH_RADIUS, DEFAULT_SEARCH_RADIUS);
    }
}
