package com.azurelan.testyourpayments.preferencevaluestore

import android.content.Context
import android.content.SharedPreferences
import com.azurelan.testyourpayments.R

object PreferencesAccessUtils {

    fun readPreferenceString(context: Context, key: String, defValue: String = "") =
        getSharedPreferenceFile(context)?.getString(key, defValue) ?: defValue

    fun writePreferenceString(context: Context, key: String, value: String) {
        val sharedPref = getSharedPreferenceFile(context)
        sharedPref?.let {
            with(it.edit()) {
                putString(key, value)
                apply()
            }
        }
    }

    private fun getSharedPreferenceFile(context: Context): SharedPreferences? =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
}