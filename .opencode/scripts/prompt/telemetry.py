"""Provider observations only: no prices, prompt text or credentials are inferred."""
from copy import deepcopy
import math


class PromptProviderError(RuntimeError):
    def __init__(self, category: str, message: str):
        self.category = category
        super().__init__(message)


LOGIN_REQUIRED = "ChatGPT login required. Run python -B -m prompt.tests.test_chatgpt_login from .opencode/scripts."


def field(value, name, default=None):
    return value.get(name, default) if isinstance(value, dict) else getattr(value, name, default)


def number(value):
    return value if type(value) in (int, float) and math.isfinite(value) and value >= 0 else None


def empty_usage():
    return dict(inputTokens=None, outputTokens=None, reasoningTokens=None, cacheReadTokens=None,
                cacheWriteTokens=None, totalTokens=None, requests=0, completedRequests=0, retries=0,
                providerDurationSeconds=None, estimatedCostUSD=None, costStatus="unavailable",
                source="provider response", status="unavailable")


class UsageObserver:
    def start_request(self):
        self._usage = empty_usage()
        self._usage["requests"] = 1  # One inference request attempt; auth calls excluded.

    def get_usage(self):
        return deepcopy(getattr(self, "_usage", empty_usage()))

    def observe_response(self, response):
        raw = field(response, "usage")
        if raw is not None:
            inputs, outputs = field(raw, "input_tokens_details"), field(raw, "output_tokens_details")
            self._usage.update(inputTokens=number(field(raw, "input_tokens")),
                               outputTokens=number(field(raw, "output_tokens")),
                               totalTokens=number(field(raw, "total_tokens")),
                               reasoningTokens=number(field(outputs, "reasoning_tokens")),
                               cacheReadTokens=number(field(inputs, "cached_tokens")),
                               cacheWriteTokens=number(field(inputs, "cache_write_tokens")))
            self._usage["status"] = "reported" if any(self._usage[key] is not None for key in
                ("inputTokens", "outputTokens", "totalTokens", "reasoningTokens", "cacheReadTokens", "cacheWriteTokens")) else "unavailable"
        if field(response, "status") == "completed":
            self._usage["completedRequests"] = 1


def checked_text(response, streamed_text=None, streamed_refusal=False):
    status = field(response, "status")
    if status != "completed":
        category = "incomplete" if status == "incomplete" else "provider_failure"
        raise PromptProviderError(category, "Prompt response did not complete successfully (" + category + ").")
    texts = []
    for item in field(response, "output", []) or []:
        for part in field(item, "content", []) or []:
            if field(part, "type") == "refusal":
                raise PromptProviderError("refusal", "Provider declined the prompt-improver request.")
            if field(item, "type") == "message" and field(part, "type") == "output_text":
                text = field(part, "text")
                if isinstance(text, str):
                    texts.append(text)
    if streamed_refusal:
        raise PromptProviderError("refusal", "Provider declined the prompt-improver request.")
    output = field(response, "output_text")
    if not isinstance(output, str) or not output.strip():
        output = "".join(texts)
    if not output.strip() and isinstance(streamed_text, str):
        output = streamed_text
    if not isinstance(output, str) or not output.strip():
        raise PromptProviderError("empty_output", "Provider returned no usable prompt text.")
    return output


def provider_error(error):
    if isinstance(error, PromptProviderError):
        return error
    status = getattr(error, "status_code", None)
    name = type(error).__name__
    category = ("authentication" if status in (401, 403) else "rate_limit" if status == 429 else
                "timeout" if "Timeout" in name else "connection" if "Connection" in name or "Connect" in name else
                "provider_failure")
    # Raw SDK errors can include request bodies or credentials. Do not archive them.
    return PromptProviderError(category, "Prompt provider request failed (" + category + ").")
