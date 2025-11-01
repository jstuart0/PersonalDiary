"""Social media integration API endpoints"""

import logging
import secrets
from datetime import datetime
from typing import Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.models.entry import Entry, EntrySource
from app.models.integration import ExternalPost, IntegrationAccount
from app.routers.auth import get_current_user
from app.schemas.integration import (
    FacebookCallbackRequest,
    FacebookCallbackResponse,
    FacebookConnectRequest,
    FacebookConnectResponse,
    FacebookPostItem,
    FacebookPullRequest,
    FacebookPullResponse,
    FacebookPushRequest,
    FacebookPushResponse,
    IntegrationAccountResponse,
    IntegrationListResponse,
)
from app.services.facebook import FacebookService

logger = logging.getLogger(__name__)

router = APIRouter()
facebook_service = FacebookService()


@router.post(
    "/facebook/connect",
    response_model=FacebookConnectResponse,
    summary="Initiate Facebook OAuth",
    description="Start Facebook OAuth flow to connect account",
)
async def connect_facebook(
    request: FacebookConnectRequest,
    current_user=Depends(get_current_user),
):
    """
    Initiate Facebook OAuth connection.

    Returns authorization URL to redirect user to Facebook.
    """
    try:
        # Generate state for CSRF protection
        state = request.state if request.state else secrets.token_urlsafe(32)

        # Get authorization URL
        authorization_url = facebook_service.get_authorization_url(state)

        logger.info(f"Facebook OAuth initiated for user {current_user.id}")

        return FacebookConnectResponse(
            authorization_url=authorization_url,
            state=state,
        )

    except Exception as e:
        logger.error(f"Facebook connect error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to initiate Facebook connection",
        )


@router.post(
    "/facebook/callback",
    response_model=FacebookCallbackResponse,
    summary="Facebook OAuth callback",
    description="Complete Facebook OAuth flow and store access token",
)
async def facebook_callback(
    request: FacebookCallbackRequest,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Handle Facebook OAuth callback.

    Exchanges authorization code for access token and stores integration account.
    """
    try:
        # Exchange code for token
        token_data = await facebook_service.exchange_code_for_token(request.code)
        access_token = token_data["access_token"]

        # Get user info from Facebook
        user_info = await facebook_service.get_user_info(access_token)

        # Check if integration already exists
        existing_query = select(IntegrationAccount).where(
            IntegrationAccount.user_id == current_user.id,
            IntegrationAccount.provider == "facebook",
            IntegrationAccount.provider_account_id == user_info["id"],
        )
        existing_result = await db.execute(existing_query)
        existing_integration = existing_result.scalar_one_or_none()

        if existing_integration:
            # Update existing integration
            existing_integration.access_token = access_token
            existing_integration.refresh_token = token_data.get("refresh_token")
            existing_integration.token_expires_at = (
                datetime.utcnow() + timedelta(seconds=token_data.get("expires_in", 0))
                if token_data.get("expires_in")
                else None
            )
            existing_integration.account_name = user_info.get("name", "Facebook User")
            existing_integration.is_active = True
            integration = existing_integration
        else:
            # Create new integration
            from datetime import timedelta

            integration = IntegrationAccount(
                user_id=current_user.id,
                provider="facebook",
                provider_account_id=user_info["id"],
                account_name=user_info.get("name", "Facebook User"),
                access_token=access_token,
                refresh_token=token_data.get("refresh_token"),
                token_expires_at=(
                    datetime.utcnow() + timedelta(seconds=token_data.get("expires_in", 0))
                    if token_data.get("expires_in")
                    else None
                ),
                scopes=["email", "public_profile", "user_posts"],
                is_active=True,
            )
            db.add(integration)

        await db.commit()
        await db.refresh(integration)

        logger.info(
            f"Facebook account connected for user {current_user.id}: {user_info['id']}"
        )

        return FacebookCallbackResponse(
            integration_account_id=integration.id,
            provider="facebook",
            account_name=integration.account_name,
            connected_at=integration.connected_at,
            scopes=integration.scopes or [],
        )

    except Exception as e:
        logger.error(f"Facebook callback error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to complete Facebook connection: {str(e)}",
        )


@router.post(
    "/facebook/push",
    response_model=FacebookPushResponse,
    summary="Push entry to Facebook",
    description="Publish diary entry to Facebook",
)
async def push_to_facebook(
    request: FacebookPushRequest,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Push a diary entry to Facebook.

    Entry content must be decrypted client-side before sending.
    """
    try:
        # Get entry
        entry_query = (
            select(Entry)
            .where(Entry.id == request.entry_id, Entry.user_id == current_user.id)
            .options(selectinload(Entry.external_post))
        )
        entry_result = await db.execute(entry_query)
        entry = entry_result.scalar_one_or_none()

        if not entry:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Entry not found"
            )

        # Check if already published
        if entry.external_post:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Entry already published to Facebook",
            )

        # Get active Facebook integration
        integration_query = select(IntegrationAccount).where(
            IntegrationAccount.user_id == current_user.id,
            IntegrationAccount.provider == "facebook",
            IntegrationAccount.is_active == True,
        )
        integration_result = await db.execute(integration_query)
        integration = integration_result.scalar_one_or_none()

        if not integration:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No active Facebook integration found. Please connect Facebook first.",
            )

        # Publish to Facebook
        post_result = await facebook_service.publish_post(
            access_token=integration.access_token or "",
            message=request.post_text,
            privacy=request.privacy_setting,
        )

        facebook_post_id = post_result["id"]

        # Get post details for URL
        post_details = await facebook_service.get_post_details(
            access_token=integration.access_token or "",
            post_id=facebook_post_id,
        )

        # Create external post record
        external_post = ExternalPost(
            entry_id=entry.id,
            integration_account_id=integration.id,
            external_post_id=facebook_post_id,
            post_url=post_details.get("permalink_url"),
            sync_direction="push",
        )
        db.add(external_post)

        # Update integration last_sync_at
        integration.last_sync_at = datetime.utcnow()

        await db.commit()
        await db.refresh(external_post)

        logger.info(
            f"Entry {entry.id} pushed to Facebook: {facebook_post_id} "
            f"for user {current_user.id}"
        )

        return FacebookPushResponse(
            entry_id=entry.id,
            external_post_id=external_post.id,
            facebook_post_id=facebook_post_id,
            posted_at=external_post.posted_at,
            post_url=external_post.post_url,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Facebook push error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to push to Facebook: {str(e)}",
        )


