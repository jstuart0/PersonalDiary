"""Entry, Tag, and EntryEvent models"""

import enum
from datetime import datetime
import uuid

from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    Enum,
    ForeignKey,
    Index,
    JSON,
    String,
    Text,
)
from sqlalchemy.dialects.postgresql import TSVECTOR, UUID
from sqlalchemy.orm import relationship

from app.database import Base


class EntrySource(str, enum.Enum):
    """Source of entry"""

    DIARY = "diary"
    FACEBOOK = "facebook"
    INSTAGRAM = "instagram"
    TWITTER = "twitter"


class EntryMood(str, enum.Enum):
    """Entry mood"""

    HAPPY = "happy"
    SAD = "sad"
    NEUTRAL = "neutral"
    EXCITED = "excited"
    ANXIOUS = "anxious"
    GRATEFUL = "grateful"


class Entry(Base):
    """
    Entry model - stores encrypted diary entries.

    Content is encrypted before storage. The encryption method depends on
    the user's encryption tier.
    """

    __tablename__ = "entries"

    # Primary key
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)

    # Foreign key to user
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)

    # Encrypted content
    encrypted_content = Column(Text, nullable=False)

    # Content hash for deduplication (SHA-256 of plaintext before encryption)
    content_hash = Column(String(64), nullable=False, index=True)

    # Source of entry
    source = Column(
        Enum(EntrySource), nullable=False, default=EntrySource.DIARY, index=True
    )

    # Timestamps
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    # Soft delete
    deleted_at = Column(DateTime, nullable=True, index=True)

    # Optional encrypted fields
    encrypted_title = Column(Text, nullable=True)
    encrypted_location = Column(Text, nullable=True)

    # Optional metadata (stored in plaintext for filtering)
    mood = Column(Enum(EntryMood), nullable=True)
    weather = Column(JSON, nullable=True)

    # Full-text search vector (for UCE users only)
    search_vector = Column(TSVECTOR, nullable=True)

    # Relationships
    user = relationship("User", back_populates="entries")
    tags = relationship("Tag", back_populates="entry", cascade="all, delete-orphan")
    media = relationship("Media", back_populates="entry")
    events = relationship(
        "EntryEvent", back_populates="entry", cascade="all, delete-orphan"
    )
    external_post = relationship(
        "ExternalPost", back_populates="entry", uselist=False, cascade="all, delete-orphan"
    )

    # Indexes
    __table_args__ = (
        Index("idx_user_created", "user_id", "created_at"),
        Index("idx_user_deleted", "user_id", "deleted_at"),
        Index("idx_content_hash", "content_hash"),
        Index("idx_search_vector", "search_vector", postgresql_using="gin"),
    )

    def __repr__(self):
        return f"<Entry {self.id} by User {self.user_id}>"

    @property
    def is_deleted(self) -> bool:
        """Check if entry is soft-deleted"""
        return self.deleted_at is not None


class Tag(Base):
    """
    Tag model - categorizes entries.

    Tags can be user-created or auto-generated.
    """

    __tablename__ = "tags"

    # Primary key
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    # Foreign key to entry
    entry_id = Column(UUID(as_uuid=True), ForeignKey("entries.id"), nullable=False)

    # Tag name (case-insensitive, normalized to lowercase)
    tag_name = Column(String(100), nullable=False)

    # Auto-generated flag
    auto_generated = Column(Boolean, default=False, nullable=False)

    # Timestamp
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    # Relationships
    entry = relationship("Entry", back_populates="tags")

    # Indexes
    __table_args__ = (
        Index("idx_entry_tag", "entry_id", "tag_name", unique=True),
        Index("idx_tag_name", "tag_name"),
    )

    def __repr__(self):
        return f"<Tag {self.tag_name} on Entry {self.entry_id}>"


class EventType(str, enum.Enum):
    """Type of entry event"""

    CREATED = "created"
    EDITED = "edited"
    SHARED = "shared"
    IMPORTED = "imported"
    TAGGED = "tagged"
    DELETED = "deleted"
    RESTORED = "restored"


class EntryEvent(Base):
    """
    EntryEvent model - audit trail for entry changes.

    Provides version history and tracking of all modifications.
    """

    __tablename__ = "entry_events"

    # Primary key
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    # Foreign key to entry
    entry_id = Column(UUID(as_uuid=True), ForeignKey("entries.id"), nullable=False)

    # Event type
    event_type = Column(Enum(EventType), nullable=False)

    # Event timestamp
    event_timestamp = Column(
        DateTime, nullable=False, default=datetime.utcnow, index=True
    )

    # Changes metadata (JSON)
    changes = Column(JSON, nullable=True)

    # Optional device/client info
    device_info = Column(JSON, nullable=True)

    # IP address (optional, for security auditing)
    ip_address = Column(String(45), nullable=True)  # IPv6 max length

    # Relationships
    entry = relationship("Entry", back_populates="events")

    # Indexes
    __table_args__ = (Index("idx_entry_event_timestamp", "entry_id", "event_timestamp"),)

    def __repr__(self):
        return f"<EntryEvent {self.event_type.value} for Entry {self.entry_id}>"
