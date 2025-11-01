"""Encryption services for E2E and UCE tiers"""

import base64
import hashlib
import json
import os
import secrets
from typing import Tuple

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305
from cryptography.hazmat.primitives.kdf.argon2 import Argon2id
from cryptography.hazmat.backends import default_backend

from app.config import settings


class E2EEncryption:
    """
    End-to-End Encryption (E2E) service.

    Server never has access to decrypted content. All encryption/decryption
    happens on the client side. The server only stores encrypted data.

    This class provides utilities for server-side operations like key validation.
    """

    @staticmethod
    def validate_public_key(public_key_b64: str) -> bool:
        """
        Validate that a public key is properly formatted.

        Args:
            public_key_b64: Base64-encoded X25519 public key

        Returns:
            True if valid, False otherwise
        """
        try:
            decoded = base64.b64decode(public_key_b64)
            return len(decoded) == 32  # X25519 public key is 32 bytes
        except Exception:
            return False

    @staticmethod
    def generate_recovery_codes(count: int = 10) -> list[str]:
        """
        Generate recovery codes for account recovery.

        Args:
            count: Number of recovery codes to generate (default: 10)

        Returns:
            List of recovery codes (format: XXXX-XXXX-XXXX-XXXX)
        """
        codes = []
        for _ in range(count):
            # Generate 16 random bytes and format as 4 groups of 4 hex chars
            random_bytes = secrets.token_bytes(8)
            hex_str = random_bytes.hex().upper()
            formatted = f"{hex_str[0:4]}-{hex_str[4:8]}-{hex_str[8:12]}-{hex_str[12:16]}"
            codes.append(formatted)
        return codes

    @staticmethod
    def hash_recovery_code(code: str) -> str:
        """
        Hash a recovery code for secure storage.

        Args:
            code: Recovery code to hash

        Returns:
            SHA-256 hash of the recovery code
        """
        return hashlib.sha256(code.encode()).hexdigest()

    @staticmethod
    def verify_encrypted_content(encrypted_content: str) -> bool:
        """
        Verify that encrypted content is properly formatted.

        Args:
            encrypted_content: Base64-encoded encrypted content

        Returns:
            True if valid format, False otherwise
        """
        try:
            decoded = base64.b64decode(encrypted_content)
            # For ChaCha20-Poly1305: nonce (12 bytes) + ciphertext + tag (16 bytes)
            return len(decoded) >= 28  # Minimum: 12 + 0 + 16
        except Exception:
            return False


class UCEEncryption:
    """
    User-Controlled Encryption (UCE) service.

    Server has access to decrypted content (encrypted with user's password-derived key).
    Enables server-side search, AI features, and easier multi-device sync.

    Encryption scheme:
    - Password → Argon2id → Key Derivation Key (KDK)
    - KDK encrypts Master Encryption Key (MEK)
    - MEK encrypts all user content using ChaCha20-Poly1305
    """

    def __init__(self):
        self.backend = default_backend()

    def derive_key_from_password(self, password: str, salt: bytes) -> bytes:
        """
        Derive encryption key from password using Argon2id.

        Args:
            password: User password
            salt: Random salt (must be unique per user)

        Returns:
            32-byte derived key
        """
        kdf = Argon2id(
            salt=salt,
            length=32,  # 256-bit key
            iterations=settings.argon2_time_cost,
            lanes=settings.argon2_parallelism,
            memory_size=settings.argon2_memory_cost,
            backend=self.backend,
        )
        return kdf.derive(password.encode())

    def generate_master_key(self) -> bytes:
        """
        Generate a random master encryption key.

        Returns:
            32-byte master key
        """
        return ChaCha20Poly1305.generate_key()

    def encrypt_master_key(self, master_key: bytes, password: str, salt: bytes) -> bytes:
        """
        Encrypt the master key with password-derived key.

        Args:
            master_key: 32-byte master encryption key
            password: User password
            salt: Salt for key derivation

        Returns:
            Encrypted master key (nonce + ciphertext + tag)
        """
        kdk = self.derive_key_from_password(password, salt)
        cipher = ChaCha20Poly1305(kdk)
        nonce = os.urandom(12)
        ciphertext = cipher.encrypt(nonce, master_key, None)
        return nonce + ciphertext

    def decrypt_master_key(
        self, encrypted_master_key: bytes, password: str, salt: bytes
    ) -> bytes:
        """
        Decrypt the master key with password-derived key.

        Args:
            encrypted_master_key: Encrypted master key (nonce + ciphertext + tag)
            password: User password
            salt: Salt used for key derivation

        Returns:
            32-byte master key

        Raises:
            Exception: If decryption fails (wrong password)
        """
        kdk = self.derive_key_from_password(password, salt)
        cipher = ChaCha20Poly1305(kdk)
        nonce = encrypted_master_key[:12]
        ciphertext = encrypted_master_key[12:]
        return cipher.decrypt(nonce, ciphertext, None)

    def encrypt_content(self, plaintext: str, master_key: bytes) -> str:
        """
        Encrypt content using master key and ChaCha20-Poly1305.

        Args:
            plaintext: Content to encrypt
            master_key: 32-byte master encryption key

        Returns:
            Base64-encoded encrypted content (nonce + ciphertext + tag)
        """
        cipher = ChaCha20Poly1305(master_key)
        nonce = os.urandom(12)
        ciphertext = cipher.encrypt(nonce, plaintext.encode(), None)
        encrypted = nonce + ciphertext
        return base64.b64encode(encrypted).decode()

    def decrypt_content(self, encrypted_content: str, master_key: bytes) -> str:
        """
        Decrypt content using master key.

        Args:
            encrypted_content: Base64-encoded encrypted content
            master_key: 32-byte master encryption key

        Returns:
            Decrypted plaintext

        Raises:
            Exception: If decryption fails
        """
        encrypted = base64.b64decode(encrypted_content)
        cipher = ChaCha20Poly1305(master_key)
        nonce = encrypted[:12]
        ciphertext = encrypted[12:]
        plaintext_bytes = cipher.decrypt(nonce, ciphertext, None)
        return plaintext_bytes.decode()

    def generate_salt(self) -> bytes:
        """
        Generate a random salt for key derivation.

        Returns:
            32-byte random salt
        """
        return os.urandom(32)

    def setup_user_encryption(
        self, password: str
    ) -> Tuple[str, str]:
        """
        Set up encryption for a new UCE user.

        Args:
            password: User password

        Returns:
            Tuple of (encrypted_master_key_b64, salt_b64)
        """
        salt = self.generate_salt()
        master_key = self.generate_master_key()
        encrypted_master_key = self.encrypt_master_key(master_key, password, salt)

        return (
            base64.b64encode(encrypted_master_key).decode(),
            base64.b64encode(salt).decode(),
        )

    def verify_password(
        self, password: str, encrypted_master_key_b64: str, salt_b64: str
    ) -> bool:
        """
        Verify user password by attempting to decrypt master key.

        Args:
            password: Password to verify
            encrypted_master_key_b64: Base64-encoded encrypted master key
            salt_b64: Base64-encoded salt

        Returns:
            True if password is correct, False otherwise
        """
        try:
            encrypted_master_key = base64.b64decode(encrypted_master_key_b64)
            salt = base64.b64decode(salt_b64)
            self.decrypt_master_key(encrypted_master_key, password, salt)
            return True
        except Exception:
            return False

    def get_master_key(
        self, password: str, encrypted_master_key_b64: str, salt_b64: str
    ) -> bytes:
        """
        Get the master key by decrypting with password.

        Args:
            password: User password
            encrypted_master_key_b64: Base64-encoded encrypted master key
            salt_b64: Base64-encoded salt

        Returns:
            32-byte master key

        Raises:
            Exception: If password is incorrect
        """
        encrypted_master_key = base64.b64decode(encrypted_master_key_b64)
        salt = base64.b64decode(salt_b64)
        return self.decrypt_master_key(encrypted_master_key, password, salt)

    @staticmethod
    def compute_content_hash(plaintext: str) -> str:
        """
        Compute SHA-256 hash of plaintext for deduplication.

        Args:
            plaintext: Content to hash

        Returns:
            Hex-encoded SHA-256 hash
        """
        return hashlib.sha256(plaintext.encode()).hexdigest()


