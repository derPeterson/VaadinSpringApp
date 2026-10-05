from prompt.ollama_client import OllamaClient


def main() -> None:
    client = OllamaClient("qwen3-coder-q3-tools:latest")

    response = client.chat(
        system_prompt=(
            "You are a prompt improver for software development tasks. "
            "Preserve the user's intent. "
            "Do not invent new requirements."
        ),
        user_prompt="Improve email normalization.",
    )

    print(response)


if __name__ == "__main__":
    main()
