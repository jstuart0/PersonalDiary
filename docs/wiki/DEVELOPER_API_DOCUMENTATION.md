# Personal Diary Platform - API Documentation

## 🔧 Developer API Reference

The Personal Diary Platform provides a comprehensive REST API for developers who want to integrate with the platform or build custom applications.

## 🌐 Base URL and Authentication

### Production API
- **Base URL**: `https://api.diary.xmojo.net/api/v1`
- **Protocol**: HTTPS only
- **Format**: JSON

### Authentication
All API endpoints require authentication via JWT Bearer tokens.

```http
Authorization: Bearer <your_jwt_token>
```

## 🔐 Authentication Endpoints

### POST /auth/register
Create a new user account with encryption tier selection.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "encryption_tier": "E2E", // or "UCE"
  "public_key": "base64_encoded_public_key" // E2E only
}
```

**Response (200):**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGci...",
  "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGci...",
  "token_type": "bearer",
  "expires_in": 900,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "encryption_tier": "E2E",
    "storage_tier": "FREE",
    "created_at": "2024-01-01T00:00:00Z"
  },
  "recovery_codes": ["XXXX-XXXX-XXXX-XXXX", ...] // E2E only
}
```

### POST /auth/login
Authenticate existing user and receive JWT tokens.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response (200):**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGci...",
  "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGci...",
  "token_type": "bearer",
  "expires_in": 900,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "encryption_tier": "UCE",
    "storage_tier": "PAID"
  }
}
```

### POST /auth/refresh
Refresh an expired access token using a refresh token.

**Request Body:**
```json
{
  "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGci..."
}
```

### GET /auth/me
Get current user information.

**Response (200):**
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "encryption_tier": "UCE",
    "storage_tier": "FREE",
    "storage_used": 1048576,
    "storage_limit": 1073741824,
    "created_at": "2024-01-01T00:00:00Z",
    "last_login": "2024-01-01T12:00:00Z"
  }
}
```

## 📝 Entry Management Endpoints

### GET /entries
Retrieve user's diary entries with pagination and filtering.

**Query Parameters:**
- `page` (int): Page number (default: 1)
- `per_page` (int): Items per page (max: 100, default: 20)
- `source` (string): Filter by source ("DIARY", "FACEBOOK", "INSTAGRAM")
- `start_date` (ISO date): Start date filter
- `end_date` (ISO date): End date filter
- `tags` (string): Comma-separated tag names

**Response (200):**
```json
{
  "entries": [
    {
      "id": "uuid",
      "encrypted_content": "base64_encoded_encrypted_content",
      "content_hash": "sha256_hash",
      "source": "DIARY",
      "created_at": "2024-01-01T12:00:00Z",
      "updated_at": "2024-01-01T12:00:00Z",
      "tags": ["personal", "work"],
      "media_count": 2,
      "external_post_id": null
    }
  ],
  "pagination": {
    "page": 1,
    "per_page": 20,
    "total": 150,
    "pages": 8
  }
}
```

### POST /entries
Create a new diary entry.

**Request Body:**
```json
{
  "encrypted_content": "base64_encoded_encrypted_content",
  "content_hash": "sha256_hash",
  "source": "DIARY",
  "tags": ["personal", "work"],
  "mood": "HAPPY", // optional
  "location": "New York, NY", // optional
  "external_post_id": null // for social media imports
}
```

**Response (201):**
```json
{
  "entry": {
    "id": "uuid",
    "encrypted_content": "base64_encoded_encrypted_content",
    "content_hash": "sha256_hash",
    "source": "DIARY",
    "created_at": "2024-01-01T12:00:00Z",
    "updated_at": "2024-01-01T12:00:00Z",
    "tags": ["personal", "work"],
    "media_count": 0
  }
}
```

### GET /entries/{entry_id}
Retrieve a specific diary entry.

**Response (200):**
```json
{
  "entry": {
    "id": "uuid",
    "encrypted_content": "base64_encoded_encrypted_content",
    "content_hash": "sha256_hash",
    "source": "DIARY",
    "created_at": "2024-01-01T12:00:00Z",
    "updated_at": "2024-01-01T12:00:00Z",
    "tags": ["personal", "work"],
    "media": [
      {
        "id": "uuid",
        "mime_type": "image/jpeg",
        "file_size": 1048576,
        "encrypted_s3_key": "encrypted_s3_key",
        "created_at": "2024-01-01T12:00:00Z"
      }
    ],
    "events": [
      {
        "event_type": "CREATED",
        "timestamp": "2024-01-01T12:00:00Z",
        "device_info": "iPhone 14, iOS 16.2"
      }
    ]
  }
}
```

