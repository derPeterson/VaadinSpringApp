import httpx

from prompt.chatgpt.credentials import ChatGPTTokenResponse


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
    ) -> ChatGPTTokenResponse:
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

        response.raise_for_status()

        return ChatGPTTokenResponse.model_validate(
            response.json()
        )
