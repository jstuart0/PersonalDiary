"""Facebook OAuth and API integration service"""

import logging
from datetime import datetime
from typing import Optional
from uuid import UUID

import httpx
from authlib.integrations.httpx_client import OAuth2Client

from app.config import settings

logger = logging.getLogger(__name__)


class FacebookService:
    """
    Facebook OAuth and API integration service.

    Handles:
    - OAuth2 authentication flow
    - Fetching user posts
    - Publishing posts to Facebook
    """

    def __init__(self):
        self.client_id = settings.facebook_client_id
        self.client_secret = settings.facebook_client_secret
        self.redirect_uri = settings.facebook_redirect_uri
        self.authorization_endpoint = "https://www.facebook.com/v18.0/dialog/oauth"
        self.token_endpoint = "https://graph.facebook.com/v18.0/oauth/access_token"
        self.api_base_url = "https://graph.facebook.com/v18.0"

    def get_authorization_url(self, state: str) -> str:
        """
        Generate Facebook OAuth authorization URL.

        Args:
            state: State parameter for CSRF protection

        Returns:
            Authorization URL to redirect user to
        """
        scopes = [
            "email",
            "public_profile",
            "user_posts",  # Read user's posts
            "publish_to_groups",  # Publish posts (if applicable)
        ]

        params = {
            "client_id": self.client_id,
            "redirect_uri": self.redirect_uri,
            "state": state,
            "scope": ",".join(scopes),
            "response_type": "code",
        }

        query_string = "&".join([f"{k}={v}" for k, v in params.items()])
        return f"{self.authorization_endpoint}?{query_string}"

    async def exchange_code_for_token(self, code: str) -> dict:
        """
        Exchange authorization code for access token.

        Args:
            code: Authorization code from Facebook callback

        Returns:
            Dict with access_token, token_type, expires_in

        Raises:
            Exception: If token exchange fails
        """
        async with httpx.AsyncClient() as client:
            response = await client.post(
                self.token_endpoint,
                data={
                    "client_id": self.client_id,
                    "client_secret": self.client_secret,
                    "redirect_uri": self.redirect_uri,
                    "code": code,
                },
            )

            if response.status_code != 200:
                logger.error(f"Token exchange failed: {response.text}")
                raise Exception(f"Failed to exchange code for token: {response.text}")

            return response.json()

    async def get_user_info(self, access_token: str) -> dict:
        """
        Get Facebook user information.

        Args:
            access_token: Facebook access token

        Returns:
            Dict with user info (id, name, email)
        """
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.api_base_url}/me",
                params={"fields": "id,name,email", "access_token": access_token},
            )

            if response.status_code != 200:
                logger.error(f"Get user info failed: {response.text}")
                raise Exception(f"Failed to get user info: {response.text}")

            return response.json()

    async def get_user_posts(
        self, access_token: str, since: Optional[datetime] = None, limit: int = 100
    ) -> list[dict]:
        """
        Fetch user's Facebook posts.

        Args:
            access_token: Facebook access token
            since: Only fetch posts after this date (optional)
            limit: Maximum number of posts to fetch

        Returns:
            List of post dicts with id, message, created_time, permalink_url
        """
        params = {
            "fields": "id,message,created_time,permalink_url",
            "limit": limit,
            "access_token": access_token,
        }

        if since:
            params["since"] = int(since.timestamp())

        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.api_base_url}/me/posts",
                params=params,
            )

            if response.status_code != 200:
                logger.error(f"Get posts failed: {response.text}")
                raise Exception(f"Failed to fetch posts: {response.text}")

            data = response.json()
            return data.get("data", [])

    async def publish_post(
        self, access_token: str, message: str, privacy: str = "SELF"
    ) -> dict:
        """
        Publish a post to Facebook.

        Args:
            access_token: Facebook access token
            message: Post content
            privacy: Privacy setting (SELF, FRIENDS, PUBLIC)

        Returns:
            Dict with post_id

        Raises:
            Exception: If publishing fails
        """
        privacy_settings = {
            "SELF": {"value": "SELF"},
            "FRIENDS": {"value": "ALL_FRIENDS"},
            "PUBLIC": {"value": "EVERYONE"},
        }

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.api_base_url}/me/feed",
                data={
                    "message": message,
                    "privacy": str(privacy_settings.get(privacy, {"value": "SELF"})),
                    "access_token": access_token,
                },
            )

            if response.status_code not in [200, 201]:
                logger.error(f"Publish post failed: {response.text}")
                raise Exception(f"Failed to publish post: {response.text}")

            return response.json()

    async def get_post_details(self, access_token: str, post_id: str) -> dict:
        """
        Get details of a specific post.

        Args:
            access_token: Facebook access token
            post_id: Facebook post ID

        Returns:
            Dict with post details
        """
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.api_base_url}/{post_id}",
                params={
                    "fields": "id,message,created_time,permalink_url",
                    "access_token": access_token,
                },
            )

            if response.status_code != 200:
                logger.error(f"Get post details failed: {response.text}")
                raise Exception(f"Failed to get post details: {response.text}")

            return response.json()

    async def delete_post(self, access_token: str, post_id: str) -> bool:
        """
        Delete a Facebook post.

        Args:
            access_token: Facebook access token
            post_id: Facebook post ID to delete

        Returns:
            True if successful
        """
        async with httpx.AsyncClient() as client:
            response = await client.delete(
                f"{self.api_base_url}/{post_id}",
                params={"access_token": access_token},
            )

            if response.status_code != 200:
                logger.error(f"Delete post failed: {response.text}")
                return False

            return True
