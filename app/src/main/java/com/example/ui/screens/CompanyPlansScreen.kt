package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.APP_VERSION
import com.example.data.model.CompanyPlan
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyPlansScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val employees by viewModel.allEmployees.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showLicenseDialog by remember { mutableStateOf(false) }
    var selectedPlanToActivate by remember { mutableStateOf<CompanyPlan?>(null) }
    var inputLicenseCode by remember { mutableStateOf("") }
    var billingCycleMonthly by remember { mutableStateOf(false) }

    val activeEmployeeCount = employees.count { it.status == "active" }
    val maxSeats = settings.maxEmployeesLimit
    val seatUsageRatio = if (maxSeats > 0) (activeEmployeeCount.toFloat() / maxSeats.toFloat()).coerceIn(0f, 1f) else 0.1f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "باقات وتراخيص الشركات",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "إصدار المنظومة: v$APP_VERSION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    if (currentUser?.role == "gm") {
                        IconButton(
                            onClick = {
                                inputLicenseCode = ""
                                showLicenseDialog = true
                            },
                            modifier = Modifier.testTag("enter_license_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VpnKey,
                                contentDescription = "إدخال مفتاح الترخيص",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Active Subscription Hero Banner
            item {
                ActiveSubscriptionCard(
                    settings = settings,
                    activeEmployees = activeEmployeeCount,
                    seatUsageRatio = seatUsageRatio,
                    onEnterKeyClick = {
                        inputLicenseCode = ""
                        showLicenseDialog = true
                    },
                    onCopyKey = { key ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("License Key", key))
                        Toast.makeText(context, "تم نسخ مفتاح الترخيص بنجاح", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Billing Cycle Switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "دورة الفوترة والاشتراك",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (billingCycleMonthly) "الدفع الشهري الميسر" else "الاشتراك السنوي (خصم 20% + ترخيص معتمد)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (billingCycleMonthly) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF047857)
                            )
                        }

                        FilterChip(
                            selected = !billingCycleMonthly,
                            onClick = { billingCycleMonthly = !billingCycleMonthly },
                            label = {
                                Text(if (billingCycleMonthly) "شهري" else "سنوي (وفر 20%)")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (!billingCycleMonthly) Icons.Default.Savings else Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Plans Title Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "باقات وعروض المنشآت والشركات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text("عقود B2B رسمية", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }

            // List of Plans
            items(CompanyPlan.ALL_PLANS, key = { it.id }) { plan ->
                val isCurrentPlan = settings.subscriptionTier.equals(plan.id, ignoreCase = true)
                PlanCard(
                    plan = plan,
                    isCurrentPlan = isCurrentPlan,
                    isMonthly = billingCycleMonthly,
                    isGm = currentUser?.role == "gm",
                    onActivateClick = {
                        selectedPlanToActivate = plan
                    }
                )
            }

            // Corporate Support & Inquiry Card
            item {
                CorporateContactCard()
            }
        }
    }

    // Plan Activation Confirmation Dialog
    selectedPlanToActivate?.let { plan ->
        AlertDialog(
            onDismissRequest = { selectedPlanToActivate = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "تفعيل ${plan.title}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هل ترغب في ترقية وتفعيل باقة المنشأة الحالية إلى ${plan.title}؟")
                    Text(
                        text = "• السعة: ${if (plan.maxEmployees == -1) "موظفون غير محدود" else "${plan.maxEmployees} موظف"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• سيتم توليد مفتاح ترخيص مؤسسي جديد وتفعيل كافة الميزات فورياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSubscription(plan) {
                            selectedPlanToActivate = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_plan_activation")
                ) {
                    Text("تأكيد وتفعيل الباقة")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlanToActivate = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Custom License Code Dialog
    if (showLicenseDialog) {
        val companyName = settings.companyName.ifBlank { "براند لايت التجارية" }
        val orgCode = com.example.util.CompanyLicenseEngine.getOrgCode(companyName)
        val orgPrefix = com.example.util.CompanyLicenseEngine.extractOrgPrefix(companyName)
        val officialKeys = remember(companyName) {
            com.example.util.CompanyLicenseEngine.getOfficialCompanyKeys(companyName)
        }

        val validationResult = remember(inputLicenseCode, companyName) {
            if (inputLicenseCode.isNotBlank()) {
                com.example.util.CompanyLicenseEngine.validateLicenseKey(inputLicenseCode, companyName)
            } else null
        }

        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "تفعيل ترخيص مؤسسي معتمد",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Organization Binding Banner
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "المنشأة المستهدفة: $companyName",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "رمز المنشأة الرقمي: $orgCode (البادئة: $orgPrefix)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "أدخل مفتاح الترخيص المشفر الخاص بمنشأتكم. يمنع النظام تفعيل تراخيص منشآت أخرى:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputLicenseCode,
                        onValueChange = { inputLicenseCode = it.uppercase() },
                        label = { Text("مفتاح الترخيص (License Key)") },
                        placeholder = { Text("CORP-$orgPrefix-PRO-2027-XXXX") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("license_input_field"),
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null)
                        },
                        isError = validationResult is com.example.util.LicenseValidationResult.MismatchOrganization ||
                                validationResult is com.example.util.LicenseValidationResult.InvalidFormat
                    )

                    // Realtime Validation Feedback Box
                    validationResult?.let { result ->
                        when (result) {
                            is com.example.util.LicenseValidationResult.Valid -> {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "ترخيص صحيح ومعتمد لباقة [${result.planTitle}] مخصص حصراً لمنشأة ($companyName)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF15803D),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            is com.example.util.LicenseValidationResult.MismatchOrganization -> {
                                Surface(
                                    color = Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(18.dp))
                                        Text(
                                            text = result.reason,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }
                                }
                            }
                            is com.example.util.LicenseValidationResult.InvalidFormat -> {
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Official Generated Keys for this organization
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "التراخيص المعتمدة لمنشأة $companyName:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    officialKeys.forEach { sample ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${sample.planTitle} (${sample.maxEmployeesText})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = sample.key,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { inputLicenseCode = sample.key },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("استخدام", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val key = inputLicenseCode.trim().uppercase()
                        val targetPlan = when {
                            key.contains("ENT") -> CompanyPlan.ALL_PLANS.find { it.id == "enterprise" }
                            key.contains("BAS") -> CompanyPlan.ALL_PLANS.find { it.id == "basic" }
                            else -> CompanyPlan.ALL_PLANS.find { it.id == "pro" }
                        } ?: CompanyPlan.ALL_PLANS[1]

                        viewModel.updateSubscription(targetPlan, customLicenseKey = key) {
                            showLicenseDialog = false
                            Toast.makeText(context, "تم توثيق وتفعيل الترخيص المؤسسي بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = inputLicenseCode.isNotBlank() && validationResult is com.example.util.LicenseValidationResult.Valid,
                    modifier = Modifier.testTag("activate_license_submit_button")
                ) {
                    Text("تفعيل الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ActiveSubscriptionCard(
    settings: com.example.data.model.AppSettings,
    activeEmployees: Int,
    seatUsageRatio: Float,
    onEnterKeyClick: () -> Unit,
    onCopyKey: (String) -> Unit
) {
    val tierGradient = when (settings.subscriptionTier) {
        "enterprise" -> listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF312E81))
        "basic" -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569))
        else -> listOf(Color(0xFF0F2B48), Color(0xFF1E40AF), Color(0xFF0284C7)) // Pro
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_subscription_card"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(tierGradient))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                val companyName = settings.companyName.ifBlank { "براند لايت التجارية" }
                val orgCode = com.example.util.CompanyLicenseEngine.getOrgCode(companyName)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "منشأة: $companyName",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF93C5FD),
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(0x3338BDF8),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "رمز: $orgCode",
                                    color = Color(0xFFBAE6FD),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = settings.subscriptionPlanName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = Color(0x33FFFFFF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4ADE80))
                            )
                            Text(
                                text = "ترخيص مقترن بالمنشأة",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0x22000000),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "حماية الملكية: مفتاح الترخيص مشفر رقمياً ومخصص لمنشأة ($companyName) فقط، ويمنع استخدامه في أي منشأة أخرى.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp
                        )
                    }
                }

                HorizontalDivider(color = Color(0x22FFFFFF))

                // License Key & Expiry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مفتاح الترخيص المؤسسي:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = settings.licenseKey,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE047)
                            )
                            IconButton(
                                onClick = { onCopyKey(settings.licenseKey) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ الترخيص",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val daysRemaining = com.example.util.CompanyLicenseEngine.calculateDaysUntilExpiry(settings.licenseExpiryDate)
                        Text(
                            text = "تاريخ التجديد:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = settings.licenseExpiryDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (daysRemaining != null) {
                            Surface(
                                color = if (daysRemaining <= 7) Color(0xFFEF4444).copy(alpha = 0.3f) else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = if (daysRemaining < 0) "منتهية الصلاحية"
                                    else if (daysRemaining == 0L) "تنتهي اليوم"
                                    else "متبقي $daysRemaining أيام",
                                    color = if (daysRemaining <= 7) Color(0xFFFCA5A5) else Color(0xFFBAE6FD),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Seats Usage Meter
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "استهلاك مقاعد الموظفين:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = if (settings.maxEmployeesLimit == -1) "$activeEmployees موظف (سعة مفتوحة)" else "$activeEmployees من أصل ${settings.maxEmployeesLimit} موظف",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    LinearProgressIndicator(
                        progress = { seatUsageRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0x33FFFFFF),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: CompanyPlan,
    isCurrentPlan: Boolean,
    isMonthly: Boolean,
    isGm: Boolean,
    onActivateClick: () -> Unit
) {
    val borderColor = if (isCurrentPlan) MaterialTheme.colorScheme.primary else if (plan.isPopular) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isCurrentPlan || plan.isPopular) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .testTag("plan_card_${plan.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlan) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (plan.isPopular) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plan.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (plan.badgeLabel.isNotBlank()) {
                    Surface(
                        color = if (plan.isPopular) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = plan.badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (plan.isPopular) Color(0xFF92400E) else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Pricing Area
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isMonthly) plan.priceMonthly else plan.priceYearly,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Highlights
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                plan.highlights.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Feature Checklist
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "المميزات والخدمات المشمولة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                plan.features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isCurrentPlan) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button
            if (isCurrentPlan) {
                OutlinedButton(
                    onClick = { /* Already active */ },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الباقة الحالية النشطة للمنشأة")
                }
            } else if (isGm) {
                Button(
                    onClick = onActivateClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تفعيل ${plan.title} للمنشأة")
                }
            }
        }
    }
}

@Composable
private fun CorporateContactCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.HeadsetMic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "قسم مبيعات وعقود الشركات والمؤسسات",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "هل تحتاج إلى تخصيص باقة مخصصة لمجموعتكم أو إضافة فروع متعددة مع ربط مباشر؟ يسعد فريق حلول الأعمال بخدمتكم.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Column {
                            Text("المبيعات المباشرة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("920000000", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Column {
                            Text("بريد الشركات", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("sales@brandlight.com", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
