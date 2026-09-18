package com.example.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.ui.components.AvatarView
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "الرئيسية", Icons.Default.Dashboard)
    object Employees : Screen("employees", "الموظفون", Icons.Default.People)
    object Requests : Screen("requests", "مراجعة الطلبات", Icons.Default.Checklist)
    object Statement : Screen("statement", "كشف الحساب", Icons.Default.ReceiptLong)
    object AdvanceRequest : Screen("advance_request", "طلب سلفة", Icons.Default.Send)
    object MyRequests : Screen("my_requests", "سجل طلباتي", Icons.Default.History)
    object NewAdvance : Screen("new_advance", "سلفة مباشرة", Icons.Default.AccountBalanceWallet)
    object NewPenalty : Screen("new_penalty", "خصم / جزاء", Icons.Default.RemoveCircle)
    object Reports : Screen("reports", "التقارير", Icons.Default.Assessment)
    object Audit : Screen("audit", "سجل التدقيق", Icons.Default.Security)
    object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)
    object CompanyPlans : Screen("company_plans", "باقات الشركات", Icons.Default.WorkspacePremium)
    object Notifications : Screen("notifications", "الإشعارات", Icons.Default.Notifications)
    object More : Screen("more", "المزيد", Icons.Default.MoreHoriz)
    object EmployeeDetail : Screen("employee_detail", "ملف الموظف", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifCount = remember(notifications) { notifications.count { !it.isRead } }

    var currentScreen by remember { mutableStateOf<String>("dashboard") }
    var selectedEmployeeDetailId by remember { mutableStateOf<Long?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observe snackbar messages
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (currentUser == null) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = { currentScreen = "dashboard" }
        )
    } else {
        val user = currentUser!!

        val isRootScreen = when (currentScreen) {
            "dashboard", "employees", "requests", "statement", "my_requests", "advance_request", "more" -> true
            else -> false
        }

        var showMoreSheet by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_scaffold"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AppTopBar(
                    user = user,
                    currentScreen = currentScreen,
                    isRootScreen = isRootScreen,
                    unreadNotifCount = unreadNotifCount,
                    onBack = { currentScreen = "dashboard" },
                    onOpenNotifications = { currentScreen = "notifications" },
                    onLogout = { viewModel.logout() }
                )
            },
            bottomBar = {
                AppBottomBar(
                    userRole = user.role,
                    currentScreen = currentScreen,
                    onNavigate = { route ->
                        if (route == "more") {
                            showMoreSheet = true
                        } else {
                            currentScreen = route
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        onNavigate = { route ->
                            if (route.startsWith("employee/")) {
                                val id = route.removePrefix("employee/").toLongOrNull()
                                if (id != null) {
                                    selectedEmployeeDetailId = id
                                    currentScreen = "employee_detail"
                                }
                            } else {
                                currentScreen = route
                            }
                        }
                    )
                    "employees" -> EmployeesScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        onNavigateToDetail = { id ->
                            selectedEmployeeDetailId = id
                            currentScreen = "employee_detail"
                        }
                    )
                    "requests" -> RequestsReviewScreen(
                        viewModel = viewModel,
                        currentUser = user
                    )
                    "statement" -> StatementScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        initialEmpId = if (user.role == "employee") user.id else null
                    )
                    "advance_request" -> AdvanceRequestScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        onSuccess = { currentScreen = "my_requests" }
                    )
                    "my_requests" -> MyRequestsScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        onNavigateToNewRequest = { currentScreen = "advance_request" }
                    )
                    "new_advance" -> NewAdvanceDirectScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        onSuccess = { currentScreen = "statement" }
                    )
                    "new_penalty" -> NewPenaltyDirectScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        onSuccess = { currentScreen = "requests" }
                    )
                    "reports" -> ReportsScreen(
                        viewModel = viewModel,
                        currentUser = user
                    )
                    "audit" -> AuditScreen(
                        viewModel = viewModel
                    )
                    "settings" -> SettingsScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        onNavigateToPlans = { currentScreen = "company_plans" }
                    )
                    "company_plans", "corporate_plans" -> {
                        if (user.role == "gm") {
                            CompanyPlansScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = "settings" }
                            )
                        } else {
                            currentScreen = "dashboard"
                        }
                    }
                    "notifications" -> NotificationsScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        onNavigateToRequests = { currentScreen = "requests" },
                        onNavigateToStatement = { currentScreen = "statement" },
                        onNavigateToMyRequests = { currentScreen = "my_requests" }
                    )
                    "employee_detail" -> {
                        if (selectedEmployeeDetailId != null) {
                            EmployeeDetailScreen(
                                empId = selectedEmployeeDetailId!!,
                                viewModel = viewModel,
                                currentUser = user,
                                onNavigateBack = { currentScreen = "employees" }
                            )
                        } else {
                            currentScreen = "employees"
                        }
                    }
                    else -> DashboardScreen(
                        viewModel = viewModel,
                        currentUser = user,
                        onNavigate = { currentScreen = it }
                    )
                }
            }
        }

        // More Sheet Modal
        if (showMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "القوائم والعمليات السريعة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (user.role == "gm" || user.role == "supervisor") {
                        MoreMenuItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "تسجيل سلفة مباشرة",
                            subtitle = "قيد سلفة فورية على موظف",
                            color = BrandGreen
                        ) {
                            showMoreSheet = false
                            currentScreen = "new_advance"
                        }

                        MoreMenuItem(
                            icon = Icons.Default.RemoveCircle,
                            title = "تسجيل خصم / جزاء",
                            subtitle = "تسجيل مخالفة أو خصم مالي",
                            color = AccentRed
                        ) {
                            showMoreSheet = false
                            currentScreen = "new_penalty"
                        }

                        MoreMenuItem(
                            icon = Icons.Default.Assessment,
                            title = "التقارير المالية والطباعة",
                            subtitle = "استخراج وتصدير تقارير PDF",
                            color = BrandBlue
                        ) {
                            showMoreSheet = false
                            currentScreen = "reports"
                        }
                    }

                    if (user.role == "gm") {
                        MoreMenuItem(
                            icon = Icons.Default.Security,
                            title = "سجل التدقيق والمراقبة",
                            subtitle = "متابعة كافة حركات النظام",
                            color = AccentAmber
                        ) {
                            showMoreSheet = false
                            currentScreen = "audit"
                        }

                        MoreMenuItem(
                            icon = Icons.Default.WorkspacePremium,
                            title = "باقات وتراخيص الشركات",
                            subtitle = "إدارة باقة المنشأة ومفاتيح التراخيص",
                            color = Color(0xFFD97706)
                        ) {
                            showMoreSheet = false
                            currentScreen = "company_plans"
                        }
                    }

                    MoreMenuItem(
                        icon = Icons.Default.Notifications,
                        title = "الإشعارات ($unreadNotifCount)",
                        subtitle = "تنبيهات السلف والخصومات",
                        color = BrandTealDark
                    ) {
                        showMoreSheet = false
                        currentScreen = "notifications"
                    }

                    MoreMenuItem(
                        icon = Icons.Default.Settings,
                        title = "الإعدادات العامة",
                        subtitle = "اسم المنشأة والعملة والسياسات",
                        color = TextMuted
                    ) {
                        showMoreSheet = false
                        currentScreen = "settings"
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    MoreMenuItem(
                        icon = Icons.Default.Logout,
                        title = "تسجيل الخروج",
                        subtitle = "إنهاء الجلسة الحالية",
                        color = AccentRed
                    ) {
                        showMoreSheet = false
                        viewModel.logout()
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    user: Employee,
    currentScreen: String,
    isRootScreen: Boolean,
    unreadNotifCount: Int,
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    val title = when (currentScreen) {
        "dashboard" -> "لوحة التحكم"
        "employees" -> "دليل الموظفين"
        "requests" -> "مراجعة الطلبات"
        "statement" -> "كشف الحساب"
        "advance_request" -> "طلب سلفة"
        "my_requests" -> "سجل طلباتي"
        "new_advance" -> "سلفة مباشرة"
        "new_penalty" -> "خصم / جزاء"
        "reports" -> "التقارير"
        "audit" -> "سجل التدقيق"
        "settings" -> "الإعدادات"
        "company_plans" -> "باقات وتراخيص الشركات"
        "notifications" -> "الإشعارات"
        "employee_detail" -> "ملف الموظف"
        else -> "مدير الموظفين"
    }

    if (showLogoutDialog) {
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
            title = {
                Text(
                    text = "تسجيل الخروج",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في تسجيل الخروج من حساب ${user.name}؟",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("نعم، تسجيل الخروج", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (!isRootScreen) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    AvatarView(employee = user, size = 34)
                }
            }
        },
        actions = {
            // Notification icon with badge
            IconButton(onClick = onOpenNotifications) {
                BadgedBox(
                    badge = {
                        if (unreadNotifCount > 0) {
                            Badge(
                                containerColor = AccentRed,
                                contentColor = Color.White
                            ) {
                                Text("$unreadNotifCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "الإشعارات",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Quick Role Chip
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (user.role) {
                    "gm" -> BrandGreenLight
                    "supervisor" -> BrandBlueLight
                    else -> AccentAmberLight
                },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = when (user.role) {
                        "gm" -> "مدير عام"
                        "supervisor" -> "مشرف"
                        else -> "موظف"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = when (user.role) {
                        "gm" -> BrandGreenDark
                        "supervisor" -> BrandBlueDark
                        else -> AccentAmber
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Prominent Logout Action Button
            IconButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag("topbar_logout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "تسجيل الخروج",
                    tint = AccentRed,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun AppBottomBar(
    userRole: String,
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        when (userRole) {
            "gm" -> {
                NavigationBarItem(
                    selected = currentScreen == "dashboard",
                    onClick = { onNavigate("dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية") }
                )
                NavigationBarItem(
                    selected = currentScreen == "employees",
                    onClick = { onNavigate("employees") },
                    icon = { Icon(Icons.Default.People, contentDescription = "الموظفون") },
                    label = { Text("الموظفون") }
                )
                NavigationBarItem(
                    selected = currentScreen == "requests",
                    onClick = { onNavigate("requests") },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "الطلبات") },
                    label = { Text("الطلبات") }
                )
                NavigationBarItem(
                    selected = currentScreen == "statement",
                    onClick = { onNavigate("statement") },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "كشوف الحساب") },
                    label = { Text("كشوف الحساب") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate("more") },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "المزيد") },
                    label = { Text("المزيد") }
                )
            }
            "supervisor" -> {
                NavigationBarItem(
                    selected = currentScreen == "dashboard",
                    onClick = { onNavigate("dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية") }
                )
                NavigationBarItem(
                    selected = currentScreen == "employees",
                    onClick = { onNavigate("employees") },
                    icon = { Icon(Icons.Default.Group, contentDescription = "فريق العمل") },
                    label = { Text("فريق العمل") }
                )
                NavigationBarItem(
                    selected = currentScreen == "requests",
                    onClick = { onNavigate("requests") },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "الطلبات") },
                    label = { Text("الطلبات") }
                )
                NavigationBarItem(
                    selected = currentScreen == "statement",
                    onClick = { onNavigate("statement") },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "كشوف الحساب") },
                    label = { Text("كشوف الحساب") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate("more") },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "المزيد") },
                    label = { Text("المزيد") }
                )
            }
            else -> {
                NavigationBarItem(
                    selected = currentScreen == "dashboard",
                    onClick = { onNavigate("dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية") }
                )
                NavigationBarItem(
                    selected = currentScreen == "advance_request",
                    onClick = { onNavigate("advance_request") },
                    icon = { Icon(Icons.Default.Send, contentDescription = "طلب سلفة") },
                    label = { Text("طلب سلفة") }
                )
                NavigationBarItem(
                    selected = currentScreen == "my_requests",
                    onClick = { onNavigate("my_requests") },
                    icon = { Icon(Icons.Default.History, contentDescription = "طلباتي") },
                    label = { Text("طلباتي") }
                )
                NavigationBarItem(
                    selected = currentScreen == "statement",
                    onClick = { onNavigate("statement") },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "كشف حسابي") },
                    label = { Text("كشف حسابي") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate("more") },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "المزيد") },
                    label = { Text("المزيد") }
                )
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
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
    }
}
