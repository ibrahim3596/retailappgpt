package com.retailpos.app.core.products

import android.content.Context

private const val PREFS = "retailpos_settings"
private const val GST_MODE_KEY = "gst_mode"

object StoreTaxPreferences {
    fun loadMode(context: Context): StoreTaxMode {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(GST_MODE_KEY, StoreTaxMode.NO_GST.storageValue)
            ?: StoreTaxMode.NO_GST.storageValue
        return StoreTaxMode.fromStorage(value)
    }
}
