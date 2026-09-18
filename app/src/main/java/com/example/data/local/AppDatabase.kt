package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AdvanceRequest
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.Department
import com.example.data.model.Employee
import com.example.data.model.Penalty
import com.example.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Department::class,
        Employee::class,
        AdvanceRequest::class,
        Penalty::class,
        Transaction::class,
        AppNotification::class,
        AuditLog::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun departmentDao(): DepartmentDao
    abstract fun advanceDao(): AdvanceDao
    abstract fun penaltyDao(): PenaltyDao
    abstract fun transactionDao(): TransactionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun auditDao(): AuditDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "employee_manager_realm.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun hashPassword(password: String): String {
            var h = 5381L
            val s = "$password|EM#2026"
            for (c in s) {
                h = ((h shl 5) + h + c.code.toLong()) and 0xFFFFFFFFL
            }
            return "h" + java.lang.Long.toString(h, 36)
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                populateInitialData(getDatabase(context))
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            // Departments
            val departments = listOf(
                Department(id = 1, name = "الإدارة العامة"),
                Department(id = 2, name = "الموارد البشرية والشؤون الإدارية"),
                Department(id = 3, name = "تقنية المعلومات والبرمجة"),
                Department(id = 4, name = "المبيعات والتسويق"),
                Department(id = 5, name = "المالية والمحاسبة"),
                Department(id = 6, name = "العمليات والتشغيل")
            )
            database.departmentDao().insertAll(departments)

            // Settings
            val settings = AppSettings(
                id = 1,
                companyName = "براند لايت التجارية",
                currency = "ريال",
                advanceMax = 10000.0,
                sessionTimeout = 30,
                penaltyNeedsGM = true,
                logo = "💼"
            )
            database.settingsDao().insertSettings(settings)

            // Employees
            val employees = listOf(
                Employee(
                    id = 1,
                    no = "1001",
                    name = "أحمد عبد الله العتيبي",
                    mobile = "0501234567",
                    email = "ahmed.gm@brandlight.com",
                    deptId = 1,
                    job = "المدير العام والرئيس التنفيذي",
                    hireDate = "2020-01-15",
                    supId = null,
                    role = "gm",
                    username = "admin",
                    passwordHash = hashPassword("admin123"),
                    status = "active"
                ),
                Employee(
                    id = 2,
                    no = "1002",
                    name = "خالد بن منصور الشمري",
                    mobile = "0559876543",
                    email = "khaled.sup@brandlight.com",
                    deptId = 3,
                    job = "مشرف فريق التطوير والتقنية",
                    hireDate = "2021-03-10",
                    supId = 1,
                    role = "supervisor",
                    username = "supervisor",
                    passwordHash = hashPassword("123456"),
                    status = "active"
                ),
                Employee(
                    id = 3,
                    no = "1003",
                    name = "سارة فهد القحطاني",
                    mobile = "0543219876",
                    email = "sara.sup@brandlight.com",
                    deptId = 4,
                    job = "مشرفة المبيعات والتسويق",
                    hireDate = "2021-06-01",
                    supId = 1,
                    role = "supervisor",
                    username = "sara",
                    passwordHash = hashPassword("123456"),
                    status = "active"
                ),
                Employee(
                    id = 4,
                    no = "1004",
                    name = "محمد ناصر الدوسري",
                    mobile = "0531122334",
                    email = "mohammed.dev@brandlight.com",
                    deptId = 3,
                    job = "مطور تطبيقات أندرويد أول",
                    hireDate = "2022-02-01",
                    supId = 2,
                    role = "employee",
                    username = "employee",
                    passwordHash = hashPassword("123456"),
                    status = "active"
                ),
                Employee(
                    id = 5,
                    no = "1005",
                    name = "عبد العزيز صالح الغامدي",
                    mobile = "0567788990",
                    email = "abdulaziz.qa@brandlight.com",
                    deptId = 3,
                    job = "مهندس جودة واختبار نظم",
                    hireDate = "2022-05-15",
                    supId = 2,
                    role = "employee",
                    username = "abdulaziz",
                    passwordHash = hashPassword("123456"),
                    status = "active"
                ),
                Employee(
                    id = 6,
                    no = "1006",
                    name = "نورة عبد الرحمن الحربي",
                    mobile = "0589900112",
                    email = "noura.sales@brandlight.com",
                    deptId = 4,
                    job = "أخصائية تسويق رقمي",
                    hireDate = "2023-01-10",
                    supId = 3,
                    role = "employee",
                    username = "noura",
                    passwordHash = hashPassword("123456"),
                    status = "active"
                ),
                Employee(
                    id = 7,
                    no = "1007",
                    name = "سلطان إبراهيم الشهري",
                    mobile = "0524455667",
                    email = "sultan.acc@brandlight.com",
                    deptId = 5,
                    job = "محاسب مالي أول",
                    hireDate = "2022-09-01",
                    supId = 1,
                    role = "employee",
                    username = "sultan",
                    passwordHash = hashPassword("123456"),
                    status = "active"
                )
            )
            database.employeeDao().insertAll(employees)

            // Advances
            val advances = listOf(
                AdvanceRequest(
                    id = 1001,
                    reqNo = "ADV-1001",
                    empId = 4,
                    amount = 4000.0,
                    date = "2026-06-15",
                    reason = "مصاريف إيجار منزلي نصف سنوي",
                    note = "مرفق عقد الإيجار للمراجعة",
                    status = "approved",
                    supNote = "معتمد — يستحق السلفة",
                    approvedBy = 2,
                    approvedAt = "2026-06-16"
                ),
                AdvanceRequest(
                    id = 1002,
                    reqNo = "ADV-1002",
                    empId = 4,
                    amount = 2500.0,
                    date = "2026-07-20",
                    reason = "صيانة طارئة للمركبة",
                    note = "فواتير الورشة جاهزة",
                    status = "approved",
                    supNote = "معتمد مع اقتطاع شهري",
                    approvedBy = 2,
                    approvedAt = "2026-07-21"
                ),
                AdvanceRequest(
                    id = 1003,
                    reqNo = "ADV-1003",
                    empId = 4,
                    amount = 3000.0,
                    date = "2026-08-18",
                    reason = "رسوم دراسية للمرحلة الجامعية",
                    note = "قسط الفصل الدراسي الأول",
                    status = "pending",
                    supNote = ""
                ),
                AdvanceRequest(
                    id = 1004,
                    reqNo = "ADV-1004",
                    empId = 5,
                    amount = 5000.0,
                    date = "2026-08-10",
                    reason = "تأثيث وتجديد سكني",
                    note = "طلب سلفة سنوية",
                    status = "approved",
                    supNote = "تم الاعتماد",
                    approvedBy = 2,
                    approvedAt = "2026-08-11"
                ),
                AdvanceRequest(
                    id = 1005,
                    reqNo = "ADV-1005",
                    empId = 6,
                    amount = 3500.0,
                    date = "2026-08-19",
                    reason = "ظرف أسري طارئ",
                    note = "",
                    status = "pending",
                    supNote = ""
                )
            )
            database.advanceDao().insertAll(advances)

            // Penalties
            val penalties = listOf(
                Penalty(
                    id = 2001,
                    reqNo = "PEN-2001",
                    empId = 4,
                    type = "lateness",
                    amount = 150.0,
                    date = "2026-07-05",
                    reason = "تأخر متكرر عن بدء الدوام الصباحي (45 دقيقة)",
                    note = "تم التنبيه الشفهي مسبقًا",
                    status = "approved",
                    supNote = "معتمد",
                    byEmpId = 2
                ),
                Penalty(
                    id = 2002,
                    reqNo = "PEN-2002",
                    empId = 4,
                    type = "absence",
                    amount = 300.0,
                    date = "2026-08-03",
                    reason = "غياب يوم عمل دون تقديم عذر مسبق أو تقرير طبي",
                    note = "مخالفة نظام الدوام الداخلي",
                    status = "approved",
                    supNote = "معتمد بعد التحقق",
                    byEmpId = 2
                ),
                Penalty(
                    id = 2003,
                    reqNo = "PEN-2003",
                    empId = 5,
                    type = "violation",
                    amount = 200.0,
                    date = "2026-08-14",
                    reason = "عدم الالتزام ببروتوكول تسليم المهام التقنية",
                    note = "تسبب في تأخير تسليم الإصدار",
                    status = "pending",
                    supNote = "بانتظار موافقة المدير العام",
                    byEmpId = 2
                )
            )
            database.penaltyDao().insertAll(penalties)

            // Transactions
            val transactions = listOf(
                Transaction(
                    id = 1,
                    no = "TRX-1001",
                    empId = 4,
                    type = "advance",
                    amount = 4000.0,
                    date = "2026-06-16",
                    desc = "سلفة: مصاريف إيجار منزلي نصف سنوي",
                    note = "معتمد من المشرف خالد الشمري",
                    byName = "خالد بن منصور الشمري",
                    srcReqNo = "ADV-1001",
                    status = "approved"
                ),
                Transaction(
                    id = 2,
                    no = "TRX-1002",
                    empId = 4,
                    type = "repayment",
                    amount = 1500.0,
                    date = "2026-06-30",
                    desc = "سداد دفعة أولى من سلفة الإيجار",
                    note = "خصم مباشر من مسير الرواتب",
                    byName = "أحمد عبد الله العتيبي",
                    srcReqNo = "",
                    status = "approved"
                ),
                Transaction(
                    id = 3,
                    no = "TRX-1003",
                    empId = 4,
                    type = "penalty",
                    amount = 150.0,
                    date = "2026-07-05",
                    desc = "جزاء تأخر — تأخر متكرر عن بدء الدوام الصباحي",
                    note = "تم التطبيق بموجب اللائحة",
                    byName = "خالد بن منصور الشمري",
                    srcReqNo = "PEN-2001",
                    status = "approved"
                ),
                Transaction(
                    id = 4,
                    no = "TRX-1004",
                    empId = 4,
                    type = "advance",
                    amount = 2500.0,
                    date = "2026-07-21",
                    desc = "سلفة: صيانة طارئة للمركبة",
                    note = "معتمد مع اقتطاع شهري",
                    byName = "خالد بن منصور الشمري",
                    srcReqNo = "ADV-1002",
                    status = "approved"
                ),
                Transaction(
                    id = 5,
                    no = "TRX-1005",
                    empId = 4,
                    type = "repayment",
                    amount = 1500.0,
                    date = "2026-07-31",
                    desc = "سداد دفعة شهر يوليو من رصيد السلف",
                    note = "اقتطاع المسير الشهري",
                    byName = "أحمد عبد الله العتيبي",
                    srcReqNo = "",
                    status = "approved"
                ),
                Transaction(
                    id = 6,
                    no = "TRX-1006",
                    empId = 4,
                    type = "penalty",
                    amount = 300.0,
                    date = "2026-08-03",
                    desc = "جزاء غياب — غياب يوم عمل دون تقديم عذر",
                    note = "معتمد من الإدارة",
                    byName = "خالد بن منصور الشمري",
                    srcReqNo = "PEN-2002",
                    status = "approved"
                ),
                Transaction(
                    id = 7,
                    no = "TRX-1007",
                    empId = 5,
                    type = "advance",
                    amount = 5000.0,
                    date = "2026-08-11",
                    desc = "سلفة: تأثيث وتجديد سكني",
                    note = "معتمد للموظف عبد العزيز",
                    byName = "خالد بن منصور الشمري",
                    srcReqNo = "ADV-1004",
                    status = "approved"
                )
            )
            database.transactionDao().insertAll(transactions)

            // Notifications
            val notifications = listOf(
                AppNotification(
                    id = 1,
                    userId = 4,
                    title = "تم اعتماد السلفة ✅",
                    body = "تم اعتماد طلبك ADV-1002 بمبلغ 2,500 ريال وقيده في كشف حسابك",
                    time = "2026-07-21 10:30",
                    isRead = true,
                    kind = "cash"
                ),
                AppNotification(
                    id = 2,
                    userId = 4,
                    title = "إشعار خصم في حسابك ⚠️",
                    body = "تم اعتماد خصم PEN-2002 بمبلغ 300 ريال بسبب غياب غير مبرر",
                    time = "2026-08-03 14:15",
                    isRead = false,
                    kind = "alert"
                ),
                AppNotification(
                    id = 3,
                    userId = 2,
                    title = "طلب سلفة جديد 📩",
                    body = "قدّم محمد الدوسري طلب سلفة ADV-1003 بمبلغ 3,000 ريال وبانتظار مراجعتكم",
                    time = "2026-08-18 09:40",
                    isRead = false,
                    kind = "bell"
                ),
                AppNotification(
                    id = 4,
                    userId = 1,
                    title = "طلب معلق يتطلب الاعتماد 📌",
                    body = "طلب سلفة جديد ADV-1005 مقدّم من نورة الحربي بقيمة 3,500 ريال",
                    time = "2026-08-19 11:20",
                    isRead = false,
                    kind = "bell"
                )
            )
            database.notificationDao().insertAll(notifications)

            // Audit Logs
            val auditLogs = listOf(
                AuditLog(
                    id = 1,
                    username = "admin",
                    user = "أحمد عبد الله العتيبي",
                    action = "تسجيل دخول ناجح",
                    details = "دخول إلى النظام المحلي",
                    time = "2026-08-20 08:30"
                ),
                AuditLog(
                    id = 2,
                    username = "supervisor",
                    user = "خالد بن منصور الشمري",
                    action = "اعتماد سلفة",
                    details = "ADV-1004 — عبد العزيز الغامدي — 5,000 ريال",
                    time = "2026-08-11 11:15"
                ),
                AuditLog(
                    id = 3,
                    username = "employee",
                    user = "محمد ناصر الدوسري",
                    action = "تقديم طلب سلفة",
                    details = "ADV-1003 بمبلغ 3,000 ريال",
                    time = "2026-08-18 09:40"
                ),
                AuditLog(
                    id = 4,
                    username = "supervisor",
                    user = "خالد بن منصور الشمري",
                    action = "تسجيل خصم",
                    details = "PEN-2003 — عبد العزيز الغامدي — 200 ريال (بانتظار اعتماد المدير العام)",
                    time = "2026-08-14 13:00"
                )
            )
            database.auditDao().insertAll(auditLogs)
        }
    }
}
