package com.example.worshipstudio.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.worshipstudio.data.model.ChatMessage
import com.example.worshipstudio.ui.theme.Mint
import com.example.worshipstudio.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

private val FAB_SIZE    = 54.dp
private val EDGE_MARGIN = 12.dp

/**
 * Church chat. On the song list the button sits fixed in the header's
 * top-right corner; on every other screen it is a draggable floating bubble
 * that snaps to the nearest edge. Both open the full-screen conversation.
 */
@Composable
fun FloatingChat(
    chatViewModel:   ChatViewModel,
    currentUserId:   String,
    currentUserName: String,
    churchId:        String,
    fixedTopRight:   Boolean = true
) {
    val state     by chatViewModel.state.collectAsState()
    var isOpen    by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val listState  = rememberLazyListState()

    // Drag state — used when the button floats (non-song-list screens)
    val density      = LocalDensity.current
    val fabSizePx    = with(density) { FAB_SIZE.toPx() }
    val edgeMarginPx = with(density) { EDGE_MARGIN.toPx() }
    var screenWidthPx  by remember { mutableFloatStateOf(0f) }
    var screenHeightPx by remember { mutableFloatStateOf(0f) }
    var fabX by remember { mutableFloatStateOf(-1f) }
    var fabY by remember { mutableFloatStateOf(-1f) }
    val snappedX by animateFloatAsState(
        targetValue   = fabX.coerceAtLeast(0f),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label         = "fabSnapX"
    )

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

    // System back closes the chat
    BackHandler(enabled = isOpen) { isOpen = false }

    fun send() {
        if (inputText.isNotBlank()) {
            chatViewModel.sendMessage(inputText, churchId, currentUserId, currentUserName)
            inputText = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                screenWidthPx  = coords.size.width.toFloat()
                screenHeightPx = coords.size.height.toFloat()
                // First layout: park the bubble on the right edge, ~65% down
                if (fabX < 0f && screenWidthPx > 0f) {
                    fabX = screenWidthPx - fabSizePx - edgeMarginPx
                    fabY = screenHeightPx * 0.65f - fabSizePx / 2
                }
            }
    ) {

        // ── Chat button ───────────────────────────────────────────────────────
        if (!isOpen) {
            if (fixedTopRight) {
                // Fixed in the song-list header's top-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 14.dp, end = 16.dp)
                ) {
                    Surface(
                        onClick = {
                            isOpen = true
                            chatViewModel.markAllRead()
                        },
                        shape    = RoundedCornerShape(16.dp),
                        color    = Mint.Field,
                        border   = BorderStroke(1.dp, Mint.BorderSubtle),
                        modifier = Modifier.size(FAB_SIZE)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Church chat",
                                tint     = Mint.Accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    UnreadBadge(state.unreadCount, Modifier.align(Alignment.TopEnd))
                }
            } else if (fabX >= 0f) {
                // Draggable bubble — snaps to the nearest horizontal edge
                Box(
                    modifier = Modifier
                        .offset { IntOffset(snappedX.roundToInt(), fabY.roundToInt()) }
                        .size(FAB_SIZE)
                        .pointerInput(screenWidthPx) {
                            detectDragGestures(
                                onDragEnd = {
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
                    Surface(
                        onClick = {
                            isOpen = true
                            chatViewModel.markAllRead()
                        },
                        shape           = CircleShape,
                        color           = Mint.Accent,
                        shadowElevation = 8.dp,
                        modifier        = Modifier.size(FAB_SIZE)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Church chat",
                                tint     = Mint.OnAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    UnreadBadge(state.unreadCount, Modifier.align(Alignment.TopEnd))
                }
            }
        }

        // ── Full-screen conversation ──────────────────────────────────────────
        AnimatedVisibility(
            visible = isOpen,
            enter   = slideInVertically { it / 3 } + fadeIn(),
            exit    = slideOutVertically { it / 3 } + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Mint.BgTop, Mint.BgBottom)))
                    .statusBarsPadding()
            ) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isOpen = false }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Close chat",
                            tint = Mint.TextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Church Chat",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Mint.Accent
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Canvas(Modifier.size(7.dp)) { drawCircle(Mint.Accent) }
                            Text(
                                churchId,
                                fontSize = 12.sp,
                                color    = Mint.TextSecondary
                            )
                        }
                    }
                }
                HorizontalDivider(color = Mint.BorderSubtle)

                // ── Messages ──────────────────────────────────────────────────
                if (state.messages.isEmpty()) {
                    Box(
                        modifier         = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No messages yet. Say hi!",
                            fontSize = 14.sp,
                            color    = Mint.TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        state    = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding      = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp, vertical = 14.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            state.messages,
                            key         = { it.id.ifEmpty { it.timestamp.toString() } },
                            contentType = { "msg" }
                        ) { msg ->
                            ChatBubble(
                                message      = msg,
                                isOwnMessage = msg.senderId == currentUserId
                            )
                        }
                    }
                }

                // ── Input row ─────────────────────────────────────────────────
                HorizontalDivider(color = Mint.BorderSubtle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Mint.BgTop.copy(alpha = 0.97f))
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value         = inputText,
                        onValueChange = { inputText = it },
                        placeholder   = {
                            Text("Message…", fontSize = 14.sp, color = Mint.TextSecondary)
                        },
                        singleLine    = true,
                        shape         = RoundedCornerShape(50),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedTextColor        = Mint.TextPrimary,
                            unfocusedTextColor      = Mint.TextPrimary,
                            focusedBorderColor      = Mint.Accent.copy(alpha = 0.6f),
                            unfocusedBorderColor    = Mint.BorderField,
                            cursorColor             = Mint.Accent,
                            focusedContainerColor   = Mint.Field,
                            unfocusedContainerColor = Mint.Field
                        ),
                        modifier      = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() })
                    )
                    Surface(
                        onClick  = { send() },
                        enabled  = inputText.isNotBlank(),
                        shape    = CircleShape,
                        color    = if (inputText.isNotBlank()) Mint.Accent
                                   else Mint.Accent.copy(alpha = 0.25f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint     = Mint.OnAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Unread count badge ─────────────────────────────────────────────────────────
@Composable
private fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    if (count > 0) {
        Box(
            modifier = modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Mint.Error),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = if (count > 9) "9+" else count.toString(),
                color      = androidx.compose.ui.graphics.Color.White,
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Single chat message — mockup style ─────────────────────────────────────────
@Composable
private fun ChatBubble(message: ChatMessage, isOwnMessage: Boolean) {
    val time = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    if (isOwnMessage) {
        // Own message: right-aligned, mint bubble
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(time, fontSize = 11.sp, color = Mint.TextSecondary)
                Text("You", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Mint.Accent)
            }
            Spacer(Modifier.height(5.dp))
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = 18.dp, bottomEnd = 5.dp
                ),
                color = Mint.Accent,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text     = message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 14.sp,
                    color    = Mint.OnAccent
                )
            }
        }
    } else {
        // Other member: avatar + name + time, dark bubble
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Mint.Field),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    message.senderName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Mint.Accent
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        message.senderName,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Mint.Accent
                    )
                    Text(time, fontSize = 11.sp, color = Mint.TextSecondary)
                }
                Spacer(Modifier.height(5.dp))
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 5.dp, topEnd = 18.dp,
                        bottomStart = 18.dp, bottomEnd = 18.dp
                    ),
                    color  = Mint.Card,
                    border = BorderStroke(1.dp, Mint.BorderSubtle),
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Text(
                        text     = message.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontSize = 14.sp,
                        color    = Mint.TextPrimary
                    )
                }
            }
        }
    }
}
