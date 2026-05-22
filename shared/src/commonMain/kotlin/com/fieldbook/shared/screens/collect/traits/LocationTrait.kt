package com.fieldbook.shared.screens.collect.traits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.activity_field_editor_no_location_yet
import com.fieldbook.shared.generated.resources.ic_trait_location
import com.fieldbook.shared.generated.resources.trait_location_save_content_description
import com.fieldbook.shared.theme.TraitButtonDefaultColor
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LocationTrait(
    value: String,
    onValueChange: (String) -> Unit,
    onValidationError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val saveContentDescription = stringResource(Res.string.trait_location_save_content_description)
    val noLocationFound = stringResource(Res.string.activity_field_editor_no_location_yet)
    var captureInProgress by remember(value) { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = {
                if (captureInProgress) return@Surface

                captureInProgress = true
                scope.launch {
                    val result = captureCurrentTraitLocation()
                    when {
                        result.location != null -> {
                            onValueChange(result.location.toStoredTraitValue())
                        }

                        result.failure == LocationCaptureFailure.SETTINGS_REQUIRED -> {
                            openTraitLocationSettings()
                        }

                        else -> {
                            onValidationError(noLocationFound)
                        }
                    }
                    captureInProgress = false
                }
            },
            modifier = Modifier
                .padding(8.dp)
                .size(112.dp),
            shape = CircleShape,
            color = TraitButtonDefaultColor,
            contentColor = Color.Black,
            border = BorderStroke(2.dp, Color.Black),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_trait_location),
                contentDescription = saveContentDescription,
                modifier = Modifier.size(54.dp),
            )
        }
    }
}

private fun CapturedLocation.toStoredTraitValue(): String {
    return "${longitude.truncateForTraitLocation(8)}; ${latitude.truncateForTraitLocation(8)}"
}

private fun Double.truncateForTraitLocation(digits: Int): String {
    val raw = toString()
    val dotIndex = raw.indexOf('.')
    if (dotIndex == -1) return raw

    val endIndex = (dotIndex + digits + 1).coerceAtMost(raw.length)
    return raw.substring(0, endIndex)
}
