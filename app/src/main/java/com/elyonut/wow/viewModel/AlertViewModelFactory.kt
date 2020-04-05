package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.elyonut.wow.AlertsManager

class AlertViewModelFactory(val application: Application, var alertsManager: AlertsManager): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AlertViewModel(application, alertsManager) as T
    }
}