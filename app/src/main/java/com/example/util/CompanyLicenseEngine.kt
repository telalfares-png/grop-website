package com.example.util

import com.example.data.model.CompanyPlan
import java.security.MessageDigest
import java.util.Locale

sealed class LicenseValidationResult {
    data class Valid(
        val tierId: String,
        val planTitle: String,
        val maxEmployees: Int,
        val expiryDate: String,
        val licenseKey: String
    ) : LicenseValidationResult()

    data class MismatchOrganization(
        val keyOrgPrefix: String,
        val currentOrgCode: String,
        val currentCompanyName: String,
        val reason: String
    ) : LicenseValidationResult()

    data class InvalidFormat(val message: String) : LicenseValidationResult()
}

data class OfficialKeySample(
    val tierId: String,
    val planTitle: String,
    val maxEmployeesText: String,
    val key: String
)

object CompanyLicenseEngine {

    private const val SECRET_SALT = "BrandLight_Enterprise_Secured_2026_Salt_#8892"

    /**
     * Extracts an organizational uppercase alphanumeric prefix from the company name.
     * E.g. "براند لايت التجارية" -> "BLT"
     */
    fun extractOrgPrefix(companyName: String): String {
        val cleanName = companyName.trim()
        if (cleanName.isBlank()) return "CORP"

        // Handle common known brand names or generate from words
        val words = cleanName.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val prefixBuilder = StringBuilder()

        for (word in words) {
            val firstChar = word.first()
            val mapped = mapArabicOrEnglishChar(firstChar)
            prefixBuilder.append(mapped)
            if (prefixBuilder.length >= 3) break
        }

        while (prefixBuilder.length < 3) {
            prefixBuilder.append("X")
        }

        return prefixBuilder.toString().uppercase(Locale.ROOT).take(4)
    }

    /**
     * Computes a deterministic 4-digit organizational code bound to the company name.
     */
    fun getOrgCode(companyName: String): String {
        val prefix = extractOrgPrefix(companyName)
        val hash = sha256Hex(companyName.trim().lowercase(Locale.ROOT) + "_org_id")
        val num = (hash.take(6).toLongOrNull(16) ?: 123456L) % 9000 + 1000
        return "$prefix-$num"
    }

    /**
     * Calculates the security checksum for an organization and tier.
     */
    private fun calculateChecksum(companyName: String, tierId: String, year: Int): String {
        val raw = "${companyName.trim().lowercase(Locale.ROOT)}_${tierId.lowercase(Locale.ROOT)}_$year$SECRET_SALT"
        val hash = sha256Hex(raw)
        val num = (hash.take(6).toLongOrNull(16) ?: 54321L) % 9000 + 1000
        val suffix = hash.substring(6, 8).uppercase(Locale.ROOT)
        return "$num$suffix"
    }

    /**
     * Generates a fully certified, organization-locked license key.
     * Format: CORP-{ORG_PREFIX}-{TIER}-{YEAR}-{CHECKSUM}
     * Example: CORP-BLT-PRO-2027-8491A2
     */
    fun generateLicenseKey(companyName: String, tierId: String, expiryYear: Int = 2027): String {
        val prefix = extractOrgPrefix(companyName)
        val tier = tierId.uppercase(Locale.ROOT)
        val checksum = calculateChecksum(companyName, tierId, expiryYear)
        return "CORP-$prefix-$tier-$expiryYear-$checksum"
    }

