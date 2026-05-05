package org.opensourcephysics.cabrillo.tracker.data.model;

/**
 * Types of tracks in the Tracker system.
 * These are physics/measurement types, not data structure types.
 */
public enum TrackType {
    /** Default track type */
    DEFAULT("Default"),

    /** A point mass track - tracks position of a small object */
    POINT_MASS("Point Mass"),
    
    /** A vector track - tracks velocity or force vectors */
    VECTOR("Vector"),
    
    /** A center of mass track - tracks the center of mass of a system */
    CENTER_OF_MASS("Center of Mass"),
    
    /** A line profile - draws a line through multiple points */
    LINE_PROFILE("Line Profile"),
    
    /** An RGB region - analyzes color in a specific region */
    RGB_REGION("RGB Region"),
    
    /** A protractor - measures angles */
    PROTRACTOR("Protractor"),
    
    /** A calibration track - defines the scale and reference for world units */
    CALIBRATION("Calibration"),
    
    /** A circle fitter - fits a circle to points */
    CIRCLE_FITTER("Circle Fitter"),
    
    /** An analytic particle - follows an analytic function */
    ANALYTIC_PARTICLE("Analytic Particle"),
    
    /** A dynamic particle - responds to physics */
    DYNAMIC_PARTICLE("Dynamic Particle"),
    
    /** A pencil drawing - free-form drawing track */
    PENCIL_DRAWING("Pencil Drawing");
    
    private final String label;
    
    TrackType(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
