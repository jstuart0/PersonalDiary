"""Entries API endpoints"""

import logging
from datetime import datetime
from typing import Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import and_, func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.models.entry import Entry, EntryEvent, EventType, Tag
from app.models.user import EncryptionTier
from app.routers.auth import get_current_user
from app.schemas.entry import (
    EntryCreate,
    EntryUpdate,
    EntryResponse,
    EntryListResponse,
    EntryEventResponse,
)

logger = logging.getLogger(__name__)

router = APIRouter()


@router.post(
    "/",
    response_model=EntryResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create entry",
    description="Create a new encrypted diary entry",
)
async def create_entry(
    entry_data: EntryCreate,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Create a new diary entry.

    Content must be encrypted client-side before submission.
    """
    try:
        # Create entry
        entry = Entry(
            user_id=current_user.id,
            encrypted_content=entry_data.encrypted_content,
            content_hash=entry_data.content_hash,
            encrypted_title=entry_data.encrypted_title,
            encrypted_location=entry_data.encrypted_location,
            mood=entry_data.mood,
            weather=entry_data.weather,
            source=entry_data.source,
        )

        db.add(entry)
        await db.flush()

        # Add tags
        if entry_data.tag_names:
            for tag_name in entry_data.tag_names:
                tag = Tag(
                    entry_id=entry.id,
                    tag_name=tag_name.lower().strip(),
                    auto_generated=False,
                )
                db.add(tag)

        # Create audit event
        event = EntryEvent(
            entry_id=entry.id,
            event_type=EventType.CREATED,
        )
        db.add(event)

        await db.commit()
        await db.refresh(entry, ["tags"])

        logger.info(f"Entry created: {entry.id} for user {current_user.id}")
        return EntryResponse.model_validate(entry)

    except Exception as e:
        logger.error(f"Entry creation error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create entry",
        )


@router.get(
    "/",
    response_model=EntryListResponse,
    summary="List entries",
    description="Get paginated list of user's entries",
)
async def list_entries(
    page: int = Query(1, ge=1, description="Page number (1-indexed)"),
    page_size: int = Query(20, ge=1, le=100, description="Items per page"),
    include_deleted: bool = Query(False, description="Include soft-deleted entries"),
    source: Optional[str] = Query(None, description="Filter by source"),
    mood: Optional[str] = Query(None, description="Filter by mood"),
    tag: Optional[str] = Query(None, description="Filter by tag name"),
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get paginated list of entries.

    Supports filtering by source, mood, and tags.
    """
    try:
        # Build query
        query = select(Entry).where(Entry.user_id == current_user.id)

        if not include_deleted:
            query = query.where(Entry.deleted_at.is_(None))

        if source:
            query = query.where(Entry.source == source)

        if mood:
            query = query.where(Entry.mood == mood)

        if tag:
            query = query.join(Tag).where(Tag.tag_name == tag.lower().strip())

        # Get total count
        count_query = select(func.count()).select_from(query.subquery())
        total_result = await db.execute(count_query)
        total = total_result.scalar()

        # Get paginated results
        query = (
            query.options(selectinload(Entry.tags))
            .order_by(Entry.created_at.desc())
            .limit(page_size)
            .offset((page - 1) * page_size)
        )

        result = await db.execute(query)
        entries = result.scalars().all()

        total_pages = (total + page_size - 1) // page_size if total > 0 else 0

        return EntryListResponse(
            entries=[EntryResponse.model_validate(e) for e in entries],
            total=total,
            page=page,
            page_size=page_size,
            total_pages=total_pages,
        )

    except Exception as e:
        logger.error(f"List entries error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve entries",
        )


@router.get(
    "/{entry_id}",
    response_model=EntryResponse,
    summary="Get entry",
    description="Get a specific entry by ID",
)
async def get_entry(
    entry_id: UUID,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get a specific entry by ID.

    User must own the entry.
    """
    try:
        query = (
            select(Entry)
            .where(Entry.id == entry_id, Entry.user_id == current_user.id)
            .options(selectinload(Entry.tags))
        )

        result = await db.execute(query)
        entry = result.scalar_one_or_none()

        if not entry:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Entry not found"
            )

        return EntryResponse.model_validate(entry)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Get entry error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve entry",
        )


@router.put(
    "/{entry_id}",
    response_model=EntryResponse,
    summary="Update entry",
    description="Update an existing entry",
)
async def update_entry(
    entry_id: UUID,
    entry_data: EntryUpdate,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Update an existing entry.

    User must own the entry. Only non-null fields will be updated.
    """
    try:
        query = (
            select(Entry)
            .where(Entry.id == entry_id, Entry.user_id == current_user.id)
            .options(selectinload(Entry.tags))
        )

        result = await db.execute(query)
        entry = result.scalar_one_or_none()

        if not entry:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Entry not found"
            )

        if entry.deleted_at:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Cannot update deleted entry",
            )

        # Track changes for audit
        changes = {}

        # Update fields
        if entry_data.encrypted_content is not None:
            changes["encrypted_content"] = True
            entry.encrypted_content = entry_data.encrypted_content

        if entry_data.content_hash is not None:
            entry.content_hash = entry_data.content_hash

        if entry_data.encrypted_title is not None:
            changes["encrypted_title"] = True
            entry.encrypted_title = entry_data.encrypted_title

        if entry_data.encrypted_location is not None:
            changes["encrypted_location"] = True
            entry.encrypted_location = entry_data.encrypted_location

        if entry_data.mood is not None:
            changes["mood"] = entry_data.mood.value
            entry.mood = entry_data.mood

        if entry_data.weather is not None:
            changes["weather"] = True
            entry.weather = entry_data.weather

        # Update tags if provided
        if entry_data.tag_names is not None:
            changes["tags"] = True
            # Remove old tags using delete statement
            from sqlalchemy import delete
            await db.execute(
                delete(Tag).where(Tag.entry_id == entry.id)
            )

            # Clear and add new tags
            entry.tags = []
            for tag_name in entry_data.tag_names:
                tag = Tag(
                    entry_id=entry.id,
                    tag_name=tag_name.lower().strip(),
                    auto_generated=False,
                )
                db.add(tag)
                entry.tags.append(tag)

        entry.updated_at = datetime.utcnow()

        # Create audit event
        event = EntryEvent(
            entry_id=entry.id,
            event_type=EventType.EDITED,
            changes=changes,
        )
        db.add(event)

        await db.commit()
        await db.refresh(entry)

        logger.info(f"Entry updated: {entry.id}")
        return EntryResponse.model_validate(entry)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Update entry error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to update entry",
        )


