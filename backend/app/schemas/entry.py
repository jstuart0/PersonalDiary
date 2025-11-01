"""Entry-related Pydantic schemas"""

from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, Field

from app.models.entry import EntryMood, EntrySource


class EntryCreate(BaseModel):
    """Schema for creating a new entry"""

    encrypted_content: str = Field(
        ..., description="Encrypted entry content (client-side encrypted)"
    )
    content_hash: str = Field(
        ...,
        min_length=64,
        max_length=64,
        description="SHA-256 hash of plaintext content for deduplication",
    )
    encrypted_title: Optional[str] = Field(
        None, description="Encrypted entry title (optional)"
    )
    encrypted_location: Optional[str] = Field(
        None, description="Encrypted location data (optional)"
    )
    mood: Optional[EntryMood] = Field(
        None, description="Entry mood (stored in plaintext for filtering)"
    )
    weather: Optional[dict] = Field(
        None, description="Weather data (stored in plaintext for context)"
    )
    tag_names: Optional[list[str]] = Field(
        default_factory=list, description="List of tag names to attach to entry"
    )
    source: EntrySource = Field(
        default=EntrySource.DIARY, description="Source of entry (diary, facebook, etc.)"
    )


class EntryUpdate(BaseModel):
    """Schema for updating an existing entry"""

    encrypted_content: Optional[str] = None
    content_hash: Optional[str] = Field(None, min_length=64, max_length=64)
    encrypted_title: Optional[str] = None
    encrypted_location: Optional[str] = None
    mood: Optional[EntryMood] = None
    weather: Optional[dict] = None
    tag_names: Optional[list[str]] = None


class TagResponse(BaseModel):
    """Schema for tag response"""

    id: UUID
    tag_name: str
    auto_generated: bool
    created_at: datetime

    class Config:
        from_attributes = True


class EntryResponse(BaseModel):
    """Schema for entry response"""

    id: UUID
    user_id: UUID
    encrypted_content: str
    content_hash: str
    encrypted_title: Optional[str] = None
    encrypted_location: Optional[str] = None
    mood: Optional[EntryMood] = None
    weather: Optional[dict] = None
    source: EntrySource
    tags: list[TagResponse] = Field(default_factory=list)
    created_at: datetime
    updated_at: datetime
    deleted_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class EntryListResponse(BaseModel):
    """Schema for paginated list of entries"""

    entries: list[EntryResponse]
    total: int
    page: int
    page_size: int
    total_pages: int


class EntryEventResponse(BaseModel):
    """Schema for entry event (audit trail) response"""

    id: UUID
    entry_id: UUID
    event_type: str
    event_timestamp: datetime
    changes: Optional[dict] = None
    device_info: Optional[dict] = None
    ip_address: Optional[str] = None

    class Config:
        from_attributes = True
