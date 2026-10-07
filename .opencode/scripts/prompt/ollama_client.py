from typing import Any
import ollama
from prompt.llm_client import LlmClient
from prompt.telemetry import UsageObserver, PromptProviderError, field, number, provider_error


class OllamaClient(UsageObserver, LlmClient):
    def __init__(self, model: str):
        self.model = model
        self.client = ollama.Client(timeout=120.0)

    def close(self):
        self.client.close()

    def chat(self, system_prompt: str, user_prompt: str,
             response_format: dict[str, Any] | None = None) -> str:
        self.start_request()
        try:
            response = self.client.chat(model=self.model, messages=[
                {"role": "system", "content": system_prompt}, {"role": "user", "content": user_prompt}],
                format=response_format, options={"temperature": 0}, stream=False)
            inputs, outputs = number(field(response, "prompt_eval_count")), number(field(response, "eval_count"))
            duration = number(field(response, "total_duration"))
            self._usage.update(inputTokens=inputs, outputTokens=outputs,
                               totalTokens=inputs + outputs if inputs is not None and outputs is not None else None,
                               providerDurationSeconds=duration / 1e9 if duration is not None else None,
                               status="reported" if inputs is not None or outputs is not None else "unavailable")
            if field(response, "done") is not True or field(response, "error"):
                raise PromptProviderError("incomplete", "Ollama did not complete the prompt response.")
            self._usage["completedRequests"] = 1
            output = field(field(response, "message"), "content")
            if not isinstance(output, str) or not output.strip():
                raise PromptProviderError("empty_output", "Ollama returned no usable prompt text.")
            return output
        except Exception as error:
            raise provider_error(error) from None
