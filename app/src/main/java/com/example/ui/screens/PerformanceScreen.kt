package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.performance.MemoryStatus
import com.example.ui.MainViewModel
import com.example.ui.components.*

@Composable
fun PerformanceScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val memoryStatus by viewModel.memoryStatus.collectAsState()
    val cacheStats by viewModel.cacheStats.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val queuedTasks by viewModel.queuedTasks.collectAsState()

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
                    Text("Queued: $queuedTasks")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                text = "Clear Caches",
                onClick = { viewModel.clearCaches() },
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
            OutlinedActionButton(
                text = "GC",
                onClick = { viewModel.forceGarbageCollection() },
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) }
            )
        }
    }
}

@Composable
fun PerformanceCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    CardContainer {
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

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        bytes >= 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))} MB"
        bytes >= 1024 -> "${String.format("%.1f", bytes / 1024.0)} KB"
        else -> "$bytes B"
    }
}
