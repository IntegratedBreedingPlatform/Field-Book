package com.fieldbook.shared.traits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.database.models.TraitObject
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.ic_plus
import com.fieldbook.shared.generated.resources.ic_transfer_cancelled
import com.fieldbook.shared.generated.resources.ic_tb_delete
import com.fieldbook.shared.generated.resources.ic_trait_categorical
import com.fieldbook.shared.generated.resources.trait_error_category_duplicate
import com.fieldbook.shared.generated.resources.traits_format_categorical
import com.fieldbook.shared.utilities.BrAPIScaleValidValuesCategories
import com.fieldbook.shared.utilities.CategoryJsonUtil
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

class CategoricalFormat : TraitFormat(
    format = Formats.CATEGORICAL,
    nameStringResource = Res.string.traits_format_categorical,
    iconDrawableResource = Res.drawable.ic_trait_categorical,
) {

    private data class CategoryItem(
        val id: Int,
        val value: String
    )

    @Composable
    override fun ParametersEditor(trait: TraitObject, onTraitChange: (TraitObject) -> Unit) {
        val initialList = CategoryJsonUtil.decodeDefinition(trait.categories)
            .map { it.value ?: it.label ?: "" }
            .filter { it.isNotEmpty() }
        val items = remember {
            mutableStateListOf<CategoryItem>().apply {
                addAll(initialList.mapIndexed { index, value -> CategoryItem(id = index, value = value) })
            }
        }
        var nextId by remember(items) { mutableStateOf(items.size) }
        var input by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }
        val categoryListHeight = 288.dp
        val duplicateCategoryMessage = stringResource(Res.string.trait_error_category_duplicate)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color.Black, RoundedCornerShape(6.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TraitEditorTitle("Categories")

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (input.isBlank()) {
                            Text(
                                text = "Type a category name to add",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                )

                if (input.isNotBlank()) {
                    IconButton(
                        onClick = { input = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_transfer_cancelled),
                            contentDescription = "Clear text",
                            tint = Color.Gray
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        val v = input.trim()
                        if (v.isNotEmpty()) {
                            val normalizedValue = v.lowercase()
                            val alreadyExists = items.any { existing ->
                                existing.value.trim().lowercase() == normalizedValue
                            }

                            if (alreadyExists) {
                                error = duplicateCategoryMessage
                            } else {
                                items.add(CategoryItem(id = nextId++, value = v))
                                input = ""
                                error = ""
                                trait.categories = encode(items)
                                onTraitChange(trait)
                            }
                        } else {
                            error = "Cannot add empty category"
                        }
                    },
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_plus),
                        contentDescription = "Add"
                    )
                }
            }

            TraitEditorUnderline()

            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = categoryListHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    items.forEachIndexed { _, item ->
                        key(item.id) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.value,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )

                                FilledIconButton(
                                    onClick = {
                                        items.removeAll { it.id == item.id }
                                        trait.categories = encode(items)
                                        onTraitChange(trait)
                                    },
                                    modifier = Modifier.size(34.dp),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete"
                                    )
                                }
                            }

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }

    private fun encode(items: SnapshotStateList<CategoryItem>): String = CategoryJsonUtil.encode(
        ArrayList(items.map { BrAPIScaleValidValuesCategories(label = it.value, value = it.value) })
    )

}
