package com.fieldbook.shared.screens.collect.traits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.ic_trait_angle
import com.fieldbook.shared.generated.resources.trait_location_save_content_description
import com.fieldbook.shared.theme.TraitButtonDefaultColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

@Composable
fun AngleTrait(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember { PlatformAngleController() }
    var currentAngle by remember { mutableFloatStateOf(value.toFloatOrNull() ?: 0f) }
    val captureContentDescription = stringResource(Res.string.trait_location_save_content_description)

    DisposableEffect(controller) {
        controller.start { angle ->
            currentAngle = angle
        }
        onDispose {
            controller.stop()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AngleCompass(
            angle = currentAngle,
            modifier = Modifier
                .width(283.dp)
                .height(175.dp)
        )
        Surface(
            onClick = {
                onValueChange(currentAngle.toStoredAngleValue())
            },
            modifier = Modifier
                .size(78.dp),
            shape = CircleShape,
            color = TraitButtonDefaultColor,
            contentColor = Color.Black,
            border = BorderStroke(2.dp, Color.Black),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_trait_angle),
                    contentDescription = captureContentDescription,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun AngleCompass(angle: Float, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val circleThickness = 2.5.dp.toPx()
        val markerThickness = 1.25.dp.toPx()
        val margin = 14.dp.toPx()
        val radius = (size.width / 2f) - margin
        val centerX = size.width / 2f
        val centerY = size.width / 2f

        drawArc(
            color = Color.Black,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = circleThickness)
        )

        for (markerAngle in -90..90 step 45) {
            val adjustedAngle = markerAngle - 90
            val radians = ((adjustedAngle.toDouble() * PI) / 180.0).toFloat()

            val startX = centerX + cos(radians) * (radius - 8.dp.toPx())
            val startY = centerY + sin(radians) * (radius - 8.dp.toPx())
            val endX = centerX + cos(radians) * radius
            val endY = centerY + sin(radians) * radius

            drawLine(
                color = Color.Black,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = markerThickness
            )

            val textX = centerX + cos(radians) * (radius - 18.dp.toPx())
            val textY = centerY + sin(radians) * (radius - 18.dp.toPx())
            val textLayout = textMeasurer.measure(
                text = markerAngle.toString(),
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    textX - textLayout.size.width / 2f,
                    textY - textLayout.size.height / 2f
                )
            )
        }

        rotate(degrees = angle, pivot = Offset(centerX, centerY)) {
            val headLength = radius * 0.68f
            val tailLength = radius * 0.22f
            val arrowWidth = radius * 0.11f

            val headPath = Path().apply {
                moveTo(centerX, centerY - headLength)
                lineTo(centerX - arrowWidth, centerY)
                lineTo(centerX + arrowWidth, centerY)
                close()
            }
            drawPath(headPath, color = Color.Red)

            val tailPath = Path().apply {
                moveTo(centerX, centerY + tailLength)
                lineTo(centerX - arrowWidth, centerY)
                lineTo(centerX + arrowWidth, centerY)
                close()
            }
            drawPath(tailPath, color = Color.Black)
        }
    }
}

private fun Float.toStoredAngleValue(): String {
    val rounded = round(this * 10f) / 10f
    val text = rounded.toString()
    return if (text.contains('.')) {
        val decimals = text.substringAfter('.')
        if (decimals.length >= 1) text else "$text" + "0"
    } else {
        "$text.0"
    }
}
