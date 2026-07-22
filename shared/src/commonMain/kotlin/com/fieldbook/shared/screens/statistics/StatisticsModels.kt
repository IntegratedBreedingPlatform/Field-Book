@file:OptIn(ExperimentalTime::class)

package com.fieldbook.shared.screens.statistics

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

private const val INTERVAL_THRESHOLD_MINUTES = 30L
private const val INTERVAL_THRESHOLD_SECONDS = INTERVAL_THRESHOLD_MINUTES * 60L

data class StatisticsObservation(
    val studyId: Long,
    val studyName: String?,
    val studyAlias: String?,
    val observationUnitId: String,
    val value: String?,
    val timestamp: String?,
    val collector: String?,
    val observationVariableName: String?,
    val observationVariableFieldBookFormat: String?,
)

data class StatisticsSection(
    val period: StatisticsPeriod,
    val title: String,
    val cards: List<StatisticsCard>,
)

data class StatisticsCard(
    val type: StatisticsCardType,
    val value: String,
    val details: StatisticsCardDetails? = null,
)

data class StatisticsCardDetails(
    val title: String = "",
    val lines: List<String> = emptyList(),
    val message: String? = null,
)

enum class StatisticsMode { TOTAL, YEAR, MONTH }

data class StatisticsPeriod(
    val mode: StatisticsMode,
    val key: String,
)

enum class StatisticsCardType {
    FIELDS,
    ENTRIES,
    DATA,
    HOURS,
    PEOPLE,
    PHOTOS,
    BUSIEST,
    MOST,
}

fun buildStatisticsSections(
    observations: List<StatisticsObservation>,
    mode: StatisticsMode,
): List<StatisticsSection> {
    val parseableObservations = observations.map { observation ->
        ParsedStatisticsObservation(
            observation = observation,
            instant = parseFieldBookInstant(observation.timestamp),
        )
    }

    return when (mode) {
        StatisticsMode.TOTAL -> listOf(
            buildStatisticsSection(
                title = "Total",
                period = StatisticsPeriod(mode, "total"),
                observations = parseableObservations,
            )
        )

        StatisticsMode.YEAR -> parseableObservations
            .groupBy { parsed -> parsed.localDate()?.year?.toString() ?: "Unknown" }
            .entries
            .sortedByDescending { it.key }
            .map { (year, group) ->
                buildStatisticsSection(
                    title = year,
                    period = StatisticsPeriod(mode, year),
                    observations = group,
                )
            }

        StatisticsMode.MONTH -> parseableObservations
            .groupBy { parsed -> parsed.localDate()?.let { "${it.year}-${it.monthNumber.toString().padStart(2, '0')}" } ?: "Unknown" }
            .entries
            .sortedByDescending { it.key }
            .map { (month, group) ->
                buildStatisticsSection(
                    title = monthTitle(month),
                    period = StatisticsPeriod(mode, month),
                    observations = group,
                )
            }
    }
}

