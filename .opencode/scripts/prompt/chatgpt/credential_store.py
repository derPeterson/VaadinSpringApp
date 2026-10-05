import json
from pathlib import Path

import keyring
from pydantic import BaseModel

from prompt.chatgpt.credentials import ChatGPTCredentials


class ChatGPTProfileMetadata(BaseModel):
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

    def __init__(self, storage_directory: Path):
        self.storage_directory = storage_directory
        self.profile_path = (
                storage_directory / "profile.json"
        )
        self.host_path = (
                storage_directory / "host.json"
        )

    def save_host_id(
            self,
            ext_agent_host_id: str,
    ) -> None:
        self.storage_directory.mkdir(
            parents=True,
            exist_ok=True,
        )

        self.host_path.write_text(
            json.dumps(
                {
                    "ext_agent_host_id": ext_agent_host_id,
                },
                indent=2,
            ),
            encoding="utf-8",
        )

    def load_host_id(self) -> str | None:
        if not self.host_path.exists():
            return None

        data = json.loads(
            self.host_path.read_text(
                encoding="utf-8"
            )
        )

        return data.get("ext_agent_host_id")

    def save_credentials(
            self,
            credentials: ChatGPTCredentials,
    ) -> None:
        self.storage_directory.mkdir(
            parents=True,
            exist_ok=True,
        )

        metadata = ChatGPTProfileMetadata(
            email=credentials.email,
            issuer=credentials.issuer,
            subject=credentials.subject,
            client_id=credentials.client_id,
            ext_agent_host_id=credentials.ext_agent_host_id,
            token_type=credentials.token_type,
            expires_in=credentials.expires_in,
            scopes=credentials.scopes,
            saved_at=credentials.saved_at,
        )

        self._save_secret(
            client_id=credentials.client_id,
            name="access_token",
            value=credentials.access_token,
        )

        self._save_secret(
            client_id=credentials.client_id,
            name="refresh_token",
            value=credentials.refresh_token,
        )

        self._save_secret(
            client_id=credentials.client_id,
            name="id_token",
            value=credentials.id_token,
        )

        self.profile_path.write_text(
            metadata.model_dump_json(indent=2),
            encoding="utf-8",
        )

    def _save_secret(
            self,
            client_id: str,
            name: str,
            value: str,
    ) -> None:
        chunks = [
            value[index:index + self.CHUNK_SIZE]
            for index in range(
                0,
                len(value),
                self.CHUNK_SIZE,
            )
        ]

        keyring.set_password(
            self.KEYRING_SERVICE,
            f"{client_id}:{name}:meta",
            str(len(chunks)),
        )

        for index, chunk in enumerate(chunks):
            keyring.set_password(
                self.KEYRING_SERVICE,
                f"{client_id}:{name}:{index}",
                chunk,
            )

    def _load_secret(
            self,
            client_id: str,
            name: str,
    ) -> str | None:
        chunk_count_text = keyring.get_password(
            self.KEYRING_SERVICE,
            f"{client_id}:{name}:meta",
        )

        if chunk_count_text is None:
            return None

        chunk_count = int(chunk_count_text)

        chunks: list[str] = []

        for index in range(chunk_count):
            chunk = keyring.get_password(
                self.KEYRING_SERVICE,
                f"{client_id}:{name}:{index}",
            )

            if chunk is None:
                return None

            chunks.append(chunk)

        return "".join(chunks)

    def load_credentials(
            self,
    ) -> ChatGPTCredentials | None:
        if not self.profile_path.exists():
            return None

        metadata = ChatGPTProfileMetadata.model_validate_json(
            self.profile_path.read_text(
                encoding="utf-8"
            )
        )

        access_token = self._load_secret(
            metadata.client_id,
            "access_token",
        )

        refresh_token = self._load_secret(
            metadata.client_id,
            "refresh_token",
        )

        id_token = self._load_secret(
            metadata.client_id,
            "id_token",
        )

        if (
                access_token is None
                or refresh_token is None
                or id_token is None
        ):
            return None

        return ChatGPTCredentials(
            email=metadata.email,
            issuer=metadata.issuer,
            subject=metadata.subject,
            client_id=metadata.client_id,
            ext_agent_host_id=metadata.ext_agent_host_id,
            id_token=id_token,
            access_token=access_token,
            refresh_token=refresh_token,
            token_type=metadata.token_type,
            expires_in=metadata.expires_in,
            scopes=metadata.scopes,
            saved_at=metadata.saved_at,
        )
