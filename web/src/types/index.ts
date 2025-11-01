/**
 * Core Type Definitions for Personal Diary Platform
 *
 * This file defines all TypeScript interfaces and types used across the application.
 * Based on the project specification's data model.
 */

// ============================================================================
// Encryption Tier Types
// ============================================================================

/**
 * Encryption tier - set at signup and immutable
 */
export enum EncryptionTier {
  E2E = 'e2e', // End-to-End Encrypted (keys never leave device)
  UCE = 'uce'  // User-Controlled Encryption (server can decrypt with password)
}

// ============================================================================
// User Types
// ============================================================================

export interface User {
  id: string;
  email: string;
  encryptionTier: EncryptionTier;
  createdAt: string;
  updatedAt: string;

  // Tier-specific fields (only populated based on tier)
  // E2E tier
  publicKey?: string;

  // UCE tier
  encryptedMasterKey?: string;
  keyDerivationSalt?: string;
}

export interface UserProfile extends User {
  displayName?: string;
  avatar?: string;
  storageUsed: number;
  storageLimit: number;
  tier: 'free' | 'paid';
}

// ============================================================================
// Entry Types
// ============================================================================

export interface Entry {
  id: string;
  userId: string;

  // Encrypted content
  encryptedContent: string; // Base64 encrypted text
  contentHash: string; // SHA-256 hash for deduplication

  // Metadata (not encrypted)
  source: EntrySource;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  deletedAt?: string; // Soft delete

  // Media references
  mediaIds: string[];

  // Sync status
  syncStatus: SyncStatus;
  lastSyncedAt?: string;
}

export enum EntrySource {
  DIARY = 'diary',
  FACEBOOK = 'facebook',
  INSTAGRAM = 'instagram'
}

export enum SyncStatus {
  PENDING = 'pending',
  SYNCING = 'syncing',
  SYNCED = 'synced',
  FAILED = 'failed',
  CONFLICT = 'conflict'
}

/**
 * Decrypted entry - only exists in memory, never persisted
 */
export interface DecryptedEntry extends Entry {
  content: string; // Decrypted plaintext
}

// ============================================================================
// Media Types
// ============================================================================

export interface Media {
  id: string;
  entryId: string;

  // Encrypted blob reference
  encryptedData?: ArrayBuffer; // Local storage
  s3Key?: string; // Server storage

  // Metadata
  mimeType: string;
  fileSize: number;
  fileHash: string; // SHA-256

  // Image/video specific
  width?: number;
  height?: number;
  duration?: number;

  createdAt: string;
  uploadStatus: UploadStatus;
}

export enum UploadStatus {
  PENDING = 'pending',
  UPLOADING = 'uploading',
  UPLOADED = 'uploaded',
  FAILED = 'failed'
}

// ============================================================================
// External Integration Types
// ============================================================================

export interface ExternalPost {
  id: string;
  entryId: string;
  platform: 'facebook' | 'instagram' | 'twitter';
  externalPostId: string;
  externalUrl: string;
  postedAt: string;
  syncStatus: SyncStatus;
}

export interface IntegrationAccount {
  id: string;
  userId: string;
  platform: 'facebook' | 'instagram' | 'twitter';

  // OAuth tokens (encrypted)
  encryptedAccessToken: string;
  encryptedRefreshToken?: string;
  tokenExpiresAt?: string;

  // Configuration
  autoImport: boolean;
  importSince?: string; // ISO date

  status: 'active' | 'expired' | 'revoked';
  createdAt: string;
  updatedAt: string;
}

// ============================================================================
// Search Types
// ============================================================================

export interface SearchQuery {
  query: string;
  filters?: SearchFilters;
  page?: number;
  perPage?: number;
}

export interface SearchFilters {
  tags?: string[];
  startDate?: string;
  endDate?: string;
  source?: EntrySource;
  sortBy?: 'relevance' | 'date-newest' | 'date-oldest';
}

export interface SearchResult {
  entries: DecryptedEntry[];
  total: number;
  page: number;
  perPage: number;
}

// ============================================================================
// Encryption Types
// ============================================================================

/**
 * Encryption keys stored in IndexedDB (encrypted at rest)
 */
export interface StoredKeys {
  userId: string;
  encryptionTier: EncryptionTier;

  // E2E keys
  publicKey?: string;
  encryptedPrivateKey?: string; // Encrypted with password

  // UCE keys
  masterKey?: string; // Decrypted master key (in memory only)
  encryptedMasterKey?: string; // From server
  derivedKey?: string; // From password (in memory only)
}

/**
 * Recovery codes for E2E tier
 */
export interface RecoveryCode {
  code: string;
  used: boolean;
  usedAt?: string;
}

// ============================================================================
// Authentication Types
// ============================================================================

export interface LoginCredentials {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface SignupData extends LoginCredentials {
  encryptionTier: EncryptionTier;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

// ============================================================================
// API Response Types
// ============================================================================

export interface ApiResponse<T> {
  data?: T;
  error?: ApiError;
  success: boolean;
}

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  perPage: number;
  hasNext: boolean;
}

// ============================================================================
// Sync Types
// ============================================================================

export interface SyncOperation {
  id: string;
  type: 'create' | 'update' | 'delete';
  entityType: 'entry' | 'media' | 'tag';
  entityId: string;
  data: any;
  createdAt: string;
  retryCount: number;
  lastError?: string;
}

export interface SyncState {
  isSyncing: boolean;
  pendingOperations: number;
  lastSyncAt?: string;
  lastError?: string;
}

// ============================================================================
// Settings Types
// ============================================================================

export interface AppSettings {
  // Appearance
  theme: 'light' | 'dark' | 'system';

  // Sync
  autoSync: boolean;
  syncInterval: number; // minutes

  // Privacy
  clearCacheOnLogout: boolean;

  // Social integrations
  integrations: {
    facebook: boolean;
    instagram: boolean;
  };
}

// ============================================================================
// Component Prop Types
// ============================================================================

export interface EntryCardProps {
  entry: DecryptedEntry;
  onEdit?: (entry: DecryptedEntry) => void;
  onDelete?: (entryId: string) => void;
  onShare?: (entry: DecryptedEntry) => void;
}

export interface EncryptionBadgeProps {
  tier: EncryptionTier;
  className?: string;
}

// ============================================================================
// Utility Types
// ============================================================================

/**
 * Makes all properties optional recursively
 */
export type DeepPartial<T> = {
  [P in keyof T]?: T[P] extends object ? DeepPartial<T[P]> : T[P];
};

/**
 * Omit multiple keys
 */
export type OmitMultiple<T, K extends keyof T> = Omit<T, K>;

/**
 * Pick multiple keys
 */
export type PickMultiple<T, K extends keyof T> = Pick<T, K>;
