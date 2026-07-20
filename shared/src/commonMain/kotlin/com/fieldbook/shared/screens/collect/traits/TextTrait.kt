package com.fieldbook.shared.screens.collect.traits

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign

@Composable
fun TextTrait(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    defaultValue: String? = null,
    isEdited: Boolean = false,
    editedColor: Color = Color.Unspecified,
    closeKeyboardOnOpen: Boolean = false,
    enabled: Boolean = true,
) {
    var local by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(value) {
        if (local.text != value) {
            local = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    LaunchedEffect(closeKeyboardOnOpen) {
        if (!enabled || closeKeyboardOnOpen) {
            keyboardController?.hide()
        } else {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    BasicTextField(
        value = local,
        onValueChange = { v ->
            local = v.copy(selection = TextRange(v.text.length))
            onValueChange(v.text)
        },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            fontWeight = if (isEdited) FontWeight.Normal else FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontStyle = if (isEdited) FontStyle.Normal else FontStyle.Italic,
            color = if (editedColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else editedColor,
        ),
        decorationBox = { innerTextField ->
            if (local.text.isBlank() && !defaultValue.isNullOrBlank()) {
                Text(
                    text = defaultValue,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            innerTextField()
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions()
    )
}
