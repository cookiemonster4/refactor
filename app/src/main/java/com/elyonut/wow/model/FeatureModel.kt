package com.elyonut.wow.model

import com.google.gson.JsonObject

data class FeatureModel(val id: String?, var properties: JsonObject?, var geometry: PolygonModel, var type: String)

