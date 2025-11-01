# AWS SES Email Configuration - Personal Diary Backend

## Overview

This document describes the AWS SES email configuration for the Personal Diary backend. All emails are sent from `no-reply@xmojo.net` using the AGILE AWS profile.

## Configuration

### Environment Variables

The following environment variables are configured in `.env`:

```bash
# AWS SES Configuration
AWS_PROFILE=AGILE
AWS_SES_REGION=us-east-1
AWS_SES_FROM_EMAIL=no-reply@xmojo.net
AWS_SES_FROM_NAME=Personal Diary
```

### AWS SES Setup

- **Sender Email**: `no-reply@xmojo.net`
- **Verified Domain**: `xmojo.net` (verified in AWS SES)
- **AWS Region**: `us-east-1`
- **AWS Profile**: AGILE
- **Send Quota**: 50,000 emails per 24 hours
- **Send Rate**: 14 emails per second

## Email Types

### 1. Email Verification

Sent when a user signs up to verify their email address.

**Trigger**: Automatically sent after signup
**Expiration**: 24 hours
**Template**: `send_verification_email()`

**Features**:
- Verification link to frontend
- Verification code (can be entered manually)
- Professional HTML design with branding

**API Endpoints**:
- `POST /api/v1/auth/verify-email` - Verify email with token
- `POST /api/v1/auth/resend-verification` - Resend verification email

### 2. Password Reset

Sent when a user requests a password reset.

**Trigger**: `POST /api/v1/auth/forgot-password`
**Expiration**: 1 hour
**Template**: `send_password_reset_email()`

**Features**:
- Password reset link to frontend
- Security warning if user didn't request reset
- Single-use token (invalidated after use)

**API Endpoints**:
- `POST /api/v1/auth/forgot-password` - Request password reset
- `POST /api/v1/auth/reset-password` - Reset password with token

### 3. Welcome Email

Sent after successful signup to welcome new users.

**Trigger**: Automatically sent after signup
**Template**: `send_welcome_email()`

**Features**:
- Different messages for E2E vs UCE encryption tiers
- Getting started guide
- Links to documentation and features
- Professional branding

### 4. Test Email

Used to verify SES configuration.

**Template**: `send_test_email()`

**Usage**:
```bash
python test_email.py your-email@example.com
python test_email.py your-email@example.com --all
```

## Email Service Architecture

### EmailService Class

Location: `/Users/jaystuart/dev/personal-diary/backend/app/services/email.py`

**Key Methods**:

```python
class EmailService:
    async def send_email(to_email, subject, html_body, text_body)
    async def send_verification_email(to_email, verification_token, user_name)
    async def send_password_reset_email(to_email, reset_token, user_name)
    async def send_welcome_email(to_email, user_name, encryption_tier)
    async def send_test_email(to_email)
```

**Features**:
- Uses boto3 with AGILE profile
- Supports HTML and plain text versions
- Professional email templates with CSS styling
- Error handling and logging
- Async/await support

### Token Management

Location: `/Users/jaystuart/dev/personal-diary/backend/app/models/token.py`

**Token Model**:
```python
class Token(Base):
    id: UUID
    token: str  # Secure random token
    token_type: TokenType  # EMAIL_VERIFICATION or PASSWORD_RESET
    user_id: UUID
    is_used: bool
    expires_at: DateTime
    created_at: DateTime
    used_at: DateTime
```

**Token Types**:
- `EMAIL_VERIFICATION`: 24-hour expiration
- `PASSWORD_RESET`: 1-hour expiration

**Properties**:
- `is_expired`: Check if token has expired
- `is_valid`: Check if token is valid (not used and not expired)

### AuthService Integration

Location: `/Users/jaystuart/dev/personal-diary/backend/app/services/auth.py`

**Email-related Methods**:
```python
async def create_verification_token(db, user_id)
async def verify_email_token(db, token_value)
async def create_password_reset_token(db, user_id)
async def verify_reset_token(db, token_value)
async def reset_password(db, token_value, new_password)
async def send_verification_email(db, user)
async def send_password_reset_email(db, user)
```

## API Endpoints

### Authentication Endpoints with Email

#### POST /api/v1/auth/signup
Creates new user account and sends verification + welcome emails.

**Request**:
```json
{
  "email": "user@example.com",
  "password": "secure-password",
  "encryption_tier": "E2E",
  "public_key": "base64-encoded-key"
}
```

**Response**: User object with tokens

**Emails Sent**:
1. Email verification email
2. Welcome email

#### POST /api/v1/auth/verify-email
Verifies user email address.

**Request**:
```json
{
  "token": "verification-token-from-email"
}
```

**Response**:
```json
{
  "message": "Email verified successfully",
  "email": "user@example.com",
  "verified": true
}
```

#### POST /api/v1/auth/resend-verification
Resends verification email to authenticated user.

**Headers**: `Authorization: Bearer <access-token>`

**Response**:
```json
{
  "message": "Verification email sent successfully"
}
```

#### POST /api/v1/auth/forgot-password
Requests password reset email.

**Request**:
```json
{
  "email": "user@example.com"
}
```

**Response**:
```json
{
  "message": "If an account exists with this email, a password reset link has been sent"
}
```

**Note**: Always returns success to prevent email enumeration attacks.

#### POST /api/v1/auth/reset-password
Resets password using reset token.

**Request**:
```json
{
  "token": "reset-token-from-email",
  "new_password": "new-secure-password"
}
```

**Response**:
```json
{
  "message": "Password reset successfully",
  "email": "user@example.com"
}
```

## Testing

### Test Email Sending

```bash
# Basic test email
python test_email.py jstuart0@gmail.com

# Test all email types
python test_email.py jstuart0@gmail.com --all
```

### Verify SES Configuration

```bash
# Check SES send quota
AWS_PROFILE=AGILE aws ses get-send-quota --region us-east-1

# List verified identities
AWS_PROFILE=AGILE aws ses list-identities --region us-east-1

# Check verification status
AWS_PROFILE=AGILE aws ses get-identity-verification-attributes \
  --identities no-reply@xmojo.net xmojo.net --region us-east-1
```

## Email Templates

All email templates include:

1. **Professional HTML Design**:
   - Responsive layout (max-width: 600px)
   - Personal Diary branding colors (#4F46E5)
   - Clear call-to-action buttons
   - Informational boxes for security/encryption info

2. **Plain Text Fallback**:
   - All emails include plain text versions
   - Maintains readability for email clients without HTML support

3. **Security Features**:
   - Unsubscribe notice (automated message)
   - Security warnings on password reset emails
   - Clear expiration times on tokens

4. **Branding**:
   - Consistent "Personal Diary" branding
   - Professional footer with copyright
   - Links to frontend application

## Frontend Integration

The frontend should handle these email-related URLs:

- **Email Verification**: `https://diary.xmojo.net/verify-email?token=<token>`
- **Password Reset**: `https://diary.xmojo.net/reset-password?token=<token>`

These URLs should:
1. Extract the token from query parameters
2. Call the appropriate API endpoint
3. Show success/error messages to the user

## Security Considerations

1. **Token Security**:
   - Tokens are generated using `secrets.token_urlsafe(32)`
   - Single-use tokens (marked as used after verification)
   - Short expiration times (1 hour for password reset, 24 hours for verification)

2. **Email Enumeration Prevention**:
   - Forgot password endpoint always returns success
   - No information disclosure about account existence

3. **Rate Limiting**:
   - Authentication endpoints have rate limiting configured
   - Prevents abuse of email sending functionality

4. **AWS SES Security**:
   - Uses AWS profile (AGILE) with appropriate IAM permissions
   - Region-specific configuration
   - Verified domain prevents spoofing

## Troubleshooting

### Email Not Received

1. **Check spam/junk folder**
2. **Verify email address is in SES verified identities**:
   ```bash
   AWS_PROFILE=AGILE aws ses list-identities --region us-east-1
   ```
3. **Check AWS SES sending limits**:
   ```bash
   AWS_PROFILE=AGILE aws ses get-send-quota --region us-east-1
   ```
4. **Review application logs** for email sending errors

### SES Errors

Common errors and solutions:

- **MessageRejected**: Email address not verified in SES (sandbox mode)
- **AccessDenied**: Check AWS profile credentials and IAM permissions
- **Throttling**: Exceeded send rate limit (14 emails/second)

### Testing in Sandbox Mode

If SES is in sandbox mode:
- Can only send to verified email addresses
- Must verify each recipient email individually
- Request production access through AWS Console

## Production Checklist

- [ ] Move SES out of sandbox mode (if applicable)
- [ ] Verify domain in SES (xmojo.net - ✅ DONE)
- [ ] Configure SPF/DKIM/DMARC records for domain
- [ ] Set up CloudWatch monitoring for SES metrics
- [ ] Configure bounce and complaint handling
- [ ] Test all email types in production
- [ ] Update frontend URLs in email templates
- [ ] Configure email sending limits and alerts
- [ ] Review and update email content
- [ ] Test email deliverability across providers (Gmail, Outlook, etc.)

## Monitoring

### Key Metrics to Monitor

1. **Send Rate**: Current send rate vs. limit (14/second)
2. **Bounce Rate**: Should be < 5%
3. **Complaint Rate**: Should be < 0.1%
4. **Delivery Rate**: Should be > 95%

### CloudWatch Metrics

Monitor these CloudWatch metrics:
- `AWS/SES/Send`
- `AWS/SES/Bounce`
- `AWS/SES/Complaint`
- `AWS/SES/Reject`

## Support

For issues with email functionality:

1. Check application logs: `LOG_LEVEL=DEBUG` in `.env`
2. Review AWS CloudWatch logs for SES
3. Test email sending with `test_email.py`
4. Verify AWS credentials and permissions
5. Check SES reputation dashboard in AWS Console

## References

- [AWS SES Documentation](https://docs.aws.amazon.com/ses/)
- [boto3 SES Documentation](https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/ses.html)
- [Email Best Practices](https://docs.aws.amazon.com/ses/latest/dg/best-practices.html)
