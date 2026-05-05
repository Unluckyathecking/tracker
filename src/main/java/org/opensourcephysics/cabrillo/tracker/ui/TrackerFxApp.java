package org.opensourcephysics.cabrillo.tracker.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;
import org.opensourcephysics.cabrillo.tracker.project.TrackerProject;
import org.opensourcephysics.cabrillo.tracker.tracking.TrackingController;
import org.opensourcephysics.cabrillo.tracker.video.FFmpegVideoSource;
import org.opensourcephysics.cabrillo.tracker.video.VideoMetadata;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/**
 * Full JavaFX UI for Tracker video analysis.
 * Supports video loading, frame scrubbing, click-to-mark tracking,
 * and real-time trajectory plotting.
 */
public class TrackerFxApp extends Application {

    // Video
    private FFmpegVideoSource videoSource;
    private VideoMetadata videoMetadata;
    private int currentFrame = 0;
    private boolean isPlaying = false;
    private javafx.animation.AnimationTimer playTimer;

    // Canvas & scaling
    private Canvas videoCanvas;
    private GraphicsContext gc;
    private double canvasWidth = 800;
    private double canvasHeight = 600;
    private double videoScale = 1.0;
    private double videoOffsetX = 0;
    private double videoOffsetY = 0;

    // Project & tracking
    private TrackerProject project;
    private Track currentTrack;
    private TrackingController controller;
    private ObservableList<Point> pointList;
    private XYChart.Series<Number, Number> trajectorySeries;

    // UI controls
    private Label frameLabel;
    private Slider frameSlider;
    private Button playButton;
    private Button prevButton;
    private Button nextButton;
    private Label videoInfoLabel;
    private TableView<Point> pointTable;
    private ToggleButton markButton;
    private ListView<Track> trackListView;
    private ObservableList<Track> trackList;

