package com.elyonut.wow.utilities

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.elyonut.wow.model.Threat

@BindingAdapter("moreContentFormatted")
fun TextView.setMoreContentFormatted(item: Threat?) {
    item?.let {
        val builder = StringBuilder()
        builder.append(String.format("גובה (מטרים): %.3f\n", item.height))
        builder.append(String.format("מרחק (מטרים): %.3f\n", item.distanceMeters))
        builder.append(String.format("אזימוט: %.3f\n", item.azimuth))
        builder.append(String.format("האם בקו ראיה: %s\n", if (item.isLos) "כן" else "לא"))
        item.properties.keySet()
            .forEach { key -> builder.append(key + ": " + item.properties[key] + "\n") }
        text = builder.toString()
    }
}