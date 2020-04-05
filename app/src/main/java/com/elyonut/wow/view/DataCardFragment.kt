package com.elyonut.wow.view

import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.utilities.OnSwipeTouchListener
import com.elyonut.wow.R
import com.elyonut.wow.model.Threat
import com.elyonut.wow.utilities.BuildingTypeMapping
import com.elyonut.wow.viewModel.DataCardViewModel
import com.elyonut.wow.viewModel.SharedViewModel
import kotlinx.android.synthetic.main.fragment_data_card.view.*

// const variables
private const val CARD_SIZE_RELATION_TO_SCREEN = 0.33
private const val EXPENDED_CARD_SIZE_RELATION_TO_SCREEN = 0.5

class DataCardFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private lateinit var dataCardViewModel: DataCardViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_card, container, false)
        dataCardViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
                .create(DataCardViewModel::class.java)
        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!


        view.buildingDataCard.layoutParams = dataCardViewModel.getRelativeLayoutParams(CARD_SIZE_RELATION_TO_SCREEN)
        initObservers(view)
        initReadMoreButton(view)
        initClosingCard(view)

        val threat: Threat = arguments!!.getParcelable("threat")
        initThreatInfo(view, threat)

        return view
    }

    private fun initThreatInfo(view: View, threat: Threat){
        val feature = threat.feature.properties()
        val featureName = feature?.get(getString(R.string.nameStr))?.asString

        view.dataType.text = if (featureName.isNullOrBlank()) {
            getString(R.string.empty_building_name)
        } else {
            featureName
        }

        view.knowledgeType.text = getString(R.string.knowledgeType_title) + ": " + feature?.get(getString(R.string.knowledgeType))?.asString
        view.eAmount.text = getString(R.string.eAmount_title) + ": " + feature?.get(getString(R.string.eAmount))?.asString
        view.type.text = getString(R.string.type_title)+ ": " + feature?.get(getString(R.string.type))?.asString
        view.range.text = getString(R.string.range_title) + ": "+ feature?.get(getString(R.string.range))?.asString

        val builder = StringBuilder()
        builder.append(String.format("גובה (מטרים): %.3f\n", threat.height))
        builder.append(String.format("מרחק (מטרים): %.3f\n", threat.distanceMeters))
        builder.append(String.format("אזימוט: %.3f\n", threat.azimuth))
        builder.append(String.format("האם בקו ראיה: %s", if (threat.isLos) "כן" else "לא"))
        view.moreContent.text = builder.toString()

        view.buildingStateColor.background.setColorFilter(Threat.color(threat), PorterDuff.Mode.MULTIPLY)
        val featureType = feature?.get(getString(R.string.type))?.asString
        view.dataTypeImage.setImageResource(BuildingTypeMapping.mapping[featureType]!!)
    }

    private fun initObservers(view: View) {
        dataCardViewModel.isReadMoreButtonClicked.observe(viewLifecycleOwner, Observer<Boolean> { extendDataCard(view) })
        dataCardViewModel.shouldCloseCard.observe(viewLifecycleOwner, Observer<Boolean> {
            closeCard()
        })
    }

    private fun extendDataCard(view: View) {
        if (dataCardViewModel.isReadMoreButtonClicked.value!!) {
            view.buildingDataCard.layoutParams =
                dataCardViewModel.getRelativeLayoutParams(EXPENDED_CARD_SIZE_RELATION_TO_SCREEN)
            view.moreContent.visibility = View.VISIBLE
            view.readMore.text = getString(R.string.read_less_hebrew)
        } else {
            view.buildingDataCard.layoutParams = dataCardViewModel.getRelativeLayoutParams(CARD_SIZE_RELATION_TO_SCREEN)
            view.moreContent.visibility = View.GONE
            view.readMore.text = getString(R.string.read_more_hebrew)
        }
    }

    private fun closeCard() {
        activity?.supportFragmentManager?.beginTransaction()?.remove(this@DataCardFragment)?.commit()
    }

    private fun initReadMoreButton(view: View) {
        view.readMore.setOnClickListener {
            dataCardViewModel.readMoreButtonClicked(view.moreContent)
        }
    }

    private fun initClosingCard(view: View) {
        initCloseCardByClickOnMap(view)
        initCloseCardButton(view)
        initFlingCloseListener(view)
    }

    private fun initCloseCardByClickOnMap(view: View) {
        view.setOnClickListener {
            dataCardViewModel.close()
        }
    }

    private fun initCloseCardButton(view: View) {
        view.closeButton?.setOnClickListener {
            onCloseCard()
        }
    }

    private fun initFlingCloseListener(view: View) {
        view.buildingDataCard.setOnTouchListener(object : OnSwipeTouchListener(this@DataCardFragment.context!!) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                onCloseCard()
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                onCloseCard()
            }
        })
    }

    private fun onCloseCard(){
        dataCardViewModel.close()
        sharedViewModel.shoulRemoveSelectedBuildingLayer.value = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnMapFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onDataCardFragmentInteraction()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DataCardFragment().apply {
            }
    }
}
