package org.opensourcephysics.cabrillo.tracker.video;

import java.time.Duration;
import java.util.Locale;

/**
 * Immutable record holding video metadata properties.
 *
 * <p>This record encapsulates all information extracted from a video file
 * during initial opening, including dimensions, timing, and codec details.</p>
 *
 * @param width width in pixels
 * @param height height in pixels
 * @param frameRate frames per second (can be fractional)
 * @param frameCount total number of frames, or -1 if unknown
 * @param duration duration in seconds, or -1.0 if unknown
 * @param format video file format/container type
 * @param codecName the video codec name
 */
public record VideoMetadata(
    int width,
    int height,
    double frameRate,
    int frameCount,
    double duration,
    String format,
    String codecName
) {
    /** No-arg constructor for empty/unknown metadata */
    public VideoMetadata() {
        this(-1, -1, -1.0, -1, -1.0, "unknown", "unknown");
    }
    /**
     * Creates a VideoMetadata instance for an unknown/empty video with all default values.
     */
    public VideoMetadata {
        if (width == 0) {
            width = -1;
        }
        if (height == 0) {
            height = -1;
        }
        if (frameRate == 0.0) {
            frameRate = -1.0;
        }
        if (frameCount == 0) {
            frameCount = -1;
        }
        if (duration == 0.0) {
            duration = -1.0;
        }
        format = format != null && !format.isEmpty() ? format : "unknown";
        codecName = codecName != null && !codecName.isEmpty() ? codecName : "unknown";
    }

    /**
     * Returns true if this metadata represents valid video information.
     *
     * @return true if dimensions are positive, false otherwise
     */
    public boolean isValid() {
        return width > 0 && height > 0 && frameRate > 0;
    }

    /**
     * Returns a simplified string representation showing dimensions and frame rate.
     *
     * @return a string like "1920x1080 at 30.0 fps"
     */
    @Override
    public String toString() {
        if (!isValid()) {
            return "VideoMetadata{unknown}";
        }
        return String.format(Locale.US, "VideoMetadata{%dx%d at %.2f fps, %d frames, %.2fs}",
            width, height, frameRate, frameCount, duration);
    }
}
