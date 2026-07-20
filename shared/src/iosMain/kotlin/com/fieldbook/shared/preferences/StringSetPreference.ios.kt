package com.fieldbook.shared.preferences

import com.russhwolf.settings.Settings

actual fun loadStringSetPreference(
    settings: Settings,
    key: String,
    legacySeparators: CharArray,
): Set<String>? {
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
    settings.putString(key, encodeStoredStringSetPreference(values))
}
