package com.stemlab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemlab.ui.theme.*

@Composable
fun SettingsPanel(
    domain: String,
    isRunning: Boolean,
    onDomainApply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(domain) { mutableStateOf(domain) }
    val changed = draft.trim() != domain.trim() && draft.isNotBlank()

    PanelCard(title = "Domain", modifier = modifier) {
        OutlinedTextField(
            value = draft,
            onValueChange = { if (!isRunning) draft = it },
            enabled = !isRunning,
            singleLine = true,
            placeholder = { Text("e.g. Python QA, SQL Optimizer", color = OnSurfaceDim, fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (changed) AccentCyan else PrimaryGreen,
                unfocusedBorderColor = SurfaceVariant,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                disabledTextColor = OnSurfaceDim,
                disabledBorderColor = SurfaceVariant,
                cursorColor = AccentCyan
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onDomainApply(draft.trim()) },
            enabled = changed && !isRunning,
            modifier = Modifier.fillMaxWidth().height(34.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan,
                contentColor = androidx.compose.ui.graphics.Color.Black,
                disabledContainerColor = SurfaceVariant,
                disabledContentColor = OnSurfaceDim
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Apply", fontSize = 12.sp)
        }
    }
}
