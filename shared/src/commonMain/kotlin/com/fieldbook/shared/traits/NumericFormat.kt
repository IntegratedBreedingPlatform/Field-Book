package com.fieldbook.shared.traits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.database.models.TraitObject
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.ic_trait_numeric
import com.fieldbook.shared.generated.resources.ic_transfer_cancelled
import com.fieldbook.shared.generated.resources.ic_transfer_error
import com.fieldbook.shared.generated.resources.traits_format_numeric
import org.jetbrains.compose.resources.painterResource

class NumericFormat : TraitFormat(
    format = Formats.NUMERIC,
    nameStringResource = Res.string.traits_format_numeric,
    iconDrawableResource = Res.drawable.ic_trait_numeric,
) {

    @Composable
    override fun ParametersEditor(trait: TraitObject, onTraitChange: (TraitObject) -> Unit) {
        var traitName by remember { mutableStateOf(trait.name) }
        var defaultVal by remember { mutableStateOf(trait.defaultValue ?: "") }
        var minVal by remember { mutableStateOf(trait.minimum ?: "") }
        var maxVal by remember { mutableStateOf(trait.maximum ?: "") }
        var details by remember { mutableStateOf(trait.details ?: "") }
        var generalError by remember { mutableStateOf("") }

        fun validateAndPublish() {
            generalError = validateNumericValues(defaultVal, minVal, maxVal)
            trait.name = traitName
            trait.defaultValue = defaultVal.ifBlank { null }
            trait.minimum = minVal.ifBlank { null }
            trait.maximum = maxVal.ifBlank { null }
            trait.details = details.ifBlank { null }
            trait.additionalInfo = generalError.ifBlank { null }
            onTraitChange(trait)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumericParameterTextField(
                title = "Name",
                placeholder = "Enter trait name",
                value = traitName,
                onValueChange = {
                    traitName = it
                    validateAndPublish()
                },
                clearable = true,
                isRequired = true
            )

            NumericParameterTextField(
                title = "Default",
                placeholder = "Optional",
                value = defaultVal,
                onValueChange = {
                    defaultVal = it
                    validateAndPublish()
                },
                clearable = true,
                numeric = true
            )

            NumericParameterTextField(
                title = "Minimum",
                placeholder = "Optional",
                value = minVal,
                onValueChange = {
                    minVal = it
                    validateAndPublish()
                },
                clearable = true,
                numeric = true
            )

            NumericParameterTextField(
                title = "Maximum",
                placeholder = "Optional",
                value = maxVal,
                onValueChange = {
                    maxVal = it
                    validateAndPublish()
                },
                clearable = true,
                numeric = true
            )

            NumericParameterTextField(
                title = "Details",
                placeholder = "Optional",
                value = details,
                onValueChange = {
                    details = it
                    validateAndPublish()
                },
                clearable = true
            )

            if (generalError.isNotBlank()) {
                Text(
                    text = generalError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun validateNumericValues(
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

@Composable
private fun NumericParameterTextField(
    title: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    clearable: Boolean = false,
    isRequired: Boolean = false,
    numeric: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TraitEditorTitle(title)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = {
                    if (!numeric || it.isBlank() || it.toDoubleOrNull() != null) {
                        onValueChange(it)
                    }
                },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (clearable) {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_transfer_cancelled),
                            contentDescription = "Clear text",
                            tint = Color.Gray
                        )
                    }
                } else if (isRequired) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_transfer_error),
                        contentDescription = "Required field",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        TraitEditorUnderline()
    }
}
