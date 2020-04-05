package com.elyonut.wow.utilities

import com.elyonut.wow.R

class BuildingTypeMapping {
    companion object {
        val mapping: HashMap<String, Int> = hashMapOf(
            "command" to R.drawable.sunflower,
            "infantry" to R.drawable.flower1,
            "observation" to R.drawable.flower2,
            "antitank_short" to R.drawable.flower3,
            "mikush" to R.drawable.rose,
            "antitank_long" to R.drawable.flower4,
            "mortar" to R.drawable.flower5
        )
    }
}