package edu.bloomu.bloombus.bloombus_tracker_android;

import java.util.List;

public class GeoJSONGeometry {
    public String type;
    public List<Double> coordinates;

    public GeoJSONGeometry(String type, List<Double> coordinates) {
        this.type = type;
        this.coordinates = coordinates;
    }
}
