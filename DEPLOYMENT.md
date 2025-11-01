# Personal Diary - Complete Deployment Guide

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Backend Setup](#backend-setup)
4. [Web Application Setup](#web-application-setup)
5. [Android Application Setup](#android-application-setup)
6. [Email Configuration](#email-configuration)
7. [Kubernetes Deployment](#kubernetes-deployment)
8. [Troubleshooting](#troubleshooting)
9. [Testing](#testing)
10. [Next Steps](#next-steps)

---

## Overview

The Personal Diary system is a multi-platform journaling application with end-to-end encryption, supporting:

- **Backend API**: FastAPI-based REST API with PostgreSQL database
- **Web Application**: React TypeScript SPA with Vite
- **Android Application**: Native Kotlin app with Jetpack Compose
- **Security**: End-to-end encryption, OAuth2 + JWT authentication
- **Email**: AWS SES integration for verification and password reset

### Architecture

```
┌─────────────────┐
│  Android App    │
│  (Kotlin)       │
└────────┬────────┘
         │
         ├─────────────────┐
         │                 │
┌────────▼────────┐  ┌────▼──────────┐
│   Web App       │  │   Backend     │
│   (React TS)    │  │   (FastAPI)   │
└────────┬────────┘  └────┬──────────┘
         │                │
         └────────┬───────┘
                  │
         ┌────────▼────────┐
         │   PostgreSQL    │
         └─────────────────┘
```

### Service URLs

- **Production Backend**: https://diary-api.xmojo.net
- **Production Web**: https://diary.xmojo.net
- **Local Backend**: http://localhost:8000
- **Local Web**: http://localhost:5173

---

## Prerequisites

### Required Software

#### Backend Development
- **Python**: 3.11 or higher
- **pip**: Latest version
- **PostgreSQL**: 14 or higher
- **Git**: Latest version

#### Web Development
- **Node.js**: 18.x or higher
- **npm**: 9.x or higher
- **Modern browser**: Chrome, Firefox, Safari, or Edge

#### Android Development
- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 17 or higher
- **Android SDK**: API level 34 (Android 14)
- **Gradle**: 8.2 or higher (bundled with Android Studio)

### System Requirements

- **RAM**: 8GB minimum (16GB recommended for Android development)
- **Disk Space**: 10GB minimum
- **OS**: macOS, Linux, or Windows 10/11

---

## Backend Setup

### 1. Clone Repository

```bash
cd /Users/jaystuart/dev/personal-diary
```

### 2. Create Python Virtual Environment

```bash
cd backend
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

### 4. Configure Environment Variables

Create a `.env` file in the `backend` directory:

```bash
# Database Configuration
DATABASE_URL=postgresql://your_user:your_password@localhost:5432/personal_diary

# Security
SECRET_KEY=your-secret-key-min-32-chars-long-use-openssl-rand-hex-32
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30
REFRESH_TOKEN_EXPIRE_DAYS=7

# Email Configuration (AWS SES)
AWS_REGION=us-east-1
AWS_SES_SENDER_EMAIL=no-reply@xmojo.net
AWS_SES_CONFIGURATION_SET=personal-diary-emails

# AWS Credentials (for local development)
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key

# Application URLs
FRONTEND_URL=http://localhost:5173
API_URL=http://localhost:8000

# CORS Origins (comma-separated)
CORS_ORIGINS=http://localhost:5173,https://diary.xmojo.net
```

### 5. Generate Secret Key

```bash
# Generate a secure secret key
python3 -c "import secrets; print(secrets.token_hex(32))"
```

Copy the output and use it as your `SECRET_KEY`.

### 6. Set Up PostgreSQL Database

```bash
# Create database
createdb personal_diary

# Or using psql
psql -U postgres
CREATE DATABASE personal_diary;
\q
```

### 7. Initialize Database Schema

```bash
# Run migrations (Alembic)
alembic upgrade head

# Or manually create tables
python3 -c "from app.database import Base, engine; Base.metadata.create_all(bind=engine)"
```

### 8. Start Backend Server

```bash
# Development mode with auto-reload
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Production mode
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

### 9. Verify Backend

```bash
# Check health endpoint
curl http://localhost:8000/health

# Expected response:
# {"status":"healthy","database":"connected"}

# Check API docs
open http://localhost:8000/docs
```

---

## Web Application Setup

### 1. Navigate to Web Directory

```bash
cd /Users/jaystuart/dev/personal-diary/web
```

### 2. Install Dependencies

```bash
npm install
```

### 3. Configure Environment Variables

Create a `.env` file in the `web` directory:

```bash
# API Configuration
VITE_API_URL=http://localhost:8000
VITE_API_BASE_URL=http://localhost:8000/api/v1

# Application Configuration
VITE_APP_NAME=Personal Diary
VITE_APP_VERSION=1.0.0

# Production URLs (for deployment)
# VITE_API_URL=https://diary-api.xmojo.net
# VITE_API_BASE_URL=https://diary-api.xmojo.net/api/v1
```

### 4. Start Development Server

```bash
npm run dev
```

The web app will be available at: http://localhost:5173

### 5. Build for Production

```bash
# Create optimized production build
npm run build

# Preview production build locally
npm run preview
```

### 6. Verify Web Application

1. Open http://localhost:5173
2. Click "Sign Up" to create an account
3. Check email for verification link
4. Log in with verified account
5. Create a test diary entry

---

## Android Application Setup

### 1. Open Project in Android Studio

```bash
# Open Android Studio and select:
File > Open > /Users/jaystuart/dev/personal-diary/android
```

### 2. Configure API Endpoint

Edit `android/app/src/main/java/com/jstuart0/personaldiary/data/remote/api/ApiConfig.kt`:

```kotlin
object ApiConfig {
    // For local development
    const val BASE_URL = "http://10.0.2.2:8000/"  // Android emulator
    // const val BASE_URL = "http://192.168.1.XXX:8000/"  // Physical device

    // For production
    // const val BASE_URL = "https://diary-api.xmojo.net/"
}
```

**Important Network Configuration Notes:**
- **Android Emulator**: Use `10.0.2.2` to access `localhost` on your development machine
- **Physical Device**: Use your computer's local IP address (e.g., `192.168.1.100`)
- **Production**: Use the production API URL

### 3. Sync Gradle Dependencies

```bash
# In Android Studio:
File > Sync Project with Gradle Files
```

Or from command line:

```bash
cd android
./gradlew build
```

### 4. Configure Android SDK

1. Open Android Studio Settings: `Preferences > Appearance & Behavior > System Settings > Android SDK`
2. Install required SDK Platform:
   - Android 14.0 (API 34)
3. Install required SDK Tools:
   - Android SDK Build-Tools 34.0.0
   - Android Emulator
   - Android SDK Platform-Tools

### 5. Create Android Virtual Device (AVD)

1. Open AVD Manager: `Tools > Device Manager`
2. Click "Create Device"
3. Select device: Pixel 7 Pro (recommended)
4. Select system image: Android 14 (API 34)
5. Click "Finish"

### 6. Run the Application

**Using Android Studio:**
1. Select your AVD or connected device
2. Click the "Run" button (green play icon)
3. Wait for build and deployment

**Using Command Line:**
```bash
cd android

# List available devices
./gradlew tasks --all | grep install

# Install debug APK on connected device
./gradlew installDebug

# Run on emulator
./gradlew installDebug
adb shell am start -n com.jstuart0.personaldiary/.MainActivity
```

### 7. Build Release APK

```bash
cd android

# Build release APK (unsigned)
./gradlew assembleRelease

# Output: android/app/build/outputs/apk/release/app-release-unsigned.apk
```

### 8. Verify Android Application

1. Launch app on emulator/device
2. Tap "Sign Up"
3. Enter valid email and password
4. Check email for verification link
5. Return to app and log in
6. Create a test entry with encryption

---

## Email Configuration

The application uses AWS SES (Simple Email Service) for sending emails.

### 1. AWS SES Setup

#### Verify Email Address

```bash
# Using AWS CLI
aws ses verify-email-identity \
  --email-address no-reply@xmojo.net \
  --region us-east-1
```

Or via AWS Console:
1. Go to SES Console: https://console.aws.amazon.com/ses/
2. Navigate to "Verified identities"
3. Click "Create identity"
4. Select "Email address"
5. Enter: `no-reply@xmojo.net`
6. Click "Create identity"
7. Check email and click verification link

#### Create Configuration Set

```bash
aws ses create-configuration-set \
  --configuration-set-name personal-diary-emails \
  --region us-east-1
```

### 2. IAM Permissions

Create an IAM user or role with the following policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    }
  ]
}
```

### 3. Test Email Sending

```bash
cd backend
source venv/bin/activate

# Run test script
python3 test_email.py
```

Expected output:
```
✓ Email service initialized
✓ Email sent successfully
Message ID: 0100018c9...
```

### 4. Email Templates

The application sends these types of emails:

1. **Email Verification**: Sent when user signs up
2. **Password Reset**: Sent when user requests password reset
3. **Password Changed**: Notification when password is changed

All templates are in `backend/app/services/email.py`.

### 5. Email Troubleshooting

**Issue**: Emails not being sent

**Solutions**:
1. Check AWS SES sandbox status (need to request production access)
2. Verify sender email address in SES
3. Check AWS credentials in `.env`
4. Review CloudWatch logs for SES errors
5. Check spam folder

**Issue**: Emails marked as spam

**Solutions**:
1. Configure SPF record for your domain
2. Configure DKIM in SES
3. Set up DMARC policy
4. Use verified domain instead of email address

---

## Kubernetes Deployment

### 1. Prerequisites

- Kubernetes cluster (thor cluster at 192.168.10.222:6443)
- kubectl configured with cluster access
- Docker for building images
- Registry access for pushing images

### 2. Build Docker Images

```bash
# Build backend image
cd backend
docker build -t diary-backend:latest .

# Build web image
cd ../web
docker build -t diary-web:latest .

# Tag for registry
docker tag diary-backend:latest your-registry/diary-backend:latest
docker tag diary-web:latest your-registry/diary-web:latest

# Push to registry
docker push your-registry/diary-backend:latest
docker push your-registry/diary-web:latest
```

### 3. Create Namespace

```bash
kubectl create namespace personal-diary
```

### 4. Create Secrets

```bash
# Database credentials
kubectl -n personal-diary create secret generic postgres-credentials \
  --from-literal=host=postgres-01.xmojo.net \
  --from-literal=database=personal_diary \
  --from-literal=user=diary_user \
  --from-literal=password=your-secure-password

# Backend secrets
kubectl -n personal-diary create secret generic backend-secrets \
  --from-literal=secret-key=$(python3 -c "import secrets; print(secrets.token_hex(32))") \
  --from-literal=aws-access-key-id=your-aws-key \
  --from-literal=aws-secret-access-key=your-aws-secret
```

### 5. Deploy to Kubernetes

```bash
# Apply all manifests
kubectl apply -f kubernetes/

# Watch deployment
kubectl -n personal-diary get pods -w

# Check status
kubectl -n personal-diary get all
```

### 6. Configure Ingress

The application is accessible via:
- Backend: https://diary-api.xmojo.net
- Web: https://diary.xmojo.net

Ingress is configured in `kubernetes/web/ingress.yaml`.

### 7. Verify Deployment

```bash
# Check backend health
curl https://diary-api.xmojo.net/health

# Check web app
curl https://diary.xmojo.net

# View logs
kubectl -n personal-diary logs -f deployment/diary-backend
kubectl -n personal-diary logs -f deployment/diary-web
```

---

## Troubleshooting

### Backend Issues

#### Database Connection Errors

**Symptom**: `Could not connect to database`

**Solutions**:
1. Verify PostgreSQL is running: `pg_isready`
2. Check database credentials in `.env`
3. Ensure database exists: `psql -l | grep personal_diary`
4. Check firewall/network connectivity
5. Verify connection string format

#### Email Sending Failures

**Symptom**: `Error sending email` or `SES error`

**Solutions**:
1. Verify AWS credentials: `aws sts get-caller-identity`
2. Check SES sandbox status (production access may be needed)
3. Verify sender email in SES: `aws ses list-identities`
4. Check AWS region in `.env`
5. Review CloudWatch Logs for detailed errors

#### Authentication Errors

**Symptom**: `Invalid token` or `Unauthorized`

**Solutions**:
1. Verify `SECRET_KEY` is set in `.env`
2. Check token expiration settings
3. Clear browser cookies/local storage
4. Regenerate authentication tokens
5. Check system clock synchronization

### Web Application Issues

#### API Connection Errors

**Symptom**: `Network error` or `Failed to fetch`

**Solutions**:
1. Verify backend is running: `curl http://localhost:8000/health`
2. Check `VITE_API_URL` in `.env`
3. Verify CORS settings in backend
4. Check browser console for errors
5. Test API directly with curl/Postman

#### Build Failures

**Symptom**: `npm run build` fails

**Solutions**:
1. Delete `node_modules` and reinstall: `rm -rf node_modules && npm install`
2. Clear Vite cache: `rm -rf .vite`
3. Check Node.js version: `node --version` (should be 18.x+)
4. Update dependencies: `npm update`
5. Check for TypeScript errors: `npm run type-check`

### Android Application Issues

#### Build Errors

**Symptom**: Gradle sync fails or build errors

**Solutions**:
1. Clean and rebuild:
   ```bash
   ./gradlew clean
   ./gradlew build
   ```
2. Invalidate caches: `File > Invalidate Caches / Restart`
3. Check JDK version (must be 17)
4. Update Android Gradle Plugin
5. Sync with Gradle files

#### Network Connection Errors

**Symptom**: `Unable to connect to server` or `SocketTimeoutException`

**Solutions**:
1. **Emulator**: Use `10.0.2.2` instead of `localhost`
2. **Physical Device**: Use computer's local IP (e.g., `192.168.1.100`)
3. Check backend is accessible: `curl http://10.0.2.2:8000/health`
4. Verify network permissions in `AndroidManifest.xml`
5. Check firewall settings on development machine
6. Add `android:usesCleartextTraffic="true"` for HTTP (development only)

#### API Response Errors

**Symptom**: `Unexpected response format` or deserialization errors

**Solutions**:
1. Enable network logging in `ApiClient`
2. Check API contract matches backend
3. Verify JSON parsing with actual API responses
4. Check for nullable fields in data models
5. Test with API documentation: `http://localhost:8000/docs`

#### Authentication Issues

**Symptom**: Login fails or token errors

**Solutions**:
1. Clear app data: `Settings > Apps > Personal Diary > Clear Data`
2. Verify email is verified in database
3. Check password requirements (min 8 chars)
4. Review backend logs for authentication errors
5. Test authentication via API docs

### Common Issues Across Platforms

#### Email Verification Not Received

**Solutions**:
1. Check spam/junk folder
2. Verify email address spelling
3. Check backend logs for email sending
4. Verify AWS SES sender identity
5. Check if in SES sandbox (can only send to verified addresses)

#### Slow Performance

**Solutions**:
1. Check database query performance
2. Add database indexes if needed
3. Enable caching for static content
4. Optimize image sizes
5. Use connection pooling for database

#### Security Errors

**Solutions**:
1. Ensure HTTPS in production
2. Verify CORS origins are correct
3. Check JWT token expiration settings
4. Review Content Security Policy headers
5. Update dependencies for security patches

---

## Testing

### Backend Testing

```bash
cd backend
source venv/bin/activate

# Run all tests
pytest

# Run with coverage
pytest --cov=app tests/

# Run specific test file
pytest tests/test_auth.py

# Run specific test
pytest tests/test_auth.py::test_register_user
```

### Web Application Testing

```bash
cd web

# Run unit tests
npm test

# Run with coverage
npm run test:coverage

# Run E2E tests (if configured)
npm run test:e2e
```

### Android Testing

```bash
cd android

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests com.jstuart0.personaldiary.AuthRepositoryTest
```

### Manual Testing Checklist

#### Registration & Authentication
- [ ] User can register with valid email and password
- [ ] Email verification is sent
- [ ] User can verify email via link
- [ ] User can log in after verification
- [ ] User cannot log in before verification
- [ ] User receives error for invalid credentials
- [ ] Password reset email is sent
- [ ] User can reset password via email link

#### Diary Entries
- [ ] User can create new entry
- [ ] Entry is encrypted before sending to server
- [ ] Entry is decrypted after retrieval
- [ ] User can view entry list
- [ ] User can update existing entry
- [ ] User can delete entry
- [ ] Search returns correct results
- [ ] Pagination works correctly

#### Media Attachments
- [ ] User can upload image
- [ ] Image is displayed in entry
- [ ] User can delete image
- [ ] Multiple images can be attached
- [ ] Image upload progress is shown

#### Security
- [ ] Access token expires after configured time
- [ ] Refresh token extends session
- [ ] Logged out users cannot access protected routes
- [ ] Entries are encrypted end-to-end
- [ ] Master key is securely stored
- [ ] Password meets complexity requirements

---

## Next Steps

### Immediate Actions Required

1. **Production Email Setup**
   - Request AWS SES production access
   - Configure SPF, DKIM, and DMARC for domain
   - Test email delivery to external addresses

2. **Security Hardening**
   - Enable HTTPS for all environments
   - Implement rate limiting on auth endpoints
   - Add CAPTCHA to registration form
   - Set up Web Application Firewall (WAF)

3. **Monitoring & Logging**
   - Deploy Grafana/Prometheus for metrics
   - Configure log aggregation (ELK/Loki)
   - Set up alerting for errors
   - Create dashboards for key metrics

4. **Testing Expansion**
   - Add integration tests for all endpoints
   - Implement E2E tests for critical user flows
   - Add performance/load testing
   - Set up automated testing pipeline

### Feature Enhancements

1. **Functionality**
   - Add support for voice notes
   - Implement entry templates
   - Add mood tracking
   - Support markdown formatting
   - Add export functionality (PDF, JSON)

2. **User Experience**
   - Implement offline mode
   - Add dark mode
   - Support multiple languages (i18n)
   - Add entry reminders/notifications
   - Implement search suggestions

3. **Performance**
   - Add Redis caching layer
   - Implement CDN for media files
   - Optimize database queries
   - Add lazy loading for entries
   - Implement background sync

4. **Platform Expansion**
   - Create iOS application
   - Add desktop application (Electron)
   - Implement browser extension
   - Add API for third-party integrations

### Infrastructure Improvements

1. **Kubernetes**
   - Set up Horizontal Pod Autoscaler
   - Implement health checks and liveness probes
   - Configure resource limits and requests
   - Set up backup strategy for database
   - Implement blue-green deployment

2. **CI/CD**
   - Create GitHub Actions workflow
   - Automate testing on PR
   - Automate Docker image builds
   - Implement automated deployment
   - Add semantic versioning

3. **Database**
   - Set up read replicas for scaling
   - Implement automated backups
   - Add database migration strategy
   - Configure connection pooling
   - Add database monitoring

### Documentation

1. **API Documentation**
   - Expand OpenAPI/Swagger docs
   - Add example requests/responses
   - Document error codes
   - Create Postman collection
   - Add authentication guide

2. **User Documentation**
   - Create user guide
   - Add FAQ section
   - Create video tutorials
   - Document privacy policy
   - Add terms of service

3. **Developer Documentation**
   - Create architecture diagrams
   - Document code standards
   - Add contribution guide
   - Document deployment process
   - Create troubleshooting guide

---

## Additional Resources

### Documentation
- **Backend API Docs**: http://localhost:8000/docs (Swagger UI)
- **Backend ReDoc**: http://localhost:8000/redoc
- **Wiki Documentation**: https://wiki.xmojo.net (search for "Personal Diary")

### Related Files
- **Authentication Report**: [AUTHENTICATION_TEST_REPORT.md](AUTHENTICATION_TEST_REPORT.md)
- **Comprehensive Test Report**: [COMPREHENSIVE_TEST_REPORT.md](COMPREHENSIVE_TEST_REPORT.md)
- **Final Test Summary**: [FINAL_TEST_SUMMARY.md](FINAL_TEST_SUMMARY.md)
- **Issue Fixes**: [ISSUE_FIXES.md](ISSUE_FIXES.md)
- **Email Setup Guide**: [backend/EMAIL_SETUP.md](backend/EMAIL_SETUP.md)
- **Email API Reference**: [backend/EMAIL_API_QUICK_REFERENCE.md](backend/EMAIL_API_QUICK_REFERENCE.md)

### External Resources
- **FastAPI Documentation**: https://fastapi.tiangolo.com/
- **React Documentation**: https://react.dev/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/
- **AWS SES Documentation**: https://docs.aws.amazon.com/ses/

---

**Last Updated**: 2025-01-01
**Version**: 1.0.0
**Maintained By**: Jay Stuart
