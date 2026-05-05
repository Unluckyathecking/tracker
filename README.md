# Tracker Rebuild (From First Principles)

A clean-room rebuild of the [OpenSourcePhysics Tracker](https://github.com/OpenSourcePhysics/tracker) video analysis application, redesigned from fundamentals with modern Java practices.

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Video     │────▶│  Tracking   │────▶│  Analysis   │
│   Engine    │     │ Controller  │     │ (diff/fit)  │
│  (FFmpeg)   │     │             │     │             │
└─────────────┘     └─────────────┘     └─────────────┘
       │                    │                   │
       ▼                    ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ FrameCache  │     │   Track     │     │    Export   │
│   (LRU)     │     │ (immutable) │     │  CSV/JSON   │
└─────────────┘     └─────────────┘     └─────────────┘
       │                    │                   │
       ▼                    ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Calibration │◀────│   Project   │────▶│  JavaFX UI  │
│  (px↔world) │     │   (Gson)    │     │  (Canvas)   │
└─────────────┘     └─────────────┘     └─────────────┘
```

## Core Design Decisions

1. **Immutable Data Model** — `Point` and `Track` are immutable. Modifications return new instances, eliminating an entire class of concurrency and state bugs.
2. **Dual Coordinate System** — Every `Point` stores both pixel and world coordinates (NaN if unknown). This removes the need for constant calibration lookups in the UI layer.
3. **Calibration as Pure Function** — `Calibration` transforms between coordinate spaces with no mutable state. Scale + origin define the affine map.
4. **Video Engine Abstraction** — `VideoSource` interface hides FFmpeg/Javacv details. `FrameCache` provides LRU buffering for smooth scrubbing.

## Build

```bash
cd /tmp/tracker_rebuild
mvn clean package          # builds JAR at target/tracker-rebuild-1.0.0-SNAPSHOT.jar
mvn test                   # 51 tests
```

## Run Console Demo

```bash
mvn compile exec:java -Dexec.mainClass="org.opensourcephysics.cabrillo.tracker.TrackerApp"
```

Shows projectile motion simulation with numerical differentiation and least-squares fitting.

## Run JavaFX UI

```bash
# On Linux with display:
mvn javafx:run

# Or directly:
mvn compile exec:java -Dexec.mainClass="org.opensourcephysics.cabrillo.tracker.ui.TrackerFxApp"
```

**Keyboard shortcuts:**
- `Left/Right` arrows — previous/next frame
- `Space` — play/pause
- `M` — toggle mark mode (click on video to mark points)
- `Ctrl+N` — new track

**Mouse:**
- Click "Mark" button, then click on video to mark points
- Use slider or arrow buttons to navigate frames
- Tracks appear in the list on the right; select one to view/edit

## Features

- **Video loading**: MP4/AVI/MOV/MKV via FFmpeg (50-frame LRU cache)
- **Frame navigation**: scrub slider, play/pause, arrow keys
- **Click-to-mark**: Mark points on any frame, stored by frame number
- **Track management**: multiple tracks, color-coded in list
- **Trajectory plot**: real-time XY position plot
- **Calibration**: pixel-to-world coordinate transforms
- **Analysis**: automatic velocity/acceleration derivation, curve fitting
- **Export**: CSV with calibrated world coordinates
- **Serialization**: JSON project save/load via Gson

## Module Overview

| Package | Classes | Purpose |
|---------|---------|---------|
| `data.model` | `Point`, `Track`, `TrackType` | Immutable domain objects |
| `calibration` | `Calibration` | Pixel ↔ world coordinate transforms |
| `analysis` | `Differentiator`, `CurveFitter`, `TrackAnalyzer` | Numerical derivatives, linear/polynomial regression, full kinematic analysis |
| `video` | `VideoSource`, `FFmpegVideoSource`, `VideoMetadata`, `FrameCache`, `VideoPlaybackController` | Video decoding, caching, playback state machine |
| `tracking` | `TrackingController` | Frame navigation, point marking |
| `project` | `TrackerProject`, `ProjectConfiguration` | Session container, settings |
| `persist` | `ProjectSerializer`, `GsonProjectSerializer` | JSON serialization via Gson |
| `export` | `DataExporter` | CSV export with calibration-aware world coords |
| `ui` | `TrackerFxApp` | JavaFX canvas + track list + table + trajectory plot |

## Key API Examples

```java
// Create a calibrated project
TrackerProject project = new TrackerProject("Projectile Motion");
project.setCalibration(new Calibration(0.1, 320, 240)); // 10 cm/px, origin (320,240)

// Build a track immutably
Track ball = Track.create("Ball", TrackType.POINT_MASS);
for (int frame = 0; frame < 60; frame++) {
    double px = 320 + frame * 5;
    double py = 240 - frame * frame * 0.1;
    Point p = Point.atPixel(frame, px, py).withWorld(px * 0.1, (240 - py) * 0.1);
    ball = ball.withPoint(frame, p);
}

// Automatic kinematic analysis
TrackAnalyzer analyzer = new TrackAnalyzer(1.0 / 30.0);
TrackAnalyzer.KinematicResult result = analyzer.analyze(ball);
System.out.printf("vx = %.2f m/s, ay = %.2f m/s^2%n",
    result.xFit().slope, result.averageAy());

// Export
DataExporter.exportToCsv(ball, 30.0, project.getCalibration(), Path.of("data.csv"));
```

## Technology Stack

- Java 17
- JavaFX 17 (SwingFXUtils for BufferedImage conversion)
- Maven 3
- JUnit 5 + AssertJ (51 tests, all passing)
- Gson (serialization)
- JavaCV / bytedeco FFmpeg (video decoding)
- Apache Commons Math 3 (optional: advanced fitting)

## File Layout

```
/tmp/tracker_rebuild/
├── pom.xml                          # Maven config
├── README.md                        # This file
├── test_video.mp4                   # Test pattern video (3s, 30fps)
├── src/
│   ├── main/java/org/opensourcephysics/cabrillo/tracker/
│   │   ├── TrackerApp.java          # Console demo
│   │   ├── data/model/Point.java    # Immutable dual-coord point
│   │   ├── data/model/Track.java    # Immutable track with Map<frame, Point>
│   │   ├── data/model/TrackType.java # Physics track types
│   │   ├── calibration/Calibration.java # Affine transform with records
│   │   ├── analysis/Differentiator.java # Finite difference (2/5-point, SavGol)
│   │   ├── analysis/CurveFitter.java    # Linear & polynomial least-squares
│   │   ├── analysis/TrackAnalyzer.java  # Full kinematic analysis pipeline
│   │   ├── video/FFmpegVideoSource.java # JavaCV video decoder
│   │   ├── video/VideoMetadata.java     # Immutable video info record
│   │   ├── video/FrameCache.java        # LRU BufferedImage cache
│   │   ├── persist/GsonProjectSerializer.java # JSON save/load
│   │   ├── export/DataExporter.java     # CSV export
│   │   └── ui/TrackerFxApp.java         # Full JavaFX UI
│   └── test/java/...              # 51 unit tests
└── target/tracker-rebuild-1.0.0-SNAPSHOT.jar
```
