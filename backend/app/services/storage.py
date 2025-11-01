"""S3 storage service for media files"""

import logging
from datetime import datetime, timedelta
from typing import Optional
from uuid import UUID, uuid4

import boto3
from botocore.client import Config
from botocore.exceptions import BotoCoreError, ClientError

from app.config import settings

logger = logging.getLogger(__name__)


class S3StorageService:
    """
    S3 storage service for media uploads and downloads.

    Handles presigned URL generation for secure client-side uploads/downloads.
    """

    def __init__(self):
        self.s3_client = boto3.client(
            "s3",
            aws_access_key_id=settings.aws_access_key_id,
            aws_secret_access_key=settings.aws_secret_access_key,
            region_name=settings.aws_region,
            endpoint_url=settings.aws_s3_endpoint_url,
            config=Config(signature_version="s3v4"),
        )
        self.bucket = settings.aws_s3_bucket

    def generate_s3_key(self, user_id: UUID, media_id: UUID, file_name: str) -> str:
        """
        Generate S3 object key with organized structure.

        Format: {user_id}/{year}/{month}/{media_id}/{filename}

        Args:
            user_id: User ID
            media_id: Media ID
            file_name: Original filename

        Returns:
            S3 object key
        """
        now = datetime.utcnow()
        year = now.strftime("%Y")
        month = now.strftime("%m")

        # Sanitize filename
        safe_filename = "".join(
            c for c in file_name if c.isalnum() or c in (".", "-", "_")
        )

        return f"{user_id}/{year}/{month}/{media_id}/{safe_filename}"

    def generate_upload_presigned_url(
        self,
        s3_key: str,
        mime_type: str,
        file_size: int,
        expires_in: int = 3600,
    ) -> dict:
        """
        Generate presigned URL for uploading media to S3.

        Args:
            s3_key: S3 object key
            mime_type: MIME type of file
            file_size: File size in bytes
            expires_in: URL expiration time in seconds (default: 1 hour)

        Returns:
            Dict with presigned URL and fields

        Raises:
            Exception: If S3 operation fails
        """
        try:
            # Generate presigned POST URL
            presigned_post = self.s3_client.generate_presigned_post(
                Bucket=self.bucket,
                Key=s3_key,
                Fields={
                    "Content-Type": mime_type,
                },
                Conditions=[
                    {"Content-Type": mime_type},
                    ["content-length-range", 1, file_size + 1024],  # Allow small overhead
                ],
                ExpiresIn=expires_in,
            )

            return {
                "url": presigned_post["url"],
                "fields": presigned_post["fields"],
            }

        except (BotoCoreError, ClientError) as e:
            logger.error(f"Failed to generate upload presigned URL: {str(e)}")
            raise Exception(f"S3 upload URL generation failed: {str(e)}")

    def generate_download_presigned_url(
        self,
        s3_key: str,
        file_name: Optional[str] = None,
        expires_in: int = 3600,
    ) -> str:
        """
        Generate presigned URL for downloading media from S3.

        Args:
            s3_key: S3 object key
            file_name: Optional filename for download (Content-Disposition)
            expires_in: URL expiration time in seconds (default: 1 hour)

        Returns:
            Presigned download URL

        Raises:
            Exception: If S3 operation fails
        """
        try:
            params = {
                "Bucket": self.bucket,
                "Key": s3_key,
            }

            # Set Content-Disposition for download with custom filename
            if file_name:
                params["ResponseContentDisposition"] = f'attachment; filename="{file_name}"'

            presigned_url = self.s3_client.generate_presigned_url(
                "get_object",
                Params=params,
                ExpiresIn=expires_in,
            )

            return presigned_url

        except (BotoCoreError, ClientError) as e:
            logger.error(f"Failed to generate download presigned URL: {str(e)}")
            raise Exception(f"S3 download URL generation failed: {str(e)}")

    def delete_object(self, s3_key: str) -> bool:
        """
        Delete object from S3.

        Args:
            s3_key: S3 object key to delete

        Returns:
            True if successful, False otherwise
        """
        try:
            self.s3_client.delete_object(Bucket=self.bucket, Key=s3_key)
            logger.info(f"Deleted S3 object: {s3_key}")
            return True

        except (BotoCoreError, ClientError) as e:
            logger.error(f"Failed to delete S3 object {s3_key}: {str(e)}")
            return False

    def object_exists(self, s3_key: str) -> bool:
        """
        Check if object exists in S3.

        Args:
            s3_key: S3 object key

        Returns:
            True if exists, False otherwise
        """
        try:
            self.s3_client.head_object(Bucket=self.bucket, Key=s3_key)
            return True
        except ClientError:
            return False

    def get_object_metadata(self, s3_key: str) -> Optional[dict]:
        """
        Get object metadata from S3.

        Args:
            s3_key: S3 object key

        Returns:
            Metadata dict if object exists, None otherwise
        """
        try:
            response = self.s3_client.head_object(Bucket=self.bucket, Key=s3_key)
            return {
                "content_length": response.get("ContentLength"),
                "content_type": response.get("ContentType"),
                "last_modified": response.get("LastModified"),
                "etag": response.get("ETag"),
            }
        except ClientError as e:
            logger.warning(f"Failed to get metadata for {s3_key}: {str(e)}")
            return None
