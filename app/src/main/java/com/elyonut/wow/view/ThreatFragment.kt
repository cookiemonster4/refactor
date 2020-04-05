package com.elyonut.wow.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.model.Threat

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ThreatFragment.OnListFragmentInteractionListener] interface.
 */
class ThreatFragment : Fragment() {

    // TODO: Customize parameters
    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null

    private lateinit var threatDataset: ArrayList<Threat>

    private lateinit var threatsRecyclerView: RecyclerView
    private lateinit var noBuildingsMessage: TextView
    private var layoutManager: RecyclerView.LayoutManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        threatDataset = arguments!!.getParcelableArrayList("threats")
        val view = inflater.inflate(R.layout.fragment_threat_list, container, false)

        threatsRecyclerView = view.findViewById(R.id.threat_list)
        noBuildingsMessage = view.findViewById(R.id.no_buildings_message)

        threatsRecyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        threatsRecyclerView.layoutManager = layoutManager
        threatsRecyclerView.itemAnimator = DefaultItemAnimator()

        layoutManager = when {
            columnCount <= 1 -> LinearLayoutManager(context)
            else -> GridLayoutManager(context, columnCount)
        }

        threatsRecyclerView.adapter = ThreatRecyclerViewAdapter(threatDataset, listener, context!!)
        setFragmentContent()

        return view
    }

    private fun setFragmentContent() {
        if (threatDataset.isEmpty()) {
            threatsRecyclerView.visibility = View.GONE
            noBuildingsMessage.visibility = View.VISIBLE
        }
        else {
            threatsRecyclerView.visibility = View.VISIBLE
            noBuildingsMessage.visibility = View.GONE
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: Threat?)
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            ThreatFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}
