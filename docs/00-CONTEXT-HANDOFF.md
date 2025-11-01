# Context Handoff: Dual-Tier Encryption Diary Platform

**Purpose**: Use this document to continue the conversation in a new Claude chat if context runs out.

---

## 🎯 Project Summary

Building a **privacy-first diary platform** with a unique dual-tier encryption approach:

### Key Innovation
Users choose their encryption model at signup (permanent choice):
- **E2E (End-to-End Encrypted)**: Maximum privacy, keys never on server, limited features
- **UCE (User-Controlled Encryption)**: Full features, encrypted at rest, server can decrypt with password

### Platform Priority
**Mobile-first**: iOS + Android native apps, plus responsive web app (PWA)

### Core Features
- Private diary entries (text + photos)
- Push entries to social media (starting with Facebook)
- Pull posts from social media back into diary
- End-to-end or user-controlled encryption
- Diary is the "source of truth" for all content

---

## 📋 Current Status

### Completed Documents

**1. Main Project Specification** (`01-project-specification.md`)
- Complete system architecture
- Technology stack decisions
- Data model concepts
- Facebook integration flows
- Open questions and decisions log

**2. AI Agent Implementation Guide** (`02-ai-agent-implementation-guide.md`)
- Comprehensive specifications for AI agent
- No code, just requirements and architecture
- Mobile-first approach detailed
- API contracts, data models, security requirements
- Testing strategy and success criteria

### Key Decisions Made

1. ✅ Dual-tier encryption (E2E + UCE) from day 1
2. ✅ Mobile-first (iOS, Android, Web)
3. ✅ No timeline pressure, 99% AI agent implementation
4. ✅ Encryption tier selected at signup, permanent
5. ✅ 4-tier pricing: E2E Free/Paid, UCE Free/Paid
6. ✅ Same price for both encryption tiers
7. ✅ Strategy pattern for encryption services
8. ✅ Feature gate system for tier-specific capabilities
9. ✅ **MVP Deployment**: Docker Compose (NOT Kubernetes initially)

---

## 🎨 Architecture Overview

```
Clients (iOS, Android, Web)
    ↓ HTTPS REST API
API Gateway (Auth, Rate Limiting)
    ↓
Encryption Tier Router
    ↓
┌──────────────┬──────────────┐
│  E2E Service │  UCE Service │
└──────────────┴──────────────┘
    ↓
Core Business Logic
    ↓
Database + Blob Storage
```

### Deployment Strategy
- **MVP**: Docker + Docker Compose (simple, manageable)
- **Post-MVP**: Migrate to Kubernetes (when scale demands it)
- Microservices architecture (ready for K8s when needed)

### Encryption Strategy Pattern
- Abstract base class for encryption services
- E2E implementation (client-side keys)
- UCE implementation (server-side key derivation)
- Factory routes to appropriate service based on user tier

### Feature Matrix
| Feature | E2E | UCE |
|---------|-----|-----|
| Encrypted entries | ✅ | ✅ |
| Social media integration | ✅ | ✅ |
| Server-side search | ❌ | ✅ |
| Server-side AI | ❌ | ✅ |
| Easy recovery | ❌ | ✅ |
| Multi-device sync | Manual | Auto |

---

## 🔐 Encryption Details

### E2E Tier
- Public key cryptography (X25519 or similar)
- Private keys stored in platform secure storage (Keychain/KeyStore/IndexedDB)
- Server stores only public key
- Recovery via recovery codes (10 codes, user must save)
- Client-side encryption/decryption
- No server-side decryption capability

### UCE Tier
- Password-based key derivation (Argon2id or PBKDF2)
- Master key encrypted with derived key
- Server stores encrypted master key
- Any device with password can decrypt
- Server can decrypt for search, AI, etc.
- Easy password reset via email

---

## 📱 User Flows

### Signup Flow
```
1. Email + Password
2. Choose Encryption Tier
   [Visual comparison card: E2E vs UCE]
   - Show capabilities side-by-side
   - Warn about permanence
   - Default: UCE for most users
3a. E2E Setup
   - Generate keypair on device
   - Send public key to server
   - Generate & save 10 recovery codes
3b. UCE Setup
   - Server generates master key
   - Encrypt with password-derived key
   - Store encrypted key
4. Complete profile
5. Onboarding tutorial
```

### Social Media Integration

**Push (Diary → Facebook)**:
- User selects entry
- Client decrypts (E2E) or server fetches (UCE)
- Post to Facebook Graph API
- Store external_post mapping
- Tag entry "shared:facebook"

**Pull (Facebook → Diary)**:
- Fetch posts via Graph API
- Deduplicate (external_post_id → content_hash → new)
- E2E: Return plaintext to client for encryption
- UCE: Server encrypts and stores
- Auto-tag "source:facebook"

---

## 🗄️ Data Model Essentials

### User
- ID, email, password_hash, **encryption_tier** (enum)
- Tier-specific: public_key (E2E) or encrypted_master_key (UCE)
- Relationships: entries, integrations, media

### Entry
- ID, user_id, **encrypted_content**, **content_hash** (SHA-256)
- Source (diary, facebook, instagram, etc.)
- Relationships: tags, media, external_posts, events

### External_Post
- Entry_id, platform, external_post_id, external_url
- Sync status
- Purpose: Track social media mappings

### Integration_Account
- User_id, platform, encrypted_tokens
- OAuth tokens for Facebook, Instagram, etc.

### Entry_Event
- Audit trail (created, edited, shared, imported)
- Enables version history

---

## 🔌 Key API Endpoints

**Authentication**:
- POST `/api/v1/auth/signup` (with encryption_tier choice)
- POST `/api/v1/auth/login`
- POST `/api/v1/auth/refresh`

