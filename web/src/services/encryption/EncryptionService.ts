/**
 * Encryption Service - Dual-Tier Implementation
 *
 * Handles encryption/decryption for both E2E and UCE tiers.
 * Provides unified interface while respecting tier-specific constraints.
 */

import { EncryptionTier, StoredKeys, RecoveryCode } from '@/types';
import * as crypto from './crypto';

// ============================================================================
// Service Interface
// ============================================================================

export interface IEncryptionService {
  /**
   * Initialize encryption for a user (called after signup/login)
   */
  initialize(tier: EncryptionTier, password: string): Promise<void>;

  /**
   * Encrypt entry content
   */
  encryptContent(content: string): Promise<string>;

  /**
   * Decrypt entry content
   */
  decryptContent(encrypted: string): Promise<string>;

  /**
   * Encrypt media data
   */
  encryptMedia(data: ArrayBuffer): Promise<ArrayBuffer>;

  /**
   * Decrypt media data
   */
  decryptMedia(encrypted: ArrayBuffer): Promise<ArrayBuffer>;

  /**
   * Generate content hash for deduplication
   */
  generateContentHash(content: string): Promise<string>;

  /**
   * Get current encryption tier
   */
  getTier(): EncryptionTier | null;

  /**
   * Clear all encryption keys from memory
   */
  clearKeys(): void;
}

// ============================================================================
// E2E Encryption Service
// ============================================================================

class E2EEncryptionService implements IEncryptionService {
  private privateKey: CryptoKey | null = null;
  private publicKey: CryptoKey | null = null;
  private encryptionKey: CryptoKey | null = null; // Derived from private key
  private isInitialized = false;

  async initialize(tier: EncryptionTier, password: string): Promise<void> {
    if (tier !== EncryptionTier.E2E) {
      throw new Error('E2EEncryptionService can only be used with E2E tier');
    }

    // Check if keys exist in storage
    const storedKeys = await this.loadKeysFromStorage();

    if (storedKeys) {
      // Decrypt private key with password
      this.privateKey = await crypto.decryptPrivateKey(
        storedKeys.encryptedPrivateKey!,
        storedKeys.iv!,
        storedKeys.salt!,
        password
      );

      this.publicKey = await crypto.importPublicKey(storedKeys.publicKey!);
    } else {
      // Generate new keypair (signup flow)
      const keypair = await crypto.generateE2EKeypair();
      this.privateKey = keypair.privateKey;
      this.publicKey = keypair.publicKey;

      // Encrypt and store private key
      const { encrypted, salt, iv } = await crypto.encryptPrivateKey(
        this.privateKey,
        password
      );

      const publicKeyJwk = await crypto.exportPublicKey(this.publicKey);

      await this.saveKeysToStorage({
        encryptedPrivateKey: encrypted,
        publicKey: publicKeyJwk,
        salt,
        iv
      });
    }

    // For content encryption, we'll use a deterministic key derivation
    // from the private key to ensure consistent encryption across sessions
    this.encryptionKey = await this.deriveContentKey();
    this.isInitialized = true;
  }

  /**
   * Derive content encryption key from private key
   * This ensures we can decrypt content across sessions
   */
  private async deriveContentKey(): Promise<CryptoKey> {
    if (!this.privateKey) {
      throw new Error('Private key not initialized');
    }

    // Export private key and hash it to derive content key
    const privateKeyJwk = await crypto.exportPrivateKey(this.privateKey);
    const keyData = crypto.stringToArrayBuffer(privateKeyJwk);
    const hash = await window.crypto.subtle.digest('SHA-256', keyData);

    // Import hash as AES key
    return window.crypto.subtle.importKey(
      'raw',
      hash,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    );
  }

  async encryptContent(content: string): Promise<string> {
    this.ensureInitialized();

    const { ciphertext, iv } = await crypto.encryptWithKey(
      content,
      this.encryptionKey!
    );

    const payload = crypto.createEncryptedPayload(ciphertext, iv);
    return crypto.serializePayload(payload);
  }

