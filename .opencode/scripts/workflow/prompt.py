from pathlib import Path


def improve(task: str, output: Path, provider: str | None = None,
            model: str | None = None) -> str:
    # Lazy imports keep branch/benchmark/usage stdlib-only. Existing core is untouched.
    from prompt.llm_client_factory import LlmClientFactory
    from prompt.models import PromptImproverConfig
    from prompt.prompt_artifact_writer import PromptArtifactWriter
    from prompt.prompt_improver import PromptImprover
    from .common import read_json

    config_path = Path(__file__).resolve().parents[1] / "prompt" / "config.json"
    config = PromptImproverConfig.model_validate(read_json(config_path))
    client = LlmClientFactory.create(provider or config.provider, model or config.model)
    improved = PromptImprover(config, client).improve(task)
    PromptArtifactWriter.write(output, task, improved)
    return improved.to_markdown()
