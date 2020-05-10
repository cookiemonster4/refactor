package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.adapter.AlertsAdapter
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.interfaces.OnClickInterface
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.view.AlertsFragment
import java.util.*

class AlertsViewModel(application: Application, var alertsManager: AlertsManager): AndroidViewModel(application) {

    fun getAlerts(): LiveData<LinkedList<AlertModel>> {
        return alertsManager.alerts
    }

    fun zoomToLocationClicked(alert: AlertModel) {
        alertsManager.zoomToLocation(alert)
    }

    fun acceptAlertClicked(alert: AlertModel) {
        alertsManager.acceptAlert(alert)
    }

    fun deleteAlertClicked(position: Int) {
        alertsManager.deleteAlert(position)
    }
}