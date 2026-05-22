package com.fieldbook.shared.screens.collect.traits

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.database.models.TraitObject
import com.fieldbook.shared.preferences.PreferenceKeys
import com.fieldbook.shared.theme.Button
import com.fieldbook.shared.utilities.BrAPIScaleValidValuesCategories
import com.fieldbook.shared.utilities.CategoryJsonUtil
import com.russhwolf.settings.Settings

@Composable
fun CategoricalTrait(
    trait: TraitObject?,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    multi: Boolean = false,
) {
    val labelValPref = remember { Settings() }
        .getString(PreferenceKeys.LABELVAL_CUSTOMIZE, "value")
    val showLabel = labelValPref == "label"

    // Parse the trait's category definition. Try JSON first, then fall back to legacy slash-separated format.
    val categories: ArrayList<BrAPIScaleValidValuesCategories> =
        CategoryJsonUtil.decodeDefinition(trait?.categories)

    // Compute the displayed values from the stored value (which may be JSON or legacy raw)
    val displayedValues: List<String> = try {
        val scale = CategoryJsonUtil.decode(value)
        if (scale.isNotEmpty()) {
            if (showLabel) scale.mapNotNull { it.label ?: it.value }
            else scale.mapNotNull { it.value ?: it.label }
        } else if (!multi && value.isNotBlank()) {
            // Legacy single value
            listOf(value)
        } else {
            emptyList()
        }
    } catch (_: Exception) {
        if (!multi && value.isNotBlank()) listOf(value) else emptyList()
    }

    // Display buttons in a grid (3 columns per row)
    val columns = 3

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        state = rememberLazyGridState(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        items(categories) { cat ->
            val buttonText = if (showLabel) cat.label ?: cat.value ?: "" else cat.value ?: cat.label ?: ""
            val isSelected = displayedValues.contains(buttonText)
            Button(
                onClick = {
                    if (multi) {
                        // Multi-select: add or remove from selection
                        val scale = CategoryJsonUtil.decode(value).toMutableList()
                        val alreadySelected = scale.any {
                            if (showLabel) (it.label ?: it.value) == buttonText
                            else (it.value ?: it.label) == buttonText
                        }
                        if (alreadySelected) {
                            val newScale = scale.filterNot {
                                if (showLabel) (it.label ?: it.value) == buttonText
                                else (it.value ?: it.label) == buttonText
                            }
                            onValueChange(CategoryJsonUtil.encode(ArrayList(newScale)))
                        } else {
                            scale.add(cat)
                            onValueChange(CategoryJsonUtil.encode(ArrayList(scale)))
                        }
                    } else {
                        // Single-select: toggle selection
                        if (isSelected) {
                            onValueChange("")
                        } else {
                            val scale = arrayListOf(cat)
                            onValueChange(CategoryJsonUtil.encode(scale))
                        }
                    }
                },
                selected = isSelected,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(buttonText)
            }
        }
    }
}
