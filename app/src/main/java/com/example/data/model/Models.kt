package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

const val APP_VERSION = "0.0.01"

@Entity(tableName = "departments")
data class Department(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val no: String,
    val name: String,
    val mobile: String,
    val email: String,
    val deptId: Long,
    val job: String,
    val hireDate: String,
    val supId: Long? = null,
    val role: String, // "gm", "supervisor", "employee"
    val username: String,
    val passwordHash: String,
    val status: String = "active", // "active", "inactive", "deleted"
    val photoUri: String? = null,
    val lastLogin: String? = null
)

@Entity(tableName = "advances")
data class AdvanceRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reqNo: String,
    val empId: Long,
    val amount: Double,
    val date: String,
    val reason: String,
    val note: String = "",
    val status: String = "pending", // "pending", "approved", "rejected"
    val supNote: String = "",
    val approvedBy: Long? = null,
    val approvedAt: String? = null
)

@Entity(tableName = "penalties")
data class Penalty(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reqNo: String,
    val empId: Long,
    val type: String, // "deduction", "absence", "lateness", "violation", "other"
    val amount: Double,
    val date: String,
    val reason: String,
    val note: String = "",
    val status: String = "pending", // "pending", "approved", "rejected", "pending_gm"
    val supNote: String = "",
    val byEmpId: Long? = null,
    val isSelfReport: Boolean = false
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val no: String,
    val empId: Long,
    val type: String, // "advance", "repayment", "deduction", "penalty", "adjustment"
    val amount: Double,
    val date: String,
    val desc: String,
    val note: String = "",
    val byName: String = "",
    val srcReqNo: String = "",
    val status: String = "approved"
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val body: String,
    val time: String,
    val isRead: Boolean = false,
    val kind: String = "bell" // "cash", "alert", "check", "bell"
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val user: String,
    val action: String,
    val details: String,
    val time: String,
    val ip: String = "127.0.0.1"
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "براند لايت التجارية",
    val currency: String = "ريال",
    val advanceMax: Double = 10000.0,
    val sessionTimeout: Int = 30,
    val penaltyNeedsGM: Boolean = true,
    val logo: String = "💼",
    val subscriptionTier: String = "pro", // "basic", "pro", "enterprise"
    val subscriptionPlanName: String = "الباقة الاحترافية (Pro)",
    val maxEmployeesLimit: Int = 50,
    val licenseKey: String = "CORP-PRO-2026-8941",
    val licenseExpiryDate: String = "2027-12-31",
    val appVersion: String = APP_VERSION,
    val autoLoginEnabled: Boolean = true
)

data class StatementRow(
    val transaction: Transaction,
    val runningBalance: Double
)

data class EmployeeFinanceStats(
    val totalAdvances: Double = 0.0,
    val totalRepaid: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val pendingRequestsCount: Int = 0
)

data class CompanyPlan(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetCompany: String,
    val priceYearly: String,
    val priceMonthly: String,
    val maxEmployees: Int, // -1 for unlimited
    val isPopular: Boolean = false,
    val badgeLabel: String = "",
    val features: List<String>,
    val highlights: List<String>
) {
    companion object {
        val ALL_PLANS = listOf(
            CompanyPlan(
                id = "basic",
                title = "الباقة الأساسية",
                subtitle = "للشركات والمؤسسات الناشئة",
                targetCompany = "المؤسسات والأنشطة التجارية حتى 15 موظف",
                priceYearly = "1,499 ريال / سنوياً",
                priceMonthly = "149 ريال / شهرياً",
                maxEmployees = 15,
                isPopular = false,
                badgeLabel = "بداية مثالية",
                features = listOf(
                    "حتى 15 حساب موظف نشط",
                    "تسجيل وإدارة طلبات السلف المالية",
                    "تسجيل الخصومات والجزاءات الفورية",
                    "كشوف حسابات تفصيلية لكل موظف",
                    "صلاحيات مستخدمين (مدير عام + موظفين)",
                    "قاعدة بيانات محلية آمنة ومشفرة",
                    "دعم فني عبر البريد الإلكتروني"
                ),
                highlights = listOf("15 موظف", "فرع واحد", "سلف وخصومات")
            ),
            CompanyPlan(
                id = "pro",
                title = "الباقة الاحترافية",
                subtitle = "للشركات والمؤسسات المتوسطة والمتنامية",
                targetCompany = "الشركات النشطة التي تحتاج دورة اعتماد هرمية وتقارير رسمية",
                priceYearly = "3,499 ريال / سنوياً",
                priceMonthly = "349 ريال / شهرياً",
                maxEmployees = 50,
                isPopular = true,
                badgeLabel = "الأكثر طلباً واختياراً ⭐",
                features = listOf(
                    "حتى 50 حساب موظف ومشرف",
                    "كل مميزات الباقة الأساسية",
                    "دورة اعتمادات هرمية (مشرفين + مدير عام)",
                    "تصدير وطباعة تقارير PDF الرسمية مع التواقيع",
                    "سجل تدقيق ومراقبة الحركات والأمان المالي (Audit Log)",
                    "نظام الإشعارات والتنبيهات المباشرة للموظفين",
                    "التحكم بسقوف وسياسات السلف والخصومات",
                    "تسجيل الدخول التلقائي والمطابقة المباشرة للهواتف",
                    "دعم فني ذو أولوية عبر الواتساب والهاتف"
                ),
                highlights = listOf("50 موظف", "تقارير PDF رسمية", "سجل تدقيق كامل", "اعتماد المشرفين")
            ),
            CompanyPlan(
                id = "enterprise",
                title = "باقة الشركات الكبرى والمجموعات",
                subtitle = "للشركات الكبرى والمجموعات القابضة والمتعددة الفروع",
                targetCompany = "المؤسسات الكبرى التي تتطلب عدد غير محدود ومرونة قصوى",
                priceYearly = "6,999 ريال / سنوياً",
                priceMonthly = "699 ريال / شهرياً",
                maxEmployees = -1, // Unlimited
                isPopular = false,
                badgeLabel = "شاملة وغير محدودة 💎",
                features = listOf(
                    "عدد موظفين ومشرفين غير محدود (Unlimited)",
                    "كل مميزات الباقة الاحترافية",
                    "إدارة متعددة الفروع والأقسام والشركات التابعة",
                    "تخصيص الهوية البصرية للشركة والشعار الرسمي والعملات",
                    "إمكانية الربط مع الأنظمة المحاسبية وسحب البيانات",
                    "سياسات متقدمة مخصصة للاعتمادات والموافقات المالية",
                    "نسخ احتياطي واستعادة آلية آمنة للبيانات",
                    "مدير حساب مخصص ودعم فني استشاري 24/7",
                    "تدريب مجاني لفريق الموارد البشرية والمحاسبة"
                ),
                highlights = listOf("موظفون غير محدود", "فروع متعددة", "تخصيص هوية المنشأة", "دعم 24/7")
            )
        )
    }
}

