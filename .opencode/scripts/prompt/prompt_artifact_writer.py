from pathlib import Path

from prompt.models import ImprovedPrompt


class PromptArtifactWriter:

    @staticmethod
    def write(
            output_directory: Path,
            original_prompt: str,
            improved_prompt: ImprovedPrompt,
    ) -> None:
        output_directory.mkdir(
            parents=True,
            exist_ok=True,
        )

        original_path = output_directory / "original-prompt.md"
        improved_path = output_directory / "improved-prompt.md"

        original_path.write_text(
            original_prompt.strip() + "\n",
            encoding="utf-8",
        )

        improved_path.write_text(
            improved_prompt.to_markdown() + "\n",
            encoding="utf-8",
        )
