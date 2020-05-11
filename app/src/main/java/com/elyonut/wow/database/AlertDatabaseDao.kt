package com.elyonut.wow.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.elyonut.wow.model.AlertModel
import java.util.*

@Dao
interface AlertDatabaseDao {
    @Insert
    fun insert(alert: AlertModel)

    @Update
    fun update(alert: AlertModel)

    @Delete
    fun delete(alert: AlertModel)

    @Query("SELECT * from alerts_table WHERE alertID = :key")
    fun get(key: Long): AlertModel?

    @Query("DELETE FROM alerts_table")
    fun clear()

    @Query("SELECT * FROM alerts_table ORDER BY alertID DESC")
    fun getAllAlerts(): LiveData<List<AlertModel>>
}