"""Shared improver execution for the workflow and standalone CLI; core unchanged."""
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import time
import uuid

from pydantic import ValidationError
from prompt.llm_client_factory import LlmClientFactory
from prompt.prompt_artifact_writer import PromptArtifactWriter
from prompt.prompt_improver import PromptImprover
from prompt.telemetry import PromptProviderError, UsageObserver, empty_usage, provider_error


def write_json(path: Path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + "." + uuid.uuid4().hex + ".tmp")
    try:
        temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


def improve(config, task, output=None, provider=None, model=None):
    effective_provider, effective_model = provider or config.provider, model or config.model
    metadata = dict(provider=effective_provider, model=effective_model,
                    startedUTC=datetime.now(timezone.utc).isoformat(), status="failed", errorCategory=None,
                    usageScope="external improver; excluded from OpenCode session usage")
    clock, client, failure = time.monotonic(), None, None
    try:
        if not task.strip():
            raise PromptProviderError("invalid_input", "The original prompt must not be empty.")
        client = LlmClientFactory.create(effective_provider, effective_model)
        try:
            result = PromptImprover(config, client).improve(task)
        except ValidationError:
            raise PromptProviderError("invalid_structured_output", "Prompt response does not match the required JSON schema.") from None
        if output is not None:
            PromptArtifactWriter.write(output, task, result)
        metadata["status"] = "completed"
        return result
    except Exception as error:
        normalized = provider_error(error)
        failure = normalized
        metadata["errorCategory"] = normalized.category
        raise normalized from None
    finally:
        metadata.update(completedUTC=datetime.now(timezone.utc).isoformat(),
                        durationSeconds=round(time.monotonic() - clock, 3))
        observed = client.get_usage() if isinstance(client, UsageObserver) else empty_usage()
        if client is not None and not isinstance(client, UsageObserver):
            # Third-party clients without instrumentation cannot claim zero requests.
            observed.update(requests=None, completedRequests=None, retries=None)
        metadata["usage"] = observed
        if isinstance(client, UsageObserver) and hasattr(client, "close"):
            try:
                client.close()
            except Exception:
                # Cleanup must not replace the observed response or provider error.
                metadata["cleanupWarning"] = "Provider connection cleanup failed."
        if output is not None:
            try:
                write_json(output / "prompt-metadata.json", metadata)
                write_json(output / "prompt-usage.json", metadata)
            except Exception as archive_error:
                if failure is None:
                    raise
                failure.__notes__ = ["Prompt metadata archive failed (" + type(archive_error).__name__ + ")."]
