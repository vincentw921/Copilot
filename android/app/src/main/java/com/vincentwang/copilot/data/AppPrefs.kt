package com.vincentwang.copilot.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Device-local preference data, the counterpart of @AppStorage on iOS:
 * pilot identity (shown on exports) and the NVG-logging toggle. Exposed
 * as StateFlows so Compose screens recompose when a value changes.
 */
object AppPrefs {
    private lateinit var prefs: SharedPreferences

    private val _pilotName = MutableStateFlow("")
    val pilotName: StateFlow<String> get() = _pilotName

    private val _certificateNumber = MutableStateFlow("")
    val certificateNumber: StateFlow<String> get() = _certificateNumber

    /** When enabled, NVG time can be logged in the flight form and the
     *  NVG total appears on the Report tab. Off by default since most
     *  civilian pilots never log night-vision-goggle time. */
    private val _nvgLoggingEnabled = MutableStateFlow(false)
    val nvgLoggingEnabled: StateFlow<Boolean> get() = _nvgLoggingEnabled

    fun init(context: Context) {
        prefs = context.getSharedPreferences("copilot_prefs", Context.MODE_PRIVATE)
        _pilotName.value = prefs.getString("pilotName", "").orEmpty()
        _certificateNumber.value = prefs.getString("certificateNumber", "").orEmpty()
        _nvgLoggingEnabled.value = prefs.getBoolean("nvgLoggingEnabled", false)
    }

    fun setPilotName(value: String) {
        _pilotName.value = value
        prefs.edit().putString("pilotName", value).apply()
    }

    fun setCertificateNumber(value: String) {
        _certificateNumber.value = value
        prefs.edit().putString("certificateNumber", value).apply()
    }

    fun setNvgLoggingEnabled(value: Boolean) {
        _nvgLoggingEnabled.value = value
        prefs.edit().putBoolean("nvgLoggingEnabled", value).apply()
    }
}
