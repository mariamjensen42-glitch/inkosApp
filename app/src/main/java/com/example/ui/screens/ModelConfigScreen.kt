package com.example.ui.screens

import androidx.compose.foundation.layout.*
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
fun ModelConfigScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val deepSeekKey by viewModel.deepSeekKey.collectAsState()
    val deepSeekBaseUrl by viewModel.deepSeekBaseUrl.collectAsState()
    val deepSeekModel by viewModel.deepSeekModel.collectAsState()
    val xiaomiMimoKey by viewModel.xiaomiMimoKey.collectAsState()
    val xiaomiMimoBaseUrl by viewModel.xiaomiMimoBaseUrl.collectAsState()
    val xiaomiMimoModel by viewModel.xiaomiMimoModel.collectAsState()
    val currentTemperature by viewModel.currentTemperature.collectAsState()

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
                    text = "Model Configuration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure LLM providers",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            IOSBadge("Config", MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Provider Selection
        CardContainer {
            SectionHeader(title = "Provider")
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProviderChip(
                    name = "Gemini",
                    isSelected = selectedProvider == "GEMINI",
                    onClick = { viewModel.updateProvider("GEMINI") }
                )
                ProviderChip(
                    name = "DeepSeek",
                    isSelected = selectedProvider == "DEEPSEEK",
                    onClick = { viewModel.updateProvider("DEEPSEEK") }
                )
                ProviderChip(
                    name = "MiMo",
                    isSelected = selectedProvider == "XIAOMI_MIMO",
                    onClick = { viewModel.updateProvider("XIAOMI_MIMO") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Provider-specific settings
        when (selectedProvider) {
            "DEEPSEEK" -> {
                CardContainer {
                    SectionHeader(title = "DeepSeek Settings")
                    
                    OutlinedTextField(
                        value = deepSeekKey,
                        onValueChange = { viewModel.updateDeepSeekKey(it) },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = deepSeekBaseUrl,
                        onValueChange = { viewModel.updateDeepSeekBaseUrl(it) },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = deepSeekModel,
                        onValueChange = { viewModel.updateDeepSeekModel(it) },
                        label = { Text("Model") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            "XIAOMI_MIMO" -> {
                CardContainer {
                    SectionHeader(title = "Xiaomi MiMo Settings")
                    
                    OutlinedTextField(
                        value = xiaomiMimoKey,
                        onValueChange = { viewModel.updateXiaomiMimoKey(it) },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = xiaomiMimoBaseUrl,
                        onValueChange = { viewModel.updateXiaomiMimoBaseUrl(it) },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = xiaomiMimoModel,
                        onValueChange = { viewModel.updateXiaomiMimoModel(it) },
                        label = { Text("Model") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Temperature
        CardContainer {
            SectionHeader(title = "Temperature")
            
            Slider(
                value = currentTemperature,
                onValueChange = { viewModel.updateTemperature(it) },
                valueRange = 0f..2f,
                steps = 20
            )
            
            Text(
                text = String.format("%.2f", currentTemperature),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        ActionButton(
            text = "Save Configuration",
            onClick = { viewModel.saveConfig() },
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(Icons.Default.Save, contentDescription = null) }
        )
    }
}

@Composable
fun ProviderChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(name) },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null
    )
}
