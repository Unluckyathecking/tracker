package org.opensourcephysics.cabrillo.tracker.tracking;

import org.opensourcephysics.cabrillo.tracker.data.model.Track;

/** Notified after the controller mutates a track in the project. */
public interface TrackingListener {
    void onTrackChanged(Track track);
}
