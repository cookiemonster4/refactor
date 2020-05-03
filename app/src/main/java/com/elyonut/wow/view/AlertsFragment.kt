package com.elyonut.wow.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.viewModel.AlertsViewModelFactory
import com.elyonut.wow.R
import com.elyonut.wow.adapter.AlertsAdapter
import com.elyonut.wow.databinding.FragmentAlertsBinding
import com.elyonut.wow.interfaces.OnClickInterface
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.viewModel.AlertsViewModel
import com.elyonut.wow.viewModel.SharedViewModel

class AlertsFragment : Fragment() {
    private var listener: OnAlertsFragmentInteractionListener? = null
    private lateinit var noAlertsMessage: TextView
    private lateinit var onClickHandler: OnClickInterface
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var alertsViewModel: AlertsViewModel
    private lateinit var alertsManager: AlertsManager
    private lateinit var  adapter: AlertsAdapter
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

        noAlertsMessage = binding.noAlertsMessage

        setFragmentContent()
        setObservers()

        alertsViewModel.getAlerts().observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.data = it
            }
        })

        binding.lifecycleOwner = this

        val manager = LinearLayoutManager(context)
        binding.alertsList.layoutManager = manager

        return binding.root
    }

    private fun onDeleteClick(alert: AlertModel) {
        alertsViewModel.deleteAlertClicked(alertsViewModel.getAlerts().value!!.indexOf(alert))
        setFragmentContent()
    }

    private fun onZoomClick(alert: AlertModel) {
        alertsViewModel.zoomToLocationClicked(alert)
    }

    private fun onAcceptClick(alert: AlertModel) {
        alertsViewModel.acceptAlertClicked(alert)
    }

    private fun initClickInterface() {
         onClickHandler = object : OnClickInterface {
            override fun setClick(view: View, position: Int) {
                when (view.id) {
                    R.id.deleteAlert -> {
                        alertsViewModel.deleteAlertClicked(position)
                        setFragmentContent()
                    }
                    R.id.zoomToLocation -> {
                        alertsViewModel.zoomToLocationClicked(alertsManager.alerts.value!![position])
                    }
                    R.id.alertAccepted -> {
                        alertsViewModel.acceptAlertClicked(alertsManager.alerts.value!![position])
                    }
                }
            }
        }
    }

    private fun setFragmentContent() {
        if (alertsViewModel.getAlerts().value!!.isEmpty()) {
            binding.alertsList.visibility = View.GONE
            binding.noAlertsMessage.visibility = View.VISIBLE
        } else {
            binding.alertsList.visibility = View.VISIBLE
            binding.noAlertsMessage.visibility = View.GONE
        }
    }

    private fun setObservers() {
        alertsManager.isAlertAccepted.observe(this, Observer {
//            alertsViewModel.setAlertAccepted()
//            adapter.notifyDataSetChanged()
        })

        alertsManager.isAlertAdded.observe(this, Observer {
            //            alertsViewModel.addAlert()
        })

        alertsManager.deletedAlertPosition.observe(this, Observer {
            //            alertsViewModel.deleteAlert(it)
//            adapter.notifyItemRemoved(it)
//            adapter.notifyItemRangeChanged(it, alertsViewModel.getAlerts().value!!.count())
        })
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
