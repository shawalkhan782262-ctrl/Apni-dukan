package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ProfitDataPoint

@Composable
fun ProfitLossChart(
    dataPoints: List<ProfitDataPoint>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Report data generating...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    val maxProfit = dataPoints.maxOf { it.profit }.coerceAtLeast(100.0)
    val maxSales = dataPoints.maxOf { it.sales }.coerceAtLeast(100.0)
    val maxValue = maxOf(maxProfit, maxSales)

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(dataPoints) {
        animationTriggered = true
    }

    val animProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "chart_animation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bikri aur Munafa Report (last 7 days)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendIndicator(label = "Bikri (Sales)", color = MaterialTheme.colorScheme.primary)
                LegendIndicator(label = "Munafa (Profit)", color = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            val primaryColor = MaterialTheme.colorScheme.primary
            val profitColor = Color(0xFF388E3C)
            val profitGradient = Brush.verticalGradient(
                colors = listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
            )
            val salesGradient = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.8f), primaryColor)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp, start = 24.dp)
            ) {
                val width = size.width
                val height = size.height
                val barWidth = (width / (dataPoints.size * 2)) * 0.7f
                val spacing = width / dataPoints.size

                // Draw Horizontal Guidelines (Grid lines)
                val gridSegments = 4
                for (i in 0..gridSegments) {
                    val y = height * i / gridSegments
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Draw Bars
                dataPoints.forEachIndexed { index, point ->
                    val x = spacing * index + (spacing / 2)

                    // Calculate Sales bar height (back, wider)
                    val salesHeight = (point.sales / maxValue) * height * animProgress
                    val salesTop = height - salesHeight
                    val salesLeft = x - barWidth

                    if (point.sales > 0) {
                        drawRoundRect(
                            brush = salesGradient,
                            topLeft = Offset(salesLeft, salesTop.toFloat()),
                            size = Size(barWidth, salesHeight.toFloat()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Calculate Profit bar height (front, thinner)
                    val profitHeight = (point.profit / maxValue) * height * animProgress
                    val profitTop = height - profitHeight
                    val profitLeft = x

                    if (point.profit > 0) {
                        drawRoundRect(
                            brush = profitGradient,
                            topLeft = Offset(profitLeft, profitTop.toFloat()),
                            size = Size(barWidth, profitHeight.toFloat()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    } else if (point.profit < 0) {
                        // Drawing negative profit (loss) below axis, but standard canvas height is limited.
                        // Let's show as a clear indicator bar with red color for visual feedback.
                        val absProfitHeight = (kotlin.math.abs(point.profit) / maxValue) * height * animProgress
                        val lossGradient = Brush.verticalGradient(
                            colors = listOf(Color(0xFFEF5350), Color(0xFFC62828))
                        )
                        drawRoundRect(
                            brush = lossGradient,
                            topLeft = Offset(profitLeft, (height - absProfitHeight).toFloat()),
                            size = Size(barWidth, absProfitHeight.toFloat()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // Labels overlaid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dataPoints.forEach { point ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = point.label,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendIndicator(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
