"""Tests for authentication endpoints"""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_signup_uce(client: AsyncClient, test_user_uce):
    """Test UCE user signup"""
    response = await client.post("/api/v1/auth/signup", json=test_user_uce)

    assert response.status_code == 201
    data = response.json()

    assert "user_id" in data
    assert data["email"] == test_user_uce["email"]
    assert data["encryption_tier"] == "uce"
    assert "jwt_token" in data
    assert "refresh_token" in data
    assert "encrypted_master_key" in data
    assert data["recovery_codes"] is None  # UCE doesn't have recovery codes


@pytest.mark.asyncio
async def test_signup_e2e(client: AsyncClient, test_user_e2e):
    """Test E2E user signup"""
    response = await client.post("/api/v1/auth/signup", json=test_user_e2e)

    assert response.status_code == 201
    data = response.json()

    assert "user_id" in data
    assert data["email"] == test_user_e2e["email"]
    assert data["encryption_tier"] == "e2e"
    assert "jwt_token" in data
    assert "refresh_token" in data
    assert "public_key" in data
    assert "recovery_codes" in data
    assert len(data["recovery_codes"]) == 10  # Should have 10 recovery codes


@pytest.mark.asyncio
async def test_signup_duplicate_email(client: AsyncClient, test_user_uce):
    """Test signup with duplicate email"""
    # First signup
    response1 = await client.post("/api/v1/auth/signup", json=test_user_uce)
    assert response1.status_code == 201

    # Second signup with same email should fail
    response2 = await client.post("/api/v1/auth/signup", json=test_user_uce)
    assert response2.status_code == 400
    assert "already registered" in response2.json()["detail"].lower()


@pytest.mark.asyncio
async def test_signup_weak_password(client: AsyncClient):
    """Test signup with weak password"""
    weak_user = {
        "email": "weak@example.com",
        "password": "weak",  # Too short
        "encryption_tier": "uce",
    }

    response = await client.post("/api/v1/auth/signup", json=weak_user)
    assert response.status_code == 422  # Validation error


@pytest.mark.asyncio
async def test_login_success(client: AsyncClient, test_user_uce):
    """Test successful login"""
    # Signup first
    signup_response = await client.post("/api/v1/auth/signup", json=test_user_uce)
    assert signup_response.status_code == 201

    # Login
    login_data = {
        "email": test_user_uce["email"],
        "password": test_user_uce["password"],
    }
    response = await client.post("/api/v1/auth/login", json=login_data)

    assert response.status_code == 200
    data = response.json()

    assert "user_id" in data
    assert data["email"] == test_user_uce["email"]
    assert "jwt_token" in data
    assert "refresh_token" in data


@pytest.mark.asyncio
async def test_login_wrong_password(client: AsyncClient, test_user_uce):
    """Test login with wrong password"""
    # Signup first
    signup_response = await client.post("/api/v1/auth/signup", json=test_user_uce)
    assert signup_response.status_code == 201

    # Login with wrong password
    login_data = {
        "email": test_user_uce["email"],
        "password": "WrongPassword123!",
    }
    response = await client.post("/api/v1/auth/login", json=login_data)

    assert response.status_code == 401
    assert "invalid" in response.json()["detail"].lower()


@pytest.mark.asyncio
async def test_login_nonexistent_user(client: AsyncClient):
    """Test login with nonexistent email"""
    login_data = {
        "email": "nonexistent@example.com",
        "password": "TestPassword123!",
    }
    response = await client.post("/api/v1/auth/login", json=login_data)

    assert response.status_code == 401


@pytest.mark.asyncio
async def test_get_me(client: AsyncClient, test_user_uce):
    """Test getting current user info"""
    # Signup and login
    signup_response = await client.post("/api/v1/auth/signup", json=test_user_uce)
    token = signup_response.json()["jwt_token"]

    # Get current user
    response = await client.get(
        "/api/v1/auth/me", headers={"Authorization": f"Bearer {token}"}
    )

    assert response.status_code == 200
    data = response.json()

    assert data["email"] == test_user_uce["email"]
    assert data["encryption_tier"] == "uce"
    assert "id" in data


@pytest.mark.asyncio
async def test_get_me_unauthorized(client: AsyncClient):
    """Test getting current user without token"""
    response = await client.get("/api/v1/auth/me")

    assert response.status_code == 401


@pytest.mark.asyncio
async def test_get_features(client: AsyncClient, test_user_uce):
    """Test getting feature gates"""
    # Signup and login
    signup_response = await client.post("/api/v1/auth/signup", json=test_user_uce)
    token = signup_response.json()["jwt_token"]

    # Get features
    response = await client.get(
        "/api/v1/auth/features", headers={"Authorization": f"Bearer {token}"}
    )

    assert response.status_code == 200
    data = response.json()

    assert "features" in data
    assert data["features"]["server_search"] is True  # UCE supports server search
    assert data["features"]["server_ai"] is True  # UCE supports server AI
    assert "storage" in data


@pytest.mark.asyncio
async def test_refresh_token(client: AsyncClient, test_user_uce):
    """Test token refresh"""
    # Signup and login
    signup_response = await client.post("/api/v1/auth/signup", json=test_user_uce)
    refresh_token = signup_response.json()["refresh_token"]

    # Refresh access token
    response = await client.post(
        "/api/v1/auth/refresh", params={"refresh_token": refresh_token}
    )

    assert response.status_code == 200
    data = response.json()

    assert "access_token" in data
    assert data["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_logout(client: AsyncClient, test_user_uce):
    """Test logout"""
    # Signup and login
    signup_response = await client.post("/api/v1/auth/signup", json=test_user_uce)
    token = signup_response.json()["jwt_token"]

    # Logout
    response = await client.post(
        "/api/v1/auth/logout", headers={"Authorization": f"Bearer {token}"}
    )

    assert response.status_code == 200
    assert "message" in response.json()
