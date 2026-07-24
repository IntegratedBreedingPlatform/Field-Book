package com.fieldbook.shared

import kotlinx.serialization.Serializable

sealed interface KmpRoute {
    @Serializable
    data class ExportField(val fieldId: Int) : KmpRoute
}
