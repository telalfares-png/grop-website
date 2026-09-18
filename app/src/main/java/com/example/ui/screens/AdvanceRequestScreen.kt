package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.data.repository.EmployeeRepository
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun AdvanceRequestScreen(
    viewModel: MainViewModel,
    currentUser: Employee,
    onSuccess: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val isAmountValid = amount in 100.0..settings.advanceMax

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تقديم طلب سلفة جديدة",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Employee Info Summary
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF4F8FB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مقدم الطلب: ${currentUser.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الرقم الوظيفي: ${currentUser.no} • ${currentUser.job}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandGreenLight
                        ) {
                            Text(
                                text = "تاريخ اليوم: ${EmployeeRepository.getCurrentDateString()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandGreenDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ السلفة المطلوب (ريال) *") },
                    placeholder = { Text("مثال: 3000") },
                    leadingIcon = {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = BrandGreen)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amountStr.isNotEmpty() && !isAmountValid,
                    supportingText = {
                        Text(
                            text = "الحد الأقصى المسموح للطلب: ${EmployeeRepository.formatAmount(settings.advanceMax)} ${settings.currency}",
                            color = if (amountStr.isNotEmpty() && !isAmountValid) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Reason Field
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب طلب السلفة *") },
                    placeholder = { Text("مثال: ظرف عائلي طارئ / صيانة سيارة...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, tint = BrandGreen)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_reason_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات إضافية (اختياري)") },
                    placeholder = { Text("أي تفاصيل إضافية تود إرفاقها...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Info Notice Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandBlueLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BrandBlueDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "سيتم إرسال الطلب آليًا إلى مشرفك المباشر والإدارة للمراجعة، وستصلك إشعارات فورية بنتائج الاعتماد وقيده في كشف حسابك.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = BrandBlueDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (isAmountValid && reason.isNotBlank()) {
                            isSubmitting = true
                            viewModel.submitAdvanceRequest(amount, reason, note) {
                                isSubmitting = false
                                onSuccess()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_advance_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    enabled = !isSubmitting && isAmountValid && reason.isNotBlank()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Text(
                                text = "إرسال طلب السلفة",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
