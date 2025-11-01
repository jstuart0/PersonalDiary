# Personal Diary Platform - Dual-Tier Encryption

A privacy-first diary platform with unique dual-tier encryption architecture, allowing users to choose between maximum privacy (E2E) or convenience with features (UCE).

---

## 🎯 Project Overview

**Status:** Architecture & Design Phase  
**Platform:** Mobile-first (iOS, Android) + Web  
**Development Approach:** 99% AI Agent Implementation  
**Deployment:** Docker Compose (MVP) → Kubernetes (Post-MVP)

### Key Innovation

Users choose their encryption model at signup:
- **E2E (End-to-End Encrypted):** Keys never leave device, maximum privacy
- **UCE (User-Controlled Encryption):** Server can decrypt, enables advanced features

This choice is **permanent** and defines the user's experience.

---

## 📚 Documentation

All specifications are in the `docs/` directory:

### Start Here

1. **[00-CONTEXT-HANDOFF.md](docs/00-CONTEXT-HANDOFF.md)**
   - Quick project summary
   - Use this to continue conversation in new Claude chat
   - Context for picking up where we left off

### Core Specifications

2. **[01-project-specification.md](docs/01-project-specification.md)**
   - Complete project vision and architecture
   - Data model concepts
   - Facebook integration flows
   - Technology stack decisions
   - Open questions and decisions

3. **[02-ai-agent-implementation-guide.md](docs/02-ai-agent-implementation-guide.md)**
   - Comprehensive guide for AI agent implementation
   - API contracts and data model requirements
   - Security requirements
   - Testing strategy
   - Deployment approach

### Platform-Specific Specifications

4. **[03-ios-app-specification.md](docs/03-ios-app-specification.md)**
   - iOS native app architecture
   - Client-side encryption implementation
   - UI/UX specifications
   - Keychain integration
   - Offline support

5. **[04-android-app-specification.md](docs/04-android-app-specification.md)**
   - Android native app architecture
   - Client-side encryption implementation
   - Material Design 3 guidelines
   - KeyStore integration
   - Offline support

6. **[05-web-app-specification.md](docs/05-web-app-specification.md)**
   - Progressive Web App architecture
   - Web Crypto API usage
   - Responsive design
   - PWA capabilities
   - IndexedDB implementation

### Feature Specifications

7. **[06-search-implementation.md](docs/06-search-implementation.md)**
   - UCE: Server-side search architecture
   - E2E: Client-side search strategies
   - Performance optimization
   - Indexing approaches

### Agent Instructions

8. **[00-INITIAL-AGENT-INSTRUCTIONS.md](docs/00-INITIAL-AGENT-INSTRUCTIONS.md)**
   - **START HERE FOR IMPLEMENTATION**
   - Complete autonomous development instructions
   - Sub-agent coordination
   - Progress tracking system
   - Git workflow
   - Wiki management

---

## 🚀 Quick Start for AI Agents

**If you're an AI agent starting implementation:**

1. Read `docs/00-INITIAL-AGENT-INSTRUCTIONS.md` first
2. Set up progress tracking system
3. Create sub-agents for parallel development
4. Follow specifications in order
5. Check code into `jstuart0/PersonalDiary` repository
6. Update wiki with progress

---

## 🏗️ Architecture

```
Mobile Apps (iOS, Android) + Web App
           ↓
    REST API (Backend)
           ↓
  Encryption Tier Router
     ↓            ↓
E2E Service    UCE Service
     ↓            ↓
  Core Business Logic
           ↓
  Database + Blob Storage
```

### Dual-Tier Encryption

| Feature | E2E Tier | UCE Tier |
|---------|----------|----------|
| Server-side search | ❌ | ✅ |
| AI features | ❌ | ✅ |
| Easy recovery | ❌ | ✅ |
| Auto multi-device | ❌ | ✅ |
| Maximum privacy | ✅ | ⚠️ Good |

---

## 🔐 Security

- All data encrypted at rest
- Client-side encryption for E2E tier
- Password-based key derivation for UCE tier
- HTTPS/TLS for all communication
- No sensitive data in logs

