package com.fieldbook.shared.screens.collect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fieldbook.shared.database.models.TraitObject
import com.fieldbook.shared.database.repository.ObservationUnitAttributeRepository
import com.fieldbook.shared.database.repository.ObservationUnitPropertyRepository
import com.fieldbook.shared.database.repository.TraitRepository
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.dialog_att_chooser_attributes
import com.fieldbook.shared.generated.resources.dialog_att_chooser_customize
import com.fieldbook.shared.generated.resources.dialog_att_chooser_other
import com.fieldbook.shared.generated.resources.dialog_att_chooser_traits
import com.fieldbook.shared.generated.resources.dialog_infobar_att_chooser_title
import com.fieldbook.shared.generated.resources.field_name_attribute
import com.fieldbook.shared.generated.resources.ic_infobar_circle
import com.fieldbook.shared.generated.resources.ic_infobar_circle_outline
import com.fieldbook.shared.generated.resources.ic_infobar_hexagon
import com.fieldbook.shared.generated.resources.ic_infobar_hexagon_outline
import com.fieldbook.shared.generated.resources.ic_infobar_pentagon
import com.fieldbook.shared.generated.resources.ic_infobar_pentagon_outline
import com.fieldbook.shared.generated.resources.ic_infobar_rhombus
import com.fieldbook.shared.generated.resources.ic_infobar_rhombus_outline
import com.fieldbook.shared.generated.resources.ic_infobar_square_rounded
import com.fieldbook.shared.generated.resources.ic_infobar_square_rounded_outline
import com.fieldbook.shared.generated.resources.ic_infobar_triangle
import com.fieldbook.shared.generated.resources.ic_infobar_triangle_outline
import com.fieldbook.shared.generated.resources.main_infobar_data_missing
import com.fieldbook.shared.generated.resources.preferences_appearance_infobar_number
import com.fieldbook.shared.generated.resources.preferences_appearance_infobar_number_description
import com.fieldbook.shared.preferences.GeneralKeys
import com.fieldbook.shared.preferences.PreferenceKeys
import com.fieldbook.shared.screens.components.NumberStepperDialog
import com.fieldbook.shared.utilities.CategoryJsonUtil
import com.russhwolf.settings.Settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val DefaultInfoBarTraitId = "-1"

private data class InfoBarSelection(
    val label: String,
    val traitId: Long? = null,
)

private data class InfoBarItem(
    val selection: InfoBarSelection,
    val value: String,
)

