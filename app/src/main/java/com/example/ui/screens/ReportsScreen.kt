package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.data.model.Transaction
import com.example.data.repository.EmployeeRepository
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.util.PdfPrintHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    currentUser: Employee
) {
    val context = LocalContext.current
    val employees by viewModel.allEmployees.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val departments by viewModel.departments.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var reportType by remember { mutableStateOf("all") } // "all", "advances", "penalties", "repayments"
    var showPreviewModal by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, reportType) {
        when (reportType) {
            "advances" -> transactions.filter { it.type == "advance" }
            "penalties" -> transactions.filter { it.type == "deduction" || it.type == "penalty" }
            "repayments" -> transactions.filter { it.type == "repayment" }
            else -> transactions
        }
    }

    val totalAdvances = remember(transactions) {
        transactions.filter { it.type == "advance" }.sumOf { it.amount }
    }
    val totalRepaid = remember(transactions) {
        transactions.filter { it.type == "repayment" }.sumOf { it.amount }
    }
    val totalDeductions = remember(transactions) {
        transactions.filter { it.type == "deduction" || it.type == "penalty" }.sumOf { it.amount }
    }
    val netBalance = totalAdvances - totalRepaid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab Selector for Management Reports
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BrandGreenDark,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .testTag("reports_tab_row")
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("سجل العمليات", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("ملخص الأقسام", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("تصدير Excel / CSV", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: Transactions Overview & PDF
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تصفية التقرير المالي",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Button(
                                onClick = { showPreviewModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("معاينة PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = reportType == "all",
                                onClick = { reportType = "all" },
                                label = { Text("الكل (${transactions.size})") }
                            )
                            FilterChip(
                                selected = reportType == "advances",
                                onClick = { reportType = "advances" },
                                label = { Text("السلف") }
                            )
                            FilterChip(
                                selected = reportType == "penalties",
                                onClick = { reportType = "penalties" },
                                label = { Text("الخصومات") }
                            )
                            FilterChip(
                                selected = reportType == "repayments",
                                onClick = { reportType = "repayments" },
                                label = { Text("السداد") }
                            )
                        }
                    }
                }

                if (filteredTransactions.isEmpty()) {
                    EmptyStateCard(
                        title = "لا توجد بيانات",
                        message = "لا توجد حركات مطابقة للتقرير المختار",
                        icon = Icons.Default.ReceiptLong
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredTransactions.reversed(), key = { it.id }) { tx ->
                            val emp = employees.find { it.id == tx.empId }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${emp?.name ?: "موظف"} — ${tx.desc}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${tx.date} • ${tx.no}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "${EmployeeRepository.formatAmount(tx.amount)} ${settings.currency}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (tx.type == "advance") BrandGreenDark else if (tx.type == "repayment") BrandBlueDark else AccentRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Tab 1: Department Financial Summary
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "توزيع الذمم المالية حسب الأقسام والإدارات",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "يوضح هذا التقرير إجمالي السلف والخصومات لكل قسم لمتابعة مؤشرات الأداء المالي.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    items(departments, key = { it.id }) { dept ->
                        val deptEmps = employees.filter { it.deptId == dept.id }
                        val deptEmpIds = deptEmps.map { it.id }.toSet()
                        val deptTxs = transactions.filter { it.empId in deptEmpIds }

                        val deptAdvances = deptTxs.filter { it.type == "advance" }.sumOf { it.amount }
                        val deptRepaid = deptTxs.filter { it.type == "repayment" }.sumOf { it.amount }
                        val deptDeductions = deptTxs.filter { it.type == "deduction" || it.type == "penalty" }.sumOf { it.amount }
                        val deptRemaining = deptAdvances - deptRepaid

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = BrandGreenDark)
                                        Text(
                                            text = dept.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = BrandBlueLight) {
                                        Text(
                                            text = "${deptEmps.size} موظف",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = BrandBlueDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("إجمالي السلف", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(
                                            "${EmployeeRepository.formatAmount(deptAdvances)} ${settings.currency}",
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreenDark
                                        )
                                    }
                                    Column {
                                        Text("المسدد", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(
                                            "${EmployeeRepository.formatAmount(deptRepaid)} ${settings.currency}",
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlueDark
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("الرصيد القائم", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(
                                            "${EmployeeRepository.formatAmount(deptRemaining)} ${settings.currency}",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AccentAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Tab 2: Export Data for Excel / CSV
                val csvContent = remember(employees, transactions) {
                    val sb = StringBuilder()
                    sb.append("رقم السند,التاريخ,اسم الموظف,الرقم الوظيفي,نوع الحركة,المبلغ,البيان\n")
                    transactions.forEach { tx ->
                        val emp = employees.find { it.id == tx.empId }
                        val typeStr = when (tx.type) {
                            "advance" -> "سلفة"
                            "repayment" -> "سداد سلفة"
                            "penalty", "deduction" -> "خصم/جزاء"
                            else -> tx.type
                        }
                        sb.append("${tx.no},${tx.date},\"${emp?.name ?: ""}\",${emp?.no ?: ""},$typeStr,${tx.amount},\"${tx.desc}\"\n")
                    }
                    sb.toString()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = BrandGreenDark)
                                    Text(
                                        text = "تصدير السجلات إلى جداول Excel / CSV",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "يمكنك تصدير كشوف الحسابات وكافة الحركات المالية لفتحها مباشرة في Microsoft Excel أو Google Sheets.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Financial Data CSV", csvContent)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ بيانات الجدول بصيغة CSV إلى الحافظة بنجاح!", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("نسخ جدول البيانات CSV للحافظة", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "معاينة بنية البيانات الجاهزة للتصدير:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BrandSlate
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = csvContent.lines().take(8).joinToString("\n") + if (csvContent.lines().size > 8) "\n..." else "",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                        modifier = Modifier.padding(10.dp),
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPreviewModal) {
        AlertDialog(
            onDismissRequest = { showPreviewModal = false },
            title = { Text("معاينة التقرير الرسمي PDF", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المنشأة: ${settings.companyName}", fontWeight = FontWeight.Bold)
                    Text("نوع التقرير: تقرير مالي رسمي شامل")
                    Text("تاريخ الاستخراج: ${EmployeeRepository.getCurrentDateTimeString()}")
                    Text("إجمالي السلف: ${EmployeeRepository.formatAmount(totalAdvances)} ${settings.currency}", fontWeight = FontWeight.Bold, color = BrandGreenDark)
                    Text("إجمالي المسدد: ${EmployeeRepository.formatAmount(totalRepaid)} ${settings.currency}", fontWeight = FontWeight.Bold, color = BrandBlueDark)
                    Text("صافي الذمم القائمة: ${EmployeeRepository.formatAmount(netBalance)} ${settings.currency}", fontWeight = FontWeight.ExtraBold, color = AccentAmber)
                    Text("إجمالي الخصومات: ${EmployeeRepository.formatAmount(totalDeductions)} ${settings.currency}", fontWeight = FontWeight.Bold, color = AccentRed)
                    HorizontalDivider()
                    Text("وثيقة جاهزة للطباعة والتصدير بجودة عالية.", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val launched = PdfPrintHelper.printFinancialReport(
                            context = context,
                            settings = settings,
                            transactions = filteredTransactions,
                            employees = employees,
                            reportTitle = "التقرير المالي الشامل للعمليات"
                        )
                        if (launched) {
                            showPreviewModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طباعة / حفظ PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewModal = false }) { Text("إغلاق") }
            }
        )
    }
}
