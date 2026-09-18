package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.APP_VERSION
import com.example.data.model.AppSettings
import com.example.data.model.Department
import com.example.data.model.Employee
import com.example.ui.MainViewModel
import com.example.ui.components.AvatarView
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    currentUser: Employee,
    onNavigateToPlans: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val employees by viewModel.allEmployees.collectAsState()
    val departments by viewModel.departments.collectAsState()

    // System Settings States
    var companyName by remember(settings) { mutableStateOf(settings.companyName) }
    var currency by remember(settings) { mutableStateOf(settings.currency) }
    var advanceMaxStr by remember(settings) { mutableStateOf(settings.advanceMax.toInt().toString()) }
    var penaltyNeedsGM by remember(settings) { mutableStateOf(settings.penaltyNeedsGM) }
    var sessionTimeoutStr by remember(settings) { mutableStateOf(settings.sessionTimeout.toString()) }
    var autoLoginEnabled by remember(settings) { mutableStateOf(settings.autoLoginEnabled) }

    // Manager Profile States
    var mgrName by remember(currentUser) { mutableStateOf(currentUser.name) }
    var mgrUsername by remember(currentUser) { mutableStateOf(currentUser.username) }
    var mgrMobile by remember(currentUser) { mutableStateOf(currentUser.mobile) }
    var mgrEmail by remember(currentUser) { mutableStateOf(currentUser.email) }
    var mgrNewPassword by remember { mutableStateOf("") }
    var mgrConfirmPassword by remember { mutableStateOf("") }
    var mgrPassVisible by remember { mutableStateOf(false) }

    // User Accounts Management States
    var accountSearchQuery by remember { mutableStateOf("") }
    var selectedEmployeeForEdit by remember { mutableStateOf<Employee?>(null) }
    var selectedEmployeeForPasswordReset by remember { mutableStateOf<Employee?>(null) }

    var showResetDialog by remember { mutableStateOf(false) }

    val isGm = currentUser.role == "gm"
    val activeEmpCount = employees.count { it.status == "active" }

    val filteredEmployees = remember(employees, accountSearchQuery) {
        if (accountSearchQuery.isBlank()) employees
        else employees.filter {
            it.name.contains(accountSearchQuery, ignoreCase = true) ||
            it.username.contains(accountSearchQuery, ignoreCase = true) ||
            it.no.contains(accountSearchQuery, ignoreCase = true) ||
            it.mobile.contains(accountSearchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Version & System Information Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_version_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "منظومة إدارة شؤون الموظفين والسلف",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "رقم الإصدار:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "v$APP_VERSION",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "مرخص للشركات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Manager / User Personal Account & Password Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manager_account_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AvatarView(employee = currentUser, size = 44)
                        Column {
                            Text(
                                text = if (isGm) "حساب المدير العام والأمان" else "بيانات الحساب الشخصي",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "تعديل الاسم واسم المستخدم وكلمة المرور الشخصية",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    RoleBadge(role = currentUser.role)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                OutlinedTextField(
                    value = mgrName,
                    onValueChange = { mgrName = it },
                    label = { Text("الاسم الكامل") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = mgrUsername,
                        onValueChange = { mgrUsername = it },
                        label = { Text("اسم المستخدم للدخول") },
                        leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = mgrMobile,
                        onValueChange = { mgrMobile = it },
                        label = { Text("رقم الجوال") },
                        leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = mgrEmail,
                    onValueChange = { mgrEmail = it },
                    label = { Text("البريد الإلكتروني") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Password change section
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                            Text(
                                text = "تغيير الرقم السري / كلمة المرور (اختياري)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = mgrNewPassword,
                            onValueChange = { mgrNewPassword = it },
                            label = { Text("كلمة المرور الجديدة (اتركها فارغة إذا لم ترغب بالتغيير)") },
                            visualTransformation = if (mgrPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { mgrPassVisible = !mgrPassVisible }) {
                                    Icon(
                                        imageVector = if (mgrPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (mgrNewPassword.isNotBlank()) {
                            OutlinedTextField(
                                value = mgrConfirmPassword,
                                onValueChange = { mgrConfirmPassword = it },
                                label = { Text("تأكيد كلمة المرور الجديدة") },
                                visualTransformation = if (mgrPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                isError = mgrConfirmPassword.isNotBlank() && mgrConfirmPassword != mgrNewPassword,
                                supportingText = {
                                    if (mgrConfirmPassword.isNotBlank() && mgrConfirmPassword != mgrNewPassword) {
                                        Text("كلمتا المرور غير متطابقتين", color = AccentRed)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (mgrNewPassword.isNotBlank() && mgrNewPassword != mgrConfirmPassword) {
                            viewModel.showSnackbar("كلمتا المرور غير متطابقتين")
                            return@Button
                        }
                        viewModel.updateEmployeeAccount(
                            empId = currentUser.id,
                            name = mgrName,
                            no = currentUser.no,
                            mobile = mgrMobile,
                            email = mgrEmail,
                            job = currentUser.job,
                            deptId = currentUser.deptId,
                            role = currentUser.role,
                            supId = currentUser.supId,
                            username = mgrUsername,
                            newPasswordRaw = mgrNewPassword.ifBlank { null },
                            status = currentUser.status
                        ) {
                            mgrNewPassword = ""
                            mgrConfirmPassword = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_manager_account_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ وتحديث بيانات الحساب والرقم السري", fontWeight = FontWeight.Bold)
                }
            }
        }

        // All Users & Accounts Password Control Card (GM ONLY)
        if (isGm) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("all_accounts_control_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = "إدارة حسابات وأرقام المستخدمين السرية",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "تعديل فوري لبيانات الدخول وتغيير كلمات السر لجميع الموظفين",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text("${employees.size} حسابات", modifier = Modifier.padding(4.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Search field
                    OutlinedTextField(
                        value = accountSearchQuery,
                        onValueChange = { accountSearchQuery = it },
                        placeholder = { Text("بحث بالاسم، اسم المستخدم، الرقم الوظيفي...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (accountSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { accountSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Employee Account Quick Action Rows
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredEmployees.forEach { emp ->
                            AccountQuickRow(
                                employee = emp,
                                onEditAccount = { selectedEmployeeForEdit = emp },
                                onResetPassword = { selectedEmployeeForPasswordReset = emp }
                            )
                        }
                    }
                }
            }
        }

        // Corporate Subscriptions Card (GM ONLY - Hidden from Employees and Supervisors)
        if (isGm) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("corporate_subscription_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "باقات وتراخيص المنشآت والشركات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = settings.subscriptionPlanName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val orgCode = com.example.util.CompanyLicenseEngine.getOrgCode(settings.companyName)

                    Text(
                        text = "ترخيص معتمد لمنشأة (${settings.companyName}) • رمز المنشأة: $orgCode",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "مفتاح الترخيص: ${settings.licenseKey}\nالسعة الحالية: $activeEmpCount من ${if (settings.maxEmployeesLimit == -1) "غير محدود" else "${settings.maxEmployeesLimit} موظف"} • مشفر ومقترن بهذه المنشأة حصراً 🔒",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onNavigateToPlans,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("view_corporate_plans_button")
                    ) {
                        Icon(Icons.Default.CardMembership, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعراض وتفعيل باقات الشركات والمميزات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // General System Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    Icon(Icons.Default.Settings, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                    Text(
                        text = "إعدادات المنشأة والسياسات المالية",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("اسم المنشأة / الشركة") },
                    enabled = isGm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("رمز العملة") },
                    enabled = isGm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = advanceMaxStr,
                    onValueChange = { advanceMaxStr = it },
                    label = { Text("الحد الأقصى المسموح للسلفة الواحدة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = isGm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = sessionTimeoutStr,
                    onValueChange = { sessionTimeoutStr = it },
                    label = { Text("مهلة انتهاء الجلسة (بالدقائق)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = isGm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Auto Login Option Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الدخول التلقائي عند مطابقة بيانات الموظف المسجلة",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "يسمح بفتح التطبيق تلقائياً على هاتف المستخدم عند مطابقة البيانات المعتمدة من المدير.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = autoLoginEnabled,
                        onCheckedChange = { if (isGm) autoLoginEnabled = it },
                        enabled = isGm,
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandGreen)
                    )
                }

                // GM Policy Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "اشتراط اعتماد المدير العام للخصومات والجزاءات",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "عند التفعيل، لا تُقيد الخصومات المسجلة من المشرفين إلا بعد مراجعة واعتماد المدير العام.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = penaltyNeedsGM,
                        onCheckedChange = { if (isGm) penaltyNeedsGM = it },
                        enabled = isGm,
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandGreen)
                    )
                }

                if (isGm) {
                    Button(
                        onClick = {
                            val newAdvMax = advanceMaxStr.toDoubleOrNull() ?: settings.advanceMax
                            val newTimeout = sessionTimeoutStr.toIntOrNull() ?: settings.sessionTimeout

                            val updated = settings.copy(
                                companyName = companyName,
                                currency = currency,
                                advanceMax = newAdvMax,
                                penaltyNeedsGM = penaltyNeedsGM,
                                sessionTimeout = newTimeout,
                                autoLoginEnabled = autoLoginEnabled
                            )
                            viewModel.updateSettings(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ الإعدادات والسياسات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Database & System Maintenance
        if (isGm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "صيانة قاعدة البيانات المحلية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentRed
                    )
                    Text(
                        text = "يمكنك إعادة تعيين قاعدة البيانات إلى حالتها الأصلية وتحميل البيانات التجريبية الشاملة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة تعيين البيانات الأولية", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Account Logout Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_logout_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = AccentRed, modifier = Modifier.size(24.dp))
                    Column {
                        Text(
                            text = "تسجيل الخروج من الحساب",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentRed
                        )
                        Text(
                            text = "الحساب الحالي: ${currentUser.name} (${currentUser.job})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Text(
                    text = "سيتم إنهاء الجلسة الحالية والعودة إلى شاشة تسجيل الدخول الرئيسية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_logout_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج الآن", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Quick Reset Password Dialog
    selectedEmployeeForPasswordReset?.let { targetEmp ->
        QuickResetPasswordDialog(
            employee = targetEmp,
            onDismiss = { selectedEmployeeForPasswordReset = null },
            onConfirm = { newPass ->
                viewModel.changePassword(targetEmp.id, newPass) {
                    selectedEmployeeForPasswordReset = null
                }
            }
        )
    }

    // Edit Full Employee Account Dialog
    selectedEmployeeForEdit?.let { targetEmp ->
        EditAccountDetailsDialog(
            employee = targetEmp,
            departments = departments,
            supervisors = employees.filter { it.role == "supervisor" || it.role == "gm" },
            onDismiss = { selectedEmployeeForEdit = null },
            onSave = { updatedEmp, newPass ->
                viewModel.updateEmployeeAccount(
                    empId = targetEmp.id,
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
                    selectedEmployeeForEdit = null
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("تأكيد إعادة التعيين", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من رغبتك في إعادة تعيين قاعدة البيانات؟ سيتم استعادة الحسابات التجريبية والأقسام الأساسية.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDatabase()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("نعم، أعد التعيين")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun AccountQuickRow(
    employee: Employee,
    onEditAccount: () -> Unit,
    onResetPassword: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                AvatarView(employee = employee, size = 36)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = employee.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        RoleBadge(role = employee.role)
                    }
                    Text(
                        text = "اسم المستخدم: ${employee.username} • الرقم: ${employee.no}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Quick Change Password Button
                IconButton(
                    onClick = onResetPassword,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFFEF3C7),
                        contentColor = Color(0xFFB45309)
                    )
                ) {
                    Icon(Icons.Outlined.Key, contentDescription = "تغيير الرقم السري", modifier = Modifier.size(18.dp))
                }

                // Edit Account Button
                IconButton(
                    onClick = onEditAccount,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل الحساب", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun QuickResetPasswordDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(32.dp))
        },
        title = {
            Text(
                text = "تغيير الرقم السري لحساب ${employee.name}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "أدخل كلمة المرور الجديدة للمستخدم (اسم المستخدم: ${employee.username}):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("الرقم السري الجديد") },
                    placeholder = { Text("مثال: 123456 أو كلمة سر قوية") },
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(
                                imageVector = if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_password_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Quick presets
                Text(text = "اقتراحات سريعة:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("123456", "Pass@2026", "Emp#1001").forEach { preset ->
                        FilterChip(
                            selected = newPassword == preset,
                            onClick = { newPassword = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newPassword.trim()) },
                enabled = newPassword.trim().length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                modifier = Modifier.testTag("confirm_reset_password_button")
            ) {
                Text("تأكيد وحفظ الرقم السري")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDetailsDialog(
    employee: Employee,
    departments: List<Department>,
    supervisors: List<Employee>,
    onDismiss: () -> Unit,
    onSave: (Employee, String) -> Unit
) {
    var name by remember { mutableStateOf(employee.name) }
    var no by remember { mutableStateOf(employee.no) }
    var mobile by remember { mutableStateOf(employee.mobile) }
    var email by remember { mutableStateOf(employee.email) }
    var deptId by remember { mutableStateOf(employee.deptId) }
    var job by remember { mutableStateOf(employee.job) }
    var role by remember { mutableStateOf(employee.role) }
    var supId by remember { mutableStateOf(employee.supId) }
    var username by remember { mutableStateOf(employee.username) }
    var newPassword by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(employee.status) }
    var passVisible by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && username.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تعديل بيانات الحساب والرقم السري", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = no,
                        onValueChange = { no = it },
                        label = { Text("الرقم الوظيفي") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = job,
                        onValueChange = { job = it },
                        label = { Text("المسمى الوظيفي") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("رقم الجوال") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Role Selector
                Text(text = "نوع الحساب والصلاحية:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("employee" to "موظف", "supervisor" to "مشرف", "gm" to "مدير عام").forEach { (r, lbl) ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(lbl) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Status Selector
                Text(text = "حالة الحساب:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("active" to "نشط", "inactive" to "معطل").forEach { (st, lbl) ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(lbl) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Password Field
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("الرقم السري الجديد (اتركه فارغاً للإبقاء)") },
                    placeholder = { Text("أدخل كلمة مرور جديدة إذا أردت تغييرها") },
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(
                                imageVector = if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val updated = employee.copy(
                            name = name,
                            no = no,
                            mobile = mobile,
                            email = email,
                            job = job,
                            deptId = deptId,
                            role = role,
                            supId = supId,
                            username = username,
                            status = status
                        )
                        onSave(updated, newPassword)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
