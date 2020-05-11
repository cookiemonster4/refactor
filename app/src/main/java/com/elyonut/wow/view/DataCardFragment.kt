package com.elyonut.wow.view

import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.elyonut.wow.utilities.OnSwipeTouchListener
import com.elyonut.wow.R
import com.elyonut.wow.databinding.FragmentDataCardBinding
import com.elyonut.wow.model.Threat
import com.elyonut.wow.viewModel.DataCardViewModel
import com.elyonut.wow.viewModel.SharedViewModel

// const variables
private const val CARD_SIZE_RELATION_TO_SCREEN = 0.33
private const val EXPENDED_CARD_SIZE_RELATION_TO_SCREEN = 0.5

class DataCardFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private lateinit var dataCardViewModel: DataCardViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var binding: FragmentDataCardBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_data_card, container, false)

        dataCardViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
                .create(DataCardViewModel::class.java)
        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

//        val threat: Threat = arguments!!.getParcelable("threat")!!
        val threat: Threat = DataCardFragmentArgs.fromBundle(arguments!!).building

        binding.threat = threat
        binding.dataCardViewModel = dataCardViewModel
        binding.buildingDataCard.layoutParams =
            dataCardViewModel.getRelativeLayoutParams(CARD_SIZE_RELATION_TO_SCREEN)

        initObservers()
        initClosingCard()
        initBuildingStateColor(threat)

        return binding.root
    }

    private fun initBuildingStateColor(threat: Threat) {
        binding.buildingStateColor.background.setColorFilter(
            Threat.color(threat),
            PorterDuff.Mode.MULTIPLY
        )
    }

    private fun initObservers() {
        dataCardViewModel.isReadMoreButtonClicked.observe(
            viewLifecycleOwner,
            Observer { extendDataCard() })
        dataCardViewModel.shouldCloseCard.observe(viewLifecycleOwner, Observer {
            closeCard()
        })
        dataCardViewModel.navigateToMapFragment.observe(viewLifecycleOwner, Observer {
            if (it) {
                navigateToMapFragment()
            }
        })
    }

    private fun extendDataCard() {
        if (dataCardViewModel.isReadMoreButtonClicked.value!!) {
            binding.buildingDataCard.layoutParams =
                dataCardViewModel.getRelativeLayoutParams(EXPENDED_CARD_SIZE_RELATION_TO_SCREEN)
            binding.moreContent.visibility = View.VISIBLE
            binding.readMore.text = getString(R.string.read_less_hebrew)
        } else {
            binding.buildingDataCard.layoutParams =
                dataCardViewModel.getRelativeLayoutParams(CARD_SIZE_RELATION_TO_SCREEN)
            binding.moreContent.visibility = View.GONE
            binding.readMore.text = getString(R.string.read_more_hebrew)
        }
    }

    private fun closeCard() {
        activity?.supportFragmentManager?.beginTransaction()?.remove(this@DataCardFragment)
            ?.commit()
    }

    private fun navigateToMapFragment() {
        sharedViewModel.shouldRemoveSelectedBuildingLayer(true)
        this.findNavController()
            .navigate(DataCardFragmentDirections.actionDataCardFragmentToMapFragment())
        dataCardViewModel.doneNavigating()
    }

    private fun initClosingCard() {
        initCloseCardButton()
        initFlingCloseListener()
    }

    private fun initCloseCardButton() {
        binding.closeButton.setOnClickListener {
            dataCardViewModel.onCloseClicked()
//            onCloseCard()
        }
    }

    private fun initFlingCloseListener() {
        binding.buildingDataCard.setOnTouchListener(object :
            OnSwipeTouchListener(this@DataCardFragment.context!!) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                dataCardViewModel.onSwiftLeftOrRight()
//                onCloseCard()
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                dataCardViewModel.onSwiftLeftOrRight()
//                onCloseCard()
            }
        })

    }

    private fun onCloseCard() {
        dataCardViewModel.close()
        sharedViewModel.shouldRemoveSelectedBuildingLayer(true)
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
