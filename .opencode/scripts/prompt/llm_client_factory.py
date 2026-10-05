from prompt.chatgpt_client import ChatGPTClient
from prompt.llm_client import LlmClient
from prompt.ollama_client import OllamaClient
from prompt.openai_client import OpenAIClient


class LlmClientFactory:
    @staticmethod
    def create(
            provider: str,
            model: str,
    ) -> LlmClient:
        match provider.lower():
            case "ollama":
                return OllamaClient(model)

            case "openai":
                return OpenAIClient(model)

            case "chatgpt":
                return ChatGPTClient(model)

            case _:
                raise ValueError(
                    f"Unsupported LLM provider: {provider}"
                )
