package com.bsi.sfachatbot.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsi.sfachatbot.model.ChatMessage
import com.bsi.sfachatbot.model.TableData
import com.bsi.sfachatbot.ui.theme.AppBarEnd
import com.bsi.sfachatbot.ui.theme.AppBarStart
import com.bsi.sfachatbot.ui.theme.ChipBackground
import com.bsi.sfachatbot.ui.theme.ChipBorder
import com.bsi.sfachatbot.ui.theme.ChipText
import com.bsi.sfachatbot.ui.theme.ShimmerBase
import com.bsi.sfachatbot.ui.theme.ShimmerHighlight
import com.bsi.sfachatbot.ui.theme.SystemMessageBorder
import com.bsi.sfachatbot.ui.theme.UserBubbleEnd
import com.bsi.sfachatbot.ui.theme.UserBubbleStart

private val SUGGESTED_QUESTIONS = listOf(
    "Show all products",
    "Top 5 salesmen by sales",
    "Total orders this month",
    "Active salesmen & routes",
    "Visit effectiveness rate",
    "Revenue by category",
    "Average order value",
    "Orders by outlet"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenDrawer: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentConversationId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val currentTitle = remember(conversations, currentConversationId) {
        conversations.find { it.id == currentConversationId }?.title ?: "SFA Chatbot"
    }

    LaunchedEffect(messages.size, isLoading) {
        val itemCount = messages.size + if (isLoading) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Sales Force Automation",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open menu",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    Brush.horizontalGradient(listOf(AppBarStart, AppBarEnd))
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                value = inputText,
                isLoading = isLoading,
                onValueChange = { inputText = it },
                onQuestionSelected = { inputText = it },
                onSend = {
                    val text = inputText.trim()
                    if (text.isNotBlank()) {
                        viewModel.sendQuestion(text)
                        inputText = ""
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
            if (isLoading) {
                item { ShimmerBubble() }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Message bubbles
// ---------------------------------------------------------------------------

@Composable
private fun MessageBubble(message: ChatMessage) {
    if (!message.isUser && message.tableData != null) {
        TableBubble(tableData = message.tableData)
        return
    }

    val isUser = message.isUser
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 48.dp else 0.dp,
                end = if (isUser) 0.dp else 48.dp
            ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Brush.linearGradient(listOf(AppBarStart, AppBarEnd)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (isUser) {
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(
                        Brush.linearGradient(listOf(UserBubbleStart, UserBubbleEnd))
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Card(
                shape = bubbleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.border(
                    width = 1.5.dp,
                    color = SystemMessageBorder.copy(alpha = 0.4f),
                    shape = bubbleShape
                )
            ) {
                Text(
                    text = message.content,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun TableBubble(tableData: TableData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Brush.linearGradient(listOf(AppBarStart, AppBarEnd)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            DataTable(tableData = tableData)
        }
    }
}

@Composable
private fun DataTable(tableData: TableData) {
    // Pre-compute once: numeric detection and formatted display values
    val numericCols = tableData.columns.indices.map { isNumericColumn(tableData.rows, it) }
    val displayRows = tableData.rows.map { row ->
        row.mapIndexed { colIdx, cell ->
            if (numericCols.getOrElse(colIdx) { false }) formatNumericCell(cell) else cell
        }
    }

    // Compute content-based fixed width per column (dp value as Float for arithmetic)
    val colWidthsFloat = tableData.columns.mapIndexed { colIdx, header ->
        val headerLen = formatColumnName(header).length
        val maxDataLen = displayRows.maxOfOrNull { it.getOrElse(colIdx) { "" }.length } ?: 0
        ((maxOf(headerLen, maxDataLen) * 7.5f) + 24f).coerceIn(80f, 220f)
    }
    val totalContentWidth = colWidthsFloat.sum() + (tableData.columns.size - 1) // +1dp separators

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidthPx = maxWidth
        val needsScroll = totalContentWidth.dp > availableWidthPx

        // When content fits: stretch columns proportionally to fill full width
        // When content overflows: use fixed widths with horizontal scroll
        val scrollState = rememberScrollState()
        val tableModifier = if (needsScroll)
            Modifier.horizontalScroll(scrollState)
        else
            Modifier.fillMaxWidth()

        Column(modifier = tableModifier) {
            @Composable
            fun cellModifier(colIdx: Int): Modifier =
                if (needsScroll)
                    Modifier.width(colWidthsFloat[colIdx].dp)
                else
                    Modifier.weight(colWidthsFloat[colIdx])

            // Header row
            Row(
                modifier = Modifier
                    .then(if (needsScroll) Modifier else Modifier.fillMaxWidth())
                    .background(Brush.horizontalGradient(listOf(AppBarStart, AppBarEnd)))
            ) {
                tableData.columns.forEachIndexed { index, colName ->
                    if (index > 0) {
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                    }
                    val isNum = numericCols.getOrElse(index) { false }
                    Text(
                        text = formatColumnName(colName),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (isNum) TextAlign.End else TextAlign.Start,
                        modifier = cellModifier(index)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            // Data rows
            displayRows.forEachIndexed { rowIndex, row ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
                val rowBg = if (rowIndex % 2 == 0)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

                Row(
                    modifier = Modifier
                        .then(if (needsScroll) Modifier else Modifier.fillMaxWidth())
                        .background(rowBg)
                ) {
                    row.forEachIndexed { colIndex, cellValue ->
                        if (colIndex > 0) {
                            Spacer(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )
                        }
                        val isNum = numericCols.getOrElse(colIndex) { false }
                        Text(
                            text = cellValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = if (isNum) TextAlign.End else TextAlign.Start,
                            modifier = cellModifier(colIndex)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatColumnName(raw: String): String =
    raw.replace("_", " ")
        .split(" ")
        .joinToString(" ") { word -> word.replaceFirstChar(Char::titlecase) }

private fun isNumericColumn(rows: List<List<String>>, colIndex: Int): Boolean {
    val sample = rows.firstOrNull {
        it.getOrNull(colIndex)?.let { v -> v != "-" } == true
    }?.getOrNull(colIndex) ?: return false
    return sample.toDoubleOrNull() != null
}

private fun formatNumericCell(value: String): String {
    if (value == "-") return value
    val d = value.toDoubleOrNull() ?: return value
    return if (d == kotlin.math.floor(d) && !d.isInfinite()) d.toLong().toString()
    else "%.2f".format(d)
}

// ---------------------------------------------------------------------------
// Shimmer loading bubble
// ---------------------------------------------------------------------------

@Composable
private fun ShimmerBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )

    Row(
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Brush.linearGradient(listOf(AppBarStart, AppBarEnd)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerLine(widthDp = 200, progress = shimmerProgress)
            ShimmerLine(widthDp = 140, progress = shimmerProgress)
        }
    }
}

@Composable
private fun ShimmerLine(widthDp: Int, progress: Float) {
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = Offset(x = progress * widthDp * 3 - widthDp.toFloat(), y = 0f),
        end = Offset(x = progress * widthDp * 3, y = 0f)
    )
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(shimmerBrush)
    )
}

// ---------------------------------------------------------------------------
// Suggested question chips + input bar
// ---------------------------------------------------------------------------

@Composable
private fun SuggestedQuestionsRow(onQuestionSelected: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SUGGESTED_QUESTIONS) { question ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ChipBackground)
                    .border(1.dp, ChipBorder, RoundedCornerShape(20.dp))
                    .clickable { onQuestionSelected(question) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.labelMedium,
                    color = ChipText,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onQuestionSelected: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            SuggestedQuestionsRow(onQuestionSelected = onQuestionSelected)
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Ask a question...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    maxLines = 3,
                    enabled = !isLoading
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = if (!isLoading && value.isNotBlank())
                                Brush.linearGradient(listOf(UserBubbleStart, UserBubbleEnd))
                            else
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    )
                                ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onSend,
                        enabled = !isLoading && value.isNotBlank(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (!isLoading && value.isNotBlank())
                                Color.White
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
