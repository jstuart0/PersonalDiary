"""Email service for sending emails via AWS SES"""

import logging
from typing import Optional, Dict, Any
from datetime import datetime, timedelta

import boto3
from botocore.exceptions import ClientError

from app.config import settings

logger = logging.getLogger(__name__)


class EmailService:
    """
    Email service using AWS SES for transactional emails.
    Uses AGILE AWS profile for authentication.
    """

    def __init__(self):
        """Initialize SES client with AGILE profile"""
        # Use boto3 session with profile
        session = boto3.Session(profile_name=settings.aws_profile)
        self.ses_client = session.client(
            "ses",
            region_name=settings.aws_ses_region
        )
        self.from_email = settings.aws_ses_from_email
        self.from_name = settings.aws_ses_from_name
        self.base_url = "https://diary.xmojo.net"  # Frontend URL for links

    def _get_sender(self) -> str:
        """Get formatted sender email address"""
        return f"{self.from_name} <{self.from_email}>"

    async def send_email(
        self,
        to_email: str,
        subject: str,
        html_body: str,
        text_body: Optional[str] = None
    ) -> bool:
        """
        Send email via AWS SES.

        Args:
            to_email: Recipient email address
            subject: Email subject
            html_body: HTML email body
            text_body: Plain text email body (optional)

        Returns:
            True if email sent successfully, False otherwise
        """
        try:
            # Build email message
            message = {
                "Subject": {"Data": subject, "Charset": "UTF-8"},
                "Body": {
                    "Html": {"Data": html_body, "Charset": "UTF-8"}
                }
            }

            # Add text body if provided
            if text_body:
                message["Body"]["Text"] = {"Data": text_body, "Charset": "UTF-8"}

            # Send email
            response = self.ses_client.send_email(
                Source=self._get_sender(),
                Destination={"ToAddresses": [to_email]},
                Message=message
            )

            logger.info(f"Email sent to {to_email}: {response['MessageId']}")
            return True

        except ClientError as e:
            error_code = e.response['Error']['Code']
            error_message = e.response['Error']['Message']
            logger.error(f"SES error sending email to {to_email}: {error_code} - {error_message}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error sending email to {to_email}: {str(e)}")
            return False

    async def send_verification_email(
        self,
        to_email: str,
        verification_token: str,
        user_name: Optional[str] = None
    ) -> bool:
        """
        Send email verification email.

        Args:
            to_email: User's email address
            verification_token: Verification token/code
            user_name: User's name (optional)

        Returns:
            True if email sent successfully
        """
        verification_link = f"{self.base_url}/verify-email?token={verification_token}"

        greeting = f"Hi {user_name}" if user_name else "Hello"

        html_body = f"""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {{ font-family: Arial, sans-serif; line-height: 1.6; color: #333; }}
        .container {{ max-width: 600px; margin: 0 auto; padding: 20px; }}
        .header {{ background-color: #4F46E5; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }}
        .content {{ background-color: #f9fafb; padding: 30px; border-radius: 0 0 5px 5px; }}
        .button {{ display: inline-block; background-color: #4F46E5; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }}
        .footer {{ text-align: center; margin-top: 20px; font-size: 12px; color: #6b7280; }}
        .code {{ font-family: monospace; font-size: 24px; letter-spacing: 3px; background-color: #e5e7eb; padding: 15px; border-radius: 5px; text-align: center; margin: 20px 0; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Verify Your Email</h1>
        </div>
        <div class="content">
            <p>{greeting},</p>
            <p>Thank you for signing up for Personal Diary! To complete your registration, please verify your email address.</p>

            <p style="text-align: center;">
                <a href="{verification_link}" class="button">Verify Email Address</a>
            </p>

            <p>Or copy and paste this link into your browser:</p>
            <p style="word-break: break-all; color: #4F46E5;">{verification_link}</p>

            <p>Alternatively, you can use this verification code:</p>
            <div class="code">{verification_token}</div>

            <p>This link will expire in 24 hours.</p>

            <p>If you didn't create an account with Personal Diary, you can safely ignore this email.</p>
        </div>
        <div class="footer">
            <p>© {datetime.now().year} Personal Diary. All rights reserved.</p>
            <p>This is an automated message, please do not reply.</p>
        </div>
    </div>
</body>
</html>
"""

        text_body = f"""
{greeting},

Thank you for signing up for Personal Diary! To complete your registration, please verify your email address.

Verification Link:
{verification_link}

Or use this verification code: {verification_token}

This link will expire in 24 hours.

If you didn't create an account with Personal Diary, you can safely ignore this email.

© {datetime.now().year} Personal Diary. All rights reserved.
This is an automated message, please do not reply.
"""

        return await self.send_email(
            to_email=to_email,
            subject="Verify Your Email - Personal Diary",
            html_body=html_body,
            text_body=text_body
        )

    async def send_password_reset_email(
        self,
        to_email: str,
        reset_token: str,
        user_name: Optional[str] = None
    ) -> bool:
        """
        Send password reset email.

        Args:
            to_email: User's email address
            reset_token: Password reset token
            user_name: User's name (optional)

        Returns:
            True if email sent successfully
        """
        reset_link = f"{self.base_url}/reset-password?token={reset_token}"

        greeting = f"Hi {user_name}" if user_name else "Hello"

        html_body = f"""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {{ font-family: Arial, sans-serif; line-height: 1.6; color: #333; }}
        .container {{ max-width: 600px; margin: 0 auto; padding: 20px; }}
        .header {{ background-color: #4F46E5; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }}
        .content {{ background-color: #f9fafb; padding: 30px; border-radius: 0 0 5px 5px; }}
        .button {{ display: inline-block; background-color: #4F46E5; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }}
        .footer {{ text-align: center; margin-top: 20px; font-size: 12px; color: #6b7280; }}
        .warning {{ background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Password Reset Request</h1>
        </div>
        <div class="content">
            <p>{greeting},</p>
            <p>We received a request to reset your password for your Personal Diary account.</p>

            <p style="text-align: center;">
                <a href="{reset_link}" class="button">Reset Password</a>
            </p>

            <p>Or copy and paste this link into your browser:</p>
            <p style="word-break: break-all; color: #4F46E5;">{reset_link}</p>

            <p>This link will expire in 1 hour for security reasons.</p>

            <div class="warning">
                <strong>⚠️ Security Notice:</strong> If you didn't request a password reset, please ignore this email. Your password will remain unchanged.
            </div>

            <p>For your security, this reset link can only be used once.</p>
        </div>
        <div class="footer">
            <p>© {datetime.now().year} Personal Diary. All rights reserved.</p>
            <p>This is an automated message, please do not reply.</p>
        </div>
    </div>
</body>
</html>
"""

        text_body = f"""
{greeting},

We received a request to reset your password for your Personal Diary account.

Reset Link:
{reset_link}

This link will expire in 1 hour for security reasons.

SECURITY NOTICE: If you didn't request a password reset, please ignore this email. Your password will remain unchanged.

For your security, this reset link can only be used once.

© {datetime.now().year} Personal Diary. All rights reserved.
This is an automated message, please do not reply.
"""

        return await self.send_email(
            to_email=to_email,
            subject="Password Reset - Personal Diary",
            html_body=html_body,
            text_body=text_body
        )

    async def send_welcome_email(
        self,
        to_email: str,
        user_name: Optional[str] = None,
        encryption_tier: str = "E2E"
    ) -> bool:
        """
        Send welcome email to new users.

        Args:
            to_email: User's email address
            user_name: User's name (optional)
            encryption_tier: User's encryption tier (E2E or UCE)

        Returns:
            True if email sent successfully
        """
        greeting = f"Hi {user_name}" if user_name else "Hello"

        # Customize message based on encryption tier
        encryption_note = ""
        if encryption_tier == "E2E":
            encryption_note = """
            <div style="background-color: #dbeafe; border-left: 4px solid #3b82f6; padding: 15px; margin: 20px 0;">
                <strong>🔒 End-to-End Encryption (E2E)</strong><br>
                Your diary entries are encrypted on your device before being sent to our servers. Only you can decrypt and read them. Please keep your recovery codes safe!
            </div>
            """
        else:
            encryption_note = """
            <div style="background-color: #dbeafe; border-left: 4px solid #3b82f6; padding: 15px; margin: 20px 0;">
                <strong>🔒 User-Controlled Encryption (UCE)</strong><br>
                Your diary entries are encrypted with your password. You can use advanced features like server-side search and AI insights.
            </div>
            """

        html_body = f"""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {{ font-family: Arial, sans-serif; line-height: 1.6; color: #333; }}
        .container {{ max-width: 600px; margin: 0 auto; padding: 20px; }}
        .header {{ background-color: #4F46E5; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }}
        .content {{ background-color: #f9fafb; padding: 30px; border-radius: 0 0 5px 5px; }}
        .button {{ display: inline-block; background-color: #4F46E5; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }}
        .footer {{ text-align: center; margin-top: 20px; font-size: 12px; color: #6b7280; }}
        .feature {{ margin: 10px 0; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Welcome to Personal Diary! 📔</h1>
        </div>
        <div class="content">
            <p>{greeting},</p>
            <p>Welcome to Personal Diary! We're excited to have you on board.</p>

            {encryption_note}

            <h3>Getting Started:</h3>
            <div class="feature">✍️ <strong>Write your first entry:</strong> Share your thoughts, experiences, and memories</div>
            <div class="feature">📸 <strong>Add media:</strong> Attach photos and videos to your entries</div>
            <div class="feature">🔍 <strong>Search your entries:</strong> Find past memories with ease</div>
            <div class="feature">📱 <strong>Access anywhere:</strong> Your diary syncs across all your devices</div>

            <p style="text-align: center;">
                <a href="{self.base_url}" class="button">Start Writing</a>
            </p>

            <p>If you have any questions or need help, feel free to reach out to our support team.</p>

            <p>Happy journaling!</p>
            <p><strong>The Personal Diary Team</strong></p>
        </div>
        <div class="footer">
            <p>© {datetime.now().year} Personal Diary. All rights reserved.</p>
            <p>This is an automated message, please do not reply.</p>
        </div>
    </div>
</body>
</html>
"""

        text_body = f"""
{greeting},

Welcome to Personal Diary! We're excited to have you on board.

Your Account: {encryption_tier} Encryption
{'Your diary entries are encrypted on your device before being sent to our servers. Only you can decrypt and read them. Please keep your recovery codes safe!' if encryption_tier == 'E2E' else 'Your diary entries are encrypted with your password. You can use advanced features like server-side search and AI insights.'}

Getting Started:
- Write your first entry: Share your thoughts, experiences, and memories
- Add media: Attach photos and videos to your entries
- Search your entries: Find past memories with ease
- Access anywhere: Your diary syncs across all your devices

Start Writing: {self.base_url}

If you have any questions or need help, feel free to reach out to our support team.

Happy journaling!
The Personal Diary Team

© {datetime.now().year} Personal Diary. All rights reserved.
This is an automated message, please do not reply.
"""

        return await self.send_email(
            to_email=to_email,
            subject="Welcome to Personal Diary! 📔",
            html_body=html_body,
            text_body=text_body
        )

    async def send_test_email(self, to_email: str) -> bool:
        """
        Send a test email to verify SES configuration.

        Args:
            to_email: Test recipient email address

        Returns:
            True if email sent successfully
        """
        html_body = f"""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {{ font-family: Arial, sans-serif; line-height: 1.6; color: #333; }}
        .container {{ max-width: 600px; margin: 0 auto; padding: 20px; }}
        .header {{ background-color: #4F46E5; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }}
        .content {{ background-color: #f9fafb; padding: 30px; border-radius: 0 0 5px 5px; }}
        .success {{ background-color: #d1fae5; border-left: 4px solid #10b981; padding: 15px; margin: 20px 0; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>AWS SES Test Email</h1>
        </div>
        <div class="content">
            <div class="success">
                <strong>✅ Success!</strong><br>
                AWS SES is configured correctly and working properly.
            </div>
            <p>This is a test email sent from Personal Diary backend using AWS SES with the AGILE profile.</p>
            <p><strong>Configuration:</strong></p>
            <ul>
                <li>From: {self.from_email}</li>
                <li>Region: {settings.aws_ses_region}</li>
                <li>Profile: AGILE</li>
            </ul>
            <p>If you received this email, your AWS SES setup is working correctly!</p>
        </div>
    </div>
</body>
</html>
"""

        text_body = f"""
AWS SES Test Email

SUCCESS! AWS SES is configured correctly and working properly.

This is a test email sent from Personal Diary backend using AWS SES with the AGILE profile.

Configuration:
- From: {self.from_email}
- Region: {settings.aws_ses_region}
- Profile: AGILE

If you received this email, your AWS SES setup is working correctly!
"""

        return await self.send_email(
            to_email=to_email,
            subject="AWS SES Test Email - Personal Diary",
            html_body=html_body,
            text_body=text_body
        )


# Create singleton instance
email_service = EmailService()
