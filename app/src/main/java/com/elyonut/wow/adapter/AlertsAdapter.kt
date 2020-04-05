package com.elyonut.wow.adapter

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.interfaces.OnClickInterface
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.view.AlertsFragment
import com.squareup.picasso.Picasso
import java.util.*

class AlertsAdapter(
    var context: Context,
    alerts: LinkedList<AlertModel>,
    onClickHandler: OnClickInterface
) : RecyclerView.Adapter<AlertsAdapter.AlertsViewHolder>() {

    var alerts = LinkedList<AlertModel>()
    var onClickInterface: OnClickInterface

    init {
        this.alerts = alerts
        this.onClickInterface = onClickHandler
    }

    inner class AlertsViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        init {
            val deleteAlert: AppCompatImageButton = view.findViewById(R.id.deleteAlert)
            val zoomLocationButton: TextView = view.findViewById(R.id.zoomToLocation)
            val alertAcceptedButton: TextView = view.findViewById(R.id.alertAccepted)

            deleteAlert.setOnClickListener(this)
            zoomLocationButton.setOnClickListener(this)
            alertAcceptedButton.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onClickInterface.setClick(p0!!, this.adapterPosition)
        }

        val alertMessage: TextView? = view.findViewById(R.id.alert_message)
        val alertImage: ImageView? = view.findViewById(R.id.alert_image)
        val currentTime: TextView? = view.findViewById(R.id.current_time)
        val cardView: CardView? = view.findViewById(R.id.card_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertsViewHolder {
        return AlertsViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.alert_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return alerts.count()
    }

    override fun onBindViewHolder(holder: AlertsViewHolder, position: Int) {
        holder.alertMessage?.text = alerts[position].message
        Picasso.with(context).load(alerts[position].image).into(holder.alertImage)
        holder.currentTime?.text = alerts[position].time

        if (!alerts[position].isRead) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.cardView?.setCardBackgroundColor(context.getColor(R.color.unreadMessage))
            }
        } else {
            holder.cardView?.setCardBackgroundColor(Color.WHITE)
        }
    }
}
