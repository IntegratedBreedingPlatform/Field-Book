package com.fieldbook.shared.screens.collect.traits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.theme.Button

@Composable
fun CounterTrait(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var count by remember(value) { mutableStateOf(value.toIntOrNull() ?: 0) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { count--; onValueChange(count.toString()) },
            shape = CircleShape,
            modifier = Modifier.size(84.dp)
        ) {
            Text("-1", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(8.dp))
        Button(
            onClick = { count++; onValueChange(count.toString()) },
            shape = CircleShape,
            modifier = Modifier.size(156.dp),
        ) {
            Text("+1", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
