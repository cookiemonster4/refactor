package com.elyonut.wow.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.databinding.MapLayerItemBinding
import com.elyonut.wow.model.MapLayer

class MapLayersAdapter(
    var context: Context,
    private val clickListener: MapLayerClickListener
) : ListAdapter<MapLayer, MapLayersAdapter.MapLayersViewHolder>(MapLayerDiffCallback()) {

    var selectedItemIndex: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapLayersViewHolder {
        return MapLayersViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: MapLayersViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, clickListener, position, selectedItemIndex, context)
    }

    class MapLayersViewHolder private constructor(private val binding: MapLayerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: MapLayer,
            clickListener: MapLayerClickListener,
            position: Int,
            selectedItemIndex: Int,
            context: Context
        ) {
            binding.mapLayer = item
            binding.clickListener = clickListener

            if (selectedItemIndex == position) {
                binding.mapItemFrame.background = context.getDrawable(R.drawable.card_selected_edge)
            } else {
                binding.mapItemFrame.background = context.getDrawable(R.drawable.card_edge)
            }
        }

        companion object {
            fun from(parent: ViewGroup): MapLayersViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding = MapLayerItemBinding.inflate(inflater, parent, false)
                return MapLayersViewHolder(binding)
            }
        }
    }

    fun setClickedPosition(position: Int) {
        selectedItemIndex = position
        notifyDataSetChanged()
    }

    class MapLayerClickListener(val clickListener: (mapLayer: MapLayer) -> Unit) {
        fun onClick(mapLayer: MapLayer) = clickListener(mapLayer)
    }
}

class MapLayerDiffCallback : DiffUtil.ItemCallback<MapLayer>() {
    override fun areItemsTheSame(oldItem: MapLayer, newItem: MapLayer): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MapLayer, newItem: MapLayer): Boolean {
        return oldItem == newItem
    }
}