/**
 * Encryption Service Exports
 */

export {
  EncryptionServiceFactory,
  E2EEncryptionService,
  UCEEncryptionService,
  RecoveryCodeManager,
  validatePassword,
  isCryptoSupported,
  isSecureContext
} from './EncryptionService';

export type { IEncryptionService } from './EncryptionService';

export * as CryptoUtils from './crypto';

// Export factory as EncryptionService for backward compatibility
export { EncryptionServiceFactory as EncryptionService } from './EncryptionService';