**Entries**:
- POST `/api/v1/entries` (create with encrypted_content)
- GET `/api/v1/entries` (list, paginated)
- GET `/api/v1/entries/{id}` (single)
- PUT `/api/v1/entries/{id}` (update)
- DELETE `/api/v1/entries/{id}` (soft delete)

**Search**:
- GET `/api/v1/search?q=...`
  - UCE: Server-side full-text search
  - E2E: Return error or metadata-only

**Social Media**:
- POST `/api/v1/integrations/facebook/connect` (OAuth)
- POST `/api/v1/integrations/facebook/push` (diary → Facebook)
- POST `/api/v1/integrations/facebook/pull` (Facebook → diary)
- GET `/api/v1/integrations/facebook/pull/status/{job_id}` (async)

**Media**:
- POST `/api/v1/media` (upload encrypted)
- GET `/api/v1/media/{id}` (download)

---

## 💰 Pricing Strategy

### 4-Tier System
| Tier | Storage | Price | Notes |
|------|---------|-------|-------|
| E2E Free | 1GB | $0 | Privacy enthusiasts |
| E2E Paid | 50GB | $8/mo | Same price, no penalty |
| UCE Free | 1GB | $0 | Most users |
| UCE Paid | 50GB | $8/mo | Full features |

**Philosophy**: Same price for both encryption models. Privacy is not a premium feature.

---

## 🧪 Testing Requirements

- Unit tests: 80%+ coverage
- Integration tests: All API endpoints
- Security tests: E2E isolation, UCE encryption, injection prevention
- E2E tests: Complete user journeys
- Performance: API < 200ms (p95)

---

## 📝 What Still Needs to Be Created

### Additional Specification Documents

1. **Client Application Detailed Specs**
   - iOS app architecture
   - Android app architecture
   - Web app architecture
   - Client-side encryption implementations
   - Local database schemas
   - Offline sync strategies

2. **Facebook Integration Deep Dive**
   - OAuth flow specifics
   - Graph API endpoint details
   - Error handling scenarios
   - Rate limiting strategies
   - Media upload/download flows

3. **Search Implementation Guide**
   - UCE: Server-side search architecture
   - E2E: Client-side search strategies
   - Performance optimization
   - Indexing strategies

4. **Multi-Device Sync Specification**
   - UCE: Automatic sync protocol
   - E2E: Device pairing flows
   - Conflict resolution
   - Offline support

5. **Account Recovery Flows**
   - E2E: Recovery code validation
   - UCE: Password reset via email
   - Security considerations
   - User communication

6. **Storage & Media Management**
   - File encryption approaches
   - Storage limit enforcement
   - Blob storage architecture
   - Media optimization (compression, thumbnails)

7. **Feature Gate System Details**
   - Implementation patterns
   - Feature flag management
   - Tier-based capability checks
   - UI conditional rendering

8. **Deployment & Operations**
   - Docker Compose setup
   - Infrastructure requirements
   - CI/CD pipeline
   - Monitoring & alerting
   - Database migration strategy
   - Secrets management
   - Migration path to Kubernetes (post-MVP)

---

## ❓ Open Questions to Resolve

### High Priority
1. Backend language? (Python/Node.js/Go)
2. Database choice? (PostgreSQL/MySQL)
3. File storage? (S3/Google Cloud/Azure)
4. Mobile framework? (Native vs React Native vs Flutter)
5. Web framework? (React/Vue/Svelte)
6. Authentication? (Custom JWT vs Auth0 vs Firebase)

### Medium Priority
7. Search engine? (PostgreSQL FTS vs Elasticsearch)
8. Real-time sync? (Polling vs WebSockets)
9. Analytics? (Privacy-respecting options)
10. Push notifications? (Firebase vs native)

### Future Considerations
11. AI features? (Auto-tagging, sentiment analysis)
12. User-to-user sharing? (Within platform)
13. Export formats? (JSON, PDF, etc.)
14. Collaboration? (Shared diaries)

---

## 🎯 Next Conversation Starting Points

Depending on what you need next:

**If continuing technical specs:**
> "Let's create detailed specifications for [iOS/Android/Web] client app architecture, including client-side encryption implementation and local storage strategies."

**If ready to implement:**
> "I have the dual-tier encryption spec. Let's start implementation with [specific component]. Here are my choices for: [list technology decisions]."

**If refining architecture:**
> "I want to review the [encryption/search/sync] architecture and discuss trade-offs before implementation."

**If answering open questions:**
> "Here are my technology choices: [language], [database], [storage], etc. Please update the spec accordingly."

---

## 📚 Reference Documents

**In this project**:
1. `01-project-specification.md` - Original comprehensive spec
2. `02-ai-agent-implementation-guide.md` - AI implementation guide

**External**:
- Facebook Graph API: https://developers.facebook.com/docs/graph-api
- OWASP Crypto Storage: https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html

---

## 🤖 Instructions for Claude (in new chat)

When starting a new conversation:

1. Read this handoff document first
2. Review the two specification documents
3. Ask which aspect to continue working on
4. Maintain consistency with decisions already made
5. Reference existing architecture when adding new specs
6. Keep mobile-first approach
7. Respect dual-tier encryption requirement
8. Remember: MVP uses Docker Compose, NOT Kubernetes

---

**Current Date**: October 31, 2025  
**Project Phase**: Architecture & Design Complete, Ready for Detailed Specs  
**Developer Context**: Senior level, 99% AI agent implementation, no timeline pressure

---

**To continue in new chat, say:**

"I'm continuing work on the dual-tier encryption diary platform. I have the context handoff document and the main specifications. I want to [your goal: create detailed specs for X / start implementation / refine architecture / etc.]"
