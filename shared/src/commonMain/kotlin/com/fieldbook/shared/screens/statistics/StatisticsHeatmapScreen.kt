package com.fieldbook.shared.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.dialog_back
import com.fieldbook.shared.generated.resources.dialog_cancel
import com.fieldbook.shared.generated.resources.dialog_ok
import com.fieldbook.shared.generated.resources.ic_stats_calendar_range
import com.fieldbook.shared.generated.resources.ic_stats_counter
import com.fieldbook.shared.generated.resources.ic_stats_scroll_bottom
import com.fieldbook.shared.generated.resources.ic_stats_scroll_top
import com.fieldbook.shared.generated.resources.stats_calendar_range
import com.fieldbook.shared.generated.resources.stats_counter
import com.fieldbook.shared.generated.resources.stats_date_range_picker_title
import com.fieldbook.shared.generated.resources.stats_first_day
import com.fieldbook.shared.generated.resources.stats_heatmap_title
import com.fieldbook.shared.generated.resources.stats_last_day
import com.fieldbook.shared.generated.resources.warning_invalid_date_range
import com.fieldbook.shared.generated.resources.warning_no_observations
import com.fieldbook.shared.utilities.epochMillisToLocalDate
import com.fieldbook.shared.utilities.localDateToEpochMillis
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsHeatmapScreen(
    heatmap: StatisticsHeatmapState,
    onBack: () -> Unit,
    onToggleCounts: () -> Unit,
    onRangeSelected: (LocalDate, LocalDate) -> Unit,
    onSnackbarMessage: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showRangePicker by remember { mutableStateOf(false) }
    val noObservationsMessage = stringResource(Res.string.warning_no_observations)

    LaunchedEffect(heatmap.months.firstOrNull()?.year, heatmap.months.firstOrNull()?.monthNumber) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentMonthIndex = heatmap.monthIndex(today)
        if (currentMonthIndex >= 0) {
            listState.scrollToItem(currentMonthIndex)
        } else if (heatmap.months.isNotEmpty()) {
            listState.scrollToItem(heatmap.months.lastIndex)
        }
    }

    if (showRangePicker) {
        StatisticsHeatmapRangeDialog(
            heatmap = heatmap,
            onDismiss = { showRangePicker = false },
            onRangeSelected = { startDate, endDate ->
                onRangeSelected(startDate, endDate)
                showRangePicker = false
            },
            onInvalidRange = { onSnackbarMessage(it) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.stats_heatmap_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.dialog_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val index = heatmap.firstObservationDateInRange?.let(heatmap::monthIndex) ?: -1
                            if (index >= 0) {
                                coroutineScope.launch { listState.animateScrollToItem(index) }
                            } else {
                                onSnackbarMessage(noObservationsMessage)
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_stats_scroll_top),
                            contentDescription = stringResource(Res.string.stats_first_day),
                        )
                    }
                    IconButton(
                        onClick = {
                            val index = heatmap.lastObservationDateInRange?.let(heatmap::monthIndex) ?: -1
                            if (index >= 0) {
                                coroutineScope.launch { listState.animateScrollToItem(index) }
                            } else {
                                onSnackbarMessage(noObservationsMessage)
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_stats_scroll_bottom),
                            contentDescription = stringResource(Res.string.stats_last_day),
                        )
                    }
                    IconButton(onClick = { showRangePicker = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_stats_calendar_range),
                            contentDescription = stringResource(Res.string.stats_calendar_range),
                        )
                    }
                    IconButton(onClick = onToggleCounts) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_stats_counter),
                            contentDescription = stringResource(Res.string.stats_counter),
                            tint = if (heatmap.showCounts) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        if (!heatmap.hasObservations) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text(
                    text = noObservationsMessage,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                count = heatmap.months.size,
                key = { index -> "${heatmap.months[index].year}-${heatmap.months[index].monthNumber}" },
            ) { index ->
                StatisticsHeatmapMonthCard(
                    month = heatmap.months[index],
                    showCounts = heatmap.showCounts,
                )
            }
        }
    }
}

@Composable
private fun StatisticsHeatmapMonthCard(
    month: StatisticsHeatmapMonth,
    showCounts: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = month.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            WeekdayHeader()
            month.days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    week.forEach { day ->
                        StatisticsHeatmapDayCell(
                            day = day,
                            showCounts = showCounts,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatisticsHeatmapDayCell(
    day: StatisticsHeatmapDay,
    showCounts: Boolean,
    modifier: Modifier = Modifier,
) {
    val date = day.date
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            val dayColor = heatmapDayColor(day.count)
            val textColor = when {
                !day.inSelectedRange -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                day.count > 0 -> Color.Black
                else -> MaterialTheme.colorScheme.onSurface
            }
            val backgroundColor = when {
                !day.inSelectedRange -> Color.Transparent
                day.count > 0 -> dayColor
                else -> Color.Transparent
            }
            val label = if (showCounts) day.count.toString() else date.dayOfMonth.toString()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (day.count > 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsHeatmapRangeDialog(
    heatmap: StatisticsHeatmapState,
    onDismiss: () -> Unit,
    onRangeSelected: (LocalDate, LocalDate) -> Unit,
    onInvalidRange: (String) -> Unit,
) {
    var selectingStart by remember { mutableStateOf(true) }
    var startDate by remember { mutableStateOf(heatmap.startDate ?: heatmap.availableStartDate) }
    var endDate by remember { mutableStateOf(heatmap.endDate ?: heatmap.availableEndDate) }
    val invalidRangeMessage = stringResource(Res.string.warning_invalid_date_range)
    val activeDate = if (selectingStart) startDate else endDate

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = startDate
                    val end = endDate
                    if (start == null || end == null || start > end) {
                        onInvalidRange(invalidRangeMessage)
                    } else {
                        onRangeSelected(start, end)
                    }
                },
            ) {
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = stringResource(Res.string.stats_date_range_picker_title),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { selectingStart = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Start: ${startDate?.toShortText().orEmpty()}")
                }
                OutlinedButton(
                    onClick = { selectingStart = false },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("End: ${endDate?.toShortText().orEmpty()}")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            key(selectingStart, activeDate) {
                val pickerState = rememberDatePickerState(
                    initialSelectedDateMillis = activeDate?.let(::localDateToEpochMillis),
                )
                LaunchedEffect(pickerState.selectedDateMillis) {
                    pickerState.selectedDateMillis?.let { selectedMillis ->
                        if (selectingStart) {
                            startDate = epochMillisToLocalDate(selectedMillis)
                        } else {
                            endDate = epochMillisToLocalDate(selectedMillis)
                        }
                    }
                }
                DatePicker(state = pickerState)
            }
        }
    }
}

private fun StatisticsHeatmapState.monthIndex(date: LocalDate): Int =
    months.indexOfFirst { month -> month.year == date.year && month.monthNumber == date.monthNumber }

private fun heatmapDayColor(count: Int): Color = when {
    count <= 0 -> Color.Transparent
    count == 1 -> Color(0xFFD7EFC1)
    count < 5 -> Color(0xFFA9D97D)
    count < 8 -> Color(0xFF77BE4B)
    else -> Color(0xFF3F8E2F)
}

private fun LocalDate.toShortText(): String =
    "${monthNumber.toString().padStart(2, '0')}/${dayOfMonth.toString().padStart(2, '0')}/$year"
