import argparse
import json
from pathlib import Path

from prompt.models import PromptImproverConfig
from prompt.execution import improve
from prompt.telemetry import PromptProviderError


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Improve a software development prompt."
    )

    parser.add_argument(
        "prompt",
        help="The original user prompt.",
    )

    parser.add_argument(
        "--provider",
        required=False,
        help="Optional LLM provider override: ollama, openai or chatgpt.",
    )

    parser.add_argument(
        "--model",
        required=False,
        help="Optional model override.",
    )

    parser.add_argument(
        "--output-dir",
        required=False,
        help="Optional directory for prompt artifacts.",
    )

    args = parser.parse_args()

    script_directory = Path(__file__).resolve().parent.parent
    config_path = script_directory / "config.json"

    config_data = json.loads(
        config_path.read_text(encoding="utf-8")
    )

    config = PromptImproverConfig.model_validate(config_data)

    provider = args.provider or config.provider
    model = args.model or config.model

    try:
        improved_prompt = improve(config, args.prompt,
                                  Path(args.output_dir) if args.output_dir else None, provider, model)
    except PromptProviderError as error:
        parser.exit(1, str(error) + "\n")

    print(improved_prompt.to_markdown())


if __name__ == "__main__":
    main()
