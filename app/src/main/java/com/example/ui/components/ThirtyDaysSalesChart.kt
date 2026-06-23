package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DailySalesDataPoint
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ThirtyDaysSalesChart(
    dataPoints: List<DailySalesDataPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var viewModeTable by remember { mutableStateOf(false) }

    if (dataPoints.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Generating 30 Days Sales Records...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
        return
    }

    // Key statistics calculations
    val totalRevenue = remember(dataPoints) { dataPoints.sumOf { it.revenue } }
    val maxRevenue = remember(dataPoints) { dataPoints.maxOf { it.revenue }.coerceAtLeast(100.0) }
    val avgRevenue = remember(dataPoints) { totalRevenue / dataPoints.size }
    val formattedTotal = remember(totalRevenue) { formatCurrency(totalRevenue) }
    val formattedAvg = remember(avgRevenue) { formatCurrency(avgRevenue) }
    val formattedMax = remember(maxRevenue) { formatCurrency(maxRevenue) }

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(dataPoints) {
        animationTriggered = true
    }

    val animProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "chart_line_animation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("thirty_days_sales_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "30 Days Revenue Trend",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "روزانہ بکری کا رجحان (آخری 30 دن)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tab/View Mode switches to support both graphical trends & interactive tabular lists
                IconButton(
                    onClick = { viewModeTable = !viewModeTable },
                    modifier = Modifier.testTag("toggle_chart_format")
                ) {
                    Icon(
                        imageVector = if (viewModeTable) Icons.Default.ShowChart else Icons.Default.TableChart,
                        contentDescription = "Toggle view",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Premium Key Stats Panel (Recharts Dashboard style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Total Revenue (کل بکری)",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rs. $formattedTotal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Daily Avg (روزانہ اوسط)",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rs. $formattedAvg",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Peak Sale (زیادہ سے زیادہ)",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rs. $formattedMax",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Interactive Line Graph or Tabular list
            if (viewModeTable) {
                // Table View
                Text(
                    text = "Daily Sales Record Table:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(dataPoints.reversed()) { index, day ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Rs. ${formatCurrency(day.revenue)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.revenue > 0) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }
            } else {
                // Graphical Interactive Grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp, start = 32.dp, end = 8.dp)
                            .pointerInput(dataPoints) {
                                // Handle tap gesture selection
                                detectTapGestures { offset ->
                                    val sectionWidth = size.width / (dataPoints.size - 1)
                                    val approxIndex = (offset.x / sectionWidth).roundToInt()
                                    selectedIndex = approxIndex.coerceIn(0, dataPoints.size - 1)
                                }
                            }
                            .pointerInput(dataPoints) {
                                // Handle drag hover tracking (just like Recharts cross-hair)
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val sectionWidth = size.width / (dataPoints.size - 1)
                                        val approxIndex = (offset.x / sectionWidth).roundToInt()
                                        selectedIndex = approxIndex.coerceIn(0, dataPoints.size - 1)
                                    },
                                    onDragEnd = { selectedIndex = null },
                                    onDragCancel = { selectedIndex = null },
                                    onDrag = { change, _ ->
                                        val sectionWidth = size.width / (dataPoints.size - 1)
                                        val approxIndex = (change.position.x / sectionWidth).roundToInt()
                                        selectedIndex = approxIndex.coerceIn(0, dataPoints.size - 1)
                                    }
                                )
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val spacing = width / (dataPoints.size - 1)

                        // 1. Draw Horizontal Grid lines
                        val verticalSlices = 4
                        for (i in 0..verticalSlices) {
                            val lineY = height * i / verticalSlices
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, lineY),
                                end = Offset(width, lineY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Helper lambdas to get coordinates
                        fun getX(index: Int): Float = index * spacing
                        fun getY(value: Double): Float {
                            val ratio = value / maxRevenue
                            return (height - (ratio * height * animProgress)).toFloat()
                        }

                        // 2. Compute smooth path & area points
                        val path = Path()
                        val fillPath = Path()

                        if (dataPoints.isNotEmpty()) {
                            val startX = getX(0)
                            val startY = getY(dataPoints[0].revenue)
                            path.moveTo(startX, startY)
                            fillPath.moveTo(startX, height)
                            fillPath.lineTo(startX, startY)

                            for (i in 1 until dataPoints.size) {
                                val prevX = getX(i - 1)
                                val prevY = getY(dataPoints[i - 1].revenue)
                                val currX = getX(i)
                                val currY = getY(dataPoints[i].revenue)

                                // Dynamic control points for beautiful smooth bezier interpolation
                                val ctrlX1 = prevX + (currX - prevX) / 2f
                                val ctrlY1 = prevY
                                val ctrlX2 = prevX + (currX - prevX) / 2f
                                val ctrlY2 = currY

                                path.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, currX, currY)
                                fillPath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, currX, currY)
                            }

                            fillPath.lineTo(getX(dataPoints.size - 1), height)
                            fillPath.close()

                            // 3. Draw gradient background fill under the spline curve
                            val areaBrush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.01f)
                                )
                            )
                            drawPath(path = fillPath, brush = areaBrush)

                            // 4. Draw smooth outer line
                            drawPath(
                                path = path,
                                color = primaryColor,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }

                        // 5. Draw interactive tracking lines/dots on tap/drag (Recharts cross-hair style)
                        selectedIndex?.let { activeIdx ->
                            if (activeIdx in dataPoints.indices) {
                                val activeX = getX(activeIdx)
                                val activeY = getY(dataPoints[activeIdx].revenue)

                                // Draw vertical helper grid line
                                drawLine(
                                    color = primaryColor.copy(alpha = 0.25f),
                                    start = Offset(activeX, 0f),
                                    end = Offset(activeX, height),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                )

                                // Draw interactive outer glowing pointer circle
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.25f),
                                    radius = 10.dp.toPx(),
                                    center = Offset(activeX, activeY)
                                )

                                // Draw interactive inner solid pointer dot
                                drawCircle(
                                    color = primaryColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(activeX, activeY)
                                )
                            }
                        }
                    }

                    // Overlaid Y-Axis values indicators
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(32.dp)
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(text = formatAxisPrice(maxRevenue), fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.Bold)
                        Text(text = formatAxisPrice(maxRevenue * 0.75), fontSize = 10.sp, color = labelColor)
                        Text(text = formatAxisPrice(maxRevenue * 0.5), fontSize = 10.sp, color = labelColor)
                        Text(text = formatAxisPrice(maxRevenue * 0.25), fontSize = 10.sp, color = labelColor)
                        Text(text = "0", fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.Bold)
                    }

                    // Overlaid dynamic Tooltip Card when hovering/touching
                    val activePoint = selectedIndex?.let { if (it in dataPoints.indices) dataPoints[it] else null }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = activePoint != null,
                        enter = fadeIn(animationSpec = tween(150)),
                        exit = fadeOut(animationSpec = tween(150)),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        activePoint?.let { point ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .widthIn(max = 180.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = point.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.inverseOnSurface
                                    )
                                    Text(
                                        text = "Rs. ${formatCurrency(point.revenue)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.inversePrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // X-Axis labels displaying (intelligent 5 days spacing decanting to avoid overlap)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = 4.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelPoints = remember(dataPoints) {
                        listOf(
                            dataPoints.getOrNull(0)?.label ?: "",
                            dataPoints.getOrNull(7)?.label ?: "",
                            dataPoints.getOrNull(14)?.label ?: "",
                            dataPoints.getOrNull(21)?.label ?: "",
                            dataPoints.getOrNull(29)?.label ?: ""
                        )
                    }
                    labelPoints.forEach { label ->
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Help info footer line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tip: Press or drag along the line to inspect daily details interactively.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return try {
        val format = NumberFormat.getNumberInstance(Locale.getDefault())
        format.maximumFractionDigits = 0
        format.format(amount)
    } catch (e: Exception) {
        amount.toInt().toString()
    }
}

private fun formatAxisPrice(value: Double): String {
    return when {
        value >= 1_000_000 -> "${String.format("%.1f", value / 1_000_000.0)}M"
        value >= 1_000 -> "${String.format("%.0f", value / 1_000.0)}k"
        else -> String.format("%.0f", value)
    }
}

// Float round to Int helper compile patch for older Kotlin variants
private fun Float.roundToInt(): Int = (this + 0.5f).toInt()
