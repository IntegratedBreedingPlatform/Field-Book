package com.fieldbook.shared.screens.collect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.cash.sqldelight.db.QueryResult
import com.fieldbook.shared.AppContext
import com.fieldbook.shared.database.Migrator
import com.fieldbook.shared.database.repository.ObservationUnitAttributeRepository
import com.fieldbook.shared.database.repository.TraitRepository
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.close
import com.fieldbook.shared.generated.resources.dialog_att_chooser_attributes
import com.fieldbook.shared.generated.resources.dialog_att_chooser_other
import com.fieldbook.shared.generated.resources.dialog_att_chooser_title_default
import com.fieldbook.shared.generated.resources.dialog_att_chooser_traits
import com.fieldbook.shared.generated.resources.dialog_back
import com.fieldbook.shared.generated.resources.dialog_close
import com.fieldbook.shared.generated.resources.ic_plus
import com.fieldbook.shared.generated.resources.ic_tb_contains
import com.fieldbook.shared.generated.resources.ic_tb_delete
import com.fieldbook.shared.generated.resources.ic_tb_equal
import com.fieldbook.shared.generated.resources.ic_tb_greater_than
import com.fieldbook.shared.generated.resources.ic_tb_less_than
import com.fieldbook.shared.generated.resources.ic_tb_not_equal
import com.fieldbook.shared.generated.resources.main_toolbar_search
import com.fieldbook.shared.generated.resources.search_dialog_clear
import com.fieldbook.shared.generated.resources.search_dialog_query_contains
import com.fieldbook.shared.generated.resources.search_dialog_query_is_equal_to
import com.fieldbook.shared.generated.resources.search_dialog_query_is_less_than
import com.fieldbook.shared.generated.resources.search_dialog_query_is_more_than
import com.fieldbook.shared.generated.resources.search_dialog_query_is_not_equal_to
import com.fieldbook.shared.generated.resources.search_dialog_search
import com.fieldbook.shared.generated.resources.search_results_dialog_title
import com.fieldbook.shared.generated.resources.search_results_missing
import com.fieldbook.shared.traits.Formats
import com.fieldbook.shared.utilities.BrAPIScaleValidValuesCategories
import com.fieldbook.shared.utilities.CategoryJsonUtil
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class CollectSearchTargetType { ATTRIBUTE, TRAIT }

private data class CollectSearchTarget(
    val label: String,
    val type: CollectSearchTargetType,
    val traitId: Long? = null,
    val traitFormat: String? = null,
    val visible: Boolean = true,
)

private data class CollectSearchTargetGroups(
    val attributes: List<CollectSearchTarget>,
    val traits: List<CollectSearchTarget>,
    val other: List<CollectSearchTarget>,
) {
    val all: List<CollectSearchTarget> = attributes + traits + other
}

private enum class CollectSearchOperator(val icon: DrawableResource) {
    EQUAL(Res.drawable.ic_tb_equal),
    NOT_EQUAL(Res.drawable.ic_tb_not_equal),
    CONTAINS(Res.drawable.ic_tb_contains),
    GREATER_THAN(Res.drawable.ic_tb_greater_than),
    LESS_THAN(Res.drawable.ic_tb_less_than),
}

private data class CollectSearchCriterion(
    val target: CollectSearchTarget,
    val operator: CollectSearchOperator = CollectSearchOperator.EQUAL,
    val text: String = "",
)

private data class CollectSearchResult(
    val id: Long,
    val unique: String,
    val primary: String,
    val secondary: String,
    val extraValues: List<String>,
)

private class CollectSearchRepository {
    private val driver get() = AppContext.driverFactory().getDriver()
    private val traitRepository = TraitRepository()
    private val attributeRepository = ObservationUnitAttributeRepository()

