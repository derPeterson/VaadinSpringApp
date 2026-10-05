import jwt
from jwt import PyJWKClient
from pydantic import BaseModel


class ChatGPTIdentity(BaseModel):
    issuer: str
    subject: str
    email: str | None = None
    name: str | None = None


class ChatGPTIdentityVerifier:
    ISSUER = "https://auth.openai.com"

    JWKS_URI = (
        "https://auth.openai.com/"
        ".well-known/jwks.json"
    )

    def __init__(self):
        self.jwks_client = PyJWKClient(
            self.JWKS_URI
        )

    def verify(
            self,
            id_token: str,
            client_id: str,
            expected_nonce: str,
    ) -> ChatGPTIdentity:
        signing_key = (
            self.jwks_client
            .get_signing_key_from_jwt(id_token)
        )

        claims = jwt.decode(
            id_token,
            signing_key.key,
            algorithms=["RS256"],
            audience=client_id,
            issuer=self.ISSUER,
            leeway=5,
            options={
                "require": [
                    "sub",
                    "exp",
                    "iat",
                ]
            },
        )

        nonce = claims.get("nonce")

        if nonce != expected_nonce:
            raise ValueError(
                "The ID token nonce does not match."
            )

        subject = claims.get("sub")

        if not isinstance(subject, str) or not subject:
            raise ValueError(
                "The ID token does not contain "
                "a valid subject."
            )

        return ChatGPTIdentity(
            issuer=self.ISSUER,
            subject=subject,
            email=claims.get("email"),
            name=claims.get("name"),
        )
