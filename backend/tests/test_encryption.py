"""Tests for encryption services"""

import pytest
from app.services.encryption import E2EEncryption, UCEEncryption, run_encryption_tests


def test_e2e_public_key_validation():
    """Test E2E public key validation"""
    import base64

    # Valid key (32 bytes)
    valid_key = base64.b64encode(b"a" * 32).decode()
    assert E2EEncryption.validate_public_key(valid_key)

    # Invalid key (wrong length)
    invalid_key = base64.b64encode(b"short").decode()
    assert not E2EEncryption.validate_public_key(invalid_key)

    # Invalid base64
    assert not E2EEncryption.validate_public_key("not-valid-base64!@#")


def test_e2e_recovery_codes():
    """Test E2E recovery code generation"""
    codes = E2EEncryption.generate_recovery_codes(10)

    assert len(codes) == 10
    assert all(len(code) == 19 for code in codes)  # XXXX-XXXX-XXXX-XXXX format
    assert all(code.count("-") == 3 for code in codes)

    # Codes should be unique
    assert len(set(codes)) == 10


def test_e2e_recovery_code_hashing():
    """Test recovery code hashing"""
    code = "ABCD-1234-EFGH-5678"
    hash1 = E2EEncryption.hash_recovery_code(code)
    hash2 = E2EEncryption.hash_recovery_code(code)

    assert hash1 == hash2  # Same input = same hash
    assert len(hash1) == 64  # SHA-256 produces 64 hex chars


def test_uce_key_derivation():
    """Test UCE password-based key derivation"""
    uce = UCEEncryption()
    password = "TestPassword123!"
    salt = uce.generate_salt()

    # Same password + salt = same key
    key1 = uce.derive_key_from_password(password, salt)
    key2 = uce.derive_key_from_password(password, salt)
    assert key1 == key2
    assert len(key1) == 32  # 256-bit key

    # Different salt = different key
    salt2 = uce.generate_salt()
    key3 = uce.derive_key_from_password(password, salt2)
    assert key1 != key3


def test_uce_master_key_encryption():
    """Test UCE master key encryption/decryption"""
    uce = UCEEncryption()
    password = "TestPassword123!"
    salt = uce.generate_salt()
    master_key = uce.generate_master_key()

    # Encrypt master key
    encrypted = uce.encrypt_master_key(master_key, password, salt)
    assert len(encrypted) > 32  # Should be longer due to nonce and tag

    # Decrypt master key
    decrypted = uce.decrypt_master_key(encrypted, password, salt)
    assert decrypted == master_key

    # Wrong password should fail
    with pytest.raises(Exception):
        uce.decrypt_master_key(encrypted, "WrongPassword", salt)


def test_uce_content_encryption():
    """Test UCE content encryption/decryption"""
    uce = UCEEncryption()
    master_key = uce.generate_master_key()
    plaintext = "This is a test diary entry!"

    # Encrypt
    encrypted = uce.encrypt_content(plaintext, master_key)
    assert encrypted != plaintext
    assert len(encrypted) > 0

    # Decrypt
    decrypted = uce.decrypt_content(encrypted, master_key)
    assert decrypted == plaintext

    # Wrong key should fail
    wrong_key = uce.generate_master_key()
    with pytest.raises(Exception):
        uce.decrypt_content(encrypted, wrong_key)


def test_uce_setup_user_encryption():
    """Test UCE user setup workflow"""
    uce = UCEEncryption()
    password = "TestPassword123!"

    # Setup encryption
    encrypted_master_key_b64, salt_b64 = uce.setup_user_encryption(password)

    assert len(encrypted_master_key_b64) > 0
    assert len(salt_b64) > 0

    # Verify password works
    assert uce.verify_password(password, encrypted_master_key_b64, salt_b64)

    # Wrong password fails
    assert not uce.verify_password("WrongPassword", encrypted_master_key_b64, salt_b64)


def test_uce_content_hash():
    """Test content hash for deduplication"""
    content1 = "Same content"
    content2 = "Same content"
    content3 = "Different content"

    hash1 = UCEEncryption.compute_content_hash(content1)
    hash2 = UCEEncryption.compute_content_hash(content2)
    hash3 = UCEEncryption.compute_content_hash(content3)

    assert hash1 == hash2  # Same content = same hash
    assert hash1 != hash3  # Different content = different hash
    assert len(hash1) == 64  # SHA-256 produces 64 hex chars


def test_encryption_test_vectors():
    """Run comprehensive encryption test vectors"""
    results = run_encryption_tests()

    assert results["e2e_public_key_validation"]
    assert results["e2e_recovery_codes"]
    assert results["uce_key_derivation"]
    assert results["uce_encrypt_decrypt"]
    assert results["uce_password_verification"]
    assert "error" not in results