private fun buildStatisticsSection(
    title: String,
    period: StatisticsPeriod,
    observations: List<ParsedStatisticsObservation>,
): StatisticsSection {
    val source = observations.map { it.observation }
    val fieldIds = source.map { it.studyId }.toSet()
    val fieldNames = source
        .distinctBy { it.studyId }
        .map { it.studyAlias?.ifBlank { null } ?: it.studyName?.ifBlank { null } ?: it.studyId.toString() }
        .sorted()
    val observationUnits = source.map { it.observationUnitId }.toSet()
    val collectors = source.mapNotNull { it.collector?.trim()?.takeIf(String::isNotEmpty) }.toSet().sorted()
    val imageCount = source.count { it.observationVariableFieldBookFormat.isCameraTraitFormat() }
    val dateCounts = observations
        .mapNotNull { it.localDate()?.toDisplayDate() }
        .groupingBy { it }
        .eachCount()
    val busiestDate = dateCounts.maxByOrNull { it.value }
    val unitCounts = source.groupingBy { it.observationUnitId }.eachCount()
    val mostObservedUnit = unitCounts.maxByOrNull { it.value }
    val mostObservedUnitLines = mostObservedUnit?.key?.let { unitId ->
        source
            .filter { it.observationUnitId == unitId }
            .map { observation ->
                listOfNotNull(
                    observation.observationVariableName?.ifBlank { null },
                    observation.value?.ifBlank { null },
                ).joinToString(": ").ifBlank { observation.observationUnitId }
            }
    }.orEmpty()

    val hours = activeHours(observations.mapNotNull { it.instant })

    return StatisticsSection(
        period = period,
        title = title,
        cards = listOf(
            StatisticsCard(
                type = StatisticsCardType.FIELDS,
                value = fieldIds.size.toString(),
                details = if (fieldNames.isNotEmpty()) StatisticsCardDetails(
                    title = "Fields imported in $title",
                    lines = fieldNames,
                ) else null,
            ),
            StatisticsCard(
                type = StatisticsCardType.ENTRIES,
                value = observationUnits.size.toString(),
                details = StatisticsCardDetails(message = "${observationUnits.size} entries have been phenotyped"),
            ),
            StatisticsCard(
                type = StatisticsCardType.DATA,
                value = source.size.toString(),
                details = StatisticsCardDetails(message = "${source.size} observations have been collected"),
            ),
            StatisticsCard(
                type = StatisticsCardType.HOURS,
                value = hours,
                details = StatisticsCardDetails(message = "$hours hours spent phenotyping"),
            ),
            StatisticsCard(
                type = StatisticsCardType.PEOPLE,
                value = collectors.size.toString(),
                details = if (collectors.isNotEmpty()) StatisticsCardDetails(
                    title = "List of People",
                    lines = collectors,
                ) else null,
            ),
            StatisticsCard(
                type = StatisticsCardType.PHOTOS,
                value = imageCount.toString(),
                details = StatisticsCardDetails(message = "$imageCount photos have been captured"),
            ),
            StatisticsCard(
                type = StatisticsCardType.BUSIEST,
                value = busiestDate?.key ?: "-",
                details = busiestDate?.let {
                    StatisticsCardDetails(message = "${it.value} observations were collected on ${it.key}")
                },
            ),
            StatisticsCard(
                type = StatisticsCardType.MOST,
                value = mostObservedUnit?.value?.toString() ?: "0",
                details = mostObservedUnit?.let {
                    StatisticsCardDetails(
                        title = it.key,
                        lines = mostObservedUnitLines,
                    )
                },
            ),
        ),
    )
}

private data class ParsedStatisticsObservation(
    val observation: StatisticsObservation,
    val instant: Instant?,
) {
    fun localDate(): LocalDate? = instant?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
}

private fun activeHours(instants: List<Instant>): String {
    val totalSeconds = instants
        .sorted()
        .zipWithNext()
        .sumOf { (previous, next) ->
            val seconds = next.epochSeconds - previous.epochSeconds
            if (seconds in 0..INTERVAL_THRESHOLD_SECONDS) seconds else 0L
        }

    return (totalSeconds / 3600.0).toStringWithTwoDecimals()
}

private fun parseFieldBookInstant(value: String?): Instant? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isEmpty()) return null

    val normalized = trimmed.replace(' ', 'T')
    val withoutFraction = normalized.replace(Regex("""\.\d{1,9}([+-]\d{2}:?\d{2}|Z)$"""), "$1")
    val localDateTime = normalized.substringBefore('.').take(19)

    return runCatching { Instant.parse(normalized) }.getOrNull()
        ?: runCatching { Instant.parse(withoutFraction) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(localDateTime).toInstant(TimeZone.currentSystemDefault()) }.getOrNull()
}

private fun LocalDate.toDisplayDate(): String =
    "${monthNumber.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}-${(year % 100).toString().padStart(2, '0')}"

private fun monthTitle(monthKey: String): String {
    val parts = monthKey.split('-')
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthKey
    val monthNumber = parts.getOrNull(1)?.toIntOrNull() ?: return monthKey
    val monthName = Month.entries.getOrNull(monthNumber - 1)?.name
        ?.lowercase()
        ?.replaceFirstChar { it.titlecase() }
        ?: return monthKey
    return "$monthName $year"
}

private fun String?.isCameraTraitFormat(): Boolean {
    return this == "photo" || this == "usb camera" || this == "gopro" || this == "canon"
}

private fun Double.toStringWithTwoDecimals(): String {
    val totalCents = kotlin.math.round(this * 100.0).toLong()
    val whole = totalCents / 100
    val cents = (totalCents % 100).toString().padStart(2, '0')
    return "$whole.$cents"
}
