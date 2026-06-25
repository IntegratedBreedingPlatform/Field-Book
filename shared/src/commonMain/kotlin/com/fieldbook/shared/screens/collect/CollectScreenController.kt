package com.fieldbook.shared.screens.collect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
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
import com.fieldbook.shared.generated.resources.trait_error_maximum_value
import com.fieldbook.shared.generated.resources.trait_error_minimum_value
import com.fieldbook.shared.generated.resources.activity_collect_unlocked_state
import com.fieldbook.shared.objects.RangeObject
import com.fieldbook.shared.preferences.GeneralKeys
import com.fieldbook.shared.screens.datagrid.DataGridSelection
import com.fieldbook.shared.theme.AppColors
import com.fieldbook.shared.traits.Formats
import com.russhwolf.settings.Settings
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

// TODO refactor to use ViewModel() ?
class CollectScreenController {
    private val traitRepository = TraitRepository()
    private val observationRepository = ObservationRepository()
    private val observationUnitRepository = ObservationUnitRepository()
    private val observationUnitPropertyRepository = ObservationUnitPropertyRepository()
    private val studyRepository = StudyRepository()

    private val settings: Settings = Settings()

    val studyId: Int = settings.getInt(GeneralKeys.SELECTED_FIELD_ID.key, 0)
    val field: FieldObject = studyRepository.getById(studyId)

    var units by mutableStateOf<List<ObservationUnitModel>>(emptyList())
        private set
    var rangeID by mutableStateOf<Array<Int>>(emptyArray())
        private set

    var unitLoading by mutableStateOf(true)
        private set
    var unitError by mutableStateOf<String?>(null)
        private set
    var currentUnitIndex by mutableStateOf(0)
        private set

    var traits by mutableStateOf<List<TraitObject>>(emptyList())
        private set
    var traitLoading by mutableStateOf(true)
        private set
    var traitError by mutableStateOf<String?>(null)
        private set
    var currentTraitIndex by mutableStateOf(0)
        private set

    var traitValues by mutableStateOf<Map<Long, List<String>>>(emptyMap())
        private set
    var traitValuesLoading by mutableStateOf(true)
        private set
    private var lastUnitId: String? = null
    private var restoredUnitSelection = false
    private var restoredTraitSelection = false
    var inputValidationMessage by mutableStateOf<String?>(null)
        private set
    var collectInteractionLocked by mutableStateOf(false)
        private set
    var dataLockState by mutableStateOf(
        CollectDataLockState.fromPersistedValue(
            settings.getInt(GeneralKeys.DATA_LOCK_STATE.key, CollectDataLockState.UNLOCKED.persistedValue)
        )
    )
        private set
    private val suppressedDefaultEntries = mutableSetOf<String>()
    private var currentObservationHadInitialValue by mutableStateOf(false)

    val primaryId = settings.getString(GeneralKeys.PRIMARY_NAME.key, "")
    val secondaryId = settings.getString(GeneralKeys.SECONDARY_NAME.key, "")
    val uniqueId = settings.getString(GeneralKeys.UNIQUE_NAME.key, "")

    var cRange: RangeObject by mutableStateOf(RangeObject())

    init {
        loadUnits()
        loadTraits()
        loadTraitValues()
    }

    fun updateCurrentRange(id: Int) {
        try {
            cRange = observationUnitPropertyRepository.getRangeFromId(
                id.toLong(),
                primaryId,
                secondaryId,
                uniqueId
            )
        } catch (e: Exception) {
            // On error, ensure UI doesn't crash and show an empty range
            e.printStackTrace()
            cRange = RangeObject("", "", "")
        }
    }

    private fun loadUnits() {
        try {
            units = observationUnitRepository.getAllObservationUnits(studyId.toLong())
            rangeID = observationUnitPropertyRepository.allRangeID(studyId)
            restoreLastUnitSelection()
            unitLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            unitError = e.message
            unitLoading = false
        }
    }

    private fun loadTraits() {
        try {
            traits = traitRepository.getVisibleTraitsWithAttributes()
            restoreLastTraitSelection()
            traitLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            traitError = e.message
            traitLoading = false
        }
    }