### PUT /entries/{entry_id}
Update an existing diary entry.

**Request Body:**
```json
{
  "encrypted_content": "base64_encoded_updated_content",
  "content_hash": "sha256_hash",
  "tags": ["personal", "work", "update"]
}
```

### DELETE /entries/{entry_id}
Soft delete a diary entry.

**Response (204):** No content

### GET /entries/{entry_id}/history
Get the complete edit history of an entry.

**Response (200):**
```json
{
  "history": [
    {
      "event_type": "CREATED",
      "timestamp": "2024-01-01T12:00:00Z",
      "device_info": "iPhone 14, iOS 16.2",
      "metadata": {}
    },
    {
      "event_type": "EDITED",
      "timestamp": "2024-01-01T13:00:00Z",
      "device_info": "MacBook Pro",
      "metadata": {
        "fields_changed": ["content", "tags"]
      }
    }
  ]
}
```

## 📸 Media Management Endpoints

### POST /media/upload-url
Get a presigned URL for uploading media files to S3.

**Request Body:**
```json
{
  "entry_id": "uuid",
  "mime_type": "image/jpeg",
  "file_size": 1048576,
  "encrypted_file_hash": "sha256_hash"
}
```

**Response (200):**
```json
{
  "media_id": "uuid",
  "upload_url": "https://s3.amazonaws.com/...",
  "expires_in": 3600
}
```

### GET /media/{media_id}/download-url
Get a presigned URL for downloading media files from S3.

**Response (200):**
```json
{
  "download_url": "https://s3.amazonaws.com/...",
  "expires_in": 3600
}
```

### GET /media
List all media files for the authenticated user.

**Query Parameters:**
- `entry_id` (uuid): Filter by entry ID
- `mime_type` (string): Filter by MIME type
- `page` (int): Page number
- `per_page` (int): Items per page

**Response (200):**
```json
{
  "media": [
    {
      "id": "uuid",
      "entry_id": "uuid",
      "mime_type": "image/jpeg",
      "file_size": 1048576,
      "encrypted_s3_key": "encrypted_s3_key",
      "created_at": "2024-01-01T12:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "per_page": 20,
    "total": 50,
    "pages": 3
  }
}
```

## 🔍 Search Endpoints (UCE Only)

### GET /search
Search entries using full-text search.

**Query Parameters:**
- `q` (string): Search query
- `tags` (string): Comma-separated tags to filter by
- `source` (string): Filter by source
- `start_date` (ISO date): Start date filter
- `end_date` (ISO date): End date filter
- `page` (int): Page number
- `per_page` (int): Items per page

**Response (200):**
```json
{
  "results": [
    {
      "entry": {
        "id": "uuid",
        "encrypted_content": "base64_encoded_content",
        "content_hash": "sha256_hash",
        "source": "DIARY",
        "created_at": "2024-01-01T12:00:00Z",
        "tags": ["vacation", "travel"]
      },
      "relevance_score": 0.95,
      "match_highlights": ["vacation", "travel"]
    }
  ],
  "pagination": {
    "page": 1,
    "per_page": 20,
    "total": 25,
    "pages": 2
  },
  "query_info": {
    "query": "vacation travel",
    "execution_time_ms": 45,
    "total_indexed_entries": 500
  }
}
```

### POST /search/rebuild-index
Rebuild the search index for the user's entries.

**Response (202):**
```json
{
  "message": "Index rebuild started",
  "job_id": "uuid"
}
```

## 🌐 Social Media Integration Endpoints

### GET /integrations
List connected social media accounts.

**Response (200):**
```json
{
  "integrations": [
    {
      "id": "uuid",
      "platform": "FACEBOOK",
      "platform_user_id": "facebook_user_id",
      "platform_username": "john_doe",
      "connected_at": "2024-01-01T00:00:00Z",
      "last_sync": "2024-01-01T12:00:00Z",
      "status": "ACTIVE"
    }
  ]
}
```

