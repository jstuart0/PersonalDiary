# Deployment Guide

This guide covers deploying the Personal Diary PWA to various platforms.

## Prerequisites

- Node.js 20+
- npm or yarn
- Environment variables configured

## Environment Variables

Create a `.env` file based on `.env.example`:

```bash
cp .env.example .env
```

Required variables:
- `VITE_API_BASE_URL` - Backend API URL
- `VITE_FACEBOOK_APP_ID` - Facebook App ID (optional)
- `VITE_ENVIRONMENT` - Environment name (development/production)

## Build for Production

```bash
npm run build
```

This creates an optimized production build in the `dist/` directory.

## Deployment Options

### Option 1: Vercel

1. Install Vercel CLI:
   ```bash
   npm install -g vercel
   ```

2. Deploy:
   ```bash
   vercel
   ```

3. Set environment variables in Vercel dashboard

Configuration is in `vercel.json`.

### Option 2: Netlify

1. Install Netlify CLI:
   ```bash
   npm install -g netlify-cli
   ```

2. Deploy:
   ```bash
   netlify deploy --prod
   ```

3. Set environment variables in Netlify dashboard

Configuration is in `netlify.toml`.

### Option 3: Docker

1. Build image:
   ```bash
   docker build -t personal-diary-web .
   ```

2. Run container:
   ```bash
   docker run -p 80:80 personal-diary-web
   ```

3. Or use docker-compose:
   ```bash
   docker-compose up -d
   ```

### Option 4: Static Hosting (S3, Azure Storage, etc.)

1. Build the app:
   ```bash
   npm run build
   ```

2. Upload `dist/` contents to your static hosting service

3. Configure:
   - Set up routing to serve `index.html` for all routes
   - Configure CORS if needed
   - Add security headers

## Post-Deployment Checklist

- [ ] Verify PWA manifest is accessible
- [ ] Test service worker registration
- [ ] Check offline functionality
- [ ] Test install prompt on mobile
- [ ] Verify HTTPS is enabled
- [ ] Test authentication flow
- [ ] Verify encryption is working
- [ ] Run Lighthouse audit (target: 90+)
- [ ] Test on multiple devices
- [ ] Configure monitoring/analytics

## Performance Optimization

The build is optimized with:
- Code splitting
- Tree shaking
- Minification
- Compression (gzip/brotli)
- Asset caching
- Service worker caching

Target Lighthouse scores:
- Performance: 90+
- Accessibility: 95+
- Best Practices: 95+
- SEO: 90+
- PWA: 100

## Security

The app includes:
- Content Security Policy headers
- XSS protection headers
- Frame options
- HTTPS enforcement
- End-to-end encryption for data

## Monitoring

Recommended monitoring:
- Error tracking (Sentry, etc.)
- Analytics (Google Analytics, Plausible, etc.)
- Performance monitoring (Lighthouse CI)
- Uptime monitoring

## Rollback

If issues occur:

### Vercel
```bash
vercel rollback
```

### Netlify
Use Netlify dashboard to rollback to previous deployment

### Docker
```bash
docker pull personal-diary-web:previous-tag
docker-compose up -d
```

## Support

For issues or questions:
- Check logs in deployment platform
- Review browser console for errors
- Check service worker status
- Verify API connectivity
