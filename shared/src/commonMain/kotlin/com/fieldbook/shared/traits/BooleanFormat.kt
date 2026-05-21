package com.fieldbook.shared.traits

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.database.models.TraitObject
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.ic_trait_boolean
import com.fieldbook.shared.generated.resources.traits_format_boolean

class BooleanFormat : TraitFormat(
    format = Formats.BOOLEAN,
    nameStringResource = Res.string.traits_format_boolean,
    iconDrawableResource = Res.drawable.ic_trait_boolean,
) {

    @Composable
    override fun ParametersEditor(trait: TraitObject, onTraitChange: (TraitObject) -> Unit) {
        var traitName by remember { mutableStateOf(trait.name) }
        var selected by remember { mutableStateOf(trait.defaultValue?.lowercase()) }
        var details by remember { mutableStateOf(trait.details ?: "") }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TraitEditorTextField(
                title = "Name",
                placeholder = "Enter trait name",
                value = traitName,
                onValueChange = {
                    traitName = it
                    trait.name = it
                    onTraitChange(trait)
                },
                clearable = true,
                isRequired = true
            )

            TraitEditorTitle("Default")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    "false" to "False",
                    null to "Unset",
                    "true" to "True"
                ).forEach { (value, label) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == value,
                            onClick = {
                                selected = value
                                trait.defaultValue = value
                                onTraitChange(trait)
                            }
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 4.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            TraitEditorTextField(
                title = "Details",
                placeholder = "Optional",
                value = details,
                onValueChange = {
                    details = it
                    trait.details = it.ifBlank { null }
                    onTraitChange(trait)
                },
                clearable = true
            )
        }
    }
}
