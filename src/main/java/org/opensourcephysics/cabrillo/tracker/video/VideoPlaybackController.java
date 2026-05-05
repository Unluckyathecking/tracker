package org.opensourcephysics.cabrillo.tracker.video;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Playback state machine for video playback control.
 *
 * <p>This class manages the playback state of a VideoSource, including
 * play, pause, stop, and seek operations. It provides a simple state machine
 * interface for controlling video playback and notifies listeners of state changes.</p>
 *
 * <p><b>State Diagram:</b></p>
 * <pre>
 *      +--------+     open()    +---------+
 *      | Closed | ----------->  | Open    |
 *      +--------+               +---------+
 *          |                           |
 *          | open()                    | play()
 *          v                           v
 *      +--------+     stop()   +-------+
 *      | Closed | <----------  | Playing|
 *      +--------+  stop()      +-------+
 *          ^     stop()         | pause()
 *          |      pause()       v
 *      +---------+            | Paused  |
 *      | Closed  | <----------+         |
 *      +---------+            +---------+
 * </pre>
 *
 * <p><b>Thread Safety:</b> This implementation is thread-safe. All state transitions
 * and listener notifications are synchronized.</p>
 *
 * @see VideoSource
 * @see PlaybackState
 * @see PlaybackListener
 */
public class VideoPlaybackController implements PlaybackController {

    /** The underlying video source */
    private VideoSource videoSource;

    /** Current playback state */
    private PlaybackState state;

    /** Current frame position */
    private int currentPosition;

    /** Total frame count */
    private int totalFrames;

    /** Frame rate for time calculations */
    private double frameRate;

    /** List of registered playback listeners */
    private final List<PlaybackListener> listeners;

    /** Lock for thread-safe state access */
    private final ReentrantReadWriteLock lock;

    /** Default constructor - created in closed state */
    public VideoPlaybackController() {
        this(null);
    }

