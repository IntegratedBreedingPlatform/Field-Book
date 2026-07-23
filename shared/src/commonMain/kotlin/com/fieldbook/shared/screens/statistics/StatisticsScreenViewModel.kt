package com.fieldbook.shared.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

data class StatisticsUiState(
    val loading: Boolean = true,
    val mode: StatisticsMode = StatisticsMode.TOTAL,
    val sections: List<StatisticsSection> = emptyList(),
    val showHeatmap: Boolean = false,
    val heatmap: StatisticsHeatmapState = StatisticsHeatmapState(),
    val error: String? = null,
)

class StatisticsScreenViewModel(
    private val repository: StatisticsRepository = StatisticsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()
    private var observations: List<StatisticsObservation> = emptyList()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching {
                withContext(Dispatchers.Default) {
                    repository.getObservations()
                }
            }.onSuccess { loadedObservations ->
                observations = loadedObservations
                val mode = _uiState.value.mode
                _uiState.value = StatisticsUiState(
                    loading = false,
                    mode = mode,
                    showHeatmap = _uiState.value.showHeatmap,
                    sections = buildStatisticsSections(loadedObservations, mode),
                    heatmap = buildStatisticsHeatmap(loadedObservations),
                )
            }.onFailure { throwable ->
                _uiState.value = StatisticsUiState(
                    loading = false,
                    mode = _uiState.value.mode,
                    error = throwable.message ?: "Unable to load statistics",
                )
            }
        }
    }

    fun setMode(mode: StatisticsMode) {
        if (_uiState.value.mode == mode) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(mode = mode, loading = true, error = null)
            runCatching {
                withContext(Dispatchers.Default) {
                    val source = observations.ifEmpty {
                        repository.getObservations().also { observations = it }
                    }
                    buildStatisticsSections(source, mode)
                }
            }.onSuccess { sections ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    sections = sections,
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = throwable.message ?: "Unable to load statistics",
                )
            }
        }
    }

    fun openHeatmap() {
        _uiState.value = _uiState.value.copy(showHeatmap = true)
    }

    fun closeHeatmap() {
        _uiState.value = _uiState.value.copy(showHeatmap = false)
    }

    fun toggleHeatmapCounts() {
        val current = _uiState.value.heatmap
        _uiState.value = _uiState.value.copy(
            heatmap = buildStatisticsHeatmap(
                observations = observations,
                startDate = current.startDate,
                endDate = current.endDate,
                showCounts = !current.showCounts,
            )
        )
    }

    fun setHeatmapRange(startDate: LocalDate, endDate: LocalDate) {
        _uiState.value = _uiState.value.copy(
            heatmap = buildStatisticsHeatmap(
                observations = observations,
                startDate = startDate,
                endDate = endDate,
                showCounts = _uiState.value.heatmap.showCounts,
            )
        )
    }
}

fun statisticsScreenViewModelFactory() = viewModelFactory {
    initializer {
        StatisticsScreenViewModel()
    }
}
