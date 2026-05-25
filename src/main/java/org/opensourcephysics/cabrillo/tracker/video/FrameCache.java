package org.opensourcephysics.cabrillo.tracker.video;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LRU (Least Recently Used) cache for decoded video frames.
 *
 * <p>Provides thread-safe caching of BufferedImage frames with a configurable
 * maximum capacity. When the cache reaches its limit, old entries are evicted
 * in LRU order to make room for new frames.</p>
 *
 * <p>The cache uses a LinkedHashMap with access order enabled to maintain
 * the LRU ordering automatically.</p>
 *
 * @see VideoSource
 * @see FFmpegVideoSource
 */
public class FrameCache {

    /** Maximum number of frames to keep in cache */
    private final int maxSize;

    /** The LRU cache storage - LinkedHashMap with access order */
    private final Map<Integer, BufferedImage> cache;

    /** Lock for thread-safe access to cache state */
    private final ReentrantReadWriteLock lock;

    /** Total frames loaded across all cache accesses */
    private final java.util.concurrent.atomic.LongAdder totalLoads;

    /** Total frame cache hits */
    private final java.util.concurrent.atomic.LongAdder totalHits;

    /**
     * Creates a FrameCache with default maximum capacity of 100 frames.
     *
     * @see #FrameCache(int)
     */
    public FrameCache() {
        this(100);
    }

    /**
     * Creates a FrameCache with the specified maximum capacity.
     *
     * @param maxSize maximum number of frames to cache; must be positive
     * @throws IllegalArgumentException if maxSize is not positive
     */
    public FrameCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive, got: " + maxSize);
        }
        this.maxSize = maxSize;
        // LinkedHashMap with accessOrder=true enables LRU behavior
        this.cache = new LinkedHashMap<Integer, BufferedImage>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, BufferedImage> eldest) {
                return size() > FrameCache.this.maxSize;
            }
        };
        this.lock = new ReentrantReadWriteLock();
        this.totalLoads = new java.util.concurrent.atomic.LongAdder();
        this.totalHits = new java.util.concurrent.atomic.LongAdder();
    }

    /**
     * Retrieves a frame from the cache if present.
     *
     * <p>Accessing a frame updates its position in the LRU order.
     * This method does not load a new frame - call loadFrame() for that.</p>
     *
     * @param frameIndex the frame index to retrieve
     * @return the cached BufferedImage, or null if not found
     * @throws IllegalStateException if frameIndex is invalid
     */
    public BufferedImage get(int frameIndex) {
        if (frameIndex < 0) {
            throw new IllegalStateException("frameIndex cannot be negative: " + frameIndex);
        }

        lock.readLock().lock();
        try {
            BufferedImage frame = cache.get(frameIndex);
            if (frame != null) {
                totalHits.increment();
            }
            return frame;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Loads and stores a frame in the cache.
     *
     * <p>If the frame is already cached, the method is a no-op.
     * If the cache is full, the least recently used entry will be evicted.</p>
     *
     * @param frameIndex the frame index
     * @param frame the BufferedImage representing the frame
     * @throws IllegalArgumentException if frameIndex is invalid or frame is null
     */
    public void put(int frameIndex, BufferedImage frame) {
        if (frameIndex < 0) {
            throw new IllegalArgumentException("frameIndex cannot be negative: " + frameIndex);
        }
        if (frame == null) {
            throw new IllegalArgumentException("frame cannot be null");
        }

        lock.writeLock().lock();
        try {
            // Only update if not already present (hit means no load needed)
            if (!cache.containsKey(frameIndex)) {
                totalLoads.increment();
                cache.put(frameIndex, frame);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a specific frame from the cache.
     *
     * @param frameIndex the frame index to remove
     * @return true if the frame was removed, false if not found
     */
    public boolean remove(int frameIndex) {
        lock.writeLock().lock();
        try {
            return cache.remove(frameIndex) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all frames from the cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the current number of frames in the cache.
     *
     * @return cache size
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the maximum number of frames the cache can hold.
     *
     * @return maximum cache size
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Returns the total number of frame cache loads.
     *
     * @return total loads count
     */
    public long getTotalLoads() {
        return totalLoads.sum();
    }

    /**
     * Returns the total number of cache hits.
     *
     * @return total hits count
     */
    public long getTotalHits() {
        return totalHits.sum();
    }

    /**
     * Returns the cache hit ratio (hits / total loads).
     *
     * @return hit ratio as a double between 0 and 1, or 0.0 if no loads have occurred
     */
    public double getHitRatio() {
        long loads = totalLoads.sum();
        if (loads == 0) {
            return 0.0;
        }
        return (double) totalHits.sum() / loads;
    }

    /**
     * Returns a summary string showing cache statistics.
     *
     * @return statistics string
     */
    @Override
    public String toString() {
        long loads = totalLoads.sum();
        long hits = totalHits.sum();
        double ratio = loads == 0 ? 0.0 : (double) hits / loads;
        return String.format("FrameCache[size=%d/%d, hits=%d, loads=%d, ratio=%.2f%%]",
            size(), maxSize, hits, loads, ratio * 100);
    }

    /**
     * Simple test of the FrameCache implementation.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        System.out.println("=== FrameCache Test ===\n");

        FrameCache cache = new FrameCache(5);
        System.out.println("Created cache with max size 5");
        System.out.println(cache + "\n");

        // Simulate adding frames
        for (int i = 0; i < 7; i++) {
            BufferedImage frame = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            cache.put(i, frame);
            System.out.printf("Added frame %d: %s\n", i, cache);
        }

        // Access frame 0 to change LRU order
        System.out.println("\nAccessing frame 0...");
        BufferedImage accessed = cache.get(0);
        System.out.println(cache + "\n");

        // Add more frames to trigger eviction
        for (int i = 7; i < 10; i++) {
            BufferedImage frame = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            cache.put(i, frame);
            System.out.printf("Added frame %d: %s\n", i, cache);
        }
        
        // Check if old frames were evicted
        System.out.println("\nChecking evicted frames:");
        System.out.println("Frame 0 present: " + (cache.get(0) != null));
        System.out.println("Frame 1 present: " + (cache.get(1) != null));
        System.out.println("Frame 6 present: " + (cache.get(6) != null));
        
        System.out.println("\n=== Test Complete ===");
    }
}
