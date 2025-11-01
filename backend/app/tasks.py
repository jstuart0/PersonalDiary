"""Celery background tasks"""

import logging
from datetime import datetime, timedelta
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker

from app.celery_app import celery_app
from app.config import settings
from app.models.entry import Entry, EntrySource
from app.models.integration import ExternalPost, IntegrationAccount
from app.models.media import Media
from app.services.encryption import UCEEncryption
from app.services.facebook import FacebookService

logger = logging.getLogger(__name__)

# Create async database engine for tasks
engine = create_async_engine(settings.database_url, echo=False)
async_session_maker = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

facebook_service = FacebookService()
uce_encryption = UCEEncryption()


async def get_db():
    """Get async database session for tasks"""
    async with async_session_maker() as session:
        yield session


@celery_app.task(name="app.tasks.sync_facebook_posts")
def sync_facebook_posts():
    """
    Periodic task to sync Facebook posts for all active integrations.

    Runs every hour to pull new posts from Facebook.
    """
    import asyncio

    asyncio.run(_sync_facebook_posts_async())


async def _sync_facebook_posts_async():
    """Async implementation of Facebook sync"""
    logger.info("Starting Facebook posts sync")

    async with async_session_maker() as db:
        try:
            # Get all active Facebook integrations with sync enabled
            query = select(IntegrationAccount).where(
                IntegrationAccount.provider == "facebook",
                IntegrationAccount.is_active == True,
                IntegrationAccount.sync_enabled == True,
            )

            result = await db.execute(query)
            integrations = result.scalars().all()

            total_synced = 0
            total_imported = 0

            for integration in integrations:
                try:
                    # Fetch posts since last sync
                    since = integration.last_sync_at or (
                        datetime.utcnow() - timedelta(days=30)
                    )

                    posts = await facebook_service.get_user_posts(
                        access_token=integration.access_token or "",
                        since=since,
                        limit=100,
                    )

                    imported_count = 0

                    for post in posts:
                        post_id = post["id"]
                        message = post.get("message", "")
                        created_time = datetime.fromisoformat(
                            post["created_time"].replace("Z", "+00:00")
                        )

                        # Check if already imported
                        existing_query = select(ExternalPost).where(
                            ExternalPost.integration_account_id == integration.id,
                            ExternalPost.external_post_id == post_id,
                        )
                        existing_result = await db.execute(existing_query)
                        if existing_result.scalar_one_or_none():
                            continue

                        # Create entry
                        content_hash = uce_encryption.compute_content_hash(message)

                        entry = Entry(
                            user_id=integration.user_id,
                            encrypted_content=message,  # TODO: Encrypt
                            content_hash=content_hash,
                            source=EntrySource.FACEBOOK,
                            created_at=created_time,
                        )
                        db.add(entry)
                        await db.flush()

                        # Create external post record
                        external_post = ExternalPost(
                            entry_id=entry.id,
                            integration_account_id=integration.id,
                            external_post_id=post_id,
                            post_url=post.get("permalink_url"),
                            sync_direction="pull",
                            posted_at=created_time,
                        )
                        db.add(external_post)

                        imported_count += 1

                    # Update last sync time
                    integration.last_sync_at = datetime.utcnow()
                    total_synced += 1
                    total_imported += imported_count

                    logger.info(
                        f"Synced Facebook for user {integration.user_id}: "
                        f"{imported_count} new posts"
                    )

                except Exception as e:
                    logger.error(
                        f"Failed to sync Facebook for integration {integration.id}: {str(e)}"
                    )
                    continue

            await db.commit()

            logger.info(
                f"Facebook sync completed: {total_synced} accounts, "
                f"{total_imported} posts imported"
            )

        except Exception as e:
            logger.error(f"Facebook sync task failed: {str(e)}")
            await db.rollback()


@celery_app.task(name="app.tasks.cleanup_expired_media")
def cleanup_expired_media():
    """
    Periodic task to clean up soft-deleted media older than 30 days.

    Permanently deletes media from S3 and database.
    """
    import asyncio

    asyncio.run(_cleanup_expired_media_async())


async def _cleanup_expired_media_async():
    """Async implementation of media cleanup"""
    logger.info("Starting expired media cleanup")

    async with async_session_maker() as db:
        try:
            # Find media deleted more than 30 days ago
            cutoff_date = datetime.utcnow() - timedelta(days=30)

            query = select(Media).where(
                Media.deleted_at.isnot(None), Media.deleted_at < cutoff_date
            )

            result = await db.execute(query)
            expired_media = result.scalars().all()

            deleted_count = 0

            from app.services.storage import S3StorageService

            storage_service = S3StorageService()

            for media in expired_media:
                try:
                    # Delete from S3
                    if storage_service.delete_object(media.s3_key):
                        # Delete from database
                        await db.delete(media)
                        deleted_count += 1
                        logger.info(f"Deleted expired media: {media.id}")
                except Exception as e:
                    logger.error(f"Failed to delete media {media.id}: {str(e)}")
                    continue

            await db.commit()

            logger.info(f"Media cleanup completed: {deleted_count} files deleted")

        except Exception as e:
            logger.error(f"Media cleanup task failed: {str(e)}")
            await db.rollback()


@celery_app.task(name="app.tasks.rebuild_search_index")
def rebuild_search_index(user_id: str):
    """
    Background task to rebuild search index for a specific user.

    Args:
        user_id: User ID (as string)
    """
    import asyncio

    asyncio.run(_rebuild_search_index_async(UUID(user_id)))


async def _rebuild_search_index_async(user_id: UUID):
    """Async implementation of search index rebuild"""
    logger.info(f"Rebuilding search index for user {user_id}")

    async with async_session_maker() as db:
        try:
            # Get all user entries
            query = select(Entry).where(
                Entry.user_id == user_id, Entry.deleted_at.is_(None)
            )

            result = await db.execute(query)
            entries = result.scalars().all()

            indexed_count = 0

            for entry in entries:
                # TODO: Decrypt content and update search_vector
                # For now, this is a placeholder
                # In production, this would:
                # 1. Get user's master key
                # 2. Decrypt entry content
                # 3. Update search_vector using PostgreSQL's to_tsvector
                indexed_count += 1

            await db.commit()

            logger.info(f"Search index rebuilt for user {user_id}: {indexed_count} entries")

        except Exception as e:
            logger.error(f"Search index rebuild failed for user {user_id}: {str(e)}")
            await db.rollback()
