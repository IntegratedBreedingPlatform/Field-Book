package com.fieldbook.shared.screens.collect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldbook.shared.database.repository.ObservationUnitAttributeRepository
import com.fieldbook.shared.database.repository.ObservationUnitPropertyRepository
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.act_collect_barcode_button_content_description
import com.fieldbook.shared.generated.resources.act_collect_delete_value_button_content_description
import com.fieldbook.shared.generated.resources.chevron_left
import com.fieldbook.shared.generated.resources.chevron_right
import com.fieldbook.shared.generated.resources.dialog_fragment_summary_neutral_button
import com.fieldbook.shared.generated.resources.fragment_summary_filter_title
import com.fieldbook.shared.generated.resources.fragment_summary_next_button_text
import com.fieldbook.shared.generated.resources.fragment_summary_prev_button_text
import com.fieldbook.shared.generated.resources.fragment_summary_toolbar_title
import com.fieldbook.shared.generated.resources.ic_field
import com.fieldbook.shared.generated.resources.ic_lock_clock
import com.fieldbook.shared.generated.resources.ic_tb_barcode
import com.fieldbook.shared.generated.resources.ic_tb_delete
import com.fieldbook.shared.generated.resources.ic_tb_details
import com.fieldbook.shared.generated.resources.ic_tb_lock
import com.fieldbook.shared.generated.resources.ic_tb_search
import com.fieldbook.shared.generated.resources.ic_tb_unlock
import com.fieldbook.shared.generated.resources.ic_transfer_error
import com.fieldbook.shared.generated.resources.main_toolbar_search
import com.fieldbook.shared.generated.resources.menu_fragment_summary_filter_title
import com.fieldbook.shared.generated.resources.pencil
import com.fieldbook.shared.generated.resources.preferences_appearance_toolbar_customize_summary
import com.fieldbook.shared.preferences.GeneralKeys
import com.fieldbook.shared.preferences.PreferenceKeys
import com.fieldbook.shared.preferences.loadStringSetPreference
import com.fieldbook.shared.preferences.loadToolbarCustomizationPreference
import com.fieldbook.shared.preferences.persistStringSetPreference
import com.fieldbook.shared.screens.ScannerScreen
import com.fieldbook.shared.screens.collect.traits.PhotoTrait
import com.fieldbook.shared.screens.collect.traits.PhotoTraitDisplayMode
import com.fieldbook.shared.screens.datagrid.DataGridScreen
import com.fieldbook.shared.traits.Formats
import com.fieldbook.shared.traits.Scannable
import com.fieldbook.shared.utilities.CategoryJsonUtil
import com.russhwolf.settings.Settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * KMP version of CollectActivity main screen logic.
 * UI and business logic will be migrated here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectScreen(
    modifier: Modifier = Modifier,
    viewModel: CollectScreenViewModel = viewModel(
        factory = collectScreenViewModelFactory()
    ),
    onBack: (() -> Unit)? = null,
) {
    val collectState by viewModel.uiState.collectAsState()
    var isCameraFullscreen by remember { mutableStateOf(false) }
    var isBarcodeScannerFullscreen by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showDataGrid by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val settings = remember { Settings() }
    val dataGridEnabled = remember {
        settings.getBoolean(PreferenceKeys.DATAGRID_SETTING, false)
    }
    val toolbarCustomization = loadCollectToolbarCustomization(settings)
    val searchEnabled = "search" in toolbarCustomization
    val summaryEnabled = "summary" in toolbarCustomization
    val lockEnabled = "lockData" in toolbarCustomization
    val handleBack: () -> Unit = {
        viewModel.persistCurrentSelection()
        onBack?.invoke()
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.persistCurrentSelection()
        }
    }

    val currentTrait = collectState.traits.getOrNull(collectState.currentTraitIndex)
    val currentValues = currentTrait?.id?.let { collectState.traitValues[it] } ?: emptyList()
    val currentFormat = currentTrait?.format?.let { formatStr ->
        Formats.entries.find { it.databaseName.equals(formatStr, ignoreCase = true) }
    }
    val isCurrentTraitCamera = currentFormat?.isCamera == true
    val canDeleteCurrentValue = viewModel.hasCurrentTraitValue() && !viewModel.isCurrentObservationLocked()
    var summaryFilter by remember(viewModel.studyId) {
        mutableStateOf(loadCollectSummaryFilter(settings, viewModel.studyId))
    }
    val observationUnitPropertyRepository = remember { ObservationUnitPropertyRepository() }
    val summaryAttributeLabels = remember(viewModel.studyId) {
        ObservationUnitAttributeRepository().getAllNames(viewModel.studyId.toLong())
    }
    val summaryAttributeValues = remember(
        collectState.currentUnitIndex,
        collectState.units,
        summaryAttributeLabels,
        viewModel.uniqueId
    ) {
        val unit = collectState.units.getOrNull(collectState.currentUnitIndex)
        if (unit == null) {
            emptyMap()
        } else {
            observationUnitPropertyRepository.getAttributeValuesForUnit(
                uniqueName = viewModel.uniqueId,
                unitId = unit.observation_unit_db_id,
                attributeLabels = summaryAttributeLabels
            )
        }
    }
    val summaryDefinitions = remember(
        summaryAttributeLabels,
        collectState.currentUnitIndex,
        collectState.units,
        collectState.traits
    ) {
        buildCollectSummaryDefinitions(
            attributeLabels = summaryAttributeLabels,
            state = collectState
        )
    }
    val summaryItems = remember(
        summaryAttributeLabels,
        collectState.currentUnitIndex,
        collectState.units,
        collectState.traits,
        collectState.traitValues,
        collectState.cRange,
        summaryFilter
    ) {
        buildCollectSummaryItems(
            viewModel = viewModel,
            state = collectState,
            attributeLabels = summaryAttributeLabels,
            attributeValues = summaryAttributeValues,
            showCategoryLabels = settings.getString(PreferenceKeys.LABELVAL_CUSTOMIZE, "value") != "value",
            filter = summaryFilter
        )
    }

    LaunchedEffect(viewModel.inputValidationMessage) {
        viewModel.inputValidationMessage?.let { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearInputValidationMessage()
        }
    }

    if (isCameraFullscreen && isCurrentTraitCamera) {
        Surface(modifier = modifier.fillMaxSize()) {
            PhotoTrait(
                state = collectState,
                values = currentValues,
                onPhotoCaptured = { viewModel.addCurrentTraitValue(it) },
                onPhotoDeleted = { viewModel.deleteCurrentTraitValue(it) },
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
                displayMode = PhotoTraitDisplayMode.FULLSCREEN,
                onCollapseRequest = { isCameraFullscreen = false }
            )
        }
        return
    }

    if (isBarcodeScannerFullscreen) {
        Surface(modifier = modifier.fillMaxSize()) {
            ScannerScreen(
                onBack = { isBarcodeScannerFullscreen = false },
                onResult = { scannedValue ->
                    val traitFormat = currentTrait?.format
                        ?.let { Formats.findTrait(it) }
                    val valueToStore = when (traitFormat) {
                        is Scannable -> traitFormat.preprocess(scannedValue)
                        else -> scannedValue
                    }
                    viewModel.updateCurrentTraitValue(valueToStore)
                    isBarcodeScannerFullscreen = false
                }
            )
        }
        return
    }

    if (showDataGrid) {
        DataGridScreen(
            modifier = modifier,
            activePlotIndex = collectState.currentUnitIndex + 1,
            activeTraitIndex = collectState.currentTraitIndex + 1,
            onBack = { showDataGrid = false },
            onSelection = { selection ->
                viewModel.applyDataGridSelection(selection)
                showDataGrid = false
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                CollectValidationSnackbar(data = data)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(text = "Collect Data") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = handleBack,
                            enabled = !collectState.collectInteractionLocked
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (searchEnabled) {
                        IconButton(
                            onClick = { showSearchDialog = true },
                            enabled = !collectState.collectInteractionLocked
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_tb_search),
                                contentDescription = stringResource(Res.string.main_toolbar_search)
                            )
                        }
                    }
                    if (dataGridEnabled) {
                        IconButton(
                            onClick = { showDataGrid = true },
                            enabled = !collectState.collectInteractionLocked
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_field),
                                contentDescription = "Data Grid"
                            )
                        }
                    }
                    if (summaryEnabled) {
                        IconButton(
                            onClick = { showSummaryDialog = true },
                            enabled = !collectState.collectInteractionLocked
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_tb_details),
                                contentDescription = stringResource(Res.string.preferences_appearance_toolbar_customize_summary)
                            )
                        }
                    }
                    if (lockEnabled) {
                        IconButton(
                            onClick = { viewModel.cycleDataLockState() }
                        ) {
                            val lockIcon = when (viewModel.dataLockState) {
                                CollectDataLockState.UNLOCKED -> Res.drawable.ic_tb_unlock
                                CollectDataLockState.LOCKED -> Res.drawable.ic_tb_lock
                                CollectDataLockState.FROZEN -> Res.drawable.ic_lock_clock
                            }
                            Icon(
                                painter = painterResource(lockIcon),
                                contentDescription = "Data lock"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            CollectBottomBar(
                canScanBarcode = !viewModel.isCurrentObservationLocked(),
                canSetNa = !viewModel.isCurrentObservationLocked(),
                canDeleteCurrentValue = canDeleteCurrentValue,
                onScanBarcode = { isBarcodeScannerFullscreen = true },
                onSetNa = viewModel::setCurrentTraitNa,
                onDelete = viewModel::clearCurrentTraitValue
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (collectState.unitLoading || collectState.traitLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (collectState.unitError != null || collectState.traitError != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${collectState.unitError ?: collectState.traitError}")
                    }
                } else if (collectState.units.isNotEmpty() && collectState.traits.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(8.dp))
                        InfoBar(
                            state = collectState,
                            viewModel = viewModel
                        )
                        Spacer(Modifier.height(8.dp))
                        TraitBox(
                            state = collectState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        RangeBox(
                            state = collectState,
                            viewModel = viewModel
                        )
                        CollectInput(
                            state = collectState,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f),
                            onExpandPhotoTrait = { isCameraFullscreen = true }
                        )
                    }
                }
            }
        }
    }

    if (showSummaryDialog) {
        CollectSummaryDialog(
            title = stringResource(Res.string.fragment_summary_toolbar_title),
            items = summaryItems,
            canNavigatePrevious = collectState.units.isNotEmpty(),
            canNavigateNext = collectState.units.isNotEmpty(),
            onFilterUpdated = { filter ->
                summaryFilter = filter
                persistCollectSummaryFilter(settings, viewModel.studyId, filter)
            },
            filterOptions = summaryDefinitions,
            initialFilter = summaryFilter,
            onTraitSelected = { traitId ->
                val traitIndex = collectState.traits.indexOfFirst { it.id == traitId }
                if (traitIndex >= 0) {
                    viewModel.updateCurrentTraitIndex(traitIndex)
                }
                showSummaryDialog = false
            },
            onPrevious = { viewModel.goToPreviousUnit() },
            onNext = { viewModel.goToNextUnit() },
            onDismiss = { showSummaryDialog = false }
        )
    }

    CollectSearchDialog(
        controller = viewModel,
        visible = showSearchDialog,
        onDismiss = { showSearchDialog = false }
    )
}

