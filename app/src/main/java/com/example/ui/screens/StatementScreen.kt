package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.EmployeeRepository
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.PdfPrintHelper
import com.example.util.findActivity
import android.app.Activity
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(
    viewModel: MainViewModel,
    currentUser: Employee,
    initialEmpId: Long? = null
) {
    val context = LocalContext.current
    val employees by viewModel.allEmployees.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val availableEmployees = remember(employees, currentUser) {
        if (currentUser.role == "gm") employees
        else if (currentUser.role == "supervisor") {
            val subs = employees.filter { it.supId == currentUser.id }
            listOf(currentUser) + subs
        } else listOf(currentUser)
    }

    var selectedEmpId by remember(currentUser.id, initialEmpId) {
        mutableStateOf(initialEmpId ?: currentUser.id)
    }

    var filterType by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var isEmpDropdownOpen by remember { mutableStateOf(false) }

    var showRepayDialog by remember { mutableStateOf(false) }
    var repayAmountStr by remember { mutableStateOf("") }
    var repayNote by remember { mutableStateOf("") }

    var showPrintDialog by remember { mutableStateOf(false) }

    val activeEmployee = remember(selectedEmpId, employees, currentUser) {
        employees.find { it.id == selectedEmpId } ?: currentUser
    }

    val activeDepartment = remember(activeEmployee.deptId, departments) {
        departments.find { it.id == activeEmployee.deptId }?.name ?: "القسم العام"
    }

    val statementRowsFlow = remember(selectedEmpId) {
        viewModel.repository.getStatementRows(selectedEmpId)
    }
    val allStatementRows by statementRowsFlow.collectAsState(initial = emptyList())

    val filteredRows = remember(allStatementRows, filterType, searchQuery) {
        allStatementRows.filter { row ->
            val tx = row.transaction
            val matchType = when (filterType) {
                "all" -> true
                "advance" -> tx.type == "advance"
                "repayment" -> tx.type == "repayment"
                "penalty" -> tx.type == "penalty" || tx.type == "deduction"
                else -> true
            }
            val matchSearch = searchQuery.isBlank() ||
                    tx.desc.contains(searchQuery, ignoreCase = true) ||
                    tx.no.contains(searchQuery, ignoreCase = true) ||
                    tx.date.contains(searchQuery, ignoreCase = true) ||
                    tx.note.contains(searchQuery, ignoreCase = true)
            matchType && matchSearch
        }
    }

    val statsFlow = remember(selectedEmpId) {
        viewModel.repository.getEmployeeFinanceStats(selectedEmpId)
    }
    val stats by statsFlow.collectAsState(initial = EmployeeFinanceStats())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. Employee Identification Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AvatarView(employee = activeEmployee, size = 48)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = activeEmployee.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (activeEmployee.id == currentUser.id) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = BrandGreenLight
                                        ) {
                                            Text(
                                                text = "حسابك",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = BrandGreenDark,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "الرقم الوظيفي: ${activeEmployee.no} • ${activeEmployee.job}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "القسم: $activeDepartment",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // Employee selector button if multiple available
                        if (availableEmployees.size > 1) {
                            Box {
                                OutlinedButton(
                                    onClick = { isEmpDropdownOpen = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("تغيير الموظف", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                }

                                DropdownMenu(
                                    expanded = isEmpDropdownOpen,
                                    onDismissRequest = { isEmpDropdownOpen = false }
                                ) {
                                    availableEmployees.forEach { emp ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(emp.name, fontWeight = if (emp.id == selectedEmpId) FontWeight.Bold else FontWeight.Normal)
                                                    Text("${emp.no} • ${emp.job}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                }
                                            },
                                            onClick = {
                                                selectedEmpId = emp.id
                                                isEmpDropdownOpen = false
                                            },
                                            leadingIcon = {
                                                if (emp.id == selectedEmpId) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = BrandGreen)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Main Balance Hero Banner
        item {
            val isZeroDebt = stats.remainingBalance <= 0.0
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                if (isZeroDebt) listOf(BrandGreenDark, BrandTealDark)
                                else listOf(BrandSlateDark, BrandSlate)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "صافي رصيد السلف القائم (الذمة المالية)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${EmployeeRepository.formatAmount(stats.remainingBalance)} ${settings.currency}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isZeroDebt) Color.White else AccentAmber
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isZeroDebt) Icons.Default.CheckCircle else Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isZeroDebt) "لا توجد أي مبالغ سلف مستحقة في الذمة المالية ✅"
                                else "المبلغ المستحق السداد على الموظف حاليًا ⚠️",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            if (currentUser.role == "gm" || currentUser.role == "supervisor") {
                                if (stats.remainingBalance > 0) {
                                    Button(
                                        onClick = {
                                            repayAmountStr = ""
                                            repayNote = ""
                                            showRepayDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسجيل سداد", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Financial Metrics 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FinancialMetricCard(
                        title = "إجمالي السلف",
                        amount = "${EmployeeRepository.formatAmount(stats.totalAdvances)} ${settings.currency}",
                        subtitle = "إجمالي المبالغ المصروفة",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = BrandGreenDark,
                        bgColor = BrandGreenLight,
                        modifier = Modifier.weight(1f)
                    )

                    FinancialMetricCard(
                        title = "إجمالي المسدد",
                        amount = "${EmployeeRepository.formatAmount(stats.totalRepaid)} ${settings.currency}",
                        subtitle = "الدفعات المستردة",
                        icon = Icons.Default.Payment,
                        accentColor = BrandBlueDark,
                        bgColor = BrandBlueLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FinancialMetricCard(
                        title = "إجمالي الخصومات",
                        amount = "${EmployeeRepository.formatAmount(stats.totalDeductions)} ${settings.currency}",
                        subtitle = "الجزاءات والاستقطاعات",
                        icon = Icons.Default.RemoveCircle,
                        accentColor = AccentRed,
                        bgColor = AccentRedLight,
                        modifier = Modifier.weight(1f)
                    )

                    FinancialMetricCard(
                        title = "عدد الحركات",
                        amount = "${allStatementRows.size} حركة",
                        subtitle = "المقيدة بالسجل المالي",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = AccentTeal,
                        bgColor = AccentTealLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Action Toolbar & Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "سجل الحركات المالية (${filteredRows.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { showPrintDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طباعة / PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Filter Chips Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        StatementFilterChip(
                            label = "الكل",
                            count = allStatementRows.size,
                            isSelected = filterType == "all",
                            onClick = { filterType = "all" }
                        )
                    }
                    item {
                        StatementFilterChip(
                            label = "السلف (+)",
                            count = allStatementRows.count { it.transaction.type == "advance" },
                            isSelected = filterType == "advance",
                            onClick = { filterType = "advance" },
                            activeColor = BrandGreenDark
                        )
                    }
                    item {
                        StatementFilterChip(
                            label = "السداد (-)",
                            count = allStatementRows.count { it.transaction.type == "repayment" },
                            isSelected = filterType == "repayment",
                            onClick = { filterType = "repayment" },
                            activeColor = BrandBlueDark
                        )
                    }
                    item {
                        StatementFilterChip(
                            label = "الخصومات والجزاءات",
                            count = allStatementRows.count { it.transaction.type == "penalty" || it.transaction.type == "deduction" },
                            isSelected = filterType == "penalty",
                            onClick = { filterType = "penalty" },
                            activeColor = AccentRed
                        )
                    }
                }

                // Quick Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في البيان أو رقم السند أو التاريخ...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // 5. Statement Rows / Empty State
        if (filteredRows.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "لا توجد حركات مالية",
                    message = if (searchQuery.isNotBlank() || filterType != "all")
                        "لا توجد حركات مطابقة لمعايير البحث أو التصفية الحالية"
                    else "لم يتم تسجيل أي حركات سلف أو سداد أو خصومات لهذا الحساب بعد",
                    icon = Icons.Default.ReceiptLong
                )
            }
        } else {
            items(filteredRows.reversed(), key = { it.transaction.id }) { row ->
                StatementRowCard(
                    row = row,
                    currency = settings.currency
                )
            }
        }
    }

    // Repayment Modal Dialog (for GM / Supervisor)
    if (showRepayDialog) {
        val maxRepay = stats.remainingBalance
        val repayAmount = repayAmountStr.toDoubleOrNull() ?: 0.0
        val isRepayValid = repayAmount in 1.0..maxRepay

        AlertDialog(
            onDismissRequest = { showRepayDialog = false },
            title = {
                Text("تسجيل سداد سلفة — ${activeEmployee.name}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentAmberLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "الرصيد المتبقي على الموظف: ${EmployeeRepository.formatAmount(maxRepay)} ${settings.currency}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = repayAmountStr,
                        onValueChange = { repayAmountStr = it },
                        label = { Text("مبلغ السداد (${settings.currency}) *") },
                        placeholder = { Text("مثال: 1500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = repayAmountStr.isNotEmpty() && !isRepayValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = repayNote,
                        onValueChange = { repayNote = it },
                        label = { Text("البيان / ملاحظة السداد") },
                        placeholder = { Text("مثال: دفعة شهرية / استقطاع من الراتب...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isRepayValid) {
                            viewModel.recordRepayment(
                                empId = activeEmployee.id,
                                amount = repayAmount,
                                date = EmployeeRepository.getCurrentDateString(),
                                note = repayNote
                            )
                            showRepayDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    enabled = isRepayValid
                ) {
                    Text("تسجيل السداد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepayDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // PDF / Print Statement Document Modal
    if (showPrintDialog) {
        AlertDialog(
            onDismissRequest = { showPrintDialog = false },
            title = {
                Text("معاينة كشف الحساب المالي — PDF", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF9FBFD),
                        border = BorderStroke(1.dp, Color(0xFFE4E9F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = settings.companyName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandSlate
                            )
                            Text(
                                text = "كشف حساب الموظف: ${activeEmployee.name} (${activeEmployee.no})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "المسمى الوظيفي: ${activeEmployee.job} • القسم: $activeDepartment",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = "تاريخ الإصدار: ${EmployeeRepository.getCurrentDateTimeString()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي السلف المستلمة:", style = MaterialTheme.typography.labelSmall)
                                Text("${EmployeeRepository.formatAmount(stats.totalAdvances)} ${settings.currency}", fontWeight = FontWeight.Bold, color = BrandGreenDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي المبالغ المسددة:", style = MaterialTheme.typography.labelSmall)
                                Text("${EmployeeRepository.formatAmount(stats.totalRepaid)} ${settings.currency}", fontWeight = FontWeight.Bold, color = BrandBlueDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("صافي الرصيد المتبقي:", style = MaterialTheme.typography.labelSmall)
                                Text("${EmployeeRepository.formatAmount(stats.remainingBalance)} ${settings.currency}", fontWeight = FontWeight.Bold, color = AccentAmber)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي الخصومات والجزاءات:", style = MaterialTheme.typography.labelSmall)
                                Text("${EmployeeRepository.formatAmount(stats.totalDeductions)} ${settings.currency}", fontWeight = FontWeight.Bold, color = AccentRed)
                            }
                        }
                    }
                    Text(
                        text = "وثيقة رسمية معتمدة من النظام، جاهزة للطباعة أو التصدير بصيغة PDF ومطابقة للقيود المحاسبية.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activity = context.findActivity() ?: (context as? Activity)
                        if (activity == null) {
                            Toast.makeText(context, "تعذر تحديد واجهة التشغيل (Activity) للطباعة", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        try {
                            val launched = PdfPrintHelper.printEmployeeStatement(
                                context = activity,
                                employee = activeEmployee,
                                departmentName = activeDepartment,
                                stats = stats,
                                rows = allStatementRows,
                                settings = settings
                            )
                            if (launched) {
                                showPrintDialog = false
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "حدث خطأ أثناء محاولة الطباعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طباعة / حفظ بتنسيق PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val activity = context.findActivity() ?: context
                            PdfPrintHelper.shareEmployeeStatement(
                                context = activity,
                                employee = activeEmployee,
                                departmentName = activeDepartment,
                                stats = stats,
                                rows = allStatementRows,
                                settings = settings
                            )
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة كملف", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { showPrintDialog = false }) {
                        Text("إغلاق")
                    }
                }
            }
        )
    }
}

@Composable
private fun FinancialMetricCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatementFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color = BrandBlue
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) activeColor else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun StatementRowCard(
    row: StatementRow,
    currency: String
) {
    val tx = row.transaction
    val isAdv = tx.type == "advance"
    val isRep = tx.type == "repayment"
    val isPen = tx.type == "penalty" || tx.type == "deduction"

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isAdv) BrandGreenLight else if (isRep) BrandBlueLight else AccentRedLight
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdv) Icons.Default.AccountBalanceWallet
                            else if (isRep) Icons.Default.Payment
                            else Icons.Default.RemoveCircle,
                            contentDescription = null,
                            tint = if (isAdv) BrandGreenDark else if (isRep) BrandBlueDark else AccentRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = tx.desc,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tx.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = tx.no,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = BrandSlate,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isAdv) "+" else "-"}${EmployeeRepository.formatAmount(tx.amount)} $currency",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAdv) BrandGreenDark else if (isRep) BrandBlueDark else AccentRed
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAdv) BrandGreenLight.copy(alpha = 0.6f) else if (isRep) BrandBlueLight.copy(alpha = 0.6f) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "الرصيد: ${EmployeeRepository.formatAmount(row.runningBalance)} $currency",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isAdv) BrandGreenDark else if (isRep) BrandBlueDark else BrandSlate,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Expanded details if clicked or notes present
            if (isExpanded || tx.note.isNotBlank() || tx.byName.isNotBlank() || tx.srcReqNo.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (tx.srcReqNo.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("رقم الطلب الأصلي:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(tx.srcReqNo, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    if (tx.byName.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المسؤول / المشرف المعتمِد:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(tx.byName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium))
                        }
                    }

                    if (tx.note.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الملاحظات والبيان:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(tx.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
