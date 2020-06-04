package com.elyonut.wow.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.interfaces.ILogger
import com.elyonut.wow.R
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.model.AlertModel
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.model.Threat
import com.elyonut.wow.utilities.Maps
import com.elyonut.wow.utilities.Menus
import com.elyonut.wow.utilities.toggleViewVisibility
import com.elyonut.wow.viewModel.MainActivityViewModel
import com.elyonut.wow.viewModel.SharedViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.android.synthetic.main.app_bar_main.*

private const val PERMISSION_REQUEST_ACCESS_LOCATION = 101

class MainActivity : AppCompatActivity(),
    DataCardFragment.OnFragmentInteractionListener,
    NavigationView.OnNavigationItemSelectedListener,
    ThreatFragment.OnListFragmentInteractionListener,
    MapFragment.OnMapFragmentInteractionListener,
    FilterFragment.OnFragmentInteractionListener,
    BottomNavigationView.OnNavigationItemSelectedListener,
    AlertsFragment.OnAlertsFragmentInteractionListener,
    AlertFragment.OnAlertFragmentInteractionListener {

    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private val logger: ILogger = TimberLogAdapter()
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private lateinit var alertsFragmentInstance: AlertsFragment
    private lateinit var navigationView: NavigationView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("com.elyonut.wow.prefs", Context.MODE_PRIVATE)
        Mapbox.getInstance(applicationContext, Maps.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)
        logger.initLogger()

        mainViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                .create(MainActivityViewModel::class.java)
        sharedViewModel =
            ViewModelProviders.of(this)[SharedViewModel::class.java]

        alertsFragmentInstance = AlertsFragment.newInstance()
        navigationView = findViewById(R.id.navigationView)
        progressBar = findViewById(R.id.progressBar)
        setObservers()
        mainViewModel.locationSetUp()
        initAreaOfInterest()
        initToolbar()
        initNavigationMenu()
        initFilterSection()
        initBottomNavigationView()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setObservers() {
        mainViewModel.isPermissionRequestNeeded.observe(this, Observer { requestPermissions() })
        mainViewModel.isPermissionDialogShown.observe(
            this,
            Observer { showLocationServiceAlertDialog() })
        mainViewModel.isProgressBarVisible.observe(
            this,
            Observer { progressBar.toggleViewVisibility(it) })
        mainViewModel.mapLayers.observe(this, Observer { updateLayersCheckbox(it) })
        mainViewModel.mapStateChanged.observe(this, Observer { sharedViewModel.mapState = it })

        mainViewModel.chosenLayerId.observe(this, Observer {
            mainViewModel.chosenLayerId.value?.let {
                sharedViewModel.selectedLayerId.postValue(it)
            }
        })

        mainViewModel.chosenTypeToFilter.observe(this, Observer {
            mainViewModel.chosenTypeToFilter.value?.let {
                sharedViewModel.chosenTypeToFilter.value = it
            }
        })

        mainViewModel.isSelectAllChecked.observe(this, Observer {
            sharedViewModel.isSelectAllChecked.value = it
            filterAllClicked(it)
        })

        mainViewModel.selectedExperimentalOption.observe(
            this,
            Observer { sharedViewModel.selectedExperimentalOption.value = it }
        )

        mainViewModel.filterSelected.observe(this, Observer {
            if (it) {
                filterButtonClicked()
            }
        })

        mainViewModel.shouldDefineArea.observe(this, Observer {
            if (it) {
                sharedViewModel.shouldDefineArea.value = it
            }
        })

        mainViewModel.shouldOpenAlertsFragment.observe(this, Observer {
            if (it) {
                openAlertsFragment()
            }
        })

        mainViewModel.coverageSettingsSelected.observe(this, Observer {
            if (it) {
                coverageSettingsButtonClicked()
            }
        })

        mainViewModel.shouldOpenThreatsFragment.observe(this, Observer {
            if (it) {
                openThreatListFragment()
            }
        })

        mainViewModel.coordinatesFeaturesInCoverage.observe(
            this,
            Observer { sharedViewModel.coordinatesFeaturesInCoverage.postValue(it) })

        sharedViewModel.isExposed.observe(this, Observer { changeAwarenessTabState(it) })
        sharedViewModel.mapClickedLatlng.observe(this, Observer { mapClicked(it) })
        sharedViewModel.shouldDefineArea.observe(this, Observer {
            if (!it) {
                mainViewModel.shouldDefineArea.value = it
            }
        })

        sharedViewModel.alertsManager.alerts.observe(this, Observer {
            editAlertsBadge(it)
        })

        sharedViewModel.coverageSearchHeightMetersChecked.observe(
            this,
            Observer { mainViewModel.coverageSearchHeightMetersCheckedChanged(it) })
    }

    private fun openThreatListFragment() {
        val fragment = ThreatFragment()
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.threat_list_fragment_container, fragment).commit()
            addToBackStack(fragment.javaClass.simpleName)
        }
    }

    @SuppressLint("InflateParams")
    private fun updateLayersCheckbox(layers: List<LayerModel>) {
        val layersSubMenu = navigationView.menu.getItem(Menus.LAYERS_MENU).subMenu

        if (layersSubMenu.size() != layers.size) {
            layers.forEachIndexed { index, layerModel ->
                val menuItem = layersSubMenu.add(R.id.nav_layers, index, index, layerModel.name)
                val checkBoxView = layoutInflater.inflate(R.layout.widget_check, null) as CheckBox
                checkBoxView.tag = layerModel
                menuItem.actionView = checkBoxView
                checkBoxView.setOnCheckedChangeListener { _, _ ->
                    (::onNavigationItemSelected)(menuItem)
                }
            }
        }
    }

    private fun mapClicked(latLng: LatLng) {
        mainViewModel.mapClicked(latLng)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {

            // Checking that the results array is not empty and that it is granted,
            // the array is according to the array sent in requestPermissions function
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mainViewModel.locationSetUp()
            } else {
                Toast.makeText(
                    application,
                    R.string.permission_not_granted,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLocationServiceAlertDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(getString(R.string.turn_on_location_title))
            .setMessage(getString(R.string.turn_on_location))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                val settingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(settingIntent)
            }.setNegativeButton(getString(R.string.no_thanks)) { dialog, _ ->
                dialog.cancel()
            }.show()
    }

    private fun editAlertsBadge(alerts: List<AlertModel>) {
        val unreadMessages = alerts.count { !it.isRead }
        if (unreadMessages == 0) {
            bottom_navigation.removeBadge(R.id.alerts)
        } else {
            bottom_navigation.getOrCreateBadge(R.id.alerts).number = unreadMessages
        }
    }

    private fun initAreaOfInterest() {
        val areaOfInterestJson = sharedPreferences.getString(Constants.AREA_OF_INTEREST_KEY, "")

        if (areaOfInterestJson != "") {
            sharedViewModel.areaOfInterest =
                gson.fromJson(areaOfInterestJson, Polygon::class.java)
        } else {
            AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(getString(R.string.area_not_defined))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    mainViewModel.shouldDefineArea.value = true // TODO Should be encapsulated
                }.setNegativeButton(getString(R.string.no_thanks)) { dialog, _ ->
                    dialog.cancel()
                }.show()
        }
    }

    private fun filterButtonClicked() {
        val filterFragment = FilterFragment.newInstance()
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentMenuParent, filterFragment).commit()
            addToBackStack(filterFragment.javaClass.simpleName)
        }
    }

    private fun coverageSettingsButtonClicked() {
        val coverageSettingsFragment = CoverageSettingsFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.apply {
            add(R.id.fragmentMenuParent, coverageSettingsFragment).commit()
            addToBackStack(coverageSettingsFragment.javaClass.simpleName)
        }
    }

    private fun initToolbar() {
        val navController = findNavController(R.id.nav_host_fragment)
        val drawerLayout = findViewById<DrawerLayout>(R.id.parentLayout)
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    // Do we need this function??
    private fun initNavigationMenu() {
        navigationView.setNavigationItemSelectedListener(this)
    }

    // TODO fix to observe layers from view model, how to do this?
    private fun initFilterSection() {
        val layerTypeValues = mainViewModel.getLayerTypeValues()?.toTypedArray()
        addSubMenuItem(
            navigationView.menu.getItem(Menus.FILTER_SUB_MENU).subMenu,
            R.id.select_all,
            getString(R.string.select_all)
        )
        layerTypeValues?.forEachIndexed { index, buildingType ->
            addSubMenuItem(
                navigationView.menu.getItem(Menus.FILTER_SUB_MENU).subMenu,
                index,
                buildingType
            )
        }
    }

    @SuppressLint("InflateParams")
    private fun addSubMenuItem(subMenu: SubMenu, id: Int, name: String) {
        val menuItem = subMenu.add(R.id.filter_options, id, Menu.NONE, name)
        val checkBoxView = layoutInflater.inflate(R.layout.widget_check, null) as CheckBox
        checkBoxView.tag = name
        menuItem.actionView = checkBoxView
        checkBoxView.setOnCheckedChangeListener { _, _ ->
            (::onNavigationItemSelected)(menuItem)
        }
    }

    private fun filterAllClicked(shouldFilter: Boolean) {
        val menu = navigationView.menu
        menu.getItem(Menus.FILTER_SUB_MENU).subMenu.forEach { menuItem ->
            (menuItem.actionView as MaterialCheckBox).isChecked = shouldFilter
        }
    }

    private fun initBottomNavigationView() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.itemIconTintList = null
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (mainViewModel.onNavigationItemSelected(item)) {
            closeDrawer()
        }

        return true
    }

    private fun changeAwarenessTabState(isExposed: Boolean) {
        val awarenessTab =
            findViewById<BottomNavigationView>(R.id.bottom_navigation).menu[Menus.AWARENESS]

        if (isExposed) {
            awarenessTab.title = getString(R.string.exposed)
            awarenessTab.icon = getDrawable(R.drawable.ic_visibility)
        } else {
            awarenessTab.title = getString(R.string.invisible)
            awarenessTab.icon = getDrawable(R.drawable.ic_visibility_off)
        }
    }

    private fun closeDrawer() {
        findViewById<DrawerLayout>(R.id.parentLayout).closeDrawer(GravityCompat.START)
    }

    private fun openAlertsFragment() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        fragmentTransaction.apply {
            add(R.id.fragment_container, alertsFragmentInstance)
            addToBackStack(alertsFragmentInstance.javaClass.simpleName)
            commit()
        }
    }

    override fun onMapFragmentInteraction() {
    }

    override fun onFilterFragmentInteraction() {
    }

    override fun onDataCardFragmentInteraction() {
    }

    override fun onListFragmentInteraction(item: Threat?) {
        sharedViewModel.selectedThreatItem.value = item
    }

    override fun onAlertsFragmentInteraction() {
    }

    override fun onAlertFragmentInteraction(uri: Uri) {
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
        var areaOfInterestJson = ""

        if (sharedViewModel.areaOfInterest != null) {
            areaOfInterestJson = gson.toJson(sharedViewModel.areaOfInterest)
        }

        sharedPreferences.edit()
            .putString(Constants.AREA_OF_INTEREST_KEY, areaOfInterestJson)
            .apply()
    }
}

