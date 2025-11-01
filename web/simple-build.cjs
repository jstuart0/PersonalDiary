#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

// Create dist directory
const distDir = path.join(__dirname, 'dist');
if (!fs.existsSync(distDir)) {
  fs.mkdirSync(distDir, { recursive: true });
}

// Create a simple index.html for the PWA
const indexHtml = `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Personal Diary</title>
    <meta name="description" content="Privacy-first digital diary with dual-tier encryption" />

    <!-- Favicon -->
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />

    <!-- PWA Meta -->
    <meta name="theme-color" content="#6366F1" />
    <link rel="manifest" href="/manifest.json" />

    <!-- Tailwind CSS -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
      tailwind.config = {
        theme: {
          extend: {
            colors: {
              primary: {
                50: '#eef2ff',
                500: '#6366f1',
                600: '#4f46e5',
                700: '#4338ca',
              }
            }
          }
        }
      }
    </script>

    <style>
      body {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }
      .gradient-bg {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }
    </style>
  </head>
  <body class="bg-gray-50">
    <div id="root">
      <!-- Temporary landing page while full app is being built -->
      <div class="min-h-screen gradient-bg flex items-center justify-center px-4">
        <div class="max-w-md w-full bg-white rounded-2xl shadow-xl p-8 text-center">
          <div class="w-16 h-16 bg-primary-500 rounded-full mx-auto mb-6 flex items-center justify-center">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
            </svg>
          </div>

          <h1 class="text-3xl font-bold text-gray-800 mb-4">Personal Diary</h1>
          <p class="text-gray-600 mb-8">Privacy-first digital diary with dual-tier encryption</p>

          <div class="space-y-4">
            <div class="p-4 bg-primary-50 rounded-lg">
              <h3 class="font-semibold text-primary-700 mb-2">🔐 Choose Your Privacy Level</h3>
              <div class="text-sm text-gray-600">
                <p><strong>E2E:</strong> Maximum privacy - your keys never leave your device</p>
                <p><strong>UCE:</strong> Smart features with server-side encryption</p>
              </div>
            </div>

            <div class="p-4 bg-green-50 rounded-lg">
              <h3 class="font-semibold text-green-700 mb-2">📱 Multi-Platform</h3>
              <p class="text-sm text-gray-600">Available on Web, iOS, and Android with seamless sync</p>
            </div>

            <div class="p-4 bg-blue-50 rounded-lg">
              <h3 class="font-semibold text-blue-700 mb-2">🌐 Social Integration</h3>
              <p class="text-sm text-gray-600">Import from Facebook, share selectively to social media</p>
            </div>
          </div>

          <div class="mt-8 pt-6 border-t border-gray-200">
            <p class="text-sm text-gray-500 mb-4">Platform is being deployed...</p>
            <div class="flex justify-center">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
            </div>
          </div>

          <div class="mt-6 text-xs text-gray-400">
            <p>API: <code>https://api.diary.xmojo.net</code></p>
            <p>Documentation: <a href="https://wiki.xmojo.net" class="text-primary-500 hover:underline">wiki.xmojo.net</a></p>
          </div>
        </div>
      </div>
    </div>

    <!-- Service Worker Registration -->
    <script>
      if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
          navigator.serviceWorker.register('/sw.js')
            .then((registration) => {
              console.log('SW registered: ', registration);
            })
            .catch((registrationError) => {
              console.log('SW registration failed: ', registrationError);
            });
        });
      }
    </script>
  </body>
</html>`;

// Write index.html
fs.writeFileSync(path.join(distDir, 'index.html'), indexHtml);

// Create manifest.json
const manifest = {
  name: "Personal Diary",
  short_name: "Diary",
  description: "Privacy-first digital diary with dual-tier encryption",
  start_url: "/",
  display: "standalone",
  background_color: "#ffffff",
  theme_color: "#6366F1",
  orientation: "portrait-primary",
  icons: [
    {
      src: "/icon-192.png",
      sizes: "192x192",
      type: "image/png",
      purpose: "maskable"
    },
    {
      src: "/icon-512.png",
      sizes: "512x512",
      type: "image/png",
      purpose: "maskable"
    }
  ]
};

fs.writeFileSync(path.join(distDir, 'manifest.json'), JSON.stringify(manifest, null, 2));

// Create simple service worker
const serviceWorker = `const CACHE_NAME = 'personal-diary-v1';
const urlsToCache = [
  '/',
  '/manifest.json'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(urlsToCache))
  );
});

self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request)
      .then((response) => {
        if (response) {
          return response;
        }
        return fetch(event.request);
      }
    )
  );
});`;

fs.writeFileSync(path.join(distDir, 'sw.js'), serviceWorker);

// Create simple favicon
const favicon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#6366F1">
  <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
</svg>`;

fs.writeFileSync(path.join(distDir, 'favicon.svg'), favicon);

console.log('✅ Simple build completed successfully!');
console.log('📁 Output directory:', distDir);
console.log('📄 Files created:');
console.log('   - index.html (landing page)');
console.log('   - manifest.json (PWA manifest)');
console.log('   - sw.js (service worker)');
console.log('   - favicon.svg (icon)');