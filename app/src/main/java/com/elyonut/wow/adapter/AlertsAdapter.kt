package com.elyonut.wow.adapter

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.databinding.AlertItemBinding
import com.elyonut.wow.model.AlertModel

class AlertsAdapter(
    var context: Context,
    private val clickListener: AlertClickListener
) : RecyclerView.Adapter<AlertsAdapter.AlertsViewHolder>() {

    var data = listOf<AlertModel>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = data.size

    class AlertsViewHolder private constructor(private val binding: AlertItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: AlertModel,
            context: Context,
            clickListener: AlertClickListener
        ) {
            binding.alert = item
            binding.clickListener = clickListener

            if (!item.isRead) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.cardView.setCardBackgroundColor(context.getColor(R.color.unreadMessage))
                }
            } else {
                binding.cardView.setCardBackgroundColor(Color.WHITE)
            }
        }

        companion object {
            fun from(parent: ViewGroup): AlertsViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding =
                    AlertItemBinding.inflate(layoutInflater, parent, false)
                return AlertsViewHolder(binding)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertsViewHolder {
        return AlertsViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: AlertsViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item, context, clickListener)
    }

    class AlertClickListener(
        val deleteClickListener: (alert: AlertModel) -> Unit,
        val zoomClickListener: (alert: AlertModel) -> Unit,
        val acceptClickListener: (alert: AlertModel) -> Unit
    ) {
        fun onDeleteClick(alert: AlertModel) = deleteClickListener(alert)
        fun onZoomClick(alert: AlertModel) = zoomClickListener(alert)
        fun onAcceptClick(alert: AlertModel) = acceptClickListener(alert)
    }
}

