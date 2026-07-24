package com.fieldbook.shared.screens.collect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.chevron_left
import com.fieldbook.shared.generated.resources.chevron_right
import com.fieldbook.shared.preferences.PreferenceKeys
import com.russhwolf.settings.Settings
import org.jetbrains.compose.resources.painterResource

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

    LaunchedEffect(state.currentUnitIndex, state.rangeID) {
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
                Text(
                    "${viewModel.primaryId}: ${state.cRange.primaryId}",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    "${viewModel.secondaryId}: ${state.cRange.secondaryId}",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
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
}
