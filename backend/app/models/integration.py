"""Social media integration models"""

import enum
from datetime import datetime
import uuid

from sqlalchemy import (
    Column,
    DateTime,
    Enum,
    ForeignKey,
    Index,
    Integer,
    JSON,
    String,
    Text,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship

from app.database import Base


class Platform(str, enum.Enum):
    """Social media platform"""

    FACEBOOK = "facebook"
    INSTAGRAM = "instagram"
    TWITTER = "twitter"
    LINKEDIN = "linkedin"


class IntegrationStatus(str, enum.Enum):
    """Status of integration account"""

    ACTIVE = "active"
    EXPIRED = "expired"
    REVOKED = "revoked"
    ERROR = "error"


class IntegrationAccount(Base):
    """
    IntegrationAccount model - stores OAuth tokens for social media platforms.

    Tokens are encrypted before storage.
    """

    __tablename__ = "integration_accounts"

    # Primary key
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)

    # Foreign key to user
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)

    # Platform
    platform = Column(Enum(Platform), nullable=False)

    # Encrypted OAuth tokens
    encrypted_access_token = Column(Text, nullable=False)
    encrypted_refresh_token = Column(Text, nullable=True)

    # Token expiration
    token_expires_at = Column(DateTime, nullable=True)

    # Status
    status = Column(
        Enum(IntegrationStatus), nullable=False, default=IntegrationStatus.ACTIVE
    )

    # Timestamps
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    # Optional platform user info
    platform_user_id = Column(String(255), nullable=True)
    platform_username = Column(String(255), nullable=True)

    # OAuth scopes granted
    scopes = Column(JSON, nullable=True)

    # Relationships
    user = relationship("User", back_populates="integration_accounts")
    external_posts = relationship(
        "ExternalPost", back_populates="integration_account", cascade="all, delete-orphan"
    )

    # Indexes
    __table_args__ = (
        Index("idx_user_platform", "user_id", "platform", unique=True),
        Index("idx_status", "status"),
    )

    def __repr__(self):
        return f"<IntegrationAccount {self.platform.value} for User {self.user_id}>"

    @property
    def is_active(self) -> bool:
        """Check if integration is active"""
        return self.status == IntegrationStatus.ACTIVE

    @property
    def is_expired(self) -> bool:
        """Check if token is expired"""
        if self.token_expires_at is None:
            return False
        return datetime.utcnow() > self.token_expires_at


class SyncStatus(str, enum.Enum):
    """Status of external post sync"""

    PENDING = "pending"
    SYNCED = "synced"
    FAILED = "failed"
    OUT_OF_SYNC = "out_of_sync"


class ExternalPost(Base):
    """
    ExternalPost model - maps diary entries to social media posts.

    Tracks the relationship between local entries and external platform posts.
    """

    __tablename__ = "external_posts"

    # Primary key
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)

    # Foreign keys
    entry_id = Column(UUID(as_uuid=True), ForeignKey("entries.id"), nullable=False)
    integration_account_id = Column(
        UUID(as_uuid=True), ForeignKey("integration_accounts.id"), nullable=False
    )

    # Platform (denormalized for easier querying)
    platform = Column(Enum(Platform), nullable=False)

    # External post ID from platform
    external_post_id = Column(String(255), nullable=False)

    # External URL to post
    external_url = Column(Text, nullable=True)

    # Sync status
    sync_status = Column(
        Enum(SyncStatus), nullable=False, default=SyncStatus.PENDING
    )

    # Timestamps
    posted_at = Column(DateTime, nullable=True)  # When posted to platform
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )
    last_sync_at = Column(DateTime, nullable=True)

    # Error tracking
    error_message = Column(Text, nullable=True)
    retry_count = Column(Integer, default=0, nullable=False)

    # Relationships
    entry = relationship("Entry", back_populates="external_post")
    integration_account = relationship("IntegrationAccount", back_populates="external_posts")

    # Indexes
    __table_args__ = (
        Index("idx_entry_platform", "entry_id", "platform", unique=True),
        Index("idx_external_post_id", "external_post_id"),
        Index("idx_sync_status", "sync_status"),
    )

    def __repr__(self):
        return f"<ExternalPost {self.platform.value}:{self.external_post_id}>"

    @property
    def is_synced(self) -> bool:
        """Check if post is synced"""
        return self.sync_status == SyncStatus.SYNCED

    @property
    def needs_retry(self) -> bool:
        """Check if failed post should be retried"""
        return self.sync_status == SyncStatus.FAILED and self.retry_count < 3
