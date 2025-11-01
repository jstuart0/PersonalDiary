"""Pytest configuration and fixtures"""

import asyncio
import pytest
import pytest_asyncio
from typing import AsyncGenerator
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from httpx import AsyncClient

from app.main import app
from app.database import Base, get_db
from app.config import settings

# Test database URL (use a separate test database)
TEST_DATABASE_URL = settings.database_url.replace("/personal_diary", "/personal_diary_test")

# Create test engine
test_engine = create_async_engine(TEST_DATABASE_URL, echo=False)
TestSessionLocal = sessionmaker(
    test_engine, class_=AsyncSession, expire_on_commit=False
)


@pytest.fixture(scope="session")
def event_loop():
    """Create event loop for async tests"""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest_asyncio.fixture(scope="function")
async def db_session() -> AsyncGenerator[AsyncSession, None]:
    """Create a fresh database session for each test"""
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    async with TestSessionLocal() as session:
        yield session

    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest_asyncio.fixture(scope="function")
async def client(db_session: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
    """Create test client with database override"""

    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    async with AsyncClient(app=app, base_url="http://test") as ac:
        yield ac

    app.dependency_overrides.clear()


@pytest.fixture
def test_user_uce():
    """Test user data for UCE tier"""
    return {
        "email": "test_uce@example.com",
        "password": "TestPassword123!",
        "encryption_tier": "uce",
    }


@pytest.fixture
def test_user_e2e():
    """Test user data for E2E tier"""
    import base64

    # Generate a valid 32-byte public key
    public_key = base64.b64encode(b"x" * 32).decode()

    return {
        "email": "test_e2e@example.com",
        "password": "TestPassword123!",
        "encryption_tier": "e2e",
        "public_key": public_key,
    }
