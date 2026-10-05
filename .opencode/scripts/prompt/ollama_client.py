from typing import Any

import ollama

from prompt.llm_client import LlmClient


class OllamaClient(LlmClient):

    def __init__(self, model: str):
        self.model = model

    def chat(
            self,
            system_prompt: str,
            user_prompt: str,
            response_format: dict[str, Any] | None = None,
    ) -> str:
        response = ollama.chat(
            model=self.model,
            messages=[
                {
                    "role": "system",
                    "content": system_prompt,
                },
                {
                    "role": "user",
                    "content": user_prompt,
                },
            ],
            format=response_format,
            options={
                "temperature": 0,
            },
        )

        return response["message"]["content"]
