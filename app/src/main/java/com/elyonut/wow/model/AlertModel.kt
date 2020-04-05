package com.elyonut.wow.model

data class AlertModel(val alertID: Int  = 0 ,val threatId: String, val message: String, val image: Int, val time: String, var isRead: Boolean = false)