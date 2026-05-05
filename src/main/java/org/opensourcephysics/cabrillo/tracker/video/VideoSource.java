package org.opensourcephysics.cabrillo.tracker.video;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Interface defining video source operations for loading and playback.
 * 
 * This interface provides a unified API for accessing video frame data,
 * independent of the underlying decoder implementation.
 * 
 * <p>{@code VideoSource} implementations handle video file loading,
 * frame extraction, and basic playback operations.</p>
 *
 * @see FFmpegVideoSource
 * @see VideoMetadata
 */
public interface VideoSource {

    /**
     * Opens a video file for reading.
     *
     * <p>This method initializes the video decoder and reads metadata
     * such as frame count, frame rate, and dimensions.</p>
     *
     * @param file the Path to the video file to open
     * @throws IllegalArgumentException if the file does not exist or is not a valid video file
     * @throws IllegalStateException if a video is already open
     * @throws RuntimeException wrapping any underlying IO or decoding errors
     */
    void open(Path file);

    /**
     * Closes the currently open video and releases all resources.
     *
     * <p>This method stops any active decoding operations, releases
     * decoded frame buffers, and makes the source ready for a new file.</p>
     *
     * @throws IllegalStateException if no video is currently open
     */
    void close();

    /**
     * Returns the total number of frames in the video.
     *
     * @return the frame count, or -1 if the video is not open or the count is unknown
     */
    int getFrameCount();

    /**
     * Returns the frame rate in frames per second.
     *
     * @return the frame rate, or -1.0 if the video is not open or the rate is unknown
     */
    double getFrameRate();

    /**
     * Returns the video width in pixels.
     *
     * @return the frame width, or -1 if the video is not open or dimensions are unknown
     */
    int getWidth();

    /**
     * Returns the video height in pixels.
     *
     * @return the frame height, or -1 if the video is not open or dimensions are unknown
     */
    int getHeight();

    /**
     * Retrieves a decoded frame at the specified index.
     *
     * <p>The frame is returned as a BufferedImage. The method may
     * decode the frame on-demand or return a cached copy if available.</p>
     *
     * @param index the zero-based frame index to retrieve
     * @return a BufferedImage representing the frame
     * @throws IllegalArgumentException if the index is out of valid range
     * @throws IllegalStateException if no video is open
     */
    BufferedImage getFrame(int index);

    /**
     * Seeks to a specific frame index.
     *
     * <p>After seeking, subsequent {@link #getFrame(int)} calls will
     * start from the sought position. The method does not affect
     * the result of {@link #getFrame(int)}.
     *
     * @param frameIndex the target frame index to seek to (zero-based)
     * @throws IllegalArgumentException if the index is out of valid range
     * @throws IllegalStateException if no video is open
     */
    void seek(int frameIndex);

    /**
     * Checks if the video source is currently open.
     *
     * @return true if a video is open, false otherwise
     */
    boolean isOpen();

    /**
     * Returns the path to the currently open video file.
     *
     * @return the Path of the open video, or null if no video is open
     */
    Path getCurrentPath();
}