@router.post(
    "/facebook/pull",
    response_model=FacebookPullResponse,
    summary="Pull posts from Facebook",
    description="Import Facebook posts as diary entries",
)
async def pull_from_facebook(
    request: FacebookPullRequest,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Pull posts from Facebook and import as diary entries.

    Posts are stored as encrypted entries with source=facebook.
    This should ideally run as a background job via Celery.
    """
    try:
        # Get active Facebook integration
        integration_query = select(IntegrationAccount).where(
            IntegrationAccount.user_id == current_user.id,
            IntegrationAccount.provider == "facebook",
            IntegrationAccount.is_active == True,
        )
        integration_result = await db.execute(integration_query)
        integration = integration_result.scalar_one_or_none()

        if not integration:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No active Facebook integration found",
            )

        # Fetch posts from Facebook
        posts = await facebook_service.get_user_posts(
            access_token=integration.access_token or "",
            since=request.since,
            limit=request.limit,
        )

        imported_count = 0
        skipped_count = 0
        post_items = []

        for post in posts:
            post_id = post["id"]
            message = post.get("message", "")
            created_time = datetime.fromisoformat(
                post["created_time"].replace("Z", "+00:00")
            )
            permalink_url = post.get("permalink_url")

            # Check if already imported
            existing_query = select(ExternalPost).where(
                ExternalPost.integration_account_id == integration.id,
                ExternalPost.external_post_id == post_id,
            )
            existing_result = await db.execute(existing_query)
            existing_post = existing_result.scalar_one_or_none()

            if existing_post:
                skipped_count += 1
                continue

            # TODO: Encrypt the message before storing
            # For now, we're storing it as-is (this should be encrypted client-side
            # or use UCE encryption server-side)

            # Create entry
            # Note: In production, content should be encrypted
            # This is a simplified implementation
            from app.services.encryption import UCEEncryption

            uce = UCEEncryption()
            content_hash = uce.compute_content_hash(message)

            entry = Entry(
                user_id=current_user.id,
                encrypted_content=message,  # TODO: Encrypt this
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
                post_url=permalink_url,
                sync_direction="pull",
                posted_at=created_time,
            )
            db.add(external_post)

            imported_count += 1

            post_items.append(
                FacebookPostItem(
                    facebook_post_id=post_id,
                    content=message,
                    posted_at=created_time,
                    permalink_url=permalink_url,
                )
            )

        # Update integration last_sync_at
        integration.last_sync_at = datetime.utcnow()

        await db.commit()

        logger.info(
            f"Facebook pull completed for user {current_user.id}: "
            f"{imported_count} imported, {skipped_count} skipped"
        )

        return FacebookPullResponse(
            posts=post_items,
            total_pulled=len(posts),
            imported_count=imported_count,
            skipped_count=skipped_count,
            last_pull_at=integration.last_sync_at,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Facebook pull error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to pull from Facebook: {str(e)}",
        )


@router.get(
    "/",
    response_model=IntegrationListResponse,
    summary="List integrations",
    description="Get all connected social media integrations",
)
async def list_integrations(
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get list of all connected social media integrations for current user.
    """
    try:
        query = select(IntegrationAccount).where(
            IntegrationAccount.user_id == current_user.id
        )

        result = await db.execute(query)
        integrations = result.scalars().all()

        return IntegrationListResponse(
            integrations=[
                IntegrationAccountResponse.model_validate(i) for i in integrations
            ],
            total=len(integrations),
        )

    except Exception as e:
        logger.error(f"List integrations error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve integrations",
        )


@router.delete(
    "/{integration_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Disconnect integration",
    description="Disconnect and remove social media integration",
)
async def disconnect_integration(
    integration_id: UUID,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Disconnect social media integration.

    This will deactivate the integration but keep historical data.
    """
    try:
        query = select(IntegrationAccount).where(
            IntegrationAccount.id == integration_id,
            IntegrationAccount.user_id == current_user.id,
        )

        result = await db.execute(query)
        integration = result.scalar_one_or_none()

        if not integration:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Integration not found",
            )

        # Deactivate instead of deleting (keep historical data)
        integration.is_active = False
        integration.access_token = None  # Remove sensitive token
        integration.refresh_token = None

        await db.commit()

        logger.info(f"Integration disconnected: {integration_id} for user {current_user.id}")

        return None

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Disconnect integration error: {str(e)}")
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to disconnect integration",
        )
