"""Authentication service for user signup, login, and JWT management"""

import secrets
from datetime import datetime, timedelta
from typing import Optional, Tuple
from uuid import UUID

import bcrypt
from jose import JWTError, jwt
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.config import settings
from app.models.user import User, EncryptionTier
from app.models.e2e import E2EPublicKey, E2ERecoveryCode
from app.models.token import Token, TokenType
from app.services.encryption import E2EEncryption, UCEEncryption
from app.services.email import email_service


class AuthService:
    """
    Authentication service handling user registration, login, and JWT tokens.
    """

    def __init__(self):
        self.e2e_encryption = E2EEncryption()
        self.uce_encryption = UCEEncryption()

    @staticmethod
    def hash_password(password: str) -> str:
        """
        Hash password using bcrypt.

        Args:
            password: Plain text password

        Returns:
            Bcrypt hashed password
        """
        salt = bcrypt.gensalt()
        hashed = bcrypt.hashpw(password.encode(), salt)
        return hashed.decode()

    @staticmethod
    def verify_password(password: str, hashed_password: str) -> bool:
        """
        Verify password against bcrypt hash.

        Args:
            password: Plain text password
            hashed_password: Bcrypt hashed password

        Returns:
            True if password matches, False otherwise
        """
        return bcrypt.checkpw(password.encode(), hashed_password.encode())

    @staticmethod
    def create_access_token(
        user_id: UUID, email: str, encryption_tier: EncryptionTier
    ) -> str:
        """
        Create JWT access token.

        Args:
            user_id: User ID
            email: User email
            encryption_tier: User encryption tier

        Returns:
            JWT access token
        """
        expires = datetime.utcnow() + timedelta(
            minutes=settings.access_token_expire_minutes
        )
        payload = {
            "sub": str(user_id),
            "email": email,
            "encryption_tier": encryption_tier.value,
            "exp": expires,
            "iat": datetime.utcnow(),
            "type": "access",
        }
        return jwt.encode(payload, settings.secret_key, algorithm=settings.algorithm)

    @staticmethod
    def create_refresh_token(user_id: UUID) -> str:
        """
        Create JWT refresh token.

        Args:
            user_id: User ID

        Returns:
            JWT refresh token
        """
        expires = datetime.utcnow() + timedelta(days=settings.refresh_token_expire_days)
        payload = {
            "sub": str(user_id),
            "exp": expires,
            "iat": datetime.utcnow(),
            "type": "refresh",
            "jti": secrets.token_urlsafe(32),  # Unique token ID
        }
        return jwt.encode(payload, settings.secret_key, algorithm=settings.algorithm)

    @staticmethod
    def verify_token(token: str, token_type: str = "access") -> Optional[dict]:
        """
        Verify and decode JWT token.

        Args:
            token: JWT token to verify
            token_type: Expected token type ("access" or "refresh")

        Returns:
            Decoded payload if valid, None otherwise
        """
        try:
            payload = jwt.decode(
                token, settings.secret_key, algorithms=[settings.algorithm]
            )
            if payload.get("type") != token_type:
                return None
            return payload
        except JWTError:
            return None

    async def signup_user(
        self,
        db: AsyncSession,
        email: str,
        password: str,
        encryption_tier: EncryptionTier,
        public_key: Optional[str] = None,
    ) -> Tuple[User, Optional[list[str]]]:
        """
        Register a new user.

        Args:
            db: Database session
            email: User email
            password: User password
            encryption_tier: Encryption tier (E2E or UCE)
            public_key: Public key (required for E2E tier)

        Returns:
            Tuple of (User, recovery_codes)
            recovery_codes is only populated for E2E tier

        Raises:
            ValueError: If email already exists or validation fails
        """
        # Check if email already exists
        result = await db.execute(select(User).filter(User.email == email))
        existing_user = result.scalar_one_or_none()
        if existing_user:
            raise ValueError("Email already registered")

        # Hash password
        password_hash = self.hash_password(password)

        # Create user based on encryption tier
        if encryption_tier == EncryptionTier.E2E:
            if not public_key:
                raise ValueError("Public key required for E2E tier")
            if not self.e2e_encryption.validate_public_key(public_key):
                raise ValueError("Invalid public key format")

            # Create E2E user
            user = User(
                email=email,
                password_hash=password_hash,
                encryption_tier=EncryptionTier.E2E,
                e2e_public_key=public_key,
            )

            # Generate recovery codes
            recovery_codes = self.e2e_encryption.generate_recovery_codes(10)

            # Store hashed recovery codes
            db.add(user)
            await db.flush()  # Get user ID

            for code in recovery_codes:
                hashed_code = self.e2e_encryption.hash_recovery_code(code)
                recovery_code_record = E2ERecoveryCode(
                    user_id=user.id, code_hash=hashed_code
                )
                db.add(recovery_code_record)

            await db.commit()
            await db.refresh(user)

            return user, recovery_codes

        else:  # UCE tier
            # Set up UCE encryption
            encrypted_master_key_b64, salt_b64 = self.uce_encryption.setup_user_encryption(
                password
            )

            user = User(
                email=email,
                password_hash=password_hash,
                encryption_tier=EncryptionTier.UCE,
                uce_encrypted_master_key=encrypted_master_key_b64,
                uce_key_derivation_salt=salt_b64,
            )

            db.add(user)
            await db.commit()
            await db.refresh(user)

            return user, None

    async def login_user(
        self, db: AsyncSession, email: str, password: str
    ) -> Tuple[User, str, str]:
        """
        Authenticate user and generate tokens.

        Args:
            db: Database session
            email: User email
            password: User password

        Returns:
            Tuple of (User, access_token, refresh_token)

        Raises:
            ValueError: If authentication fails
        """
        # Find user by email
        result = await db.execute(select(User).filter(User.email == email))
        user = result.scalar_one_or_none()

        if not user:
            raise ValueError("Invalid email or password")

        # Verify password
        if not self.verify_password(password, user.password_hash):
            raise ValueError("Invalid email or password")

        # Check if account is active
        if not user.is_active:
            raise ValueError("Account is disabled")

        # Update last login timestamp
        user.last_login_at = datetime.utcnow()
        await db.commit()
        await db.refresh(user)

        # Generate tokens
        access_token = self.create_access_token(
            user.id, user.email, user.encryption_tier
        )
        refresh_token = self.create_refresh_token(user.id)

        return user, access_token, refresh_token

    async def refresh_access_token(
        self, db: AsyncSession, refresh_token: str
    ) -> Optional[str]:
        """
        Generate new access token from refresh token.

        Args:
            db: Database session
            refresh_token: Valid refresh token

        Returns:
            New access token if valid, None otherwise
        """
        payload = self.verify_token(refresh_token, token_type="refresh")
        if not payload:
            return None

        user_id = UUID(payload["sub"])

        # Get user from database
        result = await db.execute(select(User).filter(User.id == user_id))
        user = result.scalar_one_or_none()

        if not user or not user.is_active:
            return None

        # Generate new access token
        return self.create_access_token(user.id, user.email, user.encryption_tier)

    async def verify_recovery_code(
        self, db: AsyncSession, user_id: UUID, recovery_code: str
    ) -> bool:
        """
        Verify recovery code for E2E user account recovery.

        Args:
            db: Database session
            user_id: User ID
            recovery_code: Recovery code to verify

        Returns:
            True if valid and not used, False otherwise
        """
        code_hash = self.e2e_encryption.hash_recovery_code(recovery_code)

        result = await db.execute(
            select(E2ERecoveryCode).filter(
                E2ERecoveryCode.user_id == user_id,
                E2ERecoveryCode.code_hash == code_hash,
                E2ERecoveryCode.used_at.is_(None),
            )
        )
        recovery_code_record = result.scalar_one_or_none()

        if recovery_code_record:
            # Mark as used
            recovery_code_record.used_at = datetime.utcnow()
            await db.commit()
            return True

        return False

    async def get_user_by_id(self, db: AsyncSession, user_id: UUID) -> Optional[User]:
        """
        Get user by ID.

        Args:
            db: Database session
            user_id: User ID

        Returns:
            User if found, None otherwise
        """
        result = await db.execute(select(User).filter(User.id == user_id))
        return result.scalar_one_or_none()

    async def get_user_by_email(
        self, db: AsyncSession, email: str
    ) -> Optional[User]:
        """
        Get user by email.

        Args:
            db: Database session
            email: User email

        Returns:
            User if found, None otherwise
        """
        result = await db.execute(select(User).filter(User.email == email))
        return result.scalar_one_or_none()

    async def create_verification_token(
        self, db: AsyncSession, user_id: UUID
    ) -> str:
        """
        Create email verification token.

        Args:
            db: Database session
            user_id: User ID

        Returns:
            Verification token
        """
        # Generate secure token
        token_value = secrets.token_urlsafe(32)

        # Create token record (expires in 24 hours)
        token = Token(
            token=token_value,
            token_type=TokenType.EMAIL_VERIFICATION,
            user_id=user_id,
            expires_at=datetime.utcnow() + timedelta(hours=24)
        )

        db.add(token)
        await db.commit()

        return token_value

    async def verify_email_token(
        self, db: AsyncSession, token_value: str
    ) -> Optional[User]:
        """
        Verify email verification token and mark user as verified.

        Args:
            db: Database session
            token_value: Verification token

        Returns:
            User if token is valid, None otherwise
        """
        # Find token
        result = await db.execute(
            select(Token).filter(
                Token.token == token_value,
                Token.token_type == TokenType.EMAIL_VERIFICATION,
                Token.is_used == False
            )
        )
        token = result.scalar_one_or_none()

        if not token or not token.is_valid:
            return None

        # Mark token as used
        token.is_used = True
        token.used_at = datetime.utcnow()

        # Get user and mark as verified
        user = await self.get_user_by_id(db, token.user_id)
        if user:
            user.is_verified = True
            await db.commit()
            await db.refresh(user)

        return user

    async def create_password_reset_token(
        self, db: AsyncSession, user_id: UUID
    ) -> str:
        """
        Create password reset token.

        Args:
            db: Database session
            user_id: User ID

        Returns:
            Reset token
        """
        # Generate secure token
        token_value = secrets.token_urlsafe(32)

        # Create token record (expires in 1 hour)
        token = Token(
            token=token_value,
            token_type=TokenType.PASSWORD_RESET,
            user_id=user_id,
            expires_at=datetime.utcnow() + timedelta(hours=1)
        )

        db.add(token)
        await db.commit()

        return token_value

    async def verify_reset_token(
        self, db: AsyncSession, token_value: str
    ) -> Optional[User]:
        """
        Verify password reset token.

        Args:
            db: Database session
            token_value: Reset token

        Returns:
            User if token is valid, None otherwise
        """
        # Find token
        result = await db.execute(
            select(Token).filter(
                Token.token == token_value,
                Token.token_type == TokenType.PASSWORD_RESET,
                Token.is_used == False
            )
        )
        token = result.scalar_one_or_none()

        if not token or not token.is_valid:
            return None

        # Get user
        user = await self.get_user_by_id(db, token.user_id)
        return user

    async def reset_password(
        self, db: AsyncSession, token_value: str, new_password: str
    ) -> Optional[User]:
        """
        Reset user password using reset token.

        Args:
            db: Database session
            token_value: Reset token
            new_password: New password

        Returns:
            User if password reset successful, None otherwise
        """
        # Find token
        result = await db.execute(
            select(Token).filter(
                Token.token == token_value,
                Token.token_type == TokenType.PASSWORD_RESET,
                Token.is_used == False
            )
        )
        token = result.scalar_one_or_none()

        if not token or not token.is_valid:
            return None

        # Mark token as used
        token.is_used = True
        token.used_at = datetime.utcnow()

        # Get user and update password
        user = await self.get_user_by_id(db, token.user_id)
        if user:
            user.password_hash = self.hash_password(new_password)
            await db.commit()
            await db.refresh(user)

        return user

    async def send_verification_email(
        self, db: AsyncSession, user: User
    ) -> bool:
        """
        Send verification email to user.

        Args:
            db: Database session
            user: User to send verification email to

        Returns:
            True if email sent successfully
        """
        # Create verification token
        token = await self.create_verification_token(db, user.id)

        # Send verification email
        return await email_service.send_verification_email(
            to_email=user.email,
            verification_token=token,
            user_name=user.display_name
        )

    async def send_password_reset_email(
        self, db: AsyncSession, user: User
    ) -> bool:
        """
        Send password reset email to user.

        Args:
            db: Database session
            user: User to send reset email to

        Returns:
            True if email sent successfully
        """
        # Create reset token
        token = await self.create_password_reset_token(db, user.id)

        # Send reset email
        return await email_service.send_password_reset_email(
            to_email=user.email,
            reset_token=token,
            user_name=user.display_name
        )
