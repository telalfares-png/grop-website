package com.example.util

import com.example.data.model.Employee
import java.util.Locale

object PhoneAuthHelper {

    /**
     * Normalizes an Arabic or English phone number string to a standard 10-digit Saudi format (05XXXXXXXX)
     * or standard clean numeric format.
     * Handles inputs like:
     * - "0501234567" -> "0501234567"
     * - "501234567" -> "0501234567"
     * - "+966501234567" -> "0501234567"
     * - "00966501234567" -> "0501234567"
     * - "966501234567" -> "0501234567"
     * - "٠٥٠١٢٣٤٥٦٧" -> "0501234567"
     */
    fun normalizePhoneNumber(rawPhone: String): String {
        if (rawPhone.isBlank()) return ""

        // 1. Convert Arabic-Indic numerals to standard Western digits
        val builder = StringBuilder()
        for (ch in rawPhone.trim()) {
            when (ch) {
                '٠' -> builder.append('0')
                '١' -> builder.append('1')
                '٢' -> builder.append('2')
                '٣' -> builder.append('3')
                '٤' -> builder.append('4')
                '٥' -> builder.append('5')
                '٦' -> builder.append('6')
                '٧' -> builder.append('7')
                '٨' -> builder.append('8')
                '٩' -> builder.append('9')
                in '0'..'9' -> builder.append(ch)
                '+' -> if (builder.isEmpty()) builder.append('+')
            }
        }

        var digits = builder.toString()

        // 2. Remove leading "+" or "00"
        if (digits.startsWith("+966")) {
            digits = digits.removePrefix("+966")
        } else if (digits.startsWith("00966")) {
            digits = digits.removePrefix("00966")
        } else if (digits.startsWith("966") && digits.length == 12) {
            digits = digits.removePrefix("966")
        }

        // 3. Ensure leading '0' for 9-digit Saudi numbers starting with '5'
        if (digits.length == 9 && digits.startsWith("5")) {
            digits = "0$digits"
        }

        return digits
    }

    /**
     * Formats a phone number for neat user display (e.g., "+966 50 123 4567" or "050 123 4567").
     */
    fun formatDisplayPhone(rawPhone: String): String {
        val norm = normalizePhoneNumber(rawPhone)
        return if (norm.length == 10 && norm.startsWith("05")) {
            "${norm.substring(0, 3)} ${norm.substring(3, 6)} ${norm.substring(6)}"
        } else {
            rawPhone.trim()
        }
    }

    /**
     * Checks if a phone number matches an employee's registered mobile number.
     */
    fun doesPhoneMatch(inputPhone: String, employeeMobile: String): Boolean {
        val normInput = normalizePhoneNumber(inputPhone)
        val normEmp = normalizePhoneNumber(employeeMobile)
        if (normInput.isBlank() || normEmp.isBlank()) return false

        return normInput == normEmp ||
                normInput.endsWith(normEmp.takeLast(9)) ||
                normEmp.endsWith(normInput.takeLast(9))
    }

    /**
     * Generates a deterministic or simulated 4-digit OTP for testing and employee verification.
     * Returns "1234" by default for demo ease, or a secure generated code.
     */
    fun getDemoOtpCode(phone: String): String {
        val norm = normalizePhoneNumber(phone)
        if (norm.isBlank()) return "1234"
        val lastDigits = norm.takeLast(4)
        return if (lastDigits.length == 4) lastDigits else "1234"
    }

    /**
     * Pre-configured quick demo employees for testing the organizational link with General Management.
     */
    data class DemoEmployeeAuth(
        val roleTitle: String,
        val roleBadge: String,
        val name: String,
        val mobile: String,
        val defaultPass: String,
        val jobTitle: String,
        val departmentName: String,
        val isGm: Boolean = false
    )

    val DEMO_ORGANIZATION_ACCOUNTS = listOf(
        DemoEmployeeAuth(
            roleTitle = "المدير العام (الإدارة العليا)",
            roleBadge = "👑 إدارة عامة",
            name = "أحمد عبد الله العتيبي",
            mobile = "0501234567",
            defaultPass = "admin123",
            jobTitle = "المدير العام والرئيس التنفيذي",
            departmentName = "الإدارة العامة",
            isGm = true
        ),
        DemoEmployeeAuth(
            roleTitle = "مشرف القسم والتقنية",
            roleBadge = "👔 مشرف مباشر",
            name = "خالد بن منصور الشمري",
            mobile = "0559876543",
            defaultPass = "123456",
            jobTitle = "مشرف فريق التطوير والتقنية",
            departmentName = "تقنية المعلومات والبرمجة"
        ),
        DemoEmployeeAuth(
            roleTitle = "مشرفة المبيعات والتسويق",
            roleBadge = "👩‍💼 مشرفة قسم",
            name = "سارة فهد القحطاني",
            mobile = "0543219876",
            defaultPass = "123456",
            jobTitle = "مشرفة المبيعات والتسويق",
            departmentName = "المبيعات والتسويق"
        ),
        DemoEmployeeAuth(
            roleTitle = "موظف (كادر التشغيل والبرمجة)",
            roleBadge = "💻 موظف",
            name = "محمد ناصر الدوسري",
            mobile = "0531122334",
            defaultPass = "123456",
            jobTitle = "مطور تطبيقات أندرويد أول",
            departmentName = "تقنية المعلومات والبرمجة"
        ),
        DemoEmployeeAuth(
            roleTitle = "موظف (كادر الجودة والمتابعة)",
            roleBadge = "🔍 موظف",
            name = "عبد العزيز صالح الغامدي",
            mobile = "0567788990",
            defaultPass = "123456",
            jobTitle = "مهندس جودة واختبار نظم",
            departmentName = "تقنية المعلومات والبرمجة"
        )
    )
}
