from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from openai import OpenAI
from openai.types.responses import (
    ResponseFormatTextJSONSchemaConfigParam,
    ResponseTextConfigParam,
)

from prompt.chatgpt.credential_store import ChatGPTCredentialStore
from prompt.chatgpt.token_client import ChatGPTTokenClient
from prompt.llm_client import LlmClient


class ChatGPTClient(LlmClient):
    BASE_URL = "https://api.openai.com/v1"

    def __init__(
            self,
            model: str,
            storage_directory: Path | None = None,
    ):
        self.model = model

        if storage_directory is None:
            storage_directory = (
                    Path.home()
                    / ".vaadin-prompt-improver"
                    / "chatgpt"
            )

        self.credential_store = (
            ChatGPTCredentialStore(
                storage_directory
            )
        )

        self.token_client = ChatGPTTokenClient()

    def chat(
            self,
            system_prompt: str,
            user_prompt: str,
            response_format: dict[str, Any] | None = None,
    ) -> str:
        credentials = (
            self.credential_store.load_credentials()
        )

        if credentials is None:
            raise RuntimeError(
                "No stored ChatGPT credentials found."
            )

        if not credentials.is_access_token_valid():
            refreshed = self.token_client.refresh(
                client_id=credentials.client_id,
                refresh_token=credentials.refresh_token,
            )

            credentials.access_token = (
                refreshed.access_token
            )
            credentials.refresh_token = (
                refreshed.refresh_token
            )
            credentials.token_type = (
                refreshed.token_type
            )
            credentials.expires_in = (
                refreshed.expires_in
            )
            credentials.saved_at = (
                datetime.now(
                    timezone.utc
                ).isoformat()
            )

            if refreshed.id_token is not None:
                credentials.id_token = (
                    refreshed.id_token
                )

            if refreshed.scope is not None:
                credentials.scopes = (
                    refreshed.scopes
                )

            self.credential_store.save_credentials(
                credentials
            )

        client = OpenAI(
            api_key=credentials.access_token,
            base_url=self.BASE_URL,
            max_retries=0,
        )

        if response_format is None:
            stream = client.responses.create(
                model=self.model,
                instructions=system_prompt,
                input=[
                    {
                        "role": "user",
                        "content": user_prompt,
                    }
                ],
                store=False,
                stream=True,
            )
        else:
            format_config: (
                ResponseFormatTextJSONSchemaConfigParam
            ) = {
                "type": "json_schema",
                "name": "structured_response",
                "schema": response_format,
                "strict": True,
            }

            text_config: ResponseTextConfigParam = {
                "format": format_config
            }

            stream = client.responses.create(
                model=self.model,
                instructions=system_prompt,
                input=[
                    {
                        "role": "user",
                        "content": user_prompt,
                    }
                ],
                text=text_config,
                store=False,
                stream=True,
            )

        output_parts: list[str] = []
        completed = False

        with stream:
            for event in stream:
                if (
                        event.type
                        == "response.output_text.delta"
                ):
                    output_parts.append(
                        event.delta
                    )

                elif event.type == "response.failed":
                    error = event.response.error

                    if error is None:
                        error_code = "unknown_error"
                    else:
                        error_code = error.code

                    raise RuntimeError(
                        "ChatGPT response failed: "
                        f"{error_code}"
                    )

                elif (
                        event.type
                        == "response.completed"
                ):
                    completed = True

        if not completed:
            raise RuntimeError(
                "ChatGPT response stream ended "
                "without response.completed."
            )

        return "".join(output_parts)
