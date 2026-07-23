package com.fieldbook.shared.screens.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.field_data_title
import com.fieldbook.shared.generated.resources.field_trait_chart_incompatible_format
import com.fieldbook.shared.generated.resources.field_trait_chart_no_data
import com.fieldbook.shared.generated.resources.field_trait_observation_total
import com.fieldbook.shared.generated.resources.ic_chart_bar
import com.fieldbook.shared.generated.resources.ic_chevron_down
import com.fieldbook.shared.generated.resources.ic_chevron_up
import com.fieldbook.shared.generated.resources.ic_eye
import com.fieldbook.shared.generated.resources.ic_ruler
import com.fieldbook.shared.generated.resources.ic_trait_angle
import com.fieldbook.shared.generated.resources.ic_trait_audio
import com.fieldbook.shared.generated.resources.ic_trait_boolean
import com.fieldbook.shared.generated.resources.ic_trait_camera
import com.fieldbook.shared.generated.resources.ic_trait_categorical
import com.fieldbook.shared.generated.resources.ic_trait_counter
import com.fieldbook.shared.generated.resources.ic_trait_date
import com.fieldbook.shared.generated.resources.ic_trait_gnss
import com.fieldbook.shared.generated.resources.ic_trait_gopro
import com.fieldbook.shared.generated.resources.ic_trait_location
import com.fieldbook.shared.generated.resources.ic_trait_multicat
import com.fieldbook.shared.generated.resources.ic_trait_numeric
import com.fieldbook.shared.generated.resources.ic_trait_percent
import com.fieldbook.shared.generated.resources.ic_trait_text
import com.fieldbook.shared.generated.resources.ic_trait_usb
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModel
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FieldDetailDataSection(
    traitDetails: List<FieldTraitDetailUiModel>,
    traitCount: String,
    observationCount: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_chart_bar),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.field_data_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DataSummaryChip(
                            icon = Res.drawable.ic_ruler,
                            text = traitCount
                        )
                        DataSummaryChip(
                            icon = Res.drawable.ic_eye,
                            text = observationCount
                        )
                    }
                }
                Icon(
                    painter = painterResource(
                        if (expanded) Res.drawable.ic_chevron_up else Res.drawable.ic_chevron_down
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            if (expanded) {
                if (traitDetails.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.field_trait_chart_no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    traitDetails.forEach { item ->
                        FieldTraitDetailCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataSummaryChip(
    icon: DrawableResource,
    text: String,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFDDEFCF))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Black
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )
    }
}

@Composable
private fun FieldTraitDetailCard(item: FieldTraitDetailUiModel) {
    var expanded by remember(item.traitId) { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F8F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(traitIconForFormat(item.format)),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            Res.string.field_trait_observation_total,
                            item.observationCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TraitCompletenessChart(item.completeness)
                Icon(
                    painter = painterResource(
                        if (expanded) Res.drawable.ic_chevron_up else Res.drawable.ic_chevron_down
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                TraitChart(item.chartData)
            }
        }
    }
}

@Composable
private fun TraitCompletenessChart(completeness: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val remaining = MaterialTheme.colorScheme.surfaceVariant
    val model = remember(completeness) {
        PieChartModel.build(
            completeness.coerceIn(0f, 1f),
            (1f - completeness).coerceIn(0f, 1f)
        )
    }
    val chart = rememberPieChart(
        sliceProvider = PieChart.SliceProvider.series(
            PieChart.Slice(fill = Fill(primary)),
            PieChart.Slice(fill = Fill(remaining)),
        ),
        innerSize = PieSize.Inner.fixed(34.dp),
    )

    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        PieChartHost(
            chart = chart,
            model = model,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "${(completeness * 100f).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TraitChart(chartData: TraitChartData) {
    when (chartData) {
        is TraitChartData.CategoryBars -> TraitHorizontalBarChart(chartData.bars)
        is TraitChartData.Histogram -> TraitColumnChart(chartData.bins)
        TraitChartData.IncompatibleFormat -> TraitChartMessage(
            stringResource(Res.string.field_trait_chart_incompatible_format)
        )
        TraitChartData.NoData -> TraitChartMessage(
            stringResource(Res.string.field_trait_chart_no_data)
        )
    }
}

@Composable
private fun TraitHorizontalBarChart(bars: List<ChartBar>) {
    if (bars.isEmpty()) {
        TraitChartMessage(stringResource(Res.string.field_trait_chart_no_data))
        return
    }

    val maxCount = bars.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val chartHeight = (40 + bars.size * 40).dp
    val labelWidth = 72.dp
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        bars.forEach { bar ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bar.label,
                    modifier = Modifier.width(labelWidth),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bar.count.toFloat() / maxCount.toFloat())
                            .height(40.dp)
                            .background(primary)
                            .border(1.dp, Color.Black)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = labelWidth),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AxisLabel("0")
            AxisLabel(maxCount.toString())
        }
    }
}

@Composable
private fun AxisLabel(text: String, width: Dp = 24.dp) {
    Text(
        text = text,
        modifier = Modifier.widthIn(min = width),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TraitColumnChart(bars: List<ChartBar>) {
    if (bars.isEmpty()) {
        TraitChartMessage(stringResource(Res.string.field_trait_chart_no_data))
        return
    }

    val primary = MaterialTheme.colorScheme.primary
    val column = rememberLineComponent(
        fill = Fill(primary),
        thickness = 20.dp,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    )
    val chart = rememberCartesianChart(
        rememberColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(column),
        ),
        startAxis = VerticalAxis.rememberStart(),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = remember(bars) {
                CartesianValueFormatter { _, value, _ ->
                    bars.getOrNull(value.toInt())?.label ?: value.toInt().toString()
                }
            },
            itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 1 }) },
        ),
        getXStep = { _ -> 1.0 },
    )
    val model = remember(bars) {
        CartesianChartModel(
            ColumnCartesianLayerModel.build {
                series(bars.map { it.count.coerceAtLeast(0) })
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CartesianChartHost(
            chart = chart,
            model = model,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
    }
}

@Composable
private fun TraitChartMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun traitIconForFormat(format: String): DrawableResource {
    return when (format) {
        "angle" -> Res.drawable.ic_trait_angle
        "audio" -> Res.drawable.ic_trait_audio
        "boolean" -> Res.drawable.ic_trait_boolean
        "categorical" -> Res.drawable.ic_trait_categorical
        "counter" -> Res.drawable.ic_trait_counter
        "date" -> Res.drawable.ic_trait_date
        "gnss" -> Res.drawable.ic_trait_gnss
        "gopro" -> Res.drawable.ic_trait_gopro
        "location" -> Res.drawable.ic_trait_location
        "multicat" -> Res.drawable.ic_trait_multicat
        "numeric" -> Res.drawable.ic_trait_numeric
        "percent" -> Res.drawable.ic_trait_percent
        "photo" -> Res.drawable.ic_trait_camera
        "text" -> Res.drawable.ic_trait_text
        "usb camera" -> Res.drawable.ic_trait_usb
        else -> Res.drawable.ic_trait_categorical
    }
}
