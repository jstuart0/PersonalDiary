# Personal Diary Platform - Project Overview

## 🎯 Platform Summary

The Personal Diary Platform is a privacy-first digital diary application with **dual-tier encryption**, allowing users to choose between maximum privacy (E2E) or smart features (UCE) while maintaining their digital journal as the source of truth for all social media content.

### Key Innovation: Dual-Tier Encryption

Users choose their encryption model at signup (permanent choice):
- **E2E (End-to-End Encrypted)**: Maximum privacy, keys never on server, limited features
- **UCE (User-Controlled Encryption)**: Full features, encrypted at rest, server can decrypt with password

## 📱 Available Platforms

### 1. **Web Application (PWA)**
- **URL**: https://diary.xmojo.net (when deployed)
- **Technology**: React + TypeScript Progressive Web App
- **Features**: Installable, offline support, cross-platform
- **Encryption**: Web Crypto API

### 2. **iOS Application**
- **Status**: Ready for TestFlight
- **Technology**: Native Swift + SwiftUI
- **Features**: Keychain integration, biometric auth, offline-first
- **Encryption**: CryptoKit (Apple's framework)

### 3. **Android Application**
- **Status**: Ready for Play Store internal testing
- **Technology**: Native Kotlin + Jetpack Compose
- **Features**: KeyStore integration, biometric auth, Material Design 3
- **Encryption**: Google Tink

### 4. **Backend API**
- **URL**: https://api.diary.xmojo.net
- **Technology**: Python + FastAPI
- **Database**: PostgreSQL 15+ with Redis cache
- **Deployment**: Kubernetes cluster 'thor'

## 🔐 Encryption Tiers Explained

### E2E Tier (Maximum Privacy)
**For privacy enthusiasts who want absolute control**

✅ **Advantages:**
- Private keys never leave your device
- Server cannot decrypt your content
- True end-to-end encryption
- Government-grade security (X25519 + AES-256-GCM)
- Recovery via 10 recovery codes

❌ **Limitations:**
- Limited server-side search (metadata only)
- Manual device setup for new devices
- No AI features or smart suggestions
- Limited social media automation

**Perfect for:** Privacy advocates, journalists, sensitive content

### UCE Tier (Smart Features)
**For users who want convenience with security**

✅ **Advantages:**
- Full-text search across all entries
- AI-powered auto-tagging and suggestions
- Easy account recovery via email
- Instant multi-device sync
- Advanced social media features
- Server-side processing capabilities

🔒 **Security:**
- Content encrypted at rest on server
- Password-derived master key (Argon2id)
- Server can only decrypt with your password
- Transport encryption (HTTPS/TLS)

**Perfect for:** Most users, content creators, heavy social media users

## 💰 Pricing (Same for Both Tiers)

Privacy is not a premium feature - both tiers cost the same:

| Tier | Storage | Price | Target User |
|------|---------|-------|-------------|
| Free | 1GB | $0 | Trial users |
| Paid | 50GB | $8/month | Power users |

## 🌟 Core Features

### Diary Management
- **Rich Text Entries**: Full markdown support, formatting
- **Media Support**: Photos, videos with encryption
- **Tagging System**: Manual and auto-generated tags
- **Entry History**: Track all changes and versions
- **Search**: Tier-appropriate search capabilities

### Social Media Integration
- **Facebook Integration**: Import posts, share entries
- **Instagram**: Import stories and posts (coming soon)
- **Twitter**: Cross-posting capabilities (coming soon)
- **Deduplication**: Automatic duplicate detection
- **Privacy Controls**: Choose what to share publicly

### Multi-Device Sync
- **Real-time Sync**: Changes appear instantly across devices
- **Offline Support**: All features work without internet
- **Conflict Resolution**: Smart merging of simultaneous edits
- **Device Management**: See and manage connected devices

## 🚀 Getting Started

### Web Application
1. Visit https://diary.xmojo.net
2. Click "Get Started" or "Sign Up"
3. Choose your encryption tier (E2E or UCE)
4. Create your account
5. Start writing your first entry!

### Mobile Applications
1. Download from App Store (iOS) or Play Store (Android)
2. Open the app and tap "Create Account"
3. Choose your encryption tier
4. Complete the signup process
5. Enable biometric authentication for security

### First Entry
1. Tap the "+" button or "Create Entry"
2. Write your diary entry (text, photos, videos)
3. Add tags to categorize your entry
4. Save - it's automatically encrypted and synced

## 🔒 Security & Privacy

### Data Protection
- **No Plaintext Storage**: All content encrypted before storage
- **Secure Transport**: HTTPS/TLS for all communications
- **Input Validation**: Comprehensive protection against attacks
- **Rate Limiting**: Prevents abuse and brute-force attacks

### Privacy Guarantees
- **E2E Tier**: Server cannot access your content under any circumstances
- **UCE Tier**: Content encrypted at rest, only accessible with your password
- **No Tracking**: No analytics or tracking without explicit consent
- **GDPR Compliant**: Full data portability and deletion rights

### Recovery Options
- **E2E Tier**: 10 recovery codes (save them safely!)
- **UCE Tier**: Email-based password reset
- **Account Export**: Download all your data anytime

## 📊 Feature Comparison

| Feature | E2E Tier | UCE Tier |
|---------|----------|----------|
| **Basic Diary** | ✅ | ✅ |
| Text entries | ✅ | ✅ |
| Photo/video attachments | ✅ | ✅ |
| Manual tags | ✅ | ✅ |
| **Search & Discovery** | | |
| Client-side search | ✅ | ✅ |
| Server-side search | ❌ | ✅ |
| Auto-generated tags | ❌ | ✅ |
| AI suggestions | ❌ | ✅ |
| **Social Media** | | |
| Facebook import | ✅ | ✅ |
| Facebook sharing | ✅ | ✅ |
| Advanced automation | ❌ | ✅ |
| **Device Management** | | |
| Multi-device sync | Manual | Auto |
| Easy recovery | ❌ | ✅ |
| Device authorization | Manual | Auto |

## 🛠️ Technical Architecture

### Security Implementation
- **E2E Encryption**: X25519 key agreement + AES-256-GCM
- **UCE Encryption**: Argon2id key derivation + AES-256-GCM
- **Password Hashing**: Argon2id (UCE) / Recovery codes (E2E)
- **Content Hashing**: SHA-256 for deduplication

### Infrastructure
- **Deployment**: Kubernetes cluster 'thor'
- **Database**: PostgreSQL 15+ with full-text search
- **Cache**: Redis for session management and job queue
- **Storage**: AWS S3 for encrypted media files
- **CDN**: Cloudflare for global performance

### Development
- **Backend**: Python 3.11+ with FastAPI
- **iOS**: Swift 5.9+ with SwiftUI and CryptoKit
- **Android**: Kotlin 1.9+ with Jetpack Compose and Google Tink
- **Web**: React 18+ with TypeScript and Web Crypto API

## 📞 Support & Community

### Getting Help
- **Documentation**: Complete guides available in this wiki
- **User Guides**: Step-by-step tutorials for all features
- **Developer Docs**: API documentation for integrations
- **Troubleshooting**: Common issues and solutions

### Contact
- **Email Support**: support@diary.xmojo.net
- **Bug Reports**: GitHub Issues
- **Feature Requests**: Submit via app or GitHub
- **Security Issues**: security@diary.xmojo.net

### Community
- **Discord**: Join our community server
- **Reddit**: r/PersonalDiary
- **Twitter**: @PersonalDiaryApp
- **Blog**: Latest updates and tutorials

---

**Welcome to the Personal Diary Platform - Your privacy-first digital journal.**