package edu.bloomu.bloombus.bloombus_tracker_android;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.List;

public class GeoJSONGeometry {
    public String type;
    public List<Double> coordinates;

    public GeoJSONGeometry(String type, LatLng coordinates) {
        this.type = type;
        this.coordinates = Arrays.asList(coordinates.longitude, coordinates.latitude);
    }
}
