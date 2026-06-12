package com.fieldbook.shared.traits

import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.material3.Switch
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
import com.fieldbook.shared.generated.resources.ic_trait_text
import com.fieldbook.shared.generated.resources.ic_transfer_cancelled
import com.fieldbook.shared.generated.resources.ic_transfer_error
import com.fieldbook.shared.generated.resources.traits_create_close_keyboard
import com.fieldbook.shared.generated.resources.traits_format_text
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

class TextFormat : TraitFormat(
    format = Formats.TEXT,
    nameStringResource = Res.string.traits_format_text,
    iconDrawableResource = Res.drawable.ic_trait_text,
) {

    @Composable
    override fun ParametersEditor(trait: TraitObject, onTraitChange: (TraitObject) -> Unit) {
        var traitName by remember { mutableStateOf(trait.name) }
        var defaultValue by remember { mutableStateOf(trait.defaultValue ?: "") }
        var details by remember { mutableStateOf(trait.details ?: "") }
        var closeKeyboardOnOpen by remember { mutableStateOf(trait.closeKeyboardOnOpen) }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextParameterField(
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

            TextParameterField(
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

            TextParameterField(
                title = "Default",
                placeholder = "Optional",
                value = defaultValue,
                onValueChange = {
                    defaultValue = it
                    trait.defaultValue = it.ifBlank { null }
                    onTraitChange(trait)
                },
                clearable = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TraitEditorTitle(stringResource(Res.string.traits_create_close_keyboard))
                Switch(
                    checked = closeKeyboardOnOpen,
                    onCheckedChange = {
                        closeKeyboardOnOpen = it
                        trait.closeKeyboardOnOpen = it
                        onTraitChange(trait)
                    }
                )
            }
        }
    }
}

@Composable
private fun TextParameterField(
    title: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    clearable: Boolean = false,
    isRequired: Boolean = false,
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
                onValueChange = onValueChange,
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
