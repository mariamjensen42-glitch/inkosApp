package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.*

@Composable
fun CreateBookScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var blurb by remember { mutableStateOf("") }
    var targetChapters by remember { mutableStateOf("12") }
    var chapterWordCount by remember { mutableStateOf("1000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
                    text = "Create Book",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Set up your new book",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            IOSBadge("New", MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Book Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Genre
        OutlinedTextField(
            value = genre,
            onValueChange = { genre = it },
            label = { Text("Genre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Platform
        OutlinedTextField(
            value = platform,
            onValueChange = { platform = it },
            label = { Text("Platform") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Blurb
        OutlinedTextField(
            value = blurb,
            onValueChange = { blurb = it },
            label = { Text("Book Blurb") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Target Chapters and Word Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = targetChapters,
                onValueChange = { targetChapters = it },
                label = { Text("Target Chapters") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = chapterWordCount,
                onValueChange = { chapterWordCount = it },
                label = { Text("Words/Chapter") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Create Button
        ActionButton(
            text = "Create Book",
            onClick = {
                viewModel.createBook(
                    title = title,
                    genre = genre,
                    platform = platform,
                    blurb = blurb,
                    targetChapters = targetChapters.toIntOrNull() ?: 12,
                    chapterWordCount = chapterWordCount.toIntOrNull() ?: 1000
                )
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank(),
            icon = { Icon(Icons.Default.Add, contentDescription = null) }
        )
    }
}
