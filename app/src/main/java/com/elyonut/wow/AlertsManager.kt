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
//    var alerts = MutableLiveData<LinkedList<AlertModel>>()
    var isAlertAccepted = MutableLiveData<Boolean>()
    var isAlertAdded = MutableLiveData<Boolean>()
    var deletedAlertPosition = MutableLiveData<Int>()
    var shouldPopAlert = MutableLiveData<Boolean>()
    var shouldRemoveAlert = MutableLiveData<Boolean>()
    var alertToPop = MutableLiveData<AlertModel>()
//    private var idCounter = 0

    //
//    private var job = Job()
//    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    val alerts = database.getAllAlerts()

    init {
//        alerts.value = LinkedList()
        shouldPopAlert.value = false
        shouldRemoveAlert.value = false
    }

    private suspend fun insert(alert: AlertModel) {
        withContext(Dispatchers.IO) {
            DB.getInstance(context.applicationContext).alertDatabaseDao.insert(alert)
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
//        uiScope.launch {
//            insert(alert)
//        }.invokeOnCompletion {
//            shouldPopAlert.postValue(true)
//        }

        CoroutineScope(Dispatchers.Main).launch {
            insert(alert)
//            shouldPopAlert.postValue(true)
            getAlertToPop()
        }
    }

    suspend fun getAlertToPop() {
        withContext(Dispatchers.IO) {
            if (shouldPopAlert.value!! && alerts.value!!.count { !it.isRead } > 0) {
                alertToPop.postValue(alerts.value?.findLast { !it.isRead }!!)
            }
        }
    }

    fun deleteAlert(alert: AlertModel) {
//        alerts.value?.removeAt(position)
//        updateAlertsList()
//        shouldRemoveAlert.value = true
//        shouldPopAlert.value = true
//        deletedAlertPosition.value = position

//        uiScope.launch {
//            delete(alert)
//        }.invokeOnCompletion {
//            shouldRemoveAlert.value = true
//            shouldPopAlert.value = true
//        }
    }

    fun zoomToLocation(alert: AlertModel) {
        sendBroadcastIntent(Constants.ZOOM_LOCATION_ACTION, alert.threatId, alert.alertID)
        shouldRemoveAlert.value = true
    }

    fun acceptAlert(alert: AlertModel) {
        sendBroadcastIntent(Constants.ALERT_ACCEPTED_ACTION, alert.threatId, alert.alertID)
        shouldRemoveAlert.value = true
    }

    private fun updateAlertsList() {
//        alerts.value = alerts.value
    }

    private fun sendBroadcastIntent(actionName: String, threatId: String, alertID: Int) {
        val actionIntent = Intent(actionName).apply {
            putExtra("threatID", threatId)
            putExtra("alertID", alertID)
        }

        this.context.sendBroadcast(actionIntent)
    }

    fun updateMessageAccepted(alertId: Int) {
        val alert = alerts.value?.find { it.alertID == alertId }

//        alert?.let {
//            uiScope.launch {
//                alert.isRead = true
//                update(alert)
//            }.invokeOnCompletion {
//                isAlertAccepted.value = true
//            }
//        }

//        if (alert != null) {
//            alert.isRead = true
//        }
//
//        updateAlertsList()
//        isAlertAccepted.value = true
    }
}