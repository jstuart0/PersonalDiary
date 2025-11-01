/**
 * Cryptographic Utilities using Web Crypto API
 *
 * Provides low-level cryptographic operations for both E2E and UCE encryption tiers.
 * All operations use secure browser APIs and follow best practices.
 */

import { EncryptionTier } from '@/types';

// ============================================================================
// Constants
// ============================================================================

const PBKDF2_ITERATIONS = 600000; // OWASP recommended minimum
const PBKDF2_HASH = 'SHA-256';
const AES_ALGORITHM = 'AES-GCM';
const AES_KEY_LENGTH = 256;
const IV_LENGTH = 12; // 96 bits for GCM
const SALT_LENGTH = 32; // 256 bits

const E2E_KEY_ALGORITHM = 'ECDH';
const E2E_CURVE = 'P-256';

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Generate cryptographically secure random bytes
 */
export function generateRandomBytes(length: number): Uint8Array {
  return crypto.getRandomValues(new Uint8Array(length));
}

/**
 * Convert ArrayBuffer to Base64 string
 */
export function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

/**
 * Convert Base64 string to ArrayBuffer
 */
export function base64ToArrayBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

/**
 * Convert string to ArrayBuffer
 */
export function stringToArrayBuffer(str: string): ArrayBuffer {
  return new TextEncoder().encode(str);
}

/**
 * Convert ArrayBuffer to string
 */
export function arrayBufferToString(buffer: ArrayBuffer): string {
  return new TextDecoder().decode(buffer);
}

// ============================================================================
// Hash Functions
// ============================================================================

/**
 * Generate SHA-256 hash of data (for content deduplication)
 */
export async function generateHash(data: string): Promise<string> {
  const buffer = stringToArrayBuffer(data);
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
  return arrayBufferToBase64(hashBuffer);
}

/**
 * Generate SHA-256 hash of recovery code (for E2E tier)
 */
export async function hashRecoveryCode(code: string): Promise<string> {
  return generateHash(code);
}

// ============================================================================
// PBKDF2 Key Derivation (UCE Tier)
// ============================================================================

/**
 * Derive encryption key from password using PBKDF2
 * Used for UCE tier to encrypt/decrypt master key
 */
export async function deriveKeyFromPassword(
  password: string,
  salt: Uint8Array
): Promise<CryptoKey> {
  const passwordBuffer = stringToArrayBuffer(password);

  // Import password as key material
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    passwordBuffer,
    'PBKDF2',
    false,
    ['deriveKey']
  );

  // Derive AES-GCM key from password
  return crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt,
      iterations: PBKDF2_ITERATIONS,
      hash: PBKDF2_HASH
    },
    keyMaterial,
    {
      name: AES_ALGORITHM,
      length: AES_KEY_LENGTH
    },
    false,
    ['encrypt', 'decrypt']
  );
}

/**
 * Generate salt for PBKDF2
 */
export function generateSalt(): Uint8Array {
  return generateRandomBytes(SALT_LENGTH);
}

// ============================================================================
// AES-GCM Encryption/Decryption
// ============================================================================

/**
 * Encrypt data with AES-GCM
 */
export async function encryptWithKey(
  data: string,
  key: CryptoKey
): Promise<{ ciphertext: string; iv: string }> {
  const iv = generateRandomBytes(IV_LENGTH);
  const dataBuffer = stringToArrayBuffer(data);

  const cipherBuffer = await crypto.subtle.encrypt(
    {
      name: AES_ALGORITHM,
      iv
    },
    key,
    dataBuffer
  );

  return {
    ciphertext: arrayBufferToBase64(cipherBuffer),
    iv: arrayBufferToBase64(iv)
  };
}

/**
 * Decrypt data with AES-GCM
 */
export async function decryptWithKey(
  ciphertext: string,
  iv: string,
  key: CryptoKey
): Promise<string> {
  const cipherBuffer = base64ToArrayBuffer(ciphertext);
  const ivBuffer = base64ToArrayBuffer(iv);

  const dataBuffer = await crypto.subtle.decrypt(
    {
      name: AES_ALGORITHM,
      iv: ivBuffer
    },
    key,
    cipherBuffer
  );

  return arrayBufferToString(dataBuffer);
}

// ============================================================================
// Master Key Generation (UCE Tier)
// ============================================================================

/**
 * Generate random AES-GCM master key
 * This key encrypts all user content in UCE tier
 */
export async function generateMasterKey(): Promise<CryptoKey> {
  return crypto.subtle.generateKey(
    {
      name: AES_ALGORITHM,
      length: AES_KEY_LENGTH
    },
    true, // extractable
    ['encrypt', 'decrypt']
  );
}

/**
 * Export master key to raw format
 */
export async function exportMasterKey(key: CryptoKey): Promise<string> {
  const exported = await crypto.subtle.exportKey('raw', key);
  return arrayBufferToBase64(exported);
}

/**
 * Import master key from raw format
 */
export async function importMasterKey(keyData: string): Promise<CryptoKey> {
  const keyBuffer = base64ToArrayBuffer(keyData);
  return crypto.subtle.importKey(
    'raw',
    keyBuffer,
    {
      name: AES_ALGORITHM,
      length: AES_KEY_LENGTH
    },
    false,
    ['encrypt', 'decrypt']
  );
}

// ============================================================================
// E2E Keypair Generation
// ============================================================================

/**
 * Generate ECDH keypair for E2E encryption
 */
