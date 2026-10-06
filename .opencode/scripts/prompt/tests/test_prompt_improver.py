import json
from pathlib import Path

from prompt.models import PromptImproverConfig
from prompt.llm_client_factory import LlmClientFactory
from prompt.prompt_improver import PromptImprover


def main() -> None:
    script_directory = Path(__file__).resolve().parent.parent
    config_path = script_directory / "config.json"

    config_data = json.loads(
        config_path.read_text(encoding="utf-8")
    )

    config = PromptImproverConfig.model_validate(config_data)

    client = LlmClientFactory.create(config.provider, config.model)
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
