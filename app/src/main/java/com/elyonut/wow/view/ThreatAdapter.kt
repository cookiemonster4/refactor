package com.elyonut.wow.view

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.databinding.ItemThreatBinding


import com.elyonut.wow.view.ThreatFragment.OnListFragmentInteractionListener
import com.elyonut.wow.model.Threat

class ThreatAdapter(
    private val mListener: OnListFragmentInteractionListener?,
    private val context: Context

) : ListAdapter<Threat, ThreatAdapter.ThreatItemViewHolder>(ThreatItemDiffCallback()) {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Threat
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreatItemViewHolder {
        return ThreatItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holderThreatItem: ThreatItemViewHolder, position: Int) {
        val item = getItem(position)
        holderThreatItem.bind(item)

        holderThreatItem.mIdView.text = if (item.name.isNotBlank()) {
            item.name
        } else {
            context.getString(R.string.empty_building_name)
        }

        holderThreatItem.mContentView.text = item.level.toString()
        holderThreatItem.mThreatLevel.background.setColorFilter(
            Threat.color(item),
            PorterDuff.Mode.MULTIPLY
        )
        holderThreatItem.mThreatDistance.text = String.format("%.3f", item.distanceMeters)

//        with(holderThreatItem.mView) {
//            tag = item
//            setOnClickListener(mOnClickListener)
//        }
    }

//    override fun getItemCount(): Int = mValues.size

    class ThreatItemViewHolder(private val binding: ItemThreatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Threat) {
            binding.threat = item
        }

        companion object {
            fun from(parent: ViewGroup): ThreatItemViewHolder {
                val binding =
                    ItemThreatBinding.inflate(LayoutInflater.from(parent.context), parent, false)

                return ThreatItemViewHolder(binding)
            }
        }

        val mIdView: TextView = binding.threatId
        val mContentView: TextView = binding.threatLevel
        val mThreatLevel: ImageView = binding.threatLevelColor
        val mThreatDistance: TextView = binding.threatDistance

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}

class ThreatItemDiffCallback : DiffUtil.ItemCallback<Threat>() {
    override fun areItemsTheSame(oldItem: Threat, newItem: Threat): Boolean {
        return oldItem.name == newItem.name // TODO threat ID???
    }

    override fun areContentsTheSame(oldItem: Threat, newItem: Threat): Boolean {
        return oldItem == newItem
    }

}