package com.weatherapp;

import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Simple JSON-backed recent-search history.
 * Stored at "history.json" in the working directory (project / where JAR runs).
 * Small, fault tolerant: failures are logged but do not break the app.
 */
public class HistoryManager {
    private static final Path FILE = Path.of("history.json");
    private static final int MAX_ITEMS = 25;
    private final Gson gson = new Gson();

    public static class Item {
        public String city;
        public long when;
        public Item(String city, long when) { this.city = city; this.when = when; }
    }

    public List<Item> load() {
        try {
            if (!Files.exists(FILE)) return new ArrayList<>();
            String json = Files.readString(FILE);
            Type t = new TypeToken<List<Item>>(){}.getType();
            List<Item> list = gson.fromJson(json, t);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void add(String city) {
        try {
            String normalized = city.trim();
            List<Item> list = load();
            list.removeIf(i -> i.city.equalsIgnoreCase(normalized));
            list.add(0, new Item(normalized, Instant.now().getEpochSecond()));
            if (list.size() > MAX_ITEMS) list = list.subList(0, MAX_ITEMS);
            try (Writer w = Files.newBufferedWriter(FILE)) {
                gson.toJson(list, w);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
