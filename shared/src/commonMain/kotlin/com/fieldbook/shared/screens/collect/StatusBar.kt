package com.fieldbook.shared.screens.collect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.circle_filled
import com.fieldbook.shared.generated.resources.circle_outline
import org.jetbrains.compose.resources.painterResource

@Composable
fun StatusBar(
    viewModel: CollectScreenController,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    if (state.traitValuesLoading) {
        Box(
            modifier = modifier
                .height(32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        }
        return
    }

    val listState = rememberLazyListState()

    LaunchedEffect(state.currentTraitIndex, state.traits.size) {
        if (state.traits.isNotEmpty()) {
            listState.animateScrollToItem(state.currentTraitIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .height(32.dp)
            .fillMaxWidth(),
    ) {
        itemsIndexed(
            items = state.traits,
            key = { _, trait -> trait.id ?: trait.name }
        ) { index, trait ->
            val hasObservation = trait.id?.let { state.traitValues[it] } != null
            val isCurrent = index == state.currentTraitIndex
            val iconRes = if (hasObservation) Res.drawable.circle_filled else Res.drawable.circle_outline
            val iconColor = if (isCurrent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            }
            Surface(
                color = Color.Transparent,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = trait.name,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
