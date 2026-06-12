package com.fieldbook.shared.screens.collect.traits

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private enum class BooleanState(val dbValue: String) {
    FALSE("FALSE"),
    UNSET(""),
    TRUE("TRUE");

    companion object {
        fun fromValue(value: String): BooleanState {
            return when (value.trim().uppercase()) {
                "TRUE" -> TRUE
                "FALSE" -> FALSE
                else -> UNSET
            }
        }
    }
}

@Composable
fun BooleanTrait(
    value: String,
    defaultValue: String? = null,
    useDefaultValue: Boolean = true,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveValue = when {
        value.isNotBlank() -> value
        useDefaultValue -> defaultValue.orEmpty()
        else -> ""
    }

    var currentState by remember(value, defaultValue, useDefaultValue) {
        mutableStateOf(BooleanState.fromValue(effectiveValue))
    }

    fun updateState(state: BooleanState) {
        currentState = state
        onValueChange(state.dbValue)
    }

    val thumbOffset by animateDpAsState(
        targetValue = when (currentState) {
            BooleanState.FALSE -> 0.dp
            BooleanState.UNSET -> 30.dp
            BooleanState.TRUE -> 60.dp
        },
        label = "boolean-thumb-offset"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BooleanActionButton(
            selected = currentState == BooleanState.FALSE,
            background = Color(0xFFFF2A17),
            onClick = { updateState(BooleanState.FALSE) }
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "False",
                tint = Color.White
            )
        }

        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(2.dp, Color(0xFFC8C8C8), RoundedCornerShape(6.dp))
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable { updateState(BooleanState.FALSE) }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable { updateState(BooleanState.UNSET) }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable { updateState(BooleanState.TRUE) }
                )
            }

            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(width = 32.dp, height = 34.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (currentState) {
                            BooleanState.FALSE -> Color(0xFFFFD7D2)
                            BooleanState.UNSET -> Color(0xFFB4A28C)
                            BooleanState.TRUE -> Color(0xFFD5E8B5)
                        }
                    )
            )
        }

        BooleanActionButton(
            selected = currentState == BooleanState.TRUE,
            background = Color(0xFF7CB342),
            onClick = { updateState(BooleanState.TRUE) }
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "True",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun BooleanActionButton(
    selected: Boolean,
    background: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (selected) background else background.copy(alpha = 0.92f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
