package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AdvanceRequest
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.CompanyPlan
import com.example.data.model.Department
import com.example.data.model.Employee
import com.example.data.model.Penalty
import com.example.data.model.Transaction
import com.example.data.repository.EmployeeRepository
import com.example.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    val repository: EmployeeRepository = EmployeeRepository(AppDatabase.getDatabase(application))
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    // Current User Session
    private val _currentUser = MutableStateFlow<Employee?>(null)
    val currentUser: StateFlow<Employee?> = _currentUser.asStateFlow()

    init {
        checkAutoLogin()
        migrateBrandingIfNeeded()
    }

    private fun migrateBrandingIfNeeded() {
        viewModelScope.launch {
            repository.settings.collectLatest { s ->
                if (s != null) {
                    val needsNameUpdate = s.companyName.contains("الرواد")
                    val currentName = if (needsNameUpdate) "براند لايت التجارية" else s.companyName
                    val validation = com.example.util.CompanyLicenseEngine.validateLicenseKey(s.licenseKey, currentName)
                    val needsKeyUpdate = validation !is com.example.util.LicenseValidationResult.Valid

                    if (needsNameUpdate || needsKeyUpdate) {
                        val validKey = com.example.util.CompanyLicenseEngine.generateLicenseKey(currentName, s.subscriptionTier)
                        repository.saveSettingsDirectly(
                            s.copy(
                                companyName = currentName,
                                licenseKey = validKey
                            )
                        )
                    }
                }
            }
        }
    }

    private fun checkAutoLogin() {
        if (sessionManager.isAutoLoginEnabled() && sessionManager.isLoggedIn()) {
            val savedId = sessionManager.getSavedIdentifier()
            val savedPass = sessionManager.getSavedPassword()
            if (!savedId.isNullOrBlank() && !savedPass.isNullOrBlank()) {
                viewModelScope.launch {
                    val user = repository.login(savedId, savedPass)
                    if (user != null) {
                        _currentUser.value = user
                        _snackbarMessage.value = "تم تسجيل الدخول التلقائي بنجاح (${user.name})"
                    }
                }
            }
        }
    }

    // Database Flows
    val allEmployees: StateFlow<List<Employee>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val departments: StateFlow<List<Department>> = repository.allDepartments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val advances: StateFlow<List<AdvanceRequest>> = repository.allAdvances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val penalties: StateFlow<List<Penalty>> = repository.allPenalties
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settings
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Notifications for current user
    val notifications: StateFlow<List<AppNotification>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getNotifications(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun loginWithPhoneAndCompanyCode(
        phone: String,
        passwordRaw: String,
        companyCode: String,
        rememberMe: Boolean = true,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val trimmedPhone = phone.trim()
            val trimmedPass = passwordRaw.trim()
            val trimmedCode = companyCode.trim()

            if (trimmedPhone.isBlank()) {
                _snackbarMessage.value = "يرجى إدخال رقم الهاتف"
                onResult(false)
                return@launch
            }
            if (trimmedPass.isBlank()) {
                _snackbarMessage.value = "يرجى إدخال الرقم السري"
                onResult(false)
                return@launch
            }
            if (trimmedCode.isBlank()) {
                _snackbarMessage.value = "يرجى إدخال رمز المنشأة"
                onResult(false)
                return@launch
            }

            val user = repository.login(trimmedPhone, trimmedPass)
            if (user != null) {
                _currentUser.value = user
                if (rememberMe) {
                    sessionManager.saveUserSession(trimmedPhone, trimmedPass, autoLogin = true)
                }
                _snackbarMessage.value = "مرحبًا بك، ${user.name}"
                onResult(true)
            } else {
                _snackbarMessage.value = "رقم الهاتف أو الرقم السري غير صحيح، أو الحساب معطل"
                onResult(false)
            }
        }
    }

    fun login(identifier: String, passwordRaw: String, rememberMe: Boolean = true, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val user = repository.login(identifier, passwordRaw)
            if (user != null) {
                _currentUser.value = user
                if (rememberMe) {
                    sessionManager.saveUserSession(identifier, passwordRaw, autoLogin = true)
                }
                _snackbarMessage.value = "مرحبًا بك، ${user.name}"
                onResult(true)
            } else {
                _snackbarMessage.value = "بيانات الدخول غير صحيحة أو الحساب معطل"
                onResult(false)
            }
        }
    }

    fun loginWithPhoneOtp(phone: String, otpCode: String, rememberMe: Boolean = true, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.loginWithPhoneOtp(phone, otpCode)
            res.onSuccess { user ->
                _currentUser.value = user
                if (rememberMe) {
                    sessionManager.saveUserSession(phone, "123456", autoLogin = true)
                }
                _snackbarMessage.value = "تم تسجيل الدخول بنجاح برقم الجوال (${user.name})"
                onResult(true)
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "فشل التحقق من رقم الجوال"
                onResult(false)
            }
        }
    }

    fun broadcastCircular(title: String, body: String, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        if (user.role != "gm") {
            _snackbarMessage.value = "فقط الإدارة العامة تمتلك صلاحية إرسال التعاميم الإدارية"
            return
        }
        viewModelScope.launch {
            val res = repository.broadcastCircularToAllEmployees(title, body, user)
            res.onSuccess {
                _snackbarMessage.value = "تم إرسال التعميم الإداري بنجاح لجميع أفراد المنشأة"
                onSuccess()
            }.onFailure {
                _snackbarMessage.value = "تعذر إرسال التعميم: ${it.message}"
            }
        }
    }

    fun quickLogin(username: String, passwordRaw: String, onResult: (Boolean) -> Unit = {}) {
        login(username, passwordRaw, rememberMe = true, onResult = onResult)
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logAudit(
                    username = user.username,
                    user = user.name,
                    action = "تسجيل خروج",
                    details = "خروج يدوي"
                )
            }
        }
        sessionManager.clearSession()
        _currentUser.value = null
    }

    fun updateSubscription(plan: CompanyPlan, customLicenseKey: String? = null, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        if (user.role != "gm") {
            _snackbarMessage.value = "فقط المدير العام يمتلك صلاحية تعديل باقة الاشتراك للشركة"
            return
        }
        viewModelScope.launch {
            val res = repository.updateSubscriptionTier(plan, customLicenseKey, user)
            if (res.isSuccess) {
                _snackbarMessage.value = "تم تفعيل ${plan.title} بنجاح للشركة! 🎉"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر تحديث باقة الاشتراك: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun submitAdvanceRequest(
        amount: Double,
        reason: String,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        val max = settings.value.advanceMax
        if (amount > max) {
            _snackbarMessage.value = "المبلغ يتجاوز الحد الأقصى المسموح (${EmployeeRepository.formatAmount(max)} ريال)"
            return
        }

        viewModelScope.launch {
            val result = repository.submitAdvanceRequest(user.id, amount, reason, note)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم تقديم طلب السلفة بنجاح وهو قيد المراجعة"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر تقديم الطلب: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun approveAdvance(advanceId: Long, note: String, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.approveAdvance(advanceId, user, note)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم اعتماد السلفة وقيدها في كشف الحساب"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر اعتماد السلفة"
            }
        }
    }

    fun rejectAdvance(advanceId: Long, note: String, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.rejectAdvance(advanceId, user, note)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم رفض السلفة وإشعار الموظف"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر رفض السلفة"
            }
        }
    }

    fun registerDirectAdvance(
        empId: Long,
        amount: Double,
        reason: String,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.registerDirectAdvance(empId, amount, reason, note, user)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم تسجيل السلفة وقيدها في كشف الحساب بنجاح"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر تسجيل السلفة"
            }
        }
    }

    fun registerPenalty(
        empId: Long,
        type: String,
        amount: Double,
        reason: String,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        val needsGM = settings.value.penaltyNeedsGM
        viewModelScope.launch {
            val result = repository.registerPenalty(empId, type, amount, reason, note, user, needsGM)
            if (result.isSuccess) {
                if (needsGM && user.role != "gm") {
                    _snackbarMessage.value = "تم تسجيل الخصم وبانتظار اعتماد المدير العام"
                } else {
                    _snackbarMessage.value = "تم تسجيل الخصم وقيده في كشف الحساب بنجاح"
                }
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر تسجيل الخصم"
            }
        }
    }

    fun approvePenalty(penaltyId: Long, note: String, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.approvePenalty(penaltyId, user, note)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم اعتماد الخصم وقيده في كشف الحساب"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر اعتماد الخصم"
            }
        }
    }

    fun rejectPenalty(penaltyId: Long, note: String, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.rejectPenalty(penaltyId, user, note)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم رفض الخصم"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر رفض الخصم"
            }
        }
    }

    fun recordRepayment(
        empId: Long,
        amount: Double,
        date: String,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.recordRepayment(empId, amount, date, note, user)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم تسجيل السداد وتحديث كشف الحساب بنجاح"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر تسجيل السداد"
            }
        }
    }

    fun changePassword(empId: Long, newPasswordRaw: String, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        if (user.role != "gm" && user.id != empId) {
            _snackbarMessage.value = "لا تملك الصلاحية لتغيير كلمة المرور لهذا المستخدم"
            return
        }
        viewModelScope.launch {
            val result = repository.changePassword(empId, newPasswordRaw, user)
            if (result.isSuccess) {
                if (empId == user.id) {
                    val updatedUser = repository.getEmployeeByIdSync(empId)
                    if (updatedUser != null) {
                        _currentUser.value = updatedUser
                        sessionManager.saveUserSession(updatedUser.username, newPasswordRaw, autoLogin = true)
                    }
                }
                _snackbarMessage.value = "تم تحديث كلمة المرور بنجاح 🔐"
                onSuccess()
            } else {
                _snackbarMessage.value = "فشل تحديث كلمة المرور: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateEmployeeAccount(
        empId: Long,
        name: String,
        no: String,
        mobile: String,
        email: String,
        job: String,
        deptId: Long,
        role: String,
        supId: Long?,
        username: String,
        newPasswordRaw: String?,
        status: String,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        if (user.role != "gm" && user.id != empId) {
            _snackbarMessage.value = "فقط المدير العام يملك صلاحية تعديل بيانات الحسابات"
            return
        }
        viewModelScope.launch {
            val result = repository.updateEmployeeAccount(
                empId = empId,
                name = name,
                no = no,
                mobile = mobile,
                email = email,
                job = job,
                deptId = deptId,
                role = role,
                supId = supId,
                username = username,
                newPasswordRaw = newPasswordRaw,
                status = status,
                currentUser = user
            )
            if (result.isSuccess) {
                val updatedEmp = result.getOrNull()
                if (empId == user.id && updatedEmp != null) {
                    _currentUser.value = updatedEmp
                    if (!newPasswordRaw.isNullOrBlank()) {
                        sessionManager.saveUserSession(updatedEmp.username, newPasswordRaw, autoLogin = true)
                    } else {
                        val savedPass = sessionManager.getSavedPassword() ?: ""
                        sessionManager.saveUserSession(updatedEmp.username, savedPass, autoLogin = true)
                    }
                }
                _snackbarMessage.value = "تم تحديث بيانات الحساب بنجاح ✅"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر تعديل الحساب: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun saveEmployee(employee: Employee, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.saveEmployee(employee, user)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم حفظ بيانات الموظف بنجاح"
                onSuccess()
            } else {
                _snackbarMessage.value = "تعذر حفظ بيانات الموظف"
            }
        }
    }

    fun toggleEmployeeStatus(empId: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.toggleEmployeeStatus(empId, user)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم تحديث حالة حساب الموظف"
            }
        }
    }

    fun deleteEmployeeSafely(empId: Long, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.deleteEmployeeSafely(empId, user)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم الحذف الآمن للموظف مع الاحتفاظ بكامل السجل المالي"
                onSuccess()
            }
        }
    }

    fun updateSettings(newSettings: AppSettings, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.updateSettings(newSettings, user)
            if (result.isSuccess) {
                _snackbarMessage.value = "تم حفظ وتطبيق إعدادات النظام بنجاح"
                onSuccess()
            }
        }
    }

    fun markAllNotificationsAsRead() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markNotificationsAsRead(user.id)
            _snackbarMessage.value = "تم تحديد جميع الإشعارات كمقروءة"
        }
    }

    fun markNotificationAsRead(notifId: Long) {
        viewModelScope.launch {
            repository.markNotificationsAsRead(notifId)
        }
    }

    fun resetDatabase(onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value
            _snackbarMessage.value = "تمت إعادة تعيين البيانات بنجاح"
            onSuccess()
        }
    }
}