private enum class InfoBarDialogTab(val title: StringResource) {
    ATTRIBUTES(Res.string.dialog_att_chooser_attributes),
    TRAITS(Res.string.dialog_att_chooser_traits),
    OTHER(Res.string.dialog_att_chooser_other),
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun InfoBar(controller: CollectScreenController, modifier: Modifier = Modifier) {
    val state by controller.uiState.collectAsState()
    val settings = remember { Settings() }
    val attributeRepository = remember { ObservationUnitAttributeRepository() }
    val propertyRepository = remember { ObservationUnitPropertyRepository() }
    val traitRepository = remember { TraitRepository() }
    val unit = state.units.getOrNull(state.currentUnitIndex)
    val hidePrefixEnabled = settings.getBoolean(PreferenceKeys.HIDE_INFOBAR_PREFIX, false)
    var infoBarCount by remember {
        mutableStateOf(settings.getInt(PreferenceKeys.INFOBAR_NUMBER, 3).coerceIn(1, 20))
    }
    val fieldNameLabel = stringResource(Res.string.field_name_attribute)
    val noDataLabel = stringResource(Res.string.main_infobar_data_missing)
    val showCategoryLabels = settings.getString(PreferenceKeys.LABELVAL_CUSTOMIZE, "value") != "value"

    val availableAttributes = remember(controller.studyId, fieldNameLabel) {
        buildList {
            add(fieldNameLabel)
            addAll(
                attributeRepository.getAllNames(controller.studyId.toLong())
                    .filter { it != fieldNameLabel }
            )
        }
    }
    val allTraits = remember(controller.studyId) {
        traitRepository.getAllTraitsWithAttributes()
    }
    val visibleTraits = remember(allTraits) {
        allTraits.filter { it.visible == "true" }
    }
    val hiddenTraits = remember(allTraits) {
        allTraits.filter { it.visible != "true" }
    }
    val defaultSelectionOrder = remember(
        availableAttributes,
        controller.primaryId,
        controller.secondaryId,
        fieldNameLabel
    ) {
        buildList {
            add("plot_id")
            controller.primaryId.takeIf { it.isNotBlank() }?.let(::add)
            controller.secondaryId.takeIf { it.isNotBlank() }?.let(::add)
            add(fieldNameLabel)
            addAll(availableAttributes.filterNot { it in this })
        }
    }
    var selections by remember(
        infoBarCount,
        availableAttributes,
        allTraits,
        defaultSelectionOrder
    ) {
        mutableStateOf(
            List(infoBarCount) { index ->
                loadInfoBarSelection(
                    settings = settings,
                    index = index,
                    availableAttributes = availableAttributes,
                    allTraits = allTraits,
                    defaultSelectionOrder = defaultSelectionOrder
                )
            }
        )
    }
    var wordWrapStates by remember(infoBarCount) {
        mutableStateOf(
            List(infoBarCount) { index ->
                settings.getBoolean(infoBarWordWrapKey(index), false)
            }
        )
    }
    var activeInfoBarIndex by remember { mutableStateOf<Int?>(null) }
    var showInfoBarCountDialog by remember { mutableStateOf(false) }

    val fieldNameValue = settings.getString(
        GeneralKeys.FIELD_ALIAS.key,
        controller.field.exp_name.ifBlank { noDataLabel }
    ).ifBlank { controller.field.exp_name.ifBlank { noDataLabel } }

    val attributeLabelsToQuery = selections
        .filter { it.traitId == null && it.label != fieldNameLabel }
        .map { it.label }
        .distinct()
    val unitId = unit?.observation_unit_db_id.orEmpty()
    val attributeValues = propertyRepository.getAttributeValuesForUnit(
        uniqueName = controller.uniqueId,
        unitId = unitId,
        attributeLabels = attributeLabelsToQuery
    )

    val infoBarItems = selections.map { selection ->
        InfoBarItem(
            selection = selection,
            value = resolveInfoBarValue(
                controller = controller,
                state = state,
                selection = selection,
                fieldNameLabel = fieldNameLabel,
                fieldNameValue = fieldNameValue,
                noDataLabel = noDataLabel,
                attributeValues = attributeValues,
                allTraits = allTraits,
                showCategoryLabels = showCategoryLabels
            )
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        infoBarItems.forEachIndexed { index, item ->
            val isWordWrapped = wordWrapStates.getOrElse(index) { false }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { activeInfoBarIndex = index },
                        onLongClick = {
                            val updatedWrapState = !isWordWrapped
                            settings.putBoolean(infoBarWordWrapKey(index), updatedWrapState)
                            wordWrapStates = wordWrapStates.toMutableList().apply {
                                this[index] = updatedWrapState
                            }
                        }
                    ),
                color = androidx.compose.ui.graphics.Color.Transparent
            ) {
                if (hidePrefixEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(infoBarIcon(index, isWordWrapped)),
                            contentDescription = item.selection.label,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = if (isWordWrapped) 5 else 1,
                            overflow = if (isWordWrapped) TextOverflow.Clip else TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "${item.selection.label}: ${item.value}",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (isWordWrapped) 5 else 1,
                        overflow = if (isWordWrapped) TextOverflow.Clip else TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    activeInfoBarIndex?.let { infoBarIndex ->
        InfoBarChooserDialog(
            position = infoBarIndex,
            selectedSelection = selections.getOrElse(infoBarIndex) {
                defaultSelectionOrder
                    .getOrNull(infoBarIndex)
                    ?.let(::InfoBarSelection)
                    ?: InfoBarSelection("plot_id")
            },
            attributes = availableAttributes,
            visibleTraits = visibleTraits,
            hiddenTraits = hiddenTraits,
            initialTabIndex = settings.getInt(GeneralKeys.ATTR_CHOOSER_DIALOG_TAB.key, 0),
            onTabChanged = { tabIndex ->
                settings.putInt(GeneralKeys.ATTR_CHOOSER_DIALOG_TAB.key, tabIndex)
            },
            onCustomizeClick = {
                activeInfoBarIndex = null
                showInfoBarCountDialog = true
            },
            onSelectionChosen = { selection ->
                persistInfoBarSelection(settings, infoBarIndex, selection)
                selections = selections.toMutableList().apply {
                    this[infoBarIndex] = selection
                }
                activeInfoBarIndex = null
            },
            onDismiss = { activeInfoBarIndex = null }
        )
    }

    if (showInfoBarCountDialog) {
        NumberStepperDialog(
            title = stringResource(Res.string.preferences_appearance_infobar_number),
            summary = stringResource(Res.string.preferences_appearance_infobar_number_description),
            initialValue = infoBarCount,
            onDismiss = { showInfoBarCountDialog = false },
            onSave = { updatedCount ->
                infoBarCount = updatedCount.coerceIn(1, 20)
                settings.putInt(PreferenceKeys.INFOBAR_NUMBER, infoBarCount)
                showInfoBarCountDialog = false
            }
        )
    }
}

@Composable
private fun InfoBarChooserDialog(
    position: Int,
    selectedSelection: InfoBarSelection,
    attributes: List<String>,
    visibleTraits: List<TraitObject>,
    hiddenTraits: List<TraitObject>,
    initialTabIndex: Int,
    onTabChanged: (Int) -> Unit,
    onCustomizeClick: () -> Unit,
    onSelectionChosen: (InfoBarSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTabIndex by remember(initialTabIndex) {
        mutableStateOf(initialTabIndex.coerceIn(0, InfoBarDialogTab.entries.lastIndex))
    }

    val currentChoices = when (InfoBarDialogTab.entries[selectedTabIndex]) {
        InfoBarDialogTab.ATTRIBUTES -> attributes.map { InfoBarSelection(label = it) }
        InfoBarDialogTab.TRAITS -> visibleTraits.mapNotNull { trait ->
            trait.id?.let { InfoBarSelection(label = trait.name, traitId = it) }
        }
        InfoBarDialogTab.OTHER -> hiddenTraits.mapNotNull { trait ->
            trait.id?.let { InfoBarSelection(label = trait.name, traitId = it) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                    Text(
                        text = stringResource(
                            Res.string.dialog_infobar_att_chooser_title,
                            position + 1
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onCustomizeClick) {
                        Text(stringResource(Res.string.dialog_att_chooser_customize))
                    }
                }

                Spacer(Modifier.height(8.dp))

                TabRow(selectedTabIndex = selectedTabIndex) {
                    InfoBarDialogTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                onTabChanged(index)
                            },
                            text = { Text(stringResource(tab.title)) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (currentChoices.isEmpty()) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        items(currentChoices) { choice ->
                            val isSelected = choice == selectedSelection
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelectionChosen(choice) },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                tonalElevation = if (isSelected) 3.dp else 0.dp,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                            ) {
                                Text(
                                    text = choice.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

private fun loadInfoBarSelection(
    settings: Settings,
    index: Int,
    availableAttributes: List<String>,
    allTraits: List<TraitObject>,
    defaultSelectionOrder: List<String>,
): InfoBarSelection {
    val fallbackLabel = defaultSelectionOrder.getOrElse(index) {
        defaultSelectionOrder.firstOrNull() ?: "plot_id"
    }
    val storedLabel = settings.getString(infoBarAttributeKey(index), fallbackLabel)
    val storedTraitId = settings.getString(infoBarTraitKey(index), DefaultInfoBarTraitId)

    if (storedTraitId != DefaultInfoBarTraitId) {
        val trait = allTraits.firstOrNull { it.id?.toString() == storedTraitId }
        if (trait?.id != null) {
            return InfoBarSelection(label = trait.name, traitId = trait.id)
        }
    }

    val normalizedLabel = if (storedLabel in availableAttributes || storedLabel == fallbackLabel) {
        storedLabel
    } else {
        fallbackLabel
    }

    return InfoBarSelection(label = normalizedLabel)
}

private fun persistInfoBarSelection(
    settings: Settings,
    index: Int,
    selection: InfoBarSelection,
) {
    settings.putString(infoBarAttributeKey(index), selection.label)
    settings.putString(
        infoBarTraitKey(index),
        selection.traitId?.toString() ?: DefaultInfoBarTraitId
    )
}

private fun resolveInfoBarValue(
    controller: CollectScreenController,
    state: CollectUiState,
    selection: InfoBarSelection,
    fieldNameLabel: String,
    fieldNameValue: String,
    noDataLabel: String,
    attributeValues: Map<String, String>,
    allTraits: List<TraitObject>,
    showCategoryLabels: Boolean,
): String {
    selection.traitId?.let { traitId ->
        val rawValue = state.traitValues[traitId]?.lastOrNull()
        if (rawValue.isNullOrBlank()) {
            return noDataLabel
        }

        val trait = allTraits.firstOrNull { it.id == traitId }
        return when (trait?.format?.lowercase()) {
            "categorical", "multicat" -> {
                CategoryJsonUtil.flattenMultiCategoryValue(
                    CategoryJsonUtil.decode(rawValue),
                    showLabel = showCategoryLabels
                ).ifBlank { noDataLabel }
            }

            else -> rawValue
        }
    }

    if (selection.label == fieldNameLabel) {
        return fieldNameValue
    }

    return attributeValues[selection.label]
        ?.takeIf { it.isNotBlank() }
        ?: resolveRangeFallback(controller, state, selection.label).ifBlank { noDataLabel }
}

private fun resolveRangeFallback(
    controller: CollectScreenController,
    state: CollectUiState,
    label: String,
): String {
    return when (label) {
        controller.uniqueId -> state.cRange.uniqueId
        controller.primaryId -> state.cRange.primaryId
        controller.secondaryId -> state.cRange.secondaryId
        else -> ""
    }
}

private fun infoBarAttributeKey(index: Int): String = "DROP$index"

private fun infoBarTraitKey(index: Int): String = "DROP.TRAIT$index"

private fun infoBarWordWrapKey(index: Int): String = "INFOBAR_WORD_WRAP_$index"

private fun infoBarIcon(index: Int, isWordWrapped: Boolean): DrawableResource {
    val enabledIcons = listOf(
        Res.drawable.ic_infobar_rhombus,
        Res.drawable.ic_infobar_circle,
        Res.drawable.ic_infobar_triangle,
        Res.drawable.ic_infobar_square_rounded,
        Res.drawable.ic_infobar_pentagon,
        Res.drawable.ic_infobar_hexagon
    )
    val disabledIcons = listOf(
        Res.drawable.ic_infobar_rhombus_outline,
        Res.drawable.ic_infobar_circle_outline,
        Res.drawable.ic_infobar_triangle_outline,
        Res.drawable.ic_infobar_square_rounded_outline,
        Res.drawable.ic_infobar_pentagon_outline,
        Res.drawable.ic_infobar_hexagon_outline
    )
    val icons = if (isWordWrapped) enabledIcons else disabledIcons
    return icons[index % icons.size]
}
