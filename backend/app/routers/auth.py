"""Authentication API endpoints"""

import logging
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Header, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.user import EncryptionTier
from app.schemas.user import (
    UserCreate,
    UserLogin,
    TokenResponse,
    UserResponse,
    FeatureGateResponse,
    StorageInfo,
)
from app.services.auth import AuthService

logger = logging.getLogger(__name__)

router = APIRouter()
auth_service = AuthService()


async def get_current_user(
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(get_db),
):
    """
    Dependency to get current authenticated user from JWT token.

    Args:
        authorization: Authorization header with Bearer token
        db: Database session

    Returns:
        Current user

    Raises:
        HTTPException: If authentication fails
    """
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid authorization header",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = authorization.replace("Bearer ", "")
    payload = auth_service.verify_token(token, token_type="access")

    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    user_id = UUID(payload["sub"])
    user = await auth_service.get_user_by_id(db, user_id)

    if not user or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found or inactive",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return user


@router.post(
    "/signup",
    status_code=status.HTTP_201_CREATED,
    summary="Register new user",
    description="Create a new user account with chosen encryption tier (E2E or UCE)",
)
async def signup(user_data: UserCreate, db: AsyncSession = Depends(get_db)):
    """
    Register a new user account.

    - **E2E tier**: Requires public_key, returns recovery codes (store securely!)
    - **UCE tier**: Server-side encryption, enables search and AI features

    Recovery codes are ONLY shown once during signup for E2E users.
    """
    try:
        user, recovery_codes = await auth_service.signup_user(
            db=db,
            email=user_data.email,
            password=user_data.password,
            encryption_tier=user_data.encryption_tier,
            public_key=user_data.public_key,
        )

        # Generate tokens
        access_token = auth_service.create_access_token(
            user.id, user.email, user.encryption_tier
        )
        refresh_token = auth_service.create_refresh_token(user.id)

        # Build response in format expected by web app
        response = {
            "user": {
                "id": str(user.id),
                "email": user.email,
                "encryptionTier": user.encryption_tier.value,
                "createdAt": user.created_at.isoformat(),
                "updatedAt": user.updated_at.isoformat(),
            },
            "tokens": {
                "accessToken": access_token,
                "refreshToken": refresh_token,
                "expiresIn": 900,  # 15 minutes
            }
        }

        # Add tier-specific fields
        if user.encryption_tier == EncryptionTier.E2E:
            response["user"]["publicKey"] = user.e2e_public_key
            if recovery_codes:
                response["recoveryCodes"] = recovery_codes
        else:
            response["user"]["encryptedMasterKey"] = user.uce_encrypted_master_key
            response["user"]["keyDerivationSalt"] = user.uce_key_derivation_salt

        logger.info(f"User signup successful: {user.email} ({user.encryption_tier.value})")

        # Send welcome email (async, don't wait for it)
        try:
            await auth_service.send_verification_email(db, user)
            from app.services.email import email_service
            await email_service.send_welcome_email(
                to_email=user.email,
                user_name=user.display_name,
                encryption_tier=user.encryption_tier.value.upper()
            )
        except Exception as email_error:
            # Log but don't fail signup if email fails
            logger.warning(f"Failed to send welcome/verification email: {str(email_error)}")

        return response

    except ValueError as e:
        logger.warning(f"Signup failed: {str(e)}")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except Exception as e:
        logger.error(f"Signup error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Signup failed",
        )


@router.post(
    "/login",
    summary="Login user",
    description="Authenticate user and receive JWT tokens",
)
async def login(credentials: UserLogin, db: AsyncSession = Depends(get_db)):
    """
    Authenticate user and get access tokens.

    Returns JWT access token (15 min) and refresh token (30 days).
    """
    try:
        user, access_token, refresh_token = await auth_service.login_user(
            db=db, email=credentials.email, password=credentials.password
        )

        # Build response in format expected by web app
        response = {
            "user": {
                "id": str(user.id),
                "email": user.email,
                "encryptionTier": user.encryption_tier.value,
                "createdAt": user.created_at.isoformat(),
                "updatedAt": user.updated_at.isoformat(),
            },
            "tokens": {
                "accessToken": access_token,
                "refreshToken": refresh_token,
                "expiresIn": 900,  # 15 minutes
            }
        }

        # Add tier-specific fields
        if user.encryption_tier == EncryptionTier.E2E:
            response["user"]["publicKey"] = user.e2e_public_key
        else:
            response["user"]["encryptedMasterKey"] = user.uce_encrypted_master_key
            response["user"]["keyDerivationSalt"] = user.uce_key_derivation_salt

        logger.info(f"User login successful: {user.email}")
        return response

    except ValueError as e:
        logger.warning(f"Login failed: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
        )
    except Exception as e:
        logger.error(f"Login error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Login failed",
        )


@router.post(
    "/refresh",
    response_model=dict,
    summary="Refresh access token",
    description="Get new access token using refresh token",
)
async def refresh_token(
    refresh_token: str, db: AsyncSession = Depends(get_db)
):
    """
    Refresh access token using valid refresh token.

    Returns new access token (15 min validity).
    """
    try:
        new_access_token = await auth_service.refresh_access_token(db, refresh_token)

        if not new_access_token:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired refresh token",
            )

        return {"access_token": new_access_token, "token_type": "bearer"}

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Token refresh error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Token refresh failed",
        )


@router.get(
    "/me",
    response_model=UserResponse,
    summary="Get current user",
    description="Get current authenticated user information",
)
async def get_me(current_user=Depends(get_current_user)):
    """
    Get current user information.

    Requires valid JWT access token.
    """
    return UserResponse.model_validate(current_user)


