"""Email verification and password reset token models"""

import enum
from datetime import datetime
import uuid

from sqlalchemy import Boolean, Column, DateTime, Enum, String
from sqlalchemy.orm import relationship
from sqlalchemy import ForeignKey

from app.database import Base
from app.models.types import UUID


class TokenType(str, enum.Enum):
    """Token type enum"""
    EMAIL_VERIFICATION = "email_verification"
    PASSWORD_RESET = "password_reset"


class Token(Base):
    """
    Token model for email verification and password reset.

    Tokens are single-use and expire after a set period.
    """

    __tablename__ = "tokens"

    # Primary key
    id = Column(UUID, primary_key=True, default=uuid.uuid4, index=True)

    # Token details
    token = Column(String(255), unique=True, nullable=False, index=True)
    token_type = Column(Enum(TokenType), nullable=False)

    # Associated user
    user_id = Column(UUID, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)

    # Token status
    is_used = Column(Boolean, default=False, nullable=False)
    expires_at = Column(DateTime, nullable=False)

    # Timestamps
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    used_at = Column(DateTime, nullable=True)

    # User relationship
    user = relationship("User")

    def __repr__(self):
        return f"<Token {self.token_type.value} for user {self.user_id}>"

    @property
    def is_expired(self) -> bool:
        """Check if token is expired"""
        return datetime.utcnow() > self.expires_at

    @property
    def is_valid(self) -> bool:
        """Check if token is valid (not used and not expired)"""
        return not self.is_used and not self.is_expired
