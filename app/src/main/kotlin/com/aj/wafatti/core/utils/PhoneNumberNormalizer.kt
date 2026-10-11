package com.aj.wafatti.core.utils

object PhoneNumberNormalizer {
    /**
     * Normalizes a phone number string by removing spaces, dashes, brackets,
     * and standardizing international country codes.
     */
    fun normalize(phone: String, defaultCountryCode: String = "+91"): String {
        if (phone.isBlank()) return ""

        // Keep digits and leading plus sign
        val cleaned = phone.filter { it.isDigit() || it == '+' }
        if (cleaned.isBlank()) return ""

        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("0") -> defaultCountryCode + cleaned.substring(1)
            cleaned.length == 10 -> defaultCountryCode + cleaned
            else -> cleaned
        }
    }

    /**
     * Checks whether two phone numbers represent the same normalized contact.
     */
    fun isSameNumber(phone1: String, phone2: String): Boolean {
        val norm1 = normalize(phone1)
        val norm2 = normalize(phone2)
        if (norm1.isBlank() || norm2.isBlank()) return false
        return norm1 == norm2 || norm1.endsWith(norm2.takeLast(10)) || norm2.endsWith(norm1.takeLast(10))
    }
}
