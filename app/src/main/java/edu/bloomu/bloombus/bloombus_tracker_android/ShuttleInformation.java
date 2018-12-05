package edu.bloomu.bloombus.bloombus_tracker_android;

public class ShuttleInformation {
    public String type;
    public GeoJSONGeometry geometry;
    public ShuttleGeoJSONProperties properties;

    public ShuttleInformation(String type, GeoJSONGeometry geometry, ShuttleGeoJSONProperties geoJsonProps) {
        this.type = type;
        this.geometry = geometry;
        this.properties = geoJsonProps;
    }
}
