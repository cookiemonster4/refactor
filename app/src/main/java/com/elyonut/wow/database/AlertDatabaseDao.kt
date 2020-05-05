package com.elyonut.wow.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.elyonut.wow.model.AlertModel

@Dao
interface AlertDatabaseDao {
    @Insert
    fun insert(alert: AlertModel)

    @Update
    fun update(alert: AlertModel)

    @Query("SELECT * from alerts_table WHERE alertID = :key")
    fun get(key: Long): AlertModel?

    @Query("DELETE FROM alerts_table")
    fun clear()

    @Query("SELECT * FROM alerts_table ORDER BY alertID DESC")
    fun getAllNights(): LiveData<List<AlertModel>>
}