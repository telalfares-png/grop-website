# تقرير الفحص والتدقيق الشامل لنظام إدارة الموظفين
## EmployeeManager — Cloud Migration Audit & Architecture Specification
**تاريخ الفحص:** 2026-09-17  
**حالة البناء:** `BUILD SUCCESSFUL`  
**الهدف:** التجهيز المعماري والتقني للانتقال التدريجي من قاعدة بيانات محلية (Room / SQLite) إلى بيئة سحابية مركزية متعددة الأجهزة تعتمد على (Supabase + PostgreSQL + RLS + Realtime)، دون فقدان البيانات ودون كسر استقرار النظام الحالي.

---

## الفهرس العام للتقرير
- [A. Current Architecture (البنية المعمارية الحالية)](#a-current-architecture)
- [B. Database Schema (مخطط قاعدة البيانات الحالية)](#b-database-schema)
- [C. Authentication (نظام التحقق وتسجيل الدخول الحالي)](#c-authentication)
- [D. Authorization (نظام الصلاحيات وتحديد الأدوار)](#d-authorization)
- [E. Financial Workflow (الدورة المستندية والمالية الكاملة)](#e-financial-workflow)
- [F. Repository Layer (تحليل طبقة البيانات والمستودع)](#f-repository-layer)
- [G. Cloud Migration Map (خريطة التحويل إلى PostgreSQL)](#g-cloud-migration-map)
- [H. Security Risks (سجل المخاطر الأمنية المكتشفة)](#h-security-risks)
- [I. Proposed Supabase Architecture (الهيكل السحابي المقترح)](#i-proposed-supabase-architecture)
- [J. Row Level Security Plan (خطة أمان وسرية البيانات RLS)](#j-row-level-security-plan)
- [K. Phased Migration Strategy (استراتيجية الانتقال المرحلي الآمن)](#k-phased-migration-strategy)

---

## A. Current Architecture

### 1. النمط المعماري (Architectural Pattern)
يعتمد التطبيق حاليًا على معمارية **MVVM (Model-View-ViewModel)** أحادية المعمارية (Single-tier Client-Side):
```
[ Jetpack Compose UI Screens ]
             │ (StateFlow / User Events)
             ▼
      [ MainViewModel ]
             │ (Kotlin Coroutines / Flow)
             ▼
    [ EmployeeRepository ]
             │ (Direct Dao calls)
             ▼
     [ Room Database DAOs ]
             │ (SQLite transactions)
             ▼
 [ SQLite DB (employee_manager_realm.db) ]
```

### 2. المكونات الأساسية وتوزيع المسؤوليات
* **UI Layer (`com.example.ui`):**
  * `MainActivity.kt`: نقطة الانطلاق وإعداد `enableEdgeToEdge()`.
  * `MainApp.kt`: هيكل التطبيق العام (Scaffold, TopAppBar, BottomNavigationBar, Routing, Role-based view filtering).
  * `screens/`: 14 شاشة Compose تغطي تسجيل الدخول، الداشبورد، الموظفين، تفاصيل الموظف، السلف، الجزاءات، التقارير، التدقيق، كشف الحساب، والإعدادات.
* **ViewModel Layer (`com.example.ui.MainViewModel`):**
  * يدير الـ Session الحالية عبر `_currentUser: MutableStateFlow<Employee?>`.
  * يستمع للـ Flows القادمة من المستودع ويحولها إلى `StateFlow` باستخدام `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ...)`.
  * يحتوي على دوال تنفيذ العمليات المالية والإدارية (تسجيل دخول، تقديم سلفة، اعتماد/رفض، تسجيل جزاء، سداد، تعاميم).
* **Repository Layer (`com.example.data.repository.EmployeeRepository`):**
  * يمثل الطبقة الوسيطة الوحيدة بين الـ ViewModel وقاعدة البيانات.
  * ينسق العمليات المعقدة مثل إضافة طلب سلفة ثم إرسال إشعارات للمشرفين والمدير العام ثم تسجيل حركة في سجل التدقيق (AuditLog).
  * يقوم بحساب الأرصدة التراكمية وإحصائيات الموظف برمجياً في الذاكرة عبر `Flow.map`.
* **Data / Local Layer (`com.example.data.local`):**
  * `AppDatabase`: فئة مجردة ترث `RoomDatabase`، تتضمن دالة تشفير الكلمات السريعة `hashPassword` وقائمة الـ DAOs و`DatabaseCallback` لتغذية البيانات الابتدائية (Seeding).
  * `Daos.kt`: يحتوي على 8 واجهات DAO تستخدم `Flow` للعمليات التفاعلية و`suspend` للعمليات التزامنية.

---

## B. Database Schema

يحتوي التطبيق على **8 جداول Room** معرفة في `com.example.data.model.Models.kt`، ولا تستخدم قيود المفاتيح الأجنبية الصارمة على مستوى SQLite (`@ForeignKey`)، بل تعتمد على العلاقات المنطقية (Logical Foreign Keys).

### 1. جدول الأقسام (`departments`)
* **Entity:** `Department`
* **الهدف:** تصنيف الموظفين حسب إدارات المنشأة.
* **الحقول:**
  * `id: Long` (Primary Key, `autoGenerate = true`)
  * `name: String` (اسم القسم، مثل: "الإدارة العامة"، "تقنية المعلومات والبرمجة")

### 2. جدول الموظفين (`employees`)
* **Entity:** `Employee`
* **الهدف:** تخزين السجل الشخصي والوظيفي وبيانات الدخول والصلاحيات.
* **الحقول:**
  * `id: Long` (Primary Key, `autoGenerate = true`)
  * `no: String` (الرقم الوظيفي الفريد، مثل "1001")
  * `name: String` (اسم الموظف الرباعي)
  * `mobile: String` (رقم الجوال)
  * `email: String` (البريد الإلكتروني)
  * `deptId: Long` (مفتاح خارجي منطقي يشير إلى `departments.id`)
  * `job: String` (المسمى الوظيفي)
  * `hireDate: String` (تاريخ المباشرة بتنسيق `YYYY-MM-DD`)
  * `supId: Long?` (مفتاح خارجي ذاتي منطقي يشير إلى `employees.id` للمشرف المباشر، Nullable للمدير العام)
  * `role: String` (الدور الإداري: `"gm"`, `"supervisor"`, `"employee"`)
  * `username: String` (اسم المستخدم لتسجيل الدخول)
  * `passwordHash: String` (تجزئة كلمة المرور)
  * `status: String` (حالة الحساب: `"active"`, `"inactive"`, `"deleted"`)
  * `photoUri: String?` (رابط صورة الموظف)
  * `lastLogin: String?` (تاريخ ووقت آخر تسجيل دخول)

### 3. جدول طلبات السلف (`advances`)
* **Entity:** `AdvanceRequest`
* **الهدف:** إدارة طلبات السلف المالية ودورة اعتمادها.
* **الحقول:**
  * `id: Long` (Primary Key, `autoGenerate = true`)
  * `reqNo: String` (رمز الطلب، مثل "ADV-1001")
  * `empId: Long` (مفتاح خارجي يشير إلى `employees.id`)
  * `amount: Double` (مبلغ السلفة المطلوب)
  * `date: String` (تاريخ الطلب `YYYY-MM-DD`)
  * `reason: String` (سبب طلب السلفة)
  * `note: String` (ملاحظات إضافية)
  * `status: String` (حالة الطلب: `"pending"`, `"approved"`, `"rejected"`)
  * `supNote: String` (ملاحظات المشرف أو سبب الرفض)
  * `approvedBy: Long?` (مفتاح خارجي يشير إلى `employees.id` للشخص المعتمد)
  * `approvedAt: String?` (تاريخ ووقت الاعتماد)

### 4. جدول الخصومات والجزاءات (`penalties`)
* **Entity:** `Penalty`
* **الهدف:** تقييد المخالفات والخصومات والغيابات.
* **الحقول:**
  * `id: Long` (Primary Key, `autoGenerate = true`)
  * `reqNo: String` (رمز المخالفة، مثل "PEN-2001")
  * `empId: Long` (مفتاح خارجي يشير إلى `employees.id`)
  * `type: String` (نوع الجزاء: `"deduction"`, `"absence"`, `"lateness"`, `"violation"`, `"other"`)
  * `amount: Double` (قيمة الخصم المالي)
  * `date: String` (تاريخ تسجيل المخالفة)
  * `reason: String` (سبب الجزاء)
  * `note: String` (ملاحظات إضافية)
  * `status: String` (الحالة: `"pending"`, `"approved"`, `"rejected"`, `"pending_gm"`)
  * `supNote: String` (ملاحظات الإدارة)
  * `byEmpId: Long?` (مفتاح خارجي يشير إلى الموظف/المشرف محرر الجزاء)
  * `isSelfReport: Boolean` (هل هو إقرار ذاتي أم مسجل إدارياً)

### 5. جدول الحركات المالية للأستاذ العام (`transactions`)
* **Entity:** `Transaction`
* **الهدف:** السجل المالي النهائي غير القابل للتعديل الذي يؤثر على رصيد الموظف وكشف حسابه.
* **الحقول:**
  * `id: Long` (Primary Key, `autoGenerate = true`)
  * `no: String` (رقم القيد المحاسبي، مثل "TRX-1001")
  * `empId: Long` (مفتاح خارجي يشير إلى `employees.id`)
  * `type: String` (نوع الحركة: `"advance"`, `"repayment"`, `"deduction"`, `"penalty"`, `"adjustment"`)
  * `amount: Double` (المبلغ المالي للحركة)
  * `date: String` (تاريخ الحركة `YYYY-MM-DD`)
  * `desc: String` (بيان الحركة في كشف الحساب)
  * `note: String` (ملاحظات القيد)
  * `byName: String` (اسم الموظف أو المسؤول الذي اعتمد أو سجل الحركة)
  * `srcReqNo: String` (الرقم المصدري المرجعي للطلب: مثل "ADV-1001" أو "PEN-2001" أو فارغ للسداد)
  * `status: String` (الحالة: دائمًا `"approved"`)

### 6. جدول الإشعارات (`notifications`)
* **Entity:** `AppNotification`
* **الهدف:** إرسال التنبيهات المباشرة للموظفين والإدارة.
* **الحقول:**
  * `id: Long` (Primary Key, `autoGenerate = true`)
  * `userId: Long` (مفتاح خارجي يشير إلى الموظف المستلم `employees.id`)
  * `title: String` (عنوان الإشعار)
  * `body: String` (نص الإشعار)
  * `time: String` (توقيت الإشعار `YYYY-MM-DD HH:mm`)
  * `isRead: Boolean` (حالة القراءة)
  * `kind: String` (النوع البصري للأيقونة: `"cash"`, `"alert"`, `"check"`, `"bell"`)

### 7. جدول سجل التدقيق الأمني (`audit_logs`)
* **Entity:** `AuditLog`
* **الهدف:** مراقبة وتتبع العمليات الحساسة (دخول، خروج، اعتماد، تعديل، ترقية باقة).
* **الحقول:**
  * `id: Long` (Primary Key, `autoGenerate = true`)
  * `username: String` (اسم المستخدم منفذ العملية)
  * `user: String` (الاسم الفعلي لمنفذ العملية)
  * `action: String` (نوع الإجراء: "تسجيل دخول", "تعميد سلفة", إلخ)
  * `details: String` (تفاصيل الإجراء والمبالغ والرموز)
  * `time: String` (وقت التنفيذ)
  * `ip: String` (عنوان الـ IP، الافتراضي حاليًا: "127.0.0.1")

### 8. جدول إعدادات التطبيق والترخيص (`app_settings`)
* **Entity:** `AppSettings`
* **الهدف:** جدول وحيد (Singleton row with `id = 1`) لتخزين هوية المنشأة وسياساتها.
* **الحقول:**
  * `id: Int = 1` (Primary Key)
  * `companyName: String` (اسم المنشأة، الافتراضي: "براند لايت التجارية")
  * `currency: String` (العملة: "ريال")
  * `advanceMax: Double` (الحد الأعلى للسلفة: 10000.0)
  * `sessionTimeout: Int` (مدة الجلسة بالدقائق: 30)
  * `penaltyNeedsGM: Boolean` (هل يتطلب الجزاء اعتماد المدير العام)
  * `logo: String` (أيقونة الشعار)
  * `subscriptionTier: String` (الباقة: "basic", "pro", "enterprise")
  * `subscriptionPlanName: String` (اسم باقة الاشتراك)
  * `maxEmployeesLimit: Int` (الحد الأقصى لعدد الموظفين حسب الباقة)
  * `licenseKey: String` (مفتاح الترخيص المشفر)
  * `licenseExpiryDate: String` (تاريخ انتهاء الترخيص)
  * `appVersion: String` (إصدار التطبيق "0.0.01")
  * `autoLoginEnabled: Boolean` (تفعيل الدخول التلقائي)

---

## C. Authentication

### 1. آليات تسجيل الدخول الحالية في الكود
يدعم التطبيق 3 مسارات لدخول المستخدم:
1. **اسم المستخدم وكلمة المرور (`login(identifier, passwordRaw)`):**
   * يبحث عن الموظف في `employees` بمطابقة `username`، `no`، أو `mobile`.
   * يطابق قيمة `emp.passwordHash` مع نتيجة دالة `AppDatabase.hashPassword(passwordRaw)`.
   * يتحقق من أن حالة الموظف ليست `"inactive"`.
   * يحدّث `lastLogin` ويقوم بتسجيل حركة في `audit_logs`.
2. **رقم الجوال ورمز التحقق (`loginWithPhoneOtp(phone, otpCode)`):**
   * يوحّد رقم الجوال عبر `PhoneAuthHelper.normalizePhoneNumber(phone)`.
   * يقبل الرمز إذا طابق آخر 4 أرقام من الجوال عبر `PhoneAuthHelper.getDemoOtpCode(phone)` أو الرموز الثابتة `"1234"` و `"0000"`.
3. **الدخول التلقائي (`checkAutoLogin()`):**
   * عند فتح التطبيق، يقرأ `SessionManager` قيمتي `KEY_IDENTIFIER` و `KEY_PASSWORD_RAW` المحفوظتين محليًا، ويعيد استدعاء `repository.login`.

### 2. طريقة تشفير كلمات المرور الحالية
تستخدم الدالة:
```kotlin
fun hashPassword(password: String): String {
    var h = 5381L
    val s = "$password|EM#2026"
    for (c in s) {
        h = ((h shl 5) + h + c.code.toLong()) and 0xFFFFFFFFL
    }
    return "h" + java.lang.Long.toString(h, 36)
}
```
* **التقييم:** خوارزمية غير مشفرة قياسياً تعتمد على DJB2 مع salt ثابت (`|EM#2026`). وهي 32-bit Hash معرض للتصادم والكسر الفوري عبر جداول قوس قزح (Rainbow Tables).

### 3. طريقة تخزين الجلسة (Session Management)
* تستخدم الفئة `com.example.util.SessionManager`.
* تخزن البيانات داخل `SharedPreferences` قياسي غير مشفر (`employee_manager_session_prefs`):
  * `saved_identifier`: رقم الجوال أو اسم المستخدم.
  * `saved_password_raw`: **كلمة المرور كنص صريح (Plaintext)**.
  * `auto_login_enabled`: قيمة منطقية.
  * `is_logged_in`: قيمة منطقية.
  * `last_login_timestamp`: توقيت الدخول.

---

## D. Authorization

### 1. هيكل الأدوار والصلاحيات
تحدد الصلاحيات في التطبيق عبر حقلين في كيان `Employee`:
* `role: String`:
  1. `"gm"` (المدير العام / الإدارة العامة): يمتلك الصلاحية الكاملة على مستوى التطبيق.
  2. `"supervisor"` (المشرف / مسؤول القسم): يمتلك صلاحيات إدارة واعتماد الموظفين التابعين له.
  3. `"employee"` (الموظف العادي): يقتصر على حسابه وسجلاته فقط.
* `supId: Long?`: معرّف المشرف المباشر.

### 2. تنفيذ الصلاحيات الحالي (Client-Side Filtering)
يتم التحقق من الصلاحيات بالكامل داخل كود Jetpack Compose والـ ViewModel:
* **في شاشة مراجعة الطلبات (`RequestsReviewScreen.kt`):**
  ```kotlin
  val mySubIds = remember(employees, currentUser) {
      if (currentUser.role == "gm") employees.map { it.id }
      else employees.filter { it.supId == currentUser.id }.map { it.id }
  }
  val scopedAdvances = advances.filter { mySubIds.contains(it.empId) }
  ```
* **في شاشة كشف الحساب (`StatementScreen.kt`):**
  * إذا كان المستخدم `"gm"`: تظهر قائمة بجميع الموظفين لاختيار أي موظف.
  * إذا كان المستخدم `"supervisor"`: تظهر قائمة تحتوي على المشرف نفسه + مرؤوسيه (`supId == supervisor.id`).
  * إذا كان المستخدم `"employee"`: يتم تثبيت `selectedEmpId = currentUser.id` مع تعطيل القائمة المنسدلة.
* **في شاشة الموظفين (`EmployeesScreen.kt`):**
  * الموظف العادي لا يستطيع فتح الشاشة من القائمة السفلية (BottomBar تخفيها للموظف العادي).
* **في شاشة باقات الشركات (`CompanyPlansScreen.kt`):**
  * مسموحة فقط للمدير العام (`role == "gm"`).

---

## E. Financial Workflow

### 1. دورة طلب السلفة والاعتماد المحاسبي
```
1. [الموظف] يقدم طلب سلفة (المبلغ، السبب، الملاحظة)
   ├── ينشأ سجل في جدول advances بالحالة "pending"
   ├── يرسل إشعار في جدول notifications للمشرف المباشر (emp.supId)
   ├── يرسل إشعار لجميع المدراء العامين (role = 'gm')
   └── يقيد حدث في audit_logs
   
2. [المشرف أو المدير العام] يراجع الطلب في RequestsReviewScreen
   ├── في حال الرفض:
   │   ├── تحديث advances: status = "rejected", supNote = سبب الرفض
   │   ├── إشعار الموظف بالرفض
   │   └── لا ينشأ أي قيد في transactions (كشف الحساب لا يتأثر)
   └── في حال الاعتماد:
       ├── تحديث advances: status = "approved", approvedBy, approvedAt
       ├── إنشاء قيد فوري في transactions:
       │   ├── type = "advance"
       │   ├── amount = adv.amount
       │   ├── srcReqNo = adv.reqNo
       │   └── status = "approved"
       ├── إشعار الموظف بقيد السلفة
       └── تسجيل حركة في audit_logs

3. [كشف الحساب - StatementScreen]
   ├── يستمع إلى getTransactionsByEmpId(empId)
   └── يظهر القيد تلقائياً ويزداد رصيد السلف التراكمي للموظف
```

### 2. دورة السداد (Repayment)
* لا يوجد جدول منفصل باسم `repayments`.
* عند قيام الإدارة بتسجيل سداد (جزئي أو كلي):
  * تستدعى الدالة `repository.recordRepayment(empId, amount, date, note, byUser)`.
  * ينشأ قيد محاسبي مباشر في جدول `transactions`:
    * `type = "repayment"`
    * `amount = amount`
    * `srcReqNo = ""`
  * يرسل إشعار للموظف بالسداد وقيد الحركة في حسابه.
  * ينخفض رصيد السلف المتبقي تلقائياً.

### 3. دورة الخصومات والجزاءات (Penalties / Deductions)
* يتم تسجيل الجزاء من قِبل المدير العام أو المشرف.
* إذا سجله المشرف وكان إعداد المنشأة يتطلب موافقة المدير العام (`penaltyNeedsGM == true`):
  * ينشأ في `penalties` بالحالة `"pending"`.
  * لا يدرج في `transactions` حتى يقوم المدير العام باعتماده من شاشة المراجعة.
* عند الاعتماد النهائي:
  * ينشأ قيد في `transactions` بنوع `type = "penalty"` ومصدر `srcReqNo = penalty.reqNo`.
  * يُحسب ضمن إجمالي الاستقطاعات (`totalDeductions`).

### 4. معادلات حساب الأرصدة المالية
* **رصيد السلف التراكمي الجاري للموظف (Running Balance):**
  * يبدأ من `0.0`.
  * مع كل حركة `advance` يُضاف المبلغ: `runningBal += tx.amount`.
  * مع كل حركة `repayment` يُخصم المبلغ: `runningBal -= tx.amount`.
* **الرصيد المالي الإجمالي المتبقي (Remaining Balance):**
  * `remainingBalance = totalAdvances - totalRepaid`
* **إجمالي الاستقطاعات:**
  * `totalDeductions = مجموع حركات (penalty + deduction)` (لا تُضاف إلى رصيد المديونية المستحقة للسلف، بل تظهر كإجمالي جزاءات واستقطاعات مستقلة).

---

## F. Repository Layer

* **الهيكلية المتبعة:**
  `UI Screens` ──▶ `MainViewModel` ──▶ `EmployeeRepository` ──▶ `Room DAOs` ──▶ `AppDatabase (SQLite)`
* **خصائص الطبقة:**
  * التطبيق يلتزم بنمط المستودع الواحد المركزي `EmployeeRepository` الذي يحقن `AppDatabase`.
  * يتم توفير واجهات قراءة متدفقة (`Flow<List<T>>`) تتولى تحديث واجهة المستخدم فورياً بمجرد إدراج أي سجل في Room.
  * تتولى دوال الـ `suspend` في الـ Repository إدارة السياقات عبر `withContext(Dispatchers.IO)`.
* **التقييم للمرحلة السحابية:**
  * هذه الطبقة ممتازة جداً ونقطة قوة في المشروع؛ لأنها تعزل طبقة البيانات عن الـ UI تماماً.
  * عند الربط السحابي، يمكن تحويل `EmployeeRepository` ليعمل بنمط **Offline-First / Sync Repository** دون الحاجة لتغيير واجهات الـ Compose إطلاقاً.

---

## G. Cloud Migration Map

جدول المطابقة بين كيانات Room المحلية وجداول PostgreSQL السحابية في Supabase:

| Room Entity | PostgreSQL Table | Primary Key | Foreign Keys | استراتيجية المزامنة (Sync Strategy) |
| :--- | :--- | :--- | :--- | :--- |
| `Department` | `public.departments` | `id: bigint (GENERATED ALWAYS AS IDENTITY)` | `org_id -> organizations(id)` | قراءة سحابية مع تخزين محلي (Cache-first)؛ تحديثات نادرة من الإدارة. |
| `Employee` | `public.employees` | `id: bigint (GENERATED ALWAYS AS IDENTITY)` | `org_id -> organizations(id)`<br>`dept_id -> departments(id)`<br>`sup_id -> employees(id)`<br>`auth_user_id -> auth.users(id)` | مزامنة فورية ثنائية الاتجاه؛ الموظف يقرأ حسابه، المشرف يقرأ مرؤوسيه، والمدير العام يقرأ الجميع. |
| `AdvanceRequest` | `public.advance_requests` | `id: bigint (GENERATED ALWAYS AS IDENTITY)` | `org_id -> organizations(id)`<br>`emp_id -> employees(id)`<br>`approved_by -> employees(id)` | Realtime Sync عبر Supabase Realtime؛ إرسال الطلبات من الهاتف واعتمادها فوراً. |
| `Penalty` | `public.penalties` | `id: bigint (GENERATED ALWAYS AS IDENTITY)` | `org_id -> organizations(id)`<br>`emp_id -> employees(id)`<br>`by_emp_id -> employees(id)` | مزامنة فورية؛ تظهر للموظف والمشرف المعنيين والإدارة العليا. |
| `Transaction` | `public.financial_transactions` | `id: bigint (GENERATED ALWAYS AS IDENTITY)` | `org_id -> organizations(id)`<br>`emp_id -> employees(id)`<br>`created_by -> employees(id)` | قيود غير قابلة للتعديل (Append-Only Ledger)؛ كشف الحساب يقرأ من السحابة ويُحفظ محلياً. |
| `AppNotification` | `public.notifications` | `id: bigint (GENERATED ALWAYS AS IDENTITY)` | `user_id -> employees(id)` | Realtime Subscriptions لإنشاء تنبيهات فورية على هواتف الموظفين. |
| `AuditLog` | `public.audit_logs` | `id: bigint (GENERATED ALWAYS AS IDENTITY)` | `org_id -> organizations(id)`<br>`emp_id -> employees(id)` | كتابة سحابية محمية بصلاحيات النظام (Service Role / Trigger) لمنع التلاعب. |
| `AppSettings` | `public.organizations` | `id: uuid or bigint` | — | قراءة الإعدادات وهوية المنشأة حسب رمز الشركة (`company_code`). |

---

## H. Security Risks (سجل المخاطر الأمنية المكتشفة)

> ⚠️ **ملاحظة مهمة:** تم توثيق هذه المخاطر بدقة بناءً على فحص الشيفرة الحالية دون إجراء أي تعديل عليها في هذه المرحلة.

1. **تخزين كلمات المرور الصريحة في SharedPreferences (`Plaintext Password Exposure`):**
   * **الموقع:** `com.example.util.SessionManager.kt` سطر 13 و 22 (`KEY_PASSWORD_RAW = "saved_password_raw"`).
   * **الخطر:** إذا كان الجهاز غير مشفر أو تم عمل نسخة احتياطية محلية (ADB Backup)، يمكن استخراج كلمة مرور الموظف والمدير العام كنص مكشوف.
2. **استخدام تجزئة مخصصة ضعيفة جدًا (`Weak Password Hashing`):**
   * **الموقع:** `com.example.data.local.AppDatabase.kt` دالة `hashPassword`.
   * **الخطر:** خوارزمية DJB2 32-bit ليست دالة تشفير آمنة (Non-cryptographic hash) وتتعرض لتصادمات هاش سهلة وللهجمات المباشرة دون تكلفة حوسبية (Work Factor).
3. **الاعتماد الكلي على حماية الواجهة الأمامية (`Client-Side Authorization Vulnerability`):**
   * **الموقع:** `MainViewModel.kt` و `RequestsReviewScreen.kt`.
   * **الخطر:** جميع السلف والجزاءات والموظفين تُسحب بالكامل إلى الذاكرة عبر Room Flow، ويقوم Compose بالفلترة فقط. في حال استخدام تطبيق معدل أو إرسال طلبات سحابية مباشرة دون Row Level Security (RLS)، يستطيع أي موظف الاطلاع على رواتب وسلف كافة زملائه وإدارته.
4. **رموز تحقق OTP وهمية وثابتة (`Mock Hardcoded OTP bypass`):**
   * **الموقع:** `PhoneAuthHelper.kt` و `EmployeeRepository.kt` سطر 154 (`enteredOtp == expectedOtp || enteredOtp == "1234" || enteredOtp == "0000"`).
   * **الخطر:** يستطيع أي شخص تسجيل الدخول برقم جوال أي مدير عام بمجرد إدخال الرمز `"1234"`.
5. **توليد أرقام القيود بطريقة غير فريدة (`ID Collision in Distributed Devices`):**
   * **الموقع:** `EmployeeRepository.kt` (`"ADV-" + (1000 + (System.currentTimeMillis() % 9000))`).
   * **الخطر:** في بيئة متعددة الأجهزة (Multi-device)، سيؤدي هذا الرمز القائم على باقي قسمة الوقت إلى تكرار أرقام السلف والقيود المحاسبية وتضاربها فوراً بين هاتفين يقومان بإنشاء سلفة في نفس الوقت.
6. **غياب القيود المحاسبية الصارمة على مستوى قاعدة البيانات (`Lack of DB-level Constraints`):**
   * **الموقع:** Room Entities.
   * **الخطر:** لا توجد قيود `FOREIGN KEY ... ON DELETE RESTRICT` في SQLite تمنع حذف موظف لديه حركات مالية مسجلة، مما يؤدي إلى سجلات يتيمة (Orphaned Records).

---

## I. Proposed Supabase Architecture

لتحقيق متطلبات بيئة الإنتاج السحابية متعددة المنشآت والمستخدمين، يُقترح المخطط السحابي التالي في PostgreSQL:

```sql
-- 1. المنشآت (Organizations / Tenants)
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL, -- مثل: BRANDLIGHT
    name VARCHAR(255) NOT NULL,
    currency VARCHAR(20) DEFAULT 'ريال',
    advance_max NUMERIC(12, 2) DEFAULT 10000.00,
    penalty_needs_gm BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. الموظفون المرتبطون بالمستخدم السحابي (Employees)
CREATE TABLE employees (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    auth_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    employee_no VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    mobile VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    dept_id BIGINT REFERENCES departments(id),
    job_title VARCHAR(150),
    hire_date DATE,
    supervisor_id BIGINT REFERENCES employees(id),
    role VARCHAR(50) NOT NULL CHECK (role IN ('employee', 'supervisor', 'gm')),
    status VARCHAR(50) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'deleted')),
    photo_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(org_id, employee_no),
    UNIQUE(org_id, mobile)
);

-- 3. طلبات السلف (Advance Requests)
CREATE TABLE advance_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    req_no VARCHAR(50) NOT NULL,
    emp_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    request_date DATE NOT NULL DEFAULT CURRENT_DATE,
    reason TEXT NOT NULL,
    note TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    supervisor_note TEXT,
    approved_by BIGINT REFERENCES employees(id),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. الخصومات والجزاءات (Penalties)
CREATE TABLE penalties (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    req_no VARCHAR(50) NOT NULL,
    emp_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    type VARCHAR(50) NOT NULL CHECK (type IN ('deduction', 'absence', 'lateness', 'violation', 'other')),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    penalty_date DATE NOT NULL DEFAULT CURRENT_DATE,
    reason TEXT NOT NULL,
    note TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'pending_gm')),
    supervisor_note TEXT,
    created_by BIGINT REFERENCES employees(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. سجل العمليات المالية (Financial Ledger / Transactions)
CREATE TABLE financial_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    trx_no VARCHAR(50) NOT NULL,
    emp_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    type VARCHAR(50) NOT NULL CHECK (type IN ('advance', 'repayment', 'deduction', 'penalty', 'adjustment')),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    trx_date DATE NOT NULL DEFAULT CURRENT_DATE,
    description TEXT NOT NULL,
    note TEXT,
    created_by_name VARCHAR(255) NOT NULL,
    source_ref_no VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. الإشعارات السحابية (Notifications)
CREATE TABLE notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    kind VARCHAR(50) DEFAULT 'bell',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. سجل التدقيق السحابي (Audit Logs)
CREATE TABLE audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES employees(id),
    actor_name VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## J. Row Level Security Plan (خطة أمان وسرية البيانات RLS)

> 🛡️ **المبدأ الأساسي:** حماية البيانات في PostgreSQL عبر سياسات RLS بحيث يستحيل على أي مستخدم استرجاع أو تعديل سجلات غير مصرح له بها حتى لو قام باعتراض أو تزوير طلبات الشبكة (Zero-Trust Client).

### 1. دالة استخراج سياق الموظف الحالي (Helper Function)
```sql
CREATE OR REPLACE FUNCTION current_employee_record()
RETURNS employees AS $$
  SELECT * FROM employees 
  WHERE auth_user_id = auth.uid() 
    AND status = 'active'
  LIMIT 1;
$$ LANGUAGE SQL STABLE SECURITY DEFINER;
```

### 2. سياسات جدول الموظفين (`employees`)
* **SELECT:**
  * **EMPLOYEE:** يستطيع قراءة سجله الشخصي فقط: `id = (current_employee_record()).id`.
  * **SUPERVISOR:** يستطيع قراءة سجله الشخصي + أي موظف يكون المشرف المباشر له: `id = (current_employee_record()).id OR supervisor_id = (current_employee_record()).id`.
  * **GENERAL_MANAGER:** يستطيع قراءة جميع موظفي نفس المنشأة: `org_id = (current_employee_record()).org_id AND (current_employee_record()).role = 'gm'`.
* **INSERT / UPDATE:**
  * للمدير العام فقط (`role = 'gm'`).

### 3. سياسات جدول السلف (`advance_requests`)
* **SELECT:**
  * **EMPLOYEE:** `emp_id = (current_employee_record()).id`
  * **SUPERVISOR:** `emp_id IN (SELECT id FROM employees WHERE supervisor_id = (current_employee_record()).id OR id = (current_employee_record()).id)`
  * **GENERAL_MANAGER:** `org_id = (current_employee_record()).org_id AND (current_employee_record()).role = 'gm'`
* **INSERT:**
  * الموظف يستطيع تقديم طلب لنفسه فقط (`emp_id = (current_employee_record()).id AND status = 'pending'`).
* **UPDATE (الموافقة / الرفض):**
  * المشرف المباشر للموظف، أو المدير العام للمنشأة فقط.

### 4. سياسات كشف الحساب والحركات المالية (`financial_transactions`)
* **SELECT:**
  * الموظف يرى حركاته الخاصة فقط (`emp_id = (current_employee_record()).id`).
  * المشرف يرى حركات مرؤوسيه فقط.
  * المدير العام يرى جميع الحركات.
* **INSERT:**
  * مسموح فقط للمدير العام (`role = 'gm'`)، أو من خلال Stored Procedure آمنة تُستدعى آلياً عند اعتماد السلفة أو الجزاء (Security Definer Trigger) لمنع التلاعب بالأرصدة من الهواتف مباشرة.

---

## K. Phased Migration Strategy (استراتيجية الانتقال المرحلي الآمن)

لضمان الانتقال السلس دون انقطاع الخدمة أو فقدان البيانات:

### المرحلة 1: إنشاء المخطط السحابي (Cloud Schema Setup)
* إنشاء مشروع Supabase وتجهيز جداول PostgreSQL مع المفاتيح القياسية والعلاقات والقيود.
* إعداد دوال التسلسل الآمن للأرقام المحاسبية (Sequences) لمنع تضارب `ADV-` و `TRX-`.

### المرحلة 2: التحقق والمصادقة (Authentication Migration)
* تفعيل Supabase Auth (Email/Password أو Phone Auth).
* إنشاء حسابات سحابية آمنة للموظفين مع ربطها بـ `auth_user_id`، واستبدال التجزئة القديمة بنظام التشفير القياسي `Argon2id / bcrypt`.
* استبدال حفظ كلمة المرور كنص صريح في `SessionManager` باستخدام **EncryptedSharedPreferences** أو رموز **Supabase JWT Refresh Tokens**.

### المرحلة 3: مزامنة القراءة (Read Synchronization)
* إبقاء قاعدة بيانات Room المحلية كطبقة تخزين كاش محلية سريعة (Offline Cache).
* يقوم `EmployeeRepository` بسحب البيانات المحدثة من Supabase عند توفر الإنترنت، وتحديث Room محلياً.
* لا يتم تغيير أي كود في شاشات Compose أو ViewModels.

### المرحلة 4: مزامنة الكتابة (Write Synchronization)
* عند تنفيذ عملية (طلب سلفة، سداد، جزاء)، ترسل العملية إلى Supabase API أولاً، ثم تنعكس في Room فور نجاحها.
* في حالة انقطاع الاتصال (Offline)، تُحفظ الحركات محلياً في جدول وسيط (Pending Sync Queue) لرفعها فور عودة الإنترنت.

### المرحلة 5: التزامن اللحظي (Realtime Synchronization)
* تفعيل `Supabase Realtime Postgres Changes` على جداول `advance_requests` و `notifications` و `financial_transactions`.
* فور قيام موظف بتقديم سلفة من هاتفه، يظهر الطلب فوراً على شاشة المشرف والمدير العام على هواتفهم دون الحاجة لعمل Refresh يدوي.

### المرحلة 6: الإشعارات السحابية (Cloud Push Notifications)
* ربط إشعارات Supabase مع **Firebase Cloud Messaging (FCM)** لإرسال تنبيهات شريط النظام (Push Notifications) حتى عند إغلاق التطبيق.

### المرحلة 7: إدارة الكاش والعمل بدون إنترنت (Offline & Cache Hardening)
* التأكد التام من استمرار عمل التطبيق وطباعة تقارير PDF وكشوفات الحساب في حال عدم وجود شبكة إنترنت اعتماداً على آخر نسخة محلية متزامنة في Room.

### المرحلة 8: إطلاق بيئة الإنتاج ونقل البيانات الابتدائية (Production Migration)
* رفع بيانات الموظفين والعمليات التاريخية الموجودة محلياً إلى قاعدة بيانات Supabase.
* إغلاق وضع الحسابات التجريبية وتفعيل RLS بالكامل.

---
**نهاية تقرير التدقيق المعماري.**  
جاهز للمراجعة واتخاذ قرار البدء في مراحل التنفيذ عند الموافقة.
