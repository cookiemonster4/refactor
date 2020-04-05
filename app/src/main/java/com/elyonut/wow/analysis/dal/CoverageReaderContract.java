package com.elyonut.wow.analysis.dal;

import android.provider.BaseColumns;

public final class CoverageReaderContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private CoverageReaderContract() {}

    /* Inner class that defines the table contents */
    public static class CoverageEntry implements BaseColumns {
        public static final String TABLE_NAME = "coverage_data";
        public static final String COLUMN_NAME_FEATURE_ID = "feature_id";
        public static final String COLUMN_NAME_RADIUS = "radius";
        public static final String COLUMN_NAME_RESOLUTION = "resolution";
        public static final String COLUMN_NAME_HEIGHT = "height";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LON = "lon";
    }
}
