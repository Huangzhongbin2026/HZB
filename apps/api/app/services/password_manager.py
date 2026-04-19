from __future__ import annotations

import base64
import hashlib
import secrets
import string
from datetime import UTC, datetime

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from app.models.note import PasswordVaultConfig, PasswordVaultEntry


class PasswordManagerService:
    ITERATIONS = 390000

    def _derive_key(self, master_password: str, salt: bytes, instance_id: str) -> bytes:
        return hashlib.pbkdf2_hmac(
            "sha256",
            f"{instance_id}:{master_password}".encode("utf-8"),
            salt,
            self.ITERATIONS,
            dklen=32,
        )

    def create_verifier(self, master_password: str, instance_id: str) -> tuple[str, str]:
        salt = secrets.token_bytes(16)
        key = self._derive_key(master_password, salt, instance_id)
        verifier = hashlib.sha256(key + b":vault-verifier").hexdigest()
        return base64.b64encode(salt).decode("utf-8"), verifier

    def verify_master_password(self, config: PasswordVaultConfig | None, master_password: str, instance_id: str) -> bool:
        if config is None or not config.verifier_salt or not config.verifier_hash:
            return False
        salt = base64.b64decode(config.verifier_salt.encode("utf-8"))
        key = self._derive_key(master_password, salt, instance_id)
        verifier = hashlib.sha256(key + b":vault-verifier").hexdigest()
        return secrets.compare_digest(verifier, config.verifier_hash)

    def encrypt_password(self, plaintext: str, master_password: str, instance_id: str) -> tuple[str, str]:
        salt = secrets.token_bytes(16)
        nonce = secrets.token_bytes(12)
        key = self._derive_key(master_password, salt, instance_id)
        encrypted = AESGCM(key).encrypt(nonce, plaintext.encode("utf-8"), instance_id.encode("utf-8"))
        payload = base64.b64encode(nonce + encrypted).decode("utf-8")
        encoded_salt = base64.b64encode(salt).decode("utf-8")
        return payload, encoded_salt

    def decrypt_password(self, entry: PasswordVaultEntry, master_password: str) -> str:
        salt = base64.b64decode(entry.encryption_salt.encode("utf-8"))
        payload = base64.b64decode(entry.encrypted_password.encode("utf-8"))
        nonce = payload[:12]
        ciphertext = payload[12:]
        key = self._derive_key(master_password, salt, entry.instance_id)
        decrypted = AESGCM(key).decrypt(nonce, ciphertext, entry.instance_id.encode("utf-8"))
        return decrypted.decode("utf-8")

    def build_password(self, length: int, include_symbols: bool, include_numbers: bool, include_uppercase: bool) -> str:
        alphabet = string.ascii_lowercase
        required_sets = [string.ascii_lowercase]
        if include_uppercase:
            alphabet += string.ascii_uppercase
            required_sets.append(string.ascii_uppercase)
        if include_numbers:
            alphabet += string.digits
            required_sets.append(string.digits)
        if include_symbols:
            symbols = "!@#$%^&*()-_=+[]{}:,.?"
            alphabet += symbols
            required_sets.append(symbols)

        generated = [secrets.choice(charset) for charset in required_sets]
        while len(generated) < length:
            generated.append(secrets.choice(alphabet))
        secrets.SystemRandom().shuffle(generated)
        return "".join(generated)

    def touch_entry(self, entry: PasswordVaultEntry) -> None:
        entry.last_used_at = datetime.now(UTC)
        entry.updated_at = datetime.now(UTC)


password_manager_service = PasswordManagerService()
