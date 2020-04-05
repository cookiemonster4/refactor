package com.elyonut.wow.view

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R


import com.elyonut.wow.view.ThreatFragment.OnListFragmentInteractionListener
import com.elyonut.wow.model.Threat

import kotlinx.android.synthetic.main.fragment_threat.view.*

/**
 * [RecyclerView.Adapter] that can display a [Threat] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class ThreatRecyclerViewAdapter(
    private val mValues: ArrayList<Threat>,
    private val mListener: OnListFragmentInteractionListener?,
    private val context: Context
) : RecyclerView.Adapter<ThreatRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Threat
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_threat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]

        holder.mIdView.text = if (item.name.isNotBlank()) {
            item.name
        } else {
            context.getString(R.string.empty_building_name)
        }

        holder.mContentView.text = item.level.toString()
        holder.mThreatLevel.background.setColorFilter(Threat.color(item), PorterDuff.Mode.MULTIPLY)
        holder.mThreatDistance.text = String.format("%.3f", item.distanceMeters)

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.threat_id
        val mContentView: TextView = mView.threat_level
        val mThreatLevel: ImageView = mView.threat_level_color
        val mThreatDistance: TextView = mView.threat_distance

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