export async function generateE2EKeypair(): Promise<CryptoKeyPair> {
  return crypto.subtle.generateKey(
    {
      name: E2E_KEY_ALGORITHM,
      namedCurve: E2E_CURVE
    },
    true,
    ['deriveKey']
  );
}

/**
 * Export public key to JWK format
 */
export async function exportPublicKey(publicKey: CryptoKey): Promise<string> {
  const exported = await crypto.subtle.exportKey('jwk', publicKey);
  return JSON.stringify(exported);
}

/**
 * Import public key from JWK format
 */
export async function importPublicKey(jwk: string): Promise<CryptoKey> {
  const keyData = JSON.parse(jwk);
  return crypto.subtle.importKey(
    'jwk',
    keyData,
    {
      name: E2E_KEY_ALGORITHM,
      namedCurve: E2E_CURVE
    },
    true,
    []
  );
}

/**
 * Export private key to JWK format
 */
export async function exportPrivateKey(privateKey: CryptoKey): Promise<string> {
  const exported = await crypto.subtle.exportKey('jwk', privateKey);
  return JSON.stringify(exported);
}

/**
 * Import private key from JWK format
 */
export async function importPrivateKey(jwk: string): Promise<CryptoKey> {
  const keyData = JSON.parse(jwk);
  return crypto.subtle.importKey(
    'jwk',
    keyData,
    {
      name: E2E_KEY_ALGORITHM,
      namedCurve: E2E_CURVE
    },
    false,
    ['deriveKey']
  );
}

/**
 * Encrypt private key with password-derived key (for secure storage)
 */
export async function encryptPrivateKey(
  privateKey: CryptoKey,
  password: string
): Promise<{ encrypted: string; salt: string; iv: string }> {
  const salt = generateSalt();
  const derivedKey = await deriveKeyFromPassword(password, salt);

  const privateKeyJwk = await exportPrivateKey(privateKey);
  const { ciphertext, iv } = await encryptWithKey(privateKeyJwk, derivedKey);

  return {
    encrypted: ciphertext,
    salt: arrayBufferToBase64(salt),
    iv
  };
}

/**
 * Decrypt private key with password-derived key
 */
export async function decryptPrivateKey(
  encryptedKey: string,
  iv: string,
  salt: string,
  password: string
): Promise<CryptoKey> {
  const saltBuffer = base64ToArrayBuffer(salt);
  const derivedKey = await deriveKeyFromPassword(password, saltBuffer);

  const privateKeyJwk = await decryptWithKey(encryptedKey, iv, derivedKey);
  return importPrivateKey(privateKeyJwk);
}

// ============================================================================
// Recovery Code Generation (E2E Tier)
// ============================================================================

/**
 * Generate recovery codes for E2E tier
 * Returns 10 codes, each 20 characters (4 groups of 5)
 */
export function generateRecoveryCodes(count: number = 10): string[] {
  const codes: string[] = [];
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Exclude ambiguous chars

  for (let i = 0; i < count; i++) {
    const bytes = generateRandomBytes(20);
    let code = '';

    for (let j = 0; j < 20; j++) {
      code += chars[bytes[j] % chars.length];
      if ((j + 1) % 5 === 0 && j < 19) {
        code += '-';
      }
    }

    codes.push(code);
  }

  return codes;
}

// ============================================================================
// Combined Encryption Payload
// ============================================================================

/**
 * Encrypted payload format
 * Includes all data needed for decryption
 */
export interface EncryptedPayload {
  ciphertext: string;
  iv: string;
  algorithm: string;
  version: number; // For future algorithm changes
}

/**
 * Create encrypted payload
 */
export function createEncryptedPayload(
  ciphertext: string,
  iv: string
): EncryptedPayload {
  return {
    ciphertext,
    iv,
    algorithm: AES_ALGORITHM,
    version: 1
  };
}

/**
 * Serialize encrypted payload to string
 */
export function serializePayload(payload: EncryptedPayload): string {
  return JSON.stringify(payload);
}

/**
 * Deserialize encrypted payload from string
 */
export function deserializePayload(data: string): EncryptedPayload {
  return JSON.parse(data);
}

// ============================================================================
// Validation Functions
// ============================================================================

/**
 * Validate password strength
 * Minimum requirements:
 * - 12 characters
 * - 1 uppercase, 1 lowercase, 1 digit, 1 special char
 */
export function validatePasswordStrength(password: string): {
  isValid: boolean;
  errors: string[];
} {
  const errors: string[] = [];

  if (password.length < 12) {
    errors.push('Password must be at least 12 characters');
  }

  if (!/[A-Z]/.test(password)) {
    errors.push('Password must contain at least one uppercase letter');
  }

  if (!/[a-z]/.test(password)) {
    errors.push('Password must contain at least one lowercase letter');
  }

  if (!/[0-9]/.test(password)) {
    errors.push('Password must contain at least one number');
  }

  if (!/[^A-Za-z0-9]/.test(password)) {
    errors.push('Password must contain at least one special character');
  }

  return {
    isValid: errors.length === 0,
    errors
  };
}

/**
 * Check if Web Crypto API is available
 */
export function isCryptoSupported(): boolean {
  return (
    typeof window !== 'undefined' &&
    'crypto' in window &&
    'subtle' in window.crypto &&
    typeof window.crypto.subtle !== 'undefined'
  );
}

/**
 * Check if browser is in secure context (HTTPS required for Web Crypto)
 */
export function isSecureContext(): boolean {
  return typeof window !== 'undefined' && window.isSecureContext;
}
