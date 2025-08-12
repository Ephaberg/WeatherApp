package com.weatherapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

/**
 * Utility methods for the WeatherApp.
 */
public class Utils {

    /**
     * Formats a Unix timestamp into "EEE HH:mm" format.
     * Example: "Mon 07:15"
     *
     * @param epochSeconds Unix time in seconds
     * @return Formatted time string
     */
    public static String fmtTime(long epochSeconds) {
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE HH:mm")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());
            return f.format(Instant.ofEpochSecond(epochSeconds));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Loads the API key from a .env file.
     *
     * @param envFilePath Path to the .env file
     * @return API key as a string
     * @throws IOException if reading fails
     */
    public static String loadApiKey(String envFilePath) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(envFilePath)) {
            props.load(fis);
        }
        return props.getProperty("API_KEY");
    }

    /**
     * Converts Celsius to Fahrenheit.
     */
    public static double celsiusToFahrenheit(double celsius) {
        return (celsius * 9 / 5) + 32;
    }

    /**
     * Converts Fahrenheit to Celsius.
     */
    public static double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }

    /**
     * Selects background image based on current time.
     * Returns "day.png" between 6 AM and 6 PM, otherwise "night.png".
     */
    public static String getBackgroundImage() {
        int hour = java.time.LocalTime.now().getHour();
        return (hour >= 6 && hour < 18) ? "day.png" : "night.png";
    }
}
