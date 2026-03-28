package com.example.worshipstudio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.ChatMessage
import com.example.worshipstudio.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
private val FAB_SIZE = 48.dp
private val EDGE_MARGIN = 12.dp

@Composable
fun FloatingChat(
    chatViewModel:   ChatViewModel,
    currentUserId:   String,
    currentUserName: String,
    churchId:        String
) {
    val state     by chatViewModel.state.collectAsState()
    var isOpen    by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val listState  = rememberLazyListState()
    val density    = LocalDensity.current

    val fabSizePx     = with(density) { FAB_SIZE.toPx() }
    val edgeMarginPx  = with(density) { EDGE_MARGIN.toPx() }

    // Screen size — filled in once the root Box is laid out
    var screenWidthPx  by remember { mutableFloatStateOf(0f) }
    var screenHeightPx by remember { mutableFloatStateOf(0f) }

    // FAB position — starts on the left side, ~70% down
    var fabX by remember { mutableFloatStateOf(edgeMarginPx) }
    var fabY by remember { mutableFloatStateOf(0f) } // set once screen height is known

    // Smooth snap animation on X axis
    val snappedX by animateFloatAsState(
        targetValue = fabX,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "fabSnapX"
    )

    val fabOnLeft = snappedX < (screenWidthPx / 2)

    // Start observing messages
    LaunchedEffect(churchId, currentUserId) {
        chatViewModel.init(churchId, currentUserId)
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(state.messages.size, isOpen) {
        if (isOpen && state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                screenWidthPx  = coords.size.width.toFloat()
                screenHeightPx = coords.size.height.toFloat()
                // Set initial Y once we know screen height (70% down)
                if (fabY == 0f && screenHeightPx > 0f) {
                    fabY = screenHeightPx * 0.70f - fabSizePx / 2
                }
            }
    ) {

        // ── Chat panel ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = isOpen,
            enter    = slideInVertically { it / 2 } + fadeIn(),
            exit     = slideOutVertically { it / 2 } + fadeOut(),
            modifier = Modifier
                .align(if (fabOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(
                    start  = if (fabOnLeft) EDGE_MARGIN else 0.dp,
                    end    = if (fabOnLeft) 0.dp else EDGE_MARGIN,
                    bottom = FAB_SIZE + EDGE_MARGIN + 8.dp
                )
        ) {
            Surface(
                modifier        = Modifier
                    .widthIn(min = 300.dp, max = 360.dp)
                    .navigationBarsPadding(),
                shape           = RoundedCornerShape(20.dp),
                color           = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 16.dp,
                tonalElevation  = 0.dp
            ) {
                Column(modifier = Modifier.height(420.dp)) {

                    // ── Header ────────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Church Chat",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick  = { isOpen = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, "Close chat",
                                tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    // ── Messages ──────────────────────────────────────────────
                    if (state.messages.isEmpty()) {
                        Box(
                            modifier         = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No messages yet. Say hi!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state    = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.messages, key = { it.id.ifEmpty { it.timestamp.toString() } }) { msg ->
                                ChatBubble(
                                    message      = msg,
                                    isOwnMessage = msg.senderId == currentUserId
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // ── Input row ─────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value         = inputText,
                            onValueChange = { inputText = it },
                            placeholder   = { Text("Message…", style = MaterialTheme.typography.bodySmall) },
                            singleLine    = true,
                            shape         = RoundedCornerShape(20.dp),
                            modifier      = Modifier.weight(1f),
                            textStyle     = MaterialTheme.typography.bodySmall,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                chatViewModel.sendMessage(inputText, churchId, currentUserId, currentUserName)
                                inputText = ""
                            })
                        )
                        IconButton(
                            onClick = {
                                chatViewModel.sendMessage(inputText, churchId, currentUserId, currentUserName)
                                inputText = ""
                            },
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        // ── Floating draggable button ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset(snappedX.roundToInt(), fabY.roundToInt()) }
                .size(FAB_SIZE)
                .pointerInput(screenWidthPx) {
                    detectDragGestures(
                        onDragEnd = {
                            // Snap to nearest horizontal edge
                            val snapRight = fabX + fabSizePx / 2 > screenWidthPx / 2
                            fabX = if (snapRight) screenWidthPx - fabSizePx - edgeMarginPx
                                   else edgeMarginPx
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        fabX = (fabX + dragAmount.x).coerceIn(0f, screenWidthPx - fabSizePx)
                        fabY = (fabY + dragAmount.y).coerceIn(0f, screenHeightPx - fabSizePx)
                    }
                }
        ) {
            FloatingActionButton(
                onClick = {
                    isOpen = !isOpen
                    if (isOpen) chatViewModel.markAllRead()
                },
                modifier       = Modifier.size(FAB_SIZE),
                shape          = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                elevation      = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(
                    Icons.Default.ChatBubble,
                    contentDescription = "Chat",
                    modifier           = Modifier.size(22.dp),
                    tint               = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // ── Unread badge ──────────────────────────────────────────────────
            if (state.unreadCount > 0 && !isOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (state.unreadCount > 9) "9+" else state.unreadCount.toString(),
                        color      = MaterialTheme.colorScheme.onError,
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Single chat bubble ─────────────────────────────────────────────────────────
@Composable
private fun ChatBubble(message: ChatMessage, isOwnMessage: Boolean) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        if (!isOwnMessage) {
            Text(
                text     = message.senderName,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Row(
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
            modifier              = Modifier.fillMaxWidth()
        ) {
            if (!isOwnMessage) Spacer(Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(
                    topStart    = 16.dp,
                    topEnd      = 16.dp,
                    bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                    bottomEnd   = if (isOwnMessage) 4.dp else 16.dp
                ),
                color = if (isOwnMessage)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text     = message.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = if (isOwnMessage)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            if (isOwnMessage) Spacer(Modifier.width(4.dp))
        }
        Text(
            text     = timeFormat.format(Date(message.timestamp)),
            style    = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(
                start = if (isOwnMessage) 0.dp else 8.dp,
                end   = if (isOwnMessage) 8.dp else 0.dp,
                top   = 2.dp
            )
        )
    }
}
