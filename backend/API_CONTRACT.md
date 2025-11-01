# Personal Diary API - Contract for Client Applications

**Version:** 1.0.0
**Base URL:** `https://api.diary.xmojo.net` (production) | `http://localhost:8000` (development)
**API Prefix:** `/api/v1`

---

## Authentication

### Headers
All authenticated endpoints require:
```http
Authorization: Bearer <jwt_access_token>
Content-Type: application/json
```

### Token Lifecycle
- **Access Token**: 15 minutes validity
- **Refresh Token**: 30 days validity
- **Auto-refresh**: Implement client-side refresh logic before access token expires

### Token Refresh Flow
```http
POST /api/v1/auth/refresh
Query Params: refresh_token=<refresh_token>

Response:
{
  "access_token": "new_jwt_token",
  "token_type": "bearer"
}
```

---

## Encryption Requirements

### For E2E Tier Users

**Client Responsibilities:**
1. Generate X25519 keypair on signup
2. Send public key to server (base64 encoded, 32 bytes)
3. **Never** send private key to server
4. Encrypt all content before API calls (ChaCha20-Poly1305)
5. Decrypt all content after API responses
6. Store private key securely (Keychain/KeyStore/IndexedDB)
7. Save recovery codes shown once during signup

**Encryption Format:**
```javascript
// Pseudocode
encrypted_content = base64(
  nonce (12 bytes) +
  ciphertext +
  auth_tag (16 bytes)
)
```

**Content Hash:**
```javascript
content_hash = sha256(plaintext_content).hexdigest()
```

### For UCE Tier Users

**Client Responsibilities:**
1. Password hashing is handled server-side
2. Content encryption is handled server-side
3. Master key returned encrypted on login
4. Client can optionally decrypt for display
5. Search works server-side

---

## API Endpoints

### Authentication

#### Signup
```http
POST /api/v1/auth/signup

Request:
{
  "email": "user@example.com",
  "password": "StrongPassword123!",  // Min 12 chars, upper+lower+digit
  "encryption_tier": "e2e",  // or "uce"
  "public_key": "base64_encoded_public_key"  // Required for E2E only
}

Response (201):
{
  "user_id": "uuid",
  "email": "user@example.com",
  "encryption_tier": "e2e",
  "jwt_token": "access_token",
  "refresh_token": "refresh_token",
  "public_key": "base64_public_key",  // E2E only
  "recovery_codes": ["XXXX-XXXX-XXXX-XXXX", ...],  // E2E only, shown ONCE
  "encrypted_master_key": "base64_encrypted_key"  // UCE only
}
```

#### Login
```http
POST /api/v1/auth/login

Request:
{
  "email": "user@example.com",
  "password": "StrongPassword123!"
}

Response (200):
{
  "user_id": "uuid",
  "email": "user@example.com",
  "encryption_tier": "e2e",
  "jwt_token": "access_token",
  "refresh_token": "refresh_token",
  "public_key": "base64_public_key",  // E2E only
  "encrypted_master_key": "base64_encrypted_key"  // UCE only
}
```

#### Get Current User
```http
GET /api/v1/auth/me
Authorization: Bearer <token>

Response (200):
{
  "id": "uuid",
  "email": "user@example.com",
  "encryption_tier": "e2e",
  "created_at": "2024-01-01T00:00:00Z",
  "is_active": true,
  "is_paid_tier": false,
  "display_name": null
}
```

#### Get Feature Gates
```http
GET /api/v1/auth/features
Authorization: Bearer <token>

Response (200):
{
  "user_id": "uuid",
  "encryption_tier": "uce",
  "features": {
    "server_search": true,      // UCE only
    "server_ai": true,           // UCE only
    "easy_recovery": true,       // UCE only
    "auto_multi_device_sync": true,  // UCE only
    "user_sharing": false        // Not implemented
  },
  "storage": {
    "used_bytes": 1048576,
    "limit_bytes": 1073741824,
    "percentage_used": 0.1
  },
  "tier_info": {
    "name": "Free",
    "price": "$0/mo"
  }
}
```

### Entries

