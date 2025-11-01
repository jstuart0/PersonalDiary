# Email API Quick Reference

## Overview

All email-related endpoints are under `/api/v1/auth` and use AWS SES with the AGILE profile to send from `no-reply@xmojo.net`.

## Endpoints

### 1. POST /api/v1/auth/signup

**Purpose**: Create new user account

**Emails Sent**:
- Email verification email (with 24-hour token)
- Welcome email (with encryption tier info)

**Request**:
```bash
curl -X POST http://localhost:3001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123",
    "encryption_tier": "UCE"
  }'
```

**Response**:
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "encryptionTier": "uce",
    "createdAt": "2025-11-01T12:00:00Z",
    "updatedAt": "2025-11-01T12:00:00Z",
    "encryptedMasterKey": "base64-key",
    "keyDerivationSalt": "base64-salt"
  },
  "tokens": {
    "accessToken": "jwt-token",
    "refreshToken": "jwt-token",
    "expiresIn": 900
  }
}
```

---

### 2. POST /api/v1/auth/verify-email

**Purpose**: Verify email address with token from email

**Request**:
```bash
curl -X POST http://localhost:3001/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "verification-token-from-email"
  }'
```

**Response**:
```json
{
  "message": "Email verified successfully",
  "email": "user@example.com",
  "verified": true
}
```

**Errors**:
- `400`: Invalid or expired token
- `500`: Verification failed

---

### 3. POST /api/v1/auth/resend-verification

**Purpose**: Resend verification email to authenticated user

**Authentication**: Required (Bearer token)

**Request**:
```bash
curl -X POST http://localhost:3001/api/v1/auth/resend-verification \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response**:
```json
{
  "message": "Verification email sent successfully"
}
```

**Errors**:
- `400`: Email already verified
- `401`: Not authenticated
- `500`: Failed to send email

---

### 4. POST /api/v1/auth/forgot-password

**Purpose**: Request password reset email

**Security**: Always returns success to prevent email enumeration

**Request**:
```bash
curl -X POST http://localhost:3001/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

**Response** (always success):
```json
{
  "message": "If an account exists with this email, a password reset link has been sent"
}
```

**Email Sent** (if user exists):
- Password reset email with 1-hour token

---

### 5. POST /api/v1/auth/reset-password

**Purpose**: Reset password using token from email

**Request**:
```bash
curl -X POST http://localhost:3001/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "reset-token-from-email",
    "new_password": "newSecurePassword123"
  }'
```

**Response**:
```json
{
  "message": "Password reset successfully",
  "email": "user@example.com"
}
```

**Errors**:
- `400`: Invalid/expired token or password too short (< 8 chars)
- `500`: Reset failed

---

## Email Templates

### Verification Email

**Subject**: "Verify Your Email - Personal Diary"

**Content**:
- Greeting
- Verification link: `https://diary.xmojo.net/verify-email?token=<token>`
- Manual verification code
- 24-hour expiration notice

### Password Reset Email

**Subject**: "Password Reset - Personal Diary"

**Content**:
- Greeting
- Reset link: `https://diary.xmojo.net/reset-password?token=<token>`
- Security warning
- 1-hour expiration notice

### Welcome Email

**Subject**: "Welcome to Personal Diary! 📔"

**Content**:
- Greeting
- Encryption tier information (E2E or UCE)
- Getting started guide
- Feature highlights
- Link to start writing

---

## Testing

### Send Test Email

```bash
# Basic test
python test_email.py your-email@example.com

# Test all email types
python test_email.py your-email@example.com --all
```

### Test Signup Flow

```bash
# 1. Create account
curl -X POST http://localhost:3001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "encryption_tier": "UCE"
  }' | jq '.'

# 2. Check email for verification link
# 3. Verify email (extract token from email)
curl -X POST http://localhost:3001/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "TOKEN_FROM_EMAIL"
  }' | jq '.'
```

### Test Password Reset Flow

```bash
# 1. Request password reset
curl -X POST http://localhost:3001/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }' | jq '.'

# 2. Check email for reset link
# 3. Reset password (extract token from email)
curl -X POST http://localhost:3001/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "TOKEN_FROM_EMAIL",
    "new_password": "newPassword123"
  }' | jq '.'
```

---

## Configuration

### Environment Variables

```bash
# AWS SES
AWS_PROFILE=AGILE
AWS_SES_REGION=us-east-1
AWS_SES_FROM_EMAIL=no-reply@xmojo.net
AWS_SES_FROM_NAME=Personal Diary
```

### Frontend URLs

Update these in `/Users/jaystuart/dev/personal-diary/backend/app/services/email.py`:

```python
self.base_url = "https://diary.xmojo.net"  # Production
# self.base_url = "http://localhost:3000"  # Development
```

---

## Error Handling

### Common Issues

**Email Not Received**:
1. Check spam/junk folder
2. Verify email is in AWS SES verified identities
3. Check application logs for errors
4. Test with `python test_email.py`

**Invalid Token**:
- Tokens expire (24h verification, 1h reset)
- Tokens are single-use only
- Request a new token if expired

**SES Sandbox Mode**:
- Can only send to verified email addresses
- Verify recipient email in AWS SES Console
- Request production access for unrestricted sending

---

## Security Notes

1. **Tokens**:
   - Generated with `secrets.token_urlsafe(32)`
   - Stored hashed in database
   - Single-use (marked as used after verification)
   - Short expiration times

2. **Email Enumeration Prevention**:
   - Forgot password always returns success
   - No user existence information disclosed

3. **Rate Limiting**:
   - Auth endpoints have rate limits
   - Prevents email spam abuse

---

## Support

For issues:
1. Check logs: `tail -f logs/app.log`
2. Test emails: `python test_email.py`
3. Verify SES: `AWS_PROFILE=AGILE aws ses get-send-quota --region us-east-1`
4. Check AWS Console SES dashboard
