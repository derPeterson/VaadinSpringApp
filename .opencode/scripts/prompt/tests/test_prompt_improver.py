import json
from pathlib import Path

from prompt.models import PromptImproverConfig
from prompt.ollama_client import OllamaClient
from prompt.prompt_improver import PromptImprover


def main() -> None:
    script_directory = Path(__file__).resolve().parent.parent
    config_path = script_directory / "config.json"

    config_data = json.loads(
        config_path.read_text(encoding="utf-8")
    )

    config = PromptImproverConfig.model_validate(config_data)

    client = OllamaClient("qwen3-coder-q3-tools:latest")
    improver = PromptImprover(
        config=config,
        llm_client=client,
    )

    improved_prompt = improver.improve(
        "Improve email normalization."
    )

    print(improved_prompt.to_markdown())


if __name__ == "__main__":
    main()
