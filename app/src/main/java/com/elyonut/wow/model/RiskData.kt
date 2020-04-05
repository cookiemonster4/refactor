package com.elyonut.wow.model

import com.mapbox.mapboxsdk.geometry.LatLng

class RiskData(val currentLocation: LatLng, val riskStatus: RiskStatus, val threatList: List<FeatureModel>) {

}