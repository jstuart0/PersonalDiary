package com.jstuart0.personaldiary.domain.model

/**
 * Encryption tier chosen by user at signup
 * This is a PERMANENT choice that cannot be changed
 */
enum class EncryptionTier {
    /**
     * End-to-End Encryption
     * - Keys never leave client device
     * - Server cannot decrypt user content
     * - Maximum privacy guarantee
     * - Limited server-side features
     */
    E2E,

    /**
     * User-Controlled Encryption
     * - Encrypted at rest with user password-derived keys
     * - Server stores encrypted master key
     * - Server can decrypt with user password
     * - Enables account recovery and server-side features
     */
    UCE
}
