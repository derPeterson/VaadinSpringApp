from pathlib import Path


# Keep result/CSV definitions available without importing optional provider dependencies.
USAGE_COLUMNS = {
    "PromptInputTokens": "inputTokens", "PromptOutputTokens": "outputTokens",
    "PromptReasoningTokens": "reasoningTokens", "PromptCacheReadTokens": "cacheReadTokens",
    "PromptCacheWriteTokens": "cacheWriteTokens", "PromptTotalTokens": "totalTokens",
    "PromptRequests": "requests", "PromptCompletedRequests": "completedRequests",
    "PromptRetries": "retries", "PromptProviderDurationSeconds": "providerDurationSeconds",
    "PromptEstimatedCostUSD": "estimatedCostUSD", "PromptCostStatus": "costStatus",
    "PromptUsageStatus": "status",
}
METADATA_COLUMNS = {"PromptStatus": "status", "PromptErrorCategory": "errorCategory"}


def improve(task: str, output: Path, provider: str | None = None,
            model: str | None = None) -> str:
    # Lazy imports keep branch/benchmark/usage stdlib-only. Existing core is untouched.
    from prompt.models import PromptImproverConfig
    from prompt.execution import improve as execute
    from .common import read_json

    config_path = Path(__file__).resolve().parents[1] / "prompt" / "config.json"
    config = PromptImproverConfig.model_validate(read_json(config_path))
    improved = execute(config, task, output, provider, model)
    return improved.to_markdown()
