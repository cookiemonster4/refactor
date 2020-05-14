package com.elyonut.wow

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.database.AlertDatabaseDao
import com.elyonut.wow.database.DB
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.utilities.Constants
import kotlinx.coroutines.*

class AlertsManager(var context: Context, val database: AlertDatabaseDao) {
    var shouldRemoveAlert = MutableLiveData<Boolean>()
    var alertToPop = MutableLiveData<AlertModel>()
    val alerts = database.getAllAlerts()

    init {
        shouldRemoveAlert.value = false
    }

    private suspend fun insert(alert: AlertModel) {
        withContext(Dispatchers.IO) {
            database.insert(alert)
        }
    }

    private suspend fun update(alert: AlertModel) {
        withContext(Dispatchers.IO) {
            database.update(alert)
        }
    }

    private suspend fun delete(alert: AlertModel) {
        withContext(Dispatchers.IO) {
            database.delete(alert)
        }
    }

    fun addAlert(alert: AlertModel) {
        CoroutineScope(Dispatchers.Main).launch {
            insert(alert)
            getAlertToPop()
        }
    }

    private suspend fun getAlertToPop() {
        withContext(Dispatchers.IO) {
            database.getLastUnreadAlert()?.let {
                alertToPop.postValue(it)
            }
        }
    }

    fun deleteAlert(alert: AlertModel) {
        CoroutineScope(Dispatchers.Main).launch {
            delete(alert)
            shouldRemoveAlert.postValue(true)
            getAlertToPop()
        }
    }

    fun zoomToLocation(alert: AlertModel) {
        sendBroadcastIntent(Constants.ZOOM_LOCATION_ACTION, alert.threatId)
        updateMessageAccepted(alert)
        shouldRemoveAlert.postValue(true)
    }

    private fun sendBroadcastIntent(actionName: String, threatId: String) {
        val actionIntent = Intent(actionName).apply {
            putExtra("threatID", threatId)
        }

        this.context.sendBroadcast(actionIntent)
    }

    fun updateMessageAccepted(alert: AlertModel) {
        shouldRemoveAlert.postValue(true)

        alert.let {
            CoroutineScope(Dispatchers.Main).launch {
                alert.isRead = true
                update(alert)
                getAlertToPop()
            }
        }
    }
}