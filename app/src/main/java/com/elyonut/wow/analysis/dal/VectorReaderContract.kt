package com.elyonut.wow.analysis.dal

import android.provider.BaseColumns

class VectorReaderContract  // To prevent someone from accidentally instantiating the contract class,
// make the constructor private.
private constructor() {
    /* Inner class that defines the table contents */
    object VectorEntry : BaseColumns {
        const val TABLE_NAME = "entry"
        const val COLUMN_NAME_FEATURE_ID = "feature_id"
        const val COLUMN_NAME_MAX_LON = "max_lon"
        const val COLUMN_NAME_MIN_LON = "min_lon"
        const val COLUMN_NAME_MAX_LAT = "max_lat"
        const val COLUMN_NAME_MIN_LAT = "min_lat"
    }
}