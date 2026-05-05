package org.opensourcephysics.cabrillo.tracker.analysis;

import java.util.*;

/**
 * Computes numerical derivatives from time-series data.
 * 
 * Fundamentals:
 * Given positions x(t), we want velocity v(t) = dx/dt and acceleration a(t) = dv/dt.
 * Since data is discrete, we use finite differences.
 * 
 * Methods supported:
 * - Two-point: (f(t+h) - f(t-h)) / 2h
 * - Five-point: higher order, more accurate
 * - Savitzky-Golay: smoothing + derivative simultaneously
 */
public class Differentiator {
    
    private final Method method;
    private final int windowSize;
    
    public Differentiator() {
        this(Method.TWO_POINT, 3);
    }
    
    public Differentiator(Method method, int windowSize) {
        this.method = method;
        this.windowSize = Math.max(3, windowSize | 1); // ensure odd >= 3
    }
    
    /**
     * Compute first derivative (e.g., velocity from position).
     */
    public List<Double> differentiate(List<Double> values, double dt) {
        if (values == null || values.size() < 3 || dt <= 0) {
            return Collections.emptyList();
        }
        
        switch (method) {
            case TWO_POINT:
                return twoPoint(values, dt);
            case FIVE_POINT:
                return fivePoint(values, dt);
            case SAVITZKY_GOLAY:
                return savitzkyGolay(values, dt, 1);
            default:
                return Collections.emptyList();
        }
    }
    
    /**
     * Compute second derivative (e.g., acceleration from position).
     */
    public List<Double> differentiateTwice(List<Double> values, double dt) {
        List<Double> first = differentiate(values, dt);
        return differentiate(first, dt);
    }
    
    private List<Double> twoPoint(List<Double> f, double dt) {
        List<Double> result = new ArrayList<>(Collections.nCopies(f.size(), 0.0));
        for (int i = 1; i < f.size() - 1; i++) {
            result.set(i, (f.get(i + 1) - f.get(i - 1)) / (2 * dt));
        }
        // Forward/backward difference at boundaries
        if (f.size() > 1) {
            result.set(0, (f.get(1) - f.get(0)) / dt);
            result.set(f.size() - 1, (f.get(f.size() - 1) - f.get(f.size() - 2)) / dt);
        }
        return result;
    }
    
    private List<Double> fivePoint(List<Double> f, double dt) {
        List<Double> result = new ArrayList<>(Collections.nCopies(f.size(), 0.0));
        for (int i = 2; i < f.size() - 2; i++) {
            // f'(x) ≈ (-f(x+2h) + 8f(x+h) - 8f(x-h) + f(x-2h)) / 12h
            double val = (-f.get(i + 2) + 8 * f.get(i + 1) 
                        - 8 * f.get(i - 1) + f.get(i - 2)) / (12 * dt);
            result.set(i, val);
        }
        // Fall back to two-point at edges
        for (int i : new int[]{0, 1, f.size() - 2, f.size() - 1}) {
            if (i > 0 && i < f.size() - 1) {
                result.set(i, (f.get(i + 1) - f.get(i - 1)) / (2 * dt));
            }
        }
        return result;
    }
    
    private List<Double> savitzkyGolay(List<Double> f, double dt, int derivative) {
        // Simplified: use polynomial fit over window
        int half = windowSize / 2;
        List<Double> result = new ArrayList<>(Collections.nCopies(f.size(), 0.0));
        
        for (int i = 0; i < f.size(); i++) {
            int start = Math.max(0, i - half);
            int end = Math.min(f.size(), i + half + 1);
            List<Double> window = f.subList(start, end);
            
            // Fit quadratic: y = a + bx + cx^2
            // derivative = b + 2cx evaluated at center
            double[] coeffs = fitQuadratic(window);
            double t = i - start; // local time in window
            result.set(i, (coeffs[1] + 2 * coeffs[2] * t) / dt);
        }
        return result;
    }
    
    /**
     * Fit quadratic y = a + bx + cx^2 to data points.
     * Returns [a, b, c].
     */
    private double[] fitQuadratic(List<Double> y) {
        int n = y.size();
        double sumX = 0, sumX2 = 0, sumX3 = 0, sumX4 = 0;
        double sumY = 0, sumXY = 0, sumX2Y = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double yi = y.get(i);
            sumX += x;
            sumX2 += x * x;
            sumX3 += x * x * x;
            sumX4 += x * x * x * x;
            sumY += yi;
            sumXY += x * yi;
            sumX2Y += x * x * yi;
        }
        
        // Solve normal equations for quadratic fit
        double[][] A = {
            {n, sumX, sumX2},
            {sumX, sumX2, sumX3},
            {sumX2, sumX3, sumX4}
        };
        double[] B = {sumY, sumXY, sumX2Y};
        
        return solve3x3(A, B);
    }
    
    private double[] solve3x3(double[][] A, double[] B) {
        // Cramer's rule for 3x3
        double det = determinant3x3(A);
        if (Math.abs(det) < 1e-10) return new double[]{0, 0, 0};
        
        double[][] A0 = {{B[0], A[0][1], A[0][2]}, {B[1], A[1][1], A[1][2]}, {B[2], A[2][1], A[2][2]}};
        double[][] A1 = {{A[0][0], B[0], A[0][2]}, {A[1][0], B[1], A[1][2]}, {A[2][0], B[2], A[2][2]}};
        double[][] A2 = {{A[0][0], A[0][1], B[0]}, {A[1][0], A[1][1], B[1]}, {A[2][0], A[2][1], B[2]}};
        
        return new double[]{determinant3x3(A0)/det, determinant3x3(A1)/det, determinant3x3(A2)/det};
    }
    
    private double determinant3x3(double[][] M) {
        return M[0][0]*(M[1][1]*M[2][2]-M[1][2]*M[2][1])
             - M[0][1]*(M[1][0]*M[2][2]-M[1][2]*M[2][0])
             + M[0][2]*(M[1][0]*M[2][1]-M[1][1]*M[2][0]);
    }
    
    public enum Method {
        TWO_POINT,
        FIVE_POINT,
        SAVITZKY_GOLAY
    }
}