package com.weatherapp;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Responsible for calling OpenWeatherMap APIs.
 * - Uses Java 11+ HttpClient with a timeout.
 * - Retries a small number of times for transient failures.
 * - Throws clear exceptions with API error message if available.
 *
 * IMPORTANT: supply a valid API key via .env (API_KEY=...) or environment variable.
 */
public class WeatherService {
    private final String apiKey;
    private final HttpClient http;
    private static final int MAX_RETRIES = 2;

    public WeatherService(String apiKey) {
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Fetches current weather and short forecast for a city.
     * units = "metric" or "imperial"
     */
    public WeatherData getWeather(String city, String units) throws Exception {
        WeatherData current = fetchCurrent(city, units);
        current.forecast = fetchForecast(city, units);
        return current;
    }

    private WeatherData fetchCurrent(String city, String units) throws Exception {
        String url = String.format(
            "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=%s",
            URLEncoder.encode(city, "UTF-8"), apiKey, units);
        JsonObject json = callJsonWithRetries(url);
        // parse robustly (fields may be missing)
        WeatherData w = new WeatherData();
        w.cityName = json.has("name") ? json.get("name").getAsString() : city;
        JsonObject main = json.getAsJsonObject("main");
        w.temp = main.get("temp").getAsDouble();
        w.feelsLike = main.has("feels_like") ? main.get("feels_like").getAsDouble() : w.temp;
        w.humidity = main.get("humidity").getAsInt();
        w.windSpeed = json.has("wind") && json.getAsJsonObject("wind").has("speed")
                ? json.getAsJsonObject("wind").get("speed").getAsDouble() : 0.0;
        JsonArray warr = json.getAsJsonArray("weather");
        if (warr != null && warr.size() > 0) {
            JsonObject w0 = warr.get(0).getAsJsonObject();
            w.condition = w0.has("main") ? w0.get("main").getAsString() : "";
            w.description = w0.has("description") ? w0.get("description").getAsString() : "";
            w.icon = w0.has("icon") ? w0.get("icon").getAsString() : "";
        }
        w.timestamp = json.has("dt") ? json.get("dt").getAsLong() : System.currentTimeMillis()/1000L;
        if (json.has("sys")) {
            JsonObject sys = json.getAsJsonObject("sys");
            w.sunrise = sys.has("sunrise") ? sys.get("sunrise").getAsLong() : 0L;
            w.sunset = sys.has("sunset") ? sys.get("sunset").getAsLong() : 0L;
        }
        return w;
    }

    private List<WeatherData.ForecastEntry> fetchForecast(String city, String units) throws Exception {
        String url = String.format(
            "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=%s",
            URLEncoder.encode(city, "UTF-8"), apiKey, units);
        JsonObject json = callJsonWithRetries(url);
        List<WeatherData.ForecastEntry> out = new ArrayList<>();
        if (!json.has("list")) return out;
        JsonArray arr = json.getAsJsonArray("list");
        int take = Math.min(6, arr.size()); // ~18 hours
        for (int i = 0; i < take; i++) {
            JsonObject e = arr.get(i).getAsJsonObject();
            WeatherData.ForecastEntry fe = new WeatherData.ForecastEntry();
            fe.timestamp = e.get("dt").getAsLong();
            fe.temp = e.getAsJsonObject("main").get("temp").getAsDouble();
            JsonArray warr = e.getAsJsonArray("weather");
            if (warr != null && warr.size() > 0) {
                JsonObject w0 = warr.get(0).getAsJsonObject();
                fe.condition = w0.has("main") ? w0.get("main").getAsString() : "";
                fe.icon = w0.has("icon") ? w0.get("icon").getAsString() : "";
            }
            out.add(fe);
        }
        return out;
    }

    // Reusable method that handles retries and returns parsed JSON object on success.
    private JsonObject callJsonWithRetries(String uri) throws Exception {
        int tries = 0;
        while (tries <= MAX_RETRIES) {
            tries++;
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(uri))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                int status = resp.statusCode();
                JsonElement je = JsonParser.parseString(resp.body());
                if (!je.isJsonObject()) throw new Exception("Invalid JSON response");
                JsonObject jo = je.getAsJsonObject();
                if (status >= 200 && status < 300) return jo;
                String msg = jo.has("message") ? jo.get("message").getAsString() : "HTTP " + status;
                throw new Exception("Error fetching data: " + msg);
            } catch (Exception ex) {
                if (tries > MAX_RETRIES) throw new Exception("Request failed after retries: " + ex.getMessage(), ex);
                // small backoff
                Thread.sleep(400L * tries);
            }
        }
        throw new Exception("Unreachable");
    }
}
