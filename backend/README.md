# Personal Diary Platform - Backend API

**Version:** 0.1.0
**Framework:** FastAPI (Python 3.11+)
**Database:** PostgreSQL 15+
**Architecture:** Async microservices with dual-tier encryption

---

## 🎯 Overview

This is the backend API for the Personal Diary Platform, a privacy-first diary application with unique dual-tier encryption support. Users choose between:

- **E2E (End-to-End Encrypted):** Maximum privacy, keys never on server
- **UCE (User-Controlled Encryption):** Encrypted at rest with server-side features

---

## 🏗️ Architecture

### Core Components

```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py                 # FastAPI application
│   ├── config.py               # Configuration management
│   ├── database.py             # Database connection & session
│   ├── models/                 # SQLAlchemy models
│   │   ├── __init__.py
│   │   ├── user.py             # User model with encryption tier
│   │   ├── entry.py            # Entry, Tag, EntryEvent models
│   │   ├── media.py            # Media model
│   │   ├── integration.py      # Social media integration models
│   │   └── e2e.py              # E2E-specific models
│   ├── schemas/                # Pydantic validation schemas
│   │   ├── __init__.py
│   │   ├── user.py             # User request/response schemas
│   │   ├── entry.py            # Entry schemas (to be created)
│   │   ├── media.py            # Media schemas (to be created)
│   │   ├── search.py           # Search schemas (to be created)
│   │   └── integration.py      # Integration schemas (to be created)
│   ├── services/               # Business logic (to be created)
│   │   ├── encryption/         # Encryption services
│   │   ├── auth.py             # Authentication service
│   │   ├── entry.py            # Entry management
│   │   ├── media.py            # Media storage
│   │   ├── search.py           # Search implementation
│   │   └── integration.py      # Social media integration
│   ├── routers/                # API route handlers (to be created)
│   │   ├── auth.py             # Authentication endpoints
│   │   ├── entries.py          # Entry CRUD endpoints
│   │   ├── media.py            # Media upload/download
│   │   ├── search.py           # Search endpoints
│   │   └── integrations.py     # Facebook OAuth & sync
│   ├── workers/                # Celery background tasks (to be created)
│   │   └── facebook.py         # Facebook import worker
│   └── utils/                  # Utility functions (to be created)
│       ├── security.py         # JWT, rate limiting
│       └── validators.py       # Custom validators
├── migrations/                 # Alembic database migrations (to be created)
├── tests/                      # Test suites (to be created)
│   ├── unit/
│   ├── integration/
│   └── security/
├── requirements.txt            # Python dependencies
├── Dockerfile                  # Container configuration
├── .env.example                # Environment variables template
└── README.md                   # This file
```

---

## 🚀 Quick Start

### Prerequisites

- Python 3.11+
- PostgreSQL 15+
- Redis 7+
- AWS S3 or compatible storage

### Local Development Setup

1. **Clone repository and navigate to backend:**
   ```bash
   cd backend
   ```

2. **Create virtual environment:**
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

4. **Configure environment:**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

5. **Run database migrations:**
   ```bash
   alembic upgrade head
   ```

6. **Start development server:**
   ```bash
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```

7. **Access API documentation:**
   - Swagger UI: http://localhost:8000/api/v1/docs
   - ReDoc: http://localhost:8000/api/v1/redoc
   - Health check: http://localhost:8000/health

---

## 📊 Database Models

### User Model
- **Purpose:** Store user accounts with encryption tier selection
- **Key Fields:**
  - `encryption_tier`: E2E or UCE (IMMUTABLE after creation)
  - `e2e_public_key`: Public key for E2E users
  - `uce_encrypted_master_key`: Encrypted master key for UCE users
  - `uce_key_derivation_salt`: Salt for password-based key derivation

### Entry Model
- **Purpose:** Store encrypted diary entries
- **Encryption:** Content encrypted before database storage
- **Search:** TSVECTOR for UCE users, metadata-only for E2E
- **Features:** Tags, media attachments, version history, soft delete

### Media Model
- **Purpose:** Track encrypted media files (photos, videos)
- **Storage:** Files stored in S3, references in database
- **Quota:** Enforced per-user storage limits

### Integration Models
- **IntegrationAccount:** OAuth tokens for social platforms
- **ExternalPost:** Maps local entries to social media posts
- **Deduplication:** Uses external_post_id and content_hash

### E2E Models
- **E2EPublicKey:** Public key storage (private never on server)
- **E2ERecoveryCode:** Recovery codes (hashed with SHA-256)

---

## 🔐 Encryption Architecture

### E2E Tier (End-to-End Encrypted)

**Client-Side:**
- Key generation: X25519 keypair
- Encryption: ChaCha20-Poly1305
- Private key storage: Keychain (iOS) / KeyStore (Android) / IndexedDB (Web)
- Recovery: 10 recovery codes generated at signup

**Server-Side:**
- Stores: Public key, encrypted content, recovery code hashes
- Cannot decrypt: User content remains private
- Limitations: No server-side search, no AI features

### UCE Tier (User-Controlled Encryption)

**Key Derivation:**
- Algorithm: Argon2id
- Parameters: 2 iterations, 64MB memory
- Master key: Generated by server, encrypted with derived key

**Encryption:**
- Algorithm: AES-256-GCM
- Content encrypted before database storage
- Server can decrypt with user password

