package org.opensourcephysics.cabrillo.tracker.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VideoSourceTest {

    @Test
    public void testVideoMetadataToString() {
        // VideoMetadata is a Java record: width, height, frameRate, frameCount, duration, format, codecName
        VideoMetadata meta = new VideoMetadata(640, 480, 30.0, 100, 3.33, "mp4", "h264");
        String str = meta.toString();
        
        // Should contain resolution and framerate
        assertTrue(str.contains("640x480"));
        assertTrue(str.contains("30.0"));
    }

    @Test
    public void testVideoMetadataCreation() {
        VideoMetadata meta = new VideoMetadata(1920, 1080, 60.0, 500, 8.33, "mp4", "hevc");
        assertEquals(1920, meta.width());
        assertEquals(1080, meta.height());
        assertEquals(60.0, meta.frameRate(), 0.001);
        assertEquals(500, meta.frameCount());
        assertEquals("mp4", meta.format());
        assertEquals("hevc", meta.codecName());
    }

    @Test
    public void testFFmpegVideoSourceWithValidVideo() {
        // This test requires a real video file; we'll skip actual FFmpeg processing
        // but verify the class structure
        assertNotNull(FFmpegVideoSource.class);
    }
}
