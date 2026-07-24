package com.fieldbook.shared.screens.collect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.database.repository.ObservationUnitAttributeRepository
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.chevron_left
import com.fieldbook.shared.generated.resources.chevron_right
import com.fieldbook.shared.generated.resources.dialog_att_chooser_title_default
import com.fieldbook.shared.generated.resources.dialog_cancel
import com.fieldbook.shared.generated.resources.dialog_quick_goto_go
import com.fieldbook.shared.generated.resources.dialog_quick_goto_title
import com.fieldbook.shared.generated.resources.search_results_missing
import com.fieldbook.shared.preferences.PreferenceKeys
import com.russhwolf.settings.Settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlotsProgressBar(
    currentIndex: Int,
    total: Int,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (visible && total > 0) {
        val progress = (currentIndex + 1).toFloat() / total
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = Color.LightGray
        )
    }
}

@Composable
fun RangeBox(
    state: CollectUiState,
    viewModel: CollectScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val settings = remember { Settings() }
    val showRangeProgressBar = settings.getBoolean(PreferenceKeys.RANGE_PROGRESS_BAR, true)
    val attributeRepository = remember { ObservationUnitAttributeRepository() }
    val availableAttributes = remember(viewModel.studyId) {
        attributeRepository.getAllNames(viewModel.studyId.toLong())
    }
    var attributeTarget by remember { mutableStateOf<RangeAttributeTarget?>(null) }
    var quickGoToPrimaryClicked by remember { mutableStateOf<Boolean?>(null) }
    val noResultsMessage = stringResource(Res.string.search_results_missing)

    LaunchedEffect(state.currentUnitIndex, state.rangeID, viewModel.primaryId, viewModel.secondaryId) {
        val id = state.rangeID.getOrNull(state.currentUnitIndex)
        if (id != null) {
            viewModel.updateCurrentRange(id)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showRangeProgressBar) {
            PlotsProgressBar(
                currentIndex = state.currentUnitIndex,
                total = state.units.size,
                visible = true
            )
            Spacer(Modifier.size(16.dp))
        } else {
            Spacer(Modifier.size(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.goToPreviousUnit() },
                enabled = !state.collectInteractionLocked && state.units.isNotEmpty(),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.chevron_left),
                    contentDescription = "Previous Unit",
                    modifier = Modifier.size(56.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                RangeDisplayRow(
                    label = viewModel.primaryId,
                    value = state.cRange.primaryId,
                    enabled = !state.collectInteractionLocked,
                    onLabelClick = { attributeTarget = RangeAttributeTarget.PRIMARY },
                    onValueClick = { quickGoToPrimaryClicked = true },
                )
                RangeDisplayRow(
                    label = viewModel.secondaryId,
                    value = state.cRange.secondaryId,
                    enabled = !state.collectInteractionLocked,
                    onLabelClick = { attributeTarget = RangeAttributeTarget.SECONDARY },
                    onValueClick = { quickGoToPrimaryClicked = false },
                )
            }
            IconButton(
                onClick = { viewModel.goToNextUnit() },
                enabled = !state.collectInteractionLocked && state.units.isNotEmpty(),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.chevron_right),
                    contentDescription = "Next Unit",
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }

    attributeTarget?.let { target ->
        RangeAttributeChooserDialog(
            attributes = availableAttributes,
            selectedAttribute = when (target) {
                RangeAttributeTarget.PRIMARY -> viewModel.primaryId
                RangeAttributeTarget.SECONDARY -> viewModel.secondaryId
            },
            onAttributeSelected = { attribute ->
                when (target) {
                    RangeAttributeTarget.PRIMARY -> viewModel.updatePrimaryAttribute(attribute)
                    RangeAttributeTarget.SECONDARY -> viewModel.updateSecondaryAttribute(attribute)
                }
                attributeTarget = null
            },
            onDismiss = { attributeTarget = null }
        )
    }

    quickGoToPrimaryClicked?.let { primaryClicked ->
        QuickGoToRangeDialog(
            primaryLabel = viewModel.primaryId,
            secondaryLabel = viewModel.secondaryId,
            primaryClicked = primaryClicked,
            onGo = { primaryValue, secondaryValue ->
                val moved = viewModel.moveToRange(primaryValue, secondaryValue)
                if (!moved) viewModel.showInputValidationMessage(noResultsMessage)
                quickGoToPrimaryClicked = null
            },
            onDismiss = { quickGoToPrimaryClicked = null }
        )
    }
}

private enum class RangeAttributeTarget {
    PRIMARY,
    SECONDARY,
}

@Composable
private fun RangeDisplayRow(
    label: String,
    value: String,
    enabled: Boolean,
    onLabelClick: () -> Unit,
    onValueClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, onClick = onLabelClick)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, onClick = onValueClick)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun RangeAttributeChooserDialog(
    attributes: List<String>,
    selectedAttribute: String,
    onAttributeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_att_chooser_title_default)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                items(attributes) { attribute ->
                    val selected = attribute == selectedAttribute
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAttributeSelected(attribute) }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onAttributeSelected(attribute) }
                        )
                        Text(
                            text = attribute,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun QuickGoToRangeDialog(
    primaryLabel: String,
    secondaryLabel: String,
    primaryClicked: Boolean,
    onGo: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var primaryValue by remember(primaryClicked) { mutableStateOf("") }
    var secondaryValue by remember(primaryClicked) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_quick_goto_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = primaryValue,
                    onValueChange = { primaryValue = it },
                    label = { Text(primaryLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = secondaryValue,
                    onValueChange = { secondaryValue = it },
                    label = { Text(secondaryLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onGo(primaryValue, secondaryValue) }) {
                Text(stringResource(Res.string.dialog_quick_goto_go))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        }
    )
}
