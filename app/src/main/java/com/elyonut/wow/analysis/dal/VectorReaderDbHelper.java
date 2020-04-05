package com.elyonut.wow.analysis.dal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.elyonut.wow.analysis.dal.VectorReaderContract.VectorEntry;

public class VectorReaderDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 3;
    // private static final String DATABASE_NAME = "VectorReader.db"; // uncomment for file based DB
    private static final String DATABASE_NAME = null; // uncomment for in-memory DB

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + VectorEntry.TABLE_NAME + " (" +
                    VectorEntry._ID + " INTEGER PRIMARY KEY," +
                    VectorEntry.COLUMN_NAME_FEATURE_ID + " TEXT," +
                    VectorEntry.COLUMN_NAME_MIN_LON + " REAL," +
                    VectorEntry.COLUMN_NAME_MAX_LON + " REAL," +
                    VectorEntry.COLUMN_NAME_MIN_LAT + " REAL," +
                    VectorEntry.COLUMN_NAME_MAX_LAT + " REAL)";

    private static final String SQL_CREATE_INDEX_MIN_LON =
            "CREATE INDEX " + VectorEntry.TABLE_NAME + "_" + VectorEntry.COLUMN_NAME_MIN_LON + "_idx " +
                    "ON " + VectorEntry.TABLE_NAME + "("+ VectorEntry.COLUMN_NAME_MIN_LON + ");";

    private static final String SQL_CREATE_INDEX_MAX_LON =
            "CREATE INDEX " + VectorEntry.TABLE_NAME + "_" + VectorEntry.COLUMN_NAME_MAX_LON + "_idx " +
                    "ON " + VectorEntry.TABLE_NAME + "("+ VectorEntry.COLUMN_NAME_MAX_LON + ");";

    private static final String SQL_CREATE_INDEX_MIN_LAT =
            "CREATE INDEX " + VectorEntry.TABLE_NAME + "_" + VectorEntry.COLUMN_NAME_MIN_LAT + "_idx " +
                    "ON " + VectorEntry.TABLE_NAME + "("+ VectorEntry.COLUMN_NAME_MIN_LAT + ");";

    private static final String SQL_CREATE_INDEX_MAX_LAT =
            "CREATE INDEX " + VectorEntry.TABLE_NAME + "_" + VectorEntry.COLUMN_NAME_MAX_LAT + "_idx " +
                    "ON " + VectorEntry.TABLE_NAME + "("+ VectorEntry.COLUMN_NAME_MAX_LAT + ");";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + VectorEntry.TABLE_NAME;

    public VectorReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_INDEX_MIN_LON);
        db.execSQL(SQL_CREATE_INDEX_MAX_LON);
        db.execSQL(SQL_CREATE_INDEX_MIN_LAT);
        db.execSQL(SQL_CREATE_INDEX_MAX_LAT);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
