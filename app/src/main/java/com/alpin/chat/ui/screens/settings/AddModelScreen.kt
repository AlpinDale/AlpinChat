package com.alpin.chat.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alpin.chat.R
import com.alpin.chat.domain.model.ModelConfig
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddModelScreen(
    existingModel: ModelConfig?,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var displayName by remember { mutableStateOf(existingModel?.displayName ?: "") }
    var baseUrl by remember { mutableStateOf(existingModel?.baseUrl ?: "http://") }
    var modelName by remember { mutableStateOf(existingModel?.modelName ?: "") }
    var apiKey by remember { mutableStateOf(existingModel?.apiKey ?: "") }
    var contextLength by remember { mutableStateOf((existingModel?.contextLength ?: 32768).toString()) }
    var supportsVision by remember { mutableStateOf(existingModel?.supportsVision ?: false) }
    var supportsThinking by remember { mutableStateOf(existingModel?.supportsThinking ?: false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val isEditing = existingModel != null
    val isValid = displayName.isNotBlank() && baseUrl.isNotBlank() && modelName.isNotBlank()
    val canFetchModels = baseUrl.isNotBlank() && baseUrl != "http://" && baseUrl != "https://"

    // Auto-select first model when models are fetched
    LaunchedEffect(uiState.availableModels) {
        if (uiState.availableModels.isNotEmpty() && modelName.isBlank()) {
            modelName = uiState.availableModels.first()
        }
    }

    LaunchedEffect(uiState.connectionTestResult) {
        if (uiState.connectionTestResult is ConnectionTestResult.Success) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearConnectionTestResult()
        }
    }

    // Clear available models when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearAvailableModels()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = if (isEditing) stringResource(R.string.edit_model)
                    else stringResource(R.string.add_model),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Display Name
                StyledTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = stringResource(R.string.display_name),
                    placeholder = "My Local Model"
                )

                // Base URL
                StyledTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        viewModel.clearAvailableModels()
                    },
                    label = stringResource(R.string.base_url),
                    placeholder = "http://192.168.1.100:8080/v1",
                    supportingText = "Include /v1 for OpenAI-compatible endpoints"
                )

                // API Key
                StyledTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = stringResource(R.string.api_key),
                    placeholder = "sk-...",
                    isPassword = true,
                    supportingText = "Leave empty if not required"
                )

                // Model selection with fetch button
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.model_name),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1A1A1A))
                                    .clickable(
                                        enabled = uiState.availableModels.isNotEmpty(),
                                        onClick = { modelDropdownExpanded = true },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = modelName,
                                        onValueChange = { modelName = it },
                                        modifier = Modifier.weight(1f),
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 15.sp
                                        ),
                                        cursorBrush = SolidColor(Color.White),
                                        singleLine = true,
                                        decorationBox = { innerTextField ->
                                            Box {
                                                if (modelName.isEmpty()) {
                                                    Text(
                                                        text = "Select or enter model",
                                                        color = Color.White.copy(alpha = 0.4f),
                                                        fontSize = 15.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                    if (uiState.availableModels.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select model",
                                            tint = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = modelDropdownExpanded,
                                onDismissRequest = { modelDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF2A2A2A))
                            ) {
                                uiState.availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = model,
                                                    color = Color.White
                                                )
                                                if (model == modelName) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = Color.White
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            modelName = model
                                            modelDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Fetch button
                        Box(
                            modifier = Modifier
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (canFetchModels && !uiState.isFetchingModels)
                                        Color(0xFF2A2A2A)
                                    else
                                        Color(0xFF1A1A1A)
                                )
                                .clickable(
                                    enabled = canFetchModels && !uiState.isFetchingModels,
                                    onClick = {
                                        viewModel.fetchAvailableModels(
                                            baseUrl.trimEnd('/'),
                                            apiKey.ifBlank { null })
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (uiState.isFetchingModels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Fetch models",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (canFetchModels) Color.White.copy(alpha = 0.7f) else Color.White.copy(
                                            alpha = 0.3f
                                        )
                                    )
                                }
                                Text(
                                    text = "Fetch",
                                    color = if (canFetchModels) Color.White.copy(alpha = 0.7f) else Color.White.copy(
                                        alpha = 0.3f
                                    ),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Show fetch error or success info
                    if (uiState.fetchModelsError != null) {
                        Text(
                            text = uiState.fetchModelsError!!,
                            fontSize = 12.sp,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (uiState.availableModels.isNotEmpty()) {
                        Text(
                            text = "${uiState.availableModels.size} model(s) available",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Context Length
                StyledTextField(
                    value = contextLength,
                    onValueChange = { newValue ->
                        // Only allow digits
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            contextLength = newValue
                        }
                    },
                    label = "Context Length",
                    placeholder = "32768",
                    supportingText = "Maximum tokens for conversation history"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Supports Vision toggle
                ToggleCard(
                    title = stringResource(R.string.supports_vision),
                    description = "Enable image attachment support",
                    checked = supportsVision,
                    onCheckedChange = { supportsVision = it }
                )

                // Supports Thinking toggle
                ToggleCard(
                    title = stringResource(R.string.supports_thinking),
                    description = stringResource(R.string.supports_thinking_description),
                    checked = supportsThinking,
                    onCheckedChange = { supportsThinking = it }
                )

                // Connection test result
                AnimatedVisibility(
                    visible = uiState.connectionTestResult != null,
                    enter = fadeIn(animationSpec = tween(100)),
                    exit = fadeOut(animationSpec = tween(100))
                ) {
                    val isSuccess = uiState.connectionTestResult is ConnectionTestResult.Success
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSuccess) Color(0xFF1A3A1A)
                                else Color(0xFF3A1A1A)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF6B6B)
                            )
                            Text(
                                text = when (val result = uiState.connectionTestResult) {
                                    is ConnectionTestResult.Success -> stringResource(R.string.connection_successful)
                                    is ConnectionTestResult.Failure -> result.message
                                    null -> ""
                                },
                                color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF6B6B),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Test Connection button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isValid && !uiState.isTestingConnection)
                                    Color(0xFF2A2A2A)
                                else
                                    Color(0xFF1A1A1A)
                            )
                            .clickable(
                                enabled = isValid && !uiState.isTestingConnection,
                                onClick = {
                                    val model = ModelConfig(
                                        id = existingModel?.id ?: UUID.randomUUID().toString(),
                                        displayName = displayName,
                                        baseUrl = baseUrl.trimEnd('/'),
                                        modelName = modelName,
                                        apiKey = apiKey.ifBlank { null },
                                        supportsVision = supportsVision,
                                        supportsThinking = supportsThinking,
                                        isDefault = existingModel?.isDefault ?: false,
                                        contextLength = contextLength.toIntOrNull() ?: 32768
                                    )
                                    viewModel.testConnection(model)
                                },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.test_connection),
                                color = if (isValid) Color.White.copy(alpha = 0.8f) else Color.White.copy(
                                    alpha = 0.3f
                                ),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Save button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isValid) Color.White else Color.White.copy(alpha = 0.2f)
                            )
                            .clickable(
                                enabled = isValid,
                                onClick = {
                                    val model = ModelConfig(
                                        id = existingModel?.id ?: UUID.randomUUID().toString(),
                                        displayName = displayName,
                                        baseUrl = baseUrl.trimEnd('/'),
                                        modelName = modelName,
                                        apiKey = apiKey.ifBlank { null },
                                        supportsVision = supportsVision,
                                        supportsThinking = supportsThinking,
                                        isDefault = existingModel?.isDefault ?: false,
                                        contextLength = contextLength.toIntOrNull() ?: 32768
                                    )
                                    viewModel.saveModel(model)
                                    onNavigateBack()
                                },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            color = if (isValid) Color.Black else Color.Black.copy(alpha = 0.3f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false,
    supportingText: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(
                onClick = { onCheckedChange(!checked) },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.White.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}
