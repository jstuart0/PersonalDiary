"""Media-related Pydantic schemas"""

from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, Field


class MediaUpload(BaseModel):
    """Schema for initiating media upload"""

    entry_id: Optional[UUID] = Field(
        None, description="Entry ID to attach media to (optional)"
    )
    file_name: str = Field(..., description="Original filename")
    file_size: int = Field(..., gt=0, description="File size in bytes")
    mime_type: str = Field(..., description="MIME type of the file")
    encrypted_metadata: Optional[str] = Field(
        None, description="Encrypted metadata (EXIF, etc.)"
    )


class MediaUploadResponse(BaseModel):
    """Schema for media upload response with presigned URL"""

    media_id: UUID
    upload_url: str = Field(..., description="Presigned S3 upload URL")
    upload_fields: dict = Field(
        default_factory=dict, description="Additional fields for S3 upload"
    )
    expires_at: datetime = Field(..., description="When the upload URL expires")


class MediaResponse(BaseModel):
    """Schema for media metadata response"""

    id: UUID
    user_id: UUID
    entry_id: Optional[UUID] = None
    file_name: str
    file_size: int
    mime_type: str
    s3_key: str
    encrypted_metadata: Optional[str] = None
    content_hash: str
    uploaded_at: datetime
    deleted_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class MediaDownloadResponse(BaseModel):
    """Schema for media download response with presigned URL"""

    media_id: UUID
    download_url: str = Field(..., description="Presigned S3 download URL")
    file_name: str
    mime_type: str
    file_size: int
    expires_at: datetime = Field(..., description="When the download URL expires")


class MediaListResponse(BaseModel):
    """Schema for paginated list of media"""

    media: list[MediaResponse]
    total: int
    page: int
    page_size: int
    total_pages: int
