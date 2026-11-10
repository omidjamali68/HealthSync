package com.example.healthsync.util

object SmsOtpParser {
    /**
     * Extracts a 6-digit verification code from SMS body text.
     * Specially tailored for messages such as:
     * دستیار هوشمند سلامت طب دیتا
     * کد: 123456
     * @https://teb-data.ir
     */
    fun extractOtpCode(message: String?): String? {
        if (message.isNullOrBlank()) return null

        // Primary pattern: "کد" or "code" followed by optional colons/spaces and 6 digits
        val primaryRegex = Regex("""(?:کد|code)\s*[:\s]*(\d{6})""", RegexOption.IGNORE_CASE)
        val primaryMatch = primaryRegex.find(message)
        if (primaryMatch != null) {
            return primaryMatch.groupValues[1]
        }

        // Secondary fallback: Any standalone 6-digit number in the SMS
        val fallbackRegex = Regex("""\b(\d{6})\b""")
        val fallbackMatch = fallbackRegex.find(message)
        return fallbackMatch?.groupValues[1]
    }
}
