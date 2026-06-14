package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookEntity
import com.example.ui.MainViewModel
import com.example.ui.components.*

@Composable
fun BooksListScreen(
    viewModel: MainViewModel,
    onNavigateToCreateBook: () -> Unit,
    onNavigateToModelConfig: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToPerformance: () -> Unit,
    onNavigateToTesting: () -> Unit,
    onBookSelected: (String) -> Unit
) {
    val books by viewModel.books.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "InkOS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${books.size} books",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Row {
                IconButton(onClick = onNavigateToModelConfig) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onNavigateToCreateBook) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Book",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Access Cards
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick Actions Section
            item {
                SectionHeader(title = "Quick Actions")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Sessions",
                        icon = Icons.Default.Forum,
                        color = Color(0xFF5856D6),
                        onClick = onNavigateToSessions,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Agent Tools",
                        icon = Icons.Default.Build,
                        color = Color(0xFFFF9500),
                        onClick = onNavigateToTools,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Performance",
                        icon = Icons.Default.Speed,
                        color = Color(0xFF34C759),
                        onClick = onNavigateToPerformance,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Testing",
                        icon = Icons.Default.BugReport,
                        color = Color(0xFFFF2D55),
                        onClick = onNavigateToTesting,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Books Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Books")
            }

            if (books.isEmpty()) {
                item {
                    EmptyBooksCard(onCreateBook = onNavigateToCreateBook)
                }
            } else {
                items(books) { book ->
                    BookCard(
                        book = book,
                        onClick = { onBookSelected(book.id.toString()) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CardContainer(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyBooksCard(onCreateBook: () -> Unit) {
    CardContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = "No Books",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No books yet",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create your first book to get started",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ActionButton(
                text = "Create Book",
                onClick = onCreateBook,
                icon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
        }
    }
}

@Composable
fun BookCard(
    book: BookEntity,
    onClick: () -> Unit
) {
    CardContainer(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.type,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            IOSBadge(
                text = book.status,
                color = when (book.status) {
                    "COMPLETED" -> Color(0xFF34C759)
                    "PLAYING" -> Color(0xFF007AFF)
                    else -> Color(0xFFFF9500)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Chapter ${book.currentChapterIndex}/${book.totalChapters}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Text(
                text = book.genre,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}
