package com.example.ui.screens

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
import com.example.data.testing.TestResult
import com.example.data.testing.TestSuite
import com.example.ui.MainViewModel
import com.example.ui.components.*

@Composable
fun TestingScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
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
            IconButton(onClick = onNavigateBack) {
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
        ActionButton(
            text = if (isRunningTests) "Running Tests..." else "Run All Tests",
            onClick = { viewModel.runTests() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunningTests,
            icon = {
                if (isRunningTests) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
            }
        )
    }
}

@Composable
fun TestSummaryCard(testSuite: TestSuite) {
    CardContainer {
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
            StatItem(
                label = "Total",
                value = testSuite.totalTests.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                label = "Passed",
                value = testSuite.passedTests.toString(),
                color = Color(0xFF34C759)
            )
            StatItem(
                label = "Failed",
                value = testSuite.failedTests.toString(),
                color = MaterialTheme.colorScheme.error
            )
            StatItem(
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

@Composable
fun TestResultCard(result: TestResult) {
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
