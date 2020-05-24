package com.elyonut.wow.view


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.R
import com.elyonut.wow.viewModel.CoverageSettingsViewModel
import com.elyonut.wow.viewModel.SharedViewModel
import kotlinx.android.synthetic.main.fragment_coverage_settings.view.*


class CoverageSettingsFragment : Fragment() {

    companion object {
        fun newInstance() = CoverageSettingsFragment()
    }

    private lateinit var viewModel: CoverageSettingsViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_coverage_settings, container, false)
        viewModel = ViewModelProviders.of(this).get(CoverageSettingsViewModel::class.java)
        sharedViewModel =
            ViewModelProviders.of(activity!!)[SharedViewModel::class.java]

        initTextValues(view)
        initSaveButton(view)
        initCloseButton(view)
        initCloseButton(view)

        return view
    }

    private fun initTextValues(view: View) {
        val coverageRange = view.coverageRangeValue
        coverageRange.setText(
            sharedViewModel.coverageRangeMeters.toString(),
            TextView.BufferType.EDITABLE
        )

        val resolution = view.pointResolutionValue
        resolution.setText(
            sharedViewModel.coverageResolutionMeters.toString(),
            TextView.BufferType.EDITABLE
        )

        val height = view.searchHeightValue

        if (sharedViewModel.coverageSearchHeightMeters != Constants.DEFAULT_COVERAGE_HEIGHT_METERS) {
            height.setText(
                sharedViewModel.coverageSearchHeightMeters.toString(),
                TextView.BufferType.EDITABLE
            )
        }

        val heightCheck = view.searchHeightCheckBox
        heightCheck.isChecked = sharedViewModel.coverageSearchHeightMetersChecked.value ?: false
    }

    private fun initSaveButton(view: View) {
        val saveButton: View = view.searchRadiusOptionsSaveButton

        saveButton.setOnClickListener {
            var height = Constants.DEFAULT_COVERAGE_HEIGHT_METERS
            if (view.searchHeightValue.text.isNotEmpty()) {
                height = view.searchHeightValue.text.toString().toDouble()
            }

            sharedViewModel.applySaveCoverageSettingsButtonClicked(
                view.coverageRangeValue.text.toString().toDouble(),
                view.pointResolutionValue.text.toString().toDouble(),
                height,
                view.searchHeightCheckBox.isChecked
            )
            closeSettings()
        }
    }

    private fun initCloseButton(view: View) {
        val closeButton: View = view.searchRadiusOptionsCloseButton

        closeButton.setOnClickListener {
            closeSettings()
        }
    }

    private fun closeSettings() {
        view?.clearFocus() // to close keyboard
        activity?.supportFragmentManager?.beginTransaction()?.remove(this@CoverageSettingsFragment)
            ?.commit()
    }
}
