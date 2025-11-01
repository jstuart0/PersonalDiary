"""Tests for entries endpoints"""

import pytest
from httpx import AsyncClient


async def create_authenticated_user(client: AsyncClient, user_data):
    """Helper to create and authenticate a user"""
    response = await client.post("/api/v1/auth/signup", json=user_data)
    return response.json()["jwt_token"]


@pytest.mark.asyncio
async def test_create_entry(client: AsyncClient, test_user_uce):
    """Test creating an entry"""
    token = await create_authenticated_user(client, test_user_uce)

    entry_data = {
        "encrypted_content": "base64_encrypted_content_here",
        "content_hash": "a" * 64,  # 64-char SHA-256 hash
        "encrypted_title": "base64_encrypted_title",
        "mood": "happy",
        "tag_names": ["personal", "happy"],
    }

    response = await client.post(
        "/api/v1/entries/",
        json=entry_data,
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 201
    data = response.json()

    assert "id" in data
    assert data["encrypted_content"] == entry_data["encrypted_content"]
    assert data["mood"] == "happy"
    assert len(data["tags"]) == 2


@pytest.mark.asyncio
async def test_list_entries(client: AsyncClient, test_user_uce):
    """Test listing entries"""
    token = await create_authenticated_user(client, test_user_uce)

    # Create a few entries
    for i in range(3):
        entry_data = {
            "encrypted_content": f"content_{i}",
            "content_hash": f"{'a' * 63}{i}",
        }
        await client.post(
            "/api/v1/entries/",
            json=entry_data,
            headers={"Authorization": f"Bearer {token}"},
        )

    # List entries
    response = await client.get(
        "/api/v1/entries/",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 200
    data = response.json()

    assert data["total"] == 3
    assert len(data["entries"]) == 3
    assert data["page"] == 1


@pytest.mark.asyncio
async def test_get_entry(client: AsyncClient, test_user_uce):
    """Test getting a specific entry"""
    token = await create_authenticated_user(client, test_user_uce)

    # Create entry
    entry_data = {
        "encrypted_content": "test_content",
        "content_hash": "a" * 64,
    }
    create_response = await client.post(
        "/api/v1/entries/",
        json=entry_data,
        headers={"Authorization": f"Bearer {token}"},
    )
    entry_id = create_response.json()["id"]

    # Get entry
    response = await client.get(
        f"/api/v1/entries/{entry_id}",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 200
    data = response.json()

    assert data["id"] == entry_id
    assert data["encrypted_content"] == entry_data["encrypted_content"]


@pytest.mark.asyncio
async def test_update_entry(client: AsyncClient, test_user_uce):
    """Test updating an entry"""
    token = await create_authenticated_user(client, test_user_uce)

    # Create entry
    entry_data = {
        "encrypted_content": "original_content",
        "content_hash": "a" * 64,
    }
    create_response = await client.post(
        "/api/v1/entries/",
        json=entry_data,
        headers={"Authorization": f"Bearer {token}"},
    )
    entry_id = create_response.json()["id"]

    # Update entry
    update_data = {
        "encrypted_content": "updated_content",
        "content_hash": "b" * 64,
        "mood": "grateful",
    }
    response = await client.put(
        f"/api/v1/entries/{entry_id}",
        json=update_data,
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 200
    data = response.json()

    assert data["encrypted_content"] == "updated_content"
    assert data["mood"] == "grateful"


@pytest.mark.asyncio
async def test_delete_entry(client: AsyncClient, test_user_uce):
    """Test soft deleting an entry"""
    token = await create_authenticated_user(client, test_user_uce)

    # Create entry
    entry_data = {
        "encrypted_content": "test_content",
        "content_hash": "a" * 64,
    }
    create_response = await client.post(
        "/api/v1/entries/",
        json=entry_data,
        headers={"Authorization": f"Bearer {token}"},
    )
    entry_id = create_response.json()["id"]

    # Delete entry
    response = await client.delete(
        f"/api/v1/entries/{entry_id}",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 204

    # Verify entry is deleted (not in list by default)
    list_response = await client.get(
        "/api/v1/entries/",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert list_response.json()["total"] == 0


@pytest.mark.asyncio
async def test_restore_entry(client: AsyncClient, test_user_uce):
    """Test restoring a deleted entry"""
    token = await create_authenticated_user(client, test_user_uce)

    # Create and delete entry
    entry_data = {
        "encrypted_content": "test_content",
        "content_hash": "a" * 64,
    }
    create_response = await client.post(
        "/api/v1/entries/",
        json=entry_data,
        headers={"Authorization": f"Bearer {token}"},
    )
    entry_id = create_response.json()["id"]

    await client.delete(
        f"/api/v1/entries/{entry_id}",
        headers={"Authorization": f"Bearer {token}"},
    )

    # Restore entry
    response = await client.post(
        f"/api/v1/entries/{entry_id}/restore",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 200

    # Verify entry is restored
    list_response = await client.get(
        "/api/v1/entries/",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert list_response.json()["total"] == 1
