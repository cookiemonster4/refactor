package com.elyonut.wow.analysis.dal

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.elyonut.wow.analysis.dal.CoverageReaderContract.CoverageEntry
import com.elyonut.wow.model.Coordinate
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CoverageReaderDbHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        // If you change the database schema, you must increment the database version.
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "CoverageReader.db" // uncomment for file based DB
        // private static final String DATABASE_NAME = null; // uncomment for in-memory DB
        const val ASSETS_PATH = "databases"

        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE " + CoverageEntry.TABLE_NAME + " (" +
                    CoverageEntry._ID + " INTEGER PRIMARY KEY," +
                    CoverageEntry.COLUMN_NAME_FEATURE_ID + " TEXT," +
                    CoverageEntry.COLUMN_NAME_RADIUS + " REAL," +
                    CoverageEntry.COLUMN_NAME_RESOLUTION + " REAL," +
                    CoverageEntry.COLUMN_NAME_HEIGHT + " REAL," +
                    CoverageEntry.COLUMN_NAME_LAT + " REAL," +
                    CoverageEntry.COLUMN_NAME_LON + " REAL)"
        private const val SQL_CREATE_INDEX_FEATURE_ID =
            "CREATE INDEX " + CoverageEntry.TABLE_NAME + "_" + CoverageEntry.COLUMN_NAME_FEATURE_ID + "_idx " +
                    "ON " + CoverageEntry.TABLE_NAME + "(" + CoverageEntry.COLUMN_NAME_FEATURE_ID + ");"
        private const val SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + CoverageEntry.TABLE_NAME
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(
        "${context.packageName}.database_versions",
        Context.MODE_PRIVATE
    )

    private fun installedDatabaseIsOutdated(): Boolean {
        return preferences.getInt(DATABASE_NAME, 0) < DATABASE_VERSION
    }

    private fun writeDatabaseVersionInPreferences() {
        preferences.edit().apply {
            putInt(DATABASE_NAME, DATABASE_VERSION)
            apply()
        }
    }

    private fun installDatabaseFromAssets() {
        val inputStream = context.assets.open("$ASSETS_PATH/$DATABASE_NAME")

        try {
            val outputFile = File(context.getDatabasePath(DATABASE_NAME).path)
            val outputStream = FileOutputStream(outputFile)

            inputStream.copyTo(outputStream)
            inputStream.close()

            outputStream.flush()
            outputStream.close()
        } catch (exception: Throwable) {
            throw RuntimeException("The $DATABASE_NAME database couldn't be installed.", exception)
        }
    }

    @Synchronized
    private fun installOrUpdateIfNecessary() {
        if (installedDatabaseIsOutdated()) {
            context.deleteDatabase(DATABASE_NAME)
            installDatabaseFromAssets()
            writeDatabaseVersionInPreferences()
        }
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        // Caution, any data being written by the user will be removed, otherwise disable the write,
        // create other db for user and merge them on update..
        // throw RuntimeException("The $DATABASE_NAME database is not writable.")
        installOrUpdateIfNecessary()
        return super.getWritableDatabase()
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        installOrUpdateIfNecessary()
        return super.getReadableDatabase()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
        db.execSQL(SQL_CREATE_INDEX_FEATURE_ID)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        onUpgrade(db, oldVersion, newVersion)
    }

    /**
     * NOTE: make sure running in background thread
     */
    fun insert(
        featureId: String?,
        radius: Double,
        resolution: Double,
        heightMeters: Double,
        coverageCoords: List<Coordinate>
    ) { // Gets the data repository in write mode

        val db = writableDatabase
        db.beginTransaction()
        try { // Create a new map of values, where column names are the keys
            val values = ContentValues()
            for (coord in coverageCoords) {
                values.put(CoverageEntry.COLUMN_NAME_FEATURE_ID, featureId)
                values.put(CoverageEntry.COLUMN_NAME_RADIUS, radius)
                values.put(CoverageEntry.COLUMN_NAME_RESOLUTION, resolution)
                values.put(CoverageEntry.COLUMN_NAME_HEIGHT, heightMeters)
                values.put(CoverageEntry.COLUMN_NAME_LAT, coord.latitude)
                values.put(CoverageEntry.COLUMN_NAME_LON, coord.longitude)
                // Insert the new row, returning the primary key value of the new row
                db.insert(CoverageEntry.TABLE_NAME, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * NOTE: make sure running in background thread
     */
    fun getCoverageCoords(
        featureId: String,
        radius: Double,
        resolution: Double,
        heightMeters: Double
    ): List<Coordinate> {
        val db = readableDatabase
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(
            CoverageEntry.COLUMN_NAME_LAT,
            CoverageEntry.COLUMN_NAME_LON
        )
        // Filter results WHERE coverage is the same featureId and height, with larger radius or smaller(better) resolution
        val selection = CoverageEntry.COLUMN_NAME_FEATURE_ID + " = ? " +
                "AND " +
                CoverageEntry.COLUMN_NAME_RADIUS + " >= ? " +
                "AND " +
                CoverageEntry.COLUMN_NAME_RESOLUTION + " <= ? " +
                "AND " +
                CoverageEntry.COLUMN_NAME_HEIGHT + " = ? "
        val selectionArgs = arrayOf(
            featureId,
            radius.toString(),
            resolution.toString(),
            heightMeters.toString()
        )
        // How you want the results sorted in the resulting Cursor
        val sortOrder = CoverageEntry.COLUMN_NAME_FEATURE_ID + " DESC"
        val coverageCoords: MutableList<Coordinate> =
            ArrayList()
        db.query(
            CoverageEntry.TABLE_NAME,  // The table to query
            projection,  // The array of columns to return (pass null to get all)
            selection,  // The columns for the WHERE clause
            selectionArgs,  // The values for the WHERE clause
            null,  // don't group the rows
            null,  // don't filter by row groups
            sortOrder // The sort order
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val lat = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(CoverageEntry.COLUMN_NAME_LAT)
                )
                val lon = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(CoverageEntry.COLUMN_NAME_LON)
                )
                coverageCoords.add(Coordinate(lat, lon))
            }
        }
        return coverageCoords
    }

    /**
     * NOTE: make sure running in background thread
     */
    fun remove(featureId: String, heightMeters: Double) {
        val db = writableDatabase
        // Filter results WHERE featureId, and leave the default building values (-1000) in DB
        val where =
            CoverageEntry.COLUMN_NAME_FEATURE_ID + " = ? AND " + CoverageEntry.COLUMN_NAME_HEIGHT + " = ?"
        val whereArgs =
            arrayOf(featureId, heightMeters.toString())
        db.delete(CoverageEntry.TABLE_NAME, where, whereArgs)
    }



}