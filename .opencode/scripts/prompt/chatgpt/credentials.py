from datetime import datetime, timedelta

from pydantic import BaseModel, Field


class ChatGPTTokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    id_token: str
    token_type: str
    expires_in: int
    scope: str
    earliest_refresh_at: int | None = None

    @property
    def scopes(self) -> list[str]:
        return self.scope.split()


class ChatGPTCredentials(BaseModel):
    email: str | None
    issuer: str
    subject: str

    client_id: str
    ext_agent_host_id: str

    id_token: str
    access_token: str
    refresh_token: str

    token_type: str
    expires_in: int

    scopes: list[str] = Field(default_factory=list)
    saved_at: str

    def is_access_token_valid(
            self,
            safety_margin_seconds: int = 60,
    ) -> bool:
        saved_at = datetime.fromisoformat(
            self.saved_at
        )

        expires_at = (
                saved_at
                + timedelta(seconds=self.expires_in)
        )

        refresh_before = (
                expires_at
                - timedelta(
            seconds=safety_margin_seconds
        )
        )

        return (
                datetime.now(saved_at.tzinfo)
                < refresh_before
        )
