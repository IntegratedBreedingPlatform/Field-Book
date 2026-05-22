package com.fieldbook.shared.screens.trait

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldbook.shared.database.models.TraitObject
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.dialog_back
import com.fieldbook.shared.generated.resources.dialog_close
import com.fieldbook.shared.generated.resources.dialog_confirm
import com.fieldbook.shared.generated.resources.dialog_new_trait_observations_exist_error
import com.fieldbook.shared.generated.resources.dialog_no
import com.fieldbook.shared.generated.resources.dialog_yes
import com.fieldbook.shared.theme.AlertDialog
import com.fieldbook.shared.theme.Dialog
import com.fieldbook.shared.theme.TextButton
import com.fieldbook.shared.traits.Formats
import com.fieldbook.shared.traits.TraitEditorTitle
import com.fieldbook.shared.traits.TraitEditorTextField
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class TraitCreatorStep {
    ChooseFormat,
    NameDetails
}

@Composable
fun TraitCreatorDialog(
    initialTrait: TraitObject? = null,
    observationsExistOverride: Boolean? = null,
    onDismiss: () -> Unit,
    onSuccess: (TraitObject) -> Unit,
    viewModel: TraitEditorScreenViewModel = viewModel(
        factory = traitEditorScreenViewModelFactory()
    )
) {
    val isEditing = initialTrait != null
    val initialSelectedFormat = initialTrait?.format?.let { format ->
        Formats.supportedFormats().firstOrNull {
            it.databaseName.equals(format.trim(), ignoreCase = true)
        }
    }
    var selectedFormat by remember(initialTrait?.id) {
        mutableStateOf(initialSelectedFormat)
    }
    var currentStep by remember(initialTrait?.id) {
        mutableStateOf(
            if (isEditing && initialSelectedFormat != null) {
                TraitCreatorStep.NameDetails
            } else {
                TraitCreatorStep.ChooseFormat
            }
        )
    }
    var traitName by remember(initialTrait?.id) { mutableStateOf(initialTrait?.name ?: "") }
    var traitDetails by remember(initialTrait?.id) { mutableStateOf(initialTrait?.details ?: "") }
    var observationsExist by remember(initialTrait?.id, observationsExistOverride) {
        mutableStateOf(observationsExistOverride == true)
    }
    var showDiscardChangesWarning by remember(initialTrait?.id) { mutableStateOf(false) }

    LaunchedEffect(initialTrait?.id, observationsExistOverride) {
        observationsExist = observationsExistOverride == true ||
            (initialTrait?.id?.let(viewModel::traitHasObservations) == true)
    }

    when (currentStep) {
        TraitCreatorStep.ChooseFormat -> {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Trait Layout", modifier = Modifier.padding(8.dp))

                        val formats = Formats.supportedFormats()

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(formats) { format ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable {
                                            selectedFormat = format
                                            currentStep = TraitCreatorStep.NameDetails
                                        }
                                ) {
                                    val iconRes = format.getIcon()
                                    Image(
                                        painter = painterResource(iconRes),
                                        contentDescription = format.name,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(stringResource(format.getTraitFormatDefinition().nameStringResource))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }

        TraitCreatorStep.NameDetails -> {
            val traitState = remember(initialTrait?.id, selectedFormat) {
                (initialTrait?.copy() ?: TraitObject()).apply {
                    name = traitName
                    details = traitDetails
                    format = selectedFormat?.databaseName ?: initialTrait?.format?.trim()
                    visible = visible ?: "true"
                    traitDataSource = traitDataSource ?: "local"
                }
            }

            var paramError by remember { mutableStateOf("") }
            val scrollState = rememberScrollState()
            val useFormatSpecificEditor =
                selectedFormat == Formats.TEXT ||
                    selectedFormat == Formats.NUMERIC ||
                    selectedFormat == Formats.BOOLEAN

            if (showDiscardChangesWarning) {
                AlertDialog(
                    onDismissRequest = { showDiscardChangesWarning = false },
                    title = { Text(stringResource(Res.string.dialog_close)) },
                    text = { Text(stringResource(Res.string.dialog_confirm)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDiscardChangesWarning = false
                            onDismiss()
                        }) {
                            Text(stringResource(Res.string.dialog_yes))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardChangesWarning = false }) {
                            Text(stringResource(Res.string.dialog_no))
                        }
                    }
                )
            }

            val attemptDismiss = {
                if (traitHasChanges(initialTrait, traitState, selectedFormat)) {
                    showDiscardChangesWarning = true
                } else {
                    onDismiss()
                }
            }

            Dialog(
                onDismissRequest = attemptDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val title =
                                selectedFormat?.let { stringResource(it.getTraitFormatDefinition().nameStringResource) }
                            Box(modifier = Modifier.padding(8.dp)) {
                                TraitEditorTitle("${title ?: ""} Parameters")
                            }

                            if (isEditing && observationsExist) {
                                Text(
                                    text = stringResource(Res.string.dialog_new_trait_observations_exist_error),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (!useFormatSpecificEditor) {
                                TraitEditorTextField(
                                    title = "Name",
                                    placeholder = "Enter trait name",
                                    value = traitName,
                                    onValueChange = {
                                        traitName = it
                                        traitState.name = it
                                    },
                                    clearable = true,
                                    isRequired = true
                                )

                                TraitEditorTextField(
                                    title = "Details",
                                    placeholder = "Optional",
                                    value = traitDetails,
                                    onValueChange = {
                                        traitDetails = it
                                        traitState.details = it
                                    },
                                    clearable = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            selectedFormat?.getTraitFormatDefinition()
                                ?.ParametersEditor(traitState) { updated ->
                                    paramError = updated.additionalInfo ?: ""
                                    traitName = updated.name
                                    traitDetails = updated.details ?: ""
                                }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Row {
                                    if (!isEditing || !observationsExist) {
                                        TextButton(onClick = {
                                            currentStep = TraitCreatorStep.ChooseFormat
                                        }) {
                                            Text(stringResource(Res.string.dialog_back))
                                        }
                                    }

                                    TextButton(onClick = attemptDismiss) {
                                        Text("Cancel")
                                    }

                                    Button(onClick = {
                                        if (isEditing) {
                                            viewModel.updateTrait(traitState)
                                        } else {
                                            viewModel.insertTrait(traitState)
                                        }
                                        onSuccess(traitState)
                                    }, enabled = traitName.isNotBlank() && paramError.isBlank()) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun traitHasChanges(
    initialTrait: TraitObject?,
    currentTrait: TraitObject,
    selectedFormat: Formats?
): Boolean {
    if (initialTrait == null) {
        return currentTrait.name.isNotBlank() ||
            !currentTrait.defaultValue.isNullOrBlank() ||
            !currentTrait.minimum.isNullOrBlank() ||
            !currentTrait.maximum.isNullOrBlank() ||
            !currentTrait.details.isNullOrBlank() ||
            !currentTrait.categories.isNullOrBlank() ||
            selectedFormat != null
    }

    return initialTrait.name != currentTrait.name ||
        initialTrait.format != currentTrait.format ||
        (initialTrait.defaultValue ?: "") != (currentTrait.defaultValue ?: "") ||
        (initialTrait.minimum ?: "") != (currentTrait.minimum ?: "") ||
        (initialTrait.maximum ?: "") != (currentTrait.maximum ?: "") ||
        (initialTrait.details ?: "") != (currentTrait.details ?: "") ||
        (initialTrait.categories ?: "") != (currentTrait.categories ?: "") ||
        initialTrait.closeKeyboardOnOpen != currentTrait.closeKeyboardOnOpen
}
