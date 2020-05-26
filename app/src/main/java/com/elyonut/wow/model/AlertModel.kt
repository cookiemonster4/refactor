package com.elyonut.wow.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts_table")
data class AlertModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "threat_id")
    val threatId: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "image")
    val image: Int,

    @ColumnInfo(name = "time")
    val time: String,

    @ColumnInfo(name = "is_read")
    var isRead: Boolean = false
)