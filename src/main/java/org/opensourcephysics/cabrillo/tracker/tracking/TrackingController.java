package org.opensourcephysics.cabrillo.tracker.tracking;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.project.TrackerProject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Mutates tracks within a {@link TrackerProject} via reversible edits.
 * Maintains undo/redo stacks of {@link TrackEdit}s and notifies listeners
 * when a track changes.
 */
public class TrackingController {

    private TrackerProject project;
    private int currentFrame = 0;
    private String activeTrackId;

    private final Deque<TrackEdit> undoStack = new ArrayDeque<>();
    private final Deque<TrackEdit> redoStack = new ArrayDeque<>();
    private final List<TrackingListener> listeners = new ArrayList<>();

    public TrackingController(TrackerProject project) {
        this.project = project;
    }

    public TrackerProject getProject() { return project; }

    public void setProject(TrackerProject project) {
        this.project = project;
        undoStack.clear();
        redoStack.clear();
    }

    public int getCurrentFrame() { return currentFrame; }
    public void setCurrentFrame(int frame) { this.currentFrame = Math.max(0, frame); }

    /** Backward-compat alias used by existing UI code. */
    public int getCurrentIndex() { return currentFrame; }
    public void setCurrentIndex(int i) { setCurrentFrame(i); }

    public void next() { currentFrame++; }
    public void previous() { if (currentFrame > 0) currentFrame--; }

    public void addListener(TrackingListener l) { listeners.add(l); }
    public void removeListener(TrackingListener l) { listeners.remove(l); }

    /** Set the track that {@link #addPoint(double, double)} will mutate. */
    public void setActiveTrack(Track t) {
        this.activeTrackId = t == null ? null : t.id();
    }

    public Track getActiveTrack() {
        if (activeTrackId == null) {
            return project.getTracks().isEmpty() ? null : project.getTracks().get(0);
        }
        for (Track t : project.getTracks()) {
            if (t.id().equals(activeTrackId)) return t;
        }
        return null;
    }

    /** Backward-compat alias. */
    public Track getCurrentTrack() { return getActiveTrack(); }

    public Point getCurrentPoint() {
        Track t = getActiveTrack();
        return t == null ? null : t.point(currentFrame).orElse(null);
    }

    /** Mark a point at the current frame on the active track. */
    public Point addPoint(double pixelX, double pixelY) {
        Track t = getActiveTrack();
        if (t == null) return null;
        Point before = t.point(currentFrame).orElse(null);
        Point after = Point.atPixel(currentFrame, pixelX, pixelY);
        applyEdit(new TrackEdit(t.id(), currentFrame, before, after));
        return after;
    }

    /** Move (or create) the point at the given frame on the active track. */
    public Point movePoint(int frame, double pixelX, double pixelY) {
        Track t = getActiveTrack();
        if (t == null) return null;
        Point before = t.point(frame).orElse(null);
        Point after = Point.atPixel(frame, pixelX, pixelY);
        applyEdit(new TrackEdit(t.id(), frame, before, after));
        return after;
    }

    /** Delete the point at the given frame on the active track. */
    public boolean deletePoint(int frame) {
        Track t = getActiveTrack();
        if (t == null) return false;
        Point before = t.point(frame).orElse(null);
        if (before == null) return false;
        applyEdit(new TrackEdit(t.id(), frame, before, null));
        return true;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public boolean undo() {
        TrackEdit e = undoStack.pollLast();
        if (e == null) return false;
        applyEditNoStack(e.inverse());
        redoStack.addLast(e);
        return true;
    }

    public boolean redo() {
        TrackEdit e = redoStack.pollLast();
        if (e == null) return false;
        applyEditNoStack(e);
        undoStack.addLast(e);
        return true;
    }

    private void applyEdit(TrackEdit edit) {
        applyEditNoStack(edit);
        undoStack.addLast(edit);
        redoStack.clear();
    }

    private void applyEditNoStack(TrackEdit edit) {
        List<Track> tracks = project.getTracks();
        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            if (!t.id().equals(edit.trackId())) continue;
            Track updated = edit.after() == null
                ? t.withoutPoint(edit.frame())
                : t.withPoint(edit.frame(), edit.after());
            tracks.set(i, updated);
            for (TrackingListener l : listeners) l.onTrackChanged(updated);
            return;
        }
    }
}
