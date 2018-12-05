package edu.bloomu.bloombus.bloombus_tracker_android;

class ShuttleGeoJSONProperties {
    String loopKey;
    String loopKeyDisplayName;
    long unixTime;
    long speed;
    long altitude;
    String loop;
    long[] prevCoordinates;

    public ShuttleGeoJSONProperties(String loopKey, String loopKeyDisplayName, long unixTime,
                                    long speed, long altitude, String loop, long[] prevCoordinates) {
        this.loopKey = loopKey;
        this.loopKeyDisplayName = loopKeyDisplayName;
        this.unixTime = unixTime;
        this.speed = speed;
        this.altitude = altitude;
        this.loop = loop;
        this.prevCoordinates = prevCoordinates;
    }
}