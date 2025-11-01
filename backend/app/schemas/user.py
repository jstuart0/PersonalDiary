"""User-related Pydantic schemas"""

from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, EmailStr, Field, field_validator

from app.models.user import EncryptionTier


class UserCreate(BaseModel):
    """Schema for user signup request"""

    email: EmailStr
    password: str = Field(..., min_length=12, max_length=128)
    encryption_tier: EncryptionTier
    public_key: Optional[str] = None  # Required for E2E tier

    @field_validator("password")
    @classmethod
    def validate_password_strength(cls, v: str) -> str:
        """Validate password meets strength requirements"""
        if len(v) < 12:
            raise ValueError("Password must be at least 12 characters")
        if not any(c.isupper() for c in v):
            raise ValueError("Password must contain at least one uppercase letter")
        if not any(c.islower() for c in v):
            raise ValueError("Password must contain at least one lowercase letter")
        if not any(c.isdigit() for c in v):
            raise ValueError("Password must contain at least one digit")
        return v

    @field_validator("public_key")
    @classmethod
    def validate_public_key_for_e2e(cls, v: Optional[str], info) -> Optional[str]:
        """Validate public_key is provided for E2E tier"""
        if info.data.get("encryption_tier") == EncryptionTier.E2E:
            if not v:
                raise ValueError("public_key is required for E2E encryption tier")
            # Validate base64 format and length
            import base64

            try:
                decoded = base64.b64decode(v)
                if len(decoded) != 32:  # X25519 public key is 32 bytes
                    raise ValueError("public_key must be 32 bytes when decoded")
            except Exception:
                raise ValueError("public_key must be valid base64 encoding")
        return v


class UserLogin(BaseModel):
    """Schema for user login request"""

    email: EmailStr
    password: str


class TokenResponse(BaseModel):
    """Schema for authentication token response"""

    user_id: UUID
    email: str
    encryption_tier: EncryptionTier
    jwt_token: str
    refresh_token: str

    # Tier-specific fields
    encrypted_master_key: Optional[str] = None  # For UCE tier
    public_key: Optional[str] = None  # For E2E tier
    recovery_codes: Optional[list[str]] = None  # Only on signup for E2E tier


class UserResponse(BaseModel):
    """Schema for user information response"""

    id: UUID
    email: str
    encryption_tier: EncryptionTier
    created_at: datetime
    is_active: bool
    is_paid_tier: bool
    display_name: Optional[str] = None

    class Config:
        from_attributes = True


class StorageInfo(BaseModel):
    """Schema for storage quota information"""

    used_bytes: int
    limit_bytes: int
    percentage_used: float


class FeatureGateResponse(BaseModel):
    """Schema for feature availability response"""

    user_id: UUID
    encryption_tier: EncryptionTier
    features: dict[str, bool] = Field(
        default_factory=lambda: {
            "server_search": False,
            "server_ai": False,
            "easy_recovery": False,
            "auto_multi_device_sync": False,
            "user_sharing": False,
        }
    )
    storage: StorageInfo
    tier_info: dict[str, str] = Field(
        default_factory=lambda: {"name": "Free", "price": "$0/mo"}
    )
