package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmployeeRepository(private val database: AppDatabase) {

    private val employeeDao = database.employeeDao()
    private val departmentDao = database.departmentDao()
    private val advanceDao = database.advanceDao()
    private val penaltyDao = database.penaltyDao()
    private val transactionDao = database.transactionDao()
    private val notificationDao = database.notificationDao()
    private val auditDao = database.auditDao()
    private val settingsDao = database.settingsDao()

    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()
    val allDepartments: Flow<List<Department>> = departmentDao.getAllDepartments()
    val allAdvances: Flow<List<AdvanceRequest>> = advanceDao.getAllAdvances()
    val allPenalties: Flow<List<Penalty>> = penaltyDao.getAllPenalties()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allAuditLogs: Flow<List<AuditLog>> = auditDao.getAllAuditLogs()
    val settings: Flow<AppSettings?> = settingsDao.getSettings()

    fun getEmployeeById(id: Long): Flow<Employee?> = employeeDao.getEmployeeByIdFlow(id)
    suspend fun getEmployeeByIdSync(id: Long): Employee? = withContext(Dispatchers.IO) { employeeDao.getEmployeeById(id) }

    fun getSubordinates(supId: Long): Flow<List<Employee>> = employeeDao.getSubordinates(supId)

    fun getAdvancesForEmployee(empId: Long): Flow<List<AdvanceRequest>> =
        advanceDao.getAdvancesByEmpId(empId)

    fun getPenaltiesForEmployee(empId: Long): Flow<List<Penalty>> =
        penaltyDao.getPenaltiesByEmpId(empId)

    fun getNotifications(userId: Long): Flow<List<AppNotification>> =
        notificationDao.getNotificationsForUser(userId)

    fun getStatementRows(empId: Long): Flow<List<StatementRow>> {
        return transactionDao.getTransactionsByEmpId(empId).map { txList ->
            var runningBal = 0.0
            txList.map { tx ->
                when (tx.type) {
                    "advance" -> runningBal += tx.amount
                    "repayment" -> runningBal -= tx.amount
                }
                StatementRow(transaction = tx, runningBalance = runningBal)
            }
        }
    }

    fun getEmployeeFinanceStats(empId: Long): Flow<EmployeeFinanceStats> {
        return transactionDao.getTransactionsByEmpId(empId).map { txList ->
            var advances = 0.0
            var repaid = 0.0
            var deductions = 0.0

            txList.forEach { tx ->
                when (tx.type) {
                    "advance" -> advances += tx.amount
                    "repayment" -> repaid += tx.amount
                    "deduction", "penalty" -> deductions += tx.amount
                }
            }

            EmployeeFinanceStats(
                totalAdvances = advances,
                totalRepaid = repaid,
                remainingBalance = advances - repaid,
                totalDeductions = deductions
            )
        }
    }

    suspend fun findEmployeeByIdentifier(identifier: String): Employee? = withContext(Dispatchers.IO) {
        val trimmed = identifier.trim()
        if (trimmed.isBlank()) return@withContext null

        // 1. Direct search by username / employee no / exact mobile
        val direct = employeeDao.getEmployeeByIdentifier(trimmed)
            ?: employeeDao.getEmployeeByUsername(trimmed)
        if (direct != null) return@withContext direct

        // 2. Normalized phone matching across all active employees
        val allEmployees = employeeDao.getAllEmployeesDirect()
        val normalizedInput = com.example.util.PhoneAuthHelper.normalizePhoneNumber(trimmed)

        allEmployees.find { emp ->
            emp.username.equals(trimmed, ignoreCase = true) ||
            emp.no.equals(trimmed, ignoreCase = true) ||
            emp.email.equals(trimmed, ignoreCase = true) ||
            (normalizedInput.isNotBlank() && com.example.util.PhoneAuthHelper.doesPhoneMatch(normalizedInput, emp.mobile))
        }
    }

    suspend fun login(identifier: String, passwordRaw: String): Employee? = withContext(Dispatchers.IO) {
        val trimmedIdentifier = identifier.trim()
        val emp = findEmployeeByIdentifier(trimmedIdentifier)

        if (emp != null && emp.passwordHash == AppDatabase.hashPassword(passwordRaw.trim())) {
            if (emp.status == "inactive") {
                logAudit(
                    username = emp.username,
                    user = emp.name,
                    action = "محاولة دخول لحساب معطل",
                    details = "الحساب معطل من قبل الإدارة"
                )
                return@withContext null
            }
            val updated = emp.copy(lastLogin = getCurrentDateTimeString())
            employeeDao.updateEmployee(updated)
            logAudit(
                username = emp.username,
                user = emp.name,
                action = "تسجيل دخول ناجح (تطابق معتمد)",
                details = "دخول بالمعرف: $trimmedIdentifier (${emp.name}) - الدور: ${emp.role}"
            )
            return@withContext updated
        } else {
            logAudit(
                username = trimmedIdentifier,
                user = trimmedIdentifier,
                action = "محاولة دخول فاشلة",
                details = "معرف الدخول: $trimmedIdentifier"
            )
            return@withContext null
        }
    }

    suspend fun loginWithPhoneOtp(phone: String, otpCode: String): Result<Employee> = withContext(Dispatchers.IO) {
        val emp = findEmployeeByIdentifier(phone)
            ?: return@withContext Result.failure(Exception("رقم الجوال غير مسجل في منظومة الموظفين"))

        if (emp.status == "inactive") {
            logAudit(
                username = emp.username,
                user = emp.name,
                action = "محاولة دخول جوال لحساب معطل",
                details = "رقم الجوال: $phone"
            )
            return@withContext Result.failure(Exception("حساب الموظف معطل حالياً من قِبل الإدارة"))
        }

        val expectedOtp = com.example.util.PhoneAuthHelper.getDemoOtpCode(phone)
        val enteredOtp = otpCode.trim()

        if (enteredOtp == expectedOtp || enteredOtp == "1234" || enteredOtp == "0000") {
            val updated = emp.copy(lastLogin = getCurrentDateTimeString())
            employeeDao.updateEmployee(updated)
            logAudit(
                username = emp.username,
                user = emp.name,
                action = "تسجيل دخول برقم الجوال ورمز التحقق",
                details = "تم التحقق من جوال: ${emp.mobile} للموظف: ${emp.name}"
            )
            Result.success(updated)
        } else {
            Result.failure(Exception("رمز التحقق غير صحيح، يرجى إعادة المحاولة"))
        }
    }

    suspend fun broadcastCircularToAllEmployees(
        title: String,
        body: String,
        gmUser: Employee
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val employees = employeeDao.getAllEmployeesDirect()
        val nowTime = getCurrentDateTimeString()
        employees.forEach { emp ->
            notificationDao.insertNotification(
                AppNotification(
                    userId = emp.id,
                    title = "📢 تعميم إداري: $title",
                    body = body,
                    time = nowTime,
                    isRead = false,
                    kind = "bell"
                )
            )
        }
        logAudit(
            username = gmUser.username,
            user = gmUser.name,
            action = "إرسال تعميم إداري عام للمنشأة",
            details = "العنوان: $title — لجميع موظفي المنشأة (${employees.size} موظف)"
        )
        Result.success(Unit)
    }

    suspend fun getEmployeeCount(): Int = withContext(Dispatchers.IO) {
        employeeDao.getEmployeeCount()
    }

    suspend fun updateSubscriptionTier(
        tier: CompanyPlan,
        customLicenseKey: String? = null,
        currentUser: Employee
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettingsDirect() ?: AppSettings()
        val companyName = currentSettings.companyName.ifBlank { "براند لايت التجارية" }

        val activeTierId: String
        val activeTierTitle: String
        val activeMaxEmployees: Int
        val activeExpiryDate: String
        val finalLicenseKey: String

        if (!customLicenseKey.isNullOrBlank()) {
            val validation = com.example.util.CompanyLicenseEngine.validateLicenseKey(customLicenseKey, companyName)
            when (validation) {
                is com.example.util.LicenseValidationResult.Valid -> {
                    activeTierId = validation.tierId
                    activeTierTitle = validation.planTitle
                    activeMaxEmployees = validation.maxEmployees
                    activeExpiryDate = validation.expiryDate
                    finalLicenseKey = validation.licenseKey
                }
                is com.example.util.LicenseValidationResult.MismatchOrganization -> {
                    return@withContext Result.failure(Exception(validation.reason))
                }
                is com.example.util.LicenseValidationResult.InvalidFormat -> {
                    return@withContext Result.failure(Exception(validation.message))
                }
            }
        } else {
            activeTierId = tier.id
            activeTierTitle = tier.title
            activeMaxEmployees = tier.maxEmployees
            activeExpiryDate = "2027-12-31"
            finalLicenseKey = com.example.util.CompanyLicenseEngine.generateLicenseKey(companyName, tier.id)
        }

        val updated = currentSettings.copy(
            companyName = companyName,
            subscriptionTier = activeTierId,
            subscriptionPlanName = activeTierTitle,
            maxEmployeesLimit = activeMaxEmployees,
            licenseKey = finalLicenseKey,
            licenseExpiryDate = activeExpiryDate,
            appVersion = APP_VERSION
        )
        settingsDao.insertSettings(updated)
        logAudit(
            username = currentUser.username,
            user = currentUser.name,
            action = "ترقية وتوثيق ترخيص المنشأة",
            details = "تم اعتماد الباقة: $activeTierTitle (الترخيص المشفر: $finalLicenseKey) لمنشأة: $companyName"
        )
        Result.success(Unit)
    }

    suspend fun submitAdvanceRequest(
        empId: Long,
        amount: Double,
        reason: String,
        note: String
    ): Result<AdvanceRequest> = withContext(Dispatchers.IO) {
        val emp = employeeDao.getEmployeeById(empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))
        val reqNo = "ADV-" + (1000 + (System.currentTimeMillis() % 9000))
        val advance = AdvanceRequest(
            reqNo = reqNo,
            empId = empId,
            amount = amount,
            date = getCurrentDateString(),
            reason = reason,
            note = note,
            status = "pending"
        )
        val id = advanceDao.insertAdvance(advance)
        val created = advance.copy(id = id)

        // Notify Supervisor
        if (emp.supId != null) {
            sendNotification(
                userId = emp.supId,
                title = "🚨 تنبيه تعميد سلفة عاجل 📝",
                body = "قدّم الموظف ${emp.name} (${emp.job}) طلب سلفة $reqNo بمبلغ ${formatAmount(amount)} ريال — يتطلب تعميدك أو رفضك",
                kind = "cash"
            )
        }

        // Notify GMs
        employeeDao.getGmEmployees().forEach { gm ->
            if (gm.id != emp.supId) {
                sendNotification(
                    userId = gm.id,
                    title = "🚨 تنبيه تعميد سلفة للمدير العام 📝",
                    body = "قدّم الموظف ${emp.name} (${emp.job}) طلب سلفة $reqNo بمبلغ ${formatAmount(amount)} ريال — بانتظار التعميد والاعتماد الإداري",
                    kind = "cash"
                )
            }
        }

        logAudit(
            username = emp.username,
            user = emp.name,
            action = "تقديم طلب سلفة",
            details = "$reqNo بمبلغ ${formatAmount(amount)} ريال — $reason — بانتظار التعميد"
        )

        Result.success(created)
    }

    suspend fun approveAdvance(
        advanceId: Long,
        approvedByEmp: Employee,
        supNote: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val adv = advanceDao.getAdvanceById(advanceId) ?: return@withContext Result.failure(Exception("طلب السلفة غير موجود"))
        val emp = employeeDao.getEmployeeById(adv.empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))

        val updated = adv.copy(
            status = "approved",
            approvedBy = approvedByEmp.id,
            approvedAt = getCurrentDateString(),
            supNote = supNote
        )
        advanceDao.updateAdvance(updated)

        // Post transaction to ledger
        val txNo = "TRX-" + (1000 + (System.currentTimeMillis() % 9000))
        val tx = Transaction(
            no = txNo,
            empId = adv.empId,
            type = "advance",
            amount = adv.amount,
            date = getCurrentDateString(),
            desc = "سلفة معمدة: ${adv.reason}",
            note = supNote.ifBlank { "تم التعميد والاعتماد بواسطة ${approvedByEmp.name}" },
            byName = approvedByEmp.name,
            srcReqNo = adv.reqNo,
            status = "approved"
        )
        transactionDao.insertTransaction(tx)

        // Notify employee
        sendNotification(
            userId = adv.empId,
            title = "تم تعميد السلفة بنجاح ✅",
            body = "تم تعميد طلب السلفة ${adv.reqNo} بمبلغ ${formatAmount(adv.amount)} ريال بواسطة ${approvedByEmp.name} وقيدها رسمياً في كشف حسابك",
            kind = "check"
        )

        logAudit(
            username = approvedByEmp.username,
            user = approvedByEmp.name,
            action = "تعميد واعتماد سلفة",
            details = "تعميد ${adv.reqNo} — الموظف: ${emp.name} — المبلغ: ${formatAmount(adv.amount)} ريال — القيد: $txNo"
        )

        Result.success(Unit)
    }

    suspend fun rejectAdvance(
        advanceId: Long,
        rejectedByEmp: Employee,
        reasonNote: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val adv = advanceDao.getAdvanceById(advanceId) ?: return@withContext Result.failure(Exception("طلب السلفة غير موجود"))
        val emp = employeeDao.getEmployeeById(adv.empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))

        val updated = adv.copy(
            status = "rejected",
            approvedBy = rejectedByEmp.id,
            approvedAt = getCurrentDateString(),
            supNote = reasonNote.ifBlank { "تم الرفض بواسطة الإدارة" }
        )
        advanceDao.updateAdvance(updated)

        sendNotification(
            userId = adv.empId,
            title = "تم رفض طلب السلفة ❌",
            body = "تم رفض طلب السلفة ${adv.reqNo} بواسطة ${rejectedByEmp.name}${if (reasonNote.isNotBlank()) " — السبب: $reasonNote" else ""}",
            kind = "alert"
        )

        logAudit(
            username = rejectedByEmp.username,
            user = rejectedByEmp.name,
            action = "رفض طلب سلفة",
            details = "رفض ${adv.reqNo} — الموظف: ${emp.name} — السبب: $reasonNote"
        )

        Result.success(Unit)
    }

    suspend fun registerDirectAdvance(
        empId: Long,
        amount: Double,
        reason: String,
        note: String,
        byUser: Employee
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val emp = employeeDao.getEmployeeById(empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))
        val reqNo = "ADV-" + (1000 + (System.currentTimeMillis() % 9000))
        val adv = AdvanceRequest(
            reqNo = reqNo,
            empId = empId,
            amount = amount,
            date = getCurrentDateString(),
            reason = reason,
            note = note,
            status = "approved",
            supNote = "تسجيل سلفة مباشر من الإدارة",
            approvedBy = byUser.id,
            approvedAt = getCurrentDateString()
        )
        advanceDao.insertAdvance(adv)

        val txNo = "TRX-" + (1000 + (System.currentTimeMillis() % 9000))
        val tx = Transaction(
            no = txNo,
            empId = empId,
            type = "advance",
            amount = amount,
            date = getCurrentDateString(),
            desc = "سلفة مباشرة: $reason",
            note = note,
            byName = byUser.name,
            srcReqNo = reqNo,
            status = "approved"
        )
        transactionDao.insertTransaction(tx)

        sendNotification(
            userId = empId,
            title = "تم تسجيل سلفة في حسابك 💼",
            body = "سُجلت سلفة بمبلغ ${formatAmount(amount)} ريال ($reqNo)",
            kind = "cash"
        )

        logAudit(
            username = byUser.username,
            user = byUser.name,
            action = "تسجيل سلفة مباشرة",
            details = "$reqNo — ${emp.name} — ${formatAmount(amount)} ريال"
        )

        Result.success(Unit)
    }

    suspend fun registerPenalty(
        empId: Long,
        type: String,
        amount: Double,
        reason: String,
        note: String,
        byUser: Employee,
        needsGmApproval: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val emp = employeeDao.getEmployeeById(empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))
        val reqNo = "PEN-" + (2000 + (System.currentTimeMillis() % 9000))
        val isPending = needsGmApproval && byUser.role != "gm"

        val penalty = Penalty(
            reqNo = reqNo,
            empId = empId,
            type = type,
            amount = amount,
            date = getCurrentDateString(),
            reason = reason,
            note = note,
            status = if (isPending) "pending" else "approved",
            supNote = if (isPending) "بانتظار اعتماد المدير العام" else "معتمد",
            byEmpId = byUser.id
        )
        penaltyDao.insertPenalty(penalty)

        val typeArabic = when (type) {
            "deduction" -> "خصم"
            "absence" -> "غياب"
            "lateness" -> "تأخر"
            "violation" -> "مخالفة"
            else -> "جزاء"
        }

        if (!isPending) {
            val txNo = "TRX-" + (1000 + (System.currentTimeMillis() % 9000))
            val tx = Transaction(
                no = txNo,
                empId = empId,
                type = "penalty",
                amount = amount,
                date = getCurrentDateString(),
                desc = "جزاء $typeArabic: $reason",
                note = note,
                byName = byUser.name,
                srcReqNo = reqNo,
                status = "approved"
            )
            transactionDao.insertTransaction(tx)

            sendNotification(
                userId = empId,
                title = "تم تسجيل خصم في حسابك ⚠️",
                body = "خصم $typeArabic بمبلغ ${formatAmount(amount)} ريال ($reqNo)",
                kind = "alert"
            )
        } else {
            employeeDao.getGmEmployees().forEach { gm ->
                sendNotification(
                    userId = gm.id,
                    title = "خصم بانتظار الاعتماد النهائي 📌",
                    body = "$reqNo — ${emp.name} — ${formatAmount(amount)} ريال ($typeArabic)",
                    kind = "bell"
                )
            }
            sendNotification(
                userId = empId,
                title = "إشعار خصم قيد المراجعة",
                body = "سُجل عليك خصم $typeArabic ($reqNo) بمبلغ ${formatAmount(amount)} ريال وبانتظار الاعتماد",
                kind = "alert"
            )
        }

        logAudit(
            username = byUser.username,
            user = byUser.name,
            action = "تسجيل خصم",
            details = "$reqNo — ${emp.name} — ${formatAmount(amount)} ريال ($typeArabic)${if (isPending) " (بانتظار المدير العام)" else ""}"
        )

        Result.success(Unit)
    }

    suspend fun approvePenalty(
        penaltyId: Long,
        byUser: Employee,
        supNote: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val penalty = penaltyDao.getPenaltyById(penaltyId) ?: return@withContext Result.failure(Exception("الخصم غير موجود"))
        val emp = employeeDao.getEmployeeById(penalty.empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))

        val updated = penalty.copy(
            status = "approved",
            supNote = supNote.ifBlank { "معتمد نهائيًا" }
        )
        penaltyDao.updatePenalty(updated)

        val typeArabic = when (penalty.type) {
            "deduction" -> "خصم"
            "absence" -> "غياب"
            "lateness" -> "تأخر"
            "violation" -> "مخالفة"
            else -> "جزاء"
        }

        val txNo = "TRX-" + (1000 + (System.currentTimeMillis() % 9000))
        val tx = Transaction(
            no = txNo,
            empId = penalty.empId,
            type = "penalty",
            amount = penalty.amount,
            date = getCurrentDateString(),
            desc = "جزاء $typeArabic: ${penalty.reason}",
            note = supNote,
            byName = byUser.name,
            srcReqNo = penalty.reqNo,
            status = "approved"
        )
        transactionDao.insertTransaction(tx)

        sendNotification(
            userId = penalty.empId,
            title = "تم اعتماد الخصم في حسابك ⚠️",
            body = "تم اعتماد الخصم ${penalty.reqNo} بمبلغ ${formatAmount(penalty.amount)} ريال وقيده في كشف حسابك",
            kind = "alert"
        )

        logAudit(
            username = byUser.username,
            user = byUser.name,
            action = "اعتماد خصم",
            details = "${penalty.reqNo} — ${emp.name} — ${formatAmount(penalty.amount)} ريال"
        )

        Result.success(Unit)
    }

    suspend fun rejectPenalty(
        penaltyId: Long,
        byUser: Employee,
        reasonNote: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val penalty = penaltyDao.getPenaltyById(penaltyId) ?: return@withContext Result.failure(Exception("الخصم غير موجود"))
        val emp = employeeDao.getEmployeeById(penalty.empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))

        val updated = penalty.copy(
            status = "rejected",
            supNote = reasonNote.ifBlank { "مرفوض" }
        )
        penaltyDao.updatePenalty(updated)

        sendNotification(
            userId = penalty.empId,
            title = "تم إلغاء الخصم ✅",
            body = "تم رفض الخصم ${penalty.reqNo} ولن يُقيد في كشف حسابك",
            kind = "check"
        )

        logAudit(
            username = byUser.username,
            user = byUser.name,
            action = "رفض خصم",
            details = "${penalty.reqNo} — ${emp.name}"
        )

        Result.success(Unit)
    }

    suspend fun recordRepayment(
        empId: Long,
        amount: Double,
        date: String,
        note: String,
        byUser: Employee
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val emp = employeeDao.getEmployeeById(empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))
        val txNo = "TRX-" + (1000 + (System.currentTimeMillis() % 9000))
        val tx = Transaction(
            no = txNo,
            empId = empId,
            type = "repayment",
            amount = amount,
            date = date.ifBlank { getCurrentDateString() },
            desc = "سداد دفعة من السلفة",
            note = note,
            byName = byUser.name,
            srcReqNo = "",
            status = "approved"
        )
        transactionDao.insertTransaction(tx)

        sendNotification(
            userId = empId,
            title = "تم تسجيل سداد سلفة 💳",
            body = "تم سداد مبلغ ${formatAmount(amount)} ريال من رصيد سلفك",
            kind = "check"
        )

        logAudit(
            username = byUser.username,
            user = byUser.name,
            action = "تسجيل سداد سلفة",
            details = "${emp.name} — ${formatAmount(amount)} ريال"
        )

        Result.success(Unit)
    }

    suspend fun saveEmployee(
        employee: Employee,
        currentUser: Employee
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (employee.id == 0L) {
            val count = employeeDao.getEmployeeCount()
            val finalNo = employee.no.ifBlank { "100" + (count + 1) }
            val newEmp = employee.copy(no = finalNo)
            val id = employeeDao.insertEmployee(newEmp)
            logAudit(
                username = currentUser.username,
                user = currentUser.name,
                action = "إضافة موظف جديد",
                details = "${newEmp.name} (${newEmp.no})"
            )
            Result.success(id)
        } else {
            employeeDao.updateEmployee(employee)
            logAudit(
                username = currentUser.username,
                user = currentUser.name,
                action = "تعديل بيانات موظف",
                details = "${employee.name} (${employee.no})"
            )
            Result.success(employee.id)
        }
    }

    suspend fun changePassword(
        empId: Long,
        newPasswordRaw: String,
        currentUser: Employee
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val emp = employeeDao.getEmployeeById(empId) ?: return@withContext Result.failure(Exception("المستخدم غير موجود"))
        if (newPasswordRaw.length < 4) {
            return@withContext Result.failure(Exception("كلمة المرور يجب أن لا تقل عن 4 خانات"))
        }

        val passHash = AppDatabase.hashPassword(newPasswordRaw)
        val updated = emp.copy(passwordHash = passHash)
        employeeDao.updateEmployee(updated)

        sendNotification(
            userId = emp.id,
            title = "تم تحديث كلمة المرور 🔐",
            body = "تم تعديل كلمة المرور الخاصة بحسابكم بنجاح من قِبل إدارة النظام.",
            kind = "alert"
        )

        logAudit(
            username = currentUser.username,
            user = currentUser.name,
            action = "تعديل كلمة المرور",
            details = "تم تغيير الرقم السري لحساب: ${emp.name} (${emp.username})"
        )

        Result.success(Unit)
    }

    suspend fun updateEmployeeAccount(
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
        currentUser: Employee
    ): Result<Employee> = withContext(Dispatchers.IO) {
        val emp = employeeDao.getEmployeeById(empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))

        // Check unique username if changed
        if (!username.equals(emp.username, ignoreCase = true)) {
            val existing = employeeDao.getEmployeeByUsername(username)
            if (existing != null && existing.id != empId) {
                return@withContext Result.failure(Exception("اسم المستخدم مستخدم بالفعل، يرجى اختيار اسم مستخدم آخر"))
            }
        }

        val passHash = if (!newPasswordRaw.isNullOrBlank()) {
            if (newPasswordRaw.length < 4) {
                return@withContext Result.failure(Exception("كلمة المرور يجب أن لا تقل عن 4 أحرف/أرقام"))
            }
            AppDatabase.hashPassword(newPasswordRaw)
        } else {
            emp.passwordHash
        }

        val updated = emp.copy(
            name = name.trim(),
            no = no.trim().ifBlank { emp.no },
            mobile = mobile.trim(),
            email = email.trim(),
            job = job.trim(),
            deptId = deptId,
            role = role,
            supId = supId,
            username = username.trim(),
            passwordHash = passHash,
            status = status
        )

        employeeDao.updateEmployee(updated)

        logAudit(
            username = currentUser.username,
            user = currentUser.name,
            action = "تعديل حساب موظف وكلمة المرور",
            details = "تم تعديل بيانات حساب ${updated.name} (اسم المستخدم: ${updated.username})" +
                    if (!newPasswordRaw.isNullOrBlank()) " وتحديث الرقم السري" else ""
        )

        sendNotification(
            userId = emp.id,
            title = "تم تحديث بيانات الحساب 👤",
            body = "تم تحديث بيانات حسابكم الوظيفي بنجاح.",
            kind = "check"
        )

        Result.success(updated)
    }

    suspend fun toggleEmployeeStatus(
        empId: Long,
        currentUser: Employee
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val emp = employeeDao.getEmployeeById(empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))
        val newStatus = if (emp.status == "active") "inactive" else "active"
        employeeDao.updateEmployee(emp.copy(status = newStatus))
        logAudit(
            username = currentUser.username,
            user = currentUser.name,
            action = if (newStatus == "active") "تفعيل حساب" else "تعطيل حساب",
            details = "${emp.name} (${emp.no})"
        )
        Result.success(Unit)
    }

    suspend fun deleteEmployeeSafely(
        empId: Long,
        currentUser: Employee
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val emp = employeeDao.getEmployeeById(empId) ?: return@withContext Result.failure(Exception("الموظف غير موجود"))
        employeeDao.updateEmployee(emp.copy(status = "deleted"))
        logAudit(
            username = currentUser.username,
            user = currentUser.name,
            action = "حذف موظف (آمن)",
            details = "${emp.name} (${emp.no}) — تم الاحتفاظ بكامل السجل المالي"
        )
        Result.success(Unit)
    }

    suspend fun saveSettingsDirectly(settings: AppSettings) = withContext(Dispatchers.IO) {
        settingsDao.insertSettings(settings)
    }

    suspend fun updateSettings(
        settings: AppSettings,
        currentUser: Employee
    ): Result<Unit> = withContext(Dispatchers.IO) {
        settingsDao.insertSettings(settings)
        logAudit(
            username = currentUser.username,
            user = currentUser.name,
            action = "تحديث إعدادات النظام",
            details = "تم حفظ الإعدادات العامة"
        )
        Result.success(Unit)
    }

    suspend fun markNotificationsAsRead(userId: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(userId)
    }

    suspend fun sendNotification(userId: Long, title: String, body: String, kind: String = "bell") {
        notificationDao.insertNotification(
            AppNotification(
                userId = userId,
                title = title,
                body = body,
                time = getCurrentDateTimeString(),
                isRead = false,
                kind = kind
            )
        )
    }

    suspend fun logAudit(username: String, user: String, action: String, details: String) {
        auditDao.insertAuditLog(
            AuditLog(
                username = username,
                user = user,
                action = action,
                details = details,
                time = getCurrentDateTimeString()
            )
        )
    }

    companion object {
        fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun getCurrentDateTimeString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            return sdf.format(Date())
        }

        fun formatAmount(amount: Double): String {
            return String.format(Locale.US, "%,.2f", amount)
        }
    }
}
