package com.elyonut.wow.utilities

import android.view.View

fun View.toggleViewVisibility(isVisible: Boolean) {
    visibility = if (isVisible) {
        View.GONE
    } else {
        View.VISIBLE
    }
}