package com.elyonut.wow.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.utilities.NumericFilterTypes
import com.elyonut.wow.R
import com.elyonut.wow.viewModel.FilterViewModel
import com.elyonut.wow.viewModel.SharedViewModel
import kotlinx.android.synthetic.main.fragment_filter.view.*

class FilterFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var fragmentContext: OnFragmentInteractionListener? = null
    private lateinit var filterViewModel: FilterViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter, container, false)

        filterViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
                .create(FilterViewModel::class.java)
        sharedViewModel =
            ViewModelProviders.of(activity!!)[SharedViewModel::class.java]

        setObservers(view)
        initOkButton(view)
        initCancelButton(view)
        initSpinners(view)
        return view
    }

    private fun setObservers(view: View) {
        filterViewModel.numericTypeChosen.observe(this, Observer { numericTypeChosen(view, it) })
        filterViewModel.shouldApplyFilter.observe(this, Observer { applyFilter(it, view) })
        filterViewModel.chosenLayerId.observe(this, Observer<String> {
            sharedViewModel.layerToFilterId = it
            initPropertiesSpinner(view, it)
        })
        filterViewModel.chosenProperty.observe(this, Observer<String> {
            filterViewModel.initOptionsList(it)
            sharedViewModel.chosenPropertyId = it
        })
        filterViewModel.isStringType.observe(this, Observer {
            propertyTypeChosen(view, it)
        })
    }

    private fun numericTypeChosen(view: View, numericFilterType: NumericFilterTypes) {
        when (numericFilterType) {
            NumericFilterTypes.RANGE -> {
                changeViewsVisibility(view.minRangeOptions, true)
                changeViewsVisibility(view.maxRangeOptions, true)
                changeViewsVisibility(view.specificOption, false)
            }
            NumericFilterTypes.GREATER -> {
                changeViewsVisibility(view.minRangeOptions, true)
                changeViewsVisibility(view.maxRangeOptions, false)
                changeViewsVisibility(view.specificOption, false)
            }
            NumericFilterTypes.LOWER -> {
                changeViewsVisibility(view.minRangeOptions, false)
                changeViewsVisibility(view.maxRangeOptions, true)
                changeViewsVisibility(view.specificOption, false)
            }
            NumericFilterTypes.SPECIFIC -> {
                changeViewsVisibility(view.minRangeOptions, false)
                changeViewsVisibility(view.maxRangeOptions, false)
                changeViewsVisibility(view.specificOption, true)
            }
        }
    }

    private fun propertyTypeChosen(view: View, isStringPropertyChosen: Boolean) {
        if (isStringPropertyChosen) {
            initStringPropertiesSpinner(view)
            changeViewsVisibility(view.stringOptions, isStringPropertyChosen)
            changeViewsVisibility(view.numberOptions, !isStringPropertyChosen)
        } else {
            changeViewsVisibility(view.stringOptions, isStringPropertyChosen)
            changeViewsVisibility(view.numberOptions, !isStringPropertyChosen)
        }

        sharedViewModel.isStringType = isStringPropertyChosen
        sharedViewModel.numericType = filterViewModel.numericTypeChosen.value!!
    }

    private fun initPropertiesSpinner(view: View, layerId: String) {
        val propertySpinner = view.propertiesSpinner
        val propertiesList = filterViewModel.initPropertiesList(layerId)
        propertySpinner.onItemSelectedListener = this

        if (propertiesList != null) {
            val adapter = ArrayAdapter(
                activity!!.application,
                android.R.layout.simple_spinner_item,
                propertiesList.toMutableList()
            )

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            propertySpinner.adapter = adapter
        } // TODO what else?? if null?
    }

    private fun changeViewsVisibility(view: View, shouldShowView: Boolean) {
        if (shouldShowView) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

    private fun initStringPropertiesSpinner(view: View) {
        val stringPropertySpinner = view.stringPropertySpinner
        val allPropertiesValues =
            filterViewModel.initStringPropertyOptions(filterViewModel.chosenProperty.value!!)

        if (allPropertiesValues != null) {
            val stringAdapter = ArrayAdapter(
                activity!!.application,
                android.R.layout.simple_spinner_item,
                allPropertiesValues.toMutableList()
            )

            stringAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            stringPropertySpinner.adapter = stringAdapter
            stringPropertySpinner.onItemSelectedListener = this
        } // TODO what else?? if null?
    }

    private fun applyFilter(shouldApplyFilter: Boolean, view: View) {
        if (shouldApplyFilter) {
            if (!filterViewModel.isStringType.value!!) {
                sharedViewModel.minValue = view.minNumericPicker.value
                sharedViewModel.specificValue = view.specificNumericPicker.value / 10.0

                if (view.maxValue.text.toString() != "") {
                    sharedViewModel.maxValue = view.maxValue.text.toString().toInt()
                }
            } else {
                sharedViewModel.chosenPropertyValue =
                    view.stringPropertySpinner.selectedItem.toString()
            }
        }

        sharedViewModel.shouldApplyFilter.value = shouldApplyFilter
    }

    private fun initOkButton(view: View) {
        val okButton: View = view.ok_button

        okButton.setOnClickListener {
            filterViewModel.applyFilterButtonClicked(true)
        }
    }

    private fun initCancelButton(view: View) {
        val cancelButton: View = view.remove_button

        cancelButton.setOnClickListener {
            filterViewModel.applyFilterButtonClicked(false)
        }
    }

    private fun initSpinners(view: View) {
        initLayersSpinner(view)
        initNumberPropertiesSpinner(view)
        initNumericPickers(view)
    }

    private fun initLayersSpinner(view: View) {
        val layerSpinner = view.layersSpinner
        val layersIdsList = filterViewModel.layersIdsList

        val layerAdapter = ArrayAdapter(
            activity!!.application,
            android.R.layout.simple_spinner_item,
            layersIdsList.toMutableList()
        )

        layerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        layerSpinner.adapter = layerAdapter
        layerSpinner.onItemSelectedListener = this
    }

    private fun initNumberPropertiesSpinner(view: View) {
        val numberSpinner = view.numberPropertySpinner
        val numberAdapter = ArrayAdapter(
            activity!!.application,
            android.R.layout.simple_spinner_item,
            filterViewModel.numberFilterOptions
        )

        numberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        numberSpinner.adapter = numberAdapter
        numberSpinner.onItemSelectedListener = this
    }

    private fun initNumericPickers(view: View) {
        val nums = arrayOf(0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1)

        view.minNumericPicker.minValue = 0
        view.minNumericPicker.maxValue = nums.size - 1
        view.specificNumericPicker.minValue = 0
        view.specificNumericPicker.maxValue = nums.size - 1
        view.minNumericPicker.displayedValues = nums.map { it.toString() }.toTypedArray()
        view.specificNumericPicker.displayedValues = nums.map { it.toString() }.toTypedArray()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.layersSpinner -> filterViewModel.onLayerItemSelected(position)
            R.id.propertiesSpinner -> filterViewModel.onPropertyItemSelected(position)
            R.id.numberPropertySpinner -> filterViewModel.onNumberItemSelected(position)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            fragmentContext = context
        } else {
            throw RuntimeException("$context must implement OnMapFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        fragmentContext = null
    }

    interface OnFragmentInteractionListener {
        fun onFilterFragmentInteraction()
    }

    companion object {
        @JvmStatic
        fun newInstance() = FilterFragment()
    }
}
