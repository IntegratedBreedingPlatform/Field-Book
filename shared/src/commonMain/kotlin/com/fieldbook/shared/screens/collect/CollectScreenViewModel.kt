package com.fieldbook.shared.screens.collect

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldbook.shared.database.models.FieldObject
import com.fieldbook.shared.database.models.ObservationUnitModel
import com.fieldbook.shared.database.models.TraitObject
import com.fieldbook.shared.database.repository.ObservationRepository
import com.fieldbook.shared.database.repository.ObservationUnitPropertyRepository
import com.fieldbook.shared.database.repository.ObservationUnitRepository
import com.fieldbook.shared.database.repository.StudyRepository
import com.fieldbook.shared.database.repository.TraitRepository
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.activity_collect_frozen_state
import com.fieldbook.shared.generated.resources.activity_collect_locked_state
import com.fieldbook.shared.generated.resources.activity_collect_unlocked_state
import com.fieldbook.shared.generated.resources.trait_error_maximum_value
import com.fieldbook.shared.generated.resources.trait_error_minimum_value
import com.fieldbook.shared.objects.RangeObject
import com.fieldbook.shared.preferences.GeneralKeys
import com.fieldbook.shared.screens.datagrid.DataGridSelection
import com.fieldbook.shared.theme.AppColors
import com.fieldbook.shared.traits.Formats
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

enum class CollectDataLockState(val persistedValue: Int) {
    UNLOCKED(0),
    LOCKED(1),
    FROZEN(2);

    companion object {
        fun fromPersistedValue(value: Int): CollectDataLockState {
            return entries.firstOrNull { it.persistedValue == value } ?: UNLOCKED
        }
    }
}

data class CollectUiState(
    val units: List<ObservationUnitModel> = emptyList(),
    val rangeID: Array<Int> = emptyArray(),
    val unitLoading: Boolean = true,
    val unitError: String? = null,
    val currentUnitIndex: Int = 0,
    val traits: List<TraitObject> = emptyList(),
    val traitLoading: Boolean = true,
    val traitError: String? = null,
    val currentTraitIndex: Int = 0,
    val traitValues: Map<Long, List<String>> = emptyMap(),
    val traitValuesLoading: Boolean = true,
    val inputValidationMessage: String? = null,
    val collectInteractionLocked: Boolean = false,
    val dataLockState: CollectDataLockState = CollectDataLockState.UNLOCKED,
    val currentObservationHadInitialValue: Boolean = false,
    val cRange: RangeObject = RangeObject(),
)

