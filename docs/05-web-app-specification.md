# Web App Specification - Personal Diary Platform

**Document Version:** 1.0  
**Last Updated:** October 31, 2025  
**Target:** AI Agent Implementation  
**Platform:** Progressive Web App  
**Approach:** Modern Framework + PWA

---

## 📋 Overview

Implement a Progressive Web App that handles client-side encryption, offline storage, and seamless sync. Must work on desktop and mobile browsers.

**Key Requirements:**
- Modern framework (React/Vue/Svelte - agent chooses)
- PWA capabilities (installable, offline)
- Client-side encryption (both E2E and UCE)
- IndexedDB for offline storage
- Responsive design
- Web Crypto API

---

## 🎯 Technical Stack

**Framework Options:**
- React + Vite (recommended)
- Vue 3 + Vite
- Svelte + SvelteKit

**State Management:**
- React: Context API or Zustand
- Vue: Pinia
- Svelte: Stores

**Styling:**
- Tailwind CSS (recommended)
- Or Material UI / Vuetify / Carbon

**Build Tool:** Vite (recommended)

---

## 🏗️ Architecture

```
React Components
    ↓
Custom Hooks / Context
    ↓
Services (Encryption, API, Sync, Media)
    ↓
IndexedDB + Cache Storage
```

**Directory Structure:**
```
web/
├── public/
│   ├── manifest.json
│   └── icons/
├── src/
│   ├── components/
│   ├── pages/
│   ├── hooks/
│   ├── services/
│   ├── context/
│   ├── utils/
│   └── workers/
└── tests/
```

---

## 🔐 Encryption Implementation

### Web Crypto API

**E2E Encryption:**
- Generate keypair with Web Crypto API
- Store private key in IndexedDB (encrypted)
- Public key to server

**UCE Encryption:**
- Derive key from password (PBKDF2)
- Decrypt master key from server
- Encrypt entries locally (recommended)

**Implementation:**
- `generateKeyPair()` - E2E keys
- `deriveKey()` - Password-based
- `encryptEntry()` - Encrypt content
- `decryptEntry()` - Decrypt content
- `generateHash()` - SHA-256

---

## 💾 IndexedDB Storage

**Stores:**
- Users
- Entries (with indexes on userId, createdAt, syncStatus)
- Media
- Tags
- SyncMetadata
- Keys (encrypted)

**Library:** idb (recommended) or Dexie.js

**Offline Support:**
- Store all entries locally
- Sync when online
- Queue pending operations
- Conflict resolution

---

## 🎨 UI Specifications

### Responsive Breakpoints
- Mobile: < 640px
- Tablet: 640-1024px
- Desktop: > 1024px

### Key Screens

**1. Signup Page**
- Email/password fields
- Validation
- Link to login

**2. Encryption Tier Selection**
- Two cards side-by-side (desktop)
- Stacked (mobile)
- Feature comparison
- Warning banner

**3. Recovery Codes (E2E)**
- Grid of 10 codes
- Download/copy buttons
- Required confirmation checkbox

**4. Login Page**
- Email/password
- Remember me option
- Forgot password link

**5. Timeline Page**
- Header with encryption badge
- Sidebar (desktop) / bottom nav (mobile)
- Entry cards
- Pull-to-refresh
- FAB for create

**6. Entry Detail**
- Full content
- Media gallery
- Tags
- Actions (edit, delete, share)

**7. Create/Edit Entry**
- Title input
- Content textarea
- Media upload (drag-drop)
- Tag input
- Save button

**8. Search Page**
- Search bar
- Filters panel
- Results list
- Different for E2E vs UCE

**9. Settings Page**
- Account section
- Sync section
- Storage section
- Social media section
- Appearance section

**10. Facebook Integration**
- Connect button (OAuth popup)
- Share modal
- Import modal with progress

---

## 🔄 Progressive Web App

### Requirements

**1. Web App Manifest**
```json
{
  "name": "Personal Diary",
  "short_name": "Diary",
  "icons": [...],
  "start_url": "/",
  "display": "standalone",
  "theme_color": "#...",
  "background_color": "#..."
}
```

