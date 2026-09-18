package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.EmployeeRepository
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.PdfPrintHelper

@Composable
fun EmployeeDetailScreen(
    empId: Long,
    viewModel: MainViewModel,
    currentUser: Employee,
    onNavigateBack: () -> Unit
) {
    val employeeFlow = remember(empId) { viewModel.repository.getEmployeeById(empId) }
    val employee by employeeFlow.collectAsState(initial = null)

    val departments by viewModel.departments.collectAsState()
    val allEmployees by viewModel.allEmployees.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val advancesFlow = remember(empId) { viewModel.repository.getAdvancesForEmployee(empId) }
    val advances by advancesFlow.collectAsState(initial = emptyList())

    val penaltiesFlow = remember(empId) { viewModel.repository.getPenaltiesForEmployee(empId) }
    val penalties by penaltiesFlow.collectAsState(initial = emptyList())

    val statsFlow = remember(empId) { viewModel.repository.getEmployeeFinanceStats(empId) }
    val stats by statsFlow.collectAsState(initial = EmployeeFinanceStats())

    val auditLogs by viewModel.auditLogs.collectAsState()
    val empLogs = remember(auditLogs, employee) {
        if (employee == null) emptyList()
        else auditLogs.filter { it.details.contains(employee!!.name) || it.details.contains(employee!!.no) }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("البيانات", "السلف", "الخصومات", "كشف الحساب", "التقارير", "سجل النشاط")

    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var showEditAccountDialog by remember { mutableStateOf(false) }

    if (employee == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandGreen)
        }
        return
    }

    val emp = employee!!
    val dept = departments.find { it.id == emp.deptId }
    val sup = allEmployees.find { it.id == emp.supId }
    val isGm = currentUser.role == "gm"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AvatarView(employee = emp, size = 52)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = emp.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusBadge(status = emp.status)
                        }
                        Text(
                            text = "${emp.job} • الرقم: ${emp.no} • ${dept?.name ?: "—"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "اسم الدخول: ${emp.username}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandGreenDark,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isGm) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalIconButton(
                                onClick = { showPasswordResetDialog = true },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFFFEF3C7),
                                    contentColor = Color(0xFFB45309)
                                )
                            ) {
                                Icon(Icons.Outlined.Key, contentDescription = "تغيير الرقم السري", modifier = Modifier.size(20.dp))
                            }

                            FilledTonalIconButton(
                                onClick = { showEditAccountDialog = true },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل الحساب", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                // Finance metrics strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricMiniBox("السلف", "${EmployeeRepository.formatAmount(stats.totalAdvances)}", BrandGreenDark, Modifier.weight(1f))
                    MetricMiniBox("المسدد", "${EmployeeRepository.formatAmount(stats.totalRepaid)}", BrandBlueDark, Modifier.weight(1f))
                    MetricMiniBox("المتبقي", "${EmployeeRepository.formatAmount(stats.remainingBalance)}", AccentAmber, Modifier.weight(1f))
                    MetricMiniBox("الخصومات", "${EmployeeRepository.formatAmount(stats.totalDeductions)}", AccentRed, Modifier.weight(1f))
                }
            }
        }

        // Scrollable Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BrandGreenDark,
            edgePadding = 8.dp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> PersonalInfoTab(
                employee = emp,
                deptName = dept?.name ?: "—",
                supName = sup?.name ?: "—",
                isGm = isGm,
                onEditAccount = { showEditAccountDialog = true },
                onResetPassword = { showPasswordResetDialog = true }
            )
            1 -> AdvancesTab(advances, settings.currency)
            2 -> PenaltiesTab(penalties, settings.currency)
            3 -> StatementScreen(viewModel, currentUser, initialEmpId = emp.id)
            4 -> ReportsTab(emp, dept?.name ?: "القسم العام", stats, advances, penalties, settings)
            5 -> ActivityLogTab(empLogs)
        }
    }

    if (showPasswordResetDialog) {
        QuickResetPasswordDialog(
            employee = emp,
            onDismiss = { showPasswordResetDialog = false },
            onConfirm = { newPass ->
                viewModel.changePassword(emp.id, newPass) {
                    showPasswordResetDialog = false
                }
            }
        )
    }

    if (showEditAccountDialog) {
        EditAccountDetailsDialog(
            employee = emp,
            departments = departments,
            supervisors = allEmployees.filter { it.role == "supervisor" || it.role == "gm" },
            onDismiss = { showEditAccountDialog = false },
            onSave = { updatedEmp, newPass ->
                viewModel.updateEmployeeAccount(
                    empId = emp.id,
                    name = updatedEmp.name,
                    no = updatedEmp.no,
                    mobile = updatedEmp.mobile,
                    email = updatedEmp.email,
                    job = updatedEmp.job,
                    deptId = updatedEmp.deptId,
                    role = updatedEmp.role,
                    supId = updatedEmp.supId,
                    username = updatedEmp.username,
                    newPasswordRaw = newPass.ifBlank { null },
                    status = updatedEmp.status
                ) {
                    showEditAccountDialog = false
                }
            }
        )
    }
}

