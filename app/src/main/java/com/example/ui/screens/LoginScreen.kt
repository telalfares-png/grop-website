package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.APP_VERSION
import com.example.ui.MainViewModel
import com.example.ui.components.AvatarView
import com.example.ui.theme.*
import com.example.util.CompanyLicenseEngine
import com.example.util.PhoneAuthHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val allEmployees by viewModel.allEmployees.collectAsState()
    val departments by viewModel.departments.collectAsState()

    val defaultCompanyCode = remember(settings.companyName) {
        CompanyLicenseEngine.extractOrgPrefix(settings.companyName).ifBlank { "1001" }
    }

    // Three Core Login Fields
    var companyCode by remember { mutableStateOf(defaultCompanyCode) }
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var rememberMe by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Real-time matched employee by entered phone
    val matchedEmployee = remember(phoneInput, allEmployees) {
        val norm = PhoneAuthHelper.normalizePhoneNumber(phoneInput)
        if (norm.length >= 9) {
            allEmployees.find { PhoneAuthHelper.doesPhoneMatch(norm, it.mobile) }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandSlate, BrandTealDark, BrandSlateDark)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // App Brand Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(BrandGreen, BrandBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_launcher_icon_1787459777152),
                    contentDescription = "شعار المنظومة",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "مدير الموظفين",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )

            Text(
                text = "منظومة ${settings.companyName.ifBlank { "براند لايت التجارية" }} للربط المؤسسي والإداري",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFC0D4E3),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            // Status Badge
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399))
                    )
                    Text(
                        text = "تسجيل الدخول الموحد للمنشأة • v$APP_VERSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Core Login Card (Phone + Password + Company Code)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "تسجيل الدخول",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "أدخل بيانات الدخول المعتمدة لمنشأتك",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(14.dp))
                                Text("نظام آمن 🔒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 1. رمز المنشأة (Company Code)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "رمز المنشأة",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "رمز المنظومة: $defaultCompanyCode",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = companyCode,
                            onValueChange = { companyCode = it },
                            placeholder = { Text("أدخل رمز المنشأة (مثال: $defaultCompanyCode)") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Domain, contentDescription = null, tint = BrandGreen)
                            },
                            trailingIcon = {
                                if (companyCode.isNotBlank() && companyCode != defaultCompanyCode) {
                                    IconButton(onClick = { companyCode = defaultCompanyCode }) {
                                        Icon(Icons.Default.Restore, contentDescription = "استعادة الرمز الافتراضي", modifier = Modifier.size(18.dp), tint = BrandBlue)
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("company_code_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 2. رقم الهاتف (Phone Number)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "رقم الهاتف",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = { Text("05XXXXXXXX") },
                            leadingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🇸🇦 +966", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                                }
                            },
                            trailingIcon = {
                                if (phoneInput.isNotBlank()) {
                                    IconButton(onClick = { phoneInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_login_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Real-time matched employee banner
                        AnimatedVisibility(
                            visible = matchedEmployee != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            if (matchedEmployee != null) {
                                val empDept = departments.find { it.id == matchedEmployee.deptId }
                                Surface(
                                    color = Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(10.dp),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.horizontalGradient(listOf(Color(0xFF86EFAC), Color(0xFF22C55E))),
                                        width = 1.dp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AvatarView(employee = matchedEmployee, size = 32)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = matchedEmployee.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF14532D)
                                                )
                                                Surface(
                                                    color = if (matchedEmployee.role == "gm") Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = when (matchedEmployee.role) {
                                                            "gm" -> "👑 المدير العام"
                                                            "supervisor" -> "👔 مشرف"
                                                            else -> "💼 موظف"
                                                        },
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (matchedEmployee.role == "gm") Color(0xFF92400E) else Color(0xFF166534),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${matchedEmployee.job} • ${empDept?.name ?: "الإدارة العامة"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF166534),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. الرقم السري (Password / Secret Code)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "الرقم السري",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            placeholder = { Text("••••••••") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = BrandGreen)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "إخفاء" else "إظهار",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Remember Me & Corporate Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = BrandGreen)
                            )
                            Text(
                                text = "حفظ بيانات الدخول",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "ربط مؤسسي معتمد 🏛️",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Login Action Button
                    Button(
                        onClick = {
                            if (phoneInput.isNotBlank() && passwordInput.isNotBlank() && companyCode.isNotBlank()) {
                                isLoading = true
                                viewModel.loginWithPhoneAndCompanyCode(
                                    phone = phoneInput,
                                    passwordRaw = passwordInput,
                                    companyCode = companyCode,
                                    rememberMe = rememberMe
                                ) { success ->
                                    isLoading = false
                                    if (success) onLoginSuccess()
                                }
                            } else {
                                if (companyCode.isBlank()) viewModel.showSnackbar("يرجى إدخال رمز المنشأة")
                                else if (phoneInput.isBlank()) viewModel.showSnackbar("يرجى إدخال رقم الهاتف")
                                else if (passwordInput.isBlank()) viewModel.showSnackbar("يرجى إدخال الرقم السري")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        enabled = !isLoading && phoneInput.isNotBlank() && passwordInput.isNotBlank() && companyCode.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Text("تسجيل الدخول للمنظومة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Demo Accounts to test Phone + Secret Code + Company Code instantly
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(20.dp))
                            Text(
                                text = "حسابات أفراد المنشأة التجريبية",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        Text(
                            text = "نقرة للتعبئة والدخول",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA7F3D0),
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = "اختر الحساب لتعبئة رقم الهاتف والرقم السري ورمز المنشأة تلقائياً:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0)),
                        fontSize = 11.sp
                    )

                    PhoneAuthHelper.DEMO_ORGANIZATION_ACCOUNTS.forEach { demo ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (demo.isGm) Color(0xFF1E293B) else Color(0x33000000),
                            border = if (demo.isGm) CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                                width = 1.dp
                            ) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    companyCode = defaultCompanyCode
                                    phoneInput = demo.mobile
                                    passwordInput = demo.defaultPass
                                    viewModel.loginWithPhoneAndCompanyCode(
                                        phone = demo.mobile,
                                        passwordRaw = demo.defaultPass,
                                        companyCode = defaultCompanyCode,
                                        rememberMe = true
                                    ) { success ->
                                        if (success) onLoginSuccess()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = demo.roleBadge,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (demo.isGm) Color(0xFFFBBF24) else Color.White
                                    )
                                    Column {
                                        Text(
                                            text = demo.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                        Text(
                                            text = "📱 ${demo.mobile} • كلمة المرور: ${demo.defaultPass}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1), fontSize = 10.sp)
                                        )
                                    }
                                }

                                Surface(
                                    color = BrandGreen,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "دخول فوري ⚡",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Footer
            Text(
                text = "براند لايت التجارية • الإصدار $APP_VERSION • جميع الحقوق محفوظة",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