    /**
     * Validates a license key strictly against the current company name.
     * Ensures that a license generated for "شركة أخرى" fails if used in this company.
     */
    fun validateLicenseKey(licenseKey: String, currentCompanyName: String): LicenseValidationResult {
        val trimmedKey = licenseKey.trim().uppercase(Locale.ROOT)
        if (trimmedKey.length < 12) {
            return LicenseValidationResult.InvalidFormat("مفتاح الترخيص قصير جداً أو بتنسيق غير صحيح")
        }

        val parts = trimmedKey.split("-")
        if (parts.size < 4) {
            return LicenseValidationResult.InvalidFormat("صيغة مفتاح الترخيص غير مطابقة للتنسيق المؤسسي المعتمد (CORP-XXX-TIER-YEAR-XXXX)")
        }

        // Expected formats:
        // Format A: CORP-[ORG]-[TIER]-[YEAR]-[CHECKSUM] (5 parts)
        // Format B: [ORG]-[TIER]-[YEAR]-[CHECKSUM] (4 parts)
        val orgInKey: String
        val tierInKey: String
        val yearInKey: Int
        val checksumInKey: String

        if (parts.size >= 5 && parts[0] == "CORP") {
            orgInKey = parts[1]
            tierInKey = parts[2].lowercase(Locale.ROOT)
            yearInKey = parts[3].toIntOrNull() ?: 2027
            checksumInKey = parts[4]
        } else {
            orgInKey = parts[0]
            tierInKey = parts[1].lowercase(Locale.ROOT)
            yearInKey = parts[2].toIntOrNull() ?: 2027
            checksumInKey = parts[3]
        }

        val currentPrefix = extractOrgPrefix(currentCompanyName)
        val currentOrgCode = getOrgCode(currentCompanyName)

        // 1. Check if the key belongs to another organization prefix
        if (orgInKey != currentPrefix) {
            return LicenseValidationResult.MismatchOrganization(
                keyOrgPrefix = orgInKey,
                currentOrgCode = currentOrgCode,
                currentCompanyName = currentCompanyName,
                reason = "مفتاح الترخيص هذا مخصص لمنشأة برمز [$orgInKey] ولا يطابق معرف منشأتكم الحالية [$currentPrefix - $currentCompanyName]. لا يمكن استخدام تراخيص منشآت أخرى."
            )
        }

        // 2. Validate tier
        val matchedPlan = CompanyPlan.ALL_PLANS.find { it.id.equals(tierInKey, ignoreCase = true) }
            ?: return LicenseValidationResult.InvalidFormat("فئة الباقة المحددة في الترخيص ($tierInKey) غير معروفة بالنظام.")

        // 3. Verify cryptographic checksum against current company name
        val expectedChecksum = calculateChecksum(currentCompanyName, matchedPlan.id, yearInKey)
        if (!checksumInKey.equals(expectedChecksum, ignoreCase = true)) {
            return LicenseValidationResult.MismatchOrganization(
                keyOrgPrefix = orgInKey,
                currentOrgCode = currentOrgCode,
                currentCompanyName = currentCompanyName,
                reason = "رمز التحقق والتوقيع الرقمي للمفتاح غير متطابق مع بيانات منشأة ($currentCompanyName). المفتاح مخصص لمنشأة أخرى أو تم تعديله."
            )
        }

        val expiryDate = "$yearInKey-12-31"

        return LicenseValidationResult.Valid(
            tierId = matchedPlan.id,
            planTitle = matchedPlan.title,
            maxEmployees = matchedPlan.maxEmployees,
            expiryDate = expiryDate,
            licenseKey = trimmedKey
        )
    }

    /**
     * Generates the list of valid, verified license keys for the current company
     * to display to the General Manager for evaluation and licensing.
     */
    fun getOfficialCompanyKeys(companyName: String): List<OfficialKeySample> {
        return CompanyPlan.ALL_PLANS.map { plan ->
            val key = generateLicenseKey(companyName, plan.id, 2027)
            OfficialKeySample(
                tierId = plan.id,
                planTitle = plan.title,
                maxEmployeesText = if (plan.maxEmployees == -1) "غير محدود" else "${plan.maxEmployees} موظف",
                key = key
            )
        }
    }

    /**
     * Calculates the remaining days until subscription expiry.
     * Returns null if date is unparseable.
     */
    fun calculateDaysUntilExpiry(expiryDateStr: String): Long? {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val expiryDate = sdf.parse(expiryDateStr.trim()) ?: return null
            val now = java.util.Calendar.getInstance()
            // Clear time fields for accurate day difference
            now.set(java.util.Calendar.HOUR_OF_DAY, 0)
            now.set(java.util.Calendar.MINUTE, 0)
            now.set(java.util.Calendar.SECOND, 0)
            now.set(java.util.Calendar.MILLISECOND, 0)

            val diffMillis = expiryDate.time - now.timeInMillis
            java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the plan is within the warning threshold (7 days or less, or expired).
     */
    fun isExpiryWarningActive(expiryDateStr: String): Boolean {
        val days = calculateDaysUntilExpiry(expiryDateStr) ?: return false
        return days <= 7
    }

    private fun mapArabicOrEnglishChar(c: Char): Char {
        return when (c) {
            'ا', 'أ', 'إ', 'آ', 'a', 'A' -> 'A'
            'ب', 'b', 'B' -> 'B'
            'ت', 'ة', 'ط', 't', 'T' -> 'T'
            'ث', 'س', 'ص', 's', 'S' -> 'S'
            'ج', 'j', 'J' -> 'J'
            'ح', 'ه', 'h', 'H' -> 'H'
            'خ', 'k', 'K' -> 'K'
            'د', 'ض', 'd', 'D' -> 'D'
            'ذ', 'ز', 'ظ', 'z', 'Z' -> 'Z'
            'ر', 'r', 'R' -> 'R'
            'ع', 'غ', 'g', 'G' -> 'G'
            'ف', 'f', 'F' -> 'F'
            'ق', 'q', 'Q' -> 'Q'
            'ل', 'l', 'L' -> 'L'
            'م', 'm', 'M' -> 'M'
            'ن', 'n', 'N' -> 'N'
            'و', 'w', 'W' -> 'W'
            'ي', 'ى', 'y', 'Y' -> 'Y'
            else -> if (c.isLetterOrDigit()) c.uppercaseChar() else 'X'
        }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
