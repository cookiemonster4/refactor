package com.elyonut.wow.analysis;


import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VectorEnvelope implements Comparable<VectorEnvelope>{
    private Double minLongitude;
    private Double maxLongitude;
    private Double minLatitude;
    private Double maxLatitude;
    private List<Point> coordinates;
    private Polygon polygon;
    private HashMap<String, String> properties;

    VectorEnvelope(List<Point> coordinates, HashMap<String, String> properties) {
        this.coordinates = coordinates;
        this.properties = properties;

        minLongitude = Double.MAX_VALUE;
        maxLongitude = Double.MIN_VALUE;
        minLatitude = Double.MAX_VALUE;
        maxLatitude = Double.MIN_VALUE;

        for(Point c: coordinates){
            if(c.longitude() < minLongitude)
                minLongitude = c.longitude();
            if(c.latitude() < minLatitude)
                minLatitude = c.latitude();
            if(c.longitude() > maxLongitude)
                maxLongitude = c.longitude();
            if(c.latitude() > maxLatitude)
                maxLatitude = c.latitude();
        }

        List<List<Point>> list = new ArrayList<>();
        list.add(coordinates);
        polygon = Polygon.fromLngLats(list);

    }

    public List<Point> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Point> coordinates) {
        this.coordinates = coordinates;
    }

    Double getMinLongitude() {
        return minLongitude;
    }

    public void setMinLongitude(double minLongitude) {
        this.minLongitude = minLongitude;
    }

    Double getMaxLongitude() {
        return maxLongitude;
    }

    public void setMaxLongitude(double maxLongitude) {
        this.maxLongitude = maxLongitude;
    }

    Double getMinLatitude() {
        return minLatitude;
    }

    public void setMinLatitude(double minLatitude) {
        this.minLatitude = minLatitude;
    }

    Double getMaxLatitude() {
        return maxLatitude;
    }

    public void setMaxLatitude(double maxLatitude) {
        this.maxLatitude = maxLatitude;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, String> properties) {
        this.properties = properties;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public int compareByMinLongitude(VectorEnvelope other){
        if(other == null)
            return -1;
        else return ((Double)this.getMinLongitude()).compareTo((Double)other.getMinLongitude());
    }

    @Override
    public int compareTo(VectorEnvelope vectorEnvelope) {
        return this.getMinLongitude().compareTo(vectorEnvelope.getMinLongitude()) * -1;
    }
}
