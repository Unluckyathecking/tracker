package org.opensourcephysics.cabrillo.tracker.project;

public class ProjectConfiguration {
    private int videoWidth = 800;
    private int videoHeight = 600;
    private boolean recordEnabled = true;
    private FrameController frameController;

    public ProjectConfiguration() {
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public boolean isRecordEnabled() {
        return recordEnabled;
    }

    public void setRecordEnabled(boolean recordEnabled) {
        this.recordEnabled = recordEnabled;
    }

    public FrameController getFrameController() {
        return frameController;
    }

    public void setFrameController(FrameController frameController) {
        this.frameController = frameController;
    }

    public static class FrameController {
        private int minFrame = 0;
        private int maxFrame = 0;
        private int currentFrame = 0;

        public int getMinFrame() {
            return minFrame;
        }

        public void setMinFrame(int minFrame) {
            this.minFrame = minFrame;
        }

        public int getMaxFrame() {
            return maxFrame;
        }

        public void setMaxFrame(int maxFrame) {
            this.maxFrame = maxFrame;
        }

        public int getCurrentFrame() {
            return currentFrame;
        }

        public void setCurrentFrame(int currentFrame) {
            this.currentFrame = currentFrame;
        }

        public void nextFrame() {
            if (currentFrame < maxFrame) {
                currentFrame++;
            }
        }

        public void previousFrame() {
            if (currentFrame > minFrame) {
                currentFrame--;
            }
        }
    }
}
