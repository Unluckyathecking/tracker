package org.opensourcephysics.cabrillo.tracker.tracking;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.project.TrackerProject;

public class TrackingController {
    private TrackerProject project;
    private int currentIndex = 0;

    public TrackingController(TrackerProject project) {
        this.project = project;
    }

    public TrackerProject getProject() {
        return project;
    }

    public void setProject(TrackerProject project) {
        this.project = project;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public void next() {
        if (currentIndex < 10000) {
            currentIndex++;
        }
    }

    public void previous() {
        if (currentIndex > 0) {
            currentIndex--;
        }
    }

    public Point getCurrentPoint() {
        Track track = getCurrentTrack();
        if (track != null && !track.getPoints().isEmpty()) {
            return track.getPoint(currentIndex);
        }
        return null;
    }

    public Track getCurrentTrack() {
        if (!project.getTracks().isEmpty()) {
            return project.getTracks().get(0);
        }
        return null;
    }

    public Point addPoint(double x, double y) {
        Track track = getCurrentTrack();
        if (track == null) {
            track = new Track();
            project.addTrack(track);
        }
        Point point = new Point(currentIndex, x, y);
        Track updated = track.addPoint(point);
        // Replace in project since Track is immutable
        int idx = project.getTracks().indexOf(track);
        if (idx >= 0) {
            project.getTracks().set(idx, updated);
        } else {
            project.getTracks().add(updated);
        }
        return point;
    }
}
