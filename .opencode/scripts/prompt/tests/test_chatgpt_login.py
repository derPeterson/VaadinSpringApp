import webbrowser
from datetime import datetime, timezone
from pathlib import Path

from prompt.chatgpt.auth import ChatGPTAuth
from prompt.chatgpt.callback_server import ChatGPTCallbackServer
from prompt.chatgpt.credential_store import ChatGPTCredentialStore
from prompt.chatgpt.credentials import ChatGPTCredentials
from prompt.chatgpt.identity import ChatGPTIdentityVerifier
from prompt.chatgpt.token_client import ChatGPTTokenClient


def main() -> None:
    storage_directory = (
            Path.home()
            / ".vaadin-prompt-improver"
            / "chatgpt"
    )

    store = ChatGPTCredentialStore(
        storage_directory
    )

    from prompt.telemetry import PromptProviderError

    try:
        credentials = store.credentials_for_request(ChatGPTTokenClient())
        print("Stored ChatGPT credentials are ready. No browser login required.")
        return
    except PromptProviderError as error:
        if error.category not in ("authentication", "credential_storage"):
            raise
        print("ChatGPT sign-in is required; starting the explicit browser login.")

    host_id = store.load_host_id()

    if host_id is None:
        host_id = ChatGPTAuth.create_host_id()
        store.save_host_id(host_id)

    attempt = ChatGPTAuth.create_attempt(
        ext_agent_host_id=host_id
    )

    callback_server = ChatGPTCallbackServer()
    callback_server.start()

    authorization_url = ChatGPTAuth.build_authorization_url(
        attempt=attempt,
        redirect_uri=callback_server.redirect_uri,
        agent_name="Vaadin Prompt Improver",
    )

    print("Opening ChatGPT login in browser...")

    webbrowser.open(authorization_url)

    callback = callback_server.wait_for_callback()

    if callback.state != attempt.state:
        raise RuntimeError(
            "OAuth state mismatch."
        )

    if callback.client_id is None:
        raise RuntimeError(
            "ChatGPT registration did not return a client ID."
        )

    token_client = ChatGPTTokenClient()

    token_response = token_client.exchange_code(
        client_id=callback.client_id,
        code=callback.code,
        code_verifier=attempt.code_verifier,
        redirect_uri=callback_server.redirect_uri,
    )

    identity_verifier = ChatGPTIdentityVerifier()

    identity = identity_verifier.verify(
        id_token=token_response.id_token,
        client_id=callback.client_id,
        expected_nonce=attempt.nonce,
    )

    required_scope = "chatgpt.tokens.use.direct"

    if required_scope not in token_response.scopes:
        raise RuntimeError(
            "ChatGPT plan usage permission was not granted."
        )

    credentials = ChatGPTCredentials(
        email=identity.email,
        issuer=identity.issuer,
        subject=identity.subject,
        client_id=callback.client_id,
        ext_agent_host_id=host_id,
        id_token=token_response.id_token,
        access_token=token_response.access_token,
        refresh_token=token_response.refresh_token,
        token_type=token_response.token_type,
        expires_in=token_response.expires_in,
        scopes=token_response.scopes,
        saved_at=datetime.now(
            timezone.utc
        ).isoformat(),
    )

    store.save_credentials(
        credentials
    )

    print("ChatGPT login successful.")
    print("Identity verified.")
    print(f"Email: {identity.email}")
    print(f"Subject: {identity.subject}")
    print(f"Client ID: {callback.client_id}")
    print("ChatGPT plan usage: enabled")
    print(
        f"Credentials saved in: "
        f"{storage_directory}"
    )


if __name__ == "__main__":
    main()
