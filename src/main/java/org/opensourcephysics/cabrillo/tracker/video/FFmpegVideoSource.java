package org.opensourcephysics.cabrillo.tracker.video;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

/**
 * FFmpeg-based implementation of VideoSource using JavaCV.
 *
 * <p>This class provides video file decoding using FFmpeg through the
 * JavaCV wrapper. It handles video file loading, frame extraction,
 * and timing information.</p>
 *
 * <p><b>Thread Safety:</b> This implementation is thread-safe for basic
 * operations. Concurrent reads from the same video are not supported.</p>
 *
 * @see VideoSource
 * @see VideoMetadata
 * @see FrameCache
 */
public class FFmpegVideoSource implements VideoSource {

    /** The currently open video path */
    private Path currentPath;

    /** Video metadata */
    private VideoMetadata metadata;

    /** Frame grabber for video decoding */
    private FFmpegFrameGrabber grabber;

    /** Converter for converting FFmpeg frames to BufferedImage */
    private Java2DFrameConverter converter;

    /** Cached frames */
    private final FrameCache frameCache;

    /** Lock for synchronization */
    private final ReentrantReadWriteLock lock;

    /** Whether the grabber has been started */
    private boolean started;

    /** Default cache size */
    private static final int DEFAULT_CACHE_SIZE = 100;

    /**
     * Creates a new FFmpegVideoSource with default cache size (100 frames).
     */
    public FFmpegVideoSource() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Creates a new FFmpegVideoSource with the specified cache size.
     *
     * @param cacheSize maximum number of frames to cache
     */
    public FFmpegVideoSource(int cacheSize) {
        this.frameCache = new FrameCache(cacheSize);
        this.lock = new ReentrantReadWriteLock();
        this.metadata = new VideoMetadata();
    }

