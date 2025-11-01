# Dual-Tier Encryption Architecture - AI Agent Implementation Guide

**Project**: Privacy-First Diary Platform  
**Document Version**: 1.0  
**Last Updated**: October 31, 2025  
**Target**: Engineering AI Agent (Claude Code or similar)  
**Platform Priority**: Mobile-first (iOS, Android) + Web

---

## 📋 Table of Contents

1. [Purpose & Scope](#purpose--scope)
2. [Platform Strategy](#platform-strategy)
3. [System Architecture](#system-architecture)
4. [Encryption Tier Specifications](#encryption-tier-specifications)
5. [Data Model Requirements](#data-model-requirements)
6. [API Endpoint Specifications](#api-endpoint-specifications)
7. [User Flows](#user-flows)
8. [Search Implementation](#search-implementation)
9. [Social Media Integration](#social-media-integration)
10. [Multi-Device Sync](#multi-device-sync)
11. [Security Requirements](#security-requirements)
12. [Testing Requirements](#testing-requirements)
13. [Deployment Strategy](#deployment-strategy)
14. [Implementation Checklist](#implementation-checklist)

---

## Purpose & Scope

### What This Document Contains

This document provides **specifications and requirements** for an AI agent to implement a dual-tier encryption diary platform. The AI agent should make technical implementation decisions while following these architectural guidelines.

**Includes:**
- System requirements and architecture
- Feature specifications
- API contracts (input/output definitions)
- User experience flows
- Security requirements
- Testing requirements

**Excludes:**
- Actual code implementations (agent decides)
- Specific library choices (agent decides)
- SQL statements (agent designs schema)
- CLI commands (agent chooses tools)

### Key Principle

The AI agent has **full freedom** to choose:
- Programming languages and frameworks
- Libraries and dependencies
- Database schema design
- Implementation patterns
- Testing frameworks

The AI agent **must follow**:
- Security requirements
- API contracts
- Feature specifications
- Mobile-first approach
- Dual-tier encryption architecture

---

## Platform Strategy

### 🎯 Mobile-First Approach

This is a mobile-first platform. Desktop/web is secondary.

### Primary Platforms (MVP Priority Order)

**1. iOS Native App (Priority 1)**
- Target: iOS 16+
- Framework: Agent chooses (Swift/SwiftUI recommended)
- Requirements:
  - Client-side encryption implementation
  - Keychain for secure key storage
  - Biometric authentication (Face ID/Touch ID)
  - Offline mode with local database
  - Share extension
  - Universal (iPhone + iPad)

**2. Android Native App (Priority 1)**  
- Target: Android 9+ (API 28+)
- Framework: Agent chooses (Kotlin/Jetpack Compose recommended)
- Requirements:
  - Client-side encryption implementation
  - KeyStore for secure key storage
  - Biometric authentication
  - Offline mode with local database
  - Share intent
  - Material Design 3

**3. Web Application (Priority 2)**
- Progressive Web App (PWA) capability
- Framework: Agent chooses (React/Vue/Svelte recommended)
- Responsive design (mobile + desktop)
- Requirements:
  - Web Crypto API for encryption
  - IndexedDB for local storage
  - Service Worker for offline
  - Install as PWA

### Technology Choices for Agent

Agent should choose based on:
- Development speed
- Maintainability
- Performance
- Cross-platform code reuse possibilities
- Community support

**Options to consider:**
- Native (Swift + Kotlin): Best performance, best UX
- React Native: Code reuse, faster development
- Flutter: Code reuse, good performance

**Agent decides which is best for this project.**

---

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Client Applications                    │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ iOS App  │  │Android   │  │ Web App  │             │
│  │          │  │  App     │  │  (PWA)   │             │
│  │ Encrypt  │  │ Encrypt  │  │ Encrypt  │             │
│  │ Local DB │  │ Local DB │  │IndexedDB │             │
│  └──────────┘  └──────────┘  └──────────┘             │
└───────────────────────┬──────────────────────────────────┘
                        │
                        │ HTTPS REST API / GraphQL
                        │
┌───────────────────────▼──────────────────────────────────┐
│                   API Gateway Layer                       │
│  - Authentication & Authorization (OAuth2/JWT)           │
│  - Rate Limiting                                          │
│  - Request Validation                                     │
│  - CORS Configuration                                     │
└───────────────────────┬──────────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────────┐
│              Encryption Tier Router                       │
│  - Detect user's encryption tier from JWT                │
│  - Route requests to appropriate encryption service      │
│  - Enforce tier-specific feature access                  │
└───────────────────────┬──────────────────────────────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
┌───────▼────────┐             ┌────────▼──────┐
│  E2E Service   │             │  UCE Service  │
│                │             │               │
│ No decryption  │             │ Can decrypt   │
│ Store pub key  │             │ Key derivation│
│ Validate only  │             │ Server search │
└───────┬────────┘             └────────┬──────┘
        │                               │
        └───────────────┬───────────────┘
                        │
┌───────────────────────▼──────────────────────────────────┐
│              Core Business Logic Layer                    │
│                                                           │
│  Entry Management  │  Tag System  │  Media Storage       │
│  Social Integration│  Events/Audit│  Feature Gates       │
└───────────────────────┬──────────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────────┐
│              Background Job Processing                    │
│  - Facebook import (async)                               │
│  - Scheduled sync jobs                                    │
│  - Media processing                                       │
│  - Retry failed operations                                │
└───────────────────────┬──────────────────────────────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
┌───────▼────────┐             ┌────────▼──────┐
│   Database     │             │  Blob Storage │
│  (PostgreSQL)  │             │    (S3-like)  │
│                │             │               │
│  Users         │             │  Encrypted    │
│  Entries       │             │  Media Files  │
│  External Posts│             │               │
└────────────────┘             └───────────────┘
```

### Microservices Architecture

**Agent should implement:**

1. **Core API Service**
   - Handles all client requests
   - User authentication
   - Entry CRUD
   - Encryption tier routing
   - Feature gate enforcement

2. **Integration Service** (can be separate or part of Core API)
   - Facebook OAuth and API calls
   - Future: Instagram, Twitter, etc.
   - Common integration interface

3. **Background Worker Service**
   - Async job processing
   - Facebook import jobs
   - Scheduled tasks
   - Retry logic

4. **Storage Adapter** (abstraction layer)
   - S3-compatible interface
   - Pluggable backends (S3, Google Cloud Storage, Azure, local)

### Deployment Strategy

**MVP: Docker + Docker Compose**
- Each service in Docker container
- Docker Compose for local development
- Simple deployment to single server or VPS
- No Kubernetes complexity
- Easy to develop and test

**Post-MVP: Kubernetes Migration**
- When scale demands (thousands of users)
- Microservices already prepared
- Minimal code changes needed

**Agent should:**
- Create Dockerfiles for each service
- Create docker-compose.yml for orchestration
- Document deployment process
- Keep K8s migration in mind (but don't implement)

---

## Encryption Tier Specifications

### Overview

Users choose ONE encryption tier at signup. This choice is **permanent and immutable**.

Two tiers:
1. **E2E** - End-to-End Encrypted
2. **UCE** - User-Controlled Encryption

### Tier 1: E2E (End-to-End Encrypted)

#### Cryptographic Requirements

**Agent must implement:**
- Industry-standard public key cryptography
- Recommended: X25519 (key exchange) + ChaCha20-Poly1305 or XSalsa20-Poly1305 (encryption)
- Alternative: RSA + AES-GCM (if agent prefers)
- Key size: Minimum 256-bit

**Key Generation:**
- **Where**: Client device only
- **When**: During signup
- **Process**:
  1. Generate keypair on client
  2. Store private key in secure storage (Keychain/KeyStore/IndexedDB)
  3. Send public key to server
  4. Server stores public key only

**Key Storage by Platform:**
- iOS: Keychain Services
- Android: Android KeyStore
- Web: IndexedDB + Web Crypto API (with appropriate protections)

#### E2E Capabilities Matrix

| Feature | Supported | Implementation Notes |
|---------|-----------|---------------------|
| Create encrypted entries | ✅ Yes | Client encrypts before sending |
| Read encrypted entries | ✅ Yes | Client decrypts after receiving |
| Upload encrypted media | ✅ Yes | Client encrypts files |
| Push to Facebook | ✅ Yes | Client decrypts → send plaintext |
| Pull from Facebook | ✅ Yes | Server sends plaintext → client encrypts |
| Server-side search | ❌ No | Server has no keys |
| Server-side AI | ❌ No | Server cannot decrypt |
| Password reset | ❌ No | Recovery codes only |
| Easy multi-device | ⚠️ Limited | Manual key transfer required |
| Share with other users | ⚠️ Future | Requires key exchange protocol |

#### E2E Recovery Mechanism

**Recovery Codes Only:**
- Generate 10 recovery codes at signup
- Format: `XXXX-XXXX-XXXX-XXXX` (16 hex chars per code)
- User MUST save these codes
- Server stores SHA-256 hash of each code
- If lost, account is irrecoverable

**Agent must implement:**
1. Recovery code generation
2. Display codes to user with strong warnings
3. Force user to confirm they've saved codes
4. Download as text file option
5. Copy to clipboard option
6. Validation during account recovery

#### E2E Multi-Device Sync

**Manual Process:**

Option A: QR Code Transfer (recommended)
- Primary device generates QR code containing encrypted private key
- Secondary device scans QR code
- Secondary device decrypts and stores private key

Option B: Recovery Code Entry
- User enters recovery code on new device
- Server provides encrypted private key
- Device decrypts private key with recovery code

**Agent implements:** At minimum, recovery code method. QR code is nice-to-have.

---

### Tier 2: UCE (User-Controlled Encryption)

#### Cryptographic Requirements

**Agent must implement:**
- Password-based key derivation
- Recommended: Argon2id
- Alternative: PBKDF2 (100,000+ iterations)
- Symmetric encryption: AES-256-GCM or similar
- Key size: 256-bit

**Key Derivation Process:**

1. **At Signup:**
   - Server generates master encryption key (256-bit random)
   - Server derives key from user password using Argon2id
   - Server encrypts master key with derived key
   - Server stores: encrypted_master_key + salt

2. **At Login (any device):**
   - User enters password
   - Server derives key from password + stored salt
   - Server decrypts master key
   - Server or client can now encrypt/decrypt user data

**Key Derivation Parameters (Argon2id):**
- Time cost: 2 iterations minimum
- Memory cost: 64 MB minimum
- Parallelism: 1
- Salt: 16 bytes random per user

**Alternative (PBKDF2):**
- Iterations: 100,000 minimum (prefer 600,000+)
- Hash function: SHA-256
- Salt: 16 bytes random per user

#### UCE Capabilities Matrix

| Feature | Supported | Implementation Notes |
|---------|-----------|---------------------|
| Create encrypted entries | ✅ Yes | Client or server can encrypt |
| Read encrypted entries | ✅ Yes | Server decrypts and sends OR sends encrypted to client |
| Upload encrypted media | ✅ Yes | Client or server encrypts |
| Push to Facebook | ✅ Yes | Server can decrypt and push |
| Pull from Facebook | ✅ Yes | Server encrypts and stores |
| Server-side search | ✅ Yes | Server decrypts for indexing |
| Server-side AI | ✅ Yes | Server can analyze content |
| Password reset | ✅ Yes | Standard email reset flow |
| Easy multi-device | ✅ Yes | Automatic with password |
| Share with other users | ✅ Yes | Server facilitates sharing |

#### UCE Account Recovery

**Multiple Options:**

1. **Password Reset (Primary)**
   - Standard email link flow
   - Generate new password
   - Re-encrypt master key with new derived key

2. **Recovery Codes (Optional)**
   - Like E2E, but optional additional security
   - User can set up recovery codes
   - Can use to bypass email reset

3. **Security Questions (Optional)**
   - Additional recovery method
   - Agent decides if implementing

**Agent implements:** At minimum, password reset via email.

#### UCE Multi-Device Sync

**Automatic:**
- User signs in with email + password on any device
- Server derives key, decrypts master key
- Device can now encrypt/decrypt all user data
- No manual key transfer needed

---

### Encryption Service Architecture

**Agent must implement Strategy Pattern:**

```
Base Interface:
├── encrypt_entry(content: str, user: User) -> str
├── decrypt_entry(encrypted: str, user: User) -> str
├── supports_server_search() -> bool
├── supports_server_ai() -> bool
├── supports_easy_recovery() -> bool

E2E Implementation:
├── Uses public key crypto
├── Private key never on server
├── All methods return appropriate errors for unsupported features

UCE Implementation:
├── Uses password-derived key
├── Master key on server (encrypted)
├── All methods fully functional

Factory/Router:
├── Determines user's tier
├── Returns appropriate service instance
```

**Agent designs specific implementation.**

---

## Data Model Requirements

Agent must design database schema with these entities and relationships:

### Entity: User

**Purpose:** Store user account information

**Required Fields:**
- `id` (primary key)
- `email` (unique, validated)
- `password_hash` (Bcrypt/Argon2)
- `encryption_tier` (enum: 'e2e' or 'uce') **IMMUTABLE**
- `created_at` (timestamp)
- `updated_at` (timestamp)
- `last_login_at` (timestamp, nullable)

**Conditional Fields (based on tier):**

For E2E users:
- `public_key` (base64 encoded, 32-64 bytes)

For UCE users:
- `encrypted_master_key` (base64 encoded)
- `key_derivation_salt` (base64 encoded, 16+ bytes)

**Relationships:**
- One-to-many: Entries
- One-to-many: Media
- One-to-many: Integration accounts
- One-to-many: Recovery codes (if E2E)

**Constraints:**
- Email must be unique and validated
- Encryption tier cannot be null or changed after creation
- Appropriate tier-specific fields must be populated

**Indexes:**
- Primary key on id
- Unique index on email
- Index on encryption_tier (for analytics)

---

### Entity: Entry

**Purpose:** Store diary entries (encrypted)

**Required Fields:**
- `id` (primary key)
- `user_id` (foreign key → User)
- `encrypted_content` (text, encrypted diary entry)
- `content_hash` (char 64, SHA-256 hex for deduplication)
- `source` (enum: 'diary', 'facebook', 'instagram', etc.)
- `created_at` (timestamp)
- `updated_at` (timestamp)
- `deleted_at` (timestamp, nullable - soft delete)

**Optional Fields:**
- `encrypted_title` (text, encrypted, nullable)
- `location` (text, encrypted, nullable)
- `weather` (json, nullable)
- `mood` (enum, nullable)

**Relationships:**
- Many-to-one: User
- One-to-many: Media
- One-to-many: Tags
- One-to-one: External post mapping (optional)
- One-to-many: Entry events

**Constraints:**
- Content hash must be 64 character hex string
- Source is immutable after creation
- Soft delete: deleted_at set instead of actual deletion

**Indexes:**
- Primary key on id
- Foreign key on user_id
- Index on (user_id, created_at) for timeline queries
- Index on content_hash for deduplication
- Index on source for filtering
- Index on deleted_at for soft delete queries

---

### Entity: External_Post

**Purpose:** Map diary entries to social media posts

**Required Fields:**
- `id` (primary key)
- `entry_id` (foreign key → Entry)
- `platform` (enum: 'facebook', 'instagram', 'twitter', etc.)
- `external_post_id` (varchar, platform's post ID)
- `external_url` (text, URL to post)
- `sync_status` (enum: 'pending', 'synced', 'failed', 'out_of_sync')
- `posted_at` (timestamp, when posted to platform)
- `created_at` (timestamp, when mapping created)
- `updated_at` (timestamp)

**Optional Fields:**
- `error_message` (text, if sync failed)
- `retry_count` (integer, number of retry attempts)
- `last_sync_at` (timestamp, last sync attempt)

**Relationships:**
- Many-to-one: Entry
- Many-to-one: Integration account

**Constraints:**
- Combination of (entry_id, platform) should be unique
- External post ID format validated per platform

**Indexes:**
- Primary key on id
- Foreign key on entry_id
- Unique index on (entry_id, platform)
- Index on external_post_id for lookups
- Index on sync_status for job processing

---

### Entity: Tag

**Purpose:** Categorize entries with tags

**Required Fields:**
- `id` (primary key)
- `entry_id` (foreign key → Entry)
- `tag_name` (varchar 100, case-insensitive)
- `auto_generated` (boolean, system-generated vs user-created)
- `created_at` (timestamp)

**Relationships:**
- Many-to-one: Entry

**Constraints:**
- Tag names are case-insensitive (normalize to lowercase)
- Maximum length enforced
- Combination of (entry_id, tag_name) should be unique

**Indexes:**
- Primary key on id
- Foreign key on entry_id
- Index on (entry_id, tag_name) for lookups
- Index on tag_name for autocomplete

---

### Entity: Media

**Purpose:** Store encrypted media files (photos, videos)

**Required Fields:**
- `id` (primary key)
- `entry_id` (foreign key → Entry, nullable if media not yet attached)
- `user_id` (foreign key → User)
- `encrypted_file_reference` (text, S3 key or file path)
- `file_hash` (char 64, SHA-256 of encrypted file)
- `mime_type` (varchar 100)
- `file_size` (bigint, bytes)
- `created_at` (timestamp)

**Optional Fields:**
- `encrypted_original_filename` (text, encrypted)
- `width` (integer, for images)
- `height` (integer, for images)
- `duration` (integer seconds, for videos)
- `thumbnail_reference` (text, S3 key for thumbnail)

**Relationships:**
- Many-to-one: Entry (nullable)
- Many-to-one: User

**Constraints:**
- File size limits enforced (max 50MB per file for free tier)
- Supported MIME types validated
- Entry_id nullable to allow upload before entry creation

**Indexes:**
- Primary key on id
- Foreign key on entry_id
- Foreign key on user_id
- Index on file_hash
- Index on (user_id, created_at)

---

### Entity: Integration_Account

**Purpose:** Store OAuth tokens for social media platforms

**Required Fields:**
- `id` (primary key)
- `user_id` (foreign key → User)
- `platform` (enum: 'facebook', 'instagram', 'twitter', etc.)
- `encrypted_access_token` (text, encrypted OAuth token)
- `encrypted_refresh_token` (text, encrypted, nullable)
- `token_expires_at` (timestamp, nullable)
- `status` (enum: 'active', 'expired', 'revoked')
- `created_at` (timestamp)
- `updated_at` (timestamp)

**Optional Fields:**
- `platform_user_id` (varchar, user ID on platform)
- `platform_username` (varchar, username on platform)
- `scopes` (json, granted OAuth scopes)

**Relationships:**
- Many-to-one: User
- One-to-many: External posts

**Constraints:**
- Combination of (user_id, platform) should be unique (one integration per platform)
- Tokens must be encrypted at rest
- Access tokens must be encrypted with different key than entries

**Indexes:**
- Primary key on id
- Foreign key on user_id
- Unique index on (user_id, platform)
- Index on status for active integrations

---

### Entity: Entry_Event

**Purpose:** Audit trail for entry changes (version history)

**Required Fields:**
- `id` (primary key)
- `entry_id` (foreign key → Entry)
- `event_type` (enum: 'created', 'edited', 'shared', 'imported', 'tagged', 'deleted')
- `event_timestamp` (timestamp)
- `changes` (json, metadata about changes)

**Optional Fields:**
- `device_info` (json, device/browser info)
- `ip_address` (varchar, if tracking)

**Relationships:**
- Many-to-one: Entry

**Use Cases:**
- Version history ("View History" in UI)
- Audit trail for compliance
- Analytics (optional)
- "Undo" functionality (future)

**Indexes:**
- Primary key on id
- Foreign key on entry_id
- Index on (entry_id, event_timestamp) for history queries

---

### Entity: E2E_Public_Key (E2E tier only)

**Purpose:** Store public keys for E2E users

**Required Fields:**
- `id` (primary key)
- `user_id` (foreign key → User, unique)
- `public_key` (varchar 64, base64 encoded 32-byte key)
- `created_at` (timestamp)

**Optional Fields:**
- `key_algorithm` (varchar, e.g., "X25519", "RSA-2048")

**Relationships:**
- One-to-one: User
- One-to-many: Device keys (for future multi-device)

**Constraints:**
- One public key per user
- User must have encryption_tier = 'e2e'

**Indexes:**
- Primary key on id
- Unique foreign key on user_id

---

### Entity: E2E_Recovery_Code (E2E tier only)

**Purpose:** Store recovery codes for E2E users

**Required Fields:**
- `id` (primary key)
- `user_id` (foreign key → User)
- `code_hash` (char 64, SHA-256 hash of recovery code)
- `used` (boolean, default false)
- `used_at` (timestamp, nullable)
- `created_at` (timestamp)

**Relationships:**
- Many-to-one: User

**Constraints:**
- 10 codes per user typically
- Code hash must be SHA-256 (64 hex chars)
- Once used, cannot be reused

**Indexes:**
- Primary key on id
- Foreign key on user_id
- Index on (user_id, used) for finding unused codes
- Index on code_hash for validation

---

## API Endpoint Specifications

Agent must implement these REST API endpoints with specified contracts.

### Base URL
`https://api.yourdomain.com/api/v1`

### Authentication
All endpoints except signup/login require:
- Header: `Authorization: Bearer <JWT_TOKEN>`
- JWT contains: user_id, encryption_tier, issued_at, expires_at

---

### Authentication Endpoints

#### POST `/auth/signup`

**Purpose:** Create new user account with encryption tier selection

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securepassword123",
  "encryption_tier": "e2e" | "uce",
  "public_key": "base64_encoded_public_key" // Required only if tier is "e2e"
}
```

**Response (201 Created):**
```json
{
  "user_id": "uuid",
  "email": "user@example.com",
  "encryption_tier": "e2e" | "uce",
  "jwt_token": "eyJ...",
  "refresh_token": "refresh_eyJ...",
  "recovery_codes": ["XXXX-XXXX-XXXX-XXXX", ...] // Only if E2E, array of 10
}
```

**Error Responses:**
- 400 Bad Request: Invalid input (e.g., missing public_key for E2E)
- 409 Conflict: Email already exists
- 422 Unprocessable Entity: Password too weak

**Validation Rules:**
- Email must be valid format
- Password minimum 12 characters
- If tier is "e2e", public_key is required
- Public_key must be valid base64, decode to 32 bytes

---

#### POST `/auth/login`

**Purpose:** Authenticate user

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securepassword123"
}
```

**Response (200 OK):**
```json
{
  "user_id": "uuid",
  "email": "user@example.com",
  "encryption_tier": "e2e" | "uce",
  "jwt_token": "eyJ...",
  "refresh_token": "refresh_eyJ...",
  
  // If UCE tier:
  "encrypted_master_key": "base64_encoded_encrypted_key",
  
  // If E2E tier:
  "public_key": "base64_encoded_public_key"
}
```

**Error Responses:**
- 401 Unauthorized: Invalid credentials
- 429 Too Many Requests: Rate limit exceeded

---

#### POST `/auth/refresh`

**Purpose:** Refresh JWT token

**Request Headers:**
- `Authorization: Bearer <REFRESH_TOKEN>`

**Response (200 OK):**
```json
{
  "jwt_token": "new_eyJ...",
  "refresh_token": "new_refresh_eyJ..."
}
```

**Error Responses:**
- 401 Unauthorized: Invalid or expired refresh token

---

#### POST `/auth/logout`

**Purpose:** Invalidate tokens

**Request Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Response (204 No Content)**

---

### Entry Endpoints

#### POST `/entries`

**Purpose:** Create new diary entry

**Request Body:**
```json
{
  "encrypted_content": "base64_encrypted_diary_text",
  "content_hash": "sha256_hex_hash",
  "source": "diary" | "facebook" | "instagram",
  "tags": ["vacation", "family"],  // Optional
  "media_ids": ["uuid1", "uuid2"], // Optional
  "encrypted_title": "base64_encrypted_title", // Optional
  "location": "encrypted_location", // Optional
  "mood": "happy" | "sad" | "neutral" // Optional
}
```

**Response (201 Created):**
```json
{
  "entry_id": "uuid",
  "created_at": "2025-10-31T12:00:00Z",
  "updated_at": "2025-10-31T12:00:00Z"
}
```

**Error Responses:**
- 400 Bad Request: Invalid input
- 413 Payload Too Large: Content too large
- 507 Insufficient Storage: User storage quota exceeded

**Validation:**
- encrypted_content required, max size TBD by agent
- content_hash must be 64-char hex string
- source must be valid enum value
- media_ids must reference uploaded media belonging to user

---

#### GET `/entries`

**Purpose:** List user's entries (paginated)

**Query Parameters:**
- `page` (integer, default 1)
- `per_page` (integer, default 20, max 100)
- `source` (string, filter by source, optional)
- `tags` (comma-separated, filter by tags, optional)
- `start_date` (ISO 8601, optional)
- `end_date` (ISO 8601, optional)
- `include_deleted` (boolean, default false)

**Response (200 OK):**
```json
{
  "entries": [
    {
      "entry_id": "uuid",
      "encrypted_content": "base64_encrypted_content",
      "content_hash": "sha256_hex",
      "source": "diary",
      "tags": ["vacation"],
      "media_ids": ["uuid1"],
      "created_at": "2025-10-31T12:00:00Z",
      "updated_at": "2025-10-31T12:00:00Z",
      "external_post": {  // Only if entry was shared
        "platform": "facebook",
        "external_url": "https://facebook.com/..."
      }
    }
  ],
  "pagination": {
    "total": 150,
    "page": 1,
    "per_page": 20,
    "total_pages": 8
  }
}
```

---

#### GET `/entries/{entry_id}`

**Purpose:** Get single entry by ID

**Response (200 OK):**
```json
{
  "entry_id": "uuid",
  "encrypted_content": "base64_encrypted_content",
  "content_hash": "sha256_hex",
  "source": "diary",
  "tags": ["vacation"],
  "media": [
    {
      "media_id": "uuid",
      "encrypted_file_reference": "s3_key",
      "mime_type": "image/jpeg",
      "file_size": 2048576,
      "created_at": "2025-10-31T12:00:00Z"
    }
  ],
  "created_at": "2025-10-31T12:00:00Z",
  "updated_at": "2025-10-31T12:00:00Z"
}
```

**Error Responses:**
- 404 Not Found: Entry doesn't exist or doesn't belong to user

---

#### PUT `/entries/{entry_id}`

**Purpose:** Update existing entry

**Request Body (partial updates allowed):**
```json
{
  "encrypted_content": "new_encrypted_content",
  "content_hash": "new_hash",
  "tags": ["new", "tags"]
}
```

**Response (200 OK):**
```json
{
  "entry_id": "uuid",
  "updated_at": "2025-10-31T13:00:00Z"
}
```

**Behavior:**
- Creates entry_event with type "edited"
- If entry was shared (has external_post), marks as "out_of_sync"

**Error Responses:**
- 404 Not Found
- 400 Bad Request: Invalid input

---

#### DELETE `/entries/{entry_id}`

**Purpose:** Soft delete entry

**Response (204 No Content)**

**Behavior:**
- Sets deleted_at timestamp
- Entry still in database but filtered from queries
- Creates entry_event with type "deleted"

**Error Responses:**
- 404 Not Found

---

### Search Endpoints

#### GET `/search`

**Purpose:** Search entries

**Query Parameters:**
- `q` (string, search query, required)
- `page` (integer, default 1)
- `per_page` (integer, default 20)
- `tags` (comma-separated, optional)
- `start_date` (ISO 8601, optional)
- `end_date` (ISO 8601, optional)

**Response for UCE users (200 OK):**
```json
{
  "entries": [...],  // Same format as GET /entries
  "pagination": {...}
}
```

**Response for E2E users (403 Forbidden):**
```json
{
  "error": "server_side_search_not_available",
  "message": "Server-side search is not available for E2E encryption tier. Please use client-side search.",
  "suggestion": "Download all entries and search locally."
}
```

**Alternative for E2E users:**
Agent can choose to support metadata-only search (tags, dates) for E2E users, returning limited results.

---

### Social Media Integration Endpoints

#### POST `/integrations/facebook/connect`

**Purpose:** Initiate Facebook OAuth flow

**Request Body:**
```json
{
  "redirect_uri": "https://yourapp.com/oauth/callback"
}
```

**Response (200 OK):**
```json
{
  "authorization_url": "https://www.facebook.com/v18.0/dialog/oauth?..."
}
```

**Client flow:**
1. Open authorization_url in webview/browser
2. User approves
3. Facebook redirects to redirect_uri with code
4. Client calls callback endpoint with code

---

#### POST `/integrations/facebook/callback`

**Purpose:** Complete OAuth and exchange code for tokens

**Request Body:**
```json
{
  "code": "oauth_authorization_code",
  "state": "csrf_token"
}
```

**Response (201 Created):**
```json
{
  "integration_id": "uuid",
  "platform": "facebook",
  "status": "active",
  "platform_username": "john.doe"
}
```

**Behavior:**
- Exchange code for access_token and refresh_token
- Encrypt tokens before storing
- Store in integration_accounts table

**Error Responses:**
- 400 Bad Request: Invalid code or state
- 503 Service Unavailable: Facebook API error

---

#### POST `/integrations/facebook/push`

**Purpose:** Push diary entry to Facebook

**Request Body:**
```json
{
  "entry_id": "uuid",
  "plaintext_content": "decrypted diary text",  // Client must decrypt for E2E tier
  "privacy_setting": "public" | "friends" | "only_me",
  "media_urls": ["url1", "url2"]  // Optional, pre-signed URLs for media
}
```

**Response (201 Created):**
```json
{
  "external_post_id": "facebook_post_id",
  "external_url": "https://facebook.com/user/posts/12345",
  "posted_at": "2025-10-31T12:00:00Z"
}
```

**Behavior:**
- Post to Facebook Graph API
- Store external_post mapping
- Create entry_event with type "shared"
- Tag entry with "shared:facebook"

**Error Responses:**
- 400 Bad Request: Invalid input
- 401 Unauthorized: Facebook token expired
- 503 Service Unavailable: Facebook API error

---

#### POST `/integrations/facebook/pull`

**Purpose:** Import posts from Facebook to diary

**Request Body:**
```json
{
  "since": "2023-01-01T00:00:00Z",  // Optional, start date
  "until": "2025-10-31T00:00:00Z",  // Optional, end date
  "limit": 100  // Optional, max posts to import
}
```

**Response (202 Accepted):**
```json
{
  "job_id": "uuid",
  "status": "queued",
  "message": "Import job started. Check /integrations/facebook/pull/status/{job_id} for progress."
}
```

**Behavior:**
- Create background job for async processing
- Job fetches posts from Facebook Graph API
- For each post:
  - Check deduplication (external_post_id → content_hash)
  - If new:
    - E2E tier: Return plaintext to client for encryption
    - UCE tier: Server encrypts and stores
  - Tag with "source:facebook"
  - Create entry_event with type "imported"

---

#### GET `/integrations/facebook/pull/status/{job_id}`

**Purpose:** Check import job status

**Response (200 OK):**
```json
{
  "job_id": "uuid",
  "status": "queued" | "processing" | "completed" | "failed",
  "imported_count": 42,
  "failed_count": 3,
  "total_count": 45,
  "started_at": "2025-10-31T12:00:00Z",
  "completed_at": "2025-10-31T12:05:00Z",  // Null if not completed
  "new_entries": ["entry_id1", "entry_id2"],  // Entry IDs created
  "errors": [  // If any failures
    {
      "facebook_post_id": "12345",
      "error": "Error message"
    }
  ]
}
```

**Error Responses:**
- 404 Not Found: Job doesn't exist

---

### Media Endpoints

#### POST `/media`

**Purpose:** Upload encrypted media file

**Request:**
- Content-Type: multipart/form-data
- Fields:
  - `file` (binary, encrypted file)
  - `file_hash` (string, SHA-256 of encrypted file)
  - `mime_type` (string)

**Response (201 Created):**
```json
{
  "media_id": "uuid",
  "encrypted_file_reference": "s3://bucket/path/to/file",
  "file_size": 2048576,
  "created_at": "2025-10-31T12:00:00Z"
}
```

**Behavior:**
- Validate file size against user's storage quota
- Store encrypted file in blob storage (S3)
- Create media record in database
- Return media_id for attaching to entries

**Error Responses:**
- 400 Bad Request: Invalid file or hash mismatch
- 413 Payload Too Large: File exceeds size limit
- 507 Insufficient Storage: User quota exceeded

---

#### GET `/media/{media_id}`

**Purpose:** Download encrypted media file

**Response (200 OK):**
- Content-Type: application/octet-stream
- Body: Encrypted file bytes

**OR** (if using pre-signed URLs):
```json
{
  "download_url": "https://s3.../presigned_url",
  "expires_at": "2025-10-31T13:00:00Z"
}
```

**Error Responses:**
- 404 Not Found: Media doesn't exist or doesn't belong to user

---

#### DELETE `/media/{media_id}`

**Purpose:** Delete media file

**Response (204 No Content)**

**Behavior:**
- Remove file from blob storage
- Remove media record from database
- If attached to entry, remove attachment

**Error Responses:**
- 404 Not Found

---

### Feature Gate Endpoint

#### GET `/me/features`

**Purpose:** Get list of enabled features for current user

**Response (200 OK):**
```json
{
  "user_id": "uuid",
  "encryption_tier": "e2e" | "uce",
  "features": {
    "server_search": true | false,
    "server_ai": true | false,
    "easy_recovery": true | false,
    "auto_multi_device_sync": true | false,
    "user_sharing": true | false
  },
  "storage": {
    "used_bytes": 512000000,
    "limit_bytes": 1073741824,
    "percentage_used": 47.68
  },
  "tier_info": {
    "name": "Free" | "Paid",
    "price": "$0/mo" | "$8/mo"
  }
}
```

**Use Case:**
- Client queries this on app launch
- UI conditionally renders features based on capabilities
- Shows storage usage and upgrade prompts

---

## User Flows

### User Signup Flow

**Step 1: Email & Password Entry**
- Client presents signup form
- User enters email and password
- Client validates:
  - Email format
  - Password strength (min 12 chars, mixed case, numbers, symbols)

**Step 2: Encryption Tier Selection**

Client displays visual comparison:

```
┌─────────────────────────────────┐  ┌─────────────────────────────────┐
│  🔒 Maximum Privacy (E2E)       │  │  🚀 Smart Features (UCE)        │
│                                 │  │                                 │
│  ✅ Keys never on server        │  │  ✅ Full-text search            │
│  ✅ Maximum security            │  │  ✅ AI auto-tagging             │
│  ✅ True end-to-end encryption  │  │  ✅ Easy account recovery       │
│                                 │  │  ✅ Instant multi-device sync   │
│  ❌ Limited search              │  │  ✅ Share with friends          │
│  ❌ No AI features              │  │                                 │
│  ❌ Manual device setup         │  │  Still encrypted at rest!       │
│  ❌ Recovery codes only         │  │                                 │
│                                 │  │                                 │
│        [Choose E2E]             │  │        [Choose UCE]             │
└─────────────────────────────────┘  └─────────────────────────────────┘

⚠️ This choice is permanent and cannot be changed later.
💡 Most users choose UCE for convenience.
```

**Step 3a: E2E Setup**

If user selects E2E:

1. Client generates keypair locally (X25519 or similar)
2. Client stores private key in secure storage (Keychain/KeyStore)
3. Client generates 10 recovery codes
4. Client displays recovery codes:
   ```
   ⚠️ SAVE THESE CODES NOW
   
   XXXX-XXXX-XXXX-XXXX    XXXX-XXXX-XXXX-XXXX
   XXXX-XXXX-XXXX-XXXX    XXXX-XXXX-XXXX-XXXX
   XXXX-XXXX-XXXX-XXXX    XXXX-XXXX-XXXX-XXXX
   XXXX-XXXX-XXXX-XXXX    XXXX-XXXX-XXXX-XXXX
   XXXX-XXXX-XXXX-XXXX    XXXX-XXXX-XXXX-XXXX
   
   [Download as File]  [Copy to Clipboard]
   
   □ I have saved my recovery codes
   
   [Continue] (disabled until checkbox checked)
   ```

5. User MUST check confirmation box
6. Client sends to server:
   - email, password, encryption_tier="e2e", public_key
7. Server creates account, returns JWT + recovery_codes
8. Client verifies recovery codes match

**Step 3b: UCE Setup**

If user selects UCE:

1. Client sends to server:
   - email, password, encryption_tier="uce"
2. Server:
   - Generates master encryption key
   - Derives key from password (Argon2id)
   - Encrypts master key with derived key
   - Stores encrypted_master_key + salt
3. Server returns JWT
4. User proceeds immediately (no recovery codes needed)

**Step 4: Complete Profile**
- Optional: Display name, avatar, timezone
- Skip allowed

**Step 5: Onboarding Tutorial**
- Show how to create first entry
- Show how to connect Facebook
- Tier-specific tips (E2E: save codes, UCE: easy recovery)

---

### Entry Creation Flow

**Step 1: User Opens "New Entry" Screen**

**Step 2: User Writes Content**
- Text input
- Optional: Add photos/videos
- Optional: Add location
- Optional: Select mood

**Step 3: Client Encrypts Content**

For E2E tier:
- Client retrieves private key from secure storage
- Client encrypts content with public key
- Client encrypts each media file
- Client generates content_hash (SHA-256 of plaintext)

For UCE tier:
- Client can encrypt locally (recommended) OR
- Client sends plaintext, server encrypts (simpler)

**Step 4: Client Uploads**

If media attached:
- Client uploads encrypted media first (POST /media)
- Gets media_ids back

Then client creates entry:
- POST /entries with encrypted_content, content_hash, media_ids

**Step 5: Server Stores**
- Validates encrypted format
- Stores entry
- Creates entry_event (type: "created")
- Returns entry_id

**Step 6: Client Updates UI**
- Shows new entry in timeline
- Encrypted on device, encrypted on server

---

### Facebook Push Flow

**Step 1: User Selects Entry**
- User taps entry, taps "Share to Facebook"

**Step 2: User Previews & Edits**
- Client decrypts entry (E2E) or requests decrypted (UCE)
- Shows plaintext preview
- User can edit before sharing
- User selects privacy setting (public/friends/only_me)

**Step 3: Client Sends to API**
- POST /integrations/facebook/push
- Body: entry_id, plaintext_content, privacy_setting

**Step 4: Server Posts to Facebook**
- Server calls Facebook Graph API
- Uploads media if included
- Creates post

**Step 5: Server Stores Mapping**
- Creates external_post record
- Links entry_id → facebook_post_id
- Creates entry_event (type: "shared")
- Tags entry with "shared:facebook"

**Step 6: Client Shows Success**
- Display external_url
- Show "Shared to Facebook" badge on entry
- Update UI

---

### Facebook Pull Flow

**Step 1: User Initiates Import**
- User taps "Import from Facebook"
- Optional: Select date range

**Step 2: Client Calls API**
- POST /integrations/facebook/pull
- Server returns job_id

**Step 3: Client Polls for Status**
- GET /integrations/facebook/pull/status/{job_id}
- Shows progress indicator

**Step 4: Server Processes (Background Job)**

For each Facebook post:
```
1. Check if external_post_id exists in database
   → Yes: Skip (already imported)
   → No: Continue

2. Calculate content_hash of Facebook post
3. Check if content_hash exists in database
   → Yes: Skip (duplicate content)
   → No: Continue

4. New post detected:
   If UCE tier:
     - Server encrypts content
     - Server stores entry
     - Tags with "source:facebook"
     - Creates external_post mapping
   
   If E2E tier:
     - Server queues plaintext for client
     - Client retrieves plaintext
     - Client encrypts
     - Client uploads encrypted entry
     - Tags with "source:facebook"
     - Creates external_post mapping
```

**Step 5: Client Receives New Entries**
- Job completes
- Client fetches new entries
- Updates timeline
- Shows notification: "Imported X posts from Facebook"

---

## Search Implementation

### For UCE Tier (Server-Side Search)

**Requirements:**
- Full-text search on decrypted content
- Support keyword matching, phrase search, tag filtering, date filtering
- Performance: < 200ms (95th percentile)

**Implementation Approach:**

Agent chooses one:

**Option A: PostgreSQL Full-Text Search (Simple)**
- Use PostgreSQL's built-in full-text search
- Pros: No additional services, simple to set up
- Cons: Limited features, less scalable
- Good for MVP

**Option B: Elasticsearch/OpenSearch (Advanced)**
- Dedicated search engine
- Pros: Better features, more scalable, relevance ranking
- Cons: Additional service to manage
- Good for post-MVP

**Security:**
- Search index must be encrypted at rest
- Decryption happens during indexing, not during query
- Access control per user

**Indexing Process (UCE):**
1. When entry created/updated
2. Server decrypts content using user's master key
3. Server indexes decrypted content in search engine
4. Search engine stores encrypted
5. When user searches, server decrypts results before returning

---

### For E2E Tier (Client-Side Search)

**Requirements:**
- All entries downloaded to client
- Client performs search locally
- No server-side indexing
- Support keyword matching, tag filtering, date filtering

**Implementation Approach:**

Agent chooses per platform:

**iOS:**
- Core Data with NSPredicate
- Or SQLite with FTS5
- Or in-memory search for small datasets

**Android:**
- Room with FTS
- Or SQLite with FTS4/FTS5
- Or in-memory search

**Web:**
- IndexedDB with JavaScript search
- Or in-memory search with Fuse.js
- Or Web Workers for background search

**Trade-offs:**
- Slower for large datasets (10,000+ entries)
- No cross-device search history
- Works offline (benefit!)

**Client Implementation:**
```
1. On app launch, sync all entries
2. Decrypt all entries locally
3. Store decrypted in local database (ephemeral, secure storage)
4. When user searches:
   - Query local database
   - Results instant
5. On app close, clear local decrypted cache
```

---

## Social Media Integration

### Facebook Integration

**OAuth Scopes Required:**
- `user_posts` - Read user's posts
- `publish_actions` or `pages_manage_posts` - Post to timeline
- `user_photos` - Access photos

**Graph API Endpoints Used:**
- `/me/posts` - Fetch user's posts
- `/me/photos` - Fetch user's photos
- `/me/feed` - Post to user's timeline

**Error Handling:**

Agent must handle:
- Rate limits (exponential backoff)
- Token expiration (prompt re-auth)
- Network errors (retry with backoff)
- Post deleted (mark failed, don't retry)
- API version changes (graceful degradation)

**Deduplication Strategy:**

```
Priority:
1. Match external_post_id → Skip (already imported)
2. Match content_hash → Skip (duplicate)
3. No match → Import as new entry
```

**Media Handling:**
- Download media from Facebook
- Encrypt media
- Upload to blob storage
- Link to entry

---

## Multi-Device Sync

### For UCE Tier (Automatic)

**Requirements:**
- Real-time sync when online
- Offline support with eventual consistency
- Conflict resolution

**Implementation Approach:**

Agent chooses:

**Option A: Polling (Simple)**
- Client polls /entries with `since` timestamp
- Server returns entries modified since last sync
- Simple, works everywhere
- Less real-time

**Option B: WebSockets (Real-time)**
- Client opens WebSocket connection
- Server pushes new entries immediately
- More complex, better UX
- Requires WebSocket support

**Sync Protocol:**
```
1. Client tracks last_synced_at timestamp
2. On app open or periodic interval:
   - Client: GET /entries?since={last_synced_at}
   - Server: Returns new/modified entries
   - Client: Updates local database
   - Client: Updates last_synced_at
3. On local create/update:
   - Client: POST/PUT entry
   - Server: Stores entry
   - Client: Updates local entry with server response
```

**Conflict Resolution:**
- Last write wins (with warning to user)
- Server's updated_at timestamp is source of truth
- Client shows "Conflict detected" if both devices edited same entry

---

### For E2E Tier (Manual Device Pairing)

**Option A: QR Code Transfer (Recommended)**

```
Primary Device:
1. User goes to Settings → Add Device
2. App prompts for password
3. App retrieves private key from secure storage
4. App encrypts private key with ephemeral key
5. App generates QR code containing:
   - User email
   - Encrypted private key
   - Ephemeral key (encrypted with server)
6. Display QR code on screen

Secondary Device:
1. User signs in with email + password
2. User taps "Pair with existing device"
3. App opens camera
4. User scans QR code from primary device
5. App sends ephemeral key to server for validation
6. Server confirms ownership
7. App decrypts private key
8. App stores private key in secure storage
9. Sync begins
```

**Option B: Recovery Code Entry (Simpler)**

```
Secondary Device:
1. User signs in with email + password
2. User taps "I have a recovery code"
3. User enters recovery code
4. Server validates code (checks hash)
5. Server returns encrypted private key
6. App decrypts private key using recovery code
7. App stores private key in secure storage
8. Sync begins
```

**Agent implements:** At minimum, Option B. Option A is nice-to-have.

---

## Security Requirements

### General Security

**Agent must implement:**

1. **Data Encryption at Rest**
   - All entry content encrypted in database
   - OAuth tokens encrypted in database
   - Master keys encrypted (UCE) or not stored (E2E)
   - Database-level encryption if possible

2. **Data Encryption in Transit**
   - HTTPS/TLS 1.2+ required for all API calls
   - Certificate pinning in mobile apps (recommended)
   - No mixed content

3. **Authentication**
   - JWT tokens with short expiration (15 minutes)
   - Refresh tokens with longer expiration (30 days)
   - Token rotation on refresh
   - Secure token storage on clients

4. **Password Security**
   - Bcrypt or Argon2 for password hashing
   - Minimum 12 characters, complexity requirements
   - Check against breach databases (haveibeenpwned API)
   - Rate limiting on login attempts

5. **Input Validation**
   - Validate all inputs server-side
   - Sanitize outputs to prevent XSS
   - Parameterized queries to prevent SQL injection
   - File upload validation (size, type, content)

6. **Rate Limiting**
   - Auth endpoints: 5 attempts per minute per IP
   - API endpoints: 100 requests per minute per user
   - Media upload: 10 files per minute per user

7. **Logging**
   - Never log passwords, keys, tokens, or plaintext content
   - Log authentication events
   - Log API errors (sanitized)
   - Log security events (failed logins, etc.)

8. **CORS**
   - Configure allowed origins
   - No wildcard (*) in production

9. **Security Headers**
   - CSP (Content Security Policy)
   - X-Frame-Options: DENY
   - X-Content-Type-Options: nosniff
   - Strict-Transport-Security

---

### Platform-Specific Security

**iOS:**
- Store private keys in Keychain with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`
- Enable biometric authentication (Face ID/Touch ID) for app access
- Certificate pinning for API requests
- Enable data protection (FileProtectionType.complete)

**Android:**
- Store private keys in Android KeyStore
- Enable biometric authentication (BiometricPrompt)
- Certificate pinning for API requests
- Encrypted SharedPreferences for sensitive data

**Web:**
- Store sensitive data in IndexedDB (encrypted)
- Use Web Crypto API for encryption
- Implement CSP headers
- SameSite cookies
- HttpOnly, Secure flags on cookies

---

## Testing Requirements

### Unit Testing

**Agent must create unit tests for:**

1. **Encryption Services**
   - E2E encryption/decryption
   - UCE key derivation
   - Key generation
   - Format validation

2. **API Handlers**
   - Request validation
   - Response formatting
   - Error handling
   - Authentication checks

3. **Business Logic**
   - Entry deduplication
   - Content hash generation
   - Feature gate logic
   - Storage quota enforcement

**Coverage Target:** Minimum 80%

**Testing Frameworks:** Agent chooses (pytest, Jest, Go test, etc.)

---

### Integration Testing

**Agent must create integration tests for:**

1. **User Flows**
   - Complete signup flow (E2E and UCE)
   - Login and token refresh
   - Entry creation and retrieval
   - Media upload and download

2. **Social Media Integration**
   - OAuth flow
   - Facebook push
   - Facebook pull (mocked Facebook API)
   - Deduplication logic

3. **Multi-Device Sync**
   - Entry sync across devices
   - Conflict resolution
   - Offline changes sync

4. **Search**
   - UCE server-side search
   - E2E client-side search

**Approach:**
- Use test database
- Mock external APIs (Facebook)
- Test API contracts

---

### Security Testing

**Agent must verify:**

1. **E2E Isolation**
   - Server cannot decrypt E2E entries
   - Attempt to decrypt E2E entry fails on server
   - Private keys never transmitted to server

2. **UCE Encryption**
   - Master keys properly encrypted at rest
   - Password hashing follows best practices
   - Key derivation parameters correct

3. **Authentication**
   - Invalid JWT rejected
   - Expired JWT rejected
   - Token refresh works correctly
   - Logout invalidates tokens

4. **Input Validation**
   - SQL injection attempts fail
   - XSS attempts sanitized
   - Oversized uploads rejected
   - Invalid file types rejected

5. **Rate Limiting**
   - Login rate limits enforced
   - API rate limits enforced

**Tools:**
- OWASP ZAP for vulnerability scanning
- Automated security testing in CI/CD

---

### End-to-End Testing

**Agent must create E2E tests for:**

1. **Complete User Journey**
   - Signup → Create Entry → Share to Facebook → View on Facebook
   - Import from Facebook → Entry appears in diary

2. **Cross-Platform**
   - Create entry on iOS → Appears on Android
   - Create entry on Web → Appears on iOS

3. **Error Recovery**
   - Network failure during upload → Retry succeeds
   - Token expiration → Refresh and retry succeeds

**Tools:**
- Appium for mobile E2E testing
- Selenium/Cypress for web E2E testing

---

## Deployment Strategy

### MVP Deployment: Docker + Docker Compose

**Agent must create:**

1. **Dockerfiles**
   - One per service (API, Worker, etc.)
   - Multi-stage builds for optimization
   - Security best practices (non-root user, minimal base image)

2. **docker-compose.yml**
   - All services defined
   - Database service (PostgreSQL)
   - Cache service (Redis)
   - Blob storage (MinIO for local dev, S3 for production)
   - Environment variables
   - Volume mounts

3. **Environment Configuration**
   - `.env.example` file
   - Environment-specific configs (dev, staging, prod)
   - Secrets management (not in git)

4. **Database Migrations**
   - Migration tool (Alembic, Flyway, etc.)
   - Initial schema
   - Migration versioning

**Deployment Steps:**
```bash
# Clone repo
git clone ...

# Configure environment
cp .env.example .env
# Edit .env with production values

# Start services
docker-compose up -d

# Run migrations
docker-compose exec api ./run-migrations.sh

# Verify
curl https://api.yourdomain.com/health
```

---

### Infrastructure Considerations

**Agent should document:**

1. **Hosting Options**
   - Single VPS (DigitalOcean, Linode, Vultr)
   - Cloud VM (AWS EC2, GCP Compute Engine, Azure VM)
   - Managed platform (Railway, Render, Fly.io)

2. **Database**
   - Managed PostgreSQL (AWS RDS, DigitalOcean Managed DB)
   - Or self-hosted in Docker

3. **Blob Storage**
   - AWS S3 (recommended)
   - Google Cloud Storage
   - Azure Blob Storage
   - DigitalOcean Spaces
   - Wasabi (S3-compatible, cheaper)

4. **SSL Certificate**
   - Let's Encrypt (free)
   - Or managed by hosting platform

5. **Monitoring**
   - Application logs
   - Error tracking (Sentry)
   - Uptime monitoring (UptimeRobot)
   - Performance monitoring (optional)

---

### Post-MVP: Kubernetes Migration Path

**Not implemented now, but agent should:**

1. **Keep K8s in Mind**
   - Services already containerized
   - Stateless design (except database)
   - Configuration via environment variables

2. **Future Migration**
   - Create Kubernetes manifests
   - Helm charts for deployment
   - Horizontal pod autoscaling
   - Load balancing

**When to migrate:**
- Thousands of active users
- High traffic volumes
- Need auto-scaling
- Geographic distribution

---

## Implementation Checklist

Agent should follow this phased approach:

### Phase 1: Foundation (Weeks 1-2)

**Backend:**
- [ ] Set up project structure
- [ ] Implement authentication system (JWT)
- [ ] Create user model with encryption_tier field
- [ ] Build signup endpoint with tier selection
- [ ] Implement E2E public key storage
- [ ] Implement UCE key derivation (Argon2id)
- [ ] Create database schema
- [ ] Set up Docker + docker-compose

**Mobile (start with one platform):**
- [ ] Project setup
- [ ] Authentication screens (signup/login)
- [ ] Encryption tier selection UI
- [ ] Client-side key generation (E2E)
- [ ] Secure key storage (Keychain/KeyStore)
- [ ] API client

---

### Phase 2: Core Features (Weeks 3-4)

**Backend:**
- [ ] Implement entry CRUD endpoints
- [ ] Build encryption services (E2E and UCE)
- [ ] Create feature gate system
- [ ] Implement tag system
- [ ] Build media upload/download
- [ ] Create entry event/audit system
- [ ] Set up blob storage (S3)

**Mobile:**
- [ ] Entry creation screen
- [ ] Client-side encryption implementation
- [ ] Entry list/timeline
- [ ] Entry detail view
- [ ] Media capture and upload
- [ ] Local database (SQLite/Room/Core Data)

---

### Phase 3: Social Integration (Weeks 5-6)

**Backend:**
- [ ] Facebook OAuth flow
- [ ] Facebook push implementation
- [ ] Facebook pull implementation
- [ ] Deduplication logic
- [ ] Background job system (Celery/Bull)
- [ ] Error handling and retry logic

**Mobile:**
- [ ] Facebook integration screens
- [ ] OAuth webview/browser flow
- [ ] Share to Facebook UI
- [ ] Import from Facebook UI
- [ ] External post indicators

---

### Phase 4: Search & Sync (Weeks 7-8)

**Backend:**
- [ ] UCE server-side search
- [ ] E2E metadata search (limited)
- [ ] Sync endpoints (/entries with pagination)
- [ ] WebSocket support (optional)

**Mobile:**
- [ ] Search UI
- [ ] E2E client-side search
- [ ] Multi-device sync
- [ ] Conflict resolution
- [ ] Offline mode

---

### Phase 5: Second Platform & Polish (Weeks 9-10)

**Second Mobile Platform:**
- [ ] Repeat Phase 1-4 for second platform (iOS or Android)
- [ ] Ensure API compatibility
- [ ] Cross-platform testing

**Web App (Optional):**
- [ ] Web app setup
- [ ] Authentication screens
- [ ] Entry creation/viewing
- [ ] PWA manifest
- [ ] Service Worker for offline

**Polish:**
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests
- [ ] Security tests
- [ ] E2E tests
- [ ] Performance optimization
- [ ] Documentation
- [ ] Deployment guide

---

## Success Criteria

**Agent should achieve:**

### Technical Requirements
- [ ] API response time < 200ms (95th percentile)
- [ ] Zero data breaches (all data encrypted at rest)
- [ ] 99.9% uptime for API
- [ ] E2E entries unreadable by server (verified)
- [ ] UCE entries encrypted at rest (verified)
- [ ] Facebook sync success rate > 95%

### User Experience Requirements
- [ ] Clear encryption tier choice at signup
- [ ] Seamless entry creation across platforms
- [ ] Working push to Facebook
- [ ] Working pull from Facebook
- [ ] Functional search (tier-appropriate)
- [ ] Reliable multi-device sync

### Security Requirements
- [ ] E2E entries cannot be decrypted by server
- [ ] UCE entries properly encrypted at rest
- [ ] No sensitive data in logs
- [ ] Proper token management
- [ ] Input validation on all endpoints
- [ ] Rate limiting prevents abuse

### Code Quality
- [ ] 80%+ unit test coverage
- [ ] Integration tests passing
- [ ] Security tests passing
- [ ] E2E tests passing
- [ ] Documentation complete
- [ ] Deployment automated

---

## Questions for Agent to Answer Before Starting

Before implementation, agent should determine:

1. **Backend Language?**
   - Python (FastAPI, Flask, Django)
   - Node.js (Express, NestJS)
   - Go (Gin, Echo)
   - Other

2. **Database?**
   - PostgreSQL (recommended)
   - MySQL
   - Other

3. **Blob Storage?**
   - AWS S3 (recommended)
   - Google Cloud Storage
   - Azure Blob Storage
   - MinIO (self-hosted)

4. **Mobile Framework?**
   - Native (Swift + Kotlin) - Best UX
   - React Native - Code reuse
   - Flutter - Code reuse
   - Other

5. **Web Framework?**
   - React (most popular)
   - Vue (simpler)
   - Svelte (modern)
   - Other

6. **Task Queue?**
   - Celery (Python)
   - Bull (Node.js)
   - Other

7. **Search Engine?**
   - PostgreSQL FTS (simple, MVP)
   - Elasticsearch (advanced, scalable)

8. **Real-time Sync?**
   - Polling (simple)
   - WebSockets (real-time)

**Agent documents choices in README.md before starting.**

---

## Final Notes for AI Agent

**Freedom:**
- Agent has full freedom to choose technologies
- Agent designs database schema
- Agent writes all code
- Agent chooses testing frameworks
- Agent decides implementation patterns

**Constraints:**
- Must follow API contracts defined here
- Must respect security requirements
- Must implement both encryption tiers
- Must be mobile-first
- Must follow MVP deployment strategy (Docker Compose)

**Communication:**
- Ask clarifying questions when specifications ambiguous
- Propose alternatives if better approach exists
- Document all technical decisions
- Explain trade-offs when making choices

**Quality:**
- Write clean, maintainable code
- Include comprehensive error handling
- Add detailed comments/docstrings
- Create thorough tests
- Document setup and deployment

---

**End of AI Agent Implementation Guide**

This specification provides complete guidance for implementing the dual-tier encryption diary platform. Agent should follow these specifications while making appropriate technical implementation decisions.

Good luck building! 🚀
