from prompt.chatgpt_client import ChatGPTClient
from prompt.models import ImprovedPrompt


def main() -> None:
    client = ChatGPTClient(
        model="gpt-6.1-sol"
    )

    response = client.chat(
        system_prompt=(
            "You are a prompt improver for software development tasks. "
            "Return only data matching the given JSON schema."
        ),
        user_prompt=(
            "Improve this task: "
            "Add validation for empty usernames."
        ),
        response_format=ImprovedPrompt.model_json_schema(),
    )

    print(response)

    improved = ImprovedPrompt.model_validate_json(
        response
    )

    print()
    print("Parsed successfully:")
    print(improved.to_markdown())


if __name__ == "__main__":
    main()
