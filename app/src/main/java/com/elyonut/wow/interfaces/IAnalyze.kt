package com.elyonut.wow.interfaces

import android.location.Location
import com.elyonut.wow.model.RiskStatus
import java.util.*
import kotlin.collections.ArrayList

interface IAnalyze {
    fun calcRiskStatus(location: Location): Pair<RiskStatus, HashMap<RiskStatus, ArrayList<String>>>
}