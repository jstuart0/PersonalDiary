"""Social media integration-related Pydantic schemas"""

from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, Field, HttpUrl


class FacebookConnectRequest(BaseModel):
    """Schema for initiating Facebook OAuth flow"""

    redirect_uri: HttpUrl = Field(
        ..., description="Redirect URI after OAuth completion"
    )
    state: Optional[str] = Field(
        None, description="State parameter for CSRF protection"
    )


class FacebookConnectResponse(BaseModel):
    """Schema for Facebook OAuth initiation response"""

    authorization_url: str = Field(..., description="Facebook OAuth authorization URL")
    state: str = Field(..., description="State parameter for verification")


class FacebookCallbackRequest(BaseModel):
    """Schema for Facebook OAuth callback"""

    code: str = Field(..., description="Authorization code from Facebook")
    state: str = Field(..., description="State parameter for verification")


class FacebookCallbackResponse(BaseModel):
    """Schema for Facebook OAuth callback response"""

    integration_account_id: UUID
    provider: str = "facebook"
    account_name: str
    connected_at: datetime
    scopes: list[str] = Field(default_factory=list)


class FacebookPushRequest(BaseModel):
    """Schema for pushing diary entry to Facebook"""

    entry_id: UUID = Field(..., description="Entry ID to push to Facebook")
    post_text: str = Field(..., description="Decrypted text to post to Facebook")
    privacy_setting: str = Field(
        default="SELF", description="Facebook privacy setting (SELF, FRIENDS, PUBLIC)"
    )


class FacebookPushResponse(BaseModel):
    """Schema for Facebook push response"""

    entry_id: UUID
    external_post_id: UUID
    facebook_post_id: str
    posted_at: datetime
    post_url: Optional[str] = None


class FacebookPullRequest(BaseModel):
    """Schema for pulling posts from Facebook"""

    since: Optional[datetime] = Field(
        None, description="Pull posts since this date (optional)"
    )
    limit: int = Field(
        default=100, ge=1, le=500, description="Maximum number of posts to pull"
    )


class FacebookPostItem(BaseModel):
    """Schema for individual Facebook post"""

    facebook_post_id: str
    content: str
    posted_at: datetime
    permalink_url: Optional[str] = None


class FacebookPullResponse(BaseModel):
    """Schema for Facebook pull response"""

    posts: list[FacebookPostItem]
    total_pulled: int
    imported_count: int
    skipped_count: int
    last_pull_at: datetime


class IntegrationAccountResponse(BaseModel):
    """Schema for integration account response"""

    id: UUID
    user_id: UUID
    provider: str
    account_name: str
    is_active: bool
    connected_at: datetime
    last_sync_at: Optional[datetime] = None
    sync_enabled: bool
    scopes: list[str] = Field(default_factory=list)

    class Config:
        from_attributes = True


class IntegrationListResponse(BaseModel):
    """Schema for list of integration accounts"""

    integrations: list[IntegrationAccountResponse]
    total: int


class ExternalPostResponse(BaseModel):
    """Schema for external post metadata"""

    id: UUID
    entry_id: UUID
    integration_account_id: UUID
    external_post_id: str
    posted_at: datetime
    post_url: Optional[str] = None
    sync_direction: str  # "push" or "pull"

    class Config:
        from_attributes = True
