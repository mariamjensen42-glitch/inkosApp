package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.PlayStateEntity
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

// Custom ios-style 1dp Divider helper to avoid M3 versioning discrepancies
@Composable
fun IOSDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
    )
}

// iOS pill-shaped status indicator
@Composable
fun IOSBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Main application coordinator screen
 */
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeBook by viewModel.activeBook.collectAsState()

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.CREATE_BOOK) {
                IOSBottomNavigationBar(
                    currentScreen = currentScreen,
                    activeBook = activeBook,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.BOOKS_LIST -> BooksListScreen(viewModel)
                Screen.CREATE_BOOK -> CreateBookScreen(viewModel)
                Screen.NOVEL_WORKSPACE -> NovelWorkspaceScreen(viewModel)
                Screen.SHORT_WORKSPACE -> ShortWorkspaceScreen(viewModel)
                Screen.PLAY_WORKSPACE -> PlayWorkspaceScreen(viewModel)
                Screen.MODEL_CONFIG -> ModelConfigScreen(viewModel)
            }
        }
    }
}

/**
 * iOS-styled Segmented bottom navigation bar
 */
@Composable
fun IOSBottomNavigationBar(
    currentScreen: Screen,
    activeBook: BookEntity?,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = mutableListOf(
                NavItem("My Works", Icons.Default.Menu, Screen.BOOKS_LIST)
            )

            // Dynamic entry for currently opened project
            if (activeBook != null) {
                val label = when (activeBook.type) {
                    "NOVEL" -> "Novel Desk"
                    "SHORT" -> "Short Desk"
                    else -> "Play Sandbox"
                }
                val targetScreen = when (activeBook.type) {
                    "NOVEL" -> Screen.NOVEL_WORKSPACE
                    "SHORT" -> Screen.SHORT_WORKSPACE
                    else -> Screen.PLAY_WORKSPACE
                }
                items.add(NavItem(label, Icons.Default.Edit, targetScreen))
            }

            items.add(NavItem("AI Server", Icons.Default.Settings, Screen.MODEL_CONFIG))

            items.forEach { item ->
                val selected = currentScreen == item.screen
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigate(item.screen) }
                        .padding(vertical = 6.dp)
                        .testTag("nav_btn_${item.screen.name}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

data class NavItem(val title: String, val icon: ImageVector, val screen: Screen)

/**
 * 1. Books list representation screen
 */
@Composable
fun BooksListScreen(viewModel: MainViewModel) {
    val books by viewModel.books.collectAsState()
    var selectedTabFilter by remember { mutableStateOf("ALL") } // "ALL", "NOVEL", "SHORT", "PLAY"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cupertino Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "InkOS Portal",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "A story creation AI agent workbench",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            IconButton(
                onClick = { viewModel.navigateTo(Screen.CREATE_BOOK) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("create_book_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New story",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Control filter - iOS Native Guidelines look
        Surface(
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val filters = listOf(
                    "ALL" to "All",
                    "NOVEL" to "Novel",
                    "SHORT" to "Short",
                    "PLAY" to "Play"
                )
                filters.forEach { (key, label) ->
                    val active = selectedTabFilter == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedTabFilter = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Empty state block
        val filteredBooks = if (selectedTabFilter == "ALL") books else books.filter { it.type == selectedTabFilter }
        if (filteredBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No story projects found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the '+' icon on the top right to start crafting!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBooks) { book ->
                    BookCard(book = book, onSelect = { viewModel.selectBook(book) }, onDelete = { viewModel.deleteBook(book) })
                }
            }
        }
    }
}

/**
 * Story list book entity card
 */
@Composable
fun BookCard(book: BookEntity, onSelect: () -> Unit, onDelete: () -> Unit) {
    val gradientColorList = when (book.type) {
        "NOVEL" -> listOf(Color(0xFF007AFF), Color(0xFF00C7BE))
        "SHORT" -> listOf(Color(0xFF5856D6), Color(0xFFAF52DE))
        else -> listOf(Color(0xFFFF9500), Color(0xFFFF2D55))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("book_card_${book.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant colored mini-cover placeholder
            Box(
                modifier = Modifier
                    .size(60.dp, 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(gradientColorList))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (book.type) {
                            "NOVEL" -> Icons.Default.Home
                            "SHORT" -> Icons.Default.Edit
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = book.type,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.genre,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = book.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Brief: " + (book.brief.takeIf { it.isNotEmpty() } ?: "No description provided."),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IOSBadge(
                        text = book.type,
                        color = when (book.type) {
                            "NOVEL" -> Color(0xFF007AFF)
                            "SHORT" -> Color(0xFF5856D6)
                            else -> Color(0xFFFF9500)
                        }
                    )

                    val statusColor = if (book.status == "COMPLETED") Color(0xFF34C759) else Color(0xFFFF2D55)
                    IOSBadge(text = book.status, color = statusColor)

                    if (book.type == "NOVEL") {
                        Text(
                            text = "Ch. ${book.currentChapterIndex - 1} written",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 2. Create story project card entry screen
 */
@Composable
fun CreateBookScreen(viewModel: MainViewModel) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("NOVEL") } // "NOVEL", "SHORT", "PLAY"
    var selectedGenre by remember { mutableStateOf("DETECTIVE") } // XuanHuan, Noir Detective, Urban Suspense
    var brief by remember { mutableStateOf("") }
    val isArchitecting by viewModel.isArchitecting.collectAsState()

    val genres = listOf(
        "DETECTIVE" to "Rain City Detective 🔍",
        "XUANHUAN" to "Dark Xuanhuan Fantasy 🐉",
        "CYBER_ROMANCE" to "Cyber Romance 💖",
        "SPACE_OPERA" to "Space Frontier Opera 🚀"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cupertino top nav
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
            Text(
                text = "Assemble AI Agent",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "STORY TITLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. The Bloodstained Cufflink") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_title"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "CREATIVE METHODOLOGY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf(
                            "NOVEL" to "Long-Novel",
                            "SHORT" to "Short-Outline",
                            "PLAY" to "InkOS Play"
                        )
                        types.forEach { (typeVal, typeLabel) ->
                            val active = selectedType == typeVal
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (active) 1.5.dp else 0.5.dp,
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedType = typeVal }
                                    .testTag("type_card_$typeVal")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = typeLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "STORY SETTING & GENRE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(genres) { (genreKey, label) ->
                                val active = selectedGenre == genreKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                        .border(
                                            width = 0.5.dp,
                                            color = if (active) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedGenre = genreKey }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                        .testTag("genre_btn_$genreKey"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "CREATIVE BRIEF / BRAINSTORM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    OutlinedTextField(
                        value = brief,
                        onValueChange = { brief = it },
                        placeholder = { Text("What represents the heartbeat of your world? Write core hooks, character motivations, secret agendas...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("input_brief"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }

        Button(
            onClick = {
                if (title.isNotEmpty()) {
                    viewModel.createBook(title, selectedType, selectedGenre, brief)
                }
            },
            enabled = title.isNotEmpty() && !isArchitecting,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(50.dp)
                .testTag("btn_build_world"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isArchitecting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Architecting Foundation...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Spawn Agent World", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 3. Novel creation workspaces
 */
@Composable
fun NovelWorkspaceScreen(viewModel: MainViewModel) {
    val book by viewModel.activeBook.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val activeChapter by viewModel.activeChapter.collectAsState()

    // Plan input state
    var userFocusSuggestion by remember { mutableStateOf("") }

    val isPlanning by viewModel.isPlanning.collectAsState()
    val isComposing by viewModel.isComposing.collectAsState()
    val isAuditing by viewModel.isAuditing.collectAsState()
    val isRevising by viewModel.isRevising.collectAsState()

    val currentWritingLine = book?.currentChapterIndex ?: 1

    var activeTabSegment by remember { mutableStateOf("WRITE") } // "WRITE", "INTENT", "AUDIT"

    if (book == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cupertino Work Title Block
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.BOOKS_LIST) }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Portal",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book!!.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Long form serial • Ch. ${activeChapter?.chapterIndex ?: 1} Focus",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            IOSBadge("Novel Agent", Color(0xFF007AFF))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segment selector tabs: Write body, Intent plan, Audit checklist
        Surface(
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(2.dp)) {
                val segments = listOf(
                    "WRITE" to "Prose",
                    "INTENT" to "1. Plan",
                    "AUDIT" to "2. Audit",
                    "BIBLE" to "3. Bible"
                )
                segments.forEach { (segKey, label) ->
                    val active = activeTabSegment == segKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { activeTabSegment = segKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Core Pipeline Action buttons grouped nicely (Cupertino Segmented Flow Container)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "AGENT PIPELINE PANEL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Method 1: Plan next
                    Button(
                        onClick = {
                            viewModel.planNextChapter(userFocusSuggestion)
                            activeTabSegment = "INTENT"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlanning) ColorIOSOrange else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_pipeline_plan"),
                        contentPadding = PaddingValues(0.dp),
                        enabled = !isPlanning && !isComposing && !isAuditing && !isRevising,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isPlanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("1. Plan Ch.$currentWritingLine", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Method 2: Assemble draft
                    Button(
                        onClick = {
                            viewModel.composeChapter()
                            activeTabSegment = "WRITE"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isComposing) ColorIOSIndigo else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_pipeline_compose"),
                        contentPadding = PaddingValues(0.dp),
                        enabled = activeChapter != null && !isPlanning && !isComposing && !isAuditing && !isRevising,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isComposing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("2. Compose", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Method 3: Audit continuous compliance
                    Button(
                        onClick = {
                            viewModel.auditChapter()
                            activeTabSegment = "AUDIT"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_pipeline_audit"),
                        contentPadding = PaddingValues(0.dp),
                        enabled = activeChapter != null && activeChapter!!.content.isNotEmpty() && !isPlanning && !isComposing && !isAuditing && !isRevising,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isAuditing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("3. Audit", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Method 4: Revise & Seal
                    Button(
                        onClick = {
                            viewModel.reviseChapter()
                            activeTabSegment = "WRITE"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRevising) ColorIOSGreen else ColorIOSGreen
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_pipeline_revise"),
                        contentPadding = PaddingValues(0.dp),
                        enabled = activeChapter != null && activeChapter!!.auditLogs.isNotEmpty() && !isPlanning && !isComposing && !isAuditing && !isRevising,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isRevising) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("4. Revise/Approve", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User focus input
                OutlinedTextField(
                    value = userFocusSuggestion,
                    onValueChange = { userFocusSuggestion = it },
                    placeholder = { Text("Directive (e.g. 'Build mystery around the cufflink clue')", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("focus_directive_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Select chapters segment
        Text(
            text = "CHAPTER INDEXES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth().testTag("chapter_selector_list"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chapters) { item ->
                val isActiveComp = activeChapter?.id == item.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActiveComp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        .clickable { viewModel.selectChapter(item) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ch.${item.chapterIndex}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActiveComp) Color.White else MaterialTheme.colorScheme.onBackground
                        )
                        if (item.status == "APPROVED") {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Approved",
                                tint = if (isActiveComp) Color.White else ColorIOSGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content panel based on Active Segment selection
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (activeChapter == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Blueprint Desk Empty",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start the cycle by tapping '1. Plan Ch.$currentWritingLine'! This generates human-readable boundaries for the writer agent.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            when (activeTabSegment) {
                                "WRITE" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Chapter Prose Text (${activeChapter!!.wordCount} words)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        if (activeChapter!!.status == "APPROVED") {
                                            Text(
                                                text = "🔒 SEALED",
                                                color = ColorIOSGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable {
                                                    viewModel.rollbackChapter(activeChapter!!.chapterIndex)
                                                }
                                            )
                                        } else {
                                            Text(
                                                text = "⏳ DRAFT MODE",
                                                color = ColorIOSOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (activeChapter!!.content.isEmpty()) {
                                        Text(
                                            text = "Writing desk empty. Tap '2. Compose' in the pipeline controls above to trigger the writer agent to generate the draft.",
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        Text(
                                            text = activeChapter!!.content,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                                "INTENT" -> {
                                    Text(
                                        text = "Chapter Intent and Focus Boundaries",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorIOSOrange
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = activeChapter!!.intent.ifEmpty { "No intent compiled. Tap '1. Plan' above." },
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                                    )
                                }
                                "AUDIT" -> {
                                    Text(
                                        text = "Continuous 37-Dimension Audit Log",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = activeChapter!!.auditLogs.ifEmpty { "No Audit checks available yet. Click '3. Audit' to run consistency compliance checking." },
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                "BIBLE" -> {
                                    val isArchitecting by viewModel.isArchitecting.collectAsState()
                                    var redesignBrief by remember { mutableStateOf(book!!.brief) }

                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(
                                            text = "Core Story Foundation & Bible Hub",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        // REDESIGN BLUEPRINT CARD
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = "🔄 ARCHITECT DIRECTION MATRIX",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Update the general prompt/brief direction below and ask the Architect sub-agent to fully remodel the narrative framework.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                OutlinedTextField(
                                                    value = redesignBrief,
                                                    onValueChange = { redesignBrief = it },
                                                    placeholder = { Text("Enter updated guidelines (e.g., 'Protagonist discovers a second secret lock, rivals ally')", fontSize = 12.sp) },
                                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = { viewModel.redesignFoundation(redesignBrief) },
                                                    enabled = redesignBrief.isNotEmpty() && !isArchitecting,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                                ) {
                                                    if (isArchitecting) {
                                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = Color.White)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Remodeling Blueprint...", fontSize = 12.sp)
                                                    } else {
                                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Redesign Blueprint with Architect Agent", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }

                                        // DISPLAY BOOK RULES
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "📜 STYLISTIC BOOK RULES",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                            ) {
                                                Text(
                                                    text = book!!.bookRules.ifEmpty { "No rule constraints registered." },
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier.padding(12.dp),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }

                                        // DISPLAY STORY BIBLE
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "📖 NARRATIVE BLUEPRINT DETAILS",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                            ) {
                                                Text(
                                                    text = book!!.storyBible.ifEmpty { "No outline details initialized." },
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Short Story packaging workspace
 */
@Composable
fun ShortWorkspaceScreen(viewModel: MainViewModel) {
    val book by viewModel.activeBook.collectAsState()
    val isGeneratingShort by viewModel.isGeneratingShort.collectAsState()

    var customDirectionPrompt by remember { mutableStateOf("") }
    var slideChapterCount by remember { mutableStateOf(12f) }
    var slideWordCount by remember { mutableStateOf(1000f) }

    if (book == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cupertino top nav bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.BOOKS_LIST) }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Portal", tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = book!!.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "InkOS Short • One-click pipeline compilation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            IOSBadge("Short Agent", Color(0xFF5856D6))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Left parameter sliding screen + Right display box
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f),
                    modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "COMPILATION PARAMETERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), letterSpacing = 1.sp)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "CREATIVE DIRECTION", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = customDirectionPrompt,
                                onValueChange = { customDirectionPrompt = it },
                                placeholder = { Text("e.g. 都市婚姻反转，女主拿到账本证据后反杀", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .testTag("short_direction_input"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                                )
                            )
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Total Chapters", fontSize = 12.sp)
                                Text(text = "${slideChapterCount.toInt()} chapters", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = slideChapterCount,
                                onValueChange = { slideChapterCount = it },
                                valueRange = 1f..15f,
                                modifier = Modifier.testTag("slider_chapters")
                            )
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Word count", fontSize = 12.sp)
                                Text(text = "${slideWordCount.toInt()} chars/chapter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = slideWordCount,
                                onValueChange = { slideWordCount = it },
                                valueRange = 500f..3000f,
                                modifier = Modifier.testTag("slider_words")
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.runShortStory(
                                    direction = customDirectionPrompt.ifEmpty { book!!.brief },
                                    chapterWords = slideWordCount.toInt(),
                                    targetChapters = slideChapterCount.toInt()
                                )
                            },
                            enabled = !isGeneratingShort,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_trigger_short_make")
                        ) {
                            if (isGeneratingShort) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Assembling Script Package...")
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compile Short Package", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "GENERATED COMPILATION REPORT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (book!!.salesPackage.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Short Package empty", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Configure parameters and tap 'Compile' to invoke agent builder.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = ColorIOSGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("COMPILATION FINALIZED SUCCESSFULLY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorIOSGreen)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = book!!.salesPackage,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. InkOS Play Sandboxed Open World UI
 */
@Composable
fun PlayWorkspaceScreen(viewModel: MainViewModel) {
    val book by viewModel.activeBook.collectAsState()
    val state by viewModel.playState.collectAsState()
    val isPlayLoading by viewModel.isPlayLoading.collectAsState()

    var userActionText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (book == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cupertino top area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.BOOKS_LIST) }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = book!!.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "Fantasy/Detective Open World RPG", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            IOSBadge("InkOS Play v2.0", Color(0xFFFF9500))
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (state == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Spawning World State matrix...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            // Persistent HUD layout - iOS specifications
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                modifier = Modifier
                    .fillModifierWithMaxWidth()
                    .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "🔋 ENERGY:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorIOSGreen)
                        Text(text = "88 / 100", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IOSDividerVertical()

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "⏳ WORLD CLOCK:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorIOSOrange)
                        Text(text = state!!.timeState, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IOSDividerVertical()

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "🎒 GEARS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Active", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main layout containing scenery scroll + items inventory list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Scene Description Screen Card
                item {
                    Text(
                        text = "CURRENT SCENE ATMOSPHERE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = state!!.currentScene,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Characters in scene
                item {
                    Text(
                        text = "LOCAL RESIDENT ENTITIES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val charArray = try { JSONArray(state!!.charactersJson) } catch (e: Exception) { JSONArray() }
                    if (charArray.length() == 0) {
                        Text("No characters nearby.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 0 until charArray.length()) {
                                val charObj = charArray.optJSONObject(i) ?: JSONObject()
                                PlayEntityChip(
                                    title = charObj.optString("name", "Unknown NPC"),
                                    subtext = charObj.optString("relation", "Stranger"),
                                    desc = charObj.optString("status", ""),
                                    icon = Icons.Default.Favorite,
                                    badgeColor = ColorIOSBlue
                                )
                            }
                        }
                    }
                }

                // Found Clues and Items
                item {
                    Text(
                        text = "WORLD ITEMS & COLLECTED EVIDENCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val itemsArray = try { JSONArray(state!!.itemsJson) } catch (e: Exception) { JSONArray() }
                    if (itemsArray.length() == 0) {
                        Text("Inventory bag is empty.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 0 until itemsArray.length()) {
                                val itemObj = itemsArray.optJSONObject(i) ?: JSONObject()
                                PlayEntityChip(
                                    title = itemObj.optString("name", "Unknown"),
                                    subtext = itemObj.optString("rating", "★★☆☆☆"),
                                    desc = itemObj.optString("desc", ""),
                                    icon = Icons.Default.Star,
                                    badgeColor = ColorIOSIndigo
                                )
                            }
                        }
                    }
                }

                // Historical move logs
                item {
                    Text(
                        text = "RECENT CONVERSATIONAL PATHWAY LOGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                    ) {
                        val historyArr = try { JSONArray(state!!.historyLogJson) } catch (e: Exception) { JSONArray() }
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (historyArr.length() <= 1) {
                                Text("No logs recorded yet. Initiate commands below to generate logs.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                            } else {
                                // show last 4 logs to keep UI clean
                                val startIndex = maxOf(0, historyArr.length() - 4)
                                for (i in startIndex until historyArr.length()) {
                                    val logObj = historyArr.optJSONObject(i) ?: JSONObject()
                                    val isUser = logObj.optString("role") == "user"
                                    val logText = logObj.optString("text", "")
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = if (isUser) "▶ User: " else "🤖 Agent: ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isUser) MaterialTheme.colorScheme.primary else ColorIOSOrange
                                        )
                                        Text(text = logText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Suggeseted clicking choices/HUD links
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.02f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val choices = if (book!!.genre.contains("DETECTIVE")) {
                        listOf("Inspect nearby clues", "Confront Inspector Gromm", "Enter 'Jade Club'")
                    } else {
                        listOf("Patrol borders", "Rest inside Tavern", "Inspect military maps")
                    }

                    choices.forEach { choice ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                .clickable(enabled = !isPlayLoading) {
                                    userActionText = choice
                                    viewModel.executePlayAction(choice)
                                    userActionText = ""
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = choice,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Natural Action text Input bar (the Action surface)
            OutlinedTextField(
                value = userActionText,
                onValueChange = { userActionText = it },
                placeholder = { Text("What represents your next custom action?", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("play_command_input"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (userActionText.isNotEmpty()) {
                        viewModel.executePlayAction(userActionText)
                        userActionText = ""
                        keyboardController?.hide()
                    }
                }),
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (userActionText.isNotEmpty()) {
                                viewModel.executePlayAction(userActionText)
                                userActionText = ""
                                keyboardController?.hide()
                            }
                        },
                        enabled = userActionText.isNotEmpty() && !isPlayLoading,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (userActionText.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Transparent)
                    ) {
                        if (isPlayLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (userActionText.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )
            )
        }
    }
}

// Entity Chip shown in sandbox
@Composable
fun PlayEntityChip(
    title: String,
    subtext: String,
    desc: String,
    icon: ImageVector,
    badgeColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f),
        modifier = Modifier
            .width(180.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
            Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(1.dp))
                Text(text = subtext, fontSize = 10.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                if (desc.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun IOSDividerVertical() {
    Box(
        modifier = Modifier
            .height(18.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    )
}

/**
 * 6. Model configuration client test page
 */
@Composable
fun ModelConfigScreen(viewModel: MainViewModel) {
    val isApiKeyAvailable = viewModel.isApiKeyAvailable
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val testConnectionResult by viewModel.testConnectionResult.collectAsState()

    val provider by viewModel.selectedProvider.collectAsState()
    val deepSeekKey by viewModel.deepSeekKey.collectAsState()
    val deepSeekBaseUrl by viewModel.deepSeekBaseUrl.collectAsState()
    val deepSeekModel by viewModel.deepSeekModel.collectAsState()
    val xiaomiMimoKey by viewModel.xiaomiMimoKey.collectAsState()
    val xiaomiMimoBaseUrl by viewModel.xiaomiMimoBaseUrl.collectAsState()
    val xiaomiMimoModel by viewModel.xiaomiMimoModel.collectAsState()
    val temperatureValue by viewModel.currentTemperature.collectAsState()

    var showKeyInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Cupertino Title Header
        Text(
            text = "AI Server Control",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Inkos Multi-Service router & DeepSeek protocol configurations",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Service Router Segment Panel
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { viewModel.updateProvider("GEMINI") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (provider == "GEMINI") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (provider == "GEMINI") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("Gemini", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
                Button(
                    onClick = { viewModel.updateProvider("DEEPSEEK") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (provider == "DEEPSEEK") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (provider == "DEEPSEEK") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("DeepSeek", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
                Button(
                    onClick = { viewModel.updateProvider("XIAOMI_MIMO") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (provider == "XIAOMI_MIMO") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (provider == "XIAOMI_MIMO") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("小米 MiMo", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (provider == "GEMINI") {
            // Google Gemini config form
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "GOOGLE GEMINI PROTOCOL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Keys Config Status", fontSize = 14.sp)
                        IOSBadge(
                            text = if (isApiKeyAvailable) "ACTIVE KEY" else "SIM CONSOLE FALLBACK",
                            color = if (isApiKeyAvailable) ColorIOSGreen else ColorIOSOrange
                        )
                    }

                    IOSDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Target Model ID", fontSize = 14.sp)
                        Text(text = "gemini-3.5-flash", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    IOSDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Connection Protocol", fontSize = 14.sp)
                        Text(text = "Direct REST + Retrofit", fontSize = 13.sp)
                    }
                }
            }
        } else if (provider == "DEEPSEEK") {
            // DeepSeek config form
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // API Credentials Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "DEEPSEEK API ENDPOINT CONFIG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        // Key status badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Local Keys Status", fontSize = 14.sp)
                            IOSBadge(
                                text = if (isApiKeyAvailable) "DEEPSEEK ACTIVE" else "SIM CONSOLE FALLBACK",
                                color = if (isApiKeyAvailable) ColorIOSGreen else ColorIOSOrange
                            )
                        }

                        IOSDivider()

                        // API key textfield
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "DeepSeek API Key Address", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = deepSeekKey,
                                onValueChange = { viewModel.updateDeepSeekKey(it) },
                                placeholder = { Text(text = "sk-...", fontSize = 13.sp) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showKeyInput) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showKeyInput = !showKeyInput }) {
                                        Icon(
                                            imageVector = if (showKeyInput) Icons.Default.Lock else Icons.Default.Search,
                                            contentDescription = "Toggle password visibility",
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        IOSDivider()

                        // Base URL textfield
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "API Base URL Protocol", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = deepSeekBaseUrl,
                                onValueChange = { viewModel.updateDeepSeekBaseUrl(it) },
                                placeholder = { Text(text = "https://api.deepseek.com", fontSize = 13.sp) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Choose Models Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "SELECT ACTIVE DEEPSEEK MODEL V4/PRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        val modelOptions = listOf(
                            "deepseek-v4-flash" to "Speed & Creative (Official Flash Router)",
                            "deepseek-v4-pro" to "Elite Prose & Continuity (Official Pro Router)",
                            "deepseek-chat" to "Default Compatibility Alias (Symmetric)",
                            "deepseek-reasoner" to "Cognitive Reasoning Mode (Chain-of-thought)"
                        )

                        modelOptions.forEachIndexed { index, pair ->
                            val isSelected = deepSeekModel == pair.first
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateDeepSeekModel(pair.first) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pair.first,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = pair.second,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updateDeepSeekModel(pair.first) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                // Temperature Slider Screen
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TEMPORAL CREATIVITY (HUMANITY)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            val displayTemp = ((temperatureValue * 10).toInt() / 10.0).toString()
                            Text(
                                text = displayTemp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }

                        Slider(
                            value = temperatureValue,
                            onValueChange = { viewModel.updateTemperature(it) },
                            valueRange = 0.0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Text(
                            text = if (temperatureValue > 1.3f) "🔥 Creative Writing Mode Recommended (Values 1.3-1.6 promote organic, lush vocabulary and descriptive depth)." else "❄️ Logical / Highly Cohesive focus mode.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            // Xiaomi MiMo config form
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // API Credentials Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "XIAOMI MIMO API ENDPOINT CONFIG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        // Key status badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Local Keys Status", fontSize = 14.sp)
                            IOSBadge(
                                text = if (isApiKeyAvailable) "MIMO ACTIVE" else "SIM CONSOLE FALLBACK",
                                color = if (isApiKeyAvailable) ColorIOSGreen else ColorIOSOrange
                            )
                        }

                        IOSDivider()

                        // API key textfield
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Xiaomi MiMo API Key Address", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = xiaomiMimoKey,
                                onValueChange = { viewModel.updateXiaomiMimoKey(it) },
                                placeholder = { Text(text = "sk-...", fontSize = 13.sp) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showKeyInput) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showKeyInput = !showKeyInput }) {
                                        Icon(
                                            imageVector = if (showKeyInput) Icons.Default.Lock else Icons.Default.Search,
                                            contentDescription = "Toggle password visibility",
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        IOSDivider()

                        // Base URL textfield
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "API Base URL Protocol", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = xiaomiMimoBaseUrl,
                                onValueChange = { viewModel.updateXiaomiMimoBaseUrl(it) },
                                placeholder = { Text(text = "https://api-ai.xiaomi.com/v1", fontSize = 13.sp) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Choose Models Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "SELECT ACTIVE XIAOMI MIMO MODEL V2",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        val modelOptions = listOf(
                            "mimo-v2-pro" to "Elite Prose & 1M context (Official Pro Model)",
                            "mimo-v2-omni" to "Multi-Modal Reasoning & High Quality (Official Omni Model)",
                            "mimo-v2-flash" to "Speed & Efficient Story Architect (Official Flash Model)"
                        )

                        modelOptions.forEachIndexed { index, pair ->
                            val isSelected = xiaomiMimoModel == pair.first
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateXiaomiMimoModel(pair.first) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pair.first,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = pair.second,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updateXiaomiMimoModel(pair.first) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                // Temperature Slider Screen
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TEMPORAL CREATIVITY (HUMANITY)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            val displayTemp = ((temperatureValue * 10).toInt() / 10.0).toString()
                            Text(
                                text = displayTemp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }

                        Slider(
                            value = temperatureValue,
                            onValueChange = { viewModel.updateTemperature(it) },
                            valueRange = 0.0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Text(
                            text = if (temperatureValue > 1.3f) "🔥 Creative Writing Mode Recommended (Values 1.3-1.6 promote organic, lush vocabulary and descriptive depth)." else "❄️ Logical / Highly Cohesive focus mode.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Connection test execution box
        Button(
            onClick = { viewModel.testConnection() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_test_connection"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isTestingConnection) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing API Connection Gateway...")
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test API Server Connection", fontWeight = FontWeight.Bold)
            }
        }

        if (testConnectionResult != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    text = testConnectionResult!!,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // iOS style user hint/explanation banner
        Card(
            colors = CardDefaults.cardColors(containerColor = ColorIOSBlue.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ColorIOSBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Intelligence Protocol Guidelines", fontWeight = FontWeight.Bold, color = ColorIOSBlue, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "By configuring custom remote model services, InkOS routes all content composition, planning outline structures, continuous reviews, and interactive Open World agents directly to the selected active endpoint. Local Simulation remains as instant safe standby fallback.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// Inline extension helpers to satisfy clean compilation standard
fun Modifier.fillModifierWithMaxWidth(): Modifier = this.fillMaxWidth()
