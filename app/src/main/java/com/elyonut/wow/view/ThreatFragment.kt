package com.elyonut.wow.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.analysis.ThreatAnalyzer
import com.elyonut.wow.databinding.FragmentThreatListBinding
import com.elyonut.wow.model.Threat

class ThreatFragment : Fragment() {

    private var columnCount = 1
    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var threatDataset: List<Threat>
    private lateinit var threatsRecyclerView: RecyclerView
    private lateinit var noBuildingsMessage: TextView
    private var layoutManager: RecyclerView.LayoutManager? = null
    private lateinit var binding: FragmentThreatListBinding
    private lateinit var threatAnalyzer: ThreatAnalyzer

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
        threatDataset = threatAnalyzer.currentThreats
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_threat_list, container, false)
        threatsRecyclerView = binding.threatList
        noBuildingsMessage = binding.noBuildingsMessage
        threatsRecyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        threatsRecyclerView.layoutManager = layoutManager
        threatsRecyclerView.itemAnimator = DefaultItemAnimator()
        threatsRecyclerView.adapter = ThreatRecyclerViewAdapter(threatDataset, listener, context!!)
        setFragmentContent()

        layoutManager = when {
            columnCount <= 1 -> LinearLayoutManager(context)
            else -> GridLayoutManager(context, columnCount)
        }

        return binding.root
    }

    private fun setFragmentContent() {
        if (threatDataset.isEmpty()) {
            threatsRecyclerView.visibility = View.GONE
            noBuildingsMessage.visibility = View.VISIBLE
        } else {
            threatsRecyclerView.visibility = View.VISIBLE
            noBuildingsMessage.visibility = View.GONE
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        threatAnalyzer = ThreatAnalyzer.getInstance(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
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
