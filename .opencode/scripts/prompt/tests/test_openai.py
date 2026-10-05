from prompt.openai_client import OpenAIClient


def main() -> None:
    client = OpenAIClient("gpt-5.5")

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
