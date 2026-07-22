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

data class StatisticsUiState(
    val loading: Boolean = true,
    val mode: StatisticsMode = StatisticsMode.TOTAL,
    val sections: List<StatisticsSection> = emptyList(),
    val error: String? = null,
)

class StatisticsScreenViewModel(
    private val repository: StatisticsRepository = StatisticsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching {
                withContext(Dispatchers.Default) {
                    repository.getObservations()
                }
            }.onSuccess { observations ->
                val mode = _uiState.value.mode
                _uiState.value = StatisticsUiState(
                    loading = false,
                    mode = mode,
                    sections = buildStatisticsSections(observations, mode),
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
                    buildStatisticsSections(repository.getObservations(), mode)
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
}

fun statisticsScreenViewModelFactory() = viewModelFactory {
    initializer {
        StatisticsScreenViewModel()
    }
}