    fun targetGroups(studyId: Long): CollectSearchTargetGroups {
        val attributes = attributeRepository.getAllNames(studyId)
            .filter { it != "geo_coordinates" }
            .map { CollectSearchTarget(label = it, type = CollectSearchTargetType.ATTRIBUTE) }

        val traits = traitRepository.getAllTraitsWithAttributes()
            .mapNotNull { trait ->
                val traitId = trait.id ?: return@mapNotNull null
                CollectSearchTarget(
                    label = trait.name,
                    type = CollectSearchTargetType.TRAIT,
                    traitId = traitId,
                    traitFormat = trait.format,
                    visible = trait.visible?.lowercase() != "false",
                )
            }

        val visibleTraits = traits.filter { it.visible }
        val otherTraits = traits.filterNot { it.visible }
        val sorter = compareBy<CollectSearchTarget> { it.label.lowercase() }

        return CollectSearchTargetGroups(
            attributes = attributes.sortedWith(sorter),
            traits = visibleTraits.sortedWith(sorter),
            other = otherTraits.sortedWith(sorter),
        )
    }

    fun search(
        criteria: List<CollectSearchCriterion>,
        studyId: Long,
        uniqueName: String,
        primaryName: String,
        secondaryName: String,
    ): List<CollectSearchResult> {
        if (criteria.isEmpty() || uniqueName.isBlank() || primaryName.isBlank() || secondaryName.isBlank()) return emptyList()

        val params = mutableListOf<Any?>()
        val query = criteria.joinToString(" INTERSECT ") { criterion ->
            buildCriterionQuery(
                criterion = criterion,
                studyId = studyId,
                uniqueName = uniqueName,
                primaryName = primaryName,
                secondaryName = secondaryName,
                params = params,
            )
        }

        val baseResults = executeResultsQuery(query, params)
        if (baseResults.isEmpty()) return emptyList()

        val resultColumns = buildResultColumns(criteria, primaryName, secondaryName)
        val extraColumns = resultColumns.drop(2)
        if (extraColumns.isEmpty()) return baseResults

        return baseResults.map { result ->
            result.copy(
                extraValues = extraColumns.map { target ->
                    when (target.type) {
                        CollectSearchTargetType.ATTRIBUTE -> getAttributeValue(uniqueName, result.unique, target.label)
                        CollectSearchTargetType.TRAIT -> getTraitValue(studyId, result.unique, target)
                    }
                }
            )
        }
    }

    fun resultHeaders(criteria: List<CollectSearchCriterion>, primaryName: String, secondaryName: String): List<String> {
        return buildResultColumns(criteria, primaryName, secondaryName).map { it.label }
    }

    private fun buildCriterionQuery(
        criterion: CollectSearchCriterion,
        studyId: Long,
        uniqueName: String,
        primaryName: String,
        secondaryName: String,
        params: MutableList<Any?>,
    ): String {
        val projection = "OUP.id, OUP.${sqlIdentifier(uniqueName)}, OUP.${sqlIdentifier(primaryName)}, OUP.${sqlIdentifier(secondaryName)}"
        val text = criterion.text.trim()
        val isCategorical = criterion.target.isCategorical()

        return when (criterion.target.type) {
            CollectSearchTargetType.ATTRIBUTE -> {
                params += criterion.operator.bindValue(text, isCategorical = false)
                """
                    SELECT $projection
                    FROM ${Migrator.sObservationUnitPropertyViewName} AS OUP
                    WHERE OUP.id IS NOT NULL
                      AND OUP.${sqlIdentifier(criterion.target.label)} ${criterion.operator.sqlForAttribute()} ?
                """.trimIndent()
            }

            CollectSearchTargetType.TRAIT -> {
                params += studyId
                params += criterion.target.label
                params += criterion.operator.bindValue(text, isCategorical = isCategorical)
                """
                    SELECT $projection
                    FROM ${Migrator.sObservationUnitPropertyViewName} AS OUP
                    JOIN observations AS O ON O.observation_unit_id = OUP.${sqlIdentifier(uniqueName)}
                    JOIN observation_variables AS V ON O.observation_variable_db_id = V.internal_id_observation_variable
                    WHERE OUP.id IS NOT NULL
                      AND O.study_id = ?
                      AND V.observation_variable_name = ?
                      AND ${criterion.operator.sqlForTrait()} ?
                """.trimIndent()
            }
        }
    }

