package org.opensourcephysics.cabrillo.tracker.project;

import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.tracking.TrackingController;
import org.opensourcephysics.cabrillo.tracker.calibration.Calibration;

import java.util.ArrayList;
import java.util.List;

public class TrackerProject {
    private String name;
    private ProjectConfiguration configuration;
    private List<Track> tracks;
    private TrackingController trackingController;
    private Calibration calibration;

    public TrackerProject() {
        this("Untitled Project");
    }

    public TrackerProject(String name) {
        this.name = name;
        this.configuration = new ProjectConfiguration();
        this.tracks = new ArrayList<>();
        this.trackingController = new TrackingController(this);
        this.calibration = new Calibration();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProjectConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ProjectConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void addTrack(Track track) {
        tracks.add(track);
    }

    public TrackingController getTrackingController() {
        return trackingController;
    }

    public void setTrackingController(TrackingController trackingController) {
        this.trackingController = trackingController;
    }

    public Calibration getCalibration() {
        return calibration;
    }

    public void setCalibration(Calibration calibration) {
        this.calibration = calibration;
    }
}
