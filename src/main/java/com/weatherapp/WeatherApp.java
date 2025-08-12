package com.weatherapp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * JavaFX Application:
 * - loads API key from .env via Dotenv
 * - builds the UI (search, units, history, current weather, forecast)
 * - runs network calls off the JavaFX thread and updates UI safely
 * - includes robust error handling and friendly messages
 */
public class WeatherApp extends Application {

    private WeatherService service;
    private final HistoryManager historyManager = new HistoryManager();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private TextField cityField;
    private ComboBox<String> unitsCombo;
    private Button searchBtn;
    private VBox currentBox;
    private HBox forecastBox;
    private ListView<String> historyList;
    private Text statusText;

    @Override
    public void start(Stage stage) {
        // Load API key from .env (file should be in the working directory next to the JAR)
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String apiKey = dotenv.get("API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // friendly error and exit
            showFatal("Missing API key", "Please create a .env file next to the JAR containing API_KEY=<your_key>");
            return;
        }
        service = new WeatherService(apiKey);

        // Top controls: input and units
        cityField = new TextField();
        cityField.setPromptText("City (e.g., Accra or London,UK)");
        cityField.setPrefWidth(340);

        unitsCombo = new ComboBox<>();
        unitsCombo.getItems().addAll("metric (°C, m/s)", "imperial (°F, mph)");
        unitsCombo.setValue("metric (°C, m/s)");

        searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> doSearch());

        HBox topBar = new HBox(10, new Label("Location:"), cityField, new Label("Units:"), unitsCombo, searchBtn);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Left: history
        historyList = new ListView<>();
        refreshHistory();
        historyList.setOnMouseClicked(e -> {
            String sel = historyList.getSelectionModel().getSelectedItem();
            if (sel != null && sel.contains(" - ")) {
                String city = sel.split(" - ")[0];
                cityField.setText(city);
                doSearch();
            }
        });
        VBox left = new VBox(8, new Label("Recent searches"), historyList);
        left.setPadding(new Insets(10));
        left.setPrefWidth(220);

        // Center: current weather + forecast
        currentBox = new VBox(8);
        currentBox.setPadding(new Insets(12));
        applyGlass(currentBox);

        forecastBox = new HBox(10);
        forecastBox.setPadding(new Insets(8));

        VBox center = new VBox(12, currentBox, new Separator(), new Label("Short-term forecast"), forecastBox);
        center.setPadding(new Insets(10));

        // Bottom status
        statusText = new Text("Ready");
        statusText.setFill(Color.WHITE);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(left);
        root.setCenter(center);
        root.setBottom(statusText);
        chooseBackground(root);

        Scene scene = new Scene(root, 980, 600);
        scene.getStylesheets().add(getClass().getResource("/styles/glass.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Weather Information");
        stage.show();
    }

    private void applyGlass(Region r) {
        r.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 12;");
        DropShadow ds = new DropShadow();
        ds.setRadius(8);
        ds.setOffsetY(4);
        ds.setColor(Color.color(0,0,0,0.4));
        r.setEffect(ds);
    }

    // choose gradient background depending on local time
    private void chooseBackground(Region root) {
        int hour = Instant.now().atZone(ZoneId.systemDefault()).getHour();
        if (hour >= 6 && hour < 18) {
            root.setStyle("-fx-background-color: linear-gradient(#7ec8ff, #dff1ff);");
        } else {
            root.setStyle("-fx-background-color: linear-gradient(#071428, #12263a);");
        }
    }

