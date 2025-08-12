package com.weatherapp;

import java.util.List;

/**
 * Lightweight model for current weather and forecast entries.
 * Only contains fields used by the UI to keep things simple.
 */
public class WeatherData {
    public String cityName;
    public double temp;
    public double feelsLike;
    public int humidity;
    public double windSpeed;
    public String condition;
    public String description;
    public String icon; // OpenWeatherMap icon code
    public long timestamp; // epoch seconds
    public long sunrise;
    public long sunset;
    public List<ForecastEntry> forecast;

    public static class ForecastEntry {
        public long timestamp;
        public double temp;
        public String condition;
        public String icon;
    }
}
