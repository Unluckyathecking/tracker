package org.opensourcephysics.cabrillo.tracker.analysis;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;

import java.util.List;
import java.util.TreeSet;

/**
 * Utility that derives a synthetic CENTER_OF_MASS {@link Track} from a set of
 * input tracks and their corresponding masses.
 */
public final class CenterOfMass {

    private CenterOfMass() {}

    /**
     * Build a Track named {@code name} of type CENTER_OF_MASS containing one
     * point per frame where ALL input tracks have a point. {@code masses[i]} is
     * the mass of {@code tracks.get(i)} (kg by convention).
     *
     * <p>For each common frame:
     * <ul>
     *   <li>pixel coords: mass-weighted average; frame skipped if any track lacks pixel data.</li>
     *   <li>world coords: mass-weighted average; NaN for both axes if any track lacks world data.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if tracks is empty, sizes mismatch, or any mass &lt;= 0
     */
    public static Track compute(String name, List<Track> tracks, double[] masses) {
        if (tracks == null || tracks.isEmpty()) {
            throw new IllegalArgumentException("tracks must not be empty");
        }
        if (masses == null || masses.length != tracks.size()) {
            throw new IllegalArgumentException("masses.length must equal tracks.size()");
        }
        for (int i = 0; i < masses.length; i++) {
            if (masses[i] <= 0) {
                throw new IllegalArgumentException("mass at index " + i + " must be > 0, got " + masses[i]);
            }
        }

        double totalMass = 0;
        for (double m : masses) totalMass += m;

        // Intersection of frame sets across all tracks
        TreeSet<Integer> commonFrames = new TreeSet<>(tracks.get(0).frames());
        for (int i = 1; i < tracks.size(); i++) {
            commonFrames.retainAll(tracks.get(i).frames());
        }

        Track result = Track.create(name, TrackType.CENTER_OF_MASS);

        for (int f : commonFrames) {
            double pxSum = 0, pySum = 0;
            double wxSum = 0, wySum = 0;
            boolean allHavePixel = true;
            boolean allHaveWorld = true;

            for (int i = 0; i < tracks.size(); i++) {
                Point p = tracks.get(i).point(f).orElseThrow();
                double m = masses[i];
                if (p.hasPixel()) {
                    pxSum += m * p.pixelX();
                    pySum += m * p.pixelY();
                } else {
                    allHavePixel = false;
                }
                if (p.hasWorld()) {
                    wxSum += m * p.worldX();
                    wySum += m * p.worldY();
                } else {
                    allHaveWorld = false;
                }
            }

            if (!allHavePixel) continue; // frame unusable — pixel data missing for at least one track

            double pxAvg = pxSum / totalMass;
            double pyAvg = pySum / totalMass;
            double wxAvg = allHaveWorld ? wxSum / totalMass : Double.NaN;
            double wyAvg = allHaveWorld ? wySum / totalMass : Double.NaN;

            result = result.withPoint(f, Point.of(f, pxAvg, pyAvg, wxAvg, wyAvg));
        }

        return result;
    }
}
