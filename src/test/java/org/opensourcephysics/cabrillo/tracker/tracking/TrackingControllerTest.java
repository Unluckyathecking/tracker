package org.opensourcephysics.cabrillo.tracker.tracking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;
import org.opensourcephysics.cabrillo.tracker.project.TrackerProject;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingControllerTest {

    private TrackerProject project;
    private TrackingController controller;
    private Track track;

    @BeforeEach
    void setUp() {
        project = new TrackerProject();
        controller = new TrackingController(project);
        track = Track.create("T", TrackType.POINT_MASS);
        project.addTrack(track);
        controller.setActiveTrack(track);
    }

    @Test
    void addPoint_thenUndo_pointGoneAndRedoAvailable() {
        controller.setCurrentFrame(1);
        controller.addPoint(10.0, 20.0);

        Track afterAdd = project.getTracks().get(0);
        assertThat(afterAdd.hasFrame(1)).isTrue();
        assertThat(controller.canUndo()).isTrue();
        assertThat(controller.canRedo()).isFalse();

        controller.undo();

        Track afterUndo = project.getTracks().get(0);
        assertThat(afterUndo.hasFrame(1)).isFalse();
        assertThat(controller.canUndo()).isFalse();
        assertThat(controller.canRedo()).isTrue();
    }

    @Test
    void addPoint_undoThenRedo_pointReappears() {
        controller.setCurrentFrame(3);
        controller.addPoint(5.0, 7.0);
        controller.undo();

        assertThat(project.getTracks().get(0).hasFrame(3)).isFalse();

        controller.redo();

        Track afterRedo = project.getTracks().get(0);
        assertThat(afterRedo.hasFrame(3)).isTrue();
        assertThat(afterRedo.point(3).get().pixelX()).isEqualTo(5.0);
        assertThat(afterRedo.point(3).get().pixelY()).isEqualTo(7.0);
    }

    @Test
    void movePoint_twice_undoRestoresPriorPosition() {
        controller.movePoint(2, 100.0, 200.0);
        controller.movePoint(2, 300.0, 400.0);

        Track afterSecondMove = project.getTracks().get(0);
        assertThat(afterSecondMove.point(2).get().pixelX()).isEqualTo(300.0);

        controller.undo();

        Track afterUndo = project.getTracks().get(0);
        // Point should still exist at the prior position, not be absent
        assertThat(afterUndo.hasFrame(2)).isTrue();
        assertThat(afterUndo.point(2).get().pixelX()).isEqualTo(100.0);
        assertThat(afterUndo.point(2).get().pixelY()).isEqualTo(200.0);
    }

    @Test
    void deletePoint_thenUndo_pointRestoredWithOriginalCoordinates() {
        controller.setCurrentFrame(5);
        controller.addPoint(42.0, 84.0);
        // Re-fetch since track is now stale
        controller.setCurrentFrame(5);

        controller.deletePoint(5);
        assertThat(project.getTracks().get(0).hasFrame(5)).isFalse();

        controller.undo();

        Track afterUndo = project.getTracks().get(0);
        assertThat(afterUndo.hasFrame(5)).isTrue();
        assertThat(afterUndo.point(5).get().pixelX()).isEqualTo(42.0);
        assertThat(afterUndo.point(5).get().pixelY()).isEqualTo(84.0);
    }

    @Test
    void newEditAfterUndo_clearsRedoStack() {
        controller.setCurrentFrame(10);
        controller.addPoint(1.0, 2.0);

        controller.undo();
        assertThat(controller.canRedo()).isTrue();

        // Add a different point — this should clear redo
        controller.setCurrentFrame(20);
        controller.addPoint(9.0, 8.0);

        assertThat(controller.canRedo()).isFalse();
        assertThat(controller.redo()).isFalse();
    }

    @Test
    void listener_firesOnEveryMutation() {
        AtomicInteger callCount = new AtomicInteger(0);
        TrackingListener counter = t -> callCount.incrementAndGet();
        controller.addListener(counter);

        controller.setCurrentFrame(1);
        controller.addPoint(1.0, 2.0);   // +1 (add)
        controller.undo();               // +1 (undo)
        controller.redo();               // +1 (redo)

        controller.setCurrentFrame(1);
        controller.deletePoint(1);       // +1 (delete)
        controller.undo();               // +1 (undo delete)

        assertThat(callCount.get()).isEqualTo(5);
    }
}