  async decryptContent(encrypted: string): Promise<string> {
    this.ensureInitialized();

    const payload = crypto.deserializePayload(encrypted);
    return crypto.decryptWithKey(
      payload.ciphertext,
      payload.iv,
      this.encryptionKey!
    );
  }

  async encryptMedia(data: ArrayBuffer): Promise<ArrayBuffer> {
    this.ensureInitialized();

    // Convert ArrayBuffer to base64 string for encryption
    const base64 = crypto.arrayBufferToBase64(data);
    const encrypted = await this.encryptContent(base64);

    // Convert back to ArrayBuffer
    return crypto.stringToArrayBuffer(encrypted);
  }

  async decryptMedia(encrypted: ArrayBuffer): Promise<ArrayBuffer> {
    this.ensureInitialized();

    // Convert ArrayBuffer to string
    const encryptedString = crypto.arrayBufferToString(encrypted);
    const decrypted = await this.decryptContent(encryptedString);

    // Convert base64 back to ArrayBuffer
    return crypto.base64ToArrayBuffer(decrypted);
  }

  async generateContentHash(content: string): Promise<string> {
    return crypto.generateHash(content);
  }

  getTier(): EncryptionTier {
    return EncryptionTier.E2E;
  }

  clearKeys(): void {
    this.privateKey = null;
    this.publicKey = null;
    this.encryptionKey = null;
    this.isInitialized = false;
  }

  async getPublicKey(): Promise<string> {
    if (!this.publicKey) {
      throw new Error('Public key not initialized');
    }
    return crypto.exportPublicKey(this.publicKey);
  }

  private ensureInitialized(): void {
    if (!this.isInitialized || !this.encryptionKey) {
      throw new Error('Encryption service not initialized');
    }
  }

  private async loadKeysFromStorage(): Promise<any> {
    // Will be implemented with IndexedDB service
    const stored = localStorage.getItem('e2e_keys');
    return stored ? JSON.parse(stored) : null;
  }

  private async saveKeysToStorage(keys: any): Promise<void> {
    // Will be implemented with IndexedDB service
    localStorage.setItem('e2e_keys', JSON.stringify(keys));
  }
}

// ============================================================================
// UCE Encryption Service
// ============================================================================

class UCEEncryptionService implements IEncryptionService {
  private masterKey: CryptoKey | null = null;
  private derivedKey: CryptoKey | null = null;
  private isInitialized = false;

  async initialize(tier: EncryptionTier, password: string): Promise<void> {
    if (tier !== EncryptionTier.UCE) {
      throw new Error('UCEEncryptionService can only be used with UCE tier');
    }

    // Check if encrypted master key exists in storage
    const storedKeys = await this.loadKeysFromStorage();

    if (storedKeys?.encryptedMasterKey) {
      // Decrypt master key with password-derived key
      const salt = crypto.base64ToArrayBuffer(storedKeys.salt!);
      this.derivedKey = await crypto.deriveKeyFromPassword(password, new Uint8Array(salt));

      const decryptedMasterKey = await crypto.decryptWithKey(
        storedKeys.encryptedMasterKey,
        storedKeys.iv,
        this.derivedKey
      );

      this.masterKey = await crypto.importMasterKey(decryptedMasterKey);
    } else {
      // Generate new master key (signup flow)
      this.masterKey = await crypto.generateMasterKey();

      // Derive key from password
      const salt = crypto.generateSalt();
      this.derivedKey = await crypto.deriveKeyFromPassword(password, salt);

      // Encrypt master key with password-derived key
      const exportedMasterKey = await crypto.exportMasterKey(this.masterKey);
      const { ciphertext, iv } = await crypto.encryptWithKey(
        exportedMasterKey,
        this.derivedKey
      );

      await this.saveKeysToStorage({
        encryptedMasterKey: ciphertext,
        salt: crypto.arrayBufferToBase64(salt),
        iv
      });
    }

    this.isInitialized = true;
  }

  async encryptContent(content: string): Promise<string> {
    this.ensureInitialized();

    const { ciphertext, iv } = await crypto.encryptWithKey(
      content,
      this.masterKey!
    );

    const payload = crypto.createEncryptedPayload(ciphertext, iv);
    return crypto.serializePayload(payload);
  }

