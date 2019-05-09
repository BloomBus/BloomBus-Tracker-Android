package edu.bloomu.bloombus.bloombus_tracker_android;

import com.google.android.gms.maps.model.LatLng;

class ShuttleGeoJSONProperties {
    public String loopKey;
    public String loopDisplayName;
    public long timestamp;
    public float speed;
    public float bearing;
    public double altitude;
    public String appVersion;
    public LatLng prevCoordinates;

    public ShuttleGeoJSONProperties(
        String loopKey,
        String loopDisplayName,
        long timestamp,
        float speed,
        float bearing,
        double altitude,
        String appVersion,
        LatLng prevCoordinates)
    {
        this.loopKey = loopKey;
        this.loopDisplayName = loopDisplayName;
        this.timestamp = timestamp;
        this.speed = speed;
        this.bearing = bearing;
        this.altitude = altitude;
        this.appVersion = appVersion;
        this.prevCoordinates = prevCoordinates;
    }
}