package com.elyonut.wow.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.databinding.MapLayerItemBinding
import com.elyonut.wow.interfaces.OnClickInterface
import com.elyonut.wow.model.MapLayer

class MapLayersAdapter(
    var context: Context,
    var mapLayers: ArrayList<MapLayer>,
    onClickHandler: OnClickInterface,
    private val clickListener: MapLayerClickListener
) : RecyclerView.Adapter<MapLayersAdapter.MapLayersViewHolder>() {

    var onClickInterface: OnClickInterface = onClickHandler
    var selectedItemIndex: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapLayersViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = DataBindingUtil.inflate<MapLayerItemBinding>(
            inflater,
            R.layout.map_layer_item,
            parent,
            false
        )
        return MapLayersViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mapLayers.count()
    }

    override fun onBindViewHolder(holder: MapLayersViewHolder, position: Int) {
        holder.view.mapLayer = mapLayers[position]
        holder.view.listener = clickListener

//        holder.mapName.text = mapLayers[position].name
//        holder.mapTypeImage.setImageResource(mapLayers[position].image)
//
//        if(selectedItemIndex == position){
//            holder.frame.background = context.getDrawable(R.drawable.card_selected_edge)
//        }
//        else
//        {
//            holder.frame.background = context.getDrawable(R.drawable.card_edge)
//        }


        if(selectedItemIndex == position){
            holder.view.mapItemFrame.background = context.getDrawable(R.drawable.card_selected_edge)
        }
        else
        {
            holder.view.mapItemFrame.background = context.getDrawable(R.drawable.card_edge)
        }
    }

    class MapLayersViewHolder(var view: MapLayerItemBinding) : RecyclerView.ViewHolder(view.root)

    class MapLayerClickListener(val clickListener: (mapLayerId: String) -> Unit) {
        fun onClick(mapLayer: MapLayer) = clickListener(mapLayer.id)
    }

//    override fun onMapLayerClicked(view: View) {
////        selectedItemIndex =
////        onClickInterface.setClick(view, this.adapterPosition)
////        notifyDataSetChanged()
//    }
//        val mapTypeImage: ImageView = view.findViewById(R.id.map_type_image)
//        val mapName: TextView = view.findViewById(R.id.map_type_name)
//        val frame: FrameLayout = view.findViewById(R.id.map_item_frame)
//
//        init{
//            mapTypeImage.setOnClickListener(this)
//            mapName.setOnClickListener(this)
//            frame.setOnClickListener(this)
//        }
//
//        override fun onClick(p0: View?) {
////            selectedItemIndex = this.adapterPosition
////            onClickInterface.setClick(p0!!, this.adapterPosition)
////            notifyDataSetChanged()
//        }
}