### GET /integrations/facebook/auth-url
Get Facebook OAuth authorization URL.

**Response (200):**
```json
{
  "auth_url": "https://facebook.com/oauth/authorize?...",
  "state": "random_state_string"
}
```

### POST /integrations/facebook/callback
Complete Facebook OAuth flow.

**Request Body:**
```json
{
  "code": "facebook_oauth_code",
  "state": "random_state_string"
}
```

### POST /integrations/facebook/import
Import posts from Facebook.

**Request Body:**
```json
{
  "start_date": "2023-01-01", // optional
  "end_date": "2024-01-01", // optional
  "post_types": ["text", "photo", "video"] // optional
}
```

**Response (202):**
```json
{
  "message": "Import started",
  "job_id": "uuid"
}
```

### POST /integrations/facebook/share
Share a diary entry to Facebook.

**Request Body:**
```json
{
  "entry_id": "uuid",
  "message": "Additional message for Facebook", // optional
  "privacy": "FRIENDS" // PUBLIC, FRIENDS, ONLY_ME
}
```

## 📊 Error Responses

### Standard Error Format
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request data",
    "details": {
      "field": "email",
      "reason": "Invalid email format"
    }
  }
}
```

### Common Error Codes
- `AUTHENTICATION_REQUIRED` (401): No valid JWT token provided
- `AUTHORIZATION_FAILED` (403): User lacks permission for resource
- `VALIDATION_ERROR` (400): Request data validation failed
- `RESOURCE_NOT_FOUND` (404): Requested resource doesn't exist
- `RATE_LIMIT_EXCEEDED` (429): Too many requests
- `INTERNAL_SERVER_ERROR` (500): Unexpected server error

## 🔐 Encryption Implementation Notes

### E2E Tier
- **Client-side encryption only**: Server never sees plaintext content
- **Public key exchange**: Client uploads public key during registration
- **Content format**: Base64-encoded encrypted content
- **Hash verification**: SHA-256 hash for integrity checking

### UCE Tier
- **Server-side decryption**: Server can decrypt for features like search
- **Master key encryption**: Master key encrypted with password-derived key
- **Content indexing**: Decrypted content indexed for full-text search
- **Feature availability**: AI processing, advanced search, etc.

## 📝 SDK and Client Libraries

### Official SDKs
- **JavaScript/TypeScript**: `@personal-diary/js-sdk`
- **Swift**: `PersonalDiarySDK` (Swift Package Manager)
- **Kotlin**: `personal-diary-android-sdk`
- **Python**: `personal-diary-python` (PyPI)

### Example Usage (JavaScript)
```javascript
import { PersonalDiaryClient } from '@personal-diary/js-sdk';

const client = new PersonalDiaryClient({
  baseUrl: 'https://api.diary.xmojo.net/api/v1',
  encryptionTier: 'UCE'
});

// Login
await client.auth.login('user@example.com', 'password');

// Create entry
const entry = await client.entries.create({
  content: 'My diary entry',
  tags: ['personal', 'today']
});

// Search entries (UCE only)
const results = await client.search.query('vacation');
```

## 🧪 Testing and Development

### Test Environment
- **Base URL**: `https://api-dev.diary.xmojo.net/api/v1`
- **Test accounts**: Available upon request
- **Rate limits**: Relaxed for development

### Webhooks (Coming Soon)
- **Entry created**: Notification when new entry is created
- **Import completed**: Notification when social media import finishes
- **Error notifications**: Alerts for failed operations

---

## 📋 Rate Limits

| Endpoint Category | Requests per Hour | Burst Limit |
|-------------------|-------------------|-------------|
| Authentication | 100 | 10 per minute |
| Entry Operations | 1000 | 50 per minute |
| Media Upload | 200 | 20 per minute |
| Search | 500 | 30 per minute |
| Social Media | 100 | 10 per minute |

## 📞 API Support

- **Developer Documentation**: This wiki
- **Status Page**: status.diary.xmojo.net
- **Developer Support**: developers@diary.xmojo.net
- **GitHub**: Issues and discussions
- **Discord**: #developers channel

---

**For the most up-to-date API documentation, visit the interactive docs at:** `https://api.diary.xmojo.net/docs`