    /**
     * Creates a VideoPlaybackController, optionally with a pre-loaded video source.
     *
     * @param videoSource the video source to control, or null if not yet set
     */
    public VideoPlaybackController(VideoSource videoSource) {
        this.videoSource = videoSource;
        this.state = PlaybackState.CLOSED;
        this.currentPosition = 0;
        this.totalFrames = -1;
        this.frameRate = -1.0;
        this.listeners = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void setVideoSource(VideoSource videoSource) {
        lock.writeLock().lock();
        try {
            // If already open, close first
            if (this.state == PlaybackState.OPEN || this.state == PlaybackState.PLAYING ||
                this.state == PlaybackState.PAUSED) {
                this.state = PlaybackState.CLOSED;
            }

            this.videoSource = videoSource;
            this.totalFrames = -1;
            this.frameRate = -1.0;
            this.currentPosition = 0;

            // Update from video source
            if (videoSource != null && videoSource.isOpen()) {
                this.totalFrames = videoSource.getFrameCount();
                this.frameRate = videoSource.getFrameRate();
                this.state = PlaybackState.OPEN;
                firePlaybackStateChanged();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public VideoSource getVideoSource() {
        lock.readLock().lock();
        try {
            return videoSource;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void open(Path file) {
        lock.writeLock().lock();
        try {
            if (videoSource == null) {
                videoSource = new FFmpegVideoSource();
            }

            if (videoSource.isOpen()) {
                videoSource.close();
            }

            videoSource.open(file);
            this.state = PlaybackState.OPEN;
            this.totalFrames = videoSource.getFrameCount();
            this.frameRate = videoSource.getFrameRate();

            firePlaybackStateChanged();
        } catch (Exception e) {
            this.state = PlaybackState.CLOSED;
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            this.state = PlaybackState.CLOSED;
            this.currentPosition = 0;
            this.totalFrames = -1;
            this.frameRate = -1.0;

            if (videoSource != null) {
                videoSource.close();
            }

            firePlaybackStateChanged();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void play() {
        lock.writeLock().lock();
        try {
            if (state == PlaybackState.PLAYING) {
                return; // Already playing
            }

            if (state != PlaybackState.OPEN && state != PlaybackState.PAUSED) {
                throw new IllegalStateException("Cannot play in state: " + state);
            }

            this.state = PlaybackState.PLAYING;
            firePlaybackStateChanged();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void pause() {
        lock.writeLock().lock();
        try {
            if (state != PlaybackState.PLAYING) {
                throw new IllegalStateException("Cannot pause. Current state: " + state);
            }

            this.state = PlaybackState.PAUSED;
            firePlaybackStateChanged();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void stop() {
        lock.writeLock().lock();
        try {
            if (state == PlaybackState.CLOSED) {
                return; // Already stopped
            }

            this.state = PlaybackState.CLOSED;
            this.currentPosition = 0;

            if (videoSource != null) {
                videoSource.seek(0);
            }

            firePlaybackStateChanged();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void seek(int frameIndex) {
        lock.writeLock().lock();
        try {
            if (videoSource == null || !videoSource.isOpen()) {
                throw new IllegalStateException("No video is open");
            }

            if (frameIndex < 0) {
                throw new IllegalArgumentException("Frame index cannot be negative: " + frameIndex);
            }

            if (totalFrames >= 0 && frameIndex >= totalFrames) {
                throw new IllegalArgumentException("Frame index out of bounds: " + frameIndex +
                    " (max: " + (totalFrames - 1) + ")");
            }

            videoSource.seek(frameIndex);
            this.currentPosition = frameIndex;
            firePlaybackPositionChanged(frameIndex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void seekToNextKeyFrame() {
        lock.writeLock().lock();
        try {
            if (videoSource == null || !videoSource.isOpen()) {
                throw new IllegalStateException("No video is open");
            }

            // For simplicity, seek to next frame that might be a keyframe
            // In practice, this would query the underlying decoder
            int nextKeyFrame = findNextKeyFrame(currentPosition);
            if (nextKeyFrame >= 0 && nextKeyFrame <= totalFrames) {
                seek(nextKeyFrame);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Finds the next keyframe after the current position.
     * This is a simplified implementation.
     *
     * @param current current frame position
     * @return the next keyframe position, or -1 if not found
     */
    private int findNextKeyFrame(int current) {
        // Simplified: assume every 60th frame is a keyframe (typical for 30fps video)
        int interval = 60;
        int keyFrame = ((current / interval) + 1) * interval;
        return keyFrame;
    }

    @Override
    public PlaybackState getState() {
        lock.readLock().lock();
        try {
            return state;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getCurrentPosition() {
        lock.readLock().lock();
        try {
            return currentPosition;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getFrameCount() {
        lock.readLock().lock();
        try {
            return totalFrames;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public double getFrameRate() {
        lock.readLock().lock();
        try {
            return frameRate;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public double getTime() {
        lock.readLock().lock();
        try {
            if (frameRate <= 0) {
                return -1.0;
            }
            return currentPosition / frameRate;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public double getDuration() {
        lock.readLock().lock();
        try {
            if (frameRate <= 0 || totalFrames < 0) {
                return -1.0;
            }
            return totalFrames / frameRate;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isPlaying() {
        lock.readLock().lock();
        try {
            return state == PlaybackState.PLAYING;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isPaused() {
        lock.readLock().lock();
        try {
            return state == PlaybackState.PAUSED;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isClosed() {
        lock.readLock().lock();
        try {
            return state == PlaybackState.CLOSED;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Advances the playback by one frame (used for frame-by-frame playback).
     *
     * @return the next frame position, or -1 if end of video
     */
    public int stepForward() {
        lock.writeLock().lock();
        try {
            if (currentPosition >= totalFrames) {
                return -1;
            }

            int newPosition = currentPosition + 1;
            seek(newPosition);
            firePlaybackFrameChanged(currentPosition);
            return currentPosition;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Moves the playback back by one frame (used for step-backward playback).
     *
     * @return the previous frame position, or 0 as minimum
     */
    public int stepBackward() {
        lock.writeLock().lock();
        try {
            if (currentPosition <= 0) {
                return 0;
            }

            int newPosition = currentPosition - 1;
            seek(newPosition);
            firePlaybackFrameChanged(currentPosition);
            return currentPosition;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Simulates playing for a given duration (for testing).
     *
     * @param durationMs duration in milliseconds to simulate playing
     */
    public void simulatePlay(long durationMs) {
        if (frameRate <= 0) {
            return;
        }

        int framesToAdvance = (int) Math.round(durationMs / 1000.0 * frameRate);
        int maxPosition = totalFrames;

        lock.writeLock().lock();
        try {
            currentPosition = Math.min(currentPosition + framesToAdvance, maxPosition);
            if (videoSource != null && videoSource.isOpen()) {
                videoSource.seek(currentPosition);
            }
            firePlaybackPositionChanged(currentPosition);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds a playback listener to receive state change notifications.
     *
     * @param listener the listener to add
     */
    public void addPlaybackListener(PlaybackListener listener) {
        lock.writeLock().lock();
        try {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a playback listener.
     *
     * @param listener the listener to remove
     */
    public void removePlaybackListener(PlaybackListener listener) {
        lock.writeLock().lock();
        try {
            listeners.remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Fire a playback state change event.
     */
    private void firePlaybackStateChanged() {
        PlaybackState newState = state;
        List<PlaybackListener> listenerCopy;

        lock.readLock().lock();
        try {
            listenerCopy = new ArrayList<>(listeners);
        } finally {
            lock.readLock().unlock();
        }

        for (PlaybackListener listener : listenerCopy) {
            try {
                listener.playbackStateChanged(newState);
            } catch (Exception e) {
                System.err.println("Error in playback listener: " + e.getMessage());
            }
        }
    }

    /**
     * Fire a playback position change event.
     *
     * @param position the new position
     */
    private void firePlaybackPositionChanged(int position) {
        List<PlaybackListener> listenerCopy;
        double time;
        double duration;

        lock.readLock().lock();
        try {
            listenerCopy = new ArrayList<>(listeners);
            time = currentPosition / frameRate;
            duration = totalFrames / frameRate;
        } finally {
            lock.readLock().unlock();
        }

        for (PlaybackListener listener : listenerCopy) {
            try {
                listener.playbackPositionChanged(position, time, duration);
            } catch (Exception e) {
                System.err.println("Error in playback listener: " + e.getMessage());
            }
        }
    }

    /**
     * Fire a playback frame change event.
     *
     * @param frame the new frame index
     */
    private void firePlaybackFrameChanged(int frame) {
        List<PlaybackListener> listenerCopy;

        lock.readLock().lock();
        try {
            listenerCopy = new ArrayList<>(listeners);
        } finally {
            lock.readLock().unlock();
        }

        for (PlaybackListener listener : listenerCopy) {
            try {
                listener.playbackFrameChanged(frame);
            } catch (Exception e) {
                System.err.println("Error in playback listener: " + e.getMessage());
            }
        }
    }

    /**
     * Simple test of the VideoPlaybackController.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        System.out.println("=== VideoPlaybackController Test ===\n");

        VideoPlaybackController controller = new VideoPlaybackController();

        System.out.println("Initial state: " + controller.getState());

        // Test closed state operations
        try {
            controller.play();
        } catch ( IllegalStateException e) {
            System.out.println("Correctly rejected play in closed state: " + e.getMessage());
        }

        // Create a test path for demo (using a placeholder)
        // In real use, you'd provide a valid video path
        Path dummyPath = Path.of("/dev/null"); // Invalid for real use

        // Test with empty controller
        System.out.println("\nController state after creation:");
        System.out.println("  State: " + controller.getState());
        System.out.println("  Position: " + controller.getCurrentPosition());
        System.out.println("  Frame rate: " + controller.getFrameRate());
        System.out.println("  Frame count: " + controller.getFrameCount());

        // Test adding and removing listeners
        controller.addPlaybackListener(new PlaybackListener() {
            @Override
            public void playbackStateChanged(PlaybackState state) {
                System.out.println("  State changed to: " + state);
            }

            @Override
            public void playbackPositionChanged(int position, double time, double duration) {
                System.out.println("  Position changed: frame=" + position + ", time=" + time + "s");
            }

            @Override
            public void playbackFrameChanged(int frame) {
                System.out.println("  Frame changed to: " + frame);
            }

            @Override
            public void playbackStopped() {
                System.out.println("  Playback stopped");
            }
        });

        System.out.println("\n=== Test Complete ===");
        System.out.println("Note: Full testing requires a valid video file.");
    }
}
