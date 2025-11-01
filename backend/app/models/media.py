"""Media model"""

from datetime import datetime
import uuid

from sqlalchemy import BigInteger, Column, DateTime, ForeignKey, Integer, String, Text
from app.models.types import UUID
from sqlalchemy.orm import relationship

from app.database import Base


class Media(Base):
    """
    Media model - stores encrypted media files (photos, videos).

    Files are encrypted and stored in S3 (or compatible storage).
    """

    __tablename__ = "media"

    # Primary key
    id = Column(UUID, primary_key=True, default=uuid.uuid4, index=True)

    # Foreign keys
    user_id = Column(UUID, ForeignKey("users.id"), nullable=False)
    entry_id = Column(
        UUID, ForeignKey("entries.id"), nullable=True
    )  # Nullable to allow upload before entry creation

    # Encrypted file reference (S3 key or file path)
    encrypted_file_reference = Column(Text, nullable=False)

    # File hash (SHA-256 of encrypted file)
    file_hash = Column(String(64), nullable=False, index=True)

    # MIME type
    mime_type = Column(String(100), nullable=False)

    # File size in bytes
    file_size = Column(BigInteger, nullable=False)

    # Timestamp
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)

    # Optional encrypted filename
    encrypted_original_filename = Column(Text, nullable=True)

    # Image dimensions (for images)
    width = Column(Integer, nullable=True)
    height = Column(Integer, nullable=True)

    # Video duration in seconds (for videos)
    duration = Column(Integer, nullable=True)

    # Thumbnail reference (S3 key for thumbnail)
    thumbnail_reference = Column(Text, nullable=True)

    # Relationships
    user = relationship("User", back_populates="media")
    entry = relationship("Entry", back_populates="media")

    def __repr__(self):
        return f"<Media {self.id} ({self.mime_type}, {self.file_size} bytes)>"

    @property
    def is_image(self) -> bool:
        """Check if media is an image"""
        return self.mime_type.startswith("image/")

    @property
    def is_video(self) -> bool:
        """Check if media is a video"""
        return self.mime_type.startswith("video/")
