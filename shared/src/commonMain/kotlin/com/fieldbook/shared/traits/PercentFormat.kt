package com.fieldbook.shared.traits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.database.models.TraitObject
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.ic_trait_percent
import com.fieldbook.shared.generated.resources.traits_format_percent

class PercentFormat : TraitFormat(
    format = Formats.PERCENT,
    nameStringResource = Res.string.traits_format_percent,
    iconDrawableResource = Res.drawable.ic_trait_percent,
) {

    @Composable
    override fun ParametersEditor(trait: TraitObject, onTraitChange: (TraitObject) -> Unit) {
        var defaultVal by remember { mutableStateOf(trait.defaultValue ?: "") }
        var minVal by remember { mutableStateOf(trait.minimum ?: "0") }
        var maxVal by remember { mutableStateOf(trait.maximum ?: "100") }

        var generalError by remember { mutableStateOf("") }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TraitEditorTextField(
                title = "Default",
                placeholder = "Optional",
                value = defaultVal,
                onValueChange = { defaultVal = it },
                clearable = true,
                numeric = true
            )
            generalError = validatePercentValues(defaultVal, minVal, maxVal)
            trait.defaultValue = defaultVal.ifBlank { null }

            TraitEditorTextField(
                title = "Minimum",
                placeholder = "0",
                value = minVal,
                onValueChange = { minVal = it },
                clearable = true,
                numeric = true
            )
            generalError = validatePercentValues(defaultVal, minVal, maxVal)
            trait.minimum = minVal.ifBlank { null }

            TraitEditorTextField(
                title = "Maximum",
                placeholder = "100",
                value = maxVal,
                onValueChange = { maxVal = it },
                clearable = true,
                numeric = true
            )
            generalError = validatePercentValues(defaultVal, minVal, maxVal)
            trait.maximum = maxVal.ifBlank { null }
            trait.additionalInfo = generalError.ifBlank { null }
            onTraitChange(trait)

            if (generalError.isNotBlank()) {
                Text(generalError, color = MaterialTheme.colorScheme.error)
            }
        }
    }

}

private fun validatePercentValues(
    defaultVal: String,
    minVal: String,
    maxVal: String,
): String {
    val defaultNum = defaultVal.toDoubleOrNull()
    val minNum = minVal.toDoubleOrNull()
    val maxNum = maxVal.toDoubleOrNull()

    if (defaultVal.isNotBlank() && defaultNum == null) return "Must be numeric"
    if (minVal.isNotBlank() && minNum == null) return "Must be numeric"
    if (maxVal.isNotBlank() && maxNum == null) return "Must be numeric"

    if (minNum != null && maxNum != null && maxNum < minNum) {
        return "Maximum must be greater than or equal to minimum"
    }

    if (defaultNum != null && minNum != null && defaultNum < minNum) {
        return "Default must be greater than or equal to minimum"
    }

    if (defaultNum != null && maxNum != null && defaultNum > maxNum) {
        return "Default must be less than or equal to maximum"
    }

    return ""
}
