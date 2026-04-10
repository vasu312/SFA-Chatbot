package com.bsi.sfachatbot.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsi.sfachatbot.data.remote.dto.SummaryResponse
import com.bsi.sfachatbot.data.remote.dto.SummaryStats
import com.bsi.sfachatbot.data.remote.dto.TopPerformer
import com.bsi.sfachatbot.ui.theme.AppBarEnd
import com.bsi.sfachatbot.ui.theme.AppBarStart
import com.bsi.sfachatbot.ui.theme.CardDayEnd
import com.bsi.sfachatbot.ui.theme.CardDayStart
import com.bsi.sfachatbot.ui.theme.CardMonthEnd
import com.bsi.sfachatbot.ui.theme.CardMonthStart
import com.bsi.sfachatbot.ui.theme.DashboardBgEnd
import com.bsi.sfachatbot.ui.theme.DashboardBgStart
import com.bsi.sfachatbot.ui.theme.TopOutletEnd
import com.bsi.sfachatbot.ui.theme.TopOutletStart
import com.bsi.sfachatbot.ui.theme.TopProductEnd
import com.bsi.sfachatbot.ui.theme.TopRouteEnd
import com.bsi.sfachatbot.ui.theme.TopRouteStart
import com.bsi.sfachatbot.ui.theme.TopProductStart
import com.bsi.sfachatbot.ui.theme.TopSalesmanEnd
import com.bsi.sfachatbot.ui.theme.TopSalesmanStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: SummaryResponse?,
    onOpenDrawer: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Performance Overview",
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
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(listOf(AppBarStart, AppBarEnd))
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Brush.verticalGradient(listOf(DashboardBgStart, DashboardBgEnd)))
        ) {
            // Decorative background circles
            DecorativeBackground()

            if (summary == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section label
                    SectionLabel(text = "TODAY & THIS MONTH")

                    // Day + Month summary cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "TODAY",
                            emoji = "📅",
                            gradient = Brush.linearGradient(
                                colors = listOf(CardDayStart, CardDayEnd),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            ),
                            stats = summary.day
                        )
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "THIS MONTH",
                            emoji = "📊",
                            gradient = Brush.linearGradient(
                                colors = listOf(CardMonthStart, CardMonthEnd),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            ),
                            stats = summary.month
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    SectionLabel(text = "TOP PERFORMERS THIS MONTH")

                    // Top Salesman
                    TopPerformerCard(
                        icon = Icons.Default.EmojiEvents,
                        label = "Top Salesman",
                        performer = summary.topSalesman,
                        valueLabel = "Revenue",
                        gradient = Brush.linearGradient(listOf(TopSalesmanStart, TopSalesmanEnd)),
                        formatValue = { formatCurrency(it) }
                    )

                    // Top Outlet
                    TopPerformerCard(
                        icon = Icons.Default.Store,
                        label = "Top Outlet",
                        performer = summary.topOutlet,
                        valueLabel = "Revenue",
                        gradient = Brush.linearGradient(listOf(TopOutletStart, TopOutletEnd)),
                        formatValue = { formatCurrency(it) }
                    )

                    // Top Product
                    TopPerformerCard(
                        icon = Icons.Default.Inventory2,
                        label = "Top Product",
                        performer = summary.topProduct,
                        valueLabel = "Units Sold",
                        gradient = Brush.linearGradient(listOf(TopProductStart, TopProductEnd)),
                        formatValue = { it.toInt().toString() }
                    )

                    // Top Route
                    TopPerformerCard(
                        icon = Icons.Default.Route,
                        label = "Top Route",
                        performer = summary.topRoute,
                        valueLabel = "Revenue",
                        gradient = Brush.linearGradient(listOf(TopRouteStart, TopRouteEnd)),
                        formatValue = { formatCurrency(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DecorativeBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val circleColor = Color.White.copy(alpha = 0.04f)

        // Large decorative circles scattered across the background
        drawCircle(color = circleColor, radius = w * 0.55f, center = Offset(w * 0.85f, h * 0.12f))
        drawCircle(color = circleColor, radius = w * 0.4f, center = Offset(-w * 0.1f, h * 0.3f))
        drawCircle(color = circleColor, radius = w * 0.3f, center = Offset(w * 0.5f, h * 0.65f))
        drawCircle(color = circleColor, radius = w * 0.5f, center = Offset(w * 1.0f, h * 0.7f))
        drawCircle(color = Color.White.copy(alpha = 0.025f), radius = w * 0.2f, center = Offset(w * 0.2f, h * 0.85f))

        // Stars (small bright dots)
        val starColor = Color.White.copy(alpha = 0.15f)
        val starPositions = listOf(
            Offset(w * 0.15f, h * 0.08f), Offset(w * 0.72f, h * 0.05f),
            Offset(w * 0.40f, h * 0.15f), Offset(w * 0.88f, h * 0.22f),
            Offset(w * 0.05f, h * 0.42f), Offset(w * 0.60f, h * 0.38f),
            Offset(w * 0.30f, h * 0.55f), Offset(w * 0.78f, h * 0.50f),
            Offset(w * 0.18f, h * 0.70f), Offset(w * 0.55f, h * 0.75f),
            Offset(w * 0.90f, h * 0.88f), Offset(w * 0.35f, h * 0.92f)
        )
        starPositions.forEach { pos ->
            drawCircle(color = starColor, radius = 3.dp.toPx(), center = pos)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.6f),
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    emoji: String,
    gradient: Brush,
    stats: SummaryStats
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 0.5.dp)
            DashboardStatRow(label = "Orders", value = stats.orderCount.toString())
            DashboardStatRow(label = "Value", value = formatValue(stats.orderValue))
            DashboardStatRow(label = "Visits", value = stats.totalVisits.toString())
            DashboardStatRow(label = "Lines", value = stats.linesSold.toString())
        }
    }
}

@Composable
private fun DashboardStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun TopPerformerCard(
    icon: ImageVector,
    label: String,
    performer: TopPerformer?,
    valueLabel: String,
    gradient: Brush,
    formatValue: (Float) -> String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (performer != null) {
                    Text(
                        text = performer.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "$valueLabel: ${formatValue(performer.value)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                } else {
                    Text(
                        text = "No data this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun formatValue(value: Double): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
    value >= 1_000 -> "%.1fK".format(value / 1_000)
    else -> "%.0f".format(value)
}

private fun formatCurrency(value: Float): String = when {
    value >= 1_000_000f -> "%.1fM".format(value / 1_000_000f)
    value >= 1_000f -> "%.1fK".format(value / 1_000f)
    else -> "%.0f".format(value)
}
