package edu.bloomu.bloombus.bloombus_tracker_android;

import com.google.android.gms.maps.model.LatLng;

public class GeoJSONGeometry {
    public String type;
    public LatLng coordinates;

    public GeoJSONGeometry(String type, LatLng coordinates) {
        this.type = type;
        this.coordinates = coordinates;
    }
}
