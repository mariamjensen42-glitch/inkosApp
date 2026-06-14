package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.interaction.*
import com.example.data.agent.*
import com.example.data.performance.*
import com.example.data.testing.*

/**
 * Session Management Screen
 */
@Composable
fun SessionManagementScreen(viewModel: MainViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

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
            IconButton(onClick = { viewModel.navigateTo(Screen.BOOKS_LIST) }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Session Management",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sessions.size} sessions",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            IOSBadge("Sessions", Color(0xFF5856D6))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions) { session ->
                SessionCard(
                    session = session,
                    isActive = session.sessionId == activeSession?.sessionId,
                    onClick = { viewModel.selectSession(session.sessionId) },
                    onDelete = { viewModel.deleteSession(session.sessionId) }
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.createSession() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Session")
            }

            OutlinedButton(
                onClick = { viewModel.refreshSessions() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Refresh")
            }
        }
    }
}

@Composable
fun SessionCard(
    session: BookSession,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title ?: "Session ${session.sessionId.take(8)}",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Book: ${session.bookId ?: "None"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IOSBadge(
                    text = session.sessionKind?.name ?: "CHAT",
                    color = Color(0xFF34C759)
                )

                Text(
                    text = "${session.messages.size} messages",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Agent Tools Screen
 */
@Composable
fun AgentToolsScreen(viewModel: MainViewModel) {
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
            IconButton(onClick = { viewModel.navigateTo(Screen.BOOKS_LIST) }) {
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

        // Tools Grid
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tools.chunked(2)) { toolPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    toolPair.forEach { tool ->
                        ToolCard(
                            tool = tool,
                            isSelected = tool.name == selectedTool?.name,
                            onClick = { viewModel.selectTool(tool.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (toolPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = getToolIcon(tool.name),
                contentDescription = tool.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tool.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Text(
                text = tool.description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "ls" -> {
                    OutlinedTextField(
                        value = args["path"] as? String ?: ".",
                        onValueChange = { args = args + ("path" to it) },
                        label = { Text("Directory") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    result = null
                    onExecute(args)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Execute")
            }

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
}

/**
 * Performance Monitoring Screen
 */
@Composable
fun PerformanceScreen(viewModel: MainViewModel) {
    val memoryStatus by viewModel.memoryStatus.collectAsState()
    val cacheStats by viewModel.cacheStats.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()

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
            IconButton(onClick = { viewModel.navigateTo(Screen.BOOKS_LIST) }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Performance Monitor",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "System metrics",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            IOSBadge("Performance", Color(0xFF34C759))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Memory Status
        PerformanceCard(
            title = "Memory Usage",
            icon = Icons.Default.Storage,
            content = {
                Column {
                    Text(
                        text = "Used: ${formatBytes(memoryStatus.usedMemory)} / ${formatBytes(memoryStatus.maxMemory)}",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { memoryStatus.usagePercentage.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            memoryStatus.isCritical -> MaterialTheme.colorScheme.error
                            memoryStatus.isWarning -> Color(0xFFFF9500)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%.1f%%", memoryStatus.usagePercentage * 100)} used",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Cache Stats
        PerformanceCard(
            title = "Cache Statistics",
            icon = Icons.Default.Cached,
            content = {
                Column {
                    Text("Cache entries: ${cacheStats.size}")
                    Text("Hit rate: ${String.format("%.1f%%", cacheStats.hitRate * 100)}")
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Active Tasks
        PerformanceCard(
            title = "Background Tasks",
            icon = Icons.Default.Assignment,
            content = {
                Column {
                    Text("Active: $activeTasks")
                    Text("Queued: ${viewModel.queuedTasks.collectAsState().value}")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.clearCaches() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Caches")
            }

            OutlinedButton(
                onClick = { viewModel.forceGarbageCollection() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("GC")
            }
        }
    }
}

@Composable
fun PerformanceCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

/**
 * Testing Screen
 */
@Composable
fun TestingScreen(viewModel: MainViewModel) {
    val testResults by viewModel.testResults.collectAsState()
    val isRunningTests by viewModel.isRunningTests.collectAsState()

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
            IconButton(onClick = { viewModel.navigateTo(Screen.BOOKS_LIST) }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Testing & Validation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${testResults.totalTests} tests",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            IOSBadge("Tests", Color(0xFFFF2D55))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test Summary
        TestSummaryCard(testSuite = testResults)

        Spacer(modifier = Modifier.height(16.dp))

        // Test Results List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(testResults.results) { result ->
                TestResultCard(result = result)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Run Tests Button
        Button(
            onClick = { viewModel.runTests() },
            enabled = !isRunningTests,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRunningTests) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Running Tests...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run All Tests")
            }
        }
    }
}

@Composable
fun TestSummaryCard(testSuite: TestSuite) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = testSuite.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TestStatItem(
                    label = "Total",
                    value = testSuite.totalTests.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                TestStatItem(
                    label = "Passed",
                    value = testSuite.passedTests.toString(),
                    color = Color(0xFF34C759)
                )
                TestStatItem(
                    label = "Failed",
                    value = testSuite.failedTests.toString(),
                    color = MaterialTheme.colorScheme.error
                )
                TestStatItem(
                    label = "Pass Rate",
                    value = "${String.format("%.1f%%", testSuite.passRate * 100)}",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Duration: ${testSuite.totalDuration}ms",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TestStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun TestResultCard(result: TestResult) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (result.passed) {
            Color(0xFF34C759).copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (result.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = if (result.passed) "Passed" else "Failed",
                tint = if (result.passed) Color(0xFF34C759) else MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.testName,
                    fontWeight = FontWeight.Bold
                )
                result.error?.let { error ->
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "${result.duration}ms",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// Helper functions

private fun getToolIcon(toolName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (toolName) {
        "read" -> Icons.Default.Visibility
        "write" -> Icons.Default.Edit
        "grep" -> Icons.Default.Search
        "ls" -> Icons.Default.Folder
        else -> Icons.Default.Build
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        bytes >= 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))} MB"
        bytes >= 1024 -> "${String.format("%.1f", bytes / 1024.0)} KB"
        else -> "$bytes B"
    }
}

// Data classes for UI state

data class CacheStats(
    val size: Int = 0,
    val hitRate: Double = 0.0
)
