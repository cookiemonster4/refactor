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
                if (it != alertToPop.value) {
                    alertToPop.postValue(it)
                }
            }
        }
    }

    fun deleteAlert(alert: AlertModel) {
        CoroutineScope(Dispatchers.Main).launch {
            delete(alert)
            shouldRemoveAlert.postValue(true)
        }
    }

    fun zoomToLocation(alert: AlertModel) {
        sendBroadcastIntent(Constants.ZOOM_LOCATION_ACTION, alert.threatId, alert.alertID)
        updateMessageAccepted(alert)
        shouldRemoveAlert.value = true
    }

    fun acceptAlert(alert: AlertModel) {
        sendBroadcastIntent(Constants.ALERT_ACCEPTED_ACTION, alert.threatId, alert.alertID)
        shouldRemoveAlert.value = true
    }

    private fun sendBroadcastIntent(actionName: String, threatId: String, alertID: Int) {
        val actionIntent = Intent(actionName).apply {
            putExtra("threatID", threatId)
            putExtra("alertID", alertID)
        }

        this.context.sendBroadcast(actionIntent)
    }

    fun updateMessageAccepted(alert: AlertModel) {
//        shouldRemoveAlert.value = true
//
//        val alert = alerts.value?.find { it.alertID == alertId }
//
//        alert?.let {
//            CoroutineScope(Dispatchers.Main).launch {
//                alert.isRead = true
//                update(alert)
//                getAlertToPop()
//            }
//        }

        shouldRemoveAlert.value = true

//        val alert = alerts.value?.find { it.alertID == alertId }

        alert.let {
            CoroutineScope(Dispatchers.Main).launch {
                alert.isRead = true
                update(alert)
                getAlertToPop()
            }
        }
    }
}