package com.fieldbook.shared.screens.statistics

import com.fieldbook.shared.sqldelight.FieldbookDatabase
import com.fieldbook.shared.sqldelight.createDatabase

class StatisticsRepository(
    private val dbProvider: () -> FieldbookDatabase = { createDatabase() },
) {
    private val db: FieldbookDatabase
        get() = dbProvider()

    fun getObservations(): List<StatisticsObservation> {
        return db.observationsQueries.getStatisticsObservations()
            .executeAsList()
            .map { row ->
                StatisticsObservation(
                    studyId = row.study_id,
                    studyName = row.study_name,
                    studyAlias = row.study_alias,
                    observationUnitId = row.observation_unit_id,
                    value = row.value_,
                    timestamp = row.observation_time_stamp,
                    collector = row.collector,
                    observationVariableName = row.observation_variable_name,
                    observationVariableFieldBookFormat = row.observation_variable_field_book_format,
                )
            }
    }
}
