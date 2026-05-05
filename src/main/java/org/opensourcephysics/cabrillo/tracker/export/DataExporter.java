package org.opensourcephysics.cabrillo.tracker.export;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.calibration.Calibration;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports track data to various formats.
 */
public class DataExporter {

    public static void exportToCsv(Track track, double fps, Calibration calibration, Path path) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path.toFile()))) {
            writer.println("frame,time_s,pixel_x,pixel_y,world_x,world_y");

            List<Point> points = track.getPoints();
            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                double t = p.getFrame() / fps;

                double px = p.hasPixel() ? p.pixelX() : Double.NaN;
                double py = p.hasPixel() ? p.pixelY() : Double.NaN;
                double wx = p.hasWorld() ? p.worldX() : Double.NaN;
                double wy = p.hasWorld() ? p.worldY() : Double.NaN;

                // If missing world but has pixel and calibration, compute world
                if (Double.isNaN(wx) && !Double.isNaN(px) && calibration != null) {
                    Calibration.WorldPoint wp = calibration.toWorld(px, py);
                    wx = wp.x();
                    wy = wp.y();
                }

                writer.printf("%d,%.4f,%.2f,%.2f,%.4f,%.4f%n",
                    p.getFrame(), t, px, py, wx, wy);
            }
        }
    }
}
