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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import org.opensourcephysics.cabrillo.tracker.calibration.Calibration;
import org.opensourcephysics.cabrillo.tracker.data.model.Point;
import org.opensourcephysics.cabrillo.tracker.data.model.Track;
import org.opensourcephysics.cabrillo.tracker.data.model.TrackType;
import org.opensourcephysics.cabrillo.tracker.persist.GsonProjectSerializer;
import org.opensourcephysics.cabrillo.tracker.project.TrackerProject;
import org.opensourcephysics.cabrillo.tracker.tracking.TrackingController;
import org.opensourcephysics.cabrillo.tracker.video.FFmpegVideoSource;
import org.opensourcephysics.cabrillo.tracker.video.VideoMetadata;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    private ToggleButton stickButton;
    private ListView<Track> trackListView;
    private ObservableList<Track> trackList;

    // Calibration stick state
    private final List<double[]> stickPoints = new ArrayList<>();

    // Stage reference for dialogs
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        project = new TrackerProject("Untitled");
        currentTrack = Track.create("Track 1", TrackType.POINT_MASS);
        project.addTrack(currentTrack);

        controller = project.getTrackingController();
        controller.setActiveTrack(currentTrack);  // (1) wire controller
        controller.addListener(this::onTrackChanged);  // (1) register listener

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
            // (8) Undo / Redo keybindings
            if (e.isControlDown() && e.getCode() == KeyCode.Z) controller.undo();
            if (e.isControlDown() && e.getCode() == KeyCode.Y) controller.redo();
        });

        primaryStage.setOnCloseRequest(e -> {
            if (videoSource != null) videoSource.close();
            Platform.exit();
        });
    }

    private void onTrackChanged(Track updated) {
        for (int i = 0; i < trackList.size(); i++) {
            if (trackList.get(i).id().equals(updated.id())) { trackList.set(i, updated); break; }
        }
        if (currentTrack != null && currentTrack.id().equals(updated.id())) {
            currentTrack = updated;
            pointList.setAll(currentTrack.getPoints());
            refreshTrajectoryPlot();
            displayFrame(currentFrame);
        }
    }

    private MenuBar buildMenuBar(Stage stage) {
        MenuBar bar = new MenuBar();

        // (10) File menu with Save/Open Project
        Menu fileMenu = new Menu("File");
        MenuItem openVideo = new MenuItem("Open Video...");
        openVideo.setOnAction(e -> openVideoDialog(stage));

        MenuItem saveProject = new MenuItem("Save Project...");
        saveProject.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+S"));
        saveProject.setOnAction(e -> saveProjectDialog(stage));

        MenuItem openProject = new MenuItem("Open Project...");
        openProject.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+O"));
        openProject.setOnAction(e -> openProjectDialog(stage));

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(openVideo, new SeparatorMenuItem(), saveProject, openProject,
                new SeparatorMenuItem(), exit);

        Menu trackMenu = new Menu("Track");
        MenuItem newTrack = new MenuItem("New Track");
        newTrack.setOnAction(e -> createNewTrack());
        trackMenu.getItems().add(newTrack);

        // (8) Edit menu with Undo/Redo
        Menu editMenu = new Menu("Edit");
        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+Z"));
        undoItem.setOnAction(e -> controller.undo());

        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+Y"));
        redoItem.setOnAction(e -> controller.redo());

        editMenu.getItems().addAll(undoItem, redoItem);

        bar.getMenus().addAll(fileMenu, editMenu, trackMenu);
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

        // Mouse handler for marking points or collecting stick endpoints
        videoCanvas.setOnMouseClicked(e -> {
            if (videoSource == null || !videoSource.isOpen()) return;

            // (5) Stick mode: collect two clicks then calibrate
            if (stickButton != null && stickButton.isSelected()) {
                double[] pixelCoords = canvasToVideoPixel(e.getX(), e.getY());
                if (pixelCoords == null) return;
                stickPoints.add(pixelCoords);
                if (stickPoints.size() == 2) {
                    double x1 = stickPoints.get(0)[0], y1 = stickPoints.get(0)[1];
                    double x2 = stickPoints.get(1)[0], y2 = stickPoints.get(1)[1];
                    stickPoints.clear();
                    TextInputDialog dlg = new TextInputDialog("1.0");
                    dlg.setTitle("Calibration Stick");
                    dlg.setHeaderText("Enter the real length of the stick in meters:");
                    dlg.setContentText("Length (m):");
                    dlg.showAndWait().ifPresent(input -> {
                        try {
                            double realLength = Double.parseDouble(input.trim());
                            Calibration newCal = CalibrationStickTool.calibrate(
                                    project.getCalibration(), x1, y1, x2, y2, realLength);
                            project.setCalibration(newCal);
                            displayFrame(currentFrame);
                        } catch (NumberFormatException ex) {
                            showError("Invalid input", "Please enter a valid number.");
                        }
                    });
                    stickButton.setSelected(false);
                }
                return; // suppress mark mode while stick is active
            }

            // Normal mark mode
            if (markButton != null && markButton.isSelected()) {
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
        // (5) When mark is toggled on, deselect stick
        markButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && stickButton != null && stickButton.isSelected()) {
                stickButton.setSelected(false);
                stickPoints.clear();
            }
        });

        // (5) Calibration stick toggle button
        stickButton = new ToggleButton("📏 Stick");
        stickButton.setDisable(true);
        stickButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                stickPoints.clear();
                // deselect mark mode
                if (markButton != null) markButton.setSelected(false);
            } else {
                stickPoints.clear();
            }
        });

        // (9) Auto-track button
        Button autoTrackButton = new Button("🤖 Auto-track");
        autoTrackButton.setDisable(true);
        autoTrackButton.setOnAction(e -> runAutoTrack());
        // Enable when video is loaded (we'll enable in loadVideo)
        // Store reference for enabling later
        videoInfoLabel = new Label("No video loaded");
        videoInfoLabel.setStyle("-fx-text-fill: #888;");

        // Keep auto-track button ref accessible in loadVideo via a tag
        autoTrackButton.setId("autoTrackButton");

        HBox controls = new HBox(10, prevButton, playButton, nextButton, frameLabel,
                markButton, stickButton, autoTrackButton);
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
                controller.setActiveTrack(newVal);  // (3) wire controller on selection
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
        pointTable.setEditable(true);  // (6) editable table

        TableColumn<Point, Number> frameCol = new TableColumn<>("Frame");
        frameCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getFrame()));
        frameCol.setPrefWidth(55);

        // (6) Editable X column
        TableColumn<Point, Double> xCol = new TableColumn<>("X (px)");
        xCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>((Double) c.getValue().pixelX()));
        xCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        xCol.setOnEditCommit(ev -> { Point p = ev.getRowValue();
            controller.setCurrentFrame(p.getFrame()); controller.movePoint(p.getFrame(), ev.getNewValue(), p.pixelY()); });
        xCol.setPrefWidth(75); xCol.setEditable(true);

        TableColumn<Point, Double> yCol = new TableColumn<>("Y (px)");
        yCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>((Double) c.getValue().pixelY()));
        yCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        yCol.setOnEditCommit(ev -> { Point p = ev.getRowValue();
            controller.setCurrentFrame(p.getFrame()); controller.movePoint(p.getFrame(), p.pixelX(), ev.getNewValue()); });
        yCol.setPrefWidth(75); yCol.setEditable(true);

        TableColumn<Point, String> wxCol = new TableColumn<>("world X (m)");
        wxCol.setCellValueFactory(c -> { Calibration.WorldPoint wp = project.getCalibration().toWorld(c.getValue().pixelX(), c.getValue().pixelY());
            return new ReadOnlyObjectWrapper<>(String.format("%.4f", wp.x())); });
        wxCol.setPrefWidth(85); wxCol.setEditable(false);

        TableColumn<Point, String> wyCol = new TableColumn<>("world Y (m)");
        wyCol.setCellValueFactory(c -> { Calibration.WorldPoint wp = project.getCalibration().toWorld(c.getValue().pixelX(), c.getValue().pixelY());
            return new ReadOnlyObjectWrapper<>(String.format("%.4f", wp.y())); });
        wyCol.setPrefWidth(85); wyCol.setEditable(false);

        pointTable.getColumns().addAll(frameCol, xCol, yCol, wxCol, wyCol);

        pointTable.setRowFactory(tv -> {
            TableRow<Point> row = new TableRow<>();
            MenuItem deleteItem = new MenuItem("Delete point");
            deleteItem.setOnAction(e -> { Point p = row.getItem();
                if (p != null) { controller.setCurrentFrame(p.getFrame()); controller.deletePoint(p.getFrame()); } });
            row.setContextMenu(new ContextMenu(deleteItem));
            return row;
        });

        NumberAxis xAxis = new NumberAxis(); xAxis.setLabel("X (pixels)");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Y (pixels)");
        LineChart<Number, Number> plot = new LineChart<>(xAxis, yAxis);
        plot.setTitle("Trajectory"); plot.setPrefHeight(300); plot.setLegendVisible(false); plot.setAnimated(false);
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
            new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) loadVideo(file.toPath());
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
            stickButton.setDisable(false);
            if (videoCanvas.getScene() != null) {
                Button btn = (Button) videoCanvas.getScene().lookup("#autoTrackButton");
                if (btn != null) btn.setDisable(false);
            }

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
        drawAxes();  // (4) coordinate axes overlay

        int total = videoSource.getFrameCount();
        frameLabel.setText(String.format("Frame: %d / %d", frameIndex, Math.max(0, total - 1)));
        frameSlider.setValue(frameIndex);
        currentFrame = frameIndex;
    }

    private void drawVideoFrame(Image image, int imgWidth, int imgHeight) {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        gc.setFill(Color.BLACK); gc.fillRect(0, 0, canvasWidth, canvasHeight);
        videoScale = Math.min(canvasWidth / imgWidth, canvasHeight / imgHeight);
        double drawW = imgWidth * videoScale, drawH = imgHeight * videoScale;
        videoOffsetX = (canvasWidth - drawW) / 2; videoOffsetY = (canvasHeight - drawH) / 2;
        gc.drawImage(image, videoOffsetX, videoOffsetY, drawW, drawH);
    }

    private void drawTrackedPoints(int frameIndex) {
        if (currentTrack == null) return;
        gc.setFill(Color.RED); gc.setStroke(Color.YELLOW); gc.setLineWidth(2);
        for (Track track : project.getTracks()) {
            track.point(frameIndex).ifPresent(p -> {
                double[] c = videoPixelToCanvas(p.pixelX(), p.pixelY());
                if (c != null) { gc.fillOval(c[0]-5,c[1]-5,10,10); gc.strokeOval(c[0]-8,c[1]-8,16,16); }
            });
        }
    }

    private void drawAxes() {
        Calibration cal = project.getCalibration();
        double[] origin = videoPixelToCanvas(cal.originX(), cal.originY());
        if (origin == null) return;
        double ox = origin[0], oy = origin[1], angle = cal.angle();
        gc.setFill(Color.MAGENTA); gc.fillOval(ox-6, oy-6, 12, 12);
        drawArrow(ox, oy, Math.cos(angle), -Math.sin(angle), 60, 10, 5, Color.RED, "X");
        drawArrow(ox, oy, -Math.sin(angle), -Math.cos(angle), 60, 10, 5, Color.GREEN, "Y");
    }

    private void drawArrow(double ox, double oy, double dx, double dy,
                           double arrowLen, double headLen, double headWidth,
                           Color color, String label) {
        double tipX = ox+dx*arrowLen, tipY = oy+dy*arrowLen;
        double baseX = ox+dx*(arrowLen-headLen), baseY = oy+dy*(arrowLen-headLen);
        double px = -dy, py = dx;
        gc.setStroke(color); gc.setFill(color); gc.setLineWidth(2.0);
        gc.strokeLine(ox, oy, baseX, baseY);
        gc.fillPolygon(new double[]{tipX, baseX+px*headWidth, baseX-px*headWidth},
                       new double[]{tipY, baseY+py*headWidth, baseY-py*headWidth}, 3);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(javafx.scene.text.Font.font(12));
        gc.fillText(label, tipX+dx*10, tipY+dy*10);
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

    // (2) markPoint delegates to controller; listener handles the rest
    private void markPoint(double pixelX, double pixelY) {
        controller.setCurrentFrame(currentFrame);
        controller.addPoint(pixelX, pixelY);
        // The TrackingListener (onTrackChanged) refreshes pointList, plot, and canvas
    }

    private void refreshTrajectoryPlot() {
        trajectorySeries.getData().clear();
        if (currentTrack == null) return;
        for (Point p : currentTrack.getPoints()) {
            if (p.hasPixel()) {
                trajectorySeries.getData().add(new XYChart.Data<>(p.pixelX(), p.pixelY()));
            }
        }
    }

    private void stepFrame(int delta) {
        if (videoSource == null || !videoSource.isOpen()) return;
        int max = Math.max(0, videoSource.getFrameCount()-1);
        displayFrame(Math.max(0, Math.min(currentFrame+delta, max)));
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
        isPlaying = false; playButton.setText("▶ Play"); markButton.setDisable(false);
        if (playTimer != null) { playTimer.stop(); playTimer = null; }
    }

    private void createNewTrack() {
        Track t = Track.create("Track " + (project.getTracks().size()+1), TrackType.POINT_MASS);
        project.addTrack(t); trackList.setAll(project.getTracks());
        currentTrack = t; controller.setActiveTrack(t);
        trackListView.getSelectionModel().select(t);
        pointList.clear(); trajectorySeries.getData().clear();
    }

    // (9) Auto-track: template matcher over up to 30 subsequent frames
    private void runAutoTrack() {
        if (videoSource == null || !videoSource.isOpen()) return;
        if (currentTrack == null || currentTrack.point(currentFrame).isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Auto-track");
            alert.setHeaderText(null);
            alert.setContentText("Please mark a point on the current frame before running auto-track.");
            alert.showAndWait();
            return;
        }

        int frameCount = videoSource.getFrameCount();
        int endFrame = Math.min(currentFrame + 30, frameCount - 1);

        for (int f = currentFrame + 1; f <= endFrame; f++) {
            BufferedImage prev = videoSource.getFrame(f - 1);
            BufferedImage next = videoSource.getFrame(f);
            Point pivot = currentTrack.point(f - 1).orElse(null);
            if (pivot == null) break;

            org.opensourcephysics.cabrillo.tracker.tracking.TemplateMatcher.Match m =
                    org.opensourcephysics.cabrillo.tracker.tracking.TemplateMatcher.findNext(
                            prev, next, pivot.pixelX(), pivot.pixelY());
            if (m == null) break;

            controller.setCurrentFrame(f);
            controller.addPoint(m.pixelX(), m.pixelY());
            // Listener updates currentTrack; re-fetch from project list
            for (Track t : project.getTracks()) {
                if (t.id().equals(currentTrack.id())) {
                    currentTrack = t;
                    break;
                }
            }
        }
        displayFrame(currentFrame);
    }

    // (10) Save project to JSON
    private void saveProjectDialog(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Project");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Tracker JSON", "*.json"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try (FileWriter fw = new FileWriter(file)) {
            new GsonProjectSerializer().serialize(project, fw);
        } catch (Exception ex) {
            showError("Save failed", ex.getMessage());
        }
    }

    // (10) Load project from JSON
    private void openProjectDialog(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Project");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Tracker JSON", "*.json"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try (FileReader fr = new FileReader(file)) {
            TrackerProject loaded = new GsonProjectSerializer().deserialize(fr);
            project = loaded;
            controller = project.getTrackingController();
            controller.addListener(this::onTrackChanged);

            trackList.setAll(project.getTracks());
            if (!project.getTracks().isEmpty()) {
                currentTrack = project.getTracks().get(0);
            } else {
                currentTrack = Track.create("Track 1", TrackType.POINT_MASS);
                project.addTrack(currentTrack);
                trackList.setAll(project.getTracks());
            }
            controller.setActiveTrack(currentTrack);
            trackListView.getSelectionModel().select(currentTrack);
            pointList.setAll(currentTrack.getPoints());
            refreshTrajectoryPlot();
            if (videoSource != null && videoSource.isOpen()) displayFrame(currentFrame);
        } catch (Exception ex) {
            showError("Open project failed", ex.getMessage());
        }
    }

    private void clearCanvas() {
        gc.setFill(Color.BLACK); gc.fillRect(0, 0, canvasWidth, canvasHeight);
        gc.setFill(Color.DARKGRAY); gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("No video loaded\nUse File > Open Video", canvasWidth/2, canvasHeight/2);
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title);
        a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
