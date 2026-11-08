package com.jarvis.mobile.core.notifications

/**
 * Detect likely verification-code notifications before they enter
 * notification history or notification-triggered automations.
 */
object NotificationPrivacy {

    private val explicitSecretTerms = Regex(
        """\b(?:otp|one[\s-]?time(?:\s+password)?|verification(?:\s+code)?|security\s+code|authentication\s+code|pass[\s-]?code|pin)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val codeLikeNumber = Regex(
        """(?<!\d)\d(?:[ -]?\d){3,7}(?!\d)"""
    )

    private val codeContext = Regex(
        """\b(?:code|login|log[\s-]?in|sign[\s-]?in|use|expires?|valid\s+for|do\s+not\s+share|don't\s+share)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun isSensitive(title: String, text: String): Boolean {
        val content = "$title\n$text"

        if (explicitSecretTerms.containsMatchIn(content)) return true

        return codeLikeNumber.containsMatchIn(content) &&
            codeContext.containsMatchIn(content)
    }
}
