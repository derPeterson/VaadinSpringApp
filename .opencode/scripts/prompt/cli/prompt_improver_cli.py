import argparse
import json
from pathlib import Path

from prompt.llm_client_factory import LlmClientFactory
from prompt.models import PromptImproverConfig
from prompt.prompt_artifact_writer import PromptArtifactWriter
from prompt.prompt_improver import PromptImprover


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

    client = LlmClientFactory.create(
        provider=provider,
        model=model,
    )

    improver = PromptImprover(
        config=config,
        llm_client=client,
    )

    improved_prompt = improver.improve(args.prompt)

    if args.output_dir:
        PromptArtifactWriter.write(
            output_directory=Path(args.output_dir),
            original_prompt=args.prompt,
            improved_prompt=improved_prompt,
        )

    print(improved_prompt.to_markdown())


if __name__ == "__main__":
    main()
