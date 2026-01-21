package com.alpin.chat.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alpin.chat.R
import com.alpin.chat.domain.model.Message
import com.alpin.chat.domain.model.MessageRole
import com.alpin.chat.ui.theme.UserBubbleColor
import com.alpin.chat.ui.theme.UserBubbleColorDark
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun ChatBubble(
    message: Message,
    modifier: Modifier = Modifier,
    tokenCount: Int? = null,
    onRegenerate: () -> Unit = {},
    onPreviousAlternative: () -> Unit = {},
    onNextAlternative: () -> Unit = {},
    onDelete: () -> Unit = {},
    onEdit: (String) -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var thinkingExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(message.displayContent) { mutableStateOf(message.displayContent) }

    val bubbleColor = when {
        isUser && isDark -> UserBubbleColorDark
        isUser -> UserBubbleColor
        else -> Color.Transparent
    }

    val textColor = when {
        isUser -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val hasThinkingContent = !message.displayThinkingContent.isNullOrEmpty()
    val isComplete = !message.isStreaming && !message.isThinking

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Thinking section (expandable)
        if (hasThinkingContent && !isUser) {
            ThinkingSection(
                thinkingContent = message.displayThinkingContent ?: "",
                isThinking = message.isThinking,
                isExpanded = thinkingExpanded,
                onToggle = { thinkingExpanded = !thinkingExpanded },
                isDark = isDark
            )

            if (message.displayContent.isNotEmpty() || !message.isThinking) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Main content - bubble for user, no bubble for assistant
        if (message.displayContent.isNotEmpty() || !message.isThinking || message.imageData != null) {
            if (isUser) {
                // User message with bubble
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (isEditing) {
                        // Edit mode
                        EditMessageField(
                            text = editText,
                            onTextChange = { editText = it },
                            onSave = {
                                if (editText.isNotBlank() && editText != message.displayContent) {
                                    onEdit(editText)
                                }
                                isEditing = false
                            },
                            onCancel = {
                                editText = message.displayContent
                                isEditing = false
                            },
                            isDark = isDark
                        )
                    } else {
                        // Normal display mode
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = bubbleColor,
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                message.imageData?.let { imageData ->
                                    AsyncImage(
                                        model = imageData,
                                        contentDescription = "Attached image",
                                        modifier = Modifier
                                            .widthIn(max = 200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (message.displayContent.isNotEmpty()) {
                                    MarkdownText(
                                        markdown = message.displayContent,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons always visible (hide during edit)
                    if (!isEditing) {
                        UserMessageActions(
                            tokenCount = tokenCount,
                            onCopy = { copyToClipboard(context, message.displayContent) },
                            onDelete = onDelete,
                            onEdit = { isEditing = true }
                        )
                    }
                }
            } else {
                // Assistant message without bubble - full width
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isEditing) {
                        // Edit mode for assistant
                        EditMessageField(
                            text = editText,
                            onTextChange = { editText = it },
                            onSave = {
                                if (editText.isNotBlank() && editText != message.displayContent) {
                                    onEdit(editText)
                                }
                                isEditing = false
                            },
                            onCancel = {
                                editText = message.displayContent
                                isEditing = false
                            },
                            isDark = isDark,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Normal display mode - Swipe animation for content
                        AnimatedContent(
                            targetState = message.currentAlternativeIndex,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { it } + fadeIn() togetherWith
                                            slideOutHorizontally { -it } + fadeOut()
                                } else {
                                    slideInHorizontally { -it } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                }
                            },
                            label = "alternativeSwipe"
                        ) { _ ->
                            Column {
                                message.imageData?.let { imageData ->
                                    AsyncImage(
                                        model = imageData,
                                        contentDescription = "Attached image",
                                        modifier = Modifier
                                            .widthIn(max = 200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (message.displayContent.isNotEmpty()) {
                                    MarkdownText(
                                        markdown = message.displayContent,
                                        color = textColor
                                    )
                                }

                                if (message.isStreaming && !message.isThinking) {
                                    TypingIndicator(
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons - show only when message is complete and not editing
                    if (isComplete && message.displayContent.isNotEmpty() && !isEditing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MessageActions(
                            message = message,
                            tokenCount = tokenCount,
                            onCopy = { copyToClipboard(context, message.displayContent) },
                            onRegenerate = onRegenerate,
                            onDelete = onDelete,
                            onEdit = { isEditing = true },
                            onPrevious = onPreviousAlternative,
                            onNext = onNextAlternative
                        )
                    }
                }
            }
        } else if (message.isThinking && message.displayThinkingContent.isNullOrEmpty()) {
            // Show a minimal indicator when thinking starts but no content yet
            val thinkingTextColor = if (isDark) {
                Color.White.copy(alpha = 0.5f)
            } else {
                Color.Black.copy(alpha = 0.5f)
            }
            Row(
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.thinking),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = thinkingTextColor
                )
                TypingIndicator(dotColor = thinkingTextColor)
            }
        }
    }
}

@Composable
private fun MessageActions(
    message: Message,
    tokenCount: Int?,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val hasAlternatives = message.totalAlternatives > 1

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side - action buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Copy button
            ActionIconButton(
                icon = Icons.Outlined.ContentCopy,
                contentDescription = "Copy",
                onClick = onCopy,
                tint = iconColor
            )

            // Edit button
            ActionIconButton(
                icon = Icons.Outlined.Edit,
                contentDescription = "Edit",
                onClick = onEdit,
                tint = iconColor
            )

            // Regenerate button
            ActionIconButton(
                icon = Icons.Outlined.Refresh,
                contentDescription = "Regenerate",
                onClick = onRegenerate,
                tint = iconColor
            )

            // Delete button
            ActionIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = "Delete",
                onClick = onDelete,
                tint = iconColor
            )

            // Alternative navigation (only show if there are alternatives)
            if (hasAlternatives) {
                Spacer(modifier = Modifier.width(8.dp))

                // Previous button
                ActionIconButton(
                    icon = Icons.Outlined.ChevronLeft,
                    contentDescription = "Previous response",
                    onClick = onPrevious,
                    tint = iconColor,
                    enabled = message.currentAlternativeIndex > 0
                )

                // Counter
                Text(
                    text = "${message.currentAlternativeIndex + 1}/${message.totalAlternatives}",
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor,
                    fontSize = 11.sp
                )

                // Next button
                ActionIconButton(
                    icon = Icons.Outlined.ChevronRight,
                    contentDescription = "Next response",
                    onClick = onNext,
                    tint = iconColor,
                    enabled = message.currentAlternativeIndex < message.totalAlternatives - 1
                )
            }
        }

        // Right side - token count
        if (tokenCount != null && tokenCount > 0) {
            Text(
                text = "%,d tokens".format(tokenCount),
                style = MaterialTheme.typography.labelSmall,
                color = iconColor,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) tint else tint.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun UserMessageActions(
    tokenCount: Int?,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Token count
        if (tokenCount != null && tokenCount > 0) {
            Text(
                text = "%,d tokens".format(tokenCount),
                style = MaterialTheme.typography.labelSmall,
                color = iconColor,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Copy button
        ActionIconButton(
            icon = Icons.Outlined.ContentCopy,
            contentDescription = "Copy",
            onClick = onCopy,
            tint = iconColor
        )

        // Edit button
        ActionIconButton(
            icon = Icons.Outlined.Edit,
            contentDescription = "Edit",
            onClick = onEdit,
            tint = iconColor
        )

        // Delete button
        ActionIconButton(
            icon = Icons.Outlined.Delete,
            contentDescription = "Delete",
            onClick = onDelete,
            tint = iconColor
        )
    }
}

@Composable
private fun EditMessageField(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)

    Column(modifier = modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = backgroundColor,
                unfocusedContainerColor = backgroundColor,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = borderColor,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp),
            minLines = 1,
            maxLines = 10
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onSave) {
                Text(
                    text = "Save",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ThinkingSection(
    thinkingContent: String,
    isThinking: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isDark: Boolean
) {
    // Subtle, semi-transparent text color
    val thinkingTextColor = if (isDark) {
        Color.White.copy(alpha = 0.5f)
    } else {
        Color.Black.copy(alpha = 0.5f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    // Animate the rotation of the expand icon
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "expandRotation"
    )

    // Scroll state for auto-scrolling during streaming
    val scrollState = rememberScrollState()

    // Track if user has manually scrolled away from bottom
    var userScrolledAway by remember { mutableStateOf(false) }

    // Detect when user scrolls manually
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && isThinking) {
            // User is actively scrolling during thinking - check if they scrolled up
            val isNearBottom = scrollState.value >= scrollState.maxValue - 50
            if (!isNearBottom) {
                userScrolledAway = true
            }
        }
    }

    // Reset userScrolledAway when thinking completes or when user scrolls back to bottom
    LaunchedEffect(isThinking) {
        if (!isThinking) {
            userScrolledAway = false
        }
    }

    // Auto-scroll to bottom during streaming, but only if user hasn't scrolled away
    // Use non-animated scroll for smooth continuous updates
    LaunchedEffect(thinkingContent) {
        if (isThinking && isExpanded && !userScrolledAway) {
            // Use instant scroll instead of animated to avoid jitter
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column {
        // Header row - "Thinking..." with toggle (no ripple)
        Row(
            modifier = Modifier
                .clickable(
                    onClick = onToggle,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isThinking) stringResource(R.string.thinking) else stringResource(R.string.thought_process),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing * 0.5f
                ),
                color = thinkingTextColor
            )
            if (isThinking) {
                TypingIndicator(
                    dotColor = thinkingTextColor
                )
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotationAngle },
                tint = thinkingTextColor
            )
        }

        // Expandable content with smooth spring animation
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(
                animationSpec = tween(200)
            ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(
                animationSpec = tween(150)
            )
        ) {
            if (thinkingContent.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .heightIn(max = 300.dp)
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .verticalScroll(scrollState)
                        .padding(14.dp)
                ) {
                    MarkdownText(
                        markdown = thinkingContent,
                        color = thinkingTextColor
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("message", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
