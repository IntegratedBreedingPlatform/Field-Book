package com.fieldbook.shared.preferences

import com.russhwolf.settings.Settings

private const val EmptySetSentinel = "__FIELDBOOK_EMPTY_SET__"
private const val Delimiter = '\u001F'
private const val Escape = '\\'

expect fun loadStringSetPreference(
    settings: Settings,
    key: String,
    legacySeparators: CharArray = charArrayOf(),
): Set<String>?

expect fun persistStringSetPreference(
    settings: Settings,
    key: String,
    values: Set<String>,
)

internal fun decodeStoredStringSetPreference(
    serialized: String?,
    legacySeparators: CharArray = charArrayOf(),
): Set<String>? {
    if (serialized == null) return null
    if (serialized.isEmpty() || serialized == EmptySetSentinel) return emptySet()

    val useLegacySeparators = serialized.indexOf(Delimiter) == -1 &&
        legacySeparators.any { separator -> separator in serialized }

    return if (useLegacySeparators) {
        serialized.split(*legacySeparators)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    } else {
        decodeEscapedStringSet(serialized)
    }
}

internal fun encodeStoredStringSetPreference(values: Set<String>): String {
    if (values.isEmpty()) return EmptySetSentinel

    return values.joinToString(Delimiter.toString()) { value ->
        buildString {
            value.forEach { character ->
                if (character == Escape || character == Delimiter) {
                    append(Escape)
                }
                append(character)
            }
        }
    }
}

private fun decodeEscapedStringSet(serialized: String): Set<String> {
    val items = mutableListOf<String>()
    val currentValue = StringBuilder()
    var isEscaped = false

    serialized.forEach { character ->
        when {
            isEscaped -> {
                currentValue.append(character)
                isEscaped = false
            }

            character == Escape -> isEscaped = true
            character == Delimiter -> {
                items += currentValue.toString()
                currentValue.clear()
            }

            else -> currentValue.append(character)
        }
    }

    if (isEscaped) {
        currentValue.append(Escape)
    }

    items += currentValue.toString()

    return items
        .filter { it.isNotEmpty() }
        .toSet()
}
