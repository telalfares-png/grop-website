package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.ui.theme.*

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    accentLightColor: Color,
    modifier: Modifier = Modifier,
    subText: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stat_card_${title}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Colored left border strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentLightColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (subText != null) {
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (status) {
        "approved" -> Triple("معمّد ومعتمد ✅", BrandGreenLight, BrandGreenDark)
        "pending" -> Triple("بانتظار التعميد ⏳", AccentAmberLight, AccentAmber)
        "rejected" -> Triple("مرفوض ❌", AccentRedLight, AccentRed)
        "pending_gm" -> Triple("بانتظار تعميد المدير العام ⏳", BrandBlueLight, BrandBlueDark)
        "active" -> Triple("نشط", BrandGreenLight, BrandGreenDark)
        "inactive" -> Triple("معطل", AccentRedLight, AccentRed)
        "deleted" -> Triple("محذوف", Color(0xFFEEEFF2), Color(0xFF6B7280))
        else -> Triple(status, Color(0xFFEEEFF2), Color(0xFF6B7280))
    }

    Surface(
        modifier = modifier.testTag("status_badge_$status"),
        shape = RoundedCornerShape(99.dp),
        color = bg
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (role) {
        "gm" -> Triple("مدير عام", Color(0xFFFEF3C7), Color(0xFFB45309))
        "supervisor" -> Triple("مشرف", BrandBlueLight, BrandBlueDark)
        else -> Triple("موظف", Color(0xFFE0E7FF), Color(0xFF3730A3))
    }

    Surface(
        modifier = modifier.testTag("role_badge_$role"),
        shape = RoundedCornerShape(99.dp),
        color = bg
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TypeBadge(type: String, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (type) {
        "advance" -> Triple("سلفة", BrandGreenLight, BrandGreenDark)
        "repayment" -> Triple("سداد سلفة", BrandBlueLight, BrandBlueDark)
        "deduction" -> Triple("خصم", AccentRedLight, AccentRed)
        "penalty" -> Triple("جزاء", AccentRedLight, AccentRed)
        "absence" -> Triple("غياب", AccentRedLight, AccentRed)
        "lateness" -> Triple("تأخر", AccentAmberLight, AccentAmber)
        "violation" -> Triple("مخالفة", AccentRedLight, AccentRed)
        else -> Triple(type, Color(0xFFEEEFF2), Color(0xFF6B7280))
    }

    Surface(
        modifier = modifier.testTag("type_badge_$type"),
        shape = RoundedCornerShape(99.dp),
        color = bg
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun AvatarView(
    employee: Employee?,
    modifier: Modifier = Modifier,
    size: Int = 42
) {
    val initials = remember(employee) {
        val name = employee?.name.orEmpty().trim()
        val parts = name.split(" ").filter { it.length > 1 && !it.startsWith("بن") }
        if (parts.size >= 2) "${parts[0].first()}${parts[1].first()}"
        else if (parts.isNotEmpty()) "${parts[0].first()}"
        else "م"
    }

    val gradientBrush = remember(employee?.id) {
        val id = (employee?.id ?: 1).toInt()
        val gradients = listOf(
            listOf(BrandGreen, BrandBlue),
            listOf(AccentTeal, BrandGreen),
            listOf(BrandBlue, Color(0xFF7C3AED)),
            listOf(AccentAmber, AccentRed),
            listOf(BrandGreenDark, AccentTeal)
        )
        val selected = gradients[id % gradients.size]
        Brush.linearGradient(selected)
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3.2).dp))
            .background(gradientBrush)
            .testTag("avatar_${employee?.id ?: 0}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.4).sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.FolderOpen,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