#### Create Entry
```http
POST /api/v1/entries/
Authorization: Bearer <token>

Request:
{
  "encrypted_content": "base64_encrypted_text",
  "content_hash": "sha256_hash_64_chars",
  "encrypted_title": "base64_encrypted_title",  // Optional
  "encrypted_location": "base64_encrypted_location",  // Optional
  "mood": "happy",  // Optional: happy, sad, neutral, excited, anxious, grateful
  "weather": {"temp": 72, "condition": "sunny"},  // Optional JSON
  "tag_names": ["personal", "happy"],  // Optional
  "source": "diary"  // Default: diary, facebook, instagram, twitter
}

Response (201):
{
  "id": "uuid",
  "user_id": "uuid",
  "encrypted_content": "base64_encrypted_text",
  "content_hash": "sha256_hash",
  "encrypted_title": "base64_encrypted_title",
  "encrypted_location": null,
  "mood": "happy",
  "weather": {"temp": 72, "condition": "sunny"},
  "source": "diary",
  "tags": [
    {"id": "uuid", "tag_name": "personal", "auto_generated": false, "created_at": "..."},
    {"id": "uuid", "tag_name": "happy", "auto_generated": false, "created_at": "..."}
  ],
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-01T00:00:00Z",
  "deleted_at": null
}
```

#### List Entries
```http
GET /api/v1/entries/
Authorization: Bearer <token>
Query Params:
  - page=1 (default: 1)
  - page_size=20 (default: 20, max: 100)
  - include_deleted=false
  - source=diary  // Optional filter
  - mood=happy  // Optional filter
  - tag=personal  // Optional filter

Response (200):
{
  "entries": [...],
  "total": 100,
  "page": 1,
  "page_size": 20,
  "total_pages": 5
}
```

#### Get Entry
```http
GET /api/v1/entries/{entry_id}
Authorization: Bearer <token>

Response (200):
{
  "id": "uuid",
  "user_id": "uuid",
  "encrypted_content": "base64_encrypted_text",
  ...
}
```

#### Update Entry
```http
PUT /api/v1/entries/{entry_id}
Authorization: Bearer <token>

Request:
{
  "encrypted_content": "new_encrypted_content",  // Optional
  "content_hash": "new_hash",  // Optional
  "mood": "grateful",  // Optional
  "tag_names": ["updated", "tags"]  // Optional
}

Response (200):
{
  // Updated entry object
}
```

#### Delete Entry (Soft)
```http
DELETE /api/v1/entries/{entry_id}
Authorization: Bearer <token>
Query Params:
  - permanent=false  // true for hard delete

Response (204):
No content
```

#### Restore Entry
```http
POST /api/v1/entries/{entry_id}/restore
Authorization: Bearer <token>

Response (200):
{
  // Restored entry object
}
```

### Media

#### Initiate Upload
```http
POST /api/v1/media/upload
Authorization: Bearer <token>

Request:
{
  "entry_id": "uuid",  // Optional, attach to entry
  "file_name": "photo.jpg",
  "file_size": 1048576,  // Bytes
  "mime_type": "image/jpeg",
  "encrypted_metadata": "base64_encrypted_exif"  // Optional
}

Response (201):
{
  "media_id": "uuid",
  "upload_url": "https://s3.amazonaws.com/presigned-url",
  "upload_fields": {
    "key": "...",
    "AWSAccessKeyId": "...",
    ...
  },
  "expires_at": "2024-01-01T01:00:00Z"
}
```

**Client Upload Flow:**
1. Call `/media/upload` to get presigned URL
2. Upload file directly to S3 using presigned URL (POST multipart/form-data)
3. File is now accessible via `/media/{id}/download`

#### Get Download URL
```http
GET /api/v1/media/{media_id}/download
Authorization: Bearer <token>

Response (200):
{
  "media_id": "uuid",
  "download_url": "https://s3.amazonaws.com/presigned-url",
  "file_name": "photo.jpg",
  "mime_type": "image/jpeg",
  "file_size": 1048576,
  "expires_at": "2024-01-01T01:00:00Z"
}
```

### Search (UCE Tier Only)

#### Search Entries
```http
POST /api/v1/search/
Authorization: Bearer <token>

Request:
{
  "query": "search terms",
  "mood_filter": "happy",  // Optional
  "source_filter": "diary",  // Optional
  "tag_filter": ["personal"],  // Optional
  "date_from": "2024-01-01T00:00:00Z",  // Optional
  "date_to": "2024-12-31T23:59:59Z",  // Optional
  "page": 1,
  "page_size": 20
}

Response (200):
{
  "results": [
    {
      "entry_id": "uuid",
      "encrypted_content": "...",
      "encrypted_title": "...",
      "mood": "happy",
      "source": "diary",
      "created_at": "...",
      "updated_at": "...",
      "relevance_score": 0.95,
      "snippet": "highlighted match"  // UCE only
    }
  ],
  "total": 42,
  "page": 1,
  "page_size": 20,
  "total_pages": 3,
  "query_time_ms": 45.2
}
```

**Note**: E2E users must search locally on device. API returns 403.

### Facebook Integration

