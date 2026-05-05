# Tracker Rebuild Project - First Principles Refactoring

## Project Overview
Rebuild the **Tracker** video analysis and modeling tool from first principles. This is a physics education tool that allows users to:
- Import video files (MP4, MOV, AVI, etc.) and images
- Track particle positions frame by frame
- Perform coordinate calibration
- Fit analytic functions to data
- Model physical systems (projectile motion, pendulums, etc.)
- Export data and create plots

## Current State
- ~167 Java files
- Built on OSS Physics (OSP) framework
- Uses Xuggle for video processing
- Complex Swing-based GUI
- Legacy architecture with tangled dependencies

## Goals - Rebuild from First Principles

### 1. **Core Architecture Reimagining**
Starting from scratch principles:
- **Data Model**: Design clean data structures for tracks, points, frames, measurements
- **Video Engine**: Modern video processing (FFmpeg/java-video-bootstrapper or WebAssembly)
- **Physics Engine**: Clean models for particle dynamics, forces, differential equations
- **UI Framework**: Modern reactive UI (JavaFX, QtJambi, or web-based Electron)
- **State Management**: Immutable state, event-driven architecture

### 2. **Key Components to Rebuild**

#### A. Video/Input Layer
- Video playback (seek, play, pause, frame-by-frame)
- Image import
- Frame extraction and caching
- Basic video metadata

#### B. Tracking System
- Point tracking (click/drag to mark positions)
- Automated tracking (template matching, color detection)
- Multi-particle tracking
- Calibration tools (distance, angle, perspective)

#### C. Data Processing
- Time series management
- Derivatives (velocity, acceleration)
- Data filtering/smoothing
- Statistical analysis

#### D. Modeling & Analysis
- Function fitting (linear, polynomial, custom)
- Dynamic system modeling
- Energy/momentum calculations
- Comparison of model vs experimental data

#### E. Visualization
- Scatter plots with error bars
- Vector displays (velocity, acceleration)
- Trajectory overlays
- Multi-track management

### 3. **Implementation Plan**

#### Phase 1: Foundation (Week 1-2)
- [ ] Design core data models (Track, Point, Frame, Calibration)
- [ ] Set up build system (Maven/Gradle)
- [ ] Implement video player interface
- [ ] Basic frame rendering

#### Phase 2: Tracking (Week 3-4)
- [ ] Manual point tracking UI
- [ ] Data storage and persistence
- [ ] File I/O (.trz format)
- [ ] Export functionality

#### Phase 3: Calibration & Measurements (Week 5)
- [ ] Distance calibration
- [ ] Angle calibration
- [ ] Perspective correction
- [ ] Coordinate transforms

#### Phase 4: Analysis (Week 6-7)
- [ ] Derivatives calculation
- [ ] Data fitting
- [ ] Vector calculations
- [ ] Statistics

#### Phase 5: Modeling (Week 8)
- [ ] Analytic function panel
- [ ] Dynamic system solver
- [ ] Integration with experimental data
- [ ] Model fitting

#### Phase 6: Polish (Week 9-10)
- [ ] Refinement of UX
- [ ] Performance optimization
- [ ] Tests coverage
- [ ] Documentation

## Technical Decisions Needed

### UI Framework Options:
1. **JavaFX** - Modern Java UI, good performance
2. **Swing** - Familiar but aging tech
3. **Web-based** - Electron/React, cross-platform
4. **Kotlin Multiplatform** - Future-proof

### Video Backend Options:
1. **FFmpeg + java-video-bootstrapper**
2. **Xuggle** (current, legacy)
3. **GStreamer bindings**
4. **WebAssembly FFmpeg**

### State Management:
1. **Actor model** (Akka)
2. **Reactive streams** (RxJava, Project Reactor)
3. **Immutable state** (functional approach)
4. **Event sourcing**

## Subagent Tasks

### subagent_data_model: Design and implement core data structures
- Track, Point, Frame classes
- Calibration data model
- File format design

### subagent_video_engine: Modern video playback system
- FFmpeg integration
- Frame extraction
- Playback controls

### subagent_tracking: Point tracking functionality
- Manual tracking UI
- Auto-tracking algorithms
- Multi-particle support

### subagent_analysis: Data analysis and modeling
- Derivative calculations
- Curve fitting
- Statistical analysis

### subagent_models: Physics models and dynamic systems
- Particle models
- Dynamic system solver
- Energy calculations

### subagent_ui: Modern user interface
- Main window layout
- Interactive components
- Visualization panels

## Success Criteria
1. Can import video and track points manually
2. Can calibrate distance and angle
3. Can compute derivatives (v, a)
4. Can fit functions to data
5. Can model physical systems
6. Can export data to CSV
7. All tests pass
8. Code is well-documented
9. Performance comparable to original

## Notes
- This is a complete rewrite, not incremental improvement
- Keep the proven workflow of Tracker
- Improve upon the architecture and user experience
- Maintain compatibility with .trz files where possible