    @Override
    public void start(Stage primaryStage) {
        project = new TrackerProject("Untitled");
        currentTrack = Track.create("Track 1", TrackType.POINT_MASS);
        project.addTrack(currentTrack);
        controller = project.getTrackingController();
        pointList = FXCollections.observableArrayList();
        trackList = FXCollections.observableArrayList(project.getTracks());

        BorderPane root = new BorderPane();

        // Menu
        MenuBar menuBar = buildMenuBar(primaryStage);
        root.setTop(menuBar);

        // Center: video canvas + controls
        VBox centerPanel = buildVideoPanel();
        root.setCenter(centerPanel);

        // Right: data table + plot
        VBox rightPanel = buildDataPanel();
        root.setRight(rightPanel);
        rightPanel.setPrefWidth(400);

        Scene scene = new Scene(root, 1250, 850);
        scene.getStylesheets().add(getClass().getResource("/tracker-dark.css") != null
            ? getClass().getResource("/tracker-dark.css").toExternalForm()
            : "");
        primaryStage.setTitle("Tracker Rebuild");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Keyboard shortcuts
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case LEFT -> stepFrame(-1);
                case RIGHT -> stepFrame(1);
                case SPACE -> togglePlay();
                case M -> {
                    if (markButton != null) markButton.setSelected(!markButton.isSelected());
                }
                case N -> {
                    if (e.isControlDown()) createNewTrack();
                }
                default -> {}
            }
        });

        primaryStage.setOnCloseRequest(e -> {
            if (videoSource != null) videoSource.close();
            Platform.exit();
        });
    }

    private MenuBar buildMenuBar(Stage stage) {
        MenuBar bar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem openVideo = new MenuItem("Open Video...");
        openVideo.setOnAction(e -> openVideoDialog(stage));

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(openVideo, new SeparatorMenuItem(), exit);

        Menu trackMenu = new Menu("Track");
        MenuItem newTrack = new MenuItem("New Track");
        newTrack.setOnAction(e -> createNewTrack());
        trackMenu.getItems().add(newTrack);

        bar.getMenus().addAll(fileMenu, trackMenu);
        return bar;
    }

    private VBox buildVideoPanel() {
        // Canvas with black background
        videoCanvas = new Canvas(canvasWidth, canvasHeight);
        gc = videoCanvas.getGraphicsContext2D();
        clearCanvas();

        StackPane canvasWrapper = new StackPane(videoCanvas);
        canvasWrapper.setStyle("-fx-background-color: black;");
        canvasWrapper.setPrefSize(canvasWidth, canvasHeight);
        canvasWrapper.setMaxSize(canvasWidth, canvasHeight);

        // Mouse handler for marking points
        videoCanvas.setOnMouseClicked(e -> {
            if (markButton != null && markButton.isSelected() && videoSource != null && videoSource.isOpen()) {
                double[] pixelCoords = canvasToVideoPixel(e.getX(), e.getY());
                if (pixelCoords != null) {
                    markPoint(pixelCoords[0], pixelCoords[1]);
                }
            }
        });

        // Frame controls
        frameSlider = new Slider(0, 1, 0);
        frameSlider.setShowTickMarks(false);
        frameSlider.setShowTickLabels(false);
        frameSlider.setDisable(true);
        frameSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (videoSource != null && videoSource.isOpen() && !isPlaying) {
                currentFrame = newVal.intValue();
                displayFrame(currentFrame);
            }
        });

        prevButton = new Button("⏮ Prev");
        prevButton.setDisable(true);
        prevButton.setOnAction(e -> stepFrame(-1));

        playButton = new Button("▶ Play");
        playButton.setDisable(true);
        playButton.setOnAction(e -> togglePlay());

        nextButton = new Button("Next ⏭");
        nextButton.setDisable(true);
        nextButton.setOnAction(e -> stepFrame(1));

        frameLabel = new Label("Frame: 0 / 0");
        frameLabel.setMinWidth(120);

        markButton = new ToggleButton("📍 Mark");
        markButton.setDisable(true);

        videoInfoLabel = new Label("No video loaded");
        videoInfoLabel.setStyle("-fx-text-fill: #888;");

        HBox controls = new HBox(10, prevButton, playButton, nextButton, frameLabel, markButton);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));

        VBox panel = new VBox(5, canvasWrapper, frameSlider, controls, videoInfoLabel);
        panel.setPadding(new Insets(10));
        VBox.setVgrow(canvasWrapper, Priority.ALWAYS);
        return panel;
    }

    private VBox buildDataPanel() {
        // Track list
        Label trackListTitle = new Label("Tracks");
        trackListTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        trackListView = new ListView<>(trackList);
        trackListView.setPrefHeight(120);
        trackListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name() + " (" + item.pointCount() + " pts)");
                    try {
                        setTextFill(Color.web(item.color()));
                    } catch (Exception e) {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });
        trackListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentTrack = newVal;
                pointList.setAll(currentTrack.getPoints());
                refreshTrajectoryPlot();
                displayFrame(currentFrame);
            }
        });

        // Point table
        Label tableTitle = new Label("Tracked Points");
        tableTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        pointTable = new TableView<>();
        pointTable.setItems(pointList);
        pointTable.setPrefHeight(200);

        TableColumn<Point, Number> frameCol = new TableColumn<>("Frame");
        frameCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getFrame()));
        frameCol.setPrefWidth(60);

        TableColumn<Point, Number> xCol = new TableColumn<>("X (px)");
        xCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().pixelX()));
        xCol.setPrefWidth(80);

        TableColumn<Point, Number> yCol = new TableColumn<>("Y (px)");
        yCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().pixelY()));
        yCol.setPrefWidth(80);

        pointTable.getColumns().addAll(frameCol, xCol, yCol);

        // Trajectory plot
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("X (pixels)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Y (pixels)");

        LineChart<Number, Number> plot = new LineChart<>(xAxis, yAxis);
        plot.setTitle("Trajectory");
        plot.setPrefHeight(300);
        plot.setLegendVisible(false);
        plot.setAnimated(false);

        trajectorySeries = new XYChart.Series<>();
        plot.getData().add(trajectorySeries);

        VBox panel = new VBox(10, trackListTitle, trackListView, tableTitle, pointTable, plot);
        panel.setPadding(new Insets(10));
        VBox.setVgrow(plot, Priority.ALWAYS);
        return panel;
    }

    private void openVideoDialog(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Video");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov", "*.mkv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            loadVideo(file.toPath());
        }
    }

    private void loadVideo(Path path) {
        if (videoSource != null) {
            videoSource.close();
        }
        videoSource = new FFmpegVideoSource(50);
        try {
            videoSource.open(path);
            videoMetadata = videoSource.getMetadata();
            currentFrame = 0;

            int frameCount = videoSource.getFrameCount();
            frameSlider.setMax(Math.max(1, frameCount - 1));
            frameSlider.setValue(0);
            frameSlider.setDisable(false);

            prevButton.setDisable(false);
            playButton.setDisable(false);
            nextButton.setDisable(false);
            markButton.setDisable(false);

            videoInfoLabel.setText(String.format("%s | %dx%d | %.2f fps | %d frames",
                path.getFileName(), videoSource.getWidth(), videoSource.getHeight(),
                videoSource.getFrameRate(), frameCount));

            displayFrame(0);

        } catch (Exception e) {
            showError("Failed to load video", e.getMessage());
            videoSource = null;
        }
    }

    private void displayFrame(int frameIndex) {
        if (videoSource == null || !videoSource.isOpen()) return;

        BufferedImage frame = videoSource.getFrame(frameIndex);
        if (frame == null) {
            clearCanvas();
            return;
        }

        Image fxImage = SwingFXUtils.toFXImage(frame, null);
        drawVideoFrame(fxImage, frame.getWidth(), frame.getHeight());
        drawTrackedPoints(frameIndex);

        int total = videoSource.getFrameCount();
        frameLabel.setText(String.format("Frame: %d / %d", frameIndex, Math.max(0, total - 1)));
        frameSlider.setValue(frameIndex);
        currentFrame = frameIndex;
    }

    private void drawVideoFrame(Image image, int imgWidth, int imgHeight) {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        double scaleX = canvasWidth / imgWidth;
        double scaleY = canvasHeight / imgHeight;
        videoScale = Math.min(scaleX, scaleY);

        double drawWidth = imgWidth * videoScale;
        double drawHeight = imgHeight * videoScale;
        videoOffsetX = (canvasWidth - drawWidth) / 2;
        videoOffsetY = (canvasHeight - drawHeight) / 2;

        gc.drawImage(image, videoOffsetX, videoOffsetY, drawWidth, drawHeight);
    }

    private void drawTrackedPoints(int frameIndex) {
        if (currentTrack == null) return;

        gc.setFill(Color.RED);
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(2);

        // Draw all points for this frame across all tracks
        for (Track track : project.getTracks()) {
            track.point(frameIndex).ifPresent(p -> {
                double[] canvasCoords = videoPixelToCanvas(p.pixelX(), p.pixelY());
                if (canvasCoords != null) {
                    double cx = canvasCoords[0];
                    double cy = canvasCoords[1];
                    gc.fillOval(cx - 5, cy - 5, 10, 10);
                    gc.strokeOval(cx - 8, cy - 8, 16, 16);
                }
            });
        }
    }

    private double[] canvasToVideoPixel(double canvasX, double canvasY) {
        if (videoSource == null || !videoSource.isOpen()) return null;
        double px = (canvasX - videoOffsetX) / videoScale;
        double py = (canvasY - videoOffsetY) / videoScale;
        if (px < 0 || py < 0 || px >= videoSource.getWidth() || py >= videoSource.getHeight()) {
            return null;
        }
        return new double[]{px, py};
    }

    private double[] videoPixelToCanvas(double px, double py) {
        double cx = px * videoScale + videoOffsetX;
        double cy = py * videoScale + videoOffsetY;
        return new double[]{cx, cy};
    }

    private void markPoint(double pixelX, double pixelY) {
        Point p = Point.atPixel(currentFrame, pixelX, pixelY);
        currentTrack = currentTrack.withPoint(currentFrame, p);

        // Update project track reference
        int idx = project.getTracks().indexOf(currentTrack);
        if (idx >= 0) {
            project.getTracks().set(idx, currentTrack);
        }

        pointList.setAll(currentTrack.getPoints());
        trackList.setAll(project.getTracks());
        refreshTrajectoryPlot();
        displayFrame(currentFrame); // redraw with new point
    }

    private void refreshTrajectoryPlot() {
        trajectorySeries.getData().clear();
        for (Point p : currentTrack.getPoints()) {
            if (p.hasPixel()) {
                trajectorySeries.getData().add(new XYChart.Data<>(p.pixelX(), p.pixelY()));
            }
        }
    }

    private void stepFrame(int delta) {
        if (videoSource == null || !videoSource.isOpen()) return;
        int newFrame = currentFrame + delta;
        int maxFrame = Math.max(0, videoSource.getFrameCount() - 1);
        newFrame = Math.max(0, Math.min(newFrame, maxFrame));
        displayFrame(newFrame);
    }

    private void togglePlay() {
        if (isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    private void startPlayback() {
        if (videoSource == null || !videoSource.isOpen()) return;
        isPlaying = true;
        playButton.setText("⏸ Pause");
        markButton.setDisable(true);

        double fps = videoSource.getFrameRate();
        double frameDurationMs = 1000.0 / Math.max(1.0, fps);

        playTimer = new javafx.animation.AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                double elapsedMs = (now - lastUpdate) / 1_000_000.0;
                if (elapsedMs >= frameDurationMs) {
                    int maxFrame = Math.max(0, videoSource.getFrameCount() - 1);
                    if (currentFrame >= maxFrame) {
                        currentFrame = 0;
                    } else {
                        currentFrame++;
                    }
                    displayFrame(currentFrame);
                    lastUpdate = now;
                }
            }
        };
        playTimer.start();
    }

    private void stopPlayback() {
        isPlaying = false;
        playButton.setText("▶ Play");
        markButton.setDisable(false);
        if (playTimer != null) {
            playTimer.stop();
            playTimer = null;
        }
    }

    private void createNewTrack() {
        int count = project.getTracks().size() + 1;
        currentTrack = Track.create("Track " + count, TrackType.POINT_MASS);
        project.addTrack(currentTrack);
        trackList.setAll(project.getTracks());
        trackListView.getSelectionModel().select(currentTrack);
        pointList.clear();
        trajectorySeries.getData().clear();
    }

    private void clearCanvas() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);
        gc.setFill(Color.DARKGRAY);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("No video loaded\nUse File > Open Video", canvasWidth / 2, canvasHeight / 2);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