private data class CollectSummaryDefinition(
    val label: String,
    val traitId: Long? = null,
)

private data class CollectSummaryItem(
    val label: String,
    val value: String,
    val traitId: Long? = null,
)

private data class CollectSummaryFilter(
    val attributeLabels: Set<String>? = null,
    val traitIds: Set<Long>? = null,
)

private fun buildCollectSummaryDefinitions(
    attributeLabels: List<String>,
    state: CollectUiState,
): List<CollectSummaryDefinition> {
    if (state.units.isEmpty()) return emptyList()

    val traitDefinitions = state.traits.mapNotNull { trait ->
        val traitId = trait.id ?: return@mapNotNull null
        CollectSummaryDefinition(label = trait.name, traitId = traitId)
    }.sortedBy { it.label }

    val attributeDefinitions = attributeLabels
        .map { CollectSummaryDefinition(label = it) }
        .sortedBy { it.label }

    return attributeDefinitions + traitDefinitions
}

private fun buildCollectSummaryItems(
    viewModel: CollectScreenViewModel,
    state: CollectUiState,
    attributeLabels: List<String>,
    attributeValues: Map<String, String>,
    showCategoryLabels: Boolean,
    filter: CollectSummaryFilter,
): List<CollectSummaryItem> {
    if (state.units.getOrNull(state.currentUnitIndex) == null) return emptyList()

    val visibleAttributeLabels = filter.attributeLabels

    val attributeItems = attributeLabels
        .map { label ->
            CollectSummaryItem(
                label = label,
                value = resolveSummaryAttributeValue(viewModel, state, label, attributeValues).ifBlank { "" }
            )
        }
        .filter { visibleAttributeLabels == null || it.label in visibleAttributeLabels }
        .sortedBy { it.label }

    val visibleTraitIds = filter.traitIds

    val traitItems = state.traits.mapNotNull { trait ->
        val traitId = trait.id ?: return@mapNotNull null
        if (visibleTraitIds != null && traitId !in visibleTraitIds) return@mapNotNull null
        val rawValue = state.traitValues[traitId]?.joinToString("\n").orEmpty()

        val displayValue = when (trait.format?.lowercase()) {
            Formats.CATEGORICAL.databaseName,
            Formats.MULTI_CATEGORICAL.databaseName -> {
                try {
                    CategoryJsonUtil.flattenMultiCategoryValue(
                        CategoryJsonUtil.decode(rawValue),
                        showLabel = showCategoryLabels
                    ).ifBlank { rawValue }
                } catch (_: Exception) {
                    rawValue
                }
            }

            else -> rawValue
        }

        CollectSummaryItem(
            label = trait.name,
            value = displayValue.ifBlank { "-" },
            traitId = traitId
        )
    }.sortedBy { it.label }

    return attributeItems + traitItems
}