**2. Service Worker**
- Cache static assets (cache-first)
- Network-first for API
- Stale-while-revalidate for images
- Background sync for pending entries
- Offline fallback page

**3. Install Prompt**
- Detect installability
- Show custom prompt
- Handle install/dismiss

**4. Offline Support**
- All features work offline
- Queue operations when offline
- Sync when reconnected

---

## 📸 Media Handling

**File Upload:**
- Drag-and-drop zone
- File input
- Multiple selection
- Validation (size, type)

**Image Compression:**
- Use browser-image-compression
- Compress before encryption
- Progress indicator

**Encryption:**
- Encrypt with Web Crypto
- Store in IndexedDB
- Upload to server

**Display:**
- Decrypt for display
- Lazy loading
- Lightbox viewer

---

## 🔄 Sync Implementation

**Strategies:**
1. Periodic sync (every 5 minutes)
2. Background sync (Service Worker)
3. Manual sync button
4. Sync on focus/reconnect

**Sync Service:**
- `syncAll()` - Full sync
- `syncIncremental()` - Delta sync
- `uploadPending()` - Upload queue
- `downloadNew()` - Fetch new
- `resolveConflicts()` - Last write wins

**Optional: WebSocket** for real-time updates

---

## 🧪 Testing Requirements

**Unit Tests:**
- Encryption functions
- API client
- Custom hooks
- Utilities
- Target: 80%+

**Integration Tests:**
- Signup/login flow
- Entry creation
- Media upload
- Sync process

**E2E Tests:**
- Complete user journeys
- Use Playwright or Cypress
- Test all critical paths

---

## 🚀 Deployment

**Hosting Options:**
- Vercel (recommended)
- Netlify
- Cloudflare Pages

**Build Configuration:**
- Production optimizations
- Environment variables
- Code splitting
- Tree shaking
- Minification

**Performance:**
- Lighthouse score > 90
- FCP < 1.5s
- TTI < 3.5s
- LCP < 2.5s
- CLS < 0.1

---

## ♿ Accessibility

**Requirements:**
- Semantic HTML
- ARIA labels
- Keyboard navigation
- Focus states
- Alt text for images
- Form labels
- Color contrast (WCAG AA)
- Screen reader support

**Testing:**
- axe DevTools
- Keyboard-only testing
- Screen reader testing

---

## 📝 Implementation Checklist

**Phase 1: Setup**
- [ ] Create Vite project
- [ ] Configure routing
- [ ] Set up styling
- [ ] Component library basics

**Phase 2: Authentication**
- [ ] Signup/login pages
- [ ] Tier selection
- [ ] E2E key generation
- [ ] UCE key derivation

**Phase 3: Core Features**
- [ ] Timeline page
- [ ] Entry detail/create
- [ ] IndexedDB service
- [ ] Encryption service
- [ ] API client

**Phase 4: Sync & Media**
- [ ] Sync service
- [ ] Offline mode
- [ ] Media upload
- [ ] Background sync

**Phase 5: Social**
- [ ] Facebook OAuth
- [ ] Share/import UI

**Phase 6: Search**
- [ ] Search UI
- [ ] E2E client search
- [ ] UCE server search

**Phase 7: PWA**
- [ ] Service Worker
- [ ] Manifest
- [ ] Install prompt
- [ ] Offline page

**Phase 8: Polish**
- [ ] Settings page
- [ ] Animations
- [ ] Accessibility
- [ ] Tests
- [ ] Performance optimization

---

## 🎯 Success Criteria

- [ ] PWA installable
- [ ] Works offline
- [ ] Both encryption tiers work
- [ ] Entry creation/sync works
- [ ] Facebook integration works
- [ ] Search works (tier-appropriate)
- [ ] Lighthouse > 90
- [ ] 80%+ test coverage
- [ ] WCAG AA compliant

---

**End of Web App Specification**

Agent should build a production-ready PWA that provides excellent UX on all devices and browsers.