    @Override
    public void open(Path file) {
        lock.writeLock().lock();
        try {
            if (isOpen()) {
                close();
            }

            currentPath = file;
            grabber = new FFmpegFrameGrabber(file.toAbsolutePath().toString());
            converter = new Java2DFrameConverter();

            try {
                grabber.start();
                started = true;
                metadata = extractMetadata();
            } catch (Exception e) {
                close();
                throw new IllegalArgumentException("Failed to open video: " + file, e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (grabber != null) {
                try {
                    grabber.stop();
                    grabber.release();
                } catch (Exception e) {
                    System.err.println("Error stopping grabber: " + e.getMessage());
                }
                grabber = null;
            }
            converter = null;
            frameCache.clear();
            started = false;
            metadata = new VideoMetadata();
            currentPath = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int getFrameCount() {
        lock.readLock().lock();
        try {
            return metadata.frameCount();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public double getFrameRate() {
        lock.readLock().lock();
        try {
            return metadata.frameRate();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getWidth() {
        lock.readLock().lock();
        try {
            return metadata.width();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getHeight() {
        lock.readLock().lock();
        try {
            return metadata.height();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public BufferedImage getFrame(int index) {
        lock.readLock().lock();
        try {
            validateOpen();
        } finally {
            lock.readLock().unlock();
        }

        // Check bounds
        if (index < 0 || (metadata.frameCount() >= 0 && index >= metadata.frameCount())) {
            throw new IllegalArgumentException("Invalid frame index: " + index +
                " (valid range: 0 to " + metadata.frameCount() + ")");
        }

        // Try cache first
        BufferedImage cached = frameCache.get(index);
        if (cached != null) {
            return cached;
        }

        // Decode frame
        BufferedImage frame = decodeFrameAt(index);
        if (frame != null) {
            frameCache.put(index, frame);
        }
        return frame;
    }

    @Override
    public void seek(int frameIndex) {
        lock.writeLock().lock();
        try {
            validateOpen();

            if (frameIndex < 0) {
                throw new IllegalArgumentException("Invalid frame index: " + frameIndex);
            }

            if (grabber == null) {
                throw new IllegalStateException("Grabber is null");
            }

            try {
                grabber.setFrameNumber(frameIndex);
            } catch (Exception e) {
                System.err.println("Error seeking: " + e.getMessage());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Decodes a frame at the specified index by seeking and reading frames.
     *
     * @param index the target frame index
     * @return the decoded BufferedImage, or null if frame cannot be decoded
     */
    private BufferedImage decodeFrameAt(int index) {
        if (grabber == null || converter == null) {
            return null;
        }

        try {
            // Seek to frame if not already there
            int currentFrame = grabber.getFrameNumber();
            if (currentFrame != index) {
                try {
                    grabber.setFrameNumber(index);
                } catch (Exception e) {
                    // If setFrameNumber doesn't work well, try manual seek
                    System.err.println("Using manual seek for frame " + index);
                    grabber.restart();
                    for (long i = 0; i <= index; i++) {
                        grabber.grabFrame();
                    }
                }
            }

            // Get the frame
            org.bytedeco.javacv.Frame frame = grabber.grabFrame();
            if (frame == null) {
                return null;
            }

            // Convert to BufferedImage
            BufferedImage bufferedImage = converter.convert(frame);
            if (bufferedImage != null && bufferedImage.getType() == BufferedImage.TYPE_INT_ARGB) {
                BufferedImage rgb = new BufferedImage(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                rgb.createGraphics().drawImage(bufferedImage, 0, 0, null);
                return rgb;
            }
            return bufferedImage;

        } catch (Exception e) {
            System.err.println("Error decoding frame at index " + index + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isOpen() {
        lock.readLock().lock();
        try {
            return currentPath != null && grabber != null && started;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Path getCurrentPath() {
        lock.readLock().lock();
        try {
            return currentPath;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Extracts metadata from the video file.
     *
     * @return the VideoMetadata containing video properties
     */
    private VideoMetadata extractMetadata() {
        if (grabber == null) {
            return new VideoMetadata();
        }

        try {
            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            double frameRate = grabber.getFrameRate();
            String format = grabber.getFormat();
            String codecName = "h264"; // Default or use available method

            // Try to get frame count
            int frameCount = -1;
            try {
                // Seek to end to get total frame count
                long durationInMicros = grabber.getLengthInTime();
                if (durationInMicros > 0) {
                    double totalSeconds = durationInMicros / 1_000_000.0;
                    frameCount = (int) Math.round(totalSeconds * frameRate);
                }
            } catch (Exception e) {
                // Frame count unknown
            }

            double duration = -1.0;
            try {
                long durationInMicros = grabber.getLengthInTime();
                if (durationInMicros > 0) {
                    duration = durationInMicros / 1_000_000.0;
                }
            } catch (Exception e) {
                // Duration unknown
            }

            return new VideoMetadata(width, height, frameRate, frameCount, duration,
                format != null ? format : "unknown", codecName);

        } catch (Exception e) {
            System.err.println("Error extracting metadata: " + e.getMessage());
            return new VideoMetadata();
        }
    }

    /**
     * Validates that a video is currently open.
     *
     * @throws IllegalStateException if no video is open
     */
    private void validateOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("No video is currently open");
        }
    }

    /**
     * Returns the current metadata.
     *
     * @return the VideoMetadata
     */
    public VideoMetadata getMetadata() {
        lock.readLock().lock();
        try {
            return metadata;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Simple test of the FFmpegVideoSource implementation.
     *
     * @param args command line arguments, expects path to video file
     */
    public static void main(String[] args) {
        System.out.println("=== FFmpegVideoSource Test ===\n");

        Path testPath = null;
        if (args.length > 0) {
            testPath = Path.of(args[0]);
            if (!testPath.toFile().exists()) {
                System.err.println("Video file not found: " + testPath);
                return;
            }
        }

        FFmpegVideoSource source = new FFmpegVideoSource(10);

        if (testPath != null) {
            System.out.println("Opening video: " + testPath);
            try {
                source.open(testPath);
            } catch (Exception e) {
                System.err.println("Failed to open video: " + e.getMessage());
                return;
            }

            System.out.println("Metadata: " + source.getMetadata());
            System.out.println("Frame count: " + source.getFrameCount());
            System.out.println("Frame rate: " + source.getFrameRate());
            System.out.println("Dimensions: " + source.getWidth() + "x" + source.getHeight());

            if (source.getFrameCount() > 0) {
                System.out.println("\nTesting frame retrieval...");
                int totalFrames = source.getFrameCount();

                try {
                    BufferedImage frame0 = source.getFrame(0);
                    System.out.println("Frame 0: " + (frame0 != null ? frame0.getWidth() + "x" + frame0.getHeight() : "null"));

                    int mid = totalFrames / 2;
                    source.seek(mid);
                    System.out.println("Sought to frame " + mid);

                    BufferedImage midFrame = source.getFrame(mid);
                    System.out.println("Frame " + mid + ": " + (midFrame != null ? midFrame.getWidth() + "x" + midFrame.getHeight() : "null"));

                    try {
                        source.getFrame(totalFrames + 1);
                        System.err.println("ERROR: Should have thrown exception for out of bounds");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Correctly rejected out of bounds frame index");
                    }

                } catch (Exception e) {
                    System.err.println("Error during testing: " + e.getMessage());
                }
            }

            source.close();
            System.out.println("\nClosed video source");
            System.out.println("\n=== Test Complete ===");
        } else {
            System.out.println("No video file provided for testing. Basic instantiation test:");
            System.out.println("FFmpegVideoSource created successfully");
        }
    }
}