private fun resolveSummaryAttributeValue(
    viewModel: CollectScreenViewModel,
    state: CollectUiState,
    label: String,
    attributeValues: Map<String, String>,
): String {
    attributeValues[label]?.let { return it }

    return when (label) {
        viewModel.uniqueId -> state.cRange.uniqueId
        viewModel.primaryId -> state.cRange.primaryId
        viewModel.secondaryId -> state.cRange.secondaryId
        else -> ""
    }
}

@Composable
private fun CollectSummaryDialog(
    title: String,
    items: List<CollectSummaryItem>,
    filterOptions: List<CollectSummaryDefinition>,
    initialFilter: CollectSummaryFilter,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    onFilterUpdated: (CollectSummaryFilter) -> Unit,
    onTraitSelected: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showFilterDialog by remember(initialFilter, filterOptions) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.pencil),
                            contentDescription = stringResource(Res.string.menu_fragment_summary_filter_title)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (items.isEmpty()) {
                    Text(text = "-")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(items) { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        enabled = item.traitId != null,
                                        onClick = { item.traitId?.let(onTraitSelected) }
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 1.dp,
                                shadowElevation = 0.dp,
                                border = if (item.traitId != null) {
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                    )
                                } else {
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.value,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (item.traitId != null) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (item.traitId != null) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                    if (item.traitId != null) {
                                        Icon(
                                            painter = painterResource(Res.drawable.chevron_right),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = canNavigatePrevious,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.chevron_left),
                            contentDescription = stringResource(Res.string.fragment_summary_prev_button_text)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onNext,
                        enabled = canNavigateNext,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.chevron_right),
                            contentDescription = stringResource(Res.string.fragment_summary_next_button_text)
                        )
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        CollectSummaryFilterDialog(
            title = stringResource(Res.string.fragment_summary_filter_title),
            toggleAllLabel = stringResource(Res.string.dialog_fragment_summary_neutral_button),
            options = filterOptions,
            initialFilter = initialFilter,
            onApply = {
                onFilterUpdated(it)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun CollectSummaryFilterDialog(
    title: String,
    toggleAllLabel: String,
    options: List<CollectSummaryDefinition>,
    initialFilter: CollectSummaryFilter,
    onApply: (CollectSummaryFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialAttributeSelection = remember(initialFilter, options) {
        options.filter { it.traitId == null }.associate { option ->
            option.label to (initialFilter.attributeLabels?.contains(option.label) ?: true)
        }.toMutableMap()
    }
    val initialTraitSelection = remember(initialFilter, options) {
        options.filter { it.traitId != null }.associate { option ->
            option.traitId!! to (initialFilter.traitIds?.contains(option.traitId) ?: true)
        }.toMutableMap()
    }

    var attributeSelection by remember(initialFilter, options) { mutableStateOf(initialAttributeSelection) }
    var traitSelection by remember(initialFilter, options) { mutableStateOf(initialTraitSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options) { option ->
                    val checked = if (option.traitId == null) {
                        attributeSelection[option.label] ?: true
                    } else {
                        traitSelection[option.traitId] ?: true
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (option.traitId == null) {
                                    attributeSelection = attributeSelection.toMutableMap().apply {
                                        put(option.label, !checked)
                                    }
                                } else {
                                    traitSelection = traitSelection.toMutableMap().apply {
                                        put(option.traitId, !checked)
                                    }
                                }
                            }
                            .padding(vertical = 1.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (option.traitId == null) {
                                    attributeSelection = attributeSelection.toMutableMap().apply {
                                        put(option.label, isChecked)
                                    }
                                } else {
                                    traitSelection = traitSelection.toMutableMap().apply {
                                        put(option.traitId, isChecked)
                                    }
                                }
                            }
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val enableAll = attributeSelection.values.any { !it } || traitSelection.values.any { !it }
                        attributeSelection = attributeSelection.mapValues { enableAll }.toMutableMap()
                        traitSelection = traitSelection.mapValues { enableAll }.toMutableMap()
                    }
                ) {
                    Text(toggleAllLabel)
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = {
                        onApply(
                            CollectSummaryFilter(
                                attributeLabels = attributeSelection.filterValues { it }.keys,
                                traitIds = traitSelection.filterValues { it }.keys
                            )
                        )
                    }
                ) {
                    Text("OK")
                }
            }
        }
    )
}

private fun loadCollectSummaryFilter(
    settings: Settings,
    studyId: Int,
): CollectSummaryFilter {
    val attributeKey = "${GeneralKeys.SUMMARY_FILTER_ATTRIBUTES.key}.$studyId"
    val traitKey = "${GeneralKeys.SUMMARY_FILTER_TRAITS.key}.$studyId"

    val attributeLabels = loadStringSetPreference(
        settings = settings,
        key = attributeKey,
        legacySeparators = charArrayOf('\n')
    )

    val traitIds = loadStringSetPreference(
        settings = settings,
        key = traitKey,
        legacySeparators = charArrayOf(',')
    )
        ?.mapNotNull { it.trim().toLongOrNull() }
        ?.toSet()

    return CollectSummaryFilter(
        attributeLabels = attributeLabels,
        traitIds = traitIds
    )
}

private fun loadCollectToolbarCustomization(settings: Settings): Set<String> {
    return loadToolbarCustomizationPreference(
        settings = settings,
        key = PreferenceKeys.TOOLBAR_CUSTOMIZE,
        defaultOptions = setOf("search", "summary", "lockData")
    )
}

private fun persistCollectSummaryFilter(
    settings: Settings,
    studyId: Int,
    filter: CollectSummaryFilter,
) {
    val attributeKey = "${GeneralKeys.SUMMARY_FILTER_ATTRIBUTES.key}.$studyId"
    val traitKey = "${GeneralKeys.SUMMARY_FILTER_TRAITS.key}.$studyId"

    persistStringSetPreference(
        settings = settings,
        key = attributeKey,
        values = filter.attributeLabels ?: emptySet()
    )
    persistStringSetPreference(
        settings = settings,
        key = traitKey,
        values = filter.traitIds?.map { it.toString() }?.toSet() ?: emptySet()
    )
}

@Composable
private fun CollectBottomBar(
    canScanBarcode: Boolean,
    canSetNa: Boolean,
    canDeleteCurrentValue: Boolean,
    onScanBarcode: () -> Unit,
    onSetNa: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onScanBarcode,
                    enabled = canScanBarcode,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_tb_barcode),
                        contentDescription = stringResource(Res.string.act_collect_barcode_button_content_description),
                        tint = if (canScanBarcode) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                        }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = canSetNa, onClick = onSetNa),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NA",
                    color = if (canSetNa) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(if (canSetNa) 1f else 0.38f)
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = onDelete,
                    enabled = canDeleteCurrentValue,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_tb_delete),
                        contentDescription = stringResource(Res.string.act_collect_delete_value_button_content_description),
                        tint = if (canDeleteCurrentValue) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectValidationSnackbar(data: SnackbarData) {
    val shape = RoundedCornerShape(22.dp)

    Surface(
        color = Color(0xFFFFF3F2),
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        shape = shape,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(shape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Surface(
                color = Color(0xFFD94B4B),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_transfer_error),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.size(14.dp))
            Text(
                text = data.visuals.message,
                color = Color(0xFF3A1F1F),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
