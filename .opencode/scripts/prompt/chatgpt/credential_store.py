import json
import os
from datetime import datetime, timezone
from pathlib import Path
import uuid
import keyring
from pydantic import BaseModel
from prompt.chatgpt.credentials import ChatGPTCredentials
from prompt.chatgpt.session_lock import session_lock
from prompt.telemetry import LOGIN_REQUIRED, PromptProviderError, provider_error


class ChatGPTProfileMetadata(BaseModel):
    reauth_required: bool = False
    email: str | None
    issuer: str
    subject: str
    client_id: str
    ext_agent_host_id: str
    token_type: str
    expires_in: int
    scopes: list[str]
    saved_at: str


class ChatGPTCredentialStore:
    KEYRING_SERVICE = "VaadinPromptImprover.ChatGPT"
    CHUNK_SIZE = 800

    def __init__(self, storage_directory: Path, lock_path: Path | None = None):
        self.storage_directory = storage_directory
        self.profile_path = storage_directory / "profile.json"
        self.host_path = storage_directory / "host.json"
        # The keyring service is global per user, so separate project directories
        # must share this lock, too. Tests inject their own isolated path.
        self.lock_path = lock_path or Path.home() / ".vaadin-prompt-improver/chatgpt-session.lock"

    @staticmethod
    def _atomic_text(path, value):
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.with_name(path.name + "." + uuid.uuid4().hex + ".tmp")
        try:
            temporary.write_text(value, encoding="utf-8")
            os.replace(temporary, path)
        finally:
            temporary.unlink(missing_ok=True)

    def save_host_id(self, ext_agent_host_id: str) -> None:
        with session_lock(self.lock_path):
            self._atomic_text(self.host_path, json.dumps({"ext_agent_host_id": ext_agent_host_id}, indent=2))

    def load_host_id(self) -> str | None:
        if not self.host_path.exists():
            return None
        return json.loads(self.host_path.read_text(encoding="utf-8")).get("ext_agent_host_id")

    def save_credentials(self, credentials: ChatGPTCredentials) -> None:
        with session_lock(self.lock_path):
            self._save_credentials(credentials)

    def _save_credentials(self, credentials):
        # One versioned keyring bundle publishes all three rotating tokens and
        # their expiry together. A failed chunk write leaves the old snapshot intact.
        self._save_secret(credentials.client_id, "credentials", credentials.model_dump_json())
        metadata = ChatGPTProfileMetadata.model_validate(credentials.model_dump())
        self._atomic_text(self.profile_path, metadata.model_dump_json(indent=2))
        for name in ("access_token", "refresh_token", "id_token"):
            self._cleanup_secret(credentials.client_id, name)

    def _manifest(self, client_id, name):
        raw = keyring.get_password(self.KEYRING_SERVICE, f"{client_id}:{name}:meta")
        if raw is None:
            return None
        try:
            data = json.loads(raw)
            if type(data) is int:
                data = {"count": data, "generation": None}
            count, generation = data["count"], data["generation"]
            if type(count) is not int or not 0 <= count <= 10000:
                raise ValueError()
            if generation is not None and (not isinstance(generation, str) or
                                           len(generation) != 32 or any(c not in "0123456789abcdef" for c in generation)):
                raise ValueError()
            return data
        except (ValueError, TypeError, KeyError):
            raise PromptProviderError("credential_storage", "Stored ChatGPT credentials are malformed; log in again.") from None

    @staticmethod
    def _chunk_key(client_id, name, generation, index):
        return f"{client_id}:{name}:{generation}:{index}" if generation else f"{client_id}:{name}:{index}"

    def _delete_chunks(self, client_id, name, manifest):
        if manifest is None:
            return
        for index in range(manifest["count"]):
            try:
                keyring.delete_password(self.KEYRING_SERVICE,
                                        self._chunk_key(client_id, name, manifest["generation"], index))
            except keyring.errors.KeyringError:
                pass  # Cleanup cannot turn a published rotating token into a failed refresh.

    def _cleanup_secret(self, client_id, name):
        try:
            manifest = self._manifest(client_id, name)
            self._delete_chunks(client_id, name, manifest)
            if manifest is not None:
                keyring.delete_password(self.KEYRING_SERVICE, f"{client_id}:{name}:meta")
        except (keyring.errors.KeyringError, PromptProviderError):
            pass

    def _save_secret(self, client_id: str, name: str, value: str) -> None:
        try:
            previous = self._manifest(client_id, name)
        except PromptProviderError:
            previous = None  # A fresh verified login can replace a malformed pointer.
        generation = uuid.uuid4().hex
        chunks = [value[i:i + self.CHUNK_SIZE] for i in range(0, len(value), self.CHUNK_SIZE)]
        manifest = {"generation": generation, "count": len(chunks)}
        try:
            for index, chunk in enumerate(chunks):
                keyring.set_password(self.KEYRING_SERVICE, self._chunk_key(client_id, name, generation, index), chunk)
            keyring.set_password(self.KEYRING_SERVICE, f"{client_id}:{name}:meta", json.dumps(manifest))
        except Exception:
            self._delete_chunks(client_id, name, manifest)
            raise
        self._delete_chunks(client_id, name, previous)

    def _load_secret(self, client_id: str, name: str) -> str | None:
        manifest = self._manifest(client_id, name)
        if manifest is None:
            return None
        chunks = []
        for index in range(manifest["count"]):
            chunk = keyring.get_password(self.KEYRING_SERVICE,
                                        self._chunk_key(client_id, name, manifest["generation"], index))
            if chunk is None:
                return None
            chunks.append(chunk)
        return "".join(chunks)

    def load_credentials(self) -> ChatGPTCredentials | None:
        with session_lock(self.lock_path):
            return self._load_credentials()

    def _load_credentials(self):
        if not self.profile_path.exists():
            return None
        metadata = ChatGPTProfileMetadata.model_validate_json(self.profile_path.read_text(encoding="utf-8"))
        bundle = self._load_secret(metadata.client_id, "credentials")
        if bundle is not None:
            return ChatGPTCredentials.model_validate_json(bundle)
        if self._manifest(metadata.client_id, "credentials") is not None:
            raise PromptProviderError("credential_storage", "Stored ChatGPT credential snapshot is incomplete; log in again.")
        # Read existing installations without copying any secret into profile.json.
        secrets = {name: self._load_secret(metadata.client_id, name)
                   for name in ("access_token", "refresh_token", "id_token")}
        if any(value is None for value in secrets.values()):
            return None
        return ChatGPTCredentials(**metadata.model_dump(), **secrets)

    def credentials_for_request(self, token_client):
        try:
            with session_lock(self.lock_path):
                # Re-read AFTER taking the lock: another process may have refreshed.
                credentials = self._load_credentials()
                if credentials is None:
                    raise PromptProviderError("authentication", LOGIN_REQUIRED)
                if credentials.reauth_required:
                    raise PromptProviderError("authentication", LOGIN_REQUIRED)
                if credentials.is_access_token_valid():
                    return credentials
                try:
                    refreshed = token_client.refresh(client_id=credentials.client_id, refresh_token=credentials.refresh_token)
                except PromptProviderError as error:
                    if error.category == "authentication":
                        credentials.reauth_required = True
                        try:
                            self._save_credentials(credentials)
                        except Exception:
                            error.__notes__ = ["Could not persist the ChatGPT reauthentication marker."]
                    raise
                credentials.access_token = refreshed.access_token
                if refreshed.refresh_token is not None:
                    credentials.refresh_token = refreshed.refresh_token
                credentials.token_type = refreshed.token_type
                credentials.expires_in = refreshed.expires_in
                credentials.saved_at = datetime.now(timezone.utc).isoformat()
                if refreshed.id_token is not None:
                    credentials.id_token = refreshed.id_token
                if refreshed.scope is not None:
                    credentials.scopes = refreshed.scopes
                self._save_credentials(credentials)
                return credentials
        except PromptProviderError:
            raise
        except Exception as error:
            raise provider_error(error) from None

    def require_login(self, client_id, access_token):
        with session_lock(self.lock_path):
            current = self._load_credentials()
            # Do not invalidate a replacement produced while this request ran.
            if current and current.client_id == client_id and current.access_token == access_token:
                current.reauth_required = True
                self._save_credentials(current)
