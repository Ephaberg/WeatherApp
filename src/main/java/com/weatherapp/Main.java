package com.weatherapp;

/**
 * Simple launcher. Main is required by the shaded JAR manifest.
 * It delegates to the JavaFX Application subclass.
 */
public class Main {
    public static void main(String[] args) {
        WeatherApp.main(args);
    }
}