class CollectScreenViewModel(
    private val traitRepository: TraitRepository = TraitRepository(),
    private val observationRepository: ObservationRepository = ObservationRepository(),
    private val observationUnitRepository: ObservationUnitRepository = ObservationUnitRepository(),
    private val observationUnitPropertyRepository: ObservationUnitPropertyRepository = ObservationUnitPropertyRepository(),
    private val studyRepository: StudyRepository = StudyRepository(),
    private val settings: Settings = Settings(),
) : ViewModel() {

    val studyId: Int = settings.getInt(GeneralKeys.SELECTED_FIELD_ID.key, 0)
    val field: FieldObject = studyRepository.getById(studyId)

    private val _uiState = MutableStateFlow(
        CollectUiState(
            dataLockState = CollectDataLockState.fromPersistedValue(
                settings.getInt(
                    GeneralKeys.DATA_LOCK_STATE.key,
                    CollectDataLockState.UNLOCKED.persistedValue
                )
            )
        )
    )
    val uiState: StateFlow<CollectUiState> = _uiState.asStateFlow()

    val units: List<ObservationUnitModel> get() = _uiState.value.units
    val rangeID: Array<Int> get() = _uiState.value.rangeID
    val currentUnitIndex: Int get() = _uiState.value.currentUnitIndex
    val traits: List<TraitObject> get() = _uiState.value.traits
    val currentTraitIndex: Int get() = _uiState.value.currentTraitIndex
    val traitValues: Map<Long, List<String>> get() = _uiState.value.traitValues
    val inputValidationMessage: String? get() = _uiState.value.inputValidationMessage
    val dataLockState: CollectDataLockState get() = _uiState.value.dataLockState

    private var lastUnitId: String? = null
    private var restoredUnitSelection = false
    private var restoredTraitSelection = false
    private val suppressedDefaultEntries = mutableSetOf<String>()

    val primaryId = settings.getString(GeneralKeys.PRIMARY_NAME.key, "")
    val secondaryId = settings.getString(GeneralKeys.SECONDARY_NAME.key, "")
    val uniqueId = settings.getString(GeneralKeys.UNIQUE_NAME.key, "")

    init {
        loadUnits()
        loadTraits()
        loadTraitValues()
    }

    private fun updateState(transform: CollectUiState.() -> CollectUiState) {
        _uiState.value = _uiState.value.transform()
    }

    fun updateCurrentRange(id: Int) {
        try {
            updateState {
                copy(
                    cRange = observationUnitPropertyRepository.getRangeFromId(
                        id.toLong(),
                        primaryId,
                        secondaryId,
                        uniqueId
                    )
                )
            }
        } catch (e: Exception) {
            // On error, ensure UI doesn't crash and show an empty range
            e.printStackTrace()
            updateState { copy(cRange = RangeObject("", "", "")) }
        }
    }

    private fun loadUnits() {
        try {
            updateState {
                copy(
                    units = observationUnitRepository.getAllObservationUnits(studyId.toLong()),
                    rangeID = observationUnitPropertyRepository.allRangeID(studyId)
                )
            }
            restoreLastUnitSelection()
            updateState { copy(unitLoading = false) }
        } catch (e: Exception) {
            e.printStackTrace()
            updateState { copy(unitError = e.message, unitLoading = false) }
        }
    }

    private fun loadTraits() {
        try {
            updateState { copy(traits = traitRepository.getVisibleTraitsWithAttributes()) }
            restoreLastTraitSelection()
            updateState { copy(traitLoading = false) }
        } catch (e: Exception) {
            e.printStackTrace()
            updateState { copy(traitError = e.message, traitLoading = false) }
        }
    }

    fun updateCurrentUnitIndex(index: Int): Boolean {
        if (index in units.indices && validateCurrentTraitValue()) {
            updateState { copy(currentUnitIndex = index) }
            rangeID.getOrNull(index)?.let(::updateCurrentRange)
            persistCurrentSelection()
            loadTraitValues()
            refreshCurrentObservationLockState()
            return true
        }
        return false
    }

    fun goToNextUnit(): Boolean {
        if (units.isEmpty()) return false
        val nextIndex = if (currentUnitIndex >= units.lastIndex) 0 else currentUnitIndex + 1
        return updateCurrentUnitIndex(nextIndex)
    }

    fun goToPreviousUnit(): Boolean {
        if (units.isEmpty()) return false
        val previousIndex = if (currentUnitIndex <= 0) units.lastIndex else currentUnitIndex - 1
        return updateCurrentUnitIndex(previousIndex)
    }

    fun updateCurrentTraitIndex(index: Int): Boolean {
        if (index in traits.indices && validateCurrentTraitValue()) {
            updateState { copy(currentTraitIndex = index) }
            persistCurrentSelection()
            refreshCurrentObservationLockState()
            return true
        }
        return false
    }

    fun goToNextTrait(): Boolean {
        if (traits.isEmpty()) return false
        val nextIndex = if (currentTraitIndex >= traits.lastIndex) 0 else currentTraitIndex + 1
        return updateCurrentTraitIndex(nextIndex)
    }

    fun goToPreviousTrait(): Boolean {
        if (traits.isEmpty()) return false
        val previousIndex = if (currentTraitIndex <= 0) traits.lastIndex else currentTraitIndex - 1
        return updateCurrentTraitIndex(previousIndex)
    }

    fun applyDataGridSelection(selection: DataGridSelection): Boolean {
        if (!validateCurrentTraitValue()) return false

        val unitIndex = units.indexOfFirst { it.observation_unit_db_id == selection.plotId }
        val traitIndex = selection.traitId
            ?.let { selectedTraitId -> traits.indexOfFirst { it.id == selectedTraitId } }
            ?.takeIf { it >= 0 }
            ?: traits.getOrNull(selection.traitIndex)?.let { selection.traitIndex }
            ?: -1

        if (unitIndex < 0 || traitIndex < 0) return false

        updateCurrentUnitIndex(unitIndex)
        updateCurrentTraitIndex(traitIndex)
        return true
    }

    fun moveToUnit(uniqueId: String): Boolean {
        if (!validateCurrentTraitValue()) return false

        val unitIndex = units.indexOfFirst { it.observation_unit_db_id == uniqueId }
        if (unitIndex < 0) return false

        return updateCurrentUnitIndex(unitIndex)
    }

    private fun loadTraitValues() {
        val unit = units.getOrNull(currentUnitIndex)
        val plotId = unit?.observation_unit_db_id
        if (plotId != null && plotId != lastUnitId) {
            updateState { copy(traitValuesLoading = true) }
            updateState {
                copy(
                    traitValues = observationRepository.getUserDetail(studyId.toLong(), plotId),
                    traitValuesLoading = false
                )
            }
            lastUnitId = plotId
        }
        refreshCurrentObservationLockState()
    }

    /**
     * Update the observation for the current trait and unit, and persist to DB.
     */
    fun updateCurrentTraitValue(value: String) {
        if (!canMutateCurrentObservation()) {
            showCurrentDataLockMessage()
            return
        }
        val trait = traits.getOrNull(currentTraitIndex)
        val unit = units.getOrNull(currentUnitIndex)
        val plotId = unit?.observation_unit_db_id

        if (plotId != null && trait?.id != null) {
            currentEntryKey(plotId, trait.id!!).let { entryKey ->
                if (value.isBlank()) {
                    suppressedDefaultEntries.add(entryKey)
                } else {
                    suppressedDefaultEntries.remove(entryKey)
                }
            }
            observationRepository.upsertObservation(
                plotId = plotId,
                traitDbId = trait.id!!,
                value = value,
                studyId = studyId.toLong()
            )
            updateState {
                copy(
                    traitValues = traitValues.toMutableMap().apply {
                        put(trait.id!!, listOf(value))
                    }
                )
            }
        }
    }

    fun setCurrentTraitNa() {
        updateCurrentTraitValue("NA")
    }

    fun shouldUseDefaultValue(traitId: Long?): Boolean {
        val unit = units.getOrNull(currentUnitIndex)
        val plotId = unit?.observation_unit_db_id
        if (plotId == null || traitId == null) return false

        val currentValue = traitValues[traitId]?.firstOrNull().orEmpty()
        if (currentValue.isNotEmpty()) return false

        return currentEntryKey(plotId, traitId).let { it !in suppressedDefaultEntries }
    }

    fun ensureCurrentTraitDefaultValueApplied() {
        val trait = traits.getOrNull(currentTraitIndex) ?: return
        val traitId = trait.id ?: return
        val defaultValue = trait.defaultValue?.trim().orEmpty()
        if (defaultValue.isEmpty()) return
        if (!shouldUseDefaultValue(traitId)) return

        val supportsDefault = trait.format.equals(Formats.NUMERIC.databaseName, ignoreCase = true) ||
            trait.format.equals(Formats.PERCENT.databaseName, ignoreCase = true) ||
            trait.format.equals(Formats.BOOLEAN.databaseName, ignoreCase = true)
        if (!supportsDefault) return

        if (trait.format.equals(Formats.BOOLEAN.databaseName, ignoreCase = true) &&
            defaultValue.equals("UNSET", ignoreCase = true)
        ) {
            return
        }

        updateCurrentTraitValue(defaultValue)
    }

    /**
     * Add a new observation for the current trait and unit, and persist to DB.
     */
    fun addCurrentTraitValue(value: String) {
        if (!canMutateCurrentObservation()) {
            showCurrentDataLockMessage()
            return
        }
        val trait = traits.getOrNull(currentTraitIndex)
        val unit = units.getOrNull(currentUnitIndex)
        val plotId = unit?.observation_unit_db_id

        if (plotId != null && trait?.id != null) {
            val traitId = trait.id!!
            val currentList = traitValues[traitId].orEmpty()
            if (currentList.size == 1 && currentList.first() == "NA") {
                observationRepository.deleteTraitByValue(
                    plotId = plotId,
                    traitDbId = traitId,
                    value = "NA",
                    studyId = studyId.toLong()
                )
            }
            observationRepository.insertObservation(
                plotId = plotId,
                traitDbId = traitId,
                value = value,
                studyId = studyId.toLong()
            )
            updateState {
                copy(
                    traitValues = traitValues.toMutableMap().apply {
                        val sanitizedCurrentList = currentList.filterNot { it == "NA" }
                        put(traitId, sanitizedCurrentList + value)
                    }
                )
            }
        }
    }

    /**
     * Remove one stored value for the current trait and unit.
     */
    fun deleteCurrentTraitValue(value: String) {
        if (!canMutateCurrentObservation()) {
            showCurrentDataLockMessage()
            return
        }
        val trait = traits.getOrNull(currentTraitIndex)
        val unit = units.getOrNull(currentUnitIndex)
        val plotId = unit?.observation_unit_db_id

        if (plotId != null && trait?.id != null) {
            observationRepository.deleteTraitByValue(
                plotId = plotId,
                traitDbId = trait.id!!,
                value = value,
                studyId = studyId.toLong()
            )
            updateState {
                copy(
                    traitValues = traitValues.toMutableMap().apply {
                        val currentList = get(trait.id!!).orEmpty().toMutableList()
                        currentList.remove(value)
                        if (currentList.isEmpty()) {
                            remove(trait.id!!)
                        } else {
                            put(trait.id!!, currentList)
                        }
                    }
                )
            }
        }
    }

    fun getDisplayColor(): Color {
        val defaultArgb = AppColors.fb_value_saved_color.argb
        var storedArgb = settings.getInt(GeneralKeys.SAVED_DATA_COLOR.key, defaultArgb)

        // Check if the alpha channel is 0 (fully transparent).
        // The 'ushr 24' operation isolates the alpha byte.
        if ((storedArgb ushr 24) == 0) {
            // If alpha is 0, assume it's an RGB value and make it fully opaque.
            storedArgb = storedArgb or 0xFF000000.toInt()
        }

        return Color(storedArgb)
    }

    fun clearInputValidationMessage() {
        updateState { copy(inputValidationMessage = null) }
    }

    fun showInputValidationMessage(message: String) {
        updateState { copy(inputValidationMessage = message) }
    }

    fun updateCollectInteractionLocked(locked: Boolean) {
        updateState { copy(collectInteractionLocked = locked) }
    }

    fun cycleDataLockState() {
        val nextState = when (dataLockState) {
            CollectDataLockState.UNLOCKED -> CollectDataLockState.LOCKED
            CollectDataLockState.LOCKED -> CollectDataLockState.FROZEN
            CollectDataLockState.FROZEN -> CollectDataLockState.UNLOCKED
        }
        updateState { copy(dataLockState = nextState) }
        settings.putInt(GeneralKeys.DATA_LOCK_STATE.key, nextState.persistedValue)
        showCurrentDataLockMessage()
    }

    fun isCurrentObservationLocked(): Boolean {
        return when (dataLockState) {
            CollectDataLockState.UNLOCKED -> false
            CollectDataLockState.LOCKED -> true
            CollectDataLockState.FROZEN -> _uiState.value.currentObservationHadInitialValue
        }
    }

    fun canMutateCurrentObservation(): Boolean = !isCurrentObservationLocked()

    fun hasCurrentTraitValue(): Boolean {
        val traitId = traits.getOrNull(currentTraitIndex)?.id ?: return false
        return traitValues[traitId]?.firstOrNull().isNullOrBlank().not()
    }

    fun showCurrentDataLockMessage() {
        val message = runBlocking {
            when (dataLockState) {
                CollectDataLockState.UNLOCKED -> getString(Res.string.activity_collect_unlocked_state)
                CollectDataLockState.LOCKED -> getString(Res.string.activity_collect_locked_state)
                CollectDataLockState.FROZEN -> getString(Res.string.activity_collect_frozen_state)
            }
        }
        updateState { copy(inputValidationMessage = message) }
    }

    fun clearCurrentTraitValue() {
        if (!canMutateCurrentObservation()) {
            showCurrentDataLockMessage()
            return
        }
        val trait = traits.getOrNull(currentTraitIndex)
        val unit = units.getOrNull(currentUnitIndex)
        val plotId = unit?.observation_unit_db_id
        val traitId = trait?.id
        val currentValue = traitId?.let { traitValues[it]?.firstOrNull() }.orEmpty()

        if (plotId != null && traitId != null) {
            currentEntryKey(plotId, traitId).let { suppressedDefaultEntries.add(it) }
            observationRepository.deleteTraitByValue(
                plotId = plotId,
                traitDbId = traitId,
                value = currentValue,
                studyId = studyId.toLong()
            )
            updateState {
                copy(
                    traitValues = traitValues.toMutableMap().apply {
                        remove(traitId)
                    }
                )
            }
            refreshCurrentObservationLockState()
        }
    }

    fun persistCurrentSelection() {
        currentRangeUniqueId()?.let {
            settings.putString(lastPlotKey(), it)
            settings.putString(GeneralKeys.LAST_PLOT.key, it)
        }
        traits.getOrNull(currentTraitIndex)?.id?.let {
            val traitId = it.toString()
            settings.putString(lastTraitKey(), traitId)
            settings.putString(GeneralKeys.LAST_USED_TRAIT.key, traitId)
        }
    }

    private fun restoreLastUnitSelection() {
        if (restoredUnitSelection || rangeID.isEmpty()) return
        restoredUnitSelection = true

        val lastPlot = settings.getString(lastPlotKey(), "").trim()
            .ifEmpty { settings.getString(GeneralKeys.LAST_PLOT.key, "").trim() }
        if (lastPlot.isEmpty()) {
            rangeID.firstOrNull()?.let(::updateCurrentRange)
            return
        }

        val restoredIndex = rangeID.indexOfFirst { rangeDbId ->
            try {
                observationUnitPropertyRepository.getRangeFromId(
                    rangeDbId.toLong(),
                    primaryId,
                    secondaryId,
                    uniqueId
                ).uniqueId == lastPlot
            } catch (_: Exception) {
                false
            }
        }

        updateState { copy(currentUnitIndex = if (restoredIndex >= 0) restoredIndex else 0) }
        rangeID.getOrNull(currentUnitIndex)?.let(::updateCurrentRange)
    }

    private fun restoreLastTraitSelection() {
        if (restoredTraitSelection || traits.isEmpty()) return
        restoredTraitSelection = true

        val lastTraitId = settings.getString(lastTraitKey(), "").trim()
            .ifEmpty { settings.getString(GeneralKeys.LAST_USED_TRAIT.key, "").trim() }
        if (lastTraitId.isEmpty()) {
            updateState { copy(currentTraitIndex = 0) }
            return
        }

        val restoredIndex = traits.indexOfFirst { it.id?.toString() == lastTraitId }
        updateState { copy(currentTraitIndex = if (restoredIndex >= 0) restoredIndex else 0) }
        refreshCurrentObservationLockState()
    }

    private fun refreshCurrentObservationLockState() {
        val traitId = traits.getOrNull(currentTraitIndex)?.id
        val currentValue = traitId?.let { traitValues[it]?.firstOrNull() }.orEmpty()
        updateState { copy(currentObservationHadInitialValue = currentValue.isNotEmpty()) }
    }

    private fun currentRangeUniqueId(): String? {
        val rangeDbId = rangeID.getOrNull(currentUnitIndex) ?: return null
        return try {
            observationUnitPropertyRepository.getRangeFromId(
                rangeDbId.toLong(),
                primaryId,
                secondaryId,
                uniqueId
            ).uniqueId
        } catch (_: Exception) {
            null
        }
    }

    private fun lastPlotKey(): String = "${GeneralKeys.LAST_PLOT.key}_$studyId"

    private fun lastTraitKey(): String = "${GeneralKeys.LAST_USED_TRAIT.key}_$studyId"

    private fun currentEntryKey(plotId: String, traitId: Long): String = "$studyId::$plotId::$traitId"

    private fun validateCurrentTraitValue(): Boolean {
        val trait = traits.getOrNull(currentTraitIndex) ?: return true
        val currentValue = trait.id?.let { traitValues[it]?.firstOrNull() }.orEmpty()
        val validationMessage = validateNumericTraitValue(trait, currentValue)
        updateState { copy(inputValidationMessage = validationMessage) }
        if (validationMessage != null) {
            clearCurrentTraitValue()
            return false
        }
        return true
    }

    private fun validateNumericTraitValue(trait: TraitObject, value: String): String? {
        val isNumericTrait = trait.format.equals(Formats.NUMERIC.databaseName, ignoreCase = true)
        if (!isNumericTrait) return null
        if (value.isBlank() || value == "NA") return null

        val minimum = trait.minimum?.trim().orEmpty()
        val maximum = trait.maximum?.trim().orEmpty()
        if (minimum.isEmpty() && maximum.isEmpty()) return null

        val parsedValue = value.toDoubleOrNull()

        if (maximum.isNotEmpty()) {
            val upperValue = maximum.toDoubleOrNull()
            if (parsedValue == null || upperValue == null || parsedValue > upperValue) {
                return runBlocking {
                    getString(Res.string.trait_error_maximum_value)
                }
            }
        }

        if (minimum.isNotEmpty()) {
            val lowerValue = minimum.toDoubleOrNull()
            if (parsedValue == null || lowerValue == null || parsedValue < lowerValue) {
                return runBlocking {
                    getString(Res.string.trait_error_minimum_value)
                }
            }
        }

        return null
    }
}

fun collectScreenViewModelFactory() = viewModelFactory {
    initializer {
        CollectScreenViewModel(
            traitRepository = TraitRepository(),
            observationRepository = ObservationRepository(),
            observationUnitRepository = ObservationUnitRepository(),
            observationUnitPropertyRepository = ObservationUnitPropertyRepository(),
            studyRepository = StudyRepository(),
            settings = Settings(),
        )
    }
}
