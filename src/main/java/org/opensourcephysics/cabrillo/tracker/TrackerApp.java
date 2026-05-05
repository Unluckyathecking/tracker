package org.opensourcephysics.cabrillo.tracker;

import org.opensourcephysics.cabrillo.tracker.calibration.Calibration;
import org.opensourcephysics.cabrillo.tracker.data.model.*;
import org.opensourcephysics.cabrillo.tracker.project.TrackerProject;
import org.opensourcephysics.cabrillo.tracker.tracking.TrackingController;
import org.opensourcephysics.cabrillo.tracker.analysis.Differentiator;
import org.opensourcephysics.cabrillo.tracker.analysis.CurveFitter;

import java.util.*;

/**
 * Simple demo of the rebuilt Tracker core.
 */
public class TrackerApp {
    
    public static void main(String[] args) {
        System.out.println("=== Tracker Rebuild (Fundamentals) ===\n");
        
        TrackerProject project = new TrackerProject("Projectile Motion Demo");
        System.out.println("Created: " + project);
        
        Calibration cal = new Calibration().withScale(10.0, 320, 240);
        project.setCalibration(cal);
        System.out.println("Calibration: 10 px = 1 m, origin at (320, 240)");
        
        Track ball = Track.create("Ball", TrackType.POINT_MASS);
        
        double dt = 1.0 / 30.0;
        double g = 9.8;
        double v0x = 5.0;
        double v0y = 8.0;
        double x0 = 1.0;
        double y0 = 10.0;
        
        for (int frame = 0; frame <= 60; frame++) {
            double t = frame * dt;
            double worldX = x0 + v0x * t;
            double worldY = y0 + v0y * t - 0.5 * g * t * t;
            
            if (worldY < 0) break;
            
            double pixelX = 320 + worldX * 10;
            double pixelY = 240 - worldY * 10;
            
            Point p = Point.atPixel(frame, pixelX, pixelY).withWorld(worldX, worldY);
            ball = ball.withPoint(frame, p);
        }
        
        project.addTrack(ball);
        System.out.println("Added track: " + ball);
        System.out.println("  Points: " + ball.pointCount());
        
        List<Integer> frames = ball.frames();
        List<Double> times = new ArrayList<>();
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        
        for (int f : frames) {
            ball.point(f).ifPresent(p -> {
                times.add(f * dt);
                xs.add(p.worldX());
                ys.add(p.worldY());
            });
        }
        
        Differentiator diff = new Differentiator(Differentiator.Method.FIVE_POINT, 5);
        List<Double> vx = diff.differentiate(xs, dt);
        List<Double> vy = diff.differentiate(ys, dt);
        List<Double> ax = diff.differentiateTwice(xs, dt);
        List<Double> ay = diff.differentiateTwice(ys, dt);
        
        System.out.println("\n=== Kinematics (sample) ===");
        for (int i = 0; i < Math.min(5, times.size()); i++) {
            System.out.printf("t=%.3fs: x=%.2fm, y=%.2fm, vx=%.2f, vy=%.2f, ax=%.2f, ay=%.2f%n",
                times.get(i), xs.get(i), ys.get(i),
                vx.get(i), vy.get(i), ax.get(i), ay.get(i));
        }
        
        CurveFitter.LinearResult fitX = CurveFitter.fitLinear(times, xs);
        System.out.printf("\nFit x(t): x = %.3f + %.3f*t (R^2=%.4f)%n",
            fitX.intercept, fitX.slope, fitX.rSquared);
        
        CurveFitter.PolynomialResult fitY = CurveFitter.fitPolynomial(times, ys, 2);
        System.out.printf("Fit y(t): quadratic coeffs = %s%n",
            Arrays.toString(fitY.coefficients));
        
        System.out.println("\n=== Physics Verification ===");
        System.out.printf("Expected vx = %.1f m/s, fitted = %.3f m/s%n", v0x, fitX.slope);
        System.out.printf("Expected ay = %.1f m/s^2, measured avg = %.3f m/s^2%n", 
            -g, ay.stream().skip(2).limit(ay.size()-4)
                .mapToDouble(Double::doubleValue).average().orElse(0));
        
        System.out.println("\n=== Done ===");
    }
}
