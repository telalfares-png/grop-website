package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.Employee
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*

@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    currentUser: Employee,
    onNavigateToRequests: () -> Unit = {},
    onNavigateToStatement: () -> Unit = {},
    onNavigateToMyRequests: () -> Unit = {}
) {
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الإشعارات والتنبيهات (${notifications.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (notifications.any { !it.isRead }) {
                TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعليم الكل كمقروء", color = BrandBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (notifications.isEmpty()) {
            EmptyStateCard(
                title = "لا توجد إشعارات",
                message = "صندوق إشعاراتك فارغ حاليًا. ستظهر هنا تنبيهات التعميدات وطلبات السلف والعمليات المالية فور حدوثها.",
                icon = Icons.Default.NotificationsNone
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    val isUnread = !notif.isRead
                    val isAdvanceNotice = notif.kind == "cash" || notif.title.contains("سلفة") || notif.title.contains("تعميد")
                    val isManager = currentUser.role == "gm" || currentUser.role == "supervisor"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.markNotificationAsRead(notif.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnread) Color(0xFFFEF3C7).copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 3.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isAdvanceNotice) Color(0xFFD97706)
                                            else if (notif.kind == "check") BrandGreen
                                            else if (notif.kind == "alert") AccentRed
                                            else BrandBlue
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAdvanceNotice) Icons.Default.AccountBalanceWallet
                                        else if (notif.kind == "check") Icons.Default.CheckCircle
                                        else if (notif.kind == "alert") Icons.Default.RemoveCircle
                                        else Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = notif.time,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Actionable Buttons directly from notification
                                    if (isAdvanceNotice && isManager && notif.body.contains("يتطلب تعميدك") || notif.title.contains("تنبيه تعميد")) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                viewModel.markNotificationAsRead(notif.id)
                                                onNavigateToRequests()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("اتخاذ إجراء التعميد أو الرفض ✍️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (currentUser.role == "employee" && notif.title.contains("تعميد")) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.markNotificationAsRead(notif.id)
                                                    onNavigateToStatement()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("كشف الحساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.markNotificationAsRead(notif.id)
                                                    onNavigateToMyRequests()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("متابعة طلباتي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
