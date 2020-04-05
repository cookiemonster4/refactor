package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.elyonut.wow.adapter.AlertsAdapter
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.interfaces.OnClickInterface
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.view.AlertsFragment

class AlertsViewModel(application: Application, var alertsManager: AlertsManager, onClickHandler: OnClickInterface): AndroidViewModel(application) {
    var alertsAdapter: AlertsAdapter? = null

    init {
        alertsAdapter = AlertsAdapter(
            application,
            alertsManager.alerts.value!!,
            onClickHandler
        )
    }

    fun addAlert() {
        alertsAdapter?.notifyItemInserted(0)
    }

    fun setAlertAccepted() {
        alertsAdapter?.notifyDataSetChanged()
    }

    fun deleteAlert(position: Int) {
        alertsAdapter?.notifyItemRemoved(position)
        alertsAdapter?.notifyItemRangeChanged(position, alertsManager.alerts.value!!.count())
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