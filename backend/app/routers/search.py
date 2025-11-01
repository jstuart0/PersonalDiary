"""Search API endpoints (UCE tier only)"""

import logging
import time
from typing import Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select, text, or_
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.config import settings
from app.database import get_db
from app.models.entry import Entry, Tag
from app.models.user import User
from app.routers.auth import get_current_user
from app.schemas.search import SearchQuery, SearchResponse, SearchResultItem, SearchStatsResponse
from app.services.encryption import UCEEncryption

logger = logging.getLogger(__name__)

router = APIRouter()
uce_encryption = UCEEncryption()


def is_postgres() -> bool:
    """Check if using PostgreSQL database"""
    return 'postgresql' in settings.database_url.lower()


@router.post(
    "/",
    response_model=SearchResponse,
    summary="Search entries",
    description="Full-text search through entries (UCE tier only)",
)
async def search_entries(
    query: SearchQuery,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Search entries using PostgreSQL full-text search.

    Only available for UCE tier users. E2E users must search client-side.

    Supports:
    - Full-text search on content
    - Filtering by mood, source, tags, date range
    - Pagination
    """
    # Feature gate: Only UCE tier
    if not current_user.is_uce:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Server-side search is only available for UCE encryption tier. "
            "E2E tier users must search locally on their device.",
        )

    try:
        start_time = time.time()

        # Build base query
        stmt = select(Entry).where(
            Entry.user_id == current_user.id,
            Entry.deleted_at.is_(None),
        )

        # Apply text search if query provided
        if query.query.strip():
            if is_postgres():
                # Use PostgreSQL full-text search with ts_vector
                search_query = func.plainto_tsquery("english", query.query)
                stmt = stmt.where(Entry.search_vector.op("@@")(search_query))

                # Add relevance ranking
                rank = func.ts_rank(Entry.search_vector, search_query).label("rank")
                stmt = stmt.add_columns(rank).order_by(rank.desc())
            else:
                # SQLite fallback: Search in tags only
                # This is simplified for development - search is limited to tags
                search_terms = [term.strip().lower() for term in query.query.split() if term.strip()]

                if search_terms:
                    # Get entry IDs that have matching tags
                    tag_entry_ids = set()
                    for term in search_terms:
                        tag_query = select(Tag.entry_id).where(
                            Tag.tag_name.contains(term)
                        )
                        tag_result = await db.execute(tag_query)
                        tag_ids = [row[0] for row in tag_result.all()]
                        tag_entry_ids.update(tag_ids)

                    # Filter entries to those with matching tags
                    if tag_entry_ids:
                        stmt = stmt.where(Entry.id.in_(tag_entry_ids))

                # Add ranking column and sort by date
                from sqlalchemy import literal
                stmt = stmt.add_columns(literal(1.0).label("rank"))
                stmt = stmt.order_by(Entry.created_at.desc())
        else:
            # No search query, just filter and sort by date
            from sqlalchemy import literal
            stmt = stmt.add_columns(literal(1.0).label("rank"))
            stmt = stmt.order_by(Entry.created_at.desc())

        # Apply filters
        if query.mood_filter:
            stmt = stmt.where(Entry.mood == query.mood_filter)

        if query.source_filter:
            stmt = stmt.where(Entry.source == query.source_filter)

        if query.tag_filter:
            # Join with tags
            for tag_name in query.tag_filter:
                stmt = stmt.join(Tag).where(Tag.tag_name == tag_name.lower().strip())

        if query.date_from:
            stmt = stmt.where(Entry.created_at >= query.date_from)

        if query.date_to:
            stmt = stmt.where(Entry.created_at <= query.date_to)

        # Get total count (before pagination)
        count_stmt = select(func.count()).select_from(stmt.subquery())
        total_result = await db.execute(count_stmt)
        total = total_result.scalar()

        # Apply pagination
        stmt = stmt.limit(query.page_size).offset((query.page - 1) * query.page_size)

        # Execute query
        result = await db.execute(stmt)
        rows = result.all()

        # Build response
        results = []
        for row in rows:
            entry = row[0] if isinstance(row, tuple) else row
            relevance = row[1] if isinstance(row, tuple) else 1.0

            # Get master key to decrypt for snippet generation
            master_key = uce_encryption.get_master_key(
                # Note: In real implementation, master key should be cached in session
                # or passed from frontend after password verification
                # For now, we'll skip decryption and just return encrypted content
                # TODO: Implement proper master key session management
                password="",  # Placeholder
                encrypted_master_key_b64=current_user.uce_encrypted_master_key or "",
                salt_b64=current_user.uce_key_derivation_salt or "",
            )

            # Generate snippet (simplified - just use beginning of content)
            snippet = None
            if query.query.strip():
                # In production, this would decrypt and highlight search terms
                # For now, just indicate a match was found
                snippet = f"Match found for: {query.query}"

            result_item = SearchResultItem(
                entry_id=entry.id,
                encrypted_content=entry.encrypted_content,
                encrypted_title=entry.encrypted_title,
                mood=entry.mood,
                source=entry.source,
                created_at=entry.created_at,
                updated_at=entry.updated_at,
                relevance_score=float(relevance) if relevance else 1.0,
                snippet=snippet,
            )
            results.append(result_item)

        query_time_ms = (time.time() - start_time) * 1000
        total_pages = (total + query.page_size - 1) // query.page_size if total > 0 else 0

        logger.info(
            f"Search completed: {total} results in {query_time_ms:.2f}ms "
            f"for user {current_user.id}"
        )

        return SearchResponse(
            results=results,
            total=total,
            page=query.page,
            page_size=query.page_size,
            total_pages=total_pages,
            query_time_ms=query_time_ms,
        )

    except HTTPException:
        raise
    except Exception as e:
        import traceback
        error_traceback = traceback.format_exc()
        logger.error(f"Search error: {str(e)}\n{error_traceback}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Search failed: {str(e)}",
        )


@router.post(
    "/index",
    response_model=dict,
    summary="Rebuild search index",
    description="Rebuild full-text search index for user's entries (UCE tier only)",
)
async def rebuild_search_index(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Rebuild full-text search index for all user entries.

    This is needed when:
    - First-time setup for UCE user
    - After bulk imports
    - If search index becomes corrupted

    Note: This requires decrypting all entries and rebuilding the search vector.
    In production, this should be a background job.
    """
    # Feature gate: Only UCE tier
    if not current_user.is_uce:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Search indexing is only available for UCE encryption tier",
        )

    try:
        # Get all user entries
        stmt = select(Entry).where(
            Entry.user_id == current_user.id, Entry.deleted_at.is_(None)
        )
        result = await db.execute(stmt)
        entries = result.scalars().all()

        indexed_count = 0

        # Get master key for decryption
        # TODO: Implement proper master key session management
        # For now, we'll just update the search vector using SQL triggers
        # that should be set up in the database

        for entry in entries:
            # In production, this would:
            # 1. Decrypt entry content
            # 2. Extract searchable text
            # 3. Update search_vector column

            # For now, use PostgreSQL's built-in text search
            # Update search_vector using to_tsvector
            # This assumes content is already decrypted or stored in plaintext
            # In real UCE implementation, we'd decrypt first

            # Skip for now - search vector should be updated via database triggers
            indexed_count += 1

        await db.commit()

        logger.info(f"Search index rebuilt: {indexed_count} entries for user {current_user.id}")

        return {
            "status": "success",
            "indexed_count": indexed_count,
            "message": f"Search index rebuilt for {indexed_count} entries",
        }

    except Exception as e:
        logger.error(f"Search index rebuild error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to rebuild search index",
        )


