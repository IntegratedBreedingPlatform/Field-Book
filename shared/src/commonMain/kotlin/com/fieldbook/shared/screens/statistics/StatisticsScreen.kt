package com.fieldbook.shared.screens.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.dialog_back
import com.fieldbook.shared.generated.resources.dialog_ok
import com.fieldbook.shared.generated.resources.ic_stats_busiest
import com.fieldbook.shared.generated.resources.ic_stats_calendar
import com.fieldbook.shared.generated.resources.ic_stats_export
import com.fieldbook.shared.generated.resources.ic_stats_field
import com.fieldbook.shared.generated.resources.ic_stats_most_obs
import com.fieldbook.shared.generated.resources.ic_stats_observation
import com.fieldbook.shared.generated.resources.ic_stats_people
import com.fieldbook.shared.generated.resources.ic_stats_photo
import com.fieldbook.shared.generated.resources.ic_stats_plot
import com.fieldbook.shared.generated.resources.ic_stats_time
import com.fieldbook.shared.generated.resources.settings_statistics
import com.fieldbook.shared.generated.resources.stat_title_busiest
import com.fieldbook.shared.generated.resources.stat_title_data
import com.fieldbook.shared.generated.resources.stat_title_entries
import com.fieldbook.shared.generated.resources.stat_title_fields
import com.fieldbook.shared.generated.resources.stat_title_hours
import com.fieldbook.shared.generated.resources.stat_title_most
import com.fieldbook.shared.generated.resources.stat_title_people
import com.fieldbook.shared.generated.resources.stat_title_photos
import com.fieldbook.shared.generated.resources.stats_heatmap
import com.fieldbook.shared.generated.resources.stats_tab_layout_month
import com.fieldbook.shared.generated.resources.stats_tab_layout_total
import com.fieldbook.shared.generated.resources.stats_tab_layout_year
import com.fieldbook.shared.theme.AlertDialog
import com.fieldbook.shared.theme.TextButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    onSnackbarMessage: (String) -> Unit,
    viewModel: StatisticsScreenViewModel = viewModel(
        factory = statisticsScreenViewModelFactory()
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDetails by remember { mutableStateOf<StatisticsCardDetails?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (uiState.showHeatmap) {
        StatisticsHeatmapScreen(
            heatmap = uiState.heatmap,
            onBack = viewModel::closeHeatmap,
            onToggleCounts = viewModel::toggleHeatmapCounts,
            onRangeSelected = viewModel::setHeatmapRange,
            onSnackbarMessage = onSnackbarMessage,
        )
        return
    }

    selectedDetails?.let { details ->
        StatisticsDetailsDialog(
            details = details,
            onDismiss = { selectedDetails = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_statistics)) },
                actions = {
                    IconButton(onClick = viewModel::openHeatmap) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_stats_calendar),
                            contentDescription = stringResource(Res.string.stats_heatmap),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.dialog_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatisticsModeSelector(
                selectedMode = uiState.mode,
                onSelectMode = viewModel::setMode,
            )

            when {
                uiState.loading -> Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> Box(Modifier.fillMaxSize()) {
                    Text(
                        text = uiState.error.orEmpty(),
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                uiState.sections.all { section -> section.cards.all { it.value == "0" || it.value == "-" || it.value == "0.00" } } -> Box(Modifier.fillMaxSize()) {
                    Text(
                        text = "No statistics available.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(uiState.sections) { section ->
                        StatisticsSectionCard(
                            section = section,
                            onShareClick = { image ->
                                val shared = shareStatisticsSection(section, image)
                                if (!shared) {
                                    onSnackbarMessage("Unable to share statistics image.")
                                }
                            },
                            onShareCaptureFailed = {
                                onSnackbarMessage("Unable to capture statistics image.")
                            },
                            onCardClick = { card ->
                                val details = card.details ?: return@StatisticsSectionCard
                                if (details.lines.isNotEmpty()) {
                                    selectedDetails = details
                                } else {
                                    details.message?.let(onSnackbarMessage)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsModeSelector(
    selectedMode: StatisticsMode,
    onSelectMode: (StatisticsMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatisticsMode.entries.forEach { mode ->
            val selected = selectedMode == mode
            OutlinedButton(
                onClick = { onSelectMode(mode) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(stringResource(mode.titleResource()))
            }
        }
    }
}

@Composable
private fun StatisticsSectionCard(
    section: StatisticsSection,
    onShareClick: suspend (ImageBitmap) -> Unit,
    onShareCaptureFailed: () -> Unit,
    onCardClick: (StatisticsCard) -> Unit,
) {
    val graphicsContext = LocalGraphicsContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val textMeasurer = rememberTextMeasurer()
    val graphicsLayer = remember(graphicsContext) { graphicsContext.createGraphicsLayer() }
    val coroutineScope = rememberCoroutineScope()
    val exportTiles = section.cards.map { card ->
        StatisticsExportTile(
            label = stringResource(card.type.titleResource()),
            value = card.value,
        )
    }

    DisposableEffect(graphicsContext, graphicsLayer) {
        onDispose {
            graphicsContext.releaseGraphicsLayer(graphicsLayer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                val layerSize = IntSize(
                    width = size.width.roundToInt(),
                    height = size.height.roundToInt(),
                )
                if (layerSize.width > 0 && layerSize.height > 0) {
                    val contentDrawScope = this
                    graphicsLayer.record(
                        density = this,
                        layoutDirection = layoutDirection,
                        size = layerSize,
                    ) {
                        contentDrawScope.drawContent()
                    }
                    drawLayer(graphicsLayer)
                } else {
                    drawContent()
                }
            },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = section.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val capturedImage = runCatching {
                                    graphicsLayer.toImageBitmap()
                                }.getOrNull()
                                val capturedHasPixels = capturedImage
                                    ?.let { image -> runCatching { image.hasVisiblePixels() }.getOrDefault(false) }
                                    ?: false
                                val image = if (capturedImage != null && capturedHasPixels) {
                                    capturedImage
                                } else {
                                    renderStatisticsSectionImage(
                                        graphicsContext = graphicsContext,
                                        density = density,
                                        layoutDirection = layoutDirection,
                                        textMeasurer = textMeasurer,
                                        title = section.title,
                                        tiles = exportTiles,
                                        preferredSize = graphicsLayer.size,
                                    )
                                }
                                if (image != null) {
                                    onShareClick(image)
                                } else {
                                    onShareCaptureFailed()
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_stats_export),
                            contentDescription = "Share statistics image",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 560.dp) 4 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        section.cards.chunked(columns).forEach { rowCards ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowCards.forEach { card ->
                                    StatisticTile(
                                        card = card,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onCardClick(card) },
                                    )
                                }
                                repeat(columns - rowCards.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class StatisticsExportTile(
    val label: String,
    val value: String,
)

/**
 * Fallback renderer for Compose 1.7.3, where recording drawContent() into a manually-created
 * GraphicsLayer can produce a correctly-sized but fully-transparent ImageBitmap.
 */
private suspend fun renderStatisticsSectionImage(
    graphicsContext: GraphicsContext,
    density: Density,
    layoutDirection: LayoutDirection,
    textMeasurer: TextMeasurer,
    title: String,
    tiles: List<StatisticsExportTile>,
    preferredSize: IntSize,
): ImageBitmap? {
    val exportSize = preferredSize.takeIf { it.width > 0 && it.height > 0 }
        ?: IntSize(960, 620)
    val exportLayer = graphicsContext.createGraphicsLayer()

    return try {
        exportLayer.record(
            density = density,
            layoutDirection = layoutDirection,
            size = exportSize,
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 28.dp.toPx()
            val gap = 10.dp.toPx()
            val columns = if (canvasWidth >= 560.dp.toPx()) 4 else 2
            val rows = (tiles.size + columns - 1) / columns
            val headerHeight = 70.dp.toPx()
            val tileWidth = (canvasWidth - padding * 2 - gap * (columns - 1)) / columns
            val tileHeight = ((canvasHeight - padding * 2 - headerHeight - gap * (rows - 1)) / rows)
                .coerceAtLeast(74.dp.toPx())

            drawRect(Color.White, size = Size(canvasWidth, canvasHeight))
            drawRoundRect(
                color = Color(0xFFF8FBF4),
                topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                size = Size(canvasWidth - 12.dp.toPx(), canvasHeight - 12.dp.toPx()),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            )

            val titleLayout = textMeasurer.measure(
                text = title,
                style = TextStyle(
                    color = Color(0xFF1B1B1B),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            drawText(
                textLayoutResult = titleLayout,
                topLeft = Offset(padding, padding),
            )

            tiles.forEachIndexed { index, tile ->
                val column = index % columns
                val row = index / columns
                val left = padding + column * (tileWidth + gap)
                val top = padding + headerHeight + row * (tileHeight + gap)
                drawRoundRect(
                    color = Color(0xFFEDF6E5),
                    topLeft = Offset(left, top),
                    size = Size(tileWidth, tileHeight),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                )

                val valueLayout = textMeasurer.measure(
                    text = tile.value,
                    style = TextStyle(
                        color = Color(0xFF111111),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                drawText(
                    textLayoutResult = valueLayout,
                    topLeft = Offset(
                        x = left + (tileWidth - valueLayout.size.width) / 2f,
                        y = top + tileHeight * 0.32f - valueLayout.size.height / 2f,
                    ),
                )

                val labelLayout = textMeasurer.measure(
                    text = tile.label,
                    style = TextStyle(
                        color = Color(0xFF333333),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(
                        x = left + (tileWidth - labelLayout.size.width) / 2f,
                        y = top + tileHeight * 0.68f - labelLayout.size.height / 2f,
                    ),
                )
            }
        }
        exportLayer.toImageBitmap()
    } finally {
        graphicsContext.releaseGraphicsLayer(exportLayer)
    }
}

private fun ImageBitmap.hasVisiblePixels(): Boolean {
    if (width <= 0 || height <= 0) return false

    val pixels = IntArray(width * height)
    readPixels(pixels)
    return pixels.any { pixel ->
        (pixel ushr 24) != 0
    }
}

@Composable
private fun StatisticTile(
    card: StatisticsCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hasDetails = card.details != null
    Surface(
        modifier = modifier
            .clickable(enabled = hasDetails, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(card.type.iconResource()),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = card.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = stringResource(card.type.titleResource()),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StatisticsDetailsDialog(
    details: StatisticsCardDetails,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(details.title.ifBlank { stringResource(Res.string.settings_statistics) }) },
        text = {
            if (details.lines.isEmpty()) {
                Text(details.message.orEmpty())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(details.lines) { line ->
                        Text(line)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_ok))
            }
        },
    )
}

private fun StatisticsMode.titleResource(): StringResource = when (this) {
    StatisticsMode.TOTAL -> Res.string.stats_tab_layout_total
    StatisticsMode.YEAR -> Res.string.stats_tab_layout_year
    StatisticsMode.MONTH -> Res.string.stats_tab_layout_month
}

private fun StatisticsCardType.titleResource(): StringResource = when (this) {
    StatisticsCardType.FIELDS -> Res.string.stat_title_fields
    StatisticsCardType.ENTRIES -> Res.string.stat_title_entries
    StatisticsCardType.DATA -> Res.string.stat_title_data
    StatisticsCardType.HOURS -> Res.string.stat_title_hours
    StatisticsCardType.PEOPLE -> Res.string.stat_title_people
    StatisticsCardType.PHOTOS -> Res.string.stat_title_photos
    StatisticsCardType.BUSIEST -> Res.string.stat_title_busiest
    StatisticsCardType.MOST -> Res.string.stat_title_most
}

private fun StatisticsCardType.iconResource(): DrawableResource = when (this) {
    StatisticsCardType.FIELDS -> Res.drawable.ic_stats_field
    StatisticsCardType.ENTRIES -> Res.drawable.ic_stats_plot
    StatisticsCardType.DATA -> Res.drawable.ic_stats_observation
    StatisticsCardType.HOURS -> Res.drawable.ic_stats_time
    StatisticsCardType.PEOPLE -> Res.drawable.ic_stats_people
    StatisticsCardType.PHOTOS -> Res.drawable.ic_stats_photo
    StatisticsCardType.BUSIEST -> Res.drawable.ic_stats_busiest
    StatisticsCardType.MOST -> Res.drawable.ic_stats_most_obs
}