    fun updateCurrentUnitIndex(index: Int): Boolean {
        if (index in units.indices && validateCurrentTraitValue()) {
            currentUnitIndex = index
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
            currentTraitIndex = index
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

    private fun loadTraitValues() {
        val unit = units.getOrNull(currentUnitIndex)
        val plotId = unit?.observation_unit_db_id
        if (plotId != null && plotId != lastUnitId) {
            traitValuesLoading = true
            traitValues = observationRepository.getUserDetail(studyId.toLong(), plotId)
            traitValuesLoading = false
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
            currentEntryKey(plotId, trait.id!!)?.let { entryKey ->
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
            traitValues = traitValues.toMutableMap().apply {
                put(trait.id!!, listOf(value))
            }
        }
    }

    fun setCurrentTraitNa() {
        updateCurrentTraitValue("NA")
    }

    fun updateCurrentUnitGeoCoordinates(geoCoordinates: String) {
        if (!canMutateCurrentObservation()) {
            showCurrentDataLockMessage()
            return
        }
        val unit = units.getOrNull(currentUnitIndex)
        val unitDbId = unit?.observation_unit_db_id ?: return

        observationUnitRepository.updateGeoCoordinates(
            studyId = studyId.toLong(),
            observationUnitDbId = unitDbId,
            geoCoordinates = geoCoordinates,
        )

        units = units.toMutableList().also { updated ->
            val current = updated.getOrNull(currentUnitIndex) ?: return@also
            updated[currentUnitIndex] = current.copy(
                map = current.map.toMutableMap().apply {
                    put("geo_coordinates", geoCoordinates)
                }
            )
        }
    }

    fun shouldUseDefaultValue(traitId: Long?): Boolean {
        val unit = units.getOrNull(currentUnitIndex)
        val plotId = unit?.observation_unit_db_id
        if (plotId == null || traitId == null) return false

        val currentValue = traitValues[traitId]?.firstOrNull().orEmpty()
        if (currentValue.isNotEmpty()) return false

        return currentEntryKey(plotId, traitId)?.let { it !in suppressedDefaultEntries } == true
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
            traitValues = traitValues.toMutableMap().apply {
                val sanitizedCurrentList = currentList.filterNot { it == "NA" }
                put(traitId, sanitizedCurrentList + value)
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
            traitValues = traitValues.toMutableMap().apply {
                val currentList = get(trait.id!!).orEmpty().toMutableList()
                currentList.remove(value)
                if (currentList.isEmpty()) {
                    remove(trait.id!!)
                } else {
                    put(trait.id!!, currentList)
                }
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
        inputValidationMessage = null
    }

    fun showInputValidationMessage(message: String) {
        inputValidationMessage = message
    }

    fun updateCollectInteractionLocked(locked: Boolean) {
        collectInteractionLocked = locked
    }

    fun cycleDataLockState() {
        dataLockState = when (dataLockState) {
            CollectDataLockState.UNLOCKED -> CollectDataLockState.LOCKED
            CollectDataLockState.LOCKED -> CollectDataLockState.FROZEN
            CollectDataLockState.FROZEN -> CollectDataLockState.UNLOCKED
        }
        settings.putInt(GeneralKeys.DATA_LOCK_STATE.key, dataLockState.persistedValue)
        showCurrentDataLockMessage()
    }

    fun isCurrentObservationLocked(): Boolean {
        return when (dataLockState) {
            CollectDataLockState.UNLOCKED -> false
            CollectDataLockState.LOCKED -> true
            CollectDataLockState.FROZEN -> currentObservationHadInitialValue
        }
    }

    fun canMutateCurrentObservation(): Boolean = !isCurrentObservationLocked()

    fun hasCurrentTraitValue(): Boolean {
        val traitId = traits.getOrNull(currentTraitIndex)?.id ?: return false
        return traitValues[traitId]?.firstOrNull().isNullOrBlank().not()
    }

    fun showCurrentDataLockMessage() {
        inputValidationMessage = runBlocking {
            when (dataLockState) {
                CollectDataLockState.UNLOCKED -> getString(Res.string.activity_collect_unlocked_state)
                CollectDataLockState.LOCKED -> getString(Res.string.activity_collect_locked_state)
                CollectDataLockState.FROZEN -> getString(Res.string.activity_collect_frozen_state)
            }
        }
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
            currentEntryKey(plotId, traitId)?.let { suppressedDefaultEntries.add(it) }
            observationRepository.deleteTraitByValue(
                plotId = plotId,
                traitDbId = traitId,
                value = currentValue,
                studyId = studyId.toLong()
            )
            traitValues = traitValues.toMutableMap().apply {
                remove(traitId)
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

        currentUnitIndex = if (restoredIndex >= 0) restoredIndex else 0
        rangeID.getOrNull(currentUnitIndex)?.let(::updateCurrentRange)
    }

    private fun restoreLastTraitSelection() {
        if (restoredTraitSelection || traits.isEmpty()) return
        restoredTraitSelection = true

        val lastTraitId = settings.getString(lastTraitKey(), "").trim()
            .ifEmpty { settings.getString(GeneralKeys.LAST_USED_TRAIT.key, "").trim() }
        if (lastTraitId.isEmpty()) {
            currentTraitIndex = 0
            return
        }

        val restoredIndex = traits.indexOfFirst { it.id?.toString() == lastTraitId }
        currentTraitIndex = if (restoredIndex >= 0) restoredIndex else 0
        refreshCurrentObservationLockState()
    }

    private fun refreshCurrentObservationLockState() {
        val traitId = traits.getOrNull(currentTraitIndex)?.id
        val currentValue = traitId?.let { traitValues[it]?.firstOrNull() }.orEmpty()
        currentObservationHadInitialValue = currentValue.isNotEmpty()
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
        inputValidationMessage = validateNumericTraitValue(trait, currentValue)
        if (inputValidationMessage != null) {
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
