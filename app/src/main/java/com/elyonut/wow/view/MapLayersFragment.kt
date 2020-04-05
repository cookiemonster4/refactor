package com.elyonut.wow.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.adapter.MapLayersAdapter
import com.elyonut.wow.R
import android.view.Gravity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.elyonut.wow.MapsManager
import com.elyonut.wow.interfaces.OnClickInterface
import com.elyonut.wow.model.MapLayer
import com.elyonut.wow.viewModel.SharedViewModel
import kotlinx.android.synthetic.main.alert_item.view.*
import kotlinx.android.synthetic.main.map_layer_item.view.*

class MapLayersFragment: DialogFragment() {
    private lateinit var mapLayersRecyclerView: RecyclerView
    private var mapLayersAdapter: MapLayersAdapter? = null
    private lateinit var onClickHandler: OnClickInterface
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var mapsList: ArrayList<MapLayer>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map_layers, container, false)
        mapLayersRecyclerView = view.findViewById(R.id.map_layers_list)
        mapLayersRecyclerView.layoutManager = GridLayoutManager(context, 3)
        mapLayersRecyclerView.itemAnimator = DefaultItemAnimator()

        mapsList = MapsManager(context!!).maps!!

        initClickInterface()

        mapLayersAdapter =
            MapLayersAdapter(context!!, mapsList, onClickHandler)
        mapLayersRecyclerView.adapter = mapLayersAdapter
        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

        val window = dialog!!.window
        window!!.setGravity(Gravity.TOP or Gravity.LEFT)
        val params = window.attributes
        params.x = 300
        params.y = 100
        window.attributes = params

        return view
    }

    private fun initClickInterface() {
        onClickHandler = object : OnClickInterface {
            override fun setClick(view: View, position: Int) {
                when (view.id) {
                    R.id.map_type_image -> {
                        sharedViewModel.mapStyleURL.value = mapsList[position].id
                    }
                }
            }
        }
    }
}