    private fun executeResultsQuery(sql: String, params: List<Any?>): List<CollectSearchResult> {
        return driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                val results = mutableListOf<CollectSearchResult>()
                while (cursor.next().value) {
                    results += CollectSearchResult(
                        id = cursor.getLong(0) ?: 0L,
                        unique = cursor.getString(1) ?: "",
                        primary = cursor.getString(2) ?: "",
                        secondary = cursor.getString(3) ?: "",
                        extraValues = emptyList(),
                    )
                }
                QueryResult.Value(results)
            },
            parameters = params.size,
        ) {
            params.forEachIndexed { index, value -> bindParam(index, value) }
        }.value
    }

    private fun getAttributeValue(uniqueName: String, unique: String, label: String): String {
        val sql = """
            SELECT ${sqlIdentifier(label)}
            FROM ${Migrator.sObservationUnitPropertyViewName}
            WHERE ${sqlIdentifier(uniqueName)} = ?
            LIMIT 1
        """.trimIndent()

        return driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getString(0).orEmpty() else "")
            },
            parameters = 1,
        ) { bindString(0, unique) }.value
    }

    private fun getTraitValue(studyId: Long, unique: String, target: CollectSearchTarget): String {
        val traitId = target.traitId ?: return ""
        val sql = """
            SELECT value
            FROM observations
            WHERE study_id = ? AND observation_unit_id = ? AND observation_variable_db_id = ?
            ORDER BY internal_id_observation
        """.trimIndent()

        val values = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                val values = mutableListOf<String>()
                while (cursor.next().value) values += cursor.getString(0).orEmpty()
                QueryResult.Value(values)
            },
            parameters = 3,
        ) {
            bindLong(0, studyId)
            bindString(1, unique)
            bindLong(2, traitId)
        }.value

        return values.joinToString(", ") { value -> target.decodeValue(value) }
    }

    private fun buildResultColumns(
        criteria: List<CollectSearchCriterion>,
        primaryName: String,
        secondaryName: String,
    ): List<CollectSearchTarget> {
        val columns = mutableListOf(
            CollectSearchTarget(primaryName, CollectSearchTargetType.ATTRIBUTE),
            CollectSearchTarget(secondaryName, CollectSearchTargetType.ATTRIBUTE),
        )
        criteria.map { it.target }.forEach { target ->
            if (columns.none { it.label == target.label }) columns += target
        }
        return columns
    }

    private fun CollectSearchOperator.sqlForAttribute(): String = when (this) {
        CollectSearchOperator.EQUAL -> "="
        CollectSearchOperator.NOT_EQUAL -> "!="
        CollectSearchOperator.CONTAINS -> "LIKE"
        CollectSearchOperator.GREATER_THAN -> ">"
        CollectSearchOperator.LESS_THAN -> "<"
    }

    private fun CollectSearchOperator.sqlForTrait(): String = when (this) {
        CollectSearchOperator.EQUAL -> "O.value ="
        CollectSearchOperator.NOT_EQUAL -> "O.value !="
        CollectSearchOperator.CONTAINS -> "O.value LIKE"
        CollectSearchOperator.GREATER_THAN -> "CAST(O.value AS INTEGER) >"
        CollectSearchOperator.LESS_THAN -> "CAST(O.value AS INTEGER) <"
    }

    private fun CollectSearchOperator.bindValue(text: String, isCategorical: Boolean): String {
        return when (this) {
            CollectSearchOperator.CONTAINS -> if (isCategorical) "%\"value\":\"%$text%\"%" else "%$text%"
            CollectSearchOperator.EQUAL,
            CollectSearchOperator.NOT_EQUAL -> if (isCategorical) encodeCategorical(text) else text
            CollectSearchOperator.GREATER_THAN,
            CollectSearchOperator.LESS_THAN -> text
        }
    }

    private fun CollectSearchTarget.isCategorical(): Boolean {
        val format = traitFormat?.lowercase() ?: return false
        return format == Formats.CATEGORICAL.databaseName ||
            format == Formats.MULTI_CATEGORICAL.databaseName ||
            format == "qualitative"
    }

    private fun CollectSearchTarget.decodeValue(value: String): String {
        if (!isCategorical()) return value
        return try {
            CategoryJsonUtil.decode(value).joinToString(", ") { it.value.orEmpty() }.ifBlank { value }
        } catch (_: Exception) {
            value
        }
    }

    private fun encodeCategorical(text: String): String {
        return CategoryJsonUtil.encode(arrayListOf(BrAPIScaleValidValuesCategories(label = text, value = text)))
    }

    private fun sqlIdentifier(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun app.cash.sqldelight.db.SqlPreparedStatement.bindParam(index: Int, value: Any?) {
        when (value) {
            null -> bindString(index, null)
            is Long -> bindLong(index, value)
            is Int -> bindLong(index, value.toLong())
            else -> bindString(index, value.toString())
        }
    }
}

@Composable
private fun operatorLabel(operator: CollectSearchOperator): String {
    return when (operator) {
        CollectSearchOperator.EQUAL -> stringResource(Res.string.search_dialog_query_is_equal_to)
        CollectSearchOperator.NOT_EQUAL -> stringResource(Res.string.search_dialog_query_is_not_equal_to)
        CollectSearchOperator.CONTAINS -> stringResource(Res.string.search_dialog_query_contains)
        CollectSearchOperator.GREATER_THAN -> stringResource(Res.string.search_dialog_query_is_more_than)
        CollectSearchOperator.LESS_THAN -> stringResource(Res.string.search_dialog_query_is_less_than)
    }
}

@Composable
fun CollectSearchDialog(
    controller: CollectScreenViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val repository = remember { CollectSearchRepository() }
    val targetGroups = remember(controller.studyId) { repository.targetGroups(controller.studyId.toLong()) }
    var criteria by remember(targetGroups) { mutableStateOf(emptyList<CollectSearchCriterion>()) }
    var results by remember { mutableStateOf<List<CollectSearchResult>?>(null) }
    var showingResults by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    val headers = remember(criteria, showingResults) {
        repository.resultHeaders(criteria, controller.primaryId, controller.secondaryId)
    }

    LaunchedEffect(visible) {
        if (!visible) showTargetPicker = false
    }

    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(if (showingResults) Res.string.search_results_dialog_title else Res.string.main_toolbar_search),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(painter = painterResource(Res.drawable.close), contentDescription = stringResource(Res.string.dialog_close))
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (showingResults) {
                    CollectSearchResults(
                        headers = headers,
                        results = results.orEmpty(),
                        onSelect = { result ->
                            if (controller.moveToUnit(result.unique)) onDismiss()
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showingResults = false }) {
                            Text(stringResource(Res.string.dialog_back))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onDismiss) {
                            Text(stringResource(Res.string.dialog_close))
                        }
                    }
                } else {
                    CollectSearchCriteriaEditor(
                        criteria = criteria,
                        canAddCriteria = targetGroups.all.isNotEmpty(),
                        onCriteriaChanged = {
                            criteria = it
                            results = null
                        },
                        onAddCriterion = { showTargetPicker = true },
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { criteria = emptyList(); results = null }) {
                            Text(stringResource(Res.string.search_dialog_clear))
                        }
                        Row {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(Res.string.dialog_close))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = criteria.isNotEmpty() && criteria.all { it.text.isNotBlank() },
                                onClick = {
                                    results = repository.search(
                                        criteria = criteria,
                                        studyId = controller.studyId.toLong(),
                                        uniqueName = controller.uniqueId,
                                        primaryName = controller.primaryId,
                                        secondaryName = controller.secondaryId,
                                    )
                                    showingResults = true
                                }
                            ) {
                                Text(stringResource(Res.string.search_dialog_search))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTargetPicker) {
        CollectSearchTargetPickerDialog(
            targetGroups = targetGroups,
            onTargetSelected = { target ->
                criteria = criteria + CollectSearchCriterion(target)
                results = null
                showTargetPicker = false
            },
            onDismiss = { showTargetPicker = false }
        )
    }
}

