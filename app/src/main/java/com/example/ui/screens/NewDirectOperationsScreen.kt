package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import com.example.ui.components.AvatarView
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAdvanceDirectScreen(
    viewModel: MainViewModel,
    currentUser: Employee,
    onSuccess: () -> Unit
) {
    val employees by viewModel.allEmployees.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val targetEmployees = remember(employees, currentUser) {
        if (currentUser.role == "gm") employees.filter { it.status == "active" }
        else employees.filter { it.supId == currentUser.id && it.status == "active" }
    }

    var selectedEmpId by remember { mutableStateOf<Long?>(null) }
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val isFormValid = selectedEmpId != null && amount > 0.0 && reason.isNotBlank()

    val selectedEmployee = remember(selectedEmpId, targetEmployees) {
        targetEmployees.find { it.id == selectedEmpId }
    }

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
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تسجيل سلفة مباشرة على موظف",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Employee Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedEmployee?.let { "${it.name} (${it.no})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الموظف المستفيد *") },
                        placeholder = { Text("اختر الموظف...") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = BrandGreen)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("select_employee_advance"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        targetEmployees.forEach { emp ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AvatarView(employee = emp, size = 32)
                                        Column {
                                            Text(emp.name, fontWeight = FontWeight.Bold)
                                            Text("${emp.job} • رقم: ${emp.no}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedEmpId = emp.id
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ السلفة (${settings.currency}) *") },
                    placeholder = { Text("مثال: 5000") },
                    leadingIcon = {
                        Icon(Icons.Outlined.AttachMoney, contentDescription = null, tint = BrandGreen)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_advance_amount"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Reason Field
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب السلفة *") },
                    placeholder = { Text("مثال: سلفة إيجار / سلفة علاج...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = BrandGreen)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_advance_reason"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات إدارية") },
                    placeholder = { Text("تُحفظ في قيد الحركة...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandGreenLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(20.dp))
                        Text(
                            text = "سيتم قيد السلفة مباشرة في كشف حساب الموظف كحركة معتمدة وإرسال إشعار للموظف.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandGreenDark
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isFormValid && selectedEmpId != null) {
                            isSubmitting = true
                            viewModel.registerDirectAdvance(
                                empId = selectedEmpId!!,
                                amount = amount,
                                reason = reason,
                                note = note
                            ) {
                                isSubmitting = false
                                onSuccess()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_direct_advance_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    enabled = !isSubmitting && isFormValid
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Text("حفظ السلفة وقيدها", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPenaltyDirectScreen(
    viewModel: MainViewModel,
    currentUser: Employee,
    onSuccess: () -> Unit
) {
    val employees by viewModel.allEmployees.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val targetEmployees = remember(employees, currentUser) {
        if (currentUser.role == "gm") employees.filter { it.status == "active" }
        else employees.filter { it.supId == currentUser.id && it.status == "active" }
    }

    var selectedEmpId by remember { mutableStateOf<Long?>(null) }
    var selectedType by remember { mutableStateOf("deduction") }
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isEmpExpanded by remember { mutableStateOf(false) }
    var isTypeExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val isFormValid = selectedEmpId != null && amount > 0.0 && reason.isNotBlank()

    val selectedEmployee = remember(selectedEmpId, targetEmployees) {
        targetEmployees.find { it.id == selectedEmpId }
    }

    val penaltyTypes = listOf(
        "deduction" to "خصم مالي عام",
        "absence" to "غياب بدون إذن",
        "lateness" to "تأخر متكرر",
        "violation" to "مخالفة لائحة العمل",
        "other" to "أخرى"
    )

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
                        imageVector = Icons.Default.RemoveCircle,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تسجيل خصم / جزاء على موظف",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Employee Selection
                ExposedDropdownMenuBox(
                    expanded = isEmpExpanded,
                    onExpandedChange = { isEmpExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedEmployee?.let { "${it.name} (${it.no})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الموظف المعني بالخصم *") },
                        placeholder = { Text("اختر الموظف...") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = AccentRed)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEmpExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("select_employee_penalty"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isEmpExpanded,
                        onDismissRequest = { isEmpExpanded = false }
                    ) {
                        targetEmployees.forEach { emp ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AvatarView(employee = emp, size = 32)
                                        Column {
                                            Text(emp.name, fontWeight = FontWeight.Bold)
                                            Text("${emp.job} • رقم: ${emp.no}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedEmpId = emp.id
                                    isEmpExpanded = false
                                }
                            )
                        }
                    }
                }

                // Type Selection
                ExposedDropdownMenuBox(
                    expanded = isTypeExpanded,
                    onExpandedChange = { isTypeExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = penaltyTypes.find { it.first == selectedType }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع الجزاء *") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Category, contentDescription = null, tint = AccentRed)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isTypeExpanded,
                        onDismissRequest = { isTypeExpanded = false }
                    ) {
                        penaltyTypes.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedType = key
                                    isTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ الخصم (${settings.currency}) *") },
                    placeholder = { Text("مثال: 300") },
                    leadingIcon = {
                        Icon(Icons.Outlined.MoneyOff, contentDescription = null, tint = AccentRed)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("penalty_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Reason Field
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الخصم / المخالفة *") },
                    placeholder = { Text("مثال: تأخر 45 دقيقة عن الحضور...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = AccentRed)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("penalty_reason_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات إضافية") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (settings.penaltyNeedsGM && currentUser.role != "gm") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentAmberLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                            Text(
                                text = "حسب سياسة النظام، سيُحفظ الخصم بحالة «قيد المراجعة» ويتطلب اعتماد المدير العام قبل قيده في كشف الحساب.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentAmber
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isFormValid && selectedEmpId != null) {
                            isSubmitting = true
                            viewModel.registerPenalty(
                                empId = selectedEmpId!!,
                                type = selectedType,
                                amount = amount,
                                reason = reason,
                                note = note
                            ) {
                                isSubmitting = false
                                onSuccess()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_penalty_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    enabled = !isSubmitting && isFormValid
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = null)
                            Text("حفظ الخصم", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
