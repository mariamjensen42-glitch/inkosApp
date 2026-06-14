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
fun ShortWorkspaceScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val activeBook by viewModel.activeBook.collectAsState()

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
                    text = "Short Fiction",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            IOSBadge("Short", Color(0xFF5856D6))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Area
        CardContainer {
            SectionHeader(title = "Content")
            
            Text(
                text = "Short fiction workspace",
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
                text = "Generate",
                onClick = { viewModel.generateShortFiction() },
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
            )
            OutlinedActionButton(
                text = "Export",
                onClick = { viewModel.exportBook() },
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Share, contentDescription = null) }
            )
        }
    }
}
