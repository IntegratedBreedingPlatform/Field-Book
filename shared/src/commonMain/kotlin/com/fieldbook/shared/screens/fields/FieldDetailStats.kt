package com.fieldbook.shared.screens.fields

import com.fieldbook.shared.database.models.TraitObject
import com.fieldbook.shared.utilities.CategoryJsonUtil
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

data class FieldTraitDetailUiModel(
    val traitId: Long,
    val title: String,
    val format: String,
    val categories: String?,
    val observationCount: Int,
    val completeness: Float,
    val chartData: TraitChartData,
)

sealed interface TraitChartData {
    data class Histogram(val bins: List<ChartBar>) : TraitChartData
    data class CategoryBars(val bars: List<ChartBar>) : TraitChartData
    data object NoData : TraitChartData
    data object IncompatibleFormat : TraitChartData
}

data class ChartBar(
    val label: String,
    val count: Int,
)

private val nonChartableFormats = setOf(
    "audio",
    "gnss",
    "gopro",
    "location",
    "photo",
    "text",
    "usb camera",
)

fun buildFieldTraitDetails(
    traits: List<TraitObject>,
    observationsByTraitId: Map<Long, List<String>>,
    entryCount: Int,
): List<FieldTraitDetailUiModel> {
    return traits.mapNotNull { trait ->
        val traitId = trait.id ?: return@mapNotNull null
        val format = trait.format.orEmpty().lowercase()
        val rawObservations = observationsByTraitId[traitId].orEmpty()
        val filteredObservations = rawObservations
            .mapNotNull { normalizeObservationValue(format, it) }
            .filter { it.isNotEmpty() && it != "NA" }
        val observationCount = rawObservations.size
        val completeness = if (entryCount > 0) {
            (filteredObservations.size.toFloat() / entryCount.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        FieldTraitDetailUiModel(
            traitId = traitId,
            title = trait.name,
            format = format,
            categories = trait.categories,
            observationCount = observationCount,
            completeness = completeness,
            chartData = buildTraitChartData(
                format = format,
                categories = trait.categories,
                observations = filteredObservations,
            ),
        )
    }
}

private fun buildTraitChartData(
    format: String,
    categories: String?,
    observations: List<String>,
): TraitChartData {
    if (observations.isEmpty()) return TraitChartData.NoData
    if (format in nonChartableFormats) return TraitChartData.IncompatibleFormat
    if (format == "categorical") {
        return TraitChartData.CategoryBars(buildCategoryBars(observations, parseCategories(categories)))
    }

    val numericValues = observations.map { it.toDoubleOrNull() }
    return if (numericValues.all { it != null }) {
        TraitChartData.Histogram(buildHistogramBins(numericValues.filterNotNull()))
    } else {
        TraitChartData.CategoryBars(buildCategoryBars(observations, parseCategories(categories)))
    }
}

private fun buildCategoryBars(
    observations: List<String>,
    parsedCategories: List<String>,
): List<ChartBar> {
    val counts = observations.groupingBy { it }.eachCount()
    val sortedCategories = if (parsedCategories.isNotEmpty()) {
        parsedCategories.filter { counts.containsKey(it) }.reversed().ifEmpty {
            counts.keys.sorted()
        }
    } else {
        counts.keys.sorted()
    }

    return sortedCategories.map { category ->
        ChartBar(label = category, count = counts[category] ?: 0)
    }
}

internal fun buildHistogramBins(
    values: List<Double>,
): List<ChartBar> {
    if (values.isEmpty()) return emptyList()

    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: 0.0
    val range = maxValue - minValue
    if (range == 0.0) {
        return listOf(ChartBar(label = minValue.toInt().toString(), count = values.size))
    }

    val binCount = calculateHistogramBinCount(values.size)
    val binSize = ceil(range / binCount).coerceAtLeast(1.0)
    val bins = mutableMapOf<Int, Int>()

    values.forEach { value ->
        val binIndex = floor((value - minValue) / binSize).toInt().coerceAtLeast(0)
        bins[binIndex] = (bins[binIndex] ?: 0) + 1
    }

    val maxBinIndex = maxOf(binCount - 1, bins.keys.maxOrNull() ?: 0)
    return (0..maxBinIndex).map { index ->
        val binStart = minValue + binSize * index
        val binEnd = minValue + binSize * (index + 1)

        ChartBar(
            label = formatHistogramBinLabel(binStart, binEnd, binSize),
            count = bins[index] ?: 0,
        )
    }
}

private fun calculateHistogramBinCount(valueCount: Int): Int {
    val sqrtBinCount = sqrt(valueCount.toDouble())
    return max(
        1,
        if (valueCount in 4..6) {
            floor(sqrtBinCount).toInt()
        } else {
            ceil(sqrtBinCount).toInt()
        }
    )
}

private fun formatHistogramBinLabel(
    binStart: Double,
    binEnd: Double,
    binSize: Double,
): String {
    val start = formatHistogramBoundary(binStart)
    if (binSize == 1.0) return start

    return "$start-${formatHistogramBoundary(binEnd)}"
}

private fun formatHistogramBoundary(value: Double): String {
    return value.toInt().toString()
}

private fun parseCategories(categories: String?): List<String> {
    return try {
        if (categories.isNullOrEmpty()) {
            emptyList()
        } else if (categories.startsWith("[")) {
            CategoryJsonUtil.decode(categories).mapNotNull { it.value }
        } else {
            categories.split("/").map { it.trim() }.filter { it.isNotEmpty() }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun normalizeObservationValue(format: String, value: String): String? {
    return CategoryJsonUtil.processValue(
        mapOf(
            "observation_variable_field_book_format" to format,
            "value" to value,
        )
    )
}
