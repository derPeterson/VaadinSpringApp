from pathlib import Path
import time
from datetime import datetime, timezone


def improve(task: str, output: Path, provider: str | None = None,
            model: str | None = None) -> str:
    # Lazy imports keep branch/benchmark/usage stdlib-only. Existing core is untouched.
    from prompt.llm_client_factory import LlmClientFactory
    from prompt.models import PromptImproverConfig
    from prompt.prompt_artifact_writer import PromptArtifactWriter
    from prompt.prompt_improver import PromptImprover
    from .common import read_json, write_json

    config_path = Path(__file__).resolve().parents[1] / "prompt" / "config.json"
    config = PromptImproverConfig.model_validate(read_json(config_path))
    effective_provider, effective_model = provider or config.provider, model or config.model
    started = datetime.now(timezone.utc).isoformat()
    clock = time.monotonic()
    client = LlmClientFactory.create(effective_provider, effective_model)
    improved = PromptImprover(config, client).improve(task)
    PromptArtifactWriter.write(output, task, improved)
    write_json(output / "prompt-metadata.json", {
        "provider": effective_provider, "model": effective_model,
        "startedUTC": started, "completedUTC": datetime.now(timezone.utc).isoformat(),
        "durationSeconds": round(time.monotonic() - clock, 3),
        "usageScope": "external improver; excluded from OpenCode session usage",
    })
    return improved.to_markdown()
