package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.agent.AgentTool
import com.example.ui.MainViewModel
import com.example.ui.components.*

@Composable
fun AgentToolsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val tools by viewModel.agentTools.collectAsState()
    val selectedTool by viewModel.selectedTool.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Agent Tools",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tools.size} tools available",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            IOSBadge("Tools", Color(0xFFFF9500))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tools List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tools) { tool ->
                ToolCard(
                    tool = tool,
                    isSelected = tool.name == selectedTool?.name,
                    onClick = { viewModel.selectTool(tool.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tool Details
        selectedTool?.let { tool ->
            ToolDetailsPanel(
                tool = tool,
                onExecute = { args -> viewModel.executeTool(tool.name, args) }
            )
        }
    }
}

@Composable
fun ToolCard(
    tool: AgentTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    CardContainer(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getToolIcon(tool.name),
                contentDescription = tool.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = tool.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ToolDetailsPanel(
    tool: AgentTool,
    onExecute: (Map<String, Any?>) -> Unit
) {
    var args by remember { mutableStateOf(mapOf<String, Any?>()) }
    var result by remember { mutableStateOf<Any?>(null) }

    CardContainer {
        Text(
            text = "Tool: ${tool.name}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tool.description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Simple argument input
        when (tool.name) {
            "read", "write" -> {
                OutlinedTextField(
                    value = args["path"] as? String ?: "",
                    onValueChange = { args = args + ("path" to it) },
                    label = { Text("Path") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (tool.name == "write") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = args["content"] as? String ?: "",
                        onValueChange = { args = args + ("content" to it) },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
            "grep" -> {
                OutlinedTextField(
                    value = args["pattern"] as? String ?: "",
                    onValueChange = { args = args + ("pattern" to it) },
                    label = { Text("Pattern") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            "ls" -> {
                OutlinedTextField(
                    value = args["path"] as? String ?: ".",
                    onValueChange = { args = args + ("path" to it) },
                    label = { Text("Directory") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ActionButton(
            text = "Execute",
            onClick = {
                result = null
                onExecute(args)
            },
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
        )

        result?.let { res ->
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Result:",
                fontWeight = FontWeight.Bold
            )

            Text(
                text = res.toString(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getToolIcon(toolName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (toolName) {
        "read" -> Icons.Default.Visibility
        "write" -> Icons.Default.Edit
        "grep" -> Icons.Default.Search
        "ls" -> Icons.Default.Folder
        else -> Icons.Default.Build
    }
}