#### Connect Facebook
```http
POST /api/v1/integrations/facebook/connect
Authorization: Bearer <token>

Request:
{
  "redirect_uri": "https://your-app.com/oauth/callback",
  "state": "random_csrf_token"  // Optional
}

Response (200):
{
  "authorization_url": "https://facebook.com/oauth/authorize?...",
  "state": "csrf_token"
}
```

#### OAuth Callback
```http
POST /api/v1/integrations/facebook/callback
Authorization: Bearer <token>

Request:
{
  "code": "oauth_code_from_facebook",
  "state": "csrf_token"
}

Response (200):
{
  "integration_account_id": "uuid",
  "provider": "facebook",
  "account_name": "John Doe",
  "connected_at": "2024-01-01T00:00:00Z",
  "scopes": ["email", "public_profile", "user_posts"]
}
```

#### Push to Facebook
```http
POST /api/v1/integrations/facebook/push
Authorization: Bearer <token>

Request:
{
  "entry_id": "uuid",
  "post_text": "Decrypted diary entry text",  // Client must decrypt first
  "privacy_setting": "SELF"  // SELF, FRIENDS, PUBLIC
}

Response (200):
{
  "entry_id": "uuid",
  "external_post_id": "uuid",
  "facebook_post_id": "fb_post_id",
  "posted_at": "2024-01-01T00:00:00Z",
  "post_url": "https://facebook.com/..."
}
```

#### Pull from Facebook
```http
POST /api/v1/integrations/facebook/pull
Authorization: Bearer <token>

Request:
{
  "since": "2024-01-01T00:00:00Z",  // Optional
  "limit": 100  // Default: 100, max: 500
}

Response (200):
{
  "posts": [
    {
      "facebook_post_id": "fb_id",
      "content": "Post text",
      "posted_at": "2024-01-01T00:00:00Z",
      "permalink_url": "https://facebook.com/..."
    }
  ],
  "total_pulled": 10,
  "imported_count": 8,
  "skipped_count": 2,
  "last_pull_at": "2024-01-01T00:00:00Z"
}
```

---

## Error Responses

### Standard Error Format
```json
{
  "detail": "Human-readable error message"
}
```

### Common HTTP Status Codes
- `200` - Success
- `201` - Created
- `204` - No Content (successful delete)
- `400` - Bad Request (validation error)
- `401` - Unauthorized (missing/invalid token)
- `403` - Forbidden (insufficient permissions, e.g., E2E trying to search)
- `404` - Not Found
- `413` - Payload Too Large (file size exceeded)
- `422` - Unprocessable Entity (Pydantic validation error)
- `500` - Internal Server Error

---

## Rate Limits

- **Authentication**: 5 requests per minute
- **API**: 100 requests per minute
- **Media Upload**: 10 requests per minute

Headers on rate-limited response:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1234567890
```

---

## Best Practices for Client Implementation

### 1. Token Management
```javascript
// Refresh token before it expires
if (tokenExpiresAt - Date.now() < 60000) { // 1 minute buffer
  await refreshAccessToken();
}
```

### 2. Encryption (E2E)
```javascript
// Always encrypt before sending
const encrypted = await encryptContent(plaintext, privateKey);
const hash = await sha256(plaintext);

await api.post('/entries/', {
  encrypted_content: encrypted,
  content_hash: hash
});

// Always decrypt after receiving
const entry = await api.get('/entries/123');
const plaintext = await decryptContent(entry.encrypted_content, privateKey);
```

### 3. Error Handling
```javascript
try {
  const response = await api.post('/entries/', data);
} catch (error) {
  if (error.status === 401) {
    // Token expired, refresh and retry
    await refreshToken();
    return api.post('/entries/', data);
  } else if (error.status === 403) {
    // Feature not available for your tier
    showUpgradePrompt();
  } else {
    // Handle other errors
    showErrorMessage(error.detail);
  }
}
```

### 4. Offline Support
```javascript
// Queue operations when offline
if (!navigator.onLine) {
  await localDB.saveToQueue('create_entry', entryData);
} else {
  await api.post('/entries/', entryData);
}

// Sync queue when online
window.addEventListener('online', async () => {
  await processOfflineQueue();
});
```

---

## WebSocket Support

**Status**: Not yet implemented

**Planned for v2.0**:
- Real-time sync notifications
- Multi-device conflict resolution
- Live collaboration (future)

---

## Versioning

- **Current Version**: v1
- **API Prefix**: `/api/v1`
- **Breaking Changes**: Will increment version (e.g., `/api/v2`)
- **Deprecation**: 6-month notice before removal

---

## Support

- **Interactive Docs**: https://api.diary.xmojo.net/api/v1/docs
- **Health Check**: https://api.diary.xmojo.net/health
- **Status Page**: (TBD)

---

**Last Updated**: November 1, 2024
**API Version**: 1.0.0