@router.delete(
    "/{entry_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete entry",
    description="Soft delete an entry",
)
async def delete_entry(
    entry_id: UUID,
    permanent: bool = Query(False, description="Permanently delete (cannot be undone)"),
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Delete an entry (soft delete by default).

    - Soft delete: Sets deleted_at timestamp, can be restored
    - Permanent delete: Removes from database (use with caution)
    """
    try:
        query = select(Entry).where(
            Entry.id == entry_id, Entry.user_id == current_user.id
        )

        result = await db.execute(query)
        entry = result.scalar_one_or_none()

        if not entry:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Entry not found"
            )

        if permanent:
            # Permanent delete
            await db.delete(entry)
            logger.info(f"Entry permanently deleted: {entry.id}")
        else:
            # Soft delete
            if entry.deleted_at:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Entry already deleted",
                )

            entry.deleted_at = datetime.utcnow()

            # Create audit event
            event = EntryEvent(
                entry_id=entry.id,
                event_type=EventType.DELETED,
            )
            db.add(event)

            logger.info(f"Entry soft deleted: {entry.id}")

        await db.commit()
        return None

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Delete entry error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to delete entry",
        )


@router.post(
    "/{entry_id}/restore",
    response_model=EntryResponse,
    summary="Restore entry",
    description="Restore a soft-deleted entry",
)
async def restore_entry(
    entry_id: UUID,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Restore a soft-deleted entry.
    """
    try:
        query = select(Entry).where(
            Entry.id == entry_id, Entry.user_id == current_user.id
        ).options(selectinload(Entry.tags))

        result = await db.execute(query)
        entry = result.scalar_one_or_none()

        if not entry:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Entry not found"
            )

        if not entry.deleted_at:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Entry is not deleted",
            )

        entry.deleted_at = None
        entry.updated_at = datetime.utcnow()

        # Create audit event
        event = EntryEvent(
            entry_id=entry.id,
            event_type=EventType.RESTORED,
        )
        db.add(event)

        await db.commit()
        await db.refresh(entry)

        logger.info(f"Entry restored: {entry.id}")
        return EntryResponse.model_validate(entry)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Restore entry error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to restore entry",
        )


@router.get(
    "/{entry_id}/history",
    response_model=list[EntryEventResponse],
    summary="Get entry history",
    description="Get audit trail of entry changes",
)
async def get_entry_history(
    entry_id: UUID,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get audit trail of all changes to an entry.
    """
    try:
        # Verify entry ownership
        entry_query = select(Entry).where(
            Entry.id == entry_id, Entry.user_id == current_user.id
        )
        entry_result = await db.execute(entry_query)
        entry = entry_result.scalar_one_or_none()

        if not entry:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Entry not found"
            )

        # Get events
        events_query = (
            select(EntryEvent)
            .where(EntryEvent.entry_id == entry_id)
            .order_by(EntryEvent.event_timestamp.desc())
        )

        result = await db.execute(events_query)
        events = result.scalars().all()

        return [EntryEventResponse.model_validate(e) for e in events]

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Get entry history error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve entry history",
        )
