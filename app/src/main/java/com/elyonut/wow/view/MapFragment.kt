package com.elyonut.wow.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.*
import com.elyonut.wow.databinding.AreaSelectionBinding
import com.elyonut.wow.databinding.FragmentMapBinding
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.model.Threat
import com.elyonut.wow.parser.MapboxParser
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
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList

class MapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener {
    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var mapViewModel: MapViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private var listenerMap: OnMapFragmentInteractionListener? = null

    private lateinit var broadcastReceiver: BroadcastReceiver
    private var zoomFilter = IntentFilter(Constants.ZOOM_LOCATION_ACTION)
    private lateinit var alertsManager: AlertsManager
    private lateinit var binding: FragmentMapBinding
    private lateinit var areaSelectionBinding: AreaSelectionBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Mapbox.getInstance(listenerMap as Context, Maps.MAPBOX_ACCESS_TOKEN)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false)
        mapViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
                .create(MapViewModel::class.java)
        sharedViewModel =
            activity?.run { ViewModelProviders.of(activity!!)[SharedViewModel::class.java] }!!

        alertsManager = sharedViewModel.alertsManager
        mapView = binding.mainMapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        binding.mapViewModel = mapViewModel
        initArea()
        setObservers()
        initBroadcastReceiver()
        initMapLayersButton()

        return binding.root
    }

    // TODO maybe move to alert fragment?
    private fun initBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Constants.ZOOM_LOCATION_ACTION -> {
                        mapViewModel.setZoomLocation(intent.getStringExtra("threatID"))
                        (context as FragmentActivity).supportFragmentManager.popBackStack()
                    }
                }
            }
        }
    }

    // TODO maybe change to init drawings + should the map be responsibe? or other class
    private fun initArea() {
        if (sharedViewModel.areaOfInterest != null) {
            mapViewModel.areaOfInterest.value =
                sharedViewModel.areaOfInterest // TODO Should be encapsulated

            val polygonPoints = ArrayList<Point>()
            sharedViewModel.areaOfInterest!!.coordinates().forEach { it ->
                it.forEach {
                    polygonPoints.add(it)
                }
            }

            mapViewModel.lineLayerPointList = polygonPoints
        }
    }

    private fun setObservers() {
        mapViewModel.areaOfInterest.observe(this, Observer {
            sharedViewModel.areaOfInterest = it
        })
        mapViewModel.selectedBuildingId.observe(
            this,
            Observer { showDescriptionFragment() }
        )
        mapViewModel.isLocationAdapterInitialized.observe(
            this,
            Observer {
                observeRiskStatus(it)
            })

        mapViewModel.threatAlerts.observe(this, Observer {
            sendNotification(it)
        })
        mapViewModel.shouldDisableAreaSelection.observe(this, Observer {
            if (it) {
                disableAreaSelection()
            }
        })

        sharedViewModel.selectedLayerId.observe(this, Observer {
            it?.let { mapViewModel.layerSelected(it) }
        })

        sharedViewModel.selectedExperimentalOption.observe(
            this,
            Observer { applyExperimentalOption(it) }
        )

        sharedViewModel.shouldOpenThreatsFragment.observe(this, Observer {
            if (it) {
                openThreatListFragment()
            }
        })

        sharedViewModel.selectedThreatItem.observe(
            this,
            Observer { onListFragmentInteraction(it) }
        )

        sharedViewModel.shouldApplyFilter.observe(this,
            Observer { filter(it) }
        )

        sharedViewModel.shouldDefineArea.observe(this, Observer {
            if (it) {
                enableAreaSelection()
            }
        })

        sharedViewModel.chosenTypeToFilter.observe(this, Observer {
            mapViewModel.filterLayerByType(it)

        })

        sharedViewModel.isSelectAllChecked.observe(this, Observer {
            mapViewModel.filterLayerByAllTypes(it)
        })

        mapViewModel.isFocusedOnLocation.observe(this, Observer {
            setCurrentLocationButtonIcon(it)
        })

        alertsManager.alerts.observe(this, Observer { alerts ->
            if (alerts.isNotEmpty()) {
                alerts.sortedBy { alert -> alert.time }.firstOrNull { alert -> !alert.isRead }
                    ?.let { setAlertPopUp(it) }
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

        sharedViewModel.coordinatesfeaturesInCoverage.observe(this, Observer { addCoveregae(it) })

        mapViewModel.currentThreats.observe(this, Observer {
            if (it.isNotEmpty()) {
                currentThreatUpdated()
            }
        })
    }

    fun setLayersObservers() {
        mapViewModel.mapLayers.observe(this, Observer { layersUpdated(it) })
    }

    fun layersUpdated(layers: List<LayerModel>) {
        mapViewModel.updateCurrentThreats()
        updateMapSources(layers)
    }

    private fun updateMapSources(layers: List<LayerModel>) {
        layers.forEach { layer ->
            map.style?.getSourceAs<GeoJsonSource>(layer.id)?.let { geojson ->
                geojson.setGeoJson(
                    FeatureCollection.fromFeatures(
                        layer.features.map { feature ->
                            MapboxParser.parseToMapboxFeature(feature)
                        }
                    )
                )
            }
        }
    }

    // TODO SelfCentered instead current location
    private fun setCurrentLocationButtonIcon(isInCurrentLocation: Boolean) {
        val currentLocationButton: FloatingActionButton = binding.currentLocation

        if (isInCurrentLocation) {
            currentLocationButton.setImageResource(R.drawable.ic_my_location_blue)
        } else {
            currentLocationButton.setImageResource(R.drawable.ic_my_location_black)
        }
    }

    // TODO move to alertsManager
    // Beggining of alert handling
    private fun sendNotification(threatAlerts: ArrayList<Threat>) {
        threatAlerts.forEach { threat ->
            if (shouldSendAlert(threat.id)) {

                val message =
                    getString(R.string.inside_threat_notification_content) + " " + mapViewModel.getFeatureName(
                        threat.id
                    )
                val featureType =
                    threat.enemyType
                addAlertToContainer(
                    threat.id,
                    message,
                    BuildingTypeMapping.mapping[featureType]!!
                )
            }
        }
    }

    private fun shouldSendAlert(threatID: String): Boolean {
        val sameAlert = alertsManager.alerts.value!!.find { it.threatId == threatID }

        return if (sameAlert != null) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
            val sameAlertDateTime = dateFormat.parse(sameAlert.time)
            Duration.between(
                sameAlertDateTime.toInstant(),
                Date().toInstant()
            ).seconds > Constants.ALERT_INTERVAL_IN_SECONDS
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
    // End of alert handling

    private fun observeRiskStatus(isLocationServiceInitialized: Boolean) {
        if (isLocationServiceInitialized) {
//            mapViewModel.riskStatus.observe(this, Observer { newStatus ->
//                sharedViewModel.isVisible.value =
//                    (newStatus == RiskStatus.HIGH || newStatus == RiskStatus.MEDIUM)
//                mapViewModel.currentThreatUpdated()
//            })
        }

    }

    fun currentThreatUpdated() {
        sharedViewModel.isVisible.value = true
        mapViewModel.currentThreatsUpdated()
    }

    // TODO Remove filter
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

    // TODO Maybe navigation
    private fun showDescriptionFragment() {
        val dataCardFragmentInstance = DataCardFragment.newInstance()

        activity!!.supportFragmentManager.beginTransaction().replace(
            R.id.fragmentParent,
            dataCardFragmentInstance
        ).commit()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        map.addOnMapClickListener(this)
        mapViewModel.onMapReady(map)
        setLayersObservers()
    }

    private fun initMapLayersButton() { // Data binding? is there a way use?
        binding.mapLayers.setOnClickListener {
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

    // TODO reformat, move to viewModel, CR
    override fun onMapClick(latLng: LatLng): Boolean {

        // return mapViewModel.onMapClick(map, latLng)

        val loadedMapStyle = map.style

        if (loadedMapStyle == null || !loadedMapStyle.isFullyLoaded) {
            return false
        }

        // What are these layers? why do we delete them?
        loadedMapStyle.removeLayer("threat-source-layer")
        loadedMapStyle.removeSource("threat-source")
        loadedMapStyle.removeLayer("layer-selected-location")
        loadedMapStyle.removeSource("source-marker-click")
        loadedMapStyle.removeImage("marker-icon-alertID")

        val selectedBuildingSource =
            loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.SELECTED_BUILDING_SOURCE_ID)
        selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(ArrayList()))

        mapViewModel.setLayerVisibility(Constants.THREAT_COVERAGE_LAYER_ID, visibility(NONE))

        // Checking in which state are we - maybe we should add an enum
        if (mapViewModel.isAreaSelectionMode) {
            mapViewModel.drawPolygonMode(latLng)
        } else {
            if (mapViewModel.selectLocationManual || mapViewModel.selectLocationManualConstruction || mapViewModel.selectLocationManualCoverage || mapViewModel.selectLocationManualCoverageAll) {

                // Add the marker image to map
                loadedMapStyle.addImage(
                    "marker-icon-alertID",
                    BitmapFactory.decodeResource(
                        App.resources_, R.drawable.mapbox_marker_icon_default
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

                when {
                    mapViewModel.selectLocationManual -> { // מבנים שולטים על נקודה
                        mapViewModel.updateBuildingsWithinLOS(latLng)
                        mapViewModel.selectLocationManual = false
                        mapViewModel.buildingsWithinLOS.value?.let { visualizeThreats(it) }
                    }
                    mapViewModel.selectLocationManualCoverage -> {
                        sharedViewModel.mapClickedLatlng.postValue(latLng)
                        mapViewModel.selectLocationManualCoverage = false
                    }
                    mapViewModel.selectLocationManualCoverageAll -> {
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
                }

            } else { // If we are not in the above states, than we should open a data card if a building was clicked
//                val features = loadedMapStyle.getSourceAs<GeoJsonSource>(Constants.THREAT_LAYER_ID).querySourceFeatures(Expression.)
                val building = mapViewModel.getBuildingAtLocation(latLng, Constants.THREAT_LAYER_ID)

                if (building != null) {
                    selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeature(building))

                    val threat = mapViewModel.buildingThreatToCurrentLocation(building)

                    val bundle = Bundle()
                    bundle.putParcelable("feature", threat)

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

    fun addCoveregae(coverageFeatures: List<Feature>) {
        Timber.i("map coordinates:  %s", coverageFeatures.toString())
        val threatCoverageSource: GeoJsonSource? =
            map.style?.getSourceAs(
                Constants.THREAT_COVERAGE_SOURCE_ID
            )

        threatCoverageSource?.setGeoJson(FeatureCollection.fromFeatures(coverageFeatures))

        mapViewModel.setLayerVisibility(
            Constants.THREAT_COVERAGE_LAYER_ID,
            visibility(Property.VISIBLE)
        )
    }

    // From onMapClick
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

    // TODO rename
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
            R.id.coverage_all -> {
                mapViewModel.selectLocationManualCoverageAll = true
//                Toast.makeText(listenerMap as Context, "Select Location", Toast.LENGTH_LONG).show()
            }
        }
    }

    // TODO Maybe navigation
    private fun openThreatListFragment() {
        mapViewModel.currentThreats.value?.let {
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

    private fun enableAreaSelection() {
        areaSelectionBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.area_selection,
            binding.mainMapLayout,
            true
        )

        areaSelectionBinding.mapViewModel = mapViewModel
        mapViewModel.enableAreaSelection()
        defineAreaSelectionMode(true)
    }

    private fun disableAreaSelection() {
        binding.mainMapLayout.removeView(areaSelectionBinding.areaMode)
        defineAreaSelectionMode(false)
        sharedViewModel.shouldDefineArea.value = false
    }

    private fun defineAreaSelectionMode(shouldEnable: Boolean) {
        binding.navigationButton.isEnabled = !shouldEnable
        binding.currentLocation.isEnabled = !shouldEnable
        mapViewModel.isAreaSelectionMode = shouldEnable
    }

    // TODO update layers. handle according to datacard
    private fun onListFragmentInteraction(item: Threat?) {
        if (item != null) {
//            val feature = item.feature
            val feature = MapboxParser.parseToMapboxFeature(item)
            val featureCollection = FeatureCollection.fromFeatures(arrayOf(feature))
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
                bundle.putParcelable("feature", item)
                val dataCardFragmentInstance = DataCardFragment.newInstance()
                dataCardFragmentInstance.arguments = bundle

                // TODO Change to navigation!!!
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

    // TODO Figure out what is it for?
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
        mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
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
