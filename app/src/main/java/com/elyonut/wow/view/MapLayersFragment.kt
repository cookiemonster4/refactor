package com.elyonut.wow.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.elyonut.wow.adapter.MapLayersAdapter
import com.elyonut.wow.R
import android.view.Gravity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.elyonut.wow.databinding.FragmentMapLayersBinding
import com.elyonut.wow.model.MapLayer
import com.elyonut.wow.viewModel.MapLayersViewModel
import com.elyonut.wow.viewModel.SharedViewModel

class MapLayersFragment : DialogFragment() {
    private var mapLayersAdapter: MapLayersAdapter? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var mapLayersViewModel: MapLayersViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentMapLayersBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_map_layers, container, false
        )

        val application = requireNotNull(this.activity).application

        mapLayersViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(MapLayersViewModel::class.java)

        binding.mapLayersViewModel = mapLayersViewModel

        mapLayersAdapter =
            MapLayersAdapter(context!!, MapLayersAdapter.MapLayerClickListener { mapLayer ->
                onMapLayerClicked(mapLayer)
            })

        binding.mapLayersList.adapter = mapLayersAdapter

        mapLayersViewModel.mapLayers?.observe(viewLifecycleOwner, Observer {
            it?.let {
                mapLayersAdapter!!.submitList(it)
            }
        })

        binding.lifecycleOwner = this

        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

        val manager = GridLayoutManager(activity, 3)
        binding.mapLayersList.layoutManager = manager

        setDialogPosition()

        return binding.root
    }

    private fun onMapLayerClicked(mapLayer: MapLayer) {
        sharedViewModel.mapStyleURL.value = mapLayer.id

        mapLayersAdapter?.setClickedPosition(
            mapLayersViewModel.mapLayers?.value?.indexOf(
                mapLayer
            )!!
        )
    }

    private fun setDialogPosition() {
        val window = dialog!!.window
        window!!.setGravity(Gravity.TOP or Gravity.START)
        val params = window.attributes
        params.x = 300
        params.y = 100
        window.attributes = params
    }
}