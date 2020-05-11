package com.elyonut.wow

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.utilities.Constants
import java.util.*

class AlertsManager(var context: Context) {
    var alerts = MutableLiveData<LinkedList<AlertModel>>()
    var isAlertAccepted = MutableLiveData<Boolean>()
    var isAlertAdded = MutableLiveData<Boolean>()
    var deletedAlertPosition = MutableLiveData<Int>()
    var shouldPopAlert = MutableLiveData<Boolean>()
    var shouldRemoveAlert = MutableLiveData<Boolean>()
    private var idCounter = 0

    init {
        alerts.value = LinkedList()
        shouldPopAlert.postValue(false)
        shouldRemoveAlert.postValue(false)
    }

    fun addAlert(alert: AlertModel) {
        alerts.value?.add(
            0,
            AlertModel(idCounter, alert.threatId, alert.message, alert.image, alert.time)
        )

        updateAlertsList()
        isAlertAdded.postValue(true)
        shouldPopAlert.postValue(true)
        idCounter++
    }

    fun deleteAlert(position: Int) {
        alerts.value?.removeAt(position)
        updateAlertsList()
        shouldRemoveAlert.postValue(true)
        shouldPopAlert.postValue(true)
        deletedAlertPosition.postValue(position)
    }

    fun zoomToLocation(alert: AlertModel) {
        sendBroadcastIntent(Constants.ZOOM_LOCATION_ACTION, alert.threatId, alert.alertID)
        shouldRemoveAlert.postValue(true)
    }

    fun acceptAlert(alert: AlertModel) {
        sendBroadcastIntent(Constants.ALERT_ACCEPTED_ACTION, alert.threatId, alert.alertID)
        shouldRemoveAlert.postValue(true)
    }

    private fun updateAlertsList() {
        alerts.value = alerts.value
    }

    private fun sendBroadcastIntent(actionName: String, threatId: String, alertID: Int) {
        val actionIntent = Intent(actionName).apply {
            putExtra("threatID", threatId)
            putExtra("alertID", alertID)
        }

        this.context.sendBroadcast(actionIntent)
    }

    fun updateMessageAccepted(messageID: String) {
        val alert = alerts.value?.find { it.threatId == messageID }

        if (alert != null) {
            alert.isRead = true
        }

        updateAlertsList()
        isAlertAccepted.postValue(true)
    }
}