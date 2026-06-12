package com.fieldbook.shared.screens.collect.traits

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fieldbook.shared.theme.Button
import com.fieldbook.shared.theme.numericButtonDefaults

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumericTrait(
    value: String,
    defaultValue: String? = null,
    useDefaultValue: Boolean = true,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var defaultSuppressed by remember(value, useDefaultValue) {
        mutableStateOf(!useDefaultValue)
    }
    val effectiveValue = if (value.isEmpty() && !defaultSuppressed) defaultValue.orEmpty() else value
    val buttons = listOf(
        listOf(";", "1", "2", "3"),
        listOf("+", "4", "5", "6"),
        listOf("-", "7", "8", "9"),
        listOf("*", ".", "0", "⌫")
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { label ->
                    Button(
                        onClick = {
                            when (label) {
                                "⌫" -> if (effectiveValue.isNotEmpty()) {
                                    val nextValue = effectiveValue.dropLast(1)
                                    defaultSuppressed = nextValue.isEmpty()
                                    onValueChange(nextValue)
                                }
                                "." -> if (!effectiveValue.contains('.')) {
                                    defaultSuppressed = false
                                    onValueChange(effectiveValue + label)
                                }
                                else -> {
                                    defaultSuppressed = false
                                    onValueChange(effectiveValue + label)
                                }
                            }
                        },
                        selected = false,
                        modifier = Modifier
                            .numericButtonDefaults()
                            .weight(1f)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (label == "⌫") {
                                        defaultSuppressed = true
                                        onValueChange("")
                                    }
                                }
                            ),
                    ) {
                        Text(label, color = Color.Black)
                    }
                }
            }
        }
    }
}
