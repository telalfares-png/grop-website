package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.model.Employee
import com.example.data.repository.EmployeeRepository
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(
    viewModel: MainViewModel,
    currentUser: Employee,
    onNavigateToDetail: (Long) -> Unit
) {
    val employees by viewModel.allEmployees.collectAsState()
    val departments by viewModel.departments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedDeptId by remember { mutableStateOf<Long?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }
    var employeeToResetPassword by remember { mutableStateOf<Employee?>(null) }

    var employeeToToggle by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }

    val isGm = currentUser.role == "gm"

    val filteredList = remember(employees, searchQuery, selectedDeptId, selectedStatus, currentUser) {
        var list = if (isGm) employees
        else employees.filter { it.supId == currentUser.id }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            list = list.filter { it.name.contains(q, ignoreCase = true) || it.no.contains(q) || it.job.contains(q, ignoreCase = true) }
        }
        if (selectedDeptId != null) {
            list = list.filter { it.deptId == selectedDeptId }
        }
        if (selectedStatus != null) {
            list = list.filter { it.status == selectedStatus }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Compact Persistent Search & Filter Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "بحث بالاسم أو الرقم الوظيفي...",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        maxLines = 1,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("employee_search_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (isGm) {
                        Button(
                            onClick = {
                                employeeToEdit = null
                                showAddEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("add_employee_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة موظف", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Compact Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("كل الحالات", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(32.dp)
                    )
                    FilterChip(
                        selected = selectedStatus == "active",
                        onClick = { selectedStatus = if (selectedStatus == "active") null else "active" },
                        label = { Text("نشط", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(32.dp)
                    )
                    FilterChip(
                        selected = selectedStatus == "inactive",
                        onClick = { selectedStatus = if (selectedStatus == "inactive") null else "inactive" },
                        label = { Text("معطل", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }

        // Employee Count Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isGm) "إدارة الموظفين (${filteredList.size})" else "موظفوك (${filteredList.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Employees List (Takes remaining height and scrolls independently)
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                EmptyStateCard(
                    title = "لا توجد نتائج مطابقة",
                    message = "لم يتم العثور على أي موظف مطابق للبحث أو الفلتر",
                    icon = Icons.Default.PeopleOutline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredList, key = { it.id }) { emp ->
                    val dept = departments.find { it.id == emp.deptId }
                    val supervisor = employees.find { it.id == emp.supId }

                    EmployeeRowCard(
                        employee = emp,
                        departmentName = dept?.name ?: "—",
                        supervisorName = supervisor?.name ?: "—",
                        isGm = isGm,
                        currentUserId = currentUser.id,
                        onClick = { onNavigateToDetail(emp.id) },
                        onEdit = {
                            employeeToEdit = emp
                            showAddEditDialog = true
                        },
                        onResetPassword = {
                            employeeToResetPassword = emp
                        },
                        onToggleStatus = { employeeToToggle = emp },
                        onDelete = { employeeToDelete = emp }
                    )
                }
            }
        }
    }

    // Quick Reset Password Dialog
    employeeToResetPassword?.let { targetEmp ->
        QuickResetPasswordDialog(
            employee = targetEmp,
            onDismiss = { employeeToResetPassword = null },
            onConfirm = { newPass ->
                viewModel.changePassword(targetEmp.id, newPass) {
                    employeeToResetPassword = null
                }
            }
        )
    }

    // Toggle Status Confirmation Dialog
    if (employeeToToggle != null) {
        val emp = employeeToToggle!!
        val isNowActive = emp.status == "active"
        AlertDialog(
            onDismissRequest = { employeeToToggle = null },
            title = {
                Text(
                    text = if (isNowActive) "تعطيل حساب ${emp.name}" else "تفعيل حساب ${emp.name}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isNowActive)
                        "سيتم تعطيل الحساب ومنع الموظف من تسجيل الدخول.\n✔ لن تُحذف أي بيانات مالية أو حركات تاريخية."
                    else "سيتم إعادة تفعيل الحساب وتمكين الموظف من تسجيل الدخول إلى النظام."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleEmployeeStatus(emp.id)
                        employeeToToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNowActive) AccentRed else BrandGreen
                    )
                ) {
                    Text(if (isNowActive) "تأكيد التعطيل" else "تأكيد التفعيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToToggle = null }) { Text("إلغاء") }
            }
        )
    }

    // Safe Delete Confirmation Dialog
    if (employeeToDelete != null) {
        val emp = employeeToDelete!!
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("حذف الموظف (حذف آمن)", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "سيتم إخفاء حساب ${emp.name} من القوائم النشطة.\n\n✔ سيتم الاحتفاظ بكامل سجله المالي والحركات التاريخية دون أي فقدان في قاعدة البيانات."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployeeSafely(emp.id)
                        employeeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("تأكيد الحذف الآمن")
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Add / Edit Employee Dialog
    if (showAddEditDialog) {
        AddEditEmployeeDialog(
            employee = employeeToEdit,
            departments = departments,
            supervisors = employees.filter { it.role == "supervisor" || it.role == "gm" },
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedEmp ->
                viewModel.saveEmployee(updatedEmp)
                showAddEditDialog = false
            }
        )
    }
}

@Composable
private fun EmployeeRowCard(
    employee: Employee,
    departmentName: String,
    supervisorName: String,
    isGm: Boolean,
    currentUserId: Long,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("employee_row_${employee.id}"),
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AvatarView(employee = employee, size = 42)
                    Column {
                        Row(
                             verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = employee.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusBadge(status = employee.status)
                        }
                        Text(
                            text = "${employee.job} • رقم: ${employee.no} • اسم الدخول: ${employee.username}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isGm) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = onResetPassword, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Key, contentDescription = "تغيير الرقم السري", tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل الحساب", tint = BrandBlue, modifier = Modifier.size(18.dp))
                        }
                        if (employee.id != currentUserId) {
                            IconButton(onClick = onToggleStatus, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = if (employee.status == "active") Icons.Default.Block else Icons.Default.CheckCircle,
                                    contentDescription = "تغيير الحالة",
                                    tint = if (employee.status == "active") AccentAmber else BrandGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AccentRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "القسم: $departmentName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "المشرف: $supervisorName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditEmployeeDialog(
    employee: Employee?,
    departments: List<com.example.data.model.Department>,
    supervisors: List<Employee>,
    onDismiss: () -> Unit,
    onSave: (Employee) -> Unit
) {
    var name by remember { mutableStateOf(employee?.name.orEmpty()) }
    var no by remember { mutableStateOf(employee?.no.orEmpty()) }
    var mobile by remember { mutableStateOf(employee?.mobile.orEmpty()) }
    var email by remember { mutableStateOf(employee?.email.orEmpty()) }
    var deptId by remember { mutableStateOf(employee?.deptId ?: (departments.firstOrNull()?.id ?: 1L)) }
    var job by remember { mutableStateOf(employee?.job.orEmpty()) }
    var role by remember { mutableStateOf(employee?.role ?: "employee") }
    var supId by remember { mutableStateOf(employee?.supId) }
    var username by remember { mutableStateOf(employee?.username.orEmpty()) }
    var password by remember { mutableStateOf("") }

    val isEdit = employee != null
    val isValid = name.isNotBlank() && username.isNotBlank() && (isEdit || password.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "تعديل بيانات الموظف" else "إضافة موظف جديد", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم الكامل *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = no,
                        onValueChange = { no = it },
                        label = { Text("الرقم الوظيفي") },
                        placeholder = { Text("يُولد آليًا إذا تُرك فارغًا") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = job,
                        onValueChange = { job = it },
                        label = { Text("المسمى الوظيفي") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("رقم الجوال") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم للدخول *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isEdit) "كلمة المرور (اتركها فارغة للإبقاء)" else "كلمة المرور *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val passHash = if (password.isNotBlank()) AppDatabase.hashPassword(password)
                        else employee?.passwordHash ?: AppDatabase.hashPassword("123456")

                        val newEmp = Employee(
                            id = employee?.id ?: 0L,
                            no = no,
                            name = name,
                            mobile = mobile,
                            email = email,
                            deptId = deptId,
                            job = job,
                            hireDate = employee?.hireDate ?: EmployeeRepository.getCurrentDateString(),
                            supId = supId,
                            role = role,
                            username = username,
                            passwordHash = passHash,
                            status = employee?.status ?: "active"
                        )
                        onSave(newEmp)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text("حفظ البيانات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
