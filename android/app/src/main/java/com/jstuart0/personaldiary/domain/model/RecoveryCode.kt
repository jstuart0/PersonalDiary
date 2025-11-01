package com.jstuart0.personaldiary.domain.model

/**
 * Recovery code for E2E tier account recovery
 * User MUST save these codes at signup
 */
data class RecoveryCode(
    val code: String,
    val used: Boolean = false
)
