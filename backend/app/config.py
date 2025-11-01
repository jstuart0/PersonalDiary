"""Application configuration management"""

from functools import lru_cache
from typing import List

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    # Application Configuration
    app_name: str = "Personal Diary API"
    app_env: str = "development"
    debug: bool = False
    api_v1_prefix: str = "/api/v1"

    # Server Configuration
    host: str = "0.0.0.0"
    port: int = 8000

    # Security
    secret_key: str
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 15
    refresh_token_expire_days: int = 30

    # Database Configuration
    database_url: str
    database_pool_size: int = 10
    database_max_overflow: int = 20

    # Redis Configuration
    redis_url: str = "redis://localhost:6379/0"

    # Celery Configuration
    celery_broker_url: str = "redis://localhost:6379/1"
    celery_result_backend: str = "redis://localhost:6379/2"

    # AWS S3 Configuration
    aws_access_key_id: str = ""
    aws_secret_access_key: str = ""
    aws_region: str = "us-east-1"
    aws_s3_bucket: str = ""
    aws_s3_endpoint_url: str | None = None

    # Facebook OAuth Configuration
    facebook_client_id: str = ""
    facebook_client_secret: str = ""
    facebook_redirect_uri: str = ""

    # Encryption Configuration
    encryption_key_algorithm: str = "X25519"
    symmetric_cipher_algorithm: str = "ChaCha20-Poly1305"
    key_derivation_algorithm: str = "Argon2id"
    argon2_time_cost: int = 2
    argon2_memory_cost: int = 65536  # 64MB
    argon2_parallelism: int = 1

    # Storage Limits
    free_tier_storage_bytes: int = 1073741824  # 1GB
    paid_tier_storage_bytes: int = 53687091200  # 50GB
    max_file_size_bytes: int = 52428800  # 50MB

    # Rate Limiting
    rate_limit_auth_per_minute: int = 5
    rate_limit_api_per_minute: int = 100
    rate_limit_media_per_minute: int = 10

    # CORS Configuration
    cors_origins: List[str] = ["http://localhost:3000"]
    cors_credentials: bool = True
    cors_methods: List[str] = ["*"]
    cors_headers: List[str] = ["*"]

    # Logging
    log_level: str = "INFO"
    log_format: str = "json"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    @field_validator("cors_origins", mode="before")
    @classmethod
    def parse_cors_origins(cls, v):
        """Parse CORS origins from string or list"""
        if isinstance(v, str):
            return [origin.strip() for origin in v.split(",")]
        return v


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance"""
    return Settings()


# Export settings instance
settings = get_settings()
