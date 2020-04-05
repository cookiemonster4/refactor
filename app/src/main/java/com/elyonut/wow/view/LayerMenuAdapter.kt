package com.elyonut.wow.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.model.LayerModel

class LayerMenuAdapter(private val layersDataSet: Array<LayerModel>) :
    RecyclerView.Adapter<LayerMenuAdapter.LayerViewHolder>() {
    val layerSelected = MutableLiveData<LayerModel>()

    class LayerViewHolder(checkBoxView: View) : RecyclerView.ViewHolder(checkBoxView) {
        val checkBox: CheckBox = checkBoxView.findViewById(R.id.layerCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val checkBox = LayoutInflater.from(parent.context).inflate(R.layout.checkbox_row_item, parent, false)
        return LayerViewHolder(checkBox)
    }

    override fun onBindViewHolder(layerViewHolder: LayerViewHolder, position: Int) {
        layerViewHolder.checkBox.text = layersDataSet[position].name
        layerViewHolder.checkBox.tag = position
        layerViewHolder.checkBox.setOnClickListener { view -> checkboxClicked(view) }
    }

    private fun checkboxClicked(checkBox: View) {
        checkBox as CheckBox
        val position = checkBox.tag as Int
        layerSelected.value = layersDataSet[position]
    }

    override fun getItemCount() = layersDataSet.size
}