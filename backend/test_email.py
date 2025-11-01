#!/usr/bin/env python3
"""
Test script for AWS SES email functionality.
Sends a test email to verify SES configuration.
"""

import asyncio
import sys
from app.services.email import email_service


async def test_email_send(to_email: str):
    """
    Send a test email to verify AWS SES configuration.

    Args:
        to_email: Recipient email address
    """
    print(f"\n{'='*60}")
    print("AWS SES Email Configuration Test")
    print(f"{'='*60}\n")

    print(f"Sending test email to: {to_email}")
    print(f"From: {email_service.from_email}")
    print(f"Region: {email_service.ses_client.meta.region_name}")
    print(f"Profile: AGILE\n")

    try:
        success = await email_service.send_test_email(to_email)

        if success:
            print(f"✅ SUCCESS: Test email sent successfully!")
            print(f"\nCheck your inbox at: {to_email}")
            print(f"The email should arrive within a few seconds.\n")
            return True
        else:
            print(f"❌ FAILED: Email was not sent.")
            print(f"Check the logs above for error details.\n")
            return False

    except Exception as e:
        print(f"❌ ERROR: {str(e)}\n")
        import traceback
        traceback.print_exc()
        return False


async def test_all_email_types(to_email: str):
    """
    Test all email types (verification, reset, welcome).

    Args:
        to_email: Recipient email address
    """
    print(f"\n{'='*60}")
    print("Testing All Email Types")
    print(f"{'='*60}\n")

    # Test verification email
    print("1. Testing Verification Email...")
    success1 = await email_service.send_verification_email(
        to_email=to_email,
        verification_token="TEST-VERIFICATION-TOKEN-12345",
        user_name="Test User"
    )
    print(f"   {'✅ Sent' if success1 else '❌ Failed'}\n")

    # Test password reset email
    print("2. Testing Password Reset Email...")
    success2 = await email_service.send_password_reset_email(
        to_email=to_email,
        reset_token="TEST-RESET-TOKEN-12345",
        user_name="Test User"
    )
    print(f"   {'✅ Sent' if success2 else '❌ Failed'}\n")

    # Test welcome email
    print("3. Testing Welcome Email (E2E)...")
    success3 = await email_service.send_welcome_email(
        to_email=to_email,
        user_name="Test User",
        encryption_tier="E2E"
    )
    print(f"   {'✅ Sent' if success3 else '❌ Failed'}\n")

    # Test welcome email UCE
    print("4. Testing Welcome Email (UCE)...")
    success4 = await email_service.send_welcome_email(
        to_email=to_email,
        user_name="Test User",
        encryption_tier="UCE"
    )
    print(f"   {'✅ Sent' if success4 else '❌ Failed'}\n")

    total_success = sum([success1, success2, success3, success4])
    print(f"\n{'='*60}")
    print(f"Results: {total_success}/4 emails sent successfully")
    print(f"{'='*60}\n")

    if total_success == 4:
        print(f"✅ All email types working correctly!")
        print(f"Check your inbox at: {to_email}\n")
        return True
    else:
        print(f"⚠️  Some emails failed to send. Check logs above.\n")
        return False


def main():
    """Main test function"""
    if len(sys.argv) < 2:
        print("Usage: python test_email.py <recipient-email> [--all]")
        print("\nExamples:")
        print("  python test_email.py user@example.com")
        print("  python test_email.py user@example.com --all")
        sys.exit(1)

    to_email = sys.argv[1]
    test_all = "--all" in sys.argv

    if test_all:
        success = asyncio.run(test_all_email_types(to_email))
    else:
        success = asyncio.run(test_email_send(to_email))

    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