    // Trigger search (UI call)
    private void doSearch() {
        String city = cityField.getText();
        if (city == null || city.trim().isEmpty()) {
            showAlert("Input error", "Please enter a city name.");
            return;
        }
        String units = unitsCombo.getValue().startsWith("metric") ? "metric" : "imperial";
        searchBtn.setDisable(true);
        statusText.setText("Fetching...");
        currentBox.getChildren().clear();
        forecastBox.getChildren().clear();

        // Run API call off the UI thread
        executor.submit(() -> {
            try {
                WeatherData wd = service.getWeather(city.trim(), units);
                historyManager.add(wd.cityName);
                Platform.runLater(() -> {
                    renderCurrent(wd, units);
                    renderForecast(wd, units);
                    refreshHistory();
                    statusText.setText("Updated: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.now()));
                    searchBtn.setDisable(false);
                });
            } catch (Exception ex) {
                System.err.println("Error fetching weather data: " + ex.getMessage());
                Platform.runLater(() -> {
                    showAlert("Fetch error", ex.getMessage());
                    statusText.setText("Error");
                    searchBtn.setDisable(false);
                });
            }
        });
    }

    private void renderCurrent(WeatherData wd, String units) {
        currentBox.getChildren().clear();
        HBox top = new HBox(12);
        ImageView iconView = new ImageView();
        iconView.setFitWidth(84);
        iconView.setFitHeight(84);
        if (wd.icon != null && !wd.icon.isEmpty()) {
            try {
                iconView.setImage(new Image("https://openweathermap.org/img/wn/" + wd.icon + "@2x.png", true));
            } catch (Exception ignored) {}
        }
        Label title = new Label(wd.cityName + " — " + wd.condition + " (" + wd.description + ")");
        title.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: 600;");
        top.getChildren().addAll(iconView, title);

        String tempUnit = units.equals("metric") ? "°C" : "°F";
        String windUnit = units.equals("metric") ? "m/s" : "mph";
        Label t = new Label(String.format("Temperature: %.1f %s (Feels like %.1f %s)", wd.temp, tempUnit, wd.feelsLike, tempUnit));
        Label hum = new Label("Humidity: " + wd.humidity + "%");
        Label wind = new Label(String.format("Wind: %.1f %s", wd.windSpeed, windUnit));
        t.setStyle("-fx-text-fill: white;");
        hum.setStyle("-fx-text-fill: white;");
        wind.setStyle("-fx-text-fill: white;");

        currentBox.getChildren().addAll(top, t, hum, wind);
    }

    private void renderForecast(WeatherData wd, String units) {
        forecastBox.getChildren().clear();
        String tempUnit = units.equals("metric") ? "°C" : "°F";
        for (WeatherData.ForecastEntry fe : wd.forecast) {
            VBox card = new VBox(6);
            card.setPadding(new Insets(8));
            card.setPrefWidth(120);
            card.setAlignment(Pos.CENTER);
            applyGlass(card);

            Label time = new Label(DateTimeFormatter.ofPattern("EEE HH:mm").withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(fe.timestamp)));
            ImageView iv = new ImageView();
            iv.setFitWidth(60);
            iv.setFitHeight(60);
            if (fe.icon != null && !fe.icon.trim().isEmpty()) {
                try {
                    iv.setImage(new Image("https://openweathermap.org/img/wn/" + fe.icon + "@2x.png", true));
                } catch (Exception ignored) {}
            }
            Label temp = new Label(String.format("%.1f %s", fe.temp, tempUnit));
            Label cond = new Label(fe.condition);
            time.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
            temp.setStyle("-fx-text-fill: white;");
            cond.setStyle("-fx-text-fill: white;");

            card.getChildren().addAll(time, iv, temp, cond);
            forecastBox.getChildren().add(card);
        }
    }

    private void refreshHistory() {
        historyList.getItems().clear();
        var items = historyManager.load();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        for (HistoryManager.Item it : items) {
            historyList.getItems().add(it.city + " - " + fmt.format(Instant.ofEpochSecond(it.when)));
        }
    }

    // show blocking error and exit (used if API key missing)
    private void showFatal(String title, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            a.setTitle(title);
            a.showAndWait();
            Platform.exit();
        });
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        executor.shutdownNow();
    }

    public static void main(String[] args) {
        launch();
    }
}
