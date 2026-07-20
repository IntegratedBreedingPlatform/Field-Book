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
    if (viewModel.traitValuesLoading) {
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

    LaunchedEffect(viewModel.currentTraitIndex, viewModel.traits.size) {
        if (viewModel.traits.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.currentTraitIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .height(32.dp)
            .fillMaxWidth(),
    ) {
        itemsIndexed(
            items = viewModel.traits,
            key = { _, trait -> trait.id ?: trait.name }
        ) { index, trait ->
            val hasObservation = viewModel.traitValues[trait.id] != null
            val isCurrent = index == viewModel.currentTraitIndex
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
