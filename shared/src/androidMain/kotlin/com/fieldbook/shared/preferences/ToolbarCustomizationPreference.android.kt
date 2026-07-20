package com.fieldbook.shared.preferences

import androidx.preference.PreferenceManager
import com.fieldbook.shared.AndroidAppContextHolder
import com.russhwolf.settings.Settings

actual fun loadToolbarCustomizationPreference(
    settings: Settings,
    key: String,
    defaultOptions: Set<String>,
): Set<String> {
    return loadStringSetPreference(
        settings = settings,
        key = key,
        legacySeparators = charArrayOf(',', '\n')
    ) ?: defaultOptions
}

actual fun persistToolbarCustomizationPreference(
    settings: Settings,
    key: String,
    values: Set<String>,
) {
    persistStringSetPreference(
        settings = settings,
        key = key,
        values = values
    )
}
