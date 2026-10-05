import base64
import hashlib
import secrets
import uuid
from urllib.parse import urlencode

from pydantic import BaseModel


class ChatGPTAuthAttempt(BaseModel):
    ext_agent_host_id: str
    state: str
    nonce: str
    code_verifier: str
    code_challenge: str


class ChatGPTAuth:
    AUTHORIZATION_ENDPOINT = (
        "https://auth.openai.com/api/accounts/authorize"
    )

    CLIENT_ID = "dynamic_agent_client"

    RESOURCE = "https://api.openai.com/v1"

    SCOPES = (
        "openid "
        "profile "
        "email "
        "offline_access "
        "resource.invoke "
        "chatgpt.tokens.use.direct"
    )

    @staticmethod
    def create_host_id() -> str:
        return f"urn:uuid:{uuid.uuid4()}"

    @staticmethod
    def create_attempt(
            ext_agent_host_id: str,
    ) -> ChatGPTAuthAttempt:
        state = secrets.token_urlsafe(32)
        nonce = secrets.token_urlsafe(32)
        code_verifier = secrets.token_urlsafe(64)

        code_challenge = ChatGPTAuth._create_code_challenge(
            code_verifier
        )

        return ChatGPTAuthAttempt(
            ext_agent_host_id=ext_agent_host_id,
            state=state,
            nonce=nonce,
            code_verifier=code_verifier,
            code_challenge=code_challenge,
        )

    @staticmethod
    def build_authorization_url(
            attempt: ChatGPTAuthAttempt,
            redirect_uri: str,
            agent_name: str,
    ) -> str:
        parameters = {
            "client_id": ChatGPTAuth.CLIENT_ID,
            "agent_name_hint": agent_name,
            "ext_agent_host_id": attempt.ext_agent_host_id,
            "response_type": "code",
            "redirect_uri": redirect_uri,
            "scope": ChatGPTAuth.SCOPES,
            "resource": ChatGPTAuth.RESOURCE,
            "state": attempt.state,
            "nonce": attempt.nonce,
            "code_challenge_method": "S256",
            "code_challenge": attempt.code_challenge,
        }

        return (
            f"{ChatGPTAuth.AUTHORIZATION_ENDPOINT}"
            f"?{urlencode(parameters)}"
        )

    @staticmethod
    def _create_code_challenge(
            code_verifier: str,
    ) -> str:
        digest = hashlib.sha256(
            code_verifier.encode("ascii")
        ).digest()

        return (
            base64.urlsafe_b64encode(digest)
            .decode("ascii")
            .rstrip("=")
        )
