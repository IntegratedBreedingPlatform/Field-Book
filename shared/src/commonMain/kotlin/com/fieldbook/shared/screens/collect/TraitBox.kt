package com.fieldbook.shared.screens.collect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.chevron_left
import com.fieldbook.shared.generated.resources.chevron_right
import com.fieldbook.shared.generated.resources.dialog_ok
import com.fieldbook.shared.generated.resources.select_trait
import com.fieldbook.shared.preferences.PreferenceKeys
import com.russhwolf.settings.Settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TraitBox(
    state: CollectUiState,
    viewModel: CollectScreenViewModel,
    modifier: Modifier = Modifier
) {
    val settings = remember { Settings() }
    val showTraitProgressBar = settings.getBoolean(PreferenceKeys.TRAITS_PROGRESS_BAR, true)
    var showTraitPickerDialog by remember { mutableStateOf(false) }
    val traitPickerEnabled = !state.collectInteractionLocked && state.traits.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.goToPreviousTrait() },
                enabled = !state.collectInteractionLocked && state.traits.isNotEmpty(),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.chevron_left),
                    contentDescription = "Previous Trait",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            val trait = state.traits.getOrNull(state.currentTraitIndex)
            Text(
                trait?.name ?: "-",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        enabled = traitPickerEnabled,
                        onClick = { showTraitPickerDialog = true }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { viewModel.goToNextTrait() },
                enabled = !state.collectInteractionLocked && state.traits.isNotEmpty(),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.chevron_right),
                    contentDescription = "Next Trait",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary

                )
            }
        }
        val trait = state.traits.getOrNull(state.currentTraitIndex)
        val traitDetails = trait?.details?.takeIf { it.isNotBlank() }
        if (traitDetails != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = traitDetails,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
        }
        if (showTraitProgressBar) {
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatusBar(
                    state = state,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showTraitPickerDialog) {
        TraitPickerDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showTraitPickerDialog = false }
        )
    }
}

@Composable
private fun TraitPickerDialog(
    state: CollectUiState,
    viewModel: CollectScreenViewModel,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.select_trait)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                itemsIndexed(state.traits) { index, trait ->
                    val selected = index == state.currentTraitIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.updateCurrentTraitIndex(index)
                                onDismiss()
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                viewModel.updateCurrentTraitIndex(index)
                                onDismiss()
                            }
                        )
                        Text(
                            text = trait.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_ok))
            }
        }
    )
}
