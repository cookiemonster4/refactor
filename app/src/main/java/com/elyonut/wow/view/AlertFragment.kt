package com.elyonut.wow.view

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.viewModel.AlertViewModelFactory
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.R
import com.elyonut.wow.adapter.AlertsAdapter
import com.elyonut.wow.databinding.FragmentAlertBinding
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.viewModel.AlertViewModel
import com.elyonut.wow.viewModel.SharedViewModel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.alert_item.view.*


class AlertFragment(private var alert: AlertModel) : Fragment() {
    private var listener: OnAlertFragmentInteractionListener? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var alertViewModel: AlertViewModel
    private lateinit var alertsManager: AlertsManager

    private lateinit var binding: FragmentAlertBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_alert,
            container,
            false
        )

        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

        alertsManager = sharedViewModel.alertsManager

        alertViewModel = ViewModelProviders.of(
            this,
            AlertViewModelFactory(activity!!.application, alertsManager)
        ).get(AlertViewModel::class.java)

        binding.lifecycleOwner = viewLifecycleOwner

        binding.alert = alert
        binding.alertView.deleteAlert.visibility = View.GONE

        binding.clickListener =
            AlertsAdapter.AlertClickListener({}, { onZoomClick(it) }, { onAcceptClick(it) })

        setObservers()

        return binding.root
    }

    private fun setObservers() {
        alertsManager.shouldRemoveAlert.observe(this, Observer {
            if (it) {
                removeAlert()
            }
        })
    }

    private fun onZoomClick(alert: AlertModel) {
        alertViewModel.zoomToLocationClicked(alert)
    }

    private fun onAcceptClick(alert: AlertModel) {
        alertViewModel.acceptAlertClicked(alert)
    }

    private fun removeAlert() {
        activity?.supportFragmentManager?.beginTransaction()?.remove(this@AlertFragment)
            ?.setTransition(
                FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
            )?.commit()

        alertsManager.shouldRemoveAlert.postValue(false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnAlertFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnAlertFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnAlertFragmentInteractionListener {
        fun onAlertFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AlertFragment.
         */
        @JvmStatic
        fun newInstance(alert: AlertModel) =
            AlertFragment(alert).apply {
            }
    }
}
