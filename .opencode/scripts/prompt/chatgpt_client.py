from pathlib import Path
from typing import Any
from openai import OpenAI
from prompt.chatgpt.credential_store import ChatGPTCredentialStore
from prompt.chatgpt.token_client import ChatGPTTokenClient
from prompt.llm_client import LlmClient
from prompt.telemetry import UsageObserver, PromptProviderError, LOGIN_REQUIRED, checked_text, field, provider_error


class _StreamedText:
    """Retain text parts without adding final text snapshots to their deltas."""
    def __init__(self):
        self.deltas, self.done, self.refused = {}, {}, False

    def observe(self, event):
        kind = field(event, "type")
        if kind in ("response.refusal.delta", "response.refusal.done"):
            self.refused = True
        key = (field(event, "output_index", 0), field(event, "content_index", 0))
        if kind == "response.output_text.delta":
            delta = field(event, "delta")
            if isinstance(delta, str):
                self.deltas.setdefault(key, []).append(delta)
        elif kind == "response.output_text.done":
            text = field(event, "text")
            if isinstance(text, str):
                self.done[key] = text
        elif kind == "response.content_part.done":
            self._part(key, field(event, "part"))
        elif kind == "response.output_item.done":
            item = field(event, "item")
            if field(item, "type") == "message":
                for index, part in enumerate(field(item, "content", []) or []):
                    self._part((key[0], index), part)

    def _part(self, key, part):
        if field(part, "type") == "refusal":
            self.refused = True
        elif field(part, "type") == "output_text" and isinstance(field(part, "text"), str):
            self.done[key] = field(part, "text")

    def text(self):
        return "".join(self.done[key] if key in self.done else "".join(self.deltas[key])
                       for key in sorted(self.deltas.keys() | self.done.keys()))


class ChatGPTClient(UsageObserver, LlmClient):
    BASE_URL = "https://api.openai.com/v1"

    def __init__(self, model: str, storage_directory: Path | None = None):
        self.model = model
        directory = storage_directory or Path.home() / ".vaadin-prompt-improver/chatgpt"
        self.credential_store = ChatGPTCredentialStore(directory)
        self.token_client = ChatGPTTokenClient()

    def chat(self, system_prompt: str, user_prompt: str,
             response_format: dict[str, Any] | None = None) -> str:
        # Credential acquisition is not an inference request.
        from prompt.telemetry import empty_usage
        self._usage = empty_usage()
        credentials = None
        try:
            credentials = self.credential_store.credentials_for_request(self.token_client)
            with OpenAI(api_key=credentials.access_token, base_url=self.BASE_URL,
                        max_retries=0, timeout=120.0) as client:
                params = dict(model=self.model, instructions=system_prompt,
                              input=[{"role": "user", "content": user_prompt}], store=False, stream=True)
                if response_format is not None:
                    params["text"] = {"format": {"type": "json_schema", "name": "structured_response",
                                                 "schema": response_format, "strict": True}}
                self.start_request()
                text = _StreamedText()
                with client.responses.create(**params) as stream:
                    for event in stream:
                        kind = field(event, "type")
                        text.observe(event)
                        if kind in ("response.completed", "response.failed", "response.incomplete"):
                            response = field(event, "response")
                            self.observe_response(response)
                            # Terminal status/usage are authoritative. Some streams omit final text.
                            return checked_text(response, text.text(), text.refused)
                        if kind == "error":
                            raise PromptProviderError("provider_failure", "ChatGPT stream reported an error.")
                raise PromptProviderError("interrupted_stream", "ChatGPT stream ended without a terminal response.")
        except Exception as error:
            normalized = provider_error(error)
            if normalized.category == "authentication":
                login_error = PromptProviderError("authentication", LOGIN_REQUIRED)
                if credentials is not None:
                    try:
                        self.credential_store.require_login(credentials.client_id, credentials.access_token)
                    except Exception:
                        login_error.__notes__ = ["Could not persist the ChatGPT reauthentication marker."]
                raise login_error from None
            raise normalized from None