  async decryptContent(encrypted: string): Promise<string> {
    this.ensureInitialized();

    const payload = crypto.deserializePayload(encrypted);
    return crypto.decryptWithKey(
      payload.ciphertext,
      payload.iv,
      this.masterKey!
    );
  }

  async encryptMedia(data: ArrayBuffer): Promise<ArrayBuffer> {
    this.ensureInitialized();

    const base64 = crypto.arrayBufferToBase64(data);
    const encrypted = await this.encryptContent(base64);
    return crypto.stringToArrayBuffer(encrypted);
  }

  async decryptMedia(encrypted: ArrayBuffer): Promise<ArrayBuffer> {
    this.ensureInitialized();

    const encryptedString = crypto.arrayBufferToString(encrypted);
    const decrypted = await this.decryptContent(encryptedString);
    return crypto.base64ToArrayBuffer(decrypted);
  }

  async generateContentHash(content: string): Promise<string> {
    return crypto.generateHash(content);
  }

  getTier(): EncryptionTier {
    return EncryptionTier.UCE;
  }

  clearKeys(): void {
    this.masterKey = null;
    this.derivedKey = null;
    this.isInitialized = false;
  }

  async getEncryptedMasterKey(): Promise<string> {
    const stored = await this.loadKeysFromStorage();
    if (!stored?.encryptedMasterKey) {
      throw new Error('Encrypted master key not found');
    }
    return stored.encryptedMasterKey;
  }

  private ensureInitialized(): void {
    if (!this.isInitialized || !this.masterKey) {
      throw new Error('Encryption service not initialized');
    }
  }

  private async loadKeysFromStorage(): Promise<any> {
    // Will be implemented with IndexedDB service
    const stored = localStorage.getItem('uce_keys');
    return stored ? JSON.parse(stored) : null;
  }

  private async saveKeysToStorage(keys: any): Promise<void> {
    // Will be implemented with IndexedDB service
    localStorage.setItem('uce_keys', JSON.stringify(keys));
  }
}

// ============================================================================
// Encryption Service Factory
// ============================================================================

class EncryptionServiceFactory {
  private static instance: IEncryptionService | null = null;
  private static currentTier: EncryptionTier | null = null;

  static async createService(
    tier: EncryptionTier,
    password: string
  ): Promise<IEncryptionService> {
    // Clear existing instance if tier changed
    if (this.instance && this.currentTier !== tier) {
      this.instance.clearKeys();
      this.instance = null;
    }

    if (!this.instance) {
      this.instance =
        tier === EncryptionTier.E2E
          ? new E2EEncryptionService()
          : new UCEEncryptionService();

      await this.instance.initialize(tier, password);
      this.currentTier = tier;
    }

    return this.instance;
  }

  static getInstance(): IEncryptionService | null {
    return this.instance;
  }

  static clearInstance(): void {
    if (this.instance) {
      this.instance.clearKeys();
      this.instance = null;
      this.currentTier = null;
    }
  }
}

// ============================================================================
// Recovery Code Management (E2E Tier)
// ============================================================================

export class RecoveryCodeManager {
  /**
   * Generate recovery codes for E2E tier
   */
  static generateCodes(count: number = 10): string[] {
    return crypto.generateRecoveryCodes(count);
  }

  /**
   * Hash recovery codes for storage
   */
  static async hashCodes(codes: string[]): Promise<RecoveryCode[]> {
    return Promise.all(
      codes.map(async (code) => ({
        code: await crypto.hashRecoveryCode(code),
        used: false
      }))
    );
  }

  /**
   * Verify recovery code against hash
   */
  static async verifyCode(
    code: string,
    hashedCode: string
  ): Promise<boolean> {
    const hash = await crypto.hashRecoveryCode(code);
    return hash === hashedCode;
  }
}

// ============================================================================
// Exports
// ============================================================================

export { EncryptionServiceFactory, E2EEncryptionService, UCEEncryptionService };
export type { IEncryptionService };

// Validation utilities
export const validatePassword = crypto.validatePasswordStrength;
export const isCryptoSupported = crypto.isCryptoSupported;
export const isSecureContext = crypto.isSecureContext;