@Composable
private fun CollectSearchCriteriaEditor(
    criteria: List<CollectSearchCriterion>,
    canAddCriteria: Boolean,
    onCriteriaChanged: (List<CollectSearchCriterion>) -> Unit,
    onAddCriterion: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
    ) {
        items(criteria.indices.toList()) { index ->
            val criterion = criteria[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = criterion.target.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OperatorDropdown(
                    operator = criterion.operator,
                    onOperatorChanged = { operator ->
                        onCriteriaChanged(criteria.toMutableList().also { it[index] = criterion.copy(operator = operator) })
                    },
                    modifier = Modifier.size(48.dp),
                    showLabel = false,
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = criterion.text,
                    onValueChange = { text ->
                        onCriteriaChanged(criteria.toMutableList().also { it[index] = criterion.copy(text = text) })
                    },
                    singleLine = true,
                    modifier = Modifier.width(96.dp),
                )
                IconButton(onClick = { onCriteriaChanged(criteria.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_tb_delete),
                        contentDescription = null,
                    )
                }
            }
            Divider()
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 2.dp,
        ) {
            IconButton(
                enabled = canAddCriteria,
                onClick = onAddCriterion,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun CollectSearchTargetPickerDialog(
    targetGroups: CollectSearchTargetGroups,
    onTargetSelected: (CollectSearchTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    val tabs = listOf(
        stringResource(Res.string.dialog_att_chooser_attributes) to targetGroups.attributes,
        stringResource(Res.string.dialog_att_chooser_traits) to targetGroups.traits,
        stringResource(Res.string.dialog_att_chooser_other) to targetGroups.other,
    )
    var selectedTab by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, top = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.dialog_att_chooser_title_default),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = stringResource(Res.string.dialog_close),
                        )
                    }
                }
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tab.first) },
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(tabs[selectedTab].second) { target ->
                        Text(
                            text = target.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTargetSelected(target) }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Divider()
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.dialog_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun OperatorDropdown(
    operator: CollectSearchOperator,
    onOperatorChanged: (CollectSearchOperator) -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(painter = painterResource(operator.icon), contentDescription = null, modifier = Modifier.size(20.dp))
                if (showLabel) {
                    Spacer(Modifier.width(8.dp))
                    Text(operatorLabel(operator), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CollectSearchOperator.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(operatorLabel(item)) },
                    leadingIcon = { Icon(painter = painterResource(item.icon), contentDescription = null) },
                    onClick = {
                        onOperatorChanged(item)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CollectSearchResults(
    headers: List<String>,
    results: List<CollectSearchResult>,
    onSelect: (CollectSearchResult) -> Unit,
) {
    if (results.isEmpty()) {
        Text(text = stringResource(Res.string.search_results_missing))
        return
    }

    val horizontalScroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .padding(vertical = 8.dp),
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(132.dp),
                )
            }
        }
        Divider()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
        ) {
            items(results) { result ->
                val cells = listOf(result.primary, result.secondary) + result.extraValues
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(result) }
                        .horizontalScroll(horizontalScroll)
                        .padding(vertical = 12.dp),
                ) {
                    cells.forEach { cell ->
                        Text(
                            text = cell.ifBlank { "-" },
                            modifier = Modifier.width(132.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Divider()
            }
        }
    }
}
