from abc import ABC, abstractmethod
from typing import Any


class LlmClient(ABC):

    @abstractmethod
    def chat(
            self,
            system_prompt: str,
            user_prompt: str,
            response_format: dict[str, Any] | None = None,
    ) -> str:
        pass
