package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.EmployeeRepository
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    currentUser: Employee,
    onNavigate: (String) -> Unit
) {
    when (currentUser.role) {
        "gm" -> GmDashboard(viewModel, currentUser, onNavigate)
        "supervisor" -> SupervisorDashboard(viewModel, currentUser, onNavigate)
        else -> EmployeeDashboard(viewModel, currentUser, onNavigate)
    }
}

@Composable
private fun GmDashboard(
    viewModel: MainViewModel,
    currentUser: Employee,
    onNavigate: (String) -> Unit
) {
    val employees by viewModel.allEmployees.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val advances by viewModel.advances.collectAsState()
    val penalties by viewModel.penalties.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val activeCount = employees.count { it.status == "active" }
    val totalAdvances = transactions.filter { it.type == "advance" }.sumOf { it.amount }
    val totalDeductions = transactions.filter { it.type == "deduction" || it.type == "penalty" }.sumOf { it.amount }

    val pendingAdvances = advances.filter { it.status == "pending" }
    val pendingPenalties = penalties.filter { it.status == "pending" || it.status == "pending_gm" }
    val totalPending = pendingAdvances.size + pendingPenalties.size

    val approvedCount = advances.count { it.status == "approved" } + penalties.count { it.status == "approved" }
    val rejectedCount = advances.count { it.status == "rejected" } + penalties.count { it.status == "rejected" }

    val daysUntilExpiry = remember(settings.licenseExpiryDate) {
        com.example.util.CompanyLicenseEngine.calculateDaysUntilExpiry(settings.licenseExpiryDate)
    }
    val isExpiryAlertActive = daysUntilExpiry != null && daysUntilExpiry <= 7

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastBody by remember { mutableStateOf("") }
    var isBroadcasting by remember { mutableStateOf(false) }

    var selectedAdvanceForQuickAction by remember { mutableStateOf<AdvanceRequest?>(null) }
    var quickActionType by remember { mutableStateOf<String?>(null) } // "approve" or "reject"
    var quickActionNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Expiry Warning Alert Banner (7 Days or less before expiry)
        if (isExpiryAlertActive && daysUntilExpiry != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gm_license_expiry_alert_card")
                        .clickable { onNavigate("corporate_plans") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (daysUntilExpiry < 0) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (daysUntilExpiry < 0) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                            else listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                        ),
                        width = 1.5.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (daysUntilExpiry < 0) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (daysUntilExpiry < 0) Icons.Default.Warning else Icons.Default.NotificationImportant,
                                    contentDescription = null,
                                    tint = if (daysUntilExpiry < 0) Color(0xFFDC2626) else Color(0xFFD97706),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (daysUntilExpiry < 0) "⚠️ تنبيه عاجل: انتهت صلاحية باقة المنشأة!"
                                        else if (daysUntilExpiry == 0L) "⏳ تنبيه عاجل: باقة المنشأة تنتهي اليوم!"
                                        else "⏳ تنبيه: متبقي $daysUntilExpiry أيام على انتهاء باقة المنشأة",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (daysUntilExpiry < 0) Color(0xFF991B1B) else Color(0xFF92400E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "باقة [${settings.subscriptionPlanName}] تنتهي بتاريخ ${settings.licenseExpiryDate}. يرجى التجديد لضمان استمرارية الخدمة وعدم توقف العمليات.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (daysUntilExpiry < 0) Color(0xFF7F1D1D) else Color(0xFF78350F),
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المنشأة: ${settings.companyName.ifBlank { "براند لايت التجارية" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { onNavigate("corporate_plans") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (daysUntilExpiry < 0) Color(0xFFDC2626) else Color(0xFFD97706),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تجديد الباقة الآن",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Welcome Banner
        item {
            WelcomeBanner(
                employee = currentUser,
                subtitle = "لوحة تحكم المدير العام — نظرة شاملة على السلف والجزاءات والطلبات المعلقة",
                roleTitle = "المدير العام",
                onLogout = { viewModel.logout() }
            )
        }

        // Actionable Pending Endorsements Card for GM
        if (pendingAdvances.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gm_pending_endorsements_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                        width = 1.5.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationImportant,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "🚨 تنبيه تعميد سلف معلقة (${pendingAdvances.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "طلبات سلف تتطلب تعميدك واعتمادك الإداري الفعلي",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF78350F)
                                    )
                                }
                            }

                            TextButton(onClick = { onNavigate("requests") }) {
                                Text("عرض الكل", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFFDE68A))

                        pendingAdvances.take(2).forEach { adv ->
                            val emp = employees.find { it.id == adv.empId }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${emp?.name ?: "موظف"} (${emp?.job ?: ""})",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "الطلب: ${adv.reqNo} • السبب: ${adv.reason}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${EmployeeRepository.formatAmount(adv.amount)} ${settings.currency}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = BrandGreenDark
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                selectedAdvanceForQuickAction = adv
                                                quickActionType = "approve"
                                                quickActionNote = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تعميد السلفة فوراً ✅", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                selectedAdvanceForQuickAction = adv
                                                quickActionType = "reject"
                                                quickActionNote = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(0.7f),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("رفض ❌", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Enterprise Linking and Corporate Broadcast Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("enterprise_linking_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(BrandBlueLight, BrandGreenLight)),
                    width = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "شبكة الربط المؤسسي للمنشأة",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "الإدارة العامة 🏛️ ⬅️ المشرفين (${employees.count { it.role == "supervisor" }}) ⬅️ الموظفين (${employees.count { it.role == "employee" }})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "مترابط رقمياً",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showBroadcastDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرسال تعميم إداري", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onNavigate("employees") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp), tint = BrandBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("دليل أفراد المنشأة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                        }
                    }
                }
            }
        }

        // Broadcast Circular Dialog for GM
        if (showBroadcastDialog) {
            item {
                AlertDialog(
                    onDismissRequest = { if (!isBroadcasting) showBroadcastDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = BrandBlue)
                            Text("إرسال تعميم إداري لجميع أفراد المنشأة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "سيتم توجيه هذا التعميم فوراً لجميع موظفي ومشرفي المنشأة في شريط الإشعارات ولوحة التحكم:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = broadcastTitle,
                                onValueChange = { broadcastTitle = it },
                                label = { Text("عنوان التعميم الإداري") },
                                placeholder = { Text("مثال: تعديل مواعيد العمل أو تنظيم السلف") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = broadcastBody,
                                onValueChange = { broadcastBody = it },
                                label = { Text("نص البيان الإداري") },
                                placeholder = { Text("أدخل تفاصيل التوجيه الإداري الصادر من الإدارة العامة...") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (broadcastTitle.isNotBlank() && broadcastBody.isNotBlank()) {
                                    isBroadcasting = true
                                    viewModel.broadcastCircular(broadcastTitle, broadcastBody) {
                                        isBroadcasting = false
                                        showBroadcastDialog = false
                                        broadcastTitle = ""
                                        broadcastBody = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            enabled = !isBroadcasting && broadcastTitle.isNotBlank() && broadcastBody.isNotBlank()
                        ) {
                            if (isBroadcasting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("إرسال التعميم فوراً 📢", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showBroadcastDialog = false },
                            enabled = !isBroadcasting
                        ) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }

        // Quick Endorsement Action Dialog for GM
        if (selectedAdvanceForQuickAction != null && quickActionType != null) {
            val adv = selectedAdvanceForQuickAction!!
            val isApprove = quickActionType == "approve"
            val emp = employees.find { it.id == adv.empId }

            item {
                AlertDialog(
                    onDismissRequest = {
                        selectedAdvanceForQuickAction = null
                        quickActionType = null
                    },
                    title = {
                        Text(
                            text = if (isApprove) "تعميد السلفة فوراً (${adv.reqNo}) ✅" else "رفض طلب السلفة (${adv.reqNo}) ❌",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = if (isApprove)
                                    "سيتم تعميد سلفة بمبلغ ${EmployeeRepository.formatAmount(adv.amount)} ${settings.currency} للموظف ${emp?.name ?: ""} وقيدها فوراً في كشف حسابه وإشعاره بالقرار."
                                else
                                    "سيتم رفض طلب السلفة وإشعار الموظف ${emp?.name ?: ""} بالسبب.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            OutlinedTextField(
                                value = quickActionNote,
                                onValueChange = { quickActionNote = it },
                                label = { Text(if (isApprove) "ملاحظة التعميد (اختياري)" else "سبب الرفض (اختياري)") },
                                placeholder = { Text("تظهر في كشف الحساب وتُرسل للموظف...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (isApprove) {
                                    viewModel.approveAdvance(adv.id, quickActionNote)
                                } else {
                                    viewModel.rejectAdvance(adv.id, quickActionNote)
                                }
                                selectedAdvanceForQuickAction = null
                                quickActionType = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isApprove) BrandGreen else AccentRed
                            )
                        ) {
                            Text(if (isApprove) "تأكيد التعميد والقيد الفوري ✅" else "تأكيد الرفض ❌", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            selectedAdvanceForQuickAction = null
                            quickActionType = null
                        }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }

        // Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "إجمالي الموظفين",
                        value = "${employees.size}",
                        icon = Icons.Default.People,
                        accentColor = BrandBlue,
                        accentLightColor = BrandBlueLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "الموظفون النشطون",
                        value = "$activeCount",
                        icon = Icons.Default.CheckCircle,
                        accentColor = BrandGreen,
                        accentLightColor = BrandGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "إجمالي السلف",
                        value = "${EmployeeRepository.formatAmount(totalAdvances)} ${settings.currency}",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = BrandGreenDark,
                        accentLightColor = BrandGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "إجمالي الخصومات",
                        value = "${EmployeeRepository.formatAmount(totalDeductions)} ${settings.currency}",
                        icon = Icons.Default.RemoveCircle,
                        accentColor = AccentRed,
                        accentLightColor = AccentRedLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                StatCard(
                    title = "طلبات معلقة بانتظار الاعتماد",
                    value = "$totalPending طلب",
                    icon = Icons.Default.HourglassTop,
                    accentColor = AccentAmber,
                    accentLightColor = AccentAmberLight,
                    subText = "اضغط للمراجعة المباشرة والاعتماد"
                )
            }
        }

        // Charts Section
        item {
            MonthlyBarChart(
                title = "السلف الشهرية (${settings.currency})",
                transactions = transactions,
                types = listOf("advance"),
                barColor = BrandGreen
            )
        }

        item {
            MonthlyBarChart(
                title = "الخصومات والجزاءات الشهرية (${settings.currency})",
                transactions = transactions,
                types = listOf("deduction", "penalty"),
                barColor = AccentRed
            )
        }

        item {
            StatusDoughnutChart(
                approvedCount = approvedCount,
                pendingCount = pendingAdvances.size,
                rejectedCount = rejectedCount,
                pendingGmCount = pendingPenalties.size
            )
        }

        item {
            DepartmentBarChart(
                departments = departments,
                transactions = transactions
            )
        }

        // Pending Requests Card
        item {
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "أحدث الطلبات المعلقة ($totalPending)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { onNavigate("requests") }) {
                            Text("مراجعة الكل", color = BrandBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (pendingAdvances.isEmpty() && pendingPenalties.isEmpty()) {
                        Text(
                            text = "لا توجد طلبات معلقة حاليًا ✅",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandGreenDark,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        pendingAdvances.take(3).forEach { adv ->
                            val emp = employees.find { it.id == adv.empId }
                            RequestItemRow(
                                title = "طلب سلفة: ${adv.reason}",
                                subtitle = "${emp?.name ?: "موظف"} — ${adv.date}",
                                amount = "${EmployeeRepository.formatAmount(adv.amount)} ${settings.currency}",
                                isPositive = true,
                                onClick = { onNavigate("requests") }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        }

                        pendingPenalties.take(3).forEach { pen ->
                            val emp = employees.find { it.id == pen.empId }
                            RequestItemRow(
                                title = "خصم/جزاء: ${pen.reason}",
                                subtitle = "${emp?.name ?: "موظف"} — ${pen.date}",
                                amount = "${EmployeeRepository.formatAmount(pen.amount)} ${settings.currency}",
                                isPositive = false,
                                onClick = { onNavigate("requests") }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupervisorDashboard(
    viewModel: MainViewModel,
    currentUser: Employee,
    onNavigate: (String) -> Unit
) {
    val employees by viewModel.allEmployees.collectAsState()
    val advances by viewModel.advances.collectAsState()
    val penalties by viewModel.penalties.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val mySubs = remember(employees, currentUser.id) {
        employees.filter { it.supId == currentUser.id }
    }
    val mySubIds = remember(mySubs) { mySubs.map { it.id } }

    val pendingAdvances = advances.filter { mySubIds.contains(it.empId) && it.status == "pending" }
    val pendingPenalties = penalties.filter { mySubIds.contains(it.empId) && (it.status == "pending" || it.status == "pending_gm") }
    val totalPending = pendingAdvances.size + pendingPenalties.size

    val myTeamTx = transactions.filter { mySubIds.contains(it.empId) }
    val teamAdvances = myTeamTx.filter { it.type == "advance" }.sumOf { it.amount }
    val teamDeductions = myTeamTx.filter { it.type == "deduction" || it.type == "penalty" }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            WelcomeBanner(
                employee = currentUser,
                subtitle = "لوحة تحكم المشرف — متابعة طلبات فريقك واعتماد السلف والخصومات",
                roleTitle = "مشرف مباشر",
                onLogout = { viewModel.logout() }
            )
        }

        // Supervisor Hierarchy Link Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF0FDF4),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF86EFAC), Color(0xFF22C55E))),
                    width = 1.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "سلسلة الربط الإداري المباشر",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = "الإدارة العامة 🏛️ ⬅️ إشراف القسم 👔 ⬅️ أعضاء فريقك المباشرين (${mySubs.size} موظف)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "أعضاء فريقك",
                        value = "${mySubs.size} موظفين",
                        icon = Icons.Default.Group,
                        accentColor = BrandBlue,
                        accentLightColor = BrandBlueLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "طلبات معلقة",
                        value = "$totalPending",
                        icon = Icons.Default.HourglassTop,
                        accentColor = AccentAmber,
                        accentLightColor = AccentAmberLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "سلف الفريق",
                        value = "${EmployeeRepository.formatAmount(teamAdvances)} ${settings.currency}",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = BrandGreen,
                        accentLightColor = BrandGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "خصومات الفريق",
                        value = "${EmployeeRepository.formatAmount(teamDeductions)} ${settings.currency}",
                        icon = Icons.Default.RemoveCircle,
                        accentColor = AccentRed,
                        accentLightColor = AccentRedLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigate("new_advance") },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل سلفة", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onNavigate("new_penalty") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل خصم", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subordinates List Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "موظفوك المباشرون (${mySubs.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (mySubs.isEmpty()) {
                        Text(
                            text = "لا يوجد موظفون مسندون إليك حاليًا",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        mySubs.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate("employee/${sub.id}") }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AvatarView(employee = sub, size = 40)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${sub.job} • رقم: ${sub.no}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeDashboard(
    viewModel: MainViewModel,
    currentUser: Employee,
    onNavigate: (String) -> Unit
) {
    val financeStats by viewModel.repository.getEmployeeFinanceStats(currentUser.id)
        .collectAsState(initial = EmployeeFinanceStats())
    val statementRows by viewModel.repository.getStatementRows(currentUser.id)
        .collectAsState(initial = emptyList())
    val settings by viewModel.settings.collectAsState()
    val advances by viewModel.repository.getAdvancesForEmployee(currentUser.id)
        .collectAsState(initial = emptyList())
    val pendingCount = advances.count { it.status == "pending" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            WelcomeBanner(
                employee = currentUser,
                subtitle = "${currentUser.job} • الرقم الوظيفي: ${currentUser.no} • 📱 ${com.example.util.PhoneAuthHelper.formatDisplayPhone(currentUser.mobile)}",
                roleTitle = "حساب موظف",
                onLogout = { viewModel.logout() }
            )
        }

        // Employee Corporate Connection Link Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEFF6FF),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF93C5FD), Color(0xFF3B82F6))),
                    width = 1.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "منظومة الربط المباشر مع الإدارة العامة",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "طلبات السلف وكشوف الحسابات ترتبط مباشرة بسلسلة الاعتماد: الإدارة العامة 🏛️ ⬅️ المشرف المباشر 👔 ⬅️ حسابك",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1D4ED8),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Employee Financial Metrics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "رصيد السلف المتبقي",
                    value = "${EmployeeRepository.formatAmount(financeStats.remainingBalance)} ${settings.currency}",
                    icon = Icons.Default.AccountBalance,
                    accentColor = AccentAmber,
                    accentLightColor = AccentAmberLight,
                    subText = "المبلغ المتبقي للذمة المالية"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "إجمالي السلف",
                        value = "${EmployeeRepository.formatAmount(financeStats.totalAdvances)} ${settings.currency}",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = BrandGreen,
                        accentLightColor = BrandGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "إجمالي المسدد",
                        value = "${EmployeeRepository.formatAmount(financeStats.totalRepaid)} ${settings.currency}",
                        icon = Icons.Default.Payment,
                        accentColor = BrandBlue,
                        accentLightColor = BrandBlueLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "إجمالي الخصومات",
                        value = "${EmployeeRepository.formatAmount(financeStats.totalDeductions)} ${settings.currency}",
                        icon = Icons.Default.RemoveCircle,
                        accentColor = AccentRed,
                        accentLightColor = AccentRedLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "طلبات قيد المراجعة",
                        value = "$pendingCount",
                        icon = Icons.Default.HourglassTop,
                        accentColor = AccentTeal,
                        accentLightColor = AccentTealLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onNavigate("advance_request") },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تقديم طلب سلفة جديدة", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigate("statement") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("كشف الحساب", color = BrandBlue, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onNavigate("my_requests") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = BrandGreenDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سجل طلباتي", color = BrandGreenDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent Transactions Card
        item {
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "أحدث الحركات المالية",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { onNavigate("statement") }) {
                            Text("عرض الكل", color = BrandBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (statementRows.isEmpty()) {
                        Text(
                            text = "لا توجد حركات مالية مسجلة بعد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        statementRows.takeLast(4).reversed().forEach { row ->
                            val tx = row.transaction
                            val isAdv = tx.type == "advance"
                            val isRep = tx.type == "repayment"
                            RequestItemRow(
                                title = tx.desc,
                                subtitle = "${tx.date} • ${tx.no}",
                                amount = "${if (isAdv) "+" else if (isRep) "-" else "-"} ${EmployeeRepository.formatAmount(tx.amount)} ${settings.currency}",
                                isPositive = isAdv,
                                onClick = { onNavigate("statement") }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeBanner(
    employee: Employee,
    subtitle: String,
    roleTitle: String,
    onLogout: (() -> Unit)? = null
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog && onLogout != null) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("تسجيل الخروج", fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في تسجيل الخروج من حساب ${employee.name}؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("تسجيل الخروج", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("إلغاء") }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BrandSlate, BrandTealDark)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AvatarView(employee = employee, size = 56)

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "أهلًا، ${employee.name}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (onLogout != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier.clickable { showLogoutDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "تسجيل الخروج",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "خروج",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = roleTitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB6CBDA)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestItemRow(
    title: String,
    subtitle: String,
    amount: String,
    isPositive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = if (isPositive) BrandGreenDark else AccentRed
            )
        )
    }
}
