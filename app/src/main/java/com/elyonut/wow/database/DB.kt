package com.elyonut.wow.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elyonut.wow.model.AlertModel

@Database(entities = [AlertModel::class], version = 2, exportSchema = false)
abstract class DB : RoomDatabase() {
    abstract val alertDatabaseDao: AlertDatabaseDao

    companion object {

        @Volatile
        private var INSTANCE: DB? = null

        fun getInstance(context: Context): DB {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        DB::class.java,
                        "app_database"
                    ).fallbackToDestructiveMigration().build()

                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}