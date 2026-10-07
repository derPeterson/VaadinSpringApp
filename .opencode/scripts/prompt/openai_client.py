from typing import Any
from openai import OpenAI
from prompt.llm_client import LlmClient
from prompt.telemetry import UsageObserver, checked_text, provider_error


class OpenAIClient(UsageObserver, LlmClient):
    def __init__(self, model: str):
        self.model = model
        # No implicit retries: request attempts remain measurable and failures explicit.
        self.client = OpenAI(max_retries=0, timeout=120.0)

    def close(self):
        self.client.close()

    def chat(self, system_prompt: str, user_prompt: str,
             response_format: dict[str, Any] | None = None) -> str:
        self.start_request()
        params = dict(model=self.model, instructions=system_prompt, input=user_prompt)
        if response_format is not None:
            params["text"] = {"format": {"type": "json_schema", "name": "structured_response",
                                         "schema": response_format, "strict": True}}
        try:
            response = self.client.responses.create(**params)
            self.observe_response(response)
            return checked_text(response)
        except Exception as error:
            raise provider_error(error) from None
