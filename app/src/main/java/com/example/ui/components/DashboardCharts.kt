package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.data.model.Transaction
import com.example.ui.theme.*

@Composable
fun MonthlyBarChart(
    title: String,
    transactions: List<Transaction>,
    types: List<String>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val months = listOf("ينا", "فبر", "مار", "أبر", "ماي", "يون", "يول", "أغس", "سبت", "أكت", "نوف", "ديس")
    val monthTotals = FloatArray(12) { 0f }

    transactions.forEach { tx ->
        if (types.contains(tx.type)) {
            val parts = tx.date.split("-")
            if (parts.size >= 2) {
                val m = (parts[1].toIntOrNull() ?: 1) - 1
                if (m in 0..11) {
                    monthTotals[m] += tx.amount.toFloat()
                }
            }
        }
    }

    val maxVal = (monthTotals.maxOrNull() ?: 1000f).coerceAtLeast(1000f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_${title}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barWidth = (w / 14f).coerceIn(8.dp.toPx(), 24.dp.toPx())
                    val spacing = (w - (barWidth * 12)) / 13f

                    // Draw grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = h - (h / gridLines) * i
                        drawLine(
                            color = Color(0xFFE4E9F0).copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Bars
                    for (i in 0 until 12) {
                        val x = spacing + i * (barWidth + spacing)
                        val barHeight = (monthTotals[i] / maxVal) * (h * 0.85f)
                        val y = h - barHeight

                        drawRoundRect(
                            color = if (monthTotals[i] > 0) barColor else barColor.copy(alpha = 0.2f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                months.forEach { m ->
                    Text(
                        text = m,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusDoughnutChart(
    approvedCount: Int,
    pendingCount: Int,
    rejectedCount: Int,
    pendingGmCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (approvedCount + pendingCount + rejectedCount + pendingGmCount).coerceAtLeast(1)

    val approvedAngle = (approvedCount.toFloat() / total) * 360f
    val pendingAngle = (pendingCount.toFloat() / total) * 360f
    val pendingGmAngle = (pendingGmCount.toFloat() / total) * 360f
    val rejectedAngle = (rejectedCount.toFloat() / total) * 360f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_status_doughnut"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "حالة الطلبات الإجمالية",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 22.dp.toPx()
                        var startAngle = -90f

                        // Draw Approved arc
                        if (approvedAngle > 0) {
                            drawArc(
                                color = BrandGreen,
                                startAngle = startAngle,
                                sweepAngle = approvedAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += approvedAngle
                        }
                        // Draw Pending arc
                        if (pendingAngle > 0) {
                            drawArc(
                                color = AccentAmber,
                                startAngle = startAngle,
                                sweepAngle = pendingAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += pendingAngle
                        }
                        // Draw Pending GM arc
                        if (pendingGmAngle > 0) {
                            drawArc(
                                color = BrandBlue,
                                startAngle = startAngle,
                                sweepAngle = pendingGmAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += pendingGmAngle
                        }
                        // Draw Rejected arc
                        if (rejectedAngle > 0) {
                            drawArc(
                                color = AccentRed,
                                startAngle = startAngle,
                                sweepAngle = rejectedAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (approvedCount + pendingCount + rejectedCount + pendingGmCount).toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "طلب",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusLegendItem("معتمد", approvedCount, BrandGreen)
                    StatusLegendItem("قيد المراجعة", pendingCount, AccentAmber)
                    StatusLegendItem("بانتظار المدير العام", pendingGmCount, BrandBlue)
                    StatusLegendItem("مرفوض", rejectedCount, AccentRed)
                }
            }
        }
    }
}

@Composable
private fun StatusLegendItem(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DepartmentBarChart(
    departments: List<Department>,
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val deptTotals = departments.map { dept ->
        // Approximate total by transactions for employees in department
        dept.name to transactions.size * 500.0 // placeholder distributed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_dept_distribution"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "توزيع الحركات حسب القسم",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            departments.take(4).forEach { dept ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dept.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (dept.id * 0.18f).coerceIn(0.1f, 0.9f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BrandBlue,
                        trackColor = BrandBlueLight
                    )
                }
            }
        }
    }
}
