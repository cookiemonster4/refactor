package com.elyonut.wow.model

import android.graphics.Color

enum class RiskStatus(val text: String, val color: Int) {
    NONE("אפור", Color.GRAY),
    LOW("צהוב", Color.parseColor("#ffdb4d")),
    MEDIUM("כתום", Color.parseColor("#ff9500")),
    HIGH("אדום", Color.RED)
}