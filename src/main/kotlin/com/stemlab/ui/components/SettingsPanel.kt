package com.stemlab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemlab.ui.theme.*

@Composable
fun SettingsPanel(
    projectName: String,
    domain: String,
    description: String,
    isRunning: Boolean,
    onApply: (name: String, domain: String, description: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameDraft by remember(projectName) { mutableStateOf(projectName) }
    var domainDraft by remember(domain) { mutableStateOf(domain) }
    var descriptionDraft by remember(description) { mutableStateOf(description) }
    val focusManager = LocalFocusManager.current
    val changed =
        nameDraft.trim() != projectName.trim() ||
            domainDraft.trim() != domain.trim() ||
            descriptionDraft.trim() != description.trim()
    val canApply = changed && domainDraft.isNotBlank() && !isRunning

    fun applyChanges() {
        if (!canApply) {
            focusManager.clearFocus(force = true)
            return
        }
        onApply(nameDraft.trim(), domainDraft.trim(), descriptionDraft.trim())
        focusManager.clearFocus(force = true)
    }

    PanelCard(title = "Project Details", modifier = modifier) {
        DetailField(
            value = nameDraft,
            onValueChange = { if (!isRunning) nameDraft = it },
            label = "Project name",
            placeholder = "e.g. SQL Review",
            enabled = !isRunning,
            imeAction = ImeAction.Next,
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        )
        Spacer(Modifier.height(8.dp))
        DetailField(
            value = domainDraft,
            onValueChange = { if (!isRunning) domainDraft = it },
            label = "Domain",
            placeholder = "e.g. Python QA, SQL Optimizer",
            enabled = !isRunning,
            imeAction = ImeAction.Next,
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        )
        Spacer(Modifier.height(8.dp))
        DetailField(
            value = descriptionDraft,
            onValueChange = { if (!isRunning) descriptionDraft = it },
            label = "Description",
            placeholder = "Optional notes",
            enabled = !isRunning,
            imeAction = ImeAction.Done,
            onDone = ::applyChanges
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = ::applyChanges,
            enabled = canApply,
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

@Composable
private fun DetailField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    imeAction: ImeAction,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = { Text(label, color = OnSurfaceDim, fontSize = 11.sp) },
        placeholder = { Text(placeholder, color = OnSurfaceDim, fontSize = 12.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCyan,
            unfocusedBorderColor = SurfaceVariant,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            disabledTextColor = OnSurfaceDim,
            disabledBorderColor = SurfaceVariant,
            cursorColor = AccentCyan
        ),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onNext?.invoke() },
            onDone = { onDone?.invoke() }
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        modifier = Modifier.fillMaxWidth()
    )
}
