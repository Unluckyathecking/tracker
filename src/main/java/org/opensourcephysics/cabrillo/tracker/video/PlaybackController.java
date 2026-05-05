package org.opensourcephysics.cabrillo.tracker.video;

import java.nio.file.Path;

/**
 * Interface for controlling video playback state and operations.
 *
 * <p>This interface defines the fundamental playback control operations that
 * can be applied to any video source, independent of the underlying
 * implementation details.</p>
 *
 * @see VideoSource
 * @see VideoPlaybackController
 * @see PlaybackState
 * @see PlaybackListener
 */
public interface PlaybackController {

    /**
     * Sets the video source that this controller will manage.
     *
     * <p>This method replaces the current video source. If there was an
     * existing video source, it will not be closed automatically.</p>
     *
     * @param videoSource the new video source
     */
    void setVideoSource(VideoSource videoSource);

    /**
     * Returns the currently managed video source.
     *
     * @return the video source, or null if none is set
     */
    VideoSource getVideoSource();

    /**
     * Opens a video file for playback.
     *
     * @param file the path to the video file
     * @throws IllegalArgumentException if the file cannot be opened
     */
    void open(Path file);

    /**
     * Closes the currently open video and releases resources.
     *
     * <p>This stops playback and returns the controller to the closed state.</p>
     */
    void close();

    /**
     * Starts playback of the video.
     *
     * <p>May be called from PAUSED or CLOSED state. In CLOSED state,
     * playback starts from the beginning.</p>
     *
     * @throws IllegalStateException if playback cannot be started
     */
    void play();

    /**
     * Pauses playback.
     *
     * <p>Only effective when the player is currently playing.</p>
     *
     * @throws IllegalStateException if not in PLAYING state
     */
    void pause();

    /**
     * Stops playback and resets to the beginning.
     *
     * <p>This is equivalent to seeking to frame 0 and setting the state to CLOSED.</p>
     */
    void stop();

    /**
     * Seeks to a specific frame in the video.
     *
     * @param frameIndex the frame index to seek to (zero-based)
     * @throws IllegalArgumentException if frameIndex is invalid
     * @throws IllegalStateException if no video is open
     */
    void seek(int frameIndex);

    /**
     * Seeks to the next keyframe after the current position.
     *
     * <p>This is useful for frame-by-frame playback where seeking to
     * non-keyframes may not produce valid images.</p>
     *
     * @throws IllegalStateException if no video is open
     */
    void seekToNextKeyFrame();

    /**
     * Returns the current playback state.
     *
     * @return the current playback state
     */
    PlaybackState getState();

    /**
     * Returns the current frame position.
     *
     * @return the current frame index (zero-based)
     */
    int getCurrentPosition();

    /**
     * Returns the total number of frames in the video.
     *
     * @return total frame count, or -1 if unknown
     */
    int getFrameCount();

    /**
     * Returns the frame rate of the video.
     *
     * @return frames per second, or -1.0 if unknown
     */
    double getFrameRate();

    /**
     * Returns the current time position in seconds.
     *
     * @return current time, or -1.0 if time cannot be determined
     */
    double getTime();

    /**
     * Returns the total duration of the video in seconds.
     *
     * @return duration, or -1.0 if duration cannot be determined
     */
    double getDuration();

    /**
     * Returns true if currently playing.
     *
     * @return true if state is PLAYING
     */
    boolean isPlaying();

    /**
     * Returns true if currently paused.
     *
     * @return true if state is PAUSED
     */
    boolean isPaused();

    /**
     * Returns true if closed (no video open).
     *
     * @return true if state is CLOSED
     */
    boolean isClosed();
}
