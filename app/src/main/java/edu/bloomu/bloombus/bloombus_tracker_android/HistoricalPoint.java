package edu.bloomu.bloombus.bloombus_tracker_android;

import java.util.List;

public class HistoricalPoint {
    public List<Double> coordinates;
    public String targetBusStop;
    public float speed;
    public long timestamp;

    public HistoricalPoint(List<Double> coordinates, String targetBusStop, float speed, long timestamp) {
        this.coordinates = coordinates;
        this.targetBusStop = targetBusStop;
        this.speed = speed;
        this.timestamp = timestamp;
    }
}