@Composable
private fun MetricMiniBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

@Composable
private fun PersonalInfoTab(
    employee: Employee,
    deptName: String,
    supName: String,
    isGm: Boolean = false,
    onEditAccount: () -> Unit = {},
    onResetPassword: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("البيانات الوظيفية والشخصية", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                if (isGm) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = onResetPassword,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFFEF3C7),
                                contentColor = Color(0xFFB45309)
                            )
                        ) {
                            Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تغيير الرقم السري", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onEditAccount,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعديل الحساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            HorizontalDivider()
            InfoPair("الاسم الكامل", employee.name)
            InfoPair("الرقم الوظيفي", employee.no)
            InfoPair("القسم", deptName)
            InfoPair("المسمى الوظيفي", employee.job)
            InfoPair("رقم الجوال", employee.mobile)
            InfoPair("البريد الإلكتروني", employee.email)
            InfoPair("تاريخ التعيين", employee.hireDate)
            InfoPair("المشرف المباشر", supName)
            InfoPair("نوع الحساب / الدور", employee.role)
            InfoPair("اسم المستخدم للدخول", employee.username)
            InfoPair("آخر تسجيل دخول", employee.lastLogin ?: "لم يسجل بعد")
        }
    }
}

@Composable
private fun InfoPair(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AdvancesTab(advances: List<AdvanceRequest>, currency: String) {
    if (advances.isEmpty()) {
        EmptyStateCard(title = "لا توجد سلف", message = "لم يقم الموظف بطلب أي سلف سابقة", icon = Icons.Default.AccountBalanceWallet)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(advances, key = { it.id }) { adv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "${adv.reqNo} • ${adv.date}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            StatusBadge(status = adv.status)
                        }
                        Text(text = adv.reason, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "${EmployeeRepository.formatAmount(adv.amount)} $currency", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = BrandGreenDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun PenaltiesTab(penalties: List<Penalty>, currency: String) {
    if (penalties.isEmpty()) {
        EmptyStateCard(title = "سجل نظيف", message = "لا توجد خصومات أو جزاءات مسجلة على الموظف", icon = Icons.Default.CheckCircle)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(penalties, key = { it.id }) { pen ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TypeBadge(type = pen.type)
                                StatusBadge(status = pen.status)
                            }
                            Text(text = pen.date, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Text(text = pen.reason, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "- ${EmployeeRepository.formatAmount(pen.amount)} $currency", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = AccentRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsTab(
    employee: Employee,
    departmentName: String,
    stats: EmployeeFinanceStats,
    advances: List<AdvanceRequest>,
    penalties: List<Penalty>,
    settings: AppSettings
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("إصدار التقارير الرسمية للموظف", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("يمكنك استخراج وثيقة PDF شاملة لكشف الحساب أو التقرير الشامل لملف الموظف مع سجل السلف والخصومات.", style = MaterialTheme.typography.bodySmall, color = TextMuted)

            Button(
                onClick = {
                    PdfPrintHelper.printEmployeeProfile(
                        context = context,
                        employee = employee,
                        departmentName = departmentName,
                        stats = stats,
                        advances = advances,
                        penalties = penalties,
                        settings = settings
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("طباعة / حفظ التقرير الشامل PDF", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActivityLogTab(logs: List<AuditLog>) {
    if (logs.isEmpty()) {
        EmptyStateCard(title = "لا يوجد نشاط مسجل", message = "لا توجد أحداث تدقيق خاصة بهذا الموظف", icon = Icons.Default.History)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.action, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenDark)
                            Text(log.time, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Text(log.details, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
