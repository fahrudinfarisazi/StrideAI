package com.faris.strideai.models;

import java.io.Serializable;

public class ActivitySession implements Serializable {
    private String id;
    private String title;
    private long timestamp;
    private float distanceKm;
    private int steps;
    private int calories;
    private long durationMs;
    private String mapSnapshotPath;

    public ActivitySession(String id, String title, long timestamp, float distanceKm, int steps, int calories, long durationMs, String mapSnapshotPath) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.distanceKm = distanceKm;
        this.steps = steps;
        this.calories = calories;
        this.durationMs = durationMs;
        this.mapSnapshotPath = mapSnapshotPath;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public long getTimestamp() { return timestamp; }
    public float getDistanceKm() { return distanceKm; }
    public int getSteps() { return steps; }
    public int getCalories() { return calories; }
    public long getDurationMs() { return durationMs; }
    public String getMapSnapshotPath() { return mapSnapshotPath; }
}
