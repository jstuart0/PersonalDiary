# Personal Diary PWA

A privacy-first, encrypted diary platform with Progressive Web App capabilities.

## Features

### Core Functionality
- **End-to-End Encryption (E2E)** - Your entries never leave your device unencrypted
- **User-Controlled Encryption (UCE)** - Server-assisted encryption with password-based key derivation
- **Rich Entry Management** - Create, edit, delete, and organize diary entries
- **Media Upload** - Attach photos with client-side compression and encryption
- **Advanced Search** - Full-text search with Fuse.js, tag filtering, and date ranges
- **Offline Support** - Complete offline functionality with service workers
- **PWA Installation** - Install as a native app on any device

### User Experience
- **Modern UI** - Clean, responsive design with Tailwind CSS
- **Dark Mode Ready** - System-aware theme support
- **Keyboard Shortcuts** - Cmd/Ctrl + Enter to save, and more
- **Mobile Optimized** - Touch-friendly interface
- **Progressive Loading** - Skeleton screens and optimistic updates

### Integrations
- **Facebook Sharing** - Post entries to Facebook (private by default)
- **Facebook Import** - Import your Facebook posts (coming soon)
- **Cloud Sync** - Automatic background sync when online

## Technology Stack

### Frontend Framework
- **React 19** - Latest React with hooks and concurrent features
- **TypeScript** - Full type safety
- **Vite** - Lightning-fast build tool
- **React Router** - Client-side routing

### Styling
- **Tailwind CSS 4** - Utility-first CSS framework
- **Framer Motion** - Smooth animations
- **Heroicons** - Beautiful icon set

### Data & Storage
- **IndexedDB** - Local database via idb library
- **Web Crypto API** - Native encryption
- **Service Workers** - Offline-first architecture

### Search & Performance
- **Fuse.js** - Fuzzy search
- **Web Workers** - Background processing
- **Code Splitting** - Optimized bundle sizes

### Testing
- **Vitest** - Unit and component testing
- **Testing Library** - React component testing
- **Playwright** - End-to-end testing

### DevOps
- **PWA Plugin** - Automatic service worker generation
- **Docker** - Containerized deployment
- **Vercel/Netlify Ready** - Zero-config deployment

## Project Structure

```
web/
├── src/
│   ├── components/          # React components
│   │   ├── common/          # Reusable UI components
│   │   ├── auth/            # Authentication components
│   │   ├── entries/         # Entry management components
│   │   ├── search/          # Search components
│   │   ├── media/           # Media upload components
│   │   ├── facebook/        # Facebook integration
│   │   └── pwa/             # PWA prompts and indicators
│   ├── pages/               # Page components
│   │   ├── AuthPage.tsx     # Login/signup
│   │   └── TimelinePage.tsx # Main diary timeline
│   ├── services/            # Business logic
│   │   ├── api/             # API client
│   │   ├── encryption/      # Encryption service
│   │   ├── storage/         # IndexedDB service
│   │   ├── entries/         # Entry management
│   │   ├── media/           # Media handling
│   │   ├── search/          # Search service
│   │   └── facebook/        # Facebook integration
│   ├── context/             # React contexts
│   │   ├── AuthContext.tsx
│   │   ├── EncryptionContext.tsx
│   │   ├── SyncContext.tsx
│   │   └── SettingsContext.tsx
│   ├── types/               # TypeScript types
│   ├── App.tsx              # Root component
│   └── main.tsx             # Entry point
├── tests/                   # Test files
│   └── e2e/                 # Playwright tests
├── public/                  # Static assets
├── vite.config.ts           # Vite configuration
├── tailwind.config.js       # Tailwind configuration
├── tsconfig.json            # TypeScript configuration
└── package.json             # Dependencies
```

## Getting Started

### Prerequisites
- Node.js 20+
- npm or yarn

### Installation

