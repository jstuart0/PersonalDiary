# Personal Diary Platform

A privacy-first diary platform with dual-tier encryption, allowing users to choose between maximum privacy (E2E) or smart features (UCE) while maintaining their digital journal as the source of truth for all social media content.

## 🎯 Project Overview

### Key Innovation: Dual-Tier Encryption
Users choose their encryption model at signup (permanent choice):
- **E2E (End-to-End Encrypted)**: Maximum privacy, keys never on server, limited features
- **UCE (User-Controlled Encryption)**: Full features, encrypted at rest, server can decrypt with password

### Platforms
- **Backend**: Python + FastAPI + PostgreSQL
- **iOS**: Native Swift + SwiftUI
- **Android**: Native Kotlin + Jetpack Compose
- **Web**: React + TypeScript PWA

### Core Features
- Private diary entries (text + photos)
- Push entries to social media (starting with Facebook)
- Pull posts from social media back into diary
- End-to-end or user-controlled encryption
- Diary as "source of truth" for all content

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Client Applications                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ iOS App  │  │Android   │  │ Web App  │             │
│  │ (Swift)  │  │ (Kotlin) │  │ (React)  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
└───────────────────────┬──────────────────────────────────┘
                        │ HTTPS REST API
                        │
┌───────────────────────▼──────────────────────────────────┐
│              Encryption Tier Router                       │
│        ┌──────────────┬──────────────┐                  │
│        │  E2E Service │  UCE Service │                  │
│        └──────────────┴──────────────┘                  │
└───────────────────────┬──────────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────────┐
│  Backend (Python + FastAPI)                              │
│  - Authentication & Authorization                        │
│  - Entry Management                                       │
│  - Social Media Integration (Facebook)                   │
│  - Search (PostgreSQL FTS → Elasticsearch)               │
│  - Background Jobs (Celery + Redis)                      │
└───────────────────────┬──────────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────────┐
│  Infrastructure (Kubernetes)                             │
│  - PostgreSQL Database                                   │
│  - Redis Cache & Queue                                   │
│  - AWS S3 Storage                                        │
└─────────────────────────────────────────────────────────┘
```

## 🔐 Security Architecture

### E2E Tier (Maximum Privacy)
- Public key cryptography (X25519 + ChaCha20-Poly1305)
- Private keys stored in platform secure storage only
- Server stores public key only
- Recovery via 10 recovery codes
- Client-side encryption/decryption
- No server-side search or AI features

### UCE Tier (Smart Features)
- Password-based key derivation (Argon2id)
- Master key encrypted with derived key
- Server can decrypt for search/AI features
- Easy account recovery via email
- Automatic multi-device sync
- Full-text search and AI capabilities

## 🚀 Technology Stack

### Backend
- **Language**: Python 3.11+
- **Framework**: FastAPI
- **Database**: PostgreSQL 15+
- **Cache/Queue**: Redis + Celery
- **Storage**: AWS S3
- **Encryption**: cryptography library
- **Search**: PostgreSQL FTS (MVP) → Elasticsearch (scale)

### iOS
- **Language**: Swift 5.9+
- **UI**: SwiftUI
- **Database**: SwiftData/Core Data
- **Encryption**: CryptoKit
- **Target**: iOS 16+ (Universal)

### Android
- **Language**: Kotlin 1.9+
- **UI**: Jetpack Compose
- **Database**: Room
- **Encryption**: Google Tink
- **Target**: Android 9+ (API 28+)

### Web
- **Framework**: React + TypeScript
- **Build**: Vite
- **Database**: IndexedDB
- **Encryption**: Web Crypto API
- **PWA**: Service Worker + Manifest

### Infrastructure
- **Deployment**: Kubernetes (cluster: thor)
- **Namespace**: personal-diary
- **CI/CD**: GitHub Actions
- **Monitoring**: Native Kubernetes tools

## 📁 Repository Structure

```
PersonalDiary/
├── backend/           # Python FastAPI backend
├── ios/              # iOS Swift app
├── android/          # Android Kotlin app
├── web/              # React TypeScript PWA
├── kubernetes/       # K8s manifests and configs
├── scripts/          # Automation and deployment scripts
├── tests/            # Integration and E2E tests
├── docs/             # Project specifications
├── PROGRESS.md       # Development progress tracking
├── SUB_AGENTS.md     # Sub-agent coordination
├── DECISIONS.md      # Technical decisions log
└── README.md         # This file
```

## 🔄 Development Status

**Current Phase**: Foundation Setup
**Completion**: 25%

### ✅ Completed
- Project specifications complete
- Technology stack decisions finalized
- Repository structure initialized
- Progress tracking system established

### 🔄 In Progress
- Kubernetes cluster setup
- Sub-agent deployment for parallel development

### 📋 Next Up
- Backend API implementation
- iOS app development
- Android app development
- Web PWA development

See [PROGRESS.md](PROGRESS.md) for detailed development status.

## 🎯 Feature Matrix

| Feature | E2E Tier | UCE Tier |
|---------|----------|----------|
| Encrypted entries | ✅ | ✅ |
| Social media push | ✅ | ✅ |
| Social media pull | ✅ | ✅ |
| Server-side search | ❌ | ✅ |
| Server-side AI | ❌ | ✅ |
| Easy recovery | ❌ | ✅ |
| Multi-device sync | Manual | Auto |

## 💰 Pricing Strategy

Same price for both encryption tiers - privacy is not a premium feature.

| Tier | Storage | Price | Target User |
|------|---------|-------|-------------|
| E2E Free | 1GB | $0 | Privacy enthusiasts |
| E2E Paid | 50GB | $8/mo | Privacy power users |
| UCE Free | 1GB | $0 | Casual journalers |
| UCE Paid | 50GB | $8/mo | Power users |

## 🧪 Testing Strategy

- **Unit Tests**: 80%+ coverage per component
- **Integration Tests**: API endpoints, encryption, sync
- **E2E Tests**: Complete user journeys
- **Security Tests**: Encryption isolation, input validation
- **Performance Tests**: API < 200ms, mobile responsiveness

## 📚 Documentation

- [Project Specifications](docs/) - Complete technical specifications
- [API Documentation](backend/docs/) - REST API reference
- [Mobile Setup Guides](docs/) - iOS and Android development setup
- [Deployment Guide](kubernetes/) - Kubernetes deployment instructions

## 🤝 Development Approach

This project uses **autonomous AI agent development** with:
- **Lead Agent**: Overall coordination and architecture
- **Backend Agent**: Python FastAPI implementation
- **iOS Agent**: Swift SwiftUI implementation
- **Android Agent**: Kotlin Jetpack Compose implementation
- **Web Agent**: React TypeScript PWA implementation

All agents work in parallel with coordinated integration points.

## 🔒 Security & Privacy

**Security-First Development**:
- All encryption implementations follow industry standards
- Regular security audits and testing
- No sensitive data in logs or code
- Comprehensive input validation
- Rate limiting and abuse prevention

**Privacy Guarantees**:
- E2E tier: Server cannot decrypt user content
- UCE tier: Data encrypted at rest, secure key derivation
- No tracking or analytics without consent
- Clear privacy policy and data handling

## 📄 License

[License to be determined]

## 🤖 AI Agent Development

This project is developed autonomously by AI agents following comprehensive specifications. The development process emphasizes:
- Security-first implementation
- Quality over speed
- Comprehensive testing
- Detailed documentation
- Production-ready deployment

---

**Developed autonomously with AI agents** | **Privacy-first design** | **Mobile-first approach**