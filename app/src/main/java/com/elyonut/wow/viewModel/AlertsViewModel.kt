package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.model.AlertModel

class AlertsViewModel(application: Application, var alertsManager: AlertsManager): AndroidViewModel(application) {

    fun getAlerts(): LiveData<List<AlertModel>> {
        return alertsManager.alerts
    }

    fun zoomToLocationClicked(alert: AlertModel) {
        alertsManager.zoomToLocation(alert)
    }

    fun acceptAlertClicked(alert: AlertModel) {
        alertsManager.markAsRead(alert)
    }

    fun deleteAlertClicked(alert: AlertModel) {
        alertsManager.deleteAlert(alert)
    }
}