1. Clone the repository:
   ```bash
   cd /Users/jaystuart/dev/personal-diary/web
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Create environment file:
   ```bash
   cp .env.example .env
   ```

4. Configure environment variables in `.env`:
   ```
   VITE_API_BASE_URL=http://localhost:8000/api/v1
   VITE_FACEBOOK_APP_ID=your_facebook_app_id
   ```

### Development

Start development server:
```bash
npm run dev
```

The app will be available at http://localhost:5173

### Building

Build for production:
```bash
npm run build
```

Preview production build:
```bash
npm run preview
```

### Testing

Run unit tests:
```bash
npm test
```

Run tests with UI:
```bash
npm run test:ui
```

Run tests with coverage:
```bash
npm run test:coverage
```

Run E2E tests:
```bash
npm run test:e2e
```

Run E2E tests with UI:
```bash
npm run test:e2e:ui
```

### Type Checking

```bash
npm run type-check
```

### Linting

```bash
npm run lint
```

## Architecture

### Encryption Model

The app supports two encryption tiers:

#### End-to-End Encryption (E2E)
- RSA keypair generated on device
- Private key encrypted with password
- Content encrypted with AES-256-GCM
- Server never sees plaintext
- Includes recovery codes

#### User-Controlled Encryption (UCE)
- Password-based key derivation (PBKDF2)
- Master key stored on server (encrypted)
- Content encrypted with AES-256-GCM
- Server can assist with search/features

### Data Flow

1. **Entry Creation**
   - User writes entry
   - Content encrypted locally
   - Encrypted data saved to IndexedDB
   - Sync operation queued
   - Background sync uploads to server

2. **Entry Retrieval**
   - Encrypted data loaded from IndexedDB
   - Content decrypted in memory
   - Displayed to user
   - Never persisted in plaintext

3. **Media Upload**
   - Image compressed on device
   - Compressed image encrypted
   - Encrypted blob saved to IndexedDB
   - Sync uploads to S3
   - Decryption happens on-demand

### Offline Support

The service worker provides:
- Offline page access
- Background sync
- Cache management
- Update notifications

Cached resources:
- App shell (HTML, CSS, JS)
- Static assets
- API responses (temporary)
- Media thumbnails

## Deployment

See [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed deployment instructions.

Quick deploy options:
- **Vercel**: `vercel`
- **Netlify**: `netlify deploy --prod`
- **Docker**: `docker build -t personal-diary-web .`

## Performance

Target Lighthouse scores:
- **Performance**: 90+
- **Accessibility**: 95+
- **Best Practices**: 95+
- **SEO**: 90+
- **PWA**: 100

Optimizations:
- Code splitting by route
- Lazy loading components
- Image compression
- Service worker caching
- Asset optimization
- Tree shaking

## Security

Security measures:
- End-to-end encryption
- CSP headers
- XSS protection
- HTTPS enforcement
- Secure cookie handling
- Input sanitization

## Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

PWA features require:
- Service Worker support
- IndexedDB support
- Web Crypto API support

## Contributing

This is a personal project, but suggestions and bug reports are welcome.

## License

All rights reserved.

## Roadmap

### Near-term (Next Release)
- [ ] Instagram integration
- [ ] Twitter/X integration
- [ ] Export to PDF
- [ ] Voice notes
- [ ] Calendar view

### Mid-term
- [ ] Multi-device sync improvements
- [ ] Collaborative entries
- [ ] Advanced analytics
- [ ] Themes customization
- [ ] Plugin system

### Long-term
- [ ] Native mobile apps
- [ ] Desktop apps (Electron)
- [ ] AI-powered insights
- [ ] Mood tracking
- [ ] Photo albums

## Status

**Current Version**: 1.0.0 (100% Complete)

All core features implemented:
- ✅ Entry management interface
- ✅ Media upload with encryption
- ✅ Search with Fuse.js
- ✅ Service worker and PWA
- ✅ Facebook integration
- ✅ Comprehensive testing suite
- ✅ Production deployment config

Ready for production deployment!

## Support

For issues or questions:
- Check browser console for errors
- Verify IndexedDB is enabled
- Check service worker registration
- Ensure HTTPS (required for PWA)

## Acknowledgments

Built with modern web technologies and best practices for privacy and security.
