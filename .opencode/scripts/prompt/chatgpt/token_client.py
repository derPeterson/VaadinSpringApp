import httpx

from prompt.chatgpt.credentials import ChatGPTTokenResponse, ChatGPTRefreshResponse
from prompt.telemetry import PromptProviderError, LOGIN_REQUIRED


class ChatGPTTokenClient:
    TOKEN_ENDPOINT = (
        "https://auth.openai.com/api/accounts/oauth/token"
    )

    RESOURCE = "https://api.openai.com/v1"

    def exchange_code(
            self,
            client_id: str,
            code: str,
            code_verifier: str,
            redirect_uri: str,
    ) -> ChatGPTTokenResponse:
        response = httpx.post(
            self.TOKEN_ENDPOINT,
            data={
                "grant_type": "authorization_code",
                "client_id": client_id,
                "code": code,
                "code_verifier": code_verifier,
                "redirect_uri": redirect_uri,
                "resource": self.RESOURCE,
            },
            timeout=30.0,
        )

        response.raise_for_status()

        return ChatGPTTokenResponse.model_validate(
            response.json()
        )

    def refresh(
            self,
            client_id: str,
            refresh_token: str,
    ) -> ChatGPTRefreshResponse:
        response = httpx.post(
            self.TOKEN_ENDPOINT,
            data={
                "grant_type": "refresh_token",
                "client_id": client_id,
                "refresh_token": refresh_token,
                "resource": self.RESOURCE,
            },
            timeout=30.0,
        )

        if response.status_code in (400, 401, 403):
            try:
                code = response.json().get("error")
            except (ValueError, AttributeError):
                code = None
            if isinstance(code, dict):
                code = code.get("code")
            if code in ("invalid_grant", "invalid_token", "invalid_client", "invalid_refresh_token", "token_expired",
                        "refresh_token_expired", "refresh_token_invalidated", "refresh_token_reused") or response.status_code in (401, 403):
                raise PromptProviderError("authentication", LOGIN_REQUIRED)
        response.raise_for_status()
        return ChatGPTRefreshResponse.model_validate(response.json())
