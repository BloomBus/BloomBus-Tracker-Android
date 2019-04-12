package edu.bloomu.bloombus.bloombus_tracker_android;

import java.util.List;

class ShuttleGeoJSONProperties {
    public String loopKey;
    public String loopDisplayName;
    public long timestamp;
    public float speed;
    public double altitude;
    public String appVersion;
    public List<Double> prevCoordinates;

    public ShuttleGeoJSONProperties(String loopKey, String loopDisplayName, long timestamp,
                                    float speed, double altitude, String appVersion, List<Double> prevCoordinates) {
        this.loopKey = loopKey;
        this.loopDisplayName = loopDisplayName;
        this.timestamp = timestamp;
        this.speed = speed;
        this.altitude = altitude;
        this.appVersion = appVersion;
        this.prevCoordinates = prevCoordinates;
    }
}