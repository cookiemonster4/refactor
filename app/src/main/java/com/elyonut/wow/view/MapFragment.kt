package com.elyonut.wow.view


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.*
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.model.RiskStatus
import com.elyonut.wow.model.Threat
import com.elyonut.wow.utilities.BuildingTypeMapping
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.utilities.Maps
import com.elyonut.wow.viewModel.MapViewModel
import com.elyonut.wow.viewModel.SharedViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.synthetic.main.fragment_map.view.*
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList

private const val RECORD_REQUEST_CODE = 101

class MapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener {
    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var mapViewModel: MapViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private var listenerMap: OnMapFragmentInteractionListener? = null

    private lateinit var broadcastReceiver: BroadcastReceiver
    private var zoomFilter = IntentFilter(Constants.ZOOM_LOCATION_ACTION)
    private var alertAcceptedFilter = IntentFilter(Constants.ALERT_ACCEPTED_ACTION)
    private lateinit var alertsManager: AlertsManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Mapbox.getInstance(listenerMap as Context, Maps.MAPBOX_ACCESS_TOKEN)
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
                .create(MapViewModel::class.java)
        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

        alertsManager = sharedViewModel.alertsManager
        mapView = view.findViewById(R.id.mainMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        initArea()
        setObservers(view)
        initFocusOnMyLocationButton(view)
        initBroadcastReceiver()
        initMapLayersButton(view)

        return view
    }

    private fun initBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Constants.ZOOM_LOCATION_ACTION -> {
                        mapViewModel.setZoomLocation(intent.getStringExtra("threatID"))
                        alertsManager.updateMessageAccepted(intent.getStringExtra("threatID"))
                        (context as FragmentActivity).supportFragmentManager.popBackStack()

                    }
                    Constants.ALERT_ACCEPTED_ACTION -> {
                        alertsManager.updateMessageAccepted(intent.getStringExtra("threatID"))
                    }
                }

                alertsManager.shouldPopAlert.value = true
            }
        }
    }

    private fun initArea() {
        if (sharedViewModel.areaOfInterest != null) {
            mapViewModel.areaOfInterest.value = sharedViewModel.areaOfInterest

            val polygonPoints = ArrayList<Point>()
            sharedViewModel.areaOfInterest!!.coordinates().forEach { it ->
                it.forEach {
                    polygonPoints.add(it)
                }
            }

            mapViewModel.lineLayerPointList = polygonPoints
        }
    }

    private fun setObservers(view: View) {
        mapViewModel.isAlertVisible.observe(this, Observer<Boolean> { showAlertDialog() })
        mapViewModel.noPermissionsToast.observe(this, Observer<Toast> { showToast() })
        mapViewModel.areaOfInterest.observe(this, Observer {
            sharedViewModel.areaOfInterest = it
        })
        mapViewModel.isPermissionRequestNeeded.observe(this, Observer<Boolean> {
            if (it != null && it) {
                requestPermissions()
            }
        })
        mapViewModel.selectedBuildingId.observe(
            this,
            Observer<String> { showDescriptionFragment() }
        )
        mapViewModel.isLocationAdapterInitialized.observe(
            this,
            Observer<Boolean> {
                observeRiskStatus(it)
                if (it) {
                    initLocationObserver()
                }
            })

        mapViewModel.threatAlerts.observe(this, Observer {
            sendNotification(it)
        })

        sharedViewModel.selectedLayerId.observe(this, Observer<String> {
            it?.let { mapViewModel.layerSelected(it) }
        })

        sharedViewModel.selectedExperimentalOption.observe(
            this,
            Observer<Int> { applyExperimentalOption(it) }
        )

        sharedViewModel.shouldOpenThreatsFragment.observe(this, Observer {
            if (it) {
                openThreatListFragment()
            }
        })

        sharedViewModel.selectedThreatItem.observe(
            this,
            Observer<Threat> { onListFragmentInteraction(it) }
        )

        sharedViewModel.shouldApplyFilter.observe(this,
            Observer<Boolean> { filter(it) }
        )

        sharedViewModel.shouldDefineArea.observe(this, Observer {
            if (it) {
                enableAreaSelection(view, it)
            }
        })

        sharedViewModel.chosenTypeToFilter.observe(this, Observer<Pair<String, Boolean>> {
            mapViewModel.filterLayerByType(it)

        })

        sharedViewModel.isSelectAllChecked.observe(this, Observer {
            mapViewModel.filterLayerByAllTypes(it)
        })

        mapViewModel.isFocusedOnLocation.observe(this, Observer {
            setCurrentLocationButtonIcon(it, view)
        })

        alertsManager.shouldPopAlert.observe(this, Observer { shouldPop ->
            if (shouldPop && alertsManager.alerts.value!!.count { !it.isRead } > 0) {
                alertsManager.shouldPopAlert.value = false
                setAlertPopUp(alertsManager.alerts.value?.findLast { !it.isRead }!!)
            }
        })

        sharedViewModel.shoulRemoveSelectedBuildingLayer.observe(
            this,
            Observer { shouldRemoveLayer ->
                if (shouldRemoveLayer) {
                    val selectedBuildingSource =
                        map.style?.getSourceAs<GeoJsonSource>(Constants.SELECTED_BUILDING_SOURCE_ID)
                    selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(ArrayList()))
                }
            })

        sharedViewModel.mapStyleURL.observe(this, Observer {
            mapViewModel.setMapStyle(it)
        })
    }

    private fun setCurrentLocationButtonIcon(isInCurrentLocation: Boolean, view: View) {
        val currentLocationButton: FloatingActionButton = view.findViewById(R.id.currentLocation)

        if (isInCurrentLocation) {
            currentLocationButton.setImageResource(R.drawable.ic_my_location_blue)
        } else {
            currentLocationButton.setImageResource(R.drawable.ic_my_location_black)
        }
    }

    private fun sendNotification(threatAlerts: ArrayList<Threat>) {
        threatAlerts.forEach { threat ->
            if (shouldSendAlert(threat.feature.id()!!)) {

                val message =
                    getString(R.string.inside_threat_notification_content) + " " + mapViewModel.getFeatureName(threat.feature.id()!!)


                val featureType =
                    threat.feature.properties()?.get(getString(R.string.type))?.asString
                addAlertToContainer(
                    threat.feature.id()!!,
                    message,
                    BuildingTypeMapping.mapping[featureType]!!
                )
            }
        }
    }

    private fun shouldSendAlert(threatID: String): Boolean {
        val sameAlert = alertsManager.alerts.value!!.find { it.threatId == threatID }

        return if (sameAlert != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                val sameAlertDateTime = dateFormat.parse(sameAlert.time)
                Duration.between(
                    sameAlertDateTime.toInstant(),
                    Date().toInstant()
                ).seconds > Constants.ALERT_INTERVAL_IN_SECONDS
            } else {
                false
            }
        } else {
            true
        }
    }

    private fun addAlertToContainer(threatID: String, message: String, image: Int) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
        val currentDateTime = dateFormat.format(Date())
        val alert =
            AlertModel(
                threatId = threatID,
                message = message,
                image = image,
                time = currentDateTime
            )
        alertsManager.addAlert(alert)
    }

    private fun setAlertPopUp(alert: AlertModel) {
        val alertFragmentInstance = AlertFragment.newInstance(alert)

        activity!!.supportFragmentManager.beginTransaction().replace(
            R.id.alert_Pop_Up,
            alertFragmentInstance
        ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
    }

    private fun observeRiskStatus(isLocationAdapterInitialized: Boolean) {
        if (isLocationAdapterInitialized) {
            val riskStatusObserver = Observer<RiskStatus> { newStatus ->
                sharedViewModel.isVisible.value =
                    (newStatus == RiskStatus.HIGH || newStatus == RiskStatus.MEDIUM)
                mapViewModel.checkRiskStatus()
            }

            mapViewModel.riskStatus.observe(this, riskStatusObserver)
        }

    }

    private fun filter(shouldApplyFilter: Boolean) {
        if (!shouldApplyFilter) {
            mapViewModel.removeFilter(map.style!!, sharedViewModel.layerToFilterId)
        } else {
            mapViewModel.applyFilter(
                map.style!!,
                sharedViewModel.layerToFilterId,
                sharedViewModel.chosenPropertyId,
                sharedViewModel.isStringType,
                sharedViewModel.numericType,
                sharedViewModel.chosenPropertyValue,
                sharedViewModel.specificValue,
                sharedViewModel.minValue,
                sharedViewModel.maxValue
            )
        }
    }

    private fun showDescriptionFragment() {
        val dataCardFragmentInstance = DataCardFragment.newInstance()

        activity!!.supportFragmentManager.beginTransaction().replace(
            R.id.fragmentParent,
            dataCardFragmentInstance
        ).commit()
    }

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            RECORD_REQUEST_CODE
        )
    }

    private fun showAlertDialog() {
        AlertDialog.Builder(listenerMap as Context, R.style.AlertDialogTheme)
            .setTitle(getString(R.string.turn_on_location_title))
            .setMessage(getString(R.string.turn_on_location))
            .setPositiveButton(getString(R.string.yes_hebrew)) { _, _ ->
                val settingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(settingIntent)
            }.setNegativeButton(getString(R.string.no_thanks_hebrew)) { dialog, _ ->
                dialog.cancel()
            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        mapViewModel.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun showToast() {
        mapViewModel.noPermissionsToast.value?.show()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        map.addOnMapClickListener(this)
        mapViewModel.onMapReady(map)
    }

    private fun initLocationObserver() {
        val locationObserver = Observer<Location?> { newLocation ->
            if (mapViewModel.isLocationAdapterInitialized.value == true && newLocation != null) {
                mapViewModel.changeLocation(newLocation)
            }
        }
        mapViewModel.locationAdapter!!.getCurrentLocation().observe(this, locationObserver)
    }

    private fun initFocusOnMyLocationButton(view: View) {
        val currentLocationButton: View = view.findViewById(R.id.currentLocation)
        currentLocationButton.setOnClickListener {
            mapViewModel.focusOnMyLocationClicked()
        }
    }

    private fun initMapLayersButton(view: View) {
        val mapLayersButton: View = view.findViewById(R.id.mapLayers)
        mapLayersButton.setOnClickListener {
            openDialogFragment(MapLayersFragment())
        }
    }

    private fun openDialogFragment(newDialogFragment: DialogFragment) {
        val fragmentTransaction = fragmentManager?.beginTransaction()
        val previousFragment = fragmentManager?.findFragmentByTag("dialog")

        if (previousFragment != null) {
            fragmentTransaction?.remove(previousFragment)
        }

        fragmentTransaction?.addToBackStack(null)
        newDialogFragment.show(fragmentTransaction!!, "dialog")
    }

    override fun onMapClick(latLng: LatLng): Boolean { // TODO UniqAi need to fix

        // return mapViewModel.onMapClick(map, latLng)

        val loadedMapStyle = map.style

        if (loadedMapStyle == null || !loadedMapStyle.isFullyLoaded) {
            return false
        }

        loadedMapStyle.removeLayer("threat-source-layer")
        loadedMapStyle.removeSource("threat-source")
        loadedMapStyle.removeLayer("layer-selected-location")
        loadedMapStyle.removeSource("source-marker-click")
        loadedMapStyle.removeImage("marker-icon-alertID")

        val selectedBuildingSource =
            loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.SELECTED_BUILDING_SOURCE_ID)
        selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(ArrayList()))

        mapViewModel.setLayerVisibility(Constants.THREAT_COVERAGE_LAYER_ID, visibility(NONE))

        if (mapViewModel.isAreaSelectionMode) {
            mapViewModel.drawPolygonMode(latLng)
        } else {
            if (mapViewModel.selectLocationManual || mapViewModel.selectLocationManualConstruction || mapViewModel.selectLocationManualCoverage || mapViewModel.selectLocationManualCoverageAll) {

                // Add the marker image to map
                loadedMapStyle.addImage(
                    "marker-icon-alertID",
                    BitmapFactory.decodeResource(
                        App.resourses, R.drawable.mapbox_marker_icon_default
                    )
                )

                val geoJsonSource = GeoJsonSource(
                    "source-marker-click",
                    Feature.fromGeometry(Point.fromLngLat(latLng.longitude, latLng.latitude))
                )

                loadedMapStyle.addSource(geoJsonSource)

                val symbolLayer = SymbolLayer("layer-selected-location", "source-marker-click")
                symbolLayer.withProperties(
                    PropertyFactory.iconImage("marker-icon-alertID")
                )
                loadedMapStyle.addLayer(symbolLayer)

                if (mapViewModel.selectLocationManual) {
                    mapViewModel.updateThreatFeaturesBuildings(mapView, latLng)
                    mapViewModel.selectLocationManual = false
                    mapViewModel.threatFeatures.value?.let { visualizeThreats(it) }
                } else if (mapViewModel.selectLocationManualConstruction) {

                    mapViewModel.updateThreatFeaturesConstruction(latLng)
                    mapViewModel.selectLocationManualConstruction = false
                } else if (mapViewModel.selectLocationManualCoverage) {
                    val progressBar: ProgressBar = view!!.findViewById(R.id.progressBar)
                    progressBar.visibility = VISIBLE
                    if (sharedViewModel.coverageSearchHeightMetersChecked) {
                        mapViewModel.calculateCoverageFromPoint(
                            latLng,
                            sharedViewModel.coverageRangeMeters,
                            sharedViewModel.coverageResolutionMeters,
                            sharedViewModel.coverageSearchHeightMeters,
                            progressBar
                        )
                    } else {
                        mapViewModel.calculateCoverageFromPoint(
                            latLng,
                            sharedViewModel.coverageRangeMeters,
                            sharedViewModel.coverageResolutionMeters,
                            Constants.DEFAULT_COVERAGE_HEIGHT_METERS,
                            progressBar
                        )
                    }
                    mapViewModel.selectLocationManualCoverage = false
                } else if (mapViewModel.selectLocationManualCoverageAll) {
                    val progressBar: ProgressBar = view!!.findViewById(R.id.progressBar)
                    progressBar.visibility = VISIBLE
                    mapViewModel.calculateCoverageForAll(
                        latLng,
                        sharedViewModel.coverageRangeMeters,
                        sharedViewModel.coverageResolutionMeters,
                        sharedViewModel.coverageSearchHeightMeters,
                        progressBar
                    )
                    mapViewModel.selectLocationManualCoverageAll = false
                }


            } else {

                val point = map.projection.toScreenLocation(latLng)
                val features = map.queryRenderedFeatures(point, Constants.THREAT_LAYER_ID)

                if (features.size > 0) {
                    selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))

                    val threat = mapViewModel.buildingThreatToCurrentLocation(features[0])

                    val bundle = Bundle()
                    bundle.putParcelable("threat", threat)

                    // take to function!
                    val dataCardFragmentInstance = DataCardFragment.newInstance()
                    dataCardFragmentInstance.arguments = bundle
                    activity!!.supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentParent,
                        dataCardFragmentInstance
                    ).commit()
                    activity!!.supportFragmentManager.fragments
                    activity!!.supportFragmentManager.fragments
                }
            }
        }

        return true
    }

    private fun applyExperimentalOption(id: Int) {
        when (id) {
            R.id.threat_list_menu_item -> {
                openThreatListFragment()
            }
            R.id.threat_select_location_buildings -> {
                mapViewModel.selectLocationManual = true
                Toast.makeText(listenerMap as Context, "Select Location", Toast.LENGTH_LONG).show()
            }
            R.id.threat_select_location -> {
                mapViewModel.selectLocationManualConstruction = true
                Toast.makeText(listenerMap as Context, "Select Location", Toast.LENGTH_LONG).show()
            }
            R.id.point_coverage -> {
                mapViewModel.selectLocationManualCoverage = true
                Toast.makeText(listenerMap as Context, "Select Location", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openThreatListFragment() {
        mapViewModel.threats.value?.let {
            val bundle = Bundle()
            bundle.putParcelableArrayList("threats", it)

            val transaction = activity!!.supportFragmentManager.beginTransaction()
            val fragment = ThreatFragment()
            fragment.arguments = bundle
            transaction.apply {
                replace(R.id.threat_list_fragment_container, fragment).commit()
                addToBackStack(fragment.javaClass.simpleName)
            }
        }
    }

    private fun enableAreaSelection(view: View, shouldEnable: Boolean) {
        val mainMapLayoutView = view.mainMapLayout
        val currentLocationButton = view.currentLocation
//        val radiusLayerButton = view.radiusLayer
        val navigationButton = view.navigationButton

        if (shouldEnable) {
            layoutInflater.inflate(R.layout.area_selection, mainMapLayoutView)
            val areaModeView = view.findViewById<View>(R.id.area_mode)
            initUndoButton(areaModeView)
            initCancelAreaButton(areaModeView)
            initApplyAreaButton(areaModeView)
            mapViewModel.removeAreaFromMap()
        } else {
            mainMapLayoutView.removeView(view.findViewById(R.id.area_mode))
            sharedViewModel.shouldDefineArea.value = false
        }

//        radiusLayerButton.isEnabled = !shouldEnable
        navigationButton.isEnabled = !shouldEnable
        currentLocationButton.isEnabled = !shouldEnable
        mapViewModel.isAreaSelectionMode = shouldEnable
    }

    private fun initUndoButton(view: View) {
        view.findViewById<View>(R.id.undo).setOnClickListener {
            mapViewModel.undo()
        }
    }

    private fun initApplyAreaButton(view: View) { // MVVM ? applyClicked function?
        view.findViewById<View>(R.id.apply_area).setOnClickListener {
            mapViewModel.saveAreaOfInterest()
            enableAreaSelection(view.parent as View, false)
        }
    }

    private fun initCancelAreaButton(view: View) {
        view.findViewById<View>(R.id.cancel_area).setOnClickListener {
            mapViewModel.cancelAreaSelection()
            enableAreaSelection(view.parent as View, false)
        }
    }

    private fun onListFragmentInteraction(item: Threat?) {
        if (item != null) {

            val feature = item.feature

            val featureCollection = FeatureCollection.fromFeatures(
                arrayOf(feature)
            )

            val geoJsonSource = GeoJsonSource("threat-source", featureCollection)

            val loadedMapStyle = map.style

            if (loadedMapStyle != null && loadedMapStyle.isFullyLoaded) {
                loadedMapStyle.removeLayer("threat-source-layer")
                loadedMapStyle.removeSource("threat-source")

                // colorize the feature
                loadedMapStyle.addSource(geoJsonSource)
                val fillLayer = FillLayer("threat-source-layer", "threat-source")
                fillLayer.setProperties(
                    PropertyFactory.fillExtrusionColor(Color.RED),
                    PropertyFactory.fillColor(Color.RED)
                )
                loadedMapStyle.addLayer(fillLayer)

                // open card fragment and pass the threat as an argument
                val bundle = Bundle()
                bundle.putParcelable("threat", item)
                val dataCardFragmentInstance = DataCardFragment.newInstance()
                dataCardFragmentInstance.arguments = bundle
                if (activity!!.supportFragmentManager.fragments.find { fragment -> fragment.id == R.id.fragmentParent } == null)
                    activity!!.supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentParent,
                        dataCardFragmentInstance
                    ).commit()
            }
        }

        val fragment =
            activity!!.supportFragmentManager.findFragmentById(R.id.threat_list_fragment_container)
        if (fragment != null) {
            activity!!.supportFragmentManager.beginTransaction()
                .remove(fragment).commit()
        }
    }

    private fun visualizeThreats(features: List<Feature>) {

        val loadedMapStyle = map.style

        if (loadedMapStyle == null || !loadedMapStyle.isFullyLoaded) {
            return
        }

        loadedMapStyle.removeLayer("threat-source-layer")
        loadedMapStyle.removeSource("threat-source")

        val selectedBuildingSource =
            loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.SELECTED_BUILDING_SOURCE_ID)
        selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMapFragmentInteractionListener) {
            listenerMap = context
        } else {
            throw RuntimeException("$context must implement OnMapFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listenerMap = null
    }

    interface OnMapFragmentInteractionListener {
        fun onMapFragmentInteraction()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(broadcastReceiver, zoomFilter)
        activity?.registerReceiver(broadcastReceiver, alertAcceptedFilter)
        mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mapViewModel.riskStatus.hasObservers()) {
            mapViewModel.riskStatus.removeObservers(this)
        }
        mapViewModel.clean()
        map.removeOnMapClickListener(this)
        mapView.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(broadcastReceiver)
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
