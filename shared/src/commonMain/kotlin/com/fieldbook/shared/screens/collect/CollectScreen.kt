package com.fieldbook.shared.screens.collect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.ic_field
import com.fieldbook.shared.generated.resources.ic_transfer_error
import com.fieldbook.shared.preferences.PreferenceKeys
import com.fieldbook.shared.screens.collect.traits.PhotoTrait
import com.fieldbook.shared.screens.collect.traits.PhotoTraitDisplayMode
import com.fieldbook.shared.screens.datagrid.DataGridScreen
import com.fieldbook.shared.traits.Formats
import com.russhwolf.settings.Settings
import org.jetbrains.compose.resources.painterResource

/**
 * KMP version of CollectActivity main screen logic.
 * UI and business logic will be migrated here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectScreen(
    modifier: Modifier = Modifier,
    controller: CollectScreenController = remember { CollectScreenController() },
    onBack: (() -> Unit)? = null,
) {
    var isCameraFullscreen by remember { mutableStateOf(false) }
    var showDataGrid by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val settings = remember { Settings() }
    val dataGridEnabled = remember {
        settings.getBoolean(PreferenceKeys.DATAGRID_SETTING, false)
    }
    val handleBack: () -> Unit = {
        controller.persistCurrentSelection()
        onBack?.invoke()
    }

    DisposableEffect(controller) {
        onDispose {
            controller.persistCurrentSelection()
        }
    }

    val currentTrait = controller.traits.getOrNull(controller.currentTraitIndex)
    val currentValues = currentTrait?.let { controller.traitValues[it.id] } ?: emptyList()
    val currentFormat = currentTrait?.format?.let { formatStr ->
        Formats.entries.find { it.databaseName.equals(formatStr, ignoreCase = true) }
    }
    val isCurrentTraitCamera = currentFormat?.isCamera == true

    LaunchedEffect(controller.inputValidationMessage) {
        controller.inputValidationMessage?.let { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            controller.clearInputValidationMessage()
        }
    }

    if (isCameraFullscreen && isCurrentTraitCamera) {
        Surface(modifier = modifier.fillMaxSize()) {
            PhotoTrait(
                values = currentValues,
                onPhotoCaptured = { controller.addCurrentTraitValue(it) },
                onPhotoDeleted = { controller.deleteCurrentTraitValue(it) },
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                displayMode = PhotoTraitDisplayMode.FULLSCREEN,
                onCollapseRequest = { isCameraFullscreen = false }
            )
        }
        return
    }

    if (showDataGrid) {
        DataGridScreen(
            modifier = modifier,
            activePlotIndex = controller.currentUnitIndex + 1,
            activeTraitIndex = controller.currentTraitIndex + 1,
            onBack = { showDataGrid = false },
            onSelection = { selection ->
                controller.applyDataGridSelection(selection)
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
                            enabled = !controller.collectInteractionLocked
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (dataGridEnabled) {
                        IconButton(
                            onClick = { showDataGrid = true },
                            enabled = !controller.collectInteractionLocked
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_field),
                                contentDescription = "Data Grid"
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
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (controller.unitLoading || controller.traitLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (controller.unitError != null || controller.traitError != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${controller.unitError ?: controller.traitError}")
                    }
                } else if (controller.units.isNotEmpty() && controller.traits.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(8.dp))
                        InfoBar(controller = controller)
                        Spacer(Modifier.height(8.dp))
                        TraitBox(
                            viewModel = controller,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        RangeBox(controller = controller)
                        CollectInput(
                            controller = controller,
                            modifier = Modifier.weight(1f),
                            onExpandPhotoTrait = { isCameraFullscreen = true }
                        )
                    }
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
