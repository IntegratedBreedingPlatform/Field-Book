package com.fieldbook.shared.screens.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fieldbook.shared.KmpHostScreenType
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.ic_adv_infobar_count
import com.fieldbook.shared.generated.resources.ic_hide_infobar_prefix
import com.fieldbook.shared.generated.resources.ic_nav_drawer_translate
import com.fieldbook.shared.generated.resources.ic_pref_appearance_theme
import com.fieldbook.shared.generated.resources.ic_pref_appearance_toolbar
import com.fieldbook.shared.generated.resources.ic_range_progress
import com.fieldbook.shared.generated.resources.ic_traits_progress
import com.fieldbook.shared.generated.resources.preferences_appearance_collect_screen_title
import com.fieldbook.shared.generated.resources.preference_language_default
import com.fieldbook.shared.generated.resources.preferences_appearance_application_title
import com.fieldbook.shared.generated.resources.preferences_appearance_infobar_hide_prefix
import com.fieldbook.shared.generated.resources.preferences_appearance_infobar_hide_prefix_description
import com.fieldbook.shared.generated.resources.preferences_appearance_infobar_number
import com.fieldbook.shared.generated.resources.preferences_appearance_infobar_number_description
import com.fieldbook.shared.generated.resources.preferences_appearance_language
import com.fieldbook.shared.generated.resources.preferences_appearance_language_description
import com.fieldbook.shared.generated.resources.preferences_appearance_range_progress_bar
import com.fieldbook.shared.generated.resources.preferences_appearance_range_progress_bar_description
import com.fieldbook.shared.generated.resources.preferences_appearance_toolbar_customize
import com.fieldbook.shared.generated.resources.preferences_appearance_toolbar_customize_description
import com.fieldbook.shared.generated.resources.preferences_appearance_toolbar_customize_lock
import com.fieldbook.shared.generated.resources.preferences_appearance_toolbar_customize_resources
import com.fieldbook.shared.generated.resources.preferences_appearance_toolbar_customize_search
import com.fieldbook.shared.generated.resources.preferences_appearance_toolbar_customize_summary
import com.fieldbook.shared.generated.resources.preferences_appearance_theme_summary
import com.fieldbook.shared.generated.resources.preferences_appearance_theme_title
import com.fieldbook.shared.generated.resources.preferences_appearance_traits_progress_bar
import com.fieldbook.shared.generated.resources.preferences_appearance_traits_progress_bar_description
import com.fieldbook.shared.generated.resources.preferences_appearance_title
import com.fieldbook.shared.preferences.PreferenceKeys
import com.fieldbook.shared.preferences.loadToolbarCustomizationPreference
import com.fieldbook.shared.preferences.persistToolbarCustomizationPreference
import com.fieldbook.shared.screens.components.NumberStepperDialog
import com.russhwolf.settings.Settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private data class AppearancePreferenceRow(
    val icon: DrawableResource,
    val title: StringResource,
    val summary: StringResource? = null,
    val value: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
)

private data class ToolbarOption(
    val value: String,
    val title: StringResource
)