**Features:**
- Server-side search (PostgreSQL FTS)
- AI analysis (future)
- Easy password recovery

---

## 🔌 API Endpoints

### Authentication (`/api/v1/auth`)

- `POST /signup` - Create new account with tier selection
- `POST /login` - Authenticate and get JWT token
- `POST /refresh` - Refresh JWT token
- `POST /logout` - Invalidate token

### Entries (`/api/v1/entries`)

- `GET /entries` - List user's entries (paginated, filtered)
- `POST /entries` - Create new entry (encrypted)
- `GET /entries/{id}` - Get single entry
- `PUT /entries/{id}` - Update entry (creates event)
- `DELETE /entries/{id}` - Soft delete entry

### Media (`/api/v1/media`)

- `POST /media` - Upload encrypted media file
- `GET /media/{id}` - Download or get pre-signed URL
- `DELETE /media/{id}` - Delete media file

### Search (`/api/v1/search`)

- `GET /search` - Search entries
  - UCE: Full-text search on decrypted content
  - E2E: Metadata-only (tags, dates)

### Social Integration (`/api/v1/integrations/facebook`)

- `POST /connect` - Initiate Facebook OAuth
- `POST /callback` - Complete OAuth and store tokens
- `POST /push` - Push entry to Facebook
- `POST /pull` - Import posts from Facebook (async job)
- `GET /pull/status/{job_id}` - Check import job status

### User Features (`/api/v1/me`)

- `GET /me/features` - Get feature availability and storage quota

---

## 🧪 Testing

### Run Tests

```bash
# All tests
pytest

# With coverage
pytest --cov=app --cov-report=html

# Specific test file
pytest tests/unit/test_encryption.py

# Integration tests only
pytest tests/integration/
```

### Test Coverage Target

- **Minimum:** 80% overall coverage
- **Critical:** 100% coverage for encryption services
- **Security:** Dedicated security test suite

---

## 🐳 Docker Deployment

### Build Image

```bash
docker build -t personal-diary-backend:latest .
```

### Run Container

```bash
docker run -d \
  --name personal-diary-api \
  -p 8000:8000 \
  --env-file .env \
  personal-diary-backend:latest
```

---

## ☸️ Kubernetes Deployment

### Deploy to thor Cluster

```bash
# Switch to thor context
kubectl config use-context thor

# Apply namespace (already exists)
kubectl apply -f ../kubernetes/namespace.yaml

# Create secrets
kubectl create secret generic backend-secrets \
  -n personal-diary \
  --from-env-file=.env

# Deploy application (manifests to be created)
kubectl apply -f ../kubernetes/backend/

# Check status
kubectl -n personal-diary get pods
kubectl -n personal-diary logs -f deployment/backend-api
```

---

## 🔒 Security Considerations

### Implemented

- ✅ Dual-tier encryption architecture
- ✅ Async password hashing (Argon2id)
- ✅ UUID primary keys (no sequential IDs)
- ✅ CORS configuration
- ✅ Input validation (Pydantic)
- ✅ SQL injection prevention (parameterized queries)

### To Be Implemented

- ⏳ JWT token rotation
- ⏳ Rate limiting per endpoint
- ⏳ Request logging (sanitized, no sensitive data)
- ⏳ HTTPS enforcement in production
- ⏳ Security headers (CSP, X-Frame-Options, etc.)
- ⏳ Secrets management (Kubernetes secrets)

---

## 📈 Performance Targets

- **API Response Time:** < 200ms (95th percentile)
- **Encryption/Decryption:** < 50ms per entry
- **Search Queries:** < 200ms (UCE tier)
- **File Upload:** Support up to 50MB files
- **Concurrent Requests:** Handle 100+ concurrent users

---

## 🛠️ Development Tools

### Code Quality

```bash
# Format code
black app/
isort app/

# Linting
flake8 app/

# Type checking
mypy app/
```

### Database Migrations

```bash
# Create new migration
alembic revision --autogenerate -m "description"

# Apply migrations
alembic upgrade head

# Rollback
alembic downgrade -1
```

---

## 📚 Additional Documentation

- **API Specification:** See Swagger UI at `/api/v1/docs`
- **Project Specs:** `/docs/01-project-specification.md`
- **Implementation Guide:** `/docs/02-ai-agent-implementation-guide.md`
- **Search Implementation:** `/docs/06-search-implementation.md`
- **Decisions Log:** `/DECISIONS.md`
- **Implementation Status:** `/backend/IMPLEMENTATION_STATUS.md`

---

## 🤝 Contributing

This backend is being built by an AI agent following the specifications in the `/docs` directory. All code follows:

- Security-first principles
- Clean architecture patterns
- Comprehensive error handling
- Type hints and validation
- Detailed docstrings
- Test-driven development

---

## 📝 License

[To be determined]

---

## 🎯 Current Status

**Phase:** Week 1 - Foundation & Core Authentication
**Progress:** 25% Complete

**Completed:**
- ✅ Project structure and configuration
- ✅ Database models (all 9 models)
- ✅ Pydantic schemas (user schemas)
- ✅ Docker configuration
- ✅ Main FastAPI application

**Next:**
- ⏳ Complete remaining Pydantic schemas
- ⏳ Implement encryption services
- ⏳ Build authentication system
- ⏳ Create database migrations

**See `/backend/IMPLEMENTATION_STATUS.md` for detailed progress tracking.**
