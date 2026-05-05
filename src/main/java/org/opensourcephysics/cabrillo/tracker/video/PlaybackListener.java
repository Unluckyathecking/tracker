package org.opensourcephysics.cabrillo.tracker.video;

/**
 * Listener interface for receiving playback events.
 *
 * <p>Implement this interface to receive notifications about playback
 * state changes, position changes, and frame changes. All listener
 * methods are called from the playback controller's thread context.</p>
 *
 * <p><b>Thread Safety:</b> Implementations should be prepared for
 * concurrent calls if the playback controller supports multi-threaded access.</p>
 *
 * @see VideoPlaybackController
 * @see PlaybackState
 */
public interface PlaybackListener {

    /**
     * Called when the playback state changes.
     *
     * @param state the new playback state
     */
    void playbackStateChanged(PlaybackState state);

    /**
     * Called when the playback position changes.
     *
     * @param position the new frame position (zero-based)
     * @param time the new time position in seconds
     * @param duration the total video duration in seconds
     */
    void playbackPositionChanged(int position, double time, double duration);

    /**
     * Called when the playback frame changes.
     *
     * @param frame the new frame index (zero-based)
     */
    void playbackFrameChanged(int frame);

    /**
     * Called when playback is stopped.
     */
    void playbackStopped();
}
