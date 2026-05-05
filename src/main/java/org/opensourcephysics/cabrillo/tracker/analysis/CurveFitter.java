package org.opensourcephysics.cabrillo.tracker.analysis;

import java.util.*;

/**
 * Fits mathematical curves to experimental data.
 * 
 * Fundamentals:
 * Given data points (t, y), find the best-fit parameters for a model function y = f(t; params).
 * Uses least-squares minimization.
 * 
 * Supported models:
 * - Linear: y = a + bt
 * - Polynomial: y = a0 + a1*t + a2*t^2 + ... + an*t^n
 */
public class CurveFitter {
    
    /**
     * Fit a linear model y = a + b*x.
     */
    public static LinearResult fitLinear(List<Double> x, List<Double> y) {
        double n = x.size();
        if (n == 0) return new LinearResult(0, 0, 0);
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double xi = x.get(i);
            double yi = y.get(i);
            sumX += xi;
            sumY += yi;
            sumXY += xi * yi;
            sumX2 += xi * xi;
        }
        
        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-10) {
            return new LinearResult(0, 0, 0);
        }
        
        double b = (n * sumXY - sumX * sumY) / denom;
        double a = (sumY - b * sumX) / n;
        
        double ssRes = 0, ssTot = 0;
        double meanY = sumY / n;
        for (int i = 0; i < n; i++) {
            double pred = a + b * x.get(i);
            ssRes += Math.pow(y.get(i) - pred, 2);
            ssTot += Math.pow(y.get(i) - meanY, 2);
        }
        double r2 = ssTot > 0 ? 1 - ssRes / ssTot : 0;
        
        return new LinearResult(a, b, r2);
    }
    
    /**
     * Fit a polynomial of given degree using least squares.
     * Returns coefficients [a0, a1, ..., an] for y = a0 + a1*x + ... + an*x^n
     */
    public static PolynomialResult fitPolynomial(List<Double> x, List<Double> y, int degree) {
        int n = x.size();
        if (n < degree + 1) {
            return new PolynomialResult(new double[degree + 1]);
        }
        
        // Build and solve normal equations: X'X * coeffs = X'y
        double[][] X = new double[n][degree + 1];
        double[] yArray = new double[n];
        
        for (int i = 0; i < n; i++) {
            double xi = x.get(i);
            yArray[i] = y.get(i);
            for (int d = 0; d <= degree; d++) {
                X[i][d] = Math.pow(xi, d);
            }
        }
        
        // Compute X'X (degree+1 x degree+1)
        int m = degree + 1;
        double[][] XtX = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                double sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += X[k][i] * X[k][j];
                }
                XtX[i][j] = sum;
            }
        }
        
        // Compute X'y
        double[] Xty = new double[m];
        for (int i = 0; i < m; i++) {
            double sum = 0;
            for (int k = 0; k < n; k++) {
                sum += X[k][i] * yArray[k];
            }
            Xty[i] = sum;
        }
        
        // Solve using Gaussian elimination
        double[] coeffs = solveLinearSystem(XtX, Xty);
        return new PolynomialResult(coeffs);
    }
    
    /** Solve linear system Ax = b using Gaussian elimination with partial pivoting */
    private static double[] solveLinearSystem(double[][] A, double[] b) {
        int n = A.length;
        double[][] M = new double[n][n + 1];
        
        // Augment matrix
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            M[i][n] = b[i];
        }
        
        // Forward elimination
        for (int k = 0; k < n; k++) {
            // Find pivot
            int maxRow = k;
            for (int i = k + 1; i < n; i++) {
                if (Math.abs(M[i][k]) > Math.abs(M[maxRow][k])) {
                    maxRow = i;
                }
            }
            
            // Swap rows
            double[] temp = M[k];
            M[k] = M[maxRow];
            M[maxRow] = temp;
            
            if (Math.abs(M[k][k]) < 1e-10) {
                continue; // Singular matrix
            }
            
            // Eliminate
            for (int i = k + 1; i < n; i++) {
                double factor = M[i][k] / M[k][k];
                for (int j = k; j <= n; j++) {
                    M[i][j] -= factor * M[k][j];
                }
            }
        }
        
        // Back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0;
            for (int j = i + 1; j < n; j++) {
                sum += M[i][j] * x[j];
            }
            x[i] = (M[i][n] - sum) / M[i][i];
        }
        
        return x;
    }
    
    /**
     * Evaluate a polynomial at given x.
     */
    public static double evaluatePolynomial(double[] coeffs, double x) {
        double result = 0;
        double xPower = 1;
        for (double c : coeffs) {
            result += c * xPower;
            xPower *= x;
        }
        return result;
    }
    
    // Result classes
    public static class LinearResult {
        public final double intercept;
        public final double slope;
        public final double rSquared;
        
        public LinearResult(double intercept, double slope, double rSquared) {
            this.intercept = intercept;
            this.slope = slope;
            this.rSquared = rSquared;
        }
        
        public double predict(double x) {
            return intercept + slope * x;
        }
    }
    
    public static class PolynomialResult {
        public final double[] coefficients;
        
        public PolynomialResult(double[] coefficients) {
            this.coefficients = coefficients;
        }
        
        public double predict(double x) {
            return evaluatePolynomial(coefficients, x);
        }
        
        public int degree() {
            return coefficients.length - 1;
        }
    }
}