@router.get(
    "/features",
    response_model=FeatureGateResponse,
    summary="Get feature availability",
    description="Get available features based on user encryption tier and subscription",
)
async def get_features(
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get feature availability for current user.

    Features vary based on:
    - Encryption tier (E2E vs UCE)
    - Subscription tier (Free vs Paid)
    """
    # Calculate storage usage (placeholder - implement actual calculation)
    # TODO: Implement actual storage calculation from media table
    storage_used = 0  # Placeholder
    storage_limit = current_user.storage_limit_bytes

    storage_info = StorageInfo(
        used_bytes=storage_used,
        limit_bytes=storage_limit,
        percentage_used=(storage_used / storage_limit * 100) if storage_limit > 0 else 0,
    )

    # Determine available features based on encryption tier
    features = {
        "server_search": current_user.is_uce,  # Only UCE supports server-side search
        "server_ai": current_user.is_uce,  # Only UCE supports server-side AI
        "easy_recovery": current_user.is_uce,  # UCE has password recovery
        "auto_multi_device_sync": current_user.is_uce,  # UCE auto-syncs
        "user_sharing": False,  # Not implemented yet
    }

    tier_info = {
        "name": "Paid" if current_user.is_paid_tier else "Free",
        "price": "$5/mo" if current_user.is_paid_tier else "$0/mo",
    }

    return FeatureGateResponse(
        user_id=current_user.id,
        encryption_tier=current_user.encryption_tier,
        features=features,
        storage=storage_info,
        tier_info=tier_info,
    )


@router.post(
    "/verify-recovery-code",
    response_model=dict,
    summary="Verify recovery code",
    description="Verify E2E recovery code (for account recovery workflow)",
)
async def verify_recovery_code(
    recovery_code: str,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Verify recovery code for E2E users.

    Used in account recovery workflow. Code can only be used once.
    """
    if not current_user.is_e2e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Recovery codes only available for E2E encryption tier",
        )

    is_valid = await auth_service.verify_recovery_code(
        db, current_user.id, recovery_code
    )

    if not is_valid:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or already used recovery code",
        )

    return {"valid": True, "message": "Recovery code verified successfully"}


@router.post(
    "/logout",
    response_model=dict,
    summary="Logout user",
    description="Logout current user (client should discard tokens)",
)
async def logout(current_user=Depends(get_current_user)):
    """
    Logout user.

    Client should discard access and refresh tokens.
    Server-side token invalidation not implemented yet (tokens will expire naturally).
    """
    logger.info(f"User logout: {current_user.email}")
    return {"message": "Logged out successfully"}


@router.post(
    "/verify-email",
    response_model=dict,
    summary="Verify email address",
    description="Verify user email with verification token",
)
async def verify_email(token: str, db: AsyncSession = Depends(get_db)):
    """
    Verify user email address with verification token.

    Token is sent to user's email after signup.
    """
    try:
        user = await auth_service.verify_email_token(db, token)

        if not user:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or expired verification token",
            )

        logger.info(f"Email verified for user: {user.email}")
        return {
            "message": "Email verified successfully",
            "email": user.email,
            "verified": True
        }

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Email verification error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Email verification failed",
        )


@router.post(
    "/resend-verification",
    response_model=dict,
    summary="Resend verification email",
    description="Resend verification email to user",
)
async def resend_verification(
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Resend verification email to current user.

    Only works if user is not already verified.
    """
    try:
        if current_user.is_verified:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Email already verified",
            )

        # Send verification email
        success = await auth_service.send_verification_email(db, current_user)

        if not success:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to send verification email",
            )

        logger.info(f"Verification email resent to: {current_user.email}")
        return {"message": "Verification email sent successfully"}

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Resend verification error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to resend verification email",
        )


@router.post(
    "/forgot-password",
    response_model=dict,
    summary="Request password reset",
    description="Request password reset email",
)
async def forgot_password(email: str, db: AsyncSession = Depends(get_db)):
    """
    Request password reset email.

    Sends password reset email if user exists. Always returns success
    to prevent email enumeration attacks.
    """
    try:
        # Find user by email
        user = await auth_service.get_user_by_email(db, email)

        if user:
            # Send password reset email
            await auth_service.send_password_reset_email(db, user)
            logger.info(f"Password reset email sent to: {email}")
        else:
            # Don't reveal if user exists or not
            logger.warning(f"Password reset requested for non-existent email: {email}")

        # Always return success to prevent email enumeration
        return {
            "message": "If an account exists with this email, a password reset link has been sent"
        }

    except Exception as e:
        logger.error(f"Forgot password error: {str(e)}")
        # Still return success to prevent information disclosure
        return {
            "message": "If an account exists with this email, a password reset link has been sent"
        }


@router.post(
    "/reset-password",
    response_model=dict,
    summary="Reset password",
    description="Reset password using reset token",
)
async def reset_password(
    token: str,
    new_password: str,
    db: AsyncSession = Depends(get_db)
):
    """
    Reset password using reset token.

    Token is sent to user's email after requesting password reset.
    """
    try:
        # Validate password length
        if len(new_password) < 8:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Password must be at least 8 characters",
            )

        user = await auth_service.reset_password(db, token, new_password)

        if not user:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or expired reset token",
            )

        logger.info(f"Password reset successful for user: {user.email}")
        return {
            "message": "Password reset successfully",
            "email": user.email
        }

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Password reset error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Password reset failed",
        )