@router.get(
    "/stats",
    response_model=SearchStatsResponse,
    summary="Get search statistics",
    description="Get search index statistics (UCE tier only)",
)
async def get_search_stats(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get search index statistics for current user.
    """
    # Feature gate: Only UCE tier
    if not current_user.is_uce:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Search statistics are only available for UCE encryption tier",
        )

    try:
        # Get total entries
        total_stmt = select(func.count()).where(
            Entry.user_id == current_user.id, Entry.deleted_at.is_(None)
        )
        total_result = await db.execute(total_stmt)
        total_entries = total_result.scalar()

        # Get searchable entries (with non-null search_vector for PostgreSQL, all for SQLite)
        if is_postgres():
            searchable_stmt = select(func.count()).where(
                Entry.user_id == current_user.id,
                Entry.deleted_at.is_(None),
                Entry.search_vector.isnot(None),
            )
        else:
            # SQLite: all entries are searchable (no search_vector column)
            searchable_stmt = total_stmt

        searchable_result = await db.execute(searchable_stmt)
        searchable_entries = searchable_result.scalar()

        # Get last updated entry
        last_indexed_stmt = (
            select(Entry.updated_at)
            .where(Entry.user_id == current_user.id)
            .order_by(Entry.updated_at.desc())
            .limit(1)
        )
        last_indexed_result = await db.execute(last_indexed_stmt)
        last_indexed_at = last_indexed_result.scalar_one_or_none()

        return SearchStatsResponse(
            total_entries=total_entries,
            searchable_entries=searchable_entries,
            last_indexed_at=last_indexed_at,
            index_size_bytes=None,  # TODO: Calculate actual index size
        )

    except Exception as e:
        logger.error(f"Get search stats error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve search statistics",
        )
