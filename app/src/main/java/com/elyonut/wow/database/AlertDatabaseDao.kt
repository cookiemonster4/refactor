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

    @Query("SELECT * from alerts_table WHERE is_read = 0 ORDER BY time ASC LIMIT 1")
    fun getFirstUnreadAlert(): AlertModel?

    @Query("SELECT * FROM alerts_table ORDER BY id DESC")
    fun getAll(): LiveData<List<AlertModel>>
}