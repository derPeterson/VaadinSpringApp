from typing import Any

from openai import OpenAI
from openai.types.responses import (
    ResponseFormatTextJSONSchemaConfigParam,
    ResponseTextConfigParam,
)

from prompt.llm_client import LlmClient


class OpenAIClient(LlmClient):

    def __init__(self, model: str):
        self.model = model
        self.client = OpenAI()

    def chat(
            self,
            system_prompt: str,
            user_prompt: str,
            response_format: dict[str, Any] | None = None,
    ) -> str:

        if response_format is None:
            response = self.client.responses.create(
                model=self.model,
                instructions=system_prompt,
                input=user_prompt,
            )
        else:
            format_config: ResponseFormatTextJSONSchemaConfigParam = {
                "type": "json_schema",
                "name": "structured_response",
                "schema": response_format,
                "strict": True,
            }

            text_config: ResponseTextConfigParam = {
                "format": format_config,
            }

            response = self.client.responses.create(
                model=self.model,
                instructions=system_prompt,
                input=user_prompt,
                text=text_config,
            )

        return response.output_text
