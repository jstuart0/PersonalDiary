"""Media API endpoints"""

import logging
from datetime import datetime, timedelta
from typing import Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.media import Media
from app.routers.auth import get_current_user
from app.schemas.media import (
    MediaUpload,
    MediaUploadResponse,
    MediaResponse,
    MediaDownloadResponse,
    MediaListResponse,
)
from app.services.storage import S3StorageService

logger = logging.getLogger(__name__)

router = APIRouter()
storage_service = S3StorageService()


@router.post(
    "/upload",
    response_model=MediaUploadResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Initiate media upload",
    description="Get presigned S3 URL for direct client upload",
)
async def initiate_upload(
    media_data: MediaUpload,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Initiate media upload by creating metadata record and presigned S3 URL.

    Client should upload directly to S3 using the returned URL.
    """
    try:
        # Validate file size against user's tier
        max_file_size = current_user.storage_limit_bytes  # Simplified - use max_file_size from settings
        from app.config import settings

        if media_data.file_size > settings.max_file_size_bytes:
            raise HTTPException(
                status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                detail=f"File size exceeds maximum allowed ({settings.max_file_size_bytes} bytes)",
            )

        # Check storage quota
        # TODO: Implement actual storage usage calculation
        # For now, just create the record

        # Create media record
        media = Media(
            user_id=current_user.id,
            entry_id=media_data.entry_id,
            file_name=media_data.file_name,
            file_size=media_data.file_size,
            mime_type=media_data.mime_type,
            encrypted_metadata=media_data.encrypted_metadata,
        )

        db.add(media)
        await db.flush()

        # Generate S3 key
        s3_key = storage_service.generate_s3_key(
            current_user.id, media.id, media_data.file_name
        )
        media.s3_key = s3_key

        # Generate presigned upload URL
        presigned_data = storage_service.generate_upload_presigned_url(
            s3_key=s3_key,
            mime_type=media_data.mime_type,
            file_size=media_data.file_size,
            expires_in=3600,  # 1 hour
        )

        await db.commit()
        await db.refresh(media)

        expires_at = datetime.utcnow() + timedelta(seconds=3600)

        logger.info(f"Media upload initiated: {media.id} for user {current_user.id}")

        return MediaUploadResponse(
            media_id=media.id,
            upload_url=presigned_data["url"],
            upload_fields=presigned_data["fields"],
            expires_at=expires_at,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Media upload initiation error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to initiate upload",
        )


@router.get(
    "/",
    response_model=MediaListResponse,
    summary="List media",
    description="Get paginated list of user's media files",
)
async def list_media(
    page: int = Query(1, ge=1, description="Page number (1-indexed)"),
    page_size: int = Query(20, ge=1, le=100, description="Items per page"),
    entry_id: Optional[UUID] = Query(None, description="Filter by entry ID"),
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get paginated list of media files.

    Optionally filter by entry ID.
    """
    try:
        # Build query
        query = select(Media).where(
            Media.user_id == current_user.id, Media.deleted_at.is_(None)
        )

        if entry_id:
            query = query.where(Media.entry_id == entry_id)

        # Get total count
        count_query = select(func.count()).select_from(query.subquery())
        total_result = await db.execute(count_query)
        total = total_result.scalar()

        # Get paginated results
        query = (
            query.order_by(Media.uploaded_at.desc())
            .limit(page_size)
            .offset((page - 1) * page_size)
        )

        result = await db.execute(query)
        media_items = result.scalars().all()

        total_pages = (total + page_size - 1) // page_size if total > 0 else 0

        return MediaListResponse(
            media=[MediaResponse.model_validate(m) for m in media_items],
            total=total,
            page=page,
            page_size=page_size,
            total_pages=total_pages,
        )

    except Exception as e:
        logger.error(f"List media error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve media",
        )


@router.get(
    "/{media_id}",
    response_model=MediaResponse,
    summary="Get media metadata",
    description="Get metadata for a specific media file",
)
async def get_media(
    media_id: UUID,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get media file metadata.

    User must own the media.
    """
    try:
        query = select(Media).where(
            Media.id == media_id, Media.user_id == current_user.id
        )

        result = await db.execute(query)
        media = result.scalar_one_or_none()

        if not media:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Media not found"
            )

        return MediaResponse.model_validate(media)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Get media error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve media",
        )


@router.get(
    "/{media_id}/download",
    response_model=MediaDownloadResponse,
    summary="Get media download URL",
    description="Get presigned S3 URL for downloading media file",
)
async def get_download_url(
    media_id: UUID,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get presigned download URL for media file.

    URL is valid for 1 hour.
    """
    try:
        query = select(Media).where(
            Media.id == media_id, Media.user_id == current_user.id
        )

        result = await db.execute(query)
        media = result.scalar_one_or_none()

        if not media:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Media not found"
            )

        if media.deleted_at:
            raise HTTPException(
                status_code=status.HTTP_410_GONE, detail="Media has been deleted"
            )

        # Generate presigned download URL
        download_url = storage_service.generate_download_presigned_url(
            s3_key=media.s3_key,
            file_name=media.file_name,
            expires_in=3600,  # 1 hour
        )

        expires_at = datetime.utcnow() + timedelta(seconds=3600)

        return MediaDownloadResponse(
            media_id=media.id,
            download_url=download_url,
            file_name=media.file_name,
            mime_type=media.mime_type,
            file_size=media.file_size,
            expires_at=expires_at,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Get download URL error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to generate download URL",
        )


@router.delete(
    "/{media_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete media",
    description="Soft delete media file",
)
async def delete_media(
    media_id: UUID,
    permanent: bool = Query(False, description="Permanently delete from S3"),
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Delete media file.

    - Soft delete: Marks as deleted, keeps in S3
    - Permanent delete: Removes from S3 and database
    """
    try:
        query = select(Media).where(
            Media.id == media_id, Media.user_id == current_user.id
        )

        result = await db.execute(query)
        media = result.scalar_one_or_none()

        if not media:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Media not found"
            )

        if permanent:
            # Delete from S3
            storage_service.delete_object(media.s3_key)

            # Delete from database
            await db.delete(media)
            logger.info(f"Media permanently deleted: {media.id}")
        else:
            # Soft delete
            if media.deleted_at:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Media already deleted",
                )

            media.deleted_at = datetime.utcnow()
            logger.info(f"Media soft deleted: {media.id}")

        await db.commit()
        return None

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Delete media error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to delete media",
        )