private data class AppearanceTogglePreferenceRow(
    val icon: DrawableResource,
    val title: StringResource,
    val summary: StringResource,
    val checked: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: (Boolean) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePreferencesScreen(
    onBack: (() -> Unit)? = null,
    onNavigate: ((KmpHostScreenType) -> Unit)? = null
) {
    val settings = remember { Settings() }
    val languageDefaultSummary = stringResource(Res.string.preference_language_default)
    val infoBarNumberTitle = stringResource(Res.string.preferences_appearance_infobar_number)
    val infoBarNumberSummary = stringResource(Res.string.preferences_appearance_infobar_number_description)
    val searchToolbarLabel = stringResource(Res.string.preferences_appearance_toolbar_customize_search)
    val resourcesToolbarLabel = stringResource(Res.string.preferences_appearance_toolbar_customize_resources)
    val summaryToolbarLabel = stringResource(Res.string.preferences_appearance_toolbar_customize_summary)
    val lockToolbarLabel = stringResource(Res.string.preferences_appearance_toolbar_customize_lock)
    val toolbarOptions = remember {
        listOf(
            ToolbarOption("search", Res.string.preferences_appearance_toolbar_customize_search),
            ToolbarOption("resources", Res.string.preferences_appearance_toolbar_customize_resources),
            ToolbarOption("summary", Res.string.preferences_appearance_toolbar_customize_summary),
            ToolbarOption("lockData", Res.string.preferences_appearance_toolbar_customize_lock)
        )
    }

    val languageSummary = settings.getString(
        PreferenceKeys.LANGUAGE_LOCALE_SUMMARY,
        languageDefaultSummary
    )
    var dialogState by remember { mutableStateOf<PreferenceDialogState?>(null) }
    var showToolbarActionsDialog by remember { mutableStateOf(false) }
    var showInfoBarCountDialog by remember { mutableStateOf(false) }
    var toolbarCustomization by remember {
        mutableStateOf(loadToolbarCustomization(settings, toolbarOptions.map { it.value }.toSet()))
    }
    var infoBarCount by remember {
        mutableStateOf(settings.getInt(PreferenceKeys.INFOBAR_NUMBER, 3).coerceIn(1, 20))
    }
    var hideInfoBarPrefix by remember {
        mutableStateOf(settings.getBoolean(PreferenceKeys.HIDE_INFOBAR_PREFIX, false))
    }
    var rangeProgressBarEnabled by remember {
        mutableStateOf(settings.getBoolean(PreferenceKeys.RANGE_PROGRESS_BAR, true))
    }
    var traitsProgressBarEnabled by remember {
        mutableStateOf(settings.getBoolean(PreferenceKeys.TRAITS_PROGRESS_BAR, true))
    }

    fun updateToolbarCustomization(option: String, enabled: Boolean) {
        toolbarCustomization = toolbarCustomization.toMutableSet().apply {
            if (enabled) add(option) else remove(option)
        }
        persistToolbarCustomizationPreference(
            settings = settings,
            key = PreferenceKeys.TOOLBAR_CUSTOMIZE,
            values = toolbarOptions
                .mapNotNull { toolbarOption ->
                    toolbarOption.value.takeIf { it in toolbarCustomization }
                }
                .toSet()
        )
    }

    val selectedToolbarActionsSummary = toolbarOptions
        .mapNotNull { option -> option.takeIf { option.value in toolbarCustomization } }
        .joinToString(", ") { option ->
            when (option.value) {
                "search" -> searchToolbarLabel
                "resources" -> resourcesToolbarLabel
                "summary" -> summaryToolbarLabel
                "lockData" -> lockToolbarLabel
                else -> option.value
            }
        }
        .ifBlank { "-" }

    val collectTogglePreferences = listOf(
        AppearanceTogglePreferenceRow(
            icon = Res.drawable.ic_hide_infobar_prefix,
            title = Res.string.preferences_appearance_infobar_hide_prefix,
            summary = Res.string.preferences_appearance_infobar_hide_prefix_description,
            checked = hideInfoBarPrefix,
            onCheckedChange = { enabled ->
                hideInfoBarPrefix = enabled
                settings.putBoolean(PreferenceKeys.HIDE_INFOBAR_PREFIX, enabled)
            }
        ),
        AppearanceTogglePreferenceRow(
            icon = Res.drawable.ic_range_progress,
            title = Res.string.preferences_appearance_range_progress_bar,
            summary = Res.string.preferences_appearance_range_progress_bar_description,
            checked = rangeProgressBarEnabled,
            onCheckedChange = { enabled ->
                rangeProgressBarEnabled = enabled
                settings.putBoolean(PreferenceKeys.RANGE_PROGRESS_BAR, enabled)
            }
        ),
        AppearanceTogglePreferenceRow(
            icon = Res.drawable.ic_traits_progress,
            title = Res.string.preferences_appearance_traits_progress_bar,
            summary = Res.string.preferences_appearance_traits_progress_bar_description,
            checked = traitsProgressBarEnabled,
            onCheckedChange = { enabled ->
                traitsProgressBarEnabled = enabled
                settings.putBoolean(PreferenceKeys.TRAITS_PROGRESS_BAR, enabled)
            }
        )
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.preferences_appearance_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    PreferenceSectionTitle(Res.string.preferences_appearance_application_title)
                }
                item {
                    AppearancePreferenceListRow(
                        item = AppearancePreferenceRow(
                            icon = Res.drawable.ic_pref_appearance_theme,
                            title = Res.string.preferences_appearance_theme_title,
                            summary = Res.string.preferences_appearance_theme_summary,
                            enabled = false
                        )
                    )
                    HorizontalDivider()
                }
                item {
                    AppearancePreferenceListRow(
                        item = AppearancePreferenceRow(
                            icon = Res.drawable.ic_nav_drawer_translate,
                            title = Res.string.preferences_appearance_language,
                            summary = Res.string.preferences_appearance_language_description,
                            value = languageSummary,
                            onClick = {
                                onNavigate?.invoke(KmpHostScreenType.LANGUAGE_PREFERENCES)
                            }
                        )
                    )
                    HorizontalDivider()
                }
                item {
                    PreferenceSectionTitle(Res.string.preferences_appearance_collect_screen_title)
                }
                item {
                    AppearancePreferenceListRow(
                        item = AppearancePreferenceRow(
                            icon = Res.drawable.ic_pref_appearance_toolbar,
                            title = Res.string.preferences_appearance_toolbar_customize,
                            summary = Res.string.preferences_appearance_toolbar_customize_description,
                            value = selectedToolbarActionsSummary,
                            onClick = { showToolbarActionsDialog = true }
                        )
                    )
                    HorizontalDivider()
                }
                item {
                    AppearancePreferenceListRow(
                        item = AppearancePreferenceRow(
                            icon = Res.drawable.ic_adv_infobar_count,
                            title = Res.string.preferences_appearance_infobar_number,
                            summary = Res.string.preferences_appearance_infobar_number_description,
                            value = infoBarCount.toString(),
                            onClick = { showInfoBarCountDialog = true }
                        )
                    )
                    HorizontalDivider()
                }
                collectTogglePreferences.forEach { preference ->
                    item {
                        AppearancePreferenceToggleRow(item = preference)
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showToolbarActionsDialog) {
        ToolbarActionsDialog(
            options = toolbarOptions.map { option ->
                AppearanceTogglePreferenceRow(
                    icon = Res.drawable.ic_pref_appearance_toolbar,
                    title = option.title,
                    summary = Res.string.preferences_appearance_toolbar_customize_description,
                    checked = option.value in toolbarCustomization,
                    enabled = option.value == "summary" || option.value == "lockData",
                    onCheckedChange = { enabled ->
                        if (option.value == "summary" || option.value == "lockData") {
                            updateToolbarCustomization(option.value, enabled)
                        }
                    }
                )
            },
            onDismiss = { showToolbarActionsDialog = false }
        )
    }

    if (showInfoBarCountDialog) {
        NumberStepperDialog(
            title = infoBarNumberTitle,
            summary = infoBarNumberSummary,
            initialValue = infoBarCount,
            onDismiss = { showInfoBarCountDialog = false },
            onSave = { updatedCount ->
                infoBarCount = updatedCount
                settings.putInt(PreferenceKeys.INFOBAR_NUMBER, updatedCount)
                showInfoBarCountDialog = false
            }
        )
    }

    dialogState?.let { state ->
        when (state.type) {
            PreferenceDialogType.TEXT -> PreferenceTextDialog(
                state = state,
                onDismiss = { dialogState = null }
            )
            PreferenceDialogType.OPTIONS -> PreferenceOptionsDialog(
                state = state,
                onDismiss = { dialogState = null }
            )
            PreferenceDialogType.INFO -> PreferenceInfoDialog(
                state = state,
                onDismiss = { dialogState = null }
            )
        }
    }
}

private fun loadToolbarCustomization(
    settings: Settings,
    defaultOptions: Set<String>
): Set<String> {
    return loadToolbarCustomizationPreference(
        settings = settings,
        key = PreferenceKeys.TOOLBAR_CUSTOMIZE,
        defaultOptions = defaultOptions
    )
}

@Composable
private fun PreferenceSectionTitle(title: StringResource) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
private fun AppearancePreferenceListRow(item: AppearancePreferenceRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled, onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .graphicsLayer { alpha = if (item.enabled) 1f else 0.4f },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = stringResource(item.title),
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(item.title),
                style = MaterialTheme.typography.bodyLarge
            )
            item.value?.takeIf { it.isNotBlank() }?.let { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } ?: item.summary?.let { summary ->
                Text(
                    text = stringResource(summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppearancePreferenceToggleRow(item: AppearanceTogglePreferenceRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled) { item.onCheckedChange(!item.checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .graphicsLayer { alpha = if (item.enabled) 1f else 0.45f },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = stringResource(item.title),
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = stringResource(item.title),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(item.summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = item.checked,
            enabled = item.enabled,
            onCheckedChange = item.onCheckedChange
        )
    }
}

@Composable
private fun ToolbarActionsDialog(
    options: List<AppearanceTogglePreferenceRow>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
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
                        text = stringResource(Res.string.preferences_appearance_toolbar_customize),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = stringResource(Res.string.preferences_appearance_toolbar_customize_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(options) { option ->
                        AppearancePreferenceToggleRow(item = option)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
