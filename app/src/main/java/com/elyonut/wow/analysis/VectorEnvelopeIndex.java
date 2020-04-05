package com.elyonut.wow.analysis;

import com.elyonut.wow.analysis.quadtree.Envelope;
import com.elyonut.wow.analysis.quadtree.Quadtree;
import com.elyonut.wow.analysis.quadtree.SpatialIndex;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfJoins;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VectorEnvelopeIndex {

    //    private List<VectorEnvelope> envelopeList;
//    private VectorReaderDbHelper dbHelper;
//    private HashMap<String, VectorEnvelope> featureIdToEnvelopes;
    private SpatialIndex spatialIndex;

    public VectorEnvelopeIndex(){
//        this.dbHelper = new VectorReaderDbHelper(App.Companion.getInstance().getBaseContext());
//        this.featureIdToEnvelopes = new HashMap<>();
//        this.envelopeList = new ArrayList<>();
        this.spatialIndex = new Quadtree();
    }

    public void loadBuildingFromGeoJson(String geoJson) {
        try {
            JsonObject jsonObject =  new JsonParser().parse(geoJson).getAsJsonObject();
            JsonArray features = jsonObject.getAsJsonArray("features");
            for (int featureIndex = 0; featureIndex < features.size(); featureIndex++) {
                JsonObject feature = (JsonObject) features.get(featureIndex);
                JsonObject propertiesObj = feature.getAsJsonObject("properties");
                String id = propertiesObj.get("id").getAsString();
                Double height = propertiesObj.get("height").getAsDouble();
                JsonObject routeCoordinates2dObject = feature.getAsJsonObject("geometry");
                JsonArray routeCoordinatesJsonArr2d = routeCoordinates2dObject.get("coordinates").getAsJsonArray();
                List<Point> location = new ArrayList<>();
                for (int routeIndex = 0; routeIndex < routeCoordinatesJsonArr2d.size(); routeIndex++) {
                    JsonArray coordinates2dObj = routeCoordinatesJsonArr2d.get(routeIndex).getAsJsonArray();
                    for (int coordIndex = 0; coordIndex < coordinates2dObj.size(); coordIndex++) {
                        JsonArray coordJsonArr = coordinates2dObj.get(coordIndex).getAsJsonArray();
                        Point point = Point.fromLngLat(coordJsonArr.get(0).getAsDouble(), coordJsonArr.get(1).getAsDouble());
                        location.add(point);
                    }
                }

                HashMap<String, String> buildingProperties = new HashMap<>();
                buildingProperties.put("name", id);
                buildingProperties.put("id", id);
                buildingProperties.put("height", height.toString());
                buildingProperties.put("type", "Building");

                VectorEnvelope env = new VectorEnvelope(location, buildingProperties);
                // insertToDb(env);
                // envelopeList.add(env);
                Envelope quadEnv = new Envelope(env.getMaxLongitude(), env.getMinLongitude(), env.getMaxLatitude(), env.getMinLatitude());
                spatialIndex.insert(quadEnv, env);
            }

            // Collections.sort(envelopeList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getIdFromKmlDescription(String description) {
        final Pattern pattern = Pattern.compile("(?<=UniqueId - )(?s)(.*$)", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(description);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private Double getHeightFromKmlDescription(String description) {
        final Pattern pattern = Pattern.compile("<\\/BR>max_height - (.+?)<BR>", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(description);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 4.0;
    }

    public double getHeight(Double longitude, Double latitude){
        VectorEnvelope vector = getVectorQuad(longitude, latitude);
        if(vector != null)
        {
            if(vector.getProperties().containsKey("height"))
                return Double.parseDouble(vector.getProperties().get("height"));
        }
        return 0;
    }

    /*
    private void insertToDb(VectorEnvelope env){
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String featureId = env.properties.get("id");
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(VectorEntry.COLUMN_NAME_FEATURE_ID, featureId);
        values.put(VectorEntry.COLUMN_NAME_MIN_LON, env.getMinLongitude());
        values.put(VectorEntry.COLUMN_NAME_MAX_LON, env.getMaxLongitude());
        values.put(VectorEntry.COLUMN_NAME_MIN_LAT, env.getMinLatitude());
        values.put(VectorEntry.COLUMN_NAME_MAX_LAT, env.getMaxLatitude());

        // Insert the new row, returning the primary key value of the new row
        db.insert(VectorEntry.TABLE_NAME, null, values);

        // keep mapping for featureId
        this.featureIdToEnvelopes.put(featureId, env);
    }

    private VectorEnvelope getVector(Point point){
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                BaseColumns._ID,
                VectorEntry.COLUMN_NAME_FEATURE_ID,
                VectorEntry.COLUMN_NAME_MIN_LON,
                VectorEntry.COLUMN_NAME_MAX_LON,
                VectorEntry.COLUMN_NAME_MIN_LAT,
                VectorEntry.COLUMN_NAME_MAX_LAT
        };

        // Filter results WHERE Point inside envelope rectangle
        String selection = VectorEntry.COLUMN_NAME_MIN_LON + " < ? " +
                "AND " +
                VectorEntry.COLUMN_NAME_MAX_LON + " > ? " +
                "AND " +
                VectorEntry.COLUMN_NAME_MIN_LAT + " < ? " +
                "AND " +
                VectorEntry.COLUMN_NAME_MAX_LAT + " > ? ";
        final String longitude = String.valueOf(point.longitude());
        final String latitude = String.valueOf(point.latitude());
        String[] selectionArgs = { longitude , longitude, latitude, latitude };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                VectorEntry.COLUMN_NAME_FEATURE_ID + " DESC";

        try (Cursor cursor = db.query(
                VectorEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        )) {
            while (cursor.moveToNext()) {
                String featureId = cursor.getString(
                        cursor.getColumnIndexOrThrow(VectorEntry.COLUMN_NAME_FEATURE_ID));
                VectorEnvelope env = this.featureIdToEnvelopes.get(featureId);
                boolean inside = TurfJoins.inside(point, env.getPolygon());
                if (inside) {
                    return env;
                }
            }
        }
        return null;

    }

    public VectorEnvelope getVectorNaive(Point point){
        for (VectorEnvelope env: envelopeList) {
            if(env.getMinLongitude()<= point.longitude()
                    && env.getMaxLongitude() > point.longitude()
                    && env.getMinLatitude() < point.latitude()
                    && env.getMaxLatitude() >= point.latitude()
            ){
                boolean inside = TurfJoins.inside(point, env.getPolygon());
                if(inside)
                    return env;
            }
        }
        return null;
    }
    */

    private VectorEnvelope getVectorQuad(Point point){
        Envelope searchEnv = new Envelope(point.longitude(),point.longitude(), point.latitude(), point.latitude());
        List<VectorEnvelope> queryRes = spatialIndex.query(searchEnv);

        for (VectorEnvelope env: queryRes) {
            if(env.getMinLongitude()<= point.longitude()
                    && env.getMaxLongitude() > point.longitude()
                    && env.getMinLatitude() < point.latitude()
                    && env.getMaxLatitude() >= point.latitude()
            ) {
                boolean inside = TurfJoins.inside(point, env.getPolygon());
                if (inside)
                    return env;
            }
        }

        return null;
    }

    public List<VectorEnvelope> getMultipleVectors(Envelope searchEnv, Polygon polygon){

        List<VectorEnvelope> queryRes = spatialIndex.query(searchEnv);
        List<VectorEnvelope> res = new ArrayList<>();

        for(int i = queryRes.size() -1;i>=0;i--) {
            VectorEnvelope env = queryRes.get(i);
            double pointsInside = 0;
            final double totalCorners = env.getCoordinates().size();
            for (Point point : env.getCoordinates()) {
                if (searchEnv.getMinX() <= point.longitude()
                        && searchEnv.getMaxX() > point.longitude()
                        && searchEnv.getMinY() < point.latitude()
                        && searchEnv.getMaxY() >= point.latitude()
                ) {

                    boolean inside = TurfJoins.inside(point,polygon);
                    if (inside) {
                        pointsInside++;
                        final double currentInsideRatio = pointsInside / totalCorners;
                        if(currentInsideRatio > 0.4){
                            res.add(env);
                            break;
                        }
                    }
                }
            }
        }

        return res;
    }

    public VectorEnvelope getVectorQuad(Double longitude, Double latitude){
        Point turfPoint = Point.fromLngLat(longitude, latitude);
        Envelope searchEnv = new Envelope(longitude, longitude, latitude, latitude);
        List<VectorEnvelope> queryRes = spatialIndex.query(searchEnv);

        for (VectorEnvelope env: queryRes) {
            if(env.getMinLongitude()<= turfPoint.longitude()
                    && env.getMaxLongitude() > turfPoint.longitude()
                    && env.getMinLatitude() < turfPoint.latitude()
                    && env.getMaxLatitude() >= turfPoint.latitude()
            ) {
                boolean inside = TurfJoins.inside(turfPoint, env.getPolygon());
                if (inside)
                    return env;
            }
        }

        return null;
    }




}