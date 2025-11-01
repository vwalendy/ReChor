package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.Journey;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Vue synthétique des trajets disponibles.
 * Le record expose le composant principal et la propriété du trajet sélectionné.
 */
public record SummaryUI(Node rootNode, ObservableValue<Journey> selectedJourneyO) {
    private static final String STYLE_SHEET = "summary.css";
    private static final int CELL_PADDING = 6;
    private static final int GRAPH_MARGIN = 10;
    private static final int CIRCLE_RADIUS = 3;

    private static class CellRenderer extends ListCell<Journey> {
        private final BorderPane layout;
        private final HBox headerBox;
        private final ImageView iconView;
        private final Text headerLabel;
        private final Pane graphArea;
        private final Text startLabel;
        private final Text endLabel;
        private final HBox durationBox;
        private final Text durationLabel;

        CellRenderer() {
            iconView = new ImageView();
            iconView.setFitWidth(20);
            iconView.setFitHeight(20);

            headerLabel = new Text();
            headerBox = new HBox(iconView, headerLabel);
            headerBox.getStyleClass().add("route");

            startLabel = new Text();
            startLabel.getStyleClass().add("departure");
            endLabel = new Text();

            durationLabel = new Text();
            durationBox = new HBox(durationLabel);
            durationBox.getStyleClass().add("duration");

            graphArea = new Pane() {
                @Override
                protected void layoutChildren() {
                    super.layoutChildren();
                    double midY = getHeight() * 0.5;
                    double leftX = GRAPH_MARGIN;
                    double rightX = getWidth() - GRAPH_MARGIN;

                    for (Node child : getChildren()) {
                        if (child instanceof Circle circle) {
                            Double frac = (Double) circle.getUserData();
                            if (frac == null) continue;
                            double x = leftX + frac * (rightX - leftX);
                            circle.setCenterX(x);
                            circle.setCenterY(midY);
                        } else if (child instanceof Line line) {
                            line.setStartX(leftX);
                            line.setStartY(midY);
                            line.setEndX(rightX);
                            line.setEndY(midY);
                        }
                    }
                }
            };
            graphArea.setPrefSize(0, 0);

            layout = new BorderPane();
            layout.setTop(headerBox);
            layout.setLeft(startLabel);
            layout.setRight(endLabel);
            layout.setBottom(durationBox);
            layout.setCenter(graphArea);
            layout.setPadding(new Insets(CELL_PADDING));
            layout.getStyleClass().add("journey");
        }

        @Override
        protected void updateItem(Journey journey, boolean empty) {
            super.updateItem(journey, empty);
            if (empty || journey == null) {
                setText(null);
                setGraphic(null);
            } else {
                graphArea.getChildren().clear();
                Line track = new Line();
                graphArea.getChildren().add(track);

                Circle startCircle = new Circle(CIRCLE_RADIUS);
                Circle endCircle = new Circle(CIRCLE_RADIUS);
                startCircle.getStyleClass().add("dep-arr");
                endCircle.getStyleClass().add("dep-arr");
                startCircle.setUserData(0d);
                endCircle.setUserData(1d);
                graphArea.getChildren().addAll(startCircle, endCircle);

                Journey.Leg.Transport firstLeg = null;
                for (Journey.Leg leg : journey.legs()) {
                    if (leg instanceof Journey.Leg.Transport t && firstLeg == null) {
                        firstLeg = t;
                    } else if (leg instanceof Journey.Leg.Foot f) {
                        double pos = (double) Duration.between(journey.depTime(), f.depTime()).toNanos()
                                / journey.duration().toNanos();
                        Circle transferMark = new Circle(CIRCLE_RADIUS);
                        transferMark.getStyleClass().add("transfer");
                        transferMark.setUserData(pos);
                        graphArea.getChildren().add(transferMark);
                    }
                }

                if (firstLeg != null) {
                    headerLabel.setText(FormatterFr.formatRouteDestination(firstLeg));
                    iconView.setImage(VehicleIcons.iconFor(firstLeg.vehicle()));
                }

                startLabel.setText(FormatterFr.formatTime(journey.depTime()));
                endLabel.setText(FormatterFr.formatTime(journey.arrTime()));
                durationLabel.setText(FormatterFr.formatDuration(journey.duration()));

                setGraphic(layout);
            }
        }
    }

    public static SummaryUI create(ObservableValue<List<Journey>> journeyStream,
                                   ObservableValue<LocalTime> timeStream) {
        ListView<Journey> listView = initListView(journeyStream, timeStream);
        listView.setCellFactory(view -> new CellRenderer());
        ObjectProperty<Journey> selectedProp = new SimpleObjectProperty<>();
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedProp.set(newV);
        });
        return new SummaryUI(listView, selectedProp);
    }

    private static void selectByTime(ListView<Journey> listView,
                                     List<Journey> journeys,
                                     LocalTime target) {
        int index = -1;
        for (int i = 0; i < journeys.size(); i++) {
            if (journeys.get(i).depTime().toLocalTime().isAfter(target)) {
                index = i;
                break;
            }
        }
        if (index < 0 && !journeys.isEmpty()) {
            index = journeys.size() - 1;
        }
        if (index >= 0) {
            Journey pick = journeys.get(index);
            listView.getSelectionModel().select(pick);
            Platform.runLater(() -> listView.scrollTo(pick));
        }
    }

    private static ListView<Journey> initListView(ObservableValue<List<Journey>> journeyStream,
                                                  ObservableValue<LocalTime> timeStream) {
        ObservableList<Journey> items = FXCollections.observableArrayList();
        ListView<Journey> listView = new ListView<>(items);
        if (journeyStream.getValue() != null) {
            items.addAll(journeyStream.getValue());
        }
        selectByTime(listView, journeyStream.getValue(), timeStream.getValue());
        journeyStream.addListener((obs, oldV, newV) -> {
            items.clear();
            items.addAll(newV);
            selectByTime(listView, newV, timeStream.getValue());
        });
        timeStream.addListener((obs, oldV, newV) -> {
            selectByTime(listView, journeyStream.getValue(), newV);
        });
        listView.getStylesheets().add(STYLE_SHEET);
        listView.setId("detail");
        return listView;
    }
}
