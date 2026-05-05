package org.opensourcephysics.cabrillo.tracker.video;

/**
 * Enumeration of possible playback states.
 *
 * <p>This enum represents the finite state machine states for video
 * playback, defining the valid transitions between states.</p>
 *
 * <p><b>Valid Transitions:</b></p>
 * <ul>
 *   <li>CLOSED → OPEN (via open())</li>
 *   <li>OPEN → PLAYING (via play())</li>
 *   <li>OPEN → PAUSED (via pause())</li>
 *   <li>PLAYING → PAUSED (via pause())</li>
 *   <li>PAUSED → PLAYING (via play())</li>
 *   <li>Any state → CLOSED (via stop() or close())</li>
 * </ul>
 */
public enum PlaybackState {
    /** Initial state or after video is closed */
    CLOSED,

    /** Video is open but not playing */
    OPEN,

    /** Video is actively playing */
    PLAYING,

    /** Video is paused */
    PAUSED
}