# Test vectors for encryption validation
E2E_TEST_VECTORS = {
    "public_key_valid": "qmFzZTY0IGVuY29kZWQgMzIgYnl0ZSBwdWJsaWMga2V5",  # Valid format
    "public_key_invalid_length": "dG9vIHNob3J0",  # Too short
    "public_key_invalid_encoding": "not-valid-base64!@#",  # Invalid base64
}

UCE_TEST_VECTORS = {
    "password": "TestPassword123!",
    "plaintext": "This is a test diary entry for encryption validation.",
    "salt_hex": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
}


def run_encryption_tests() -> dict:
    """
    Run encryption test vectors to validate implementation.

    Returns:
        Dictionary with test results
    """
    results = {
        "e2e_public_key_validation": False,
        "e2e_recovery_codes": False,
        "uce_key_derivation": False,
        "uce_encrypt_decrypt": False,
        "uce_password_verification": False,
    }

    try:
        # Test E2E public key validation
        assert E2EEncryption.validate_public_key(E2E_TEST_VECTORS["public_key_valid"])
        assert not E2EEncryption.validate_public_key(
            E2E_TEST_VECTORS["public_key_invalid_length"]
        )
        results["e2e_public_key_validation"] = True

        # Test E2E recovery codes
        codes = E2EEncryption.generate_recovery_codes(10)
        assert len(codes) == 10
        assert all(len(code) == 19 for code in codes)  # XXXX-XXXX-XXXX-XXXX
        hashed = E2EEncryption.hash_recovery_code(codes[0])
        assert len(hashed) == 64  # SHA-256 hex
        results["e2e_recovery_codes"] = True

        # Test UCE key derivation
        uce = UCEEncryption()
        salt = bytes.fromhex(UCE_TEST_VECTORS["salt_hex"])
        key1 = uce.derive_key_from_password(UCE_TEST_VECTORS["password"], salt)
        key2 = uce.derive_key_from_password(UCE_TEST_VECTORS["password"], salt)
        assert key1 == key2  # Same password + salt = same key
        assert len(key1) == 32  # 256-bit key
        results["uce_key_derivation"] = True

        # Test UCE encrypt/decrypt
        master_key = uce.generate_master_key()
        encrypted = uce.encrypt_content(UCE_TEST_VECTORS["plaintext"], master_key)
        decrypted = uce.decrypt_content(encrypted, master_key)
        assert decrypted == UCE_TEST_VECTORS["plaintext"]
        results["uce_encrypt_decrypt"] = True

        # Test UCE password verification
        encrypted_master_key_b64, salt_b64 = uce.setup_user_encryption(
            UCE_TEST_VECTORS["password"]
        )
        assert uce.verify_password(
            UCE_TEST_VECTORS["password"], encrypted_master_key_b64, salt_b64
        )
        assert not uce.verify_password(
            "WrongPassword123!", encrypted_master_key_b64, salt_b64
        )
        results["uce_password_verification"] = True

    except Exception as e:
        results["error"] = str(e)

    return results
