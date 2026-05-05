package org.opensourcephysics.cabrillo.tracker.tracking;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;

/**
 * A reversible edit to a track. {@code before} and {@code after} are the
 * point at {@code frame} on the track identified by {@code trackId}, before
 * and after the edit. Either may be null (null = no point at that frame).
 */
public record TrackEdit(String trackId, int frame, Point before, Point after) {
    public TrackEdit inverse() {
        return new TrackEdit(trackId, frame, after, before);
    }
}
