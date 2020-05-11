package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.interfaces.OnClickInterface
import com.elyonut.wow.view.AlertsFragment

class AlertsViewModelFactory(val application: Application, var alertsManager: AlertsManager): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return  AlertsViewModel(application, alertsManager) as T
    }
}