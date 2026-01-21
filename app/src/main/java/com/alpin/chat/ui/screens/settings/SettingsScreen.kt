package com.alpin.chat.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alpin.chat.R
import com.alpin.chat.domain.model.ModelConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddModel: (ModelConfig?) -> Unit,
    onNavigateToTokenizer: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showSystemPromptDialog by remember { mutableStateOf(false) }
    var editedSystemPrompt by remember { mutableStateOf(uiState.settings.systemPrompt) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }

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
                    text = stringResource(R.string.settings),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Models Section
                item {
                    SectionHeader(title = "Models")
                }

                if (uiState.models.isEmpty()) {
                    item {
                        EmptyModelsCard(onAddModel = { onNavigateToAddModel(null) })
                    }
                } else {
                    items(uiState.models, key = { it.id }) { model ->
                        ModelCard(
                            model = model,
                            onEdit = { onNavigateToAddModel(model) },
                            onDelete = { viewModel.deleteModel(model.id) },
                            onSetDefault = { viewModel.setDefaultModel(model.id) }
                        )
                    }

                    item {
                        AddModelButton(onClick = { onNavigateToAddModel(null) })
                    }
                }

                // Generation Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = "Sampling")
                }

                // Temperature (always visible)
                item {
                    SamplingSliderSetting(
                        label = stringResource(R.string.temperature),
                        value = uiState.settings.temperature,
                        enabled = uiState.settings.temperature != null,
                        onValueChange = { value, enabled ->
                            viewModel.updateTemperature(value, enabled)
                        },
                        valueRange = 0f..2f,
                        steps = 19,
                        defaultValue = 0.7f,
                        formatValue = { String.format("%.2f", it) }
                    )
                }

                // Top P (always visible)
                item {
                    SamplingSliderSetting(
                        label = "Top P",
                        value = uiState.settings.topP,
                        enabled = uiState.settings.topP != null,
                        onValueChange = { value, enabled ->
                            viewModel.updateTopP(value, enabled)
                        },
                        valueRange = 0f..1f,
                        steps = 99,
                        defaultValue = 1.0f,
                        formatValue = { String.format("%.2f", it) }
                    )
                }

                // Advanced section header
                item {
                    AdvancedSectionHeader(
                        expanded = advancedExpanded,
                        onClick = { advancedExpanded = !advancedExpanded }
                    )
                }

                // Advanced sampling parameters
                item {
                    AnimatedVisibility(
                        visible = advancedExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Top K
                            SamplingIntSliderSetting(
                                label = "Top K",
                                value = uiState.settings.topK,
                                enabled = uiState.settings.topK != null,
                                onValueChange = { value, enabled ->
                                    viewModel.updateTopK(value, enabled)
                                },
                                valueRange = 1..200,
                                defaultValue = 40
                            )

                            // Min P
                            SamplingSliderSetting(
                                label = "Min P",
                                value = uiState.settings.minP,
                                enabled = uiState.settings.minP != null,
                                onValueChange = { value, enabled ->
                                    viewModel.updateMinP(value, enabled)
                                },
                                valueRange = 0f..1f,
                                steps = 99,
                                defaultValue = 0.0f,
                                formatValue = { String.format("%.2f", it) }
                            )

                            // Presence Penalty
                            SamplingSliderSetting(
                                label = "Presence Penalty",
                                value = uiState.settings.presencePenalty,
                                enabled = uiState.settings.presencePenalty != null,
                                onValueChange = { value, enabled ->
                                    viewModel.updatePresencePenalty(value, enabled)
                                },
                                valueRange = -2f..2f,
                                steps = 39,
                                defaultValue = 0.0f,
                                formatValue = { String.format("%.1f", it) }
                            )

                            // Frequency Penalty
                            SamplingSliderSetting(
                                label = "Frequency Penalty",
                                value = uiState.settings.frequencyPenalty,
                                enabled = uiState.settings.frequencyPenalty != null,
                                onValueChange = { value, enabled ->
                                    viewModel.updateFrequencyPenalty(value, enabled)
                                },
                                valueRange = -2f..2f,
                                steps = 39,
                                defaultValue = 0.0f,
                                formatValue = { String.format("%.1f", it) }
                            )

                            // Repetition Penalty
                            SamplingSliderSetting(
                                label = "Repetition Penalty",
                                value = uiState.settings.repetitionPenalty,
                                enabled = uiState.settings.repetitionPenalty != null,
                                onValueChange = { value, enabled ->
                                    viewModel.updateRepetitionPenalty(value, enabled)
                                },
                                valueRange = 0f..2f,
                                steps = 19,
                                defaultValue = 1.0f,
                                formatValue = { String.format("%.2f", it) }
                            )
                        }
                    }
                }

                // System Prompt Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = "System")
                }

                item {
                    SystemPromptSetting(
                        systemPrompt = uiState.settings.systemPrompt,
                        onClick = {
                            editedSystemPrompt = uiState.settings.systemPrompt
                            showSystemPromptDialog = true
                        }
                    )
                }

                // Tools Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = "Tools")
                }

                item {
                    TokenizerNavigationCard(
                        tokenizerLoaded = uiState.tokenizerLoaded,
                        onClick = onNavigateToTokenizer
                    )
                }
            }
        }
    }

    if (showSystemPromptDialog) {
        AlertDialog(
            onDismissRequest = { showSystemPromptDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.system_prompt),
                    color = Color.White
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2A))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = editedSystemPrompt,
                        onValueChange = { editedSystemPrompt = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(Color.White)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSystemPrompt(editedSystemPrompt)
                        showSystemPromptDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemPromptDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun AdvancedSectionHeader(
    expanded: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(
                onClick = onClick,
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
            Text(
                text = "Advanced",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(20.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun EmptyModelsCard(onAddModel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(
                onClick = onAddModel,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
            Text(
                text = stringResource(R.string.no_models),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.add_first_model),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AddModelButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = stringResource(R.string.add_model),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(
                onClick = onEdit,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.displayName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (model.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Default",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = model.modelName,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
                Text(
                    text = model.baseUrl,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!model.isDefault) {
                    IconButton(
                        onClick = onSetDefault,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Set as default",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFFF6B6B)
                    )
                }
            }
        }
    }
}

@Composable
private fun SamplingSliderSetting(
    label: String,
    value: Float?,
    enabled: Boolean,
    onValueChange: (Float?, Boolean) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    defaultValue: Float,
    formatValue: (Float) -> String
) {
    var sliderValue by remember(value, enabled) {
        mutableFloatStateOf(value ?: defaultValue)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = label,
                        color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 15.sp
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { newEnabled ->
                            onValueChange(if (newEnabled) sliderValue else null, newEnabled)
                        },
                        modifier = Modifier.height(20.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
                Text(
                    text = if (enabled) formatValue(sliderValue) else "Off",
                    color = Color.White.copy(alpha = if (enabled) 0.6f else 0.4f),
                    fontSize = 15.sp
                )
            }
            if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onValueChange(sliderValue, true) },
                    valueRange = valueRange,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

@Composable
private fun SamplingIntSliderSetting(
    label: String,
    value: Int?,
    enabled: Boolean,
    onValueChange: (Int?, Boolean) -> Unit,
    valueRange: IntRange,
    defaultValue: Int
) {
    var sliderValue by remember(value, enabled) {
        mutableIntStateOf(value ?: defaultValue)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = label,
                        color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 15.sp
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { newEnabled ->
                            onValueChange(if (newEnabled) sliderValue else null, newEnabled)
                        },
                        modifier = Modifier.height(20.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
                Text(
                    text = if (enabled) sliderValue.toString() else "Off",
                    color = Color.White.copy(alpha = if (enabled) 0.6f else 0.4f),
                    fontSize = 15.sp
                )
            }
            if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue.toFloat(),
                    onValueChange = { sliderValue = it.toInt() },
                    onValueChangeFinished = { onValueChange(sliderValue, true) },
                    valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                    steps = valueRange.last - valueRange.first - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

@Composable
private fun SystemPromptSetting(
    systemPrompt: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(
                onClick = onClick,
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
                    text = stringResource(R.string.system_prompt),
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    text = systemPrompt.take(60) + if (systemPrompt.length > 60) "..." else "",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun TokenizerNavigationCard(
    tokenizerLoaded: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(
                onClick = onClick,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tokenizer",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (tokenizerLoaded) Color(0xFF4CAF50)
                                else Color(0xFFFF5252)
                            )
                    )
                }
                Text(
                    text = "Visualize how text is tokenized",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
