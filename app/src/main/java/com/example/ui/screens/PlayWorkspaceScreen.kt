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
import com.example.ui.MainViewModel
import com.example.ui.components.*

@Composable
fun PlayWorkspaceScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val activeBook by viewModel.activeBook.collectAsState()
    val playState by viewModel.playState.collectAsState()

    if (activeBook == null) {
        onNavigateBack()
        return
    }

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
                    text = activeBook!!.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Interactive Play",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            IOSBadge("Play", Color(0xFF34C759))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Play State
        CardContainer {
            SectionHeader(title = "Game State")
            
            playState?.let { state ->
                Text(
                    text = "Time: ${state.timeState}",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scene: ${state.currentScene.take(100)}...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            } ?: Text(
                text = "No active play state",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                text = "Start Play",
                onClick = { viewModel.startPlay() },
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
            )
            OutlinedActionButton(
                text = "Reset",
                onClick = { viewModel.resetPlay() },
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) }
            )
        }
    }
}
