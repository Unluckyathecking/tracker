# Tracker - Rebuild from Fundamentals

## What Tracker Does (Core Purpose)
Tracker is a video analysis and modeling tool for physics education. Users:
1. Load a video of physical motion
2. Mark positions of objects frame-by-frame
3. Calibrate real-world distances/angles
4. Compute kinematics (position, velocity, acceleration)
5. Fit mathematical models to data
6. Visualize and export results

## Fundamental Components (First Principles)

### 1. MEDIA LAYER
- **Purpose**: Decode video into frames
- **Inputs**: Video file (mp4, mov, avi, gif)
- **Outputs**: Individual images (BufferedImage) with timestamps
- **Key operations**: play, pause, seek, next/prev frame, get frame N

### 2. TRACKING LAYER  
- **Purpose**: Record object positions over time
- **Inputs**: User clicks on frame, or automated detection
- **Outputs**: Time-series of (x,y) pixel coordinates per track
- **Key operations**: add point, delete point, interpolate, auto-track

### 3. CALIBRATION LAYER
- **Purpose**: Convert pixels to real-world units
- **Inputs**: Known distance/angle in video, coordinate system
- **Outputs**: Scale factor (m/pixel), origin offset, rotation
- **Key operations**: set scale, set origin, set tilt

### 4. ANALYSIS LAYER
- **Purpose**: Compute physics from tracked data
- **Inputs**: Calibrated time-series positions
- **Outputs**: Velocity, acceleration, energy, momentum, etc.
- **Key operations**: numerical differentiation, smoothing, statistics

### 5. MODELING LAYER
- **Purpose**: Compare data to theoretical models
- **Inputs**: Track data, model equations
- **Outputs**: Fitted parameters, residuals, overlay visualization
- **Key operations**: least-squares fit, dynamic simulation

### 6. PRESENTATION LAYER
- **Purpose**: Display everything to user
- **Inputs**: All other layers
- **Outputs**: Interactive GUI with video, plots, tables
- **Key operations**: render video with overlays, update plots, export

## Architecture Principles
- Immutable data structures
- Event-driven communication
- Layered architecture (no circular deps)
- Testable units
- Modern Java (21+) with records, pattern matching
