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
fun NovelWorkspaceScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val activeBook by viewModel.activeBook.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val activeChapter by viewModel.activeChapter.collectAsState()

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
                    text = "Chapter ${activeChapter?.chapterIndex ?: 1}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            IOSBadge("Novel", Color(0xFF007AFF))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chapter List
        CardContainer {
            SectionHeader(title = "Chapters")
            
            chapters.forEach { chapter ->
                ChapterItem(
                    chapter = chapter,
                    isActive = chapter.id == activeChapter?.id,
                    onClick = { viewModel.selectChapter(chapter.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                text = "Write Next",
                onClick = { viewModel.writeNextChapter() },
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            OutlinedActionButton(
                text = "Revise",
                onClick = { viewModel.reviseChapter() },
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) }
            )
        }
    }
}

@Composable
fun ChapterItem(
    chapter: com.example.data.local.ChapterEntity,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.Book else Icons.Default.Description,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chapter ${chapter.chapterIndex}",
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = chapter.title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        IOSBadge(
            text = chapter.status,
            color = if (chapter.status == "APPROVED") Color(0xFF34C759) else Color(0xFFFF9500)
        )
    }
}
