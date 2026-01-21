package com.alpin.chat.ui.screens.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alpin.chat.R
import com.alpin.chat.domain.model.Conversation
import com.alpin.chat.ui.components.ConversationItem
import java.util.Calendar

private data class ConversationGroup(
    val title: String,
    val conversations: List<Conversation>
)

private fun groupConversations(conversations: List<Conversation>): List<ConversationGroup> {
    if (conversations.isEmpty()) return emptyList()

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val sevenDaysAgo = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -7)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val thirtyDaysAgo = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -30)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val todayList = mutableListOf<Conversation>()
    val yesterdayList = mutableListOf<Conversation>()
    val last7DaysList = mutableListOf<Conversation>()
    val last30DaysList = mutableListOf<Conversation>()
    val olderList = mutableListOf<Conversation>()

    conversations.forEach { conversation ->
        val conversationDate = Calendar.getInstance().apply {
            timeInMillis = conversation.updatedAt
        }

        when {
            conversationDate.timeInMillis >= today.timeInMillis -> todayList.add(conversation)
            conversationDate.timeInMillis >= yesterday.timeInMillis -> yesterdayList.add(conversation)
            conversationDate.timeInMillis >= sevenDaysAgo.timeInMillis -> last7DaysList.add(conversation)
            conversationDate.timeInMillis >= thirtyDaysAgo.timeInMillis -> last30DaysList.add(conversation)
            else -> olderList.add(conversation)
        }
    }

    return buildList {
        if (todayList.isNotEmpty()) add(ConversationGroup("Today", todayList))
        if (yesterdayList.isNotEmpty()) add(ConversationGroup("Yesterday", yesterdayList))
        if (last7DaysList.isNotEmpty()) add(ConversationGroup("Previous 7 Days", last7DaysList))
        if (last30DaysList.isNotEmpty()) add(ConversationGroup("Previous 30 Days", last30DaysList))
        if (olderList.isNotEmpty()) add(ConversationGroup("Older", olderList))
    }
}

@Composable
fun ConversationsDrawer(
    selectedConversationId: String?,
    onConversationSelected: (String) -> Unit,
    onNewConversation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversationGroups = remember(uiState.conversations) {
        groupConversations(uiState.conversations)
    }

    Surface(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        color = Color(0xFF0A0A0A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header section
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // New Chat button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            onClick = onNewConversation,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A2A2A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.new_chat),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search bar
                val searchShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(searchShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::searchConversations,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.search),
                                            color = Color.White.copy(alpha = 0.35f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.06f),
                thickness = 1.dp
            )

            // Conversations list
            if (uiState.conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_conversations),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start a new chat to begin",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    conversationGroups.forEach { group ->
                        // Section header
                        item(key = "header_${group.title}") {
                            Text(
                                text = group.title,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 8.dp
                                )
                            )
                        }

                        // Conversations in this group
                        items(
                            items = group.conversations,
                            key = { it.id }
                        ) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                isSelected = conversation.id == selectedConversationId,
                                onClick = { onConversationSelected(conversation.id) },
                                onDelete = { viewModel.deleteConversation(conversation.id) },
                                showConfirmation = uiState.confirmDeleteConversation,
                                onDisableConfirmation = { viewModel.disableDeleteConfirmation() },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            // Bottom section with settings
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.06f),
                thickness = 1.dp
            )

            // Settings row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onNavigateToSettings,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                        modifier = Modifier.size(18.dp),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}
