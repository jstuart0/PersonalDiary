"""Search-related Pydantic schemas"""

from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, Field

from app.models.entry import EntryMood, EntrySource


class SearchQuery(BaseModel):
    """Schema for search query (UCE tier only)"""

    query: str = Field(..., min_length=1, max_length=500, description="Search query")
    mood_filter: Optional[EntryMood] = Field(
        None, description="Filter by mood (optional)"
    )
    source_filter: Optional[EntrySource] = Field(
        None, description="Filter by source (optional)"
    )
    tag_filter: Optional[list[str]] = Field(
        None, description="Filter by tags (optional)"
    )
    date_from: Optional[datetime] = Field(
        None, description="Filter entries from this date (optional)"
    )
    date_to: Optional[datetime] = Field(
        None, description="Filter entries until this date (optional)"
    )
    page: int = Field(default=1, ge=1, description="Page number (1-indexed)")
    page_size: int = Field(
        default=20, ge=1, le=100, description="Number of results per page"
    )


class SearchResultItem(BaseModel):
    """Schema for individual search result"""

    entry_id: UUID
    encrypted_content: str
    encrypted_title: Optional[str] = None
    mood: Optional[EntryMood] = None
    source: EntrySource
    created_at: datetime
    updated_at: datetime
    relevance_score: float = Field(
        ..., ge=0.0, le=1.0, description="Search relevance score"
    )
    snippet: Optional[str] = Field(
        None, description="Highlighted snippet (UCE tier only)"
    )

    class Config:
        from_attributes = True


class SearchResponse(BaseModel):
    """Schema for search results response"""

    results: list[SearchResultItem]
    total: int
    page: int
    page_size: int
    total_pages: int
    query_time_ms: float = Field(..., description="Query execution time in milliseconds")


class SearchStatsResponse(BaseModel):
    """Schema for search statistics (UCE tier only)"""

    total_entries: int
    searchable_entries: int
    last_indexed_at: Optional[datetime] = None
    index_size_bytes: Optional[int] = None
