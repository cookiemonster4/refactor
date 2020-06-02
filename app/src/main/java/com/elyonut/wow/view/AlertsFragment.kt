package com.elyonut.wow.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.R
import com.elyonut.wow.adapter.AlertsAdapter
import com.elyonut.wow.databinding.FragmentAlertsBinding
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.viewModel.AlertsViewModel
import com.elyonut.wow.viewModel.AlertsViewModelFactory
import com.elyonut.wow.viewModel.SharedViewModel

class AlertsFragment : Fragment() {
    private var listener: OnAlertsFragmentInteractionListener? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var alertsViewModel: AlertsViewModel
    private lateinit var alertsManager: AlertsManager
    private lateinit var adapter: AlertsAdapter
    private lateinit var binding: FragmentAlertsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_alerts, container, false
        )

        val application = requireNotNull(this.activity).application

        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

        alertsManager = sharedViewModel.alertsManager

        alertsViewModel = ViewModelProviders.of(
            this,
            AlertsViewModelFactory(
                application,
                alertsManager
            )
        ).get(AlertsViewModel::class.java)

        adapter = AlertsAdapter(
            context!!,
            AlertsAdapter.AlertClickListener(
                { onDeleteClick(it) },
                { onZoomClick(it) },
                { onAcceptClick(it) })
        )

        binding.alertsList.adapter = adapter

        alertsViewModel.getAlerts().observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it.isEmpty()) {
                    binding.alertsList.visibility = View.GONE
                    binding.noAlertsMessage.visibility = View.VISIBLE
                } else {
                    adapter.data = it
                    binding.alertsList.visibility = View.VISIBLE
                    binding.noAlertsMessage.visibility = View.GONE
                }
            }
        })

        binding.lifecycleOwner = this

        val manager = LinearLayoutManager(context)
        binding.alertsList.layoutManager = manager

        return binding.root
    }

    private fun onDeleteClick(alert: AlertModel) {
        alertsViewModel.deleteAlertClicked(alert)
    }

    private fun onZoomClick(alert: AlertModel) {
        alertsViewModel.zoomToLocationClicked(alert)
    }

    private fun onAcceptClick(alert: AlertModel) {
        alertsViewModel.acceptAlertClicked(alert)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnAlertsFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnAlertFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnAlertsFragmentInteractionListener {
        fun onAlertsFragmentInteraction()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            AlertsFragment().apply {
            }
    }
}