---

## 📱 Features

### Core Features (MVP)
- ✅ Create encrypted diary entries
- ✅ Upload encrypted photos/videos
- ✅ Tag entries (user tags + auto-tags)
- ✅ Timeline view
- ✅ Search (tier-dependent)

### Social Media Integration
- ✅ Push entries to Facebook
- ✅ Pull posts from Facebook
- ✅ Deduplication
- ✅ Sync status tracking

### Future Integrations
- Instagram
- Twitter/X
- LinkedIn
- Other platforms

---

## 🎨 Design Principles

1. **Privacy First**: Default to private, opt-in to share
2. **Mobile First**: Designed for mobile, works on desktop
3. **User Choice**: Explicit control over encryption tier
4. **Transparency**: Clear indicators of encryption status
5. **Simplicity**: Complex security made simple

---

## 💰 Pricing

| Tier | Storage | Price |
|------|---------|-------|
| E2E Free | 1GB | $0 |
| E2E Paid | 50GB | $8/mo |
| UCE Free | 1GB | $0 |
| UCE Paid | 50GB | $8/mo |

**Philosophy:** Privacy is not a premium feature. Same price for both tiers.

---

## 📊 Tech Stack (Recommendations)

### Backend
- Language: Python / Node.js / Go (agent chooses)
- Framework: FastAPI / Express / Gin
- Database: PostgreSQL
- Cache: Redis
- Queue: Celery / Bull
- Storage: AWS S3

### Mobile
- iOS: Swift + SwiftUI (native recommended)
- Android: Kotlin + Jetpack Compose (native recommended)
- Alternative: React Native or Flutter for code reuse

### Web
- Framework: React / Vue / Svelte (agent chooses)
- PWA: Service Worker + Manifest
- Storage: IndexedDB
- Crypto: Web Crypto API

### Infrastructure
- Containers: Docker
- Orchestration: Docker Compose (MVP)
- Future: Kubernetes (post-MVP)
- CI/CD: GitHub Actions

---

## 📈 Development Roadmap

### Phase 1: Foundation (Weeks 1-2)
- Backend authentication
- User model with encryption tiers
- Database schema
- Docker setup

### Phase 2: Core Features (Weeks 3-4)
- Entry CRUD
- Encryption services
- Media upload
- Tag system

### Phase 3: Social Integration (Weeks 5-6)
- Facebook OAuth
- Push to Facebook
- Pull from Facebook
- Background jobs

### Phase 4: Client Apps (Weeks 7-8)
- iOS app
- Android app
- Web app
- Client-side encryption

### Phase 5: Polish & Launch (Weeks 9-10)
- Testing
- Documentation
- Deployment
- Launch

---

## 🧪 Testing

- Unit tests: 80%+ coverage
- Integration tests: All API endpoints
- Security tests: Encryption isolation, injection prevention
- E2E tests: Complete user journeys
- Performance: API < 200ms (p95)

---

## 🤝 Contributing

This project is primarily developed by AI agents following specifications.

**For humans:**
- Review specifications in `docs/`
- Provide feedback via issues
- Suggest improvements

**For AI agents:**
- Follow `docs/00-INITIAL-AGENT-INSTRUCTIONS.md`
- Maintain progress tracking
- Update wiki documentation
- Write comprehensive tests
- Document all decisions

---

## 📝 License

TBD

---

## 📧 Contact

Repository: https://github.com/jstuart0/PersonalDiary  
Organization: jstuart0

---

## 🎯 Success Metrics

### User Metrics
- Users can create encrypted entries
- Users can push/pull from Facebook
- Zero data breaches
- Account recovery works reliably

### Technical Metrics
- API response time < 200ms (p95)
- Facebook sync success rate > 95%
- 99.9% uptime

### Business Metrics
- Free-to-paid conversion: 5-10%
- User retention: D7, D30
- Feature usage tracking

---

**This is a living project. Documentation and code evolve as implementation progresses.**
