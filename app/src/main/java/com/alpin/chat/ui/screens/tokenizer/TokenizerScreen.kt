package com.alpin.chat.ui.screens.tokenizer

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// Beautiful color palette for tokens - carefully selected for visual appeal and contrast
private val tokenColors = listOf(
    Color(0xFF7C3AED), // Purple
    Color(0xFF2563EB), // Blue
    Color(0xFF0891B2), // Cyan
    Color(0xFF059669), // Emerald
    Color(0xFF65A30D), // Lime
    Color(0xFFCA8A04), // Yellow
    Color(0xFFEA580C), // Orange
    Color(0xFFDC2626), // Red
    Color(0xFFDB2777), // Pink
    Color(0xFF9333EA), // Violet
    Color(0xFF4F46E5), // Indigo
    Color(0xFF0284C7), // Light Blue
    Color(0xFF0D9488), // Teal
    Color(0xFF16A34A), // Green
    Color(0xFF84CC16), // Light Green
    Color(0xFFEAB308), // Amber
    Color(0xFFF97316), // Light Orange
    Color(0xFFEF4444), // Light Red
    Color(0xFFEC4899), // Light Pink
    Color(0xFFA855F7), // Light Purple
)

@Composable
fun TokenizerScreen(
    onNavigateBack: () -> Unit,
    viewModel: TokenizerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
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
                    text = "Tokenizer",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                StatusCard(
                    isLoading = uiState.tokenizerLoading,
                    isLoaded = uiState.tokenizerLoaded,
                    vocabSize = uiState.vocabSize,
                    errorMessage = uiState.errorMessage
                )

                if (uiState.tokenizerLoaded) {
                    // Inline Tokenizer Input with visualization
                    TokenizerInput(
                        inputText = uiState.inputText,
                        tokens = uiState.tokens,
                        tokenCount = uiState.tokens.size,
                        onInputChange = { viewModel.updateInputText(it) },
                        onClear = { viewModel.clearInput() },
                        isTokenizing = uiState.isTokenizing
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    isLoading: Boolean,
    isLoaded: Boolean,
    vocabSize: Int,
    errorMessage: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(20.dp)
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Loading tokenizer...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        } else {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "GLM4 Tokenizer",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isLoaded) Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    else Color(0xFFFF5252).copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isLoaded) Color(0xFF4CAF50)
                                            else Color(0xFFFF5252)
                                        )
                                )
                                Text(
                                    text = if (isLoaded) "Ready" else "Error",
                                    color = if (isLoaded) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                if (isLoaded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Vocabulary: ${"%,d".format(vocabSize)} tokens",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }

                if (!isLoaded && errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF5252).copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenizerInput(
    inputText: String,
    tokens: List<TokenInfo>,
    tokenCount: Int,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    isTokenizing: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // Build annotated string with colored tokens
    val annotatedText = remember(inputText, tokens) {
        buildAnnotatedString {
            if (tokens.isEmpty() || inputText.isEmpty()) {
                append(inputText)
                if (inputText.isNotEmpty()) {
                    addStyle(
                        SpanStyle(color = Color.White),
                        0,
                        inputText.length
                    )
                }
            } else {
                var currentIndex = 0
                tokens.forEachIndexed { index, token ->
                    val tokenText = token.text
                    val color = tokenColors[index % tokenColors.size]

                    val tokenStart = currentIndex
                    val tokenEnd = tokenStart + tokenText.length

                    if (tokenStart < inputText.length) {
                        val actualEnd = minOf(tokenEnd, inputText.length)
                        append(inputText.substring(tokenStart, actualEnd))
                        addStyle(
                            SpanStyle(
                                color = color,
                                background = color.copy(alpha = 0.2f)
                            ),
                            length - (actualEnd - tokenStart),
                            length
                        )
                        currentIndex = actualEnd
                    }
                }
                // Append any remaining text (not yet tokenized)
                if (currentIndex < inputText.length) {
                    append(inputText.substring(currentIndex))
                    addStyle(
                        SpanStyle(color = Color.White.copy(alpha = 0.5f)),
                        length - (inputText.length - currentIndex),
                        length
                    )
                }
            }
        }
    }

    // Track cursor/selection state separately from text
    var selection by remember { mutableStateOf(TextRange(inputText.length)) }

    // Create TextFieldValue with annotated string for colored text
    val textFieldValue = TextFieldValue(
        annotatedString = annotatedText,
        selection = selection
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        // Header row with token count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - token count badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (inputText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isTokenizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White.copy(alpha = 0.6f),
                                    strokeWidth = 1.5.dp
                                )
                            }
                            Text(
                                text = if (isTokenizing) "counting..." else "%,d tokens".format(tokenCount),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Right side - clear button
            if (inputText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            onClick = {
                                onClear()
                                selection = TextRange.Zero
                            },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Clear",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Input area - fills remaining space
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .clickable(
                    onClick = { focusRequester.requestFocus() },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Placeholder
            if (inputText.isEmpty()) {
                Text(
                    text = "Type or paste text here to see how it tokenizes...",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Single BasicTextField with colored annotated string
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    selection = newValue.selection
                    if (newValue.text != inputText) {
                        onInputChange(newValue.text)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(Color.White)
            )
        }
    }
}

