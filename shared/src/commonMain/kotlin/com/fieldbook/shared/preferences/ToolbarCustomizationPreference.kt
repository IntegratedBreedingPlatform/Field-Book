package com.fieldbook.shared.preferences

import com.russhwolf.settings.Settings

expect fun loadToolbarCustomizationPreference(
    settings: Settings,
    key: String,
    defaultOptions: Set<String>,
): Set<String>

expect fun persistToolbarCustomizationPreference(
    settings: Settings,
    key: String,
    values: Set<String>,
)
