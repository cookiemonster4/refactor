package com.elyonut.wow.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.interfaces.OnClickInterface
import com.elyonut.wow.model.MapLayer

class MapLayersAdapter(
    var context: Context,
    var mapLayers: ArrayList<MapLayer>,
    onClickHandler: OnClickInterface
) : RecyclerView.Adapter<MapLayersAdapter.MapLayersViewHolder>() {

    var onClickInterface: OnClickInterface = onClickHandler
    var selectedItemIndex: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapLayersViewHolder {
        return MapLayersViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.map_layer_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mapLayers.count()
    }

    override fun onBindViewHolder(holder: MapLayersViewHolder, position: Int) {
        holder.mapName.text = mapLayers[position].name
        holder.mapTypeImage.setImageResource(mapLayers[position].image)

        if(selectedItemIndex == position){
            holder.frame.background = context.getDrawable(R.drawable.card_selected_edge)
        }
        else
        {
            holder.frame.background = context.getDrawable(R.drawable.card_edge)
        }
    }

    inner class MapLayersViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val mapTypeImage: ImageView = view.findViewById(R.id.map_type_image)
        val mapName: TextView = view.findViewById(R.id.map_type_name)
        val frame: FrameLayout = view.findViewById(R.id.map_item_frame)

        init{
            mapTypeImage.setOnClickListener(this)
            mapName.setOnClickListener(this)
            frame.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            selectedItemIndex = this.adapterPosition
            onClickInterface.setClick(p0!!, this.adapterPosition)
            notifyDataSetChanged()
        }
    }

}