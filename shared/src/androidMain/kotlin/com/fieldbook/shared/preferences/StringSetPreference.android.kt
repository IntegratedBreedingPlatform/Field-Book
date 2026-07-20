package com.fieldbook.shared.preferences

import androidx.preference.PreferenceManager
import com.fieldbook.shared.AndroidAppContextHolder
import com.russhwolf.settings.Settings

actual fun loadStringSetPreference(
    settings: Settings,
    key: String,
    legacySeparators: CharArray,
): Set<String>? {
    val preferences = PreferenceManager.getDefaultSharedPreferences(AndroidAppContextHolder.context)
    if (preferences.contains(key)) {
        return preferences.getStringSet(key, emptySet())?.toSet() ?: emptySet()
    }

    return decodeStoredStringSetPreference(
        serialized = settings.getStringOrNull(key),
        legacySeparators = legacySeparators
    )
}

actual fun persistStringSetPreference(
    settings: Settings,
    key: String,
    values: Set<String>,
) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(AndroidAppContextHolder.context)
    preferences.edit().putStringSet(key, values).apply()
}
