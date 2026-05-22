package com.fieldbook.shared.traits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.ic_transfer_cancelled
import com.fieldbook.shared.generated.resources.ic_transfer_error
import org.jetbrains.compose.resources.painterResource

@Composable
fun TraitEditorTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    )
}

@Composable
fun TraitEditorUnderline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF93C45A))
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun TraitEditorTextField(
    title: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    clearable: Boolean = false,
    isRequired: Boolean = false,
    numeric: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp)
    ) {
        TraitEditorTitle(title)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = {
                    if (!numeric || it.isBlank() || it.toDoubleOrNull() != null) {
                        onValueChange(it)
                    }
                },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (clearable) {
                if (value.isNotBlank()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_transfer_cancelled),
                            contentDescription = "Clear text",
                            tint = Color.Gray
                        )
                    }
                } else if (isRequired) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_transfer_error),
                        contentDescription = "Required field",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        TraitEditorUnderline()
    }
}
