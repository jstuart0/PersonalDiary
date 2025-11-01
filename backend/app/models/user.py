"""User model"""

import enum
from datetime import datetime
from typing import List

from sqlalchemy import Boolean, Column, DateTime, Enum, String, Text
from sqlalchemy.orm import relationship
import uuid

from app.database import Base
from app.models.types import UUID


class EncryptionTier(str, enum.Enum):
    """Encryption tier enum"""

    E2E = "e2e"  # End-to-end encrypted
    UCE = "uce"  # User-controlled encryption


class User(Base):
    """
    User model - stores user account information.

    The encryption_tier field is IMMUTABLE after creation and determines
    what features are available to the user.
    """

    __tablename__ = "users"

    # Primary key
    id = Column(UUID, primary_key=True, default=uuid.uuid4, index=True)

    # Basic information
    email = Column(String(255), unique=True, nullable=False, index=True)
    password_hash = Column(String(255), nullable=False)

    # Encryption tier (IMMUTABLE)
    encryption_tier = Column(
        Enum(EncryptionTier), nullable=False, default=EncryptionTier.UCE
    )

    # Timestamps
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )
    last_login_at = Column(DateTime, nullable=True)

    # Tier-specific fields for E2E users
    # Public key stored on server (private key never leaves client)
    e2e_public_key = Column(Text, nullable=True)

    # Tier-specific fields for UCE users
    # Master encryption key (encrypted with password-derived key)
    uce_encrypted_master_key = Column(Text, nullable=True)
    # Salt for key derivation
    uce_key_derivation_salt = Column(String(64), nullable=True)

    # Optional user profile
    display_name = Column(String(255), nullable=True)
    timezone = Column(String(50), nullable=True, default="UTC")

    # Account status
    is_active = Column(Boolean, default=True, nullable=False)
    is_verified = Column(Boolean, default=False, nullable=False)

    # Storage tier (for quota enforcement)
    is_paid_tier = Column(Boolean, default=False, nullable=False)

    # Relationships
    entries = relationship("Entry", back_populates="user", cascade="all, delete-orphan")
    media = relationship("Media", back_populates="user", cascade="all, delete-orphan")
    integration_accounts = relationship(
        "IntegrationAccount", back_populates="user", cascade="all, delete-orphan"
    )
    e2e_public_keys = relationship(
        "E2EPublicKey", back_populates="user", cascade="all, delete-orphan", uselist=False
    )
    e2e_recovery_codes = relationship(
        "E2ERecoveryCode", back_populates="user", cascade="all, delete-orphan"
    )

    def __repr__(self):
        return f"<User {self.email} ({self.encryption_tier.value})>"

    @property
    def is_e2e(self) -> bool:
        """Check if user uses E2E encryption"""
        return self.encryption_tier == EncryptionTier.E2E

    @property
    def is_uce(self) -> bool:
        """Check if user uses UCE encryption"""
        return self.encryption_tier == EncryptionTier.UCE

    @property
    def storage_limit_bytes(self) -> int:
        """Get storage limit based on tier"""
        from app.config import settings

        if self.is_paid_tier:
            return settings.paid_tier_storage_bytes
        return settings.free_tier_storage_bytes
