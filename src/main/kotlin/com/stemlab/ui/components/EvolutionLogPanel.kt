package com.stemlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemlab.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EvolutionLogPanel(logs: List<String>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(logs.size - 1) }
        }
    }

    PanelCard(title = "Evolution Log", modifier = modifier) {
        if (logs.isEmpty()) {
            Text("No log entries yet.", color = OnSurfaceDim, fontSize = 13.sp)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceDark)
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize().padding(end = 10.dp)
                ) {
                    items(logs) { line ->
                        val color = when {
                            line.contains("Selected") || line.contains("complete") -> SuccessGreen
                            line.contains("Rejected") -> ErrorRed
                            line.contains("score") || line.contains("Score") -> AccentCyan
                            line.contains("ERROR") -> ErrorRed
                            else -> OnSurfaceDim
                        }
                        Text(
                            text = line,
                            color = color,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }
    }
}
