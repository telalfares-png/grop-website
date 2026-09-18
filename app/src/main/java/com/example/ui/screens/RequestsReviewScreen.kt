package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.EmployeeRepository
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun RequestsReviewScreen(
    viewModel: MainViewModel,
    currentUser: Employee
) {
    val employees by viewModel.allEmployees.collectAsState()
    val advances by viewModel.advances.collectAsState()
    val penalties by viewModel.penalties.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Advances, 1: Penalties
    var statusFilter by remember { mutableStateOf("all") } // "all", "pending", "approved", "rejected"

    val mySubIds = remember(employees, currentUser) {
        if (currentUser.role == "gm") employees.map { it.id }
        else employees.filter { it.supId == currentUser.id }.map { it.id }
    }

    val scopedAdvances = remember(advances, mySubIds, statusFilter) {
        advances.filter { mySubIds.contains(it.empId) }
            .filter { if (statusFilter == "all") true else it.status == statusFilter }
    }

    val scopedPenalties = remember(penalties, mySubIds, statusFilter) {
        penalties.filter { mySubIds.contains(it.empId) }
            .filter {
                if (statusFilter == "all") true
                else if (statusFilter == "pending") it.status == "pending" || it.status == "pending_gm"
                else it.status == statusFilter
            }
    }

    var selectedAdvanceForAction by remember { mutableStateOf<AdvanceRequest?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "approve" or "reject"
    var actionNote by remember { mutableStateOf("") }

    var selectedPenaltyForAction by remember { mutableStateOf<Penalty?>(null) }
    var penaltyActionType by remember { mutableStateOf<String?>(null) } // "approve" or "reject"
    var penaltyActionNote by remember { mutableStateOf("") }

    var itemDetailDialog by remember { mutableStateOf<Any?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BrandGreenDark,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("طلبات السلف (${scopedAdvances.size})", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("الخصومات والجزاءات (${scopedPenalties.size})", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == "all",
                onClick = { statusFilter = "all" },
                label = { Text("الكل") }
            )
            FilterChip(
                selected = statusFilter == "pending",
                onClick = { statusFilter = "pending" },
                label = { Text("قيد المراجعة") }
            )
            FilterChip(
                selected = statusFilter == "approved",
                onClick = { statusFilter = "approved" },
                label = { Text("معتمد") }
            )
            FilterChip(
                selected = statusFilter == "rejected",
                onClick = { statusFilter = "rejected" },
                label = { Text("مرفوض") }
            )
        }

        // List Content
        if (selectedTab == 0) {
            if (scopedAdvances.isEmpty()) {
                EmptyStateCard(
                    title = "لا توجد طلبات سلف",
                    message = "لا توجد طلبات مطابقة للفلتر المحدد",
                    icon = Icons.Default.AccountBalanceWallet
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(scopedAdvances, key = { it.id }) { adv ->
                        val emp = employees.find { it.id == adv.empId }
                        AdvanceCardItem(
                            advance = adv,
                            employee = emp,
                            currency = settings.currency,
                            onViewDetail = { itemDetailDialog = adv },
                            onApprove = {
                                selectedAdvanceForAction = adv
                                actionType = "approve"
                                actionNote = ""
                            },
                            onReject = {
                                selectedAdvanceForAction = adv
                                actionType = "reject"
                                actionNote = ""
                            }
                        )
                    }
                }
            }
        } else {
            if (scopedPenalties.isEmpty()) {
                EmptyStateCard(
                    title = "لا توجد خصومات أو جزاءات",
                    message = "لا توجد سجلات مطابقة للفلتر المحدد",
                    icon = Icons.Default.RemoveCircle
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(scopedPenalties, key = { it.id }) { pen ->
                        val emp = employees.find { it.id == pen.empId }
                        PenaltyCardItem(
                            penalty = pen,
                            employee = emp,
                            currency = settings.currency,
                            canApprove = currentUser.role == "gm" || !settings.penaltyNeedsGM,
                            onViewDetail = { itemDetailDialog = pen },
                            onApprove = {
                                selectedPenaltyForAction = pen
                                penaltyActionType = "approve"
                                penaltyActionNote = ""
                            },
                            onReject = {
                                selectedPenaltyForAction = pen
                                penaltyActionType = "reject"
                                penaltyActionNote = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Advance Action Dialog (Approve / Reject)
    if (selectedAdvanceForAction != null && actionType != null) {
        val adv = selectedAdvanceForAction!!
        val isApprove = actionType == "approve"
        val emp = employees.find { it.id == adv.empId }

        AlertDialog(
            onDismissRequest = {
                selectedAdvanceForAction = null
                actionType = null
            },
            title = {
                Text(
                    text = if (isApprove) "تعميد السلفة واعتمادها (${adv.reqNo}) ✅" else "رفض طلب السلفة (${adv.reqNo}) ❌",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isApprove)
                            "سيتم تعميد سلفة بمبلغ ${EmployeeRepository.formatAmount(adv.amount)} ${settings.currency} للموظف ${emp?.name ?: ""} وقيدها فوراً في كشف حسابه المالي وإشعاره بالقرار والتعميد رسمياً."
                        else "سيتم رفض طلب السلفة وإشعار الموظف بسبب الرفض.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = actionNote,
                        onValueChange = { actionNote = it },
                        label = { Text("ملاحظة التعميد / سبب القرار (اختياري)") },
                        placeholder = { Text("تُحفظ في سجل العمليات وكشف الحساب وتُرسل للموظف...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isApprove) {
                            viewModel.approveAdvance(adv.id, actionNote)
                        } else {
                            viewModel.rejectAdvance(adv.id, actionNote)
                        }
                        selectedAdvanceForAction = null
                        actionType = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApprove) BrandGreen else AccentRed
                    )
                ) {
                    Text(if (isApprove) "تأكيد التعميد والقيد ✅" else "تأكيد الرفض ❌", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedAdvanceForAction = null
                    actionType = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Penalty Action Dialog (Approve / Reject)
    if (selectedPenaltyForAction != null && penaltyActionType != null) {
        val pen = selectedPenaltyForAction!!
        val isApprove = penaltyActionType == "approve"
        val emp = employees.find { it.id == pen.empId }

        AlertDialog(
            onDismissRequest = {
                selectedPenaltyForAction = null
                penaltyActionType = null
            },
            title = {
                Text(
                    text = if (isApprove) "اعتماد الخصم (${pen.reqNo})" else "رفض الخصم (${pen.reqNo})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isApprove)
                            "سيتم اعتماد خصم بمبلغ ${EmployeeRepository.formatAmount(pen.amount)} ${settings.currency} على الموظف ${emp?.name ?: ""} وقيده في كشف حسابه."
                        else "سيتم إلغاء الخصم ولن يُقيد في كشف حساب الموظف.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = penaltyActionNote,
                        onValueChange = { penaltyActionNote = it },
                        label = { Text("ملاحظة المدير العام (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isApprove) {
                            viewModel.approvePenalty(pen.id, penaltyActionNote)
                        } else {
                            viewModel.rejectPenalty(pen.id, penaltyActionNote)
                        }
                        selectedPenaltyForAction = null
                        penaltyActionType = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApprove) BrandGreen else AccentRed
                    )
                ) {
                    Text(if (isApprove) "اعتماد نهائي" else "رفض الخصم", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedPenaltyForAction = null
                    penaltyActionType = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Item Detail View Dialog
    if (itemDetailDialog != null) {
        val item = itemDetailDialog
        AlertDialog(
            onDismissRequest = { itemDetailDialog = null },
            title = {
                Text(
                    text = if (item is AdvanceRequest) "تفاصيل طلب السلفة ${item.reqNo}" else if (item is Penalty) "تفاصيل الخصم ${item.reqNo}" else "تفاصيل",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item is AdvanceRequest) {
                        val emp = employees.find { it.id == item.empId }
                        DetailRow("الموظف:", emp?.name ?: "—")
                        DetailRow("المبلغ:", "${EmployeeRepository.formatAmount(item.amount)} ${settings.currency}")
                        DetailRow("التاريخ:", item.date)
                        DetailRow("الحالة:", item.status)
                        DetailRow("السبب:", item.reason)
                        if (item.note.isNotBlank()) DetailRow("ملاحظات الموظف:", item.note)
                        if (item.supNote.isNotBlank()) DetailRow("ملاحظات المشرف:", item.supNote)
                    } else if (item is Penalty) {
                        val emp = employees.find { it.id == item.empId }
                        DetailRow("الموظف:", emp?.name ?: "—")
                        DetailRow("نوع الجزاء:", item.type)
                        DetailRow("المبلغ:", "${EmployeeRepository.formatAmount(item.amount)} ${settings.currency}")
                        DetailRow("التاريخ:", item.date)
                        DetailRow("الحالة:", item.status)
                        DetailRow("السبب:", item.reason)
                        if (item.note.isNotBlank()) DetailRow("ملاحظات:", item.note)
                        if (item.supNote.isNotBlank()) DetailRow("ملاحظات المشرف/المدير:", item.supNote)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { itemDetailDialog = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AdvanceCardItem(
    advance: AdvanceRequest,
    employee: Employee?,
    currency: String,
    onViewDetail: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarView(employee = employee, size = 36)
                    Column {
                        Text(
                            text = employee?.name ?: "موظف",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${advance.reqNo} • ${advance.date}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(status = advance.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = advance.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${EmployeeRepository.formatAmount(advance.amount)} $currency",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = BrandGreenDark
                )
            }

            if (advance.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onViewDetail,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("تفاصيل", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1.1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعميد السلفة", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        modifier = Modifier.weight(0.9f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رفض", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PenaltyCardItem(
    penalty: Penalty,
    employee: Employee?,
    currency: String,
    canApprove: Boolean,
    onViewDetail: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarView(employee = employee, size = 36)
                    Column {
                        Text(
                            text = employee?.name ?: "موظف",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${penalty.reqNo} • ${penalty.date}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TypeBadge(type = penalty.type)
                    StatusBadge(status = penalty.status)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = penalty.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "- ${EmployeeRepository.formatAmount(penalty.amount)} $currency",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = AccentRed
                )
            }

            if (penalty.status == "pending" || penalty.status == "pending_gm") {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onViewDetail,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("تفاصيل", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = onApprove,
                        enabled = canApprove,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اعتماد", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReject,
                        enabled = canApprove,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رفض", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
