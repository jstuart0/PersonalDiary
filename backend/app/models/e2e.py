"""E2E encryption specific models"""

from datetime import datetime
import uuid

from sqlalchemy import Boolean, Column, DateTime, ForeignKey, Index, String, Text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship

from app.database import Base


class E2EPublicKey(Base):
    """
    E2EPublicKey model - stores public keys for E2E users.

    Private keys never leave the client device.
    Only one public key per user in MVP (multi-device keys in future).
    """

    __tablename__ = "e2e_public_keys"

    # Primary key
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    # Foreign key to user (unique - one public key per user)
    user_id = Column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, unique=True
    )

    # Public key (base64 encoded)
    public_key = Column(Text, nullable=False)

    # Key algorithm identifier
    key_algorithm = Column(String(50), default="X25519", nullable=False)

    # Timestamp
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    # Relationships
    user = relationship("User", back_populates="e2e_public_keys")

    def __repr__(self):
        return f"<E2EPublicKey for User {self.user_id}>"


class E2ERecoveryCode(Base):
    """
    E2ERecoveryCode model - stores recovery codes for E2E users.

    Codes are hashed (SHA-256) before storage.
    10 codes per user typically.
    """

    __tablename__ = "e2e_recovery_codes"

    # Primary key
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    # Foreign key to user
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)

    # Code hash (SHA-256 of recovery code)
    code_hash = Column(String(64), nullable=False, index=True)

    # Usage tracking
    used = Column(Boolean, default=False, nullable=False)
    used_at = Column(DateTime, nullable=True)

    # Timestamp
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    # Relationships
    user = relationship("User", back_populates="e2e_recovery_codes")

    # Indexes
    __table_args__ = (
        Index("idx_user_used", "user_id", "used"),
        Index("idx_code_hash", "code_hash"),
    )

    def __repr__(self):
        return f"<E2ERecoveryCode for User {self.user_id} (used={self.used})>"

    def mark_as_used(self):
        """Mark recovery code as used"""
        self.used = True
        self.used_at = datetime.utcnow()
