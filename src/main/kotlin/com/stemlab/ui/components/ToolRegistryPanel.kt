package com.stemlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemlab.core.model.ToolSpec
import com.stemlab.ui.theme.*

@Composable
fun ToolRegistryPanel(tools: List<ToolSpec>, modifier: Modifier = Modifier) {
    PanelCard(title = "Selected Tools", modifier = modifier) {
        if (tools.isEmpty()) {
            Text("Tools appear here after evolution.", color = OnSurfaceDim, fontSize = 13.sp)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tools) { tool -> ToolChip(tool) }
            }
        }
    }
}

@Composable
private fun ToolChip(tool: ToolSpec) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(SuccessGreen)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(tool.name, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "~${tool.estimatedTokensPerCall} tokens · \$%.4f/call".format(tool.estimatedCostPerCall),
                color = OnSurfaceDim,
                fontSize = 11.sp
            )
        }
    }
}
