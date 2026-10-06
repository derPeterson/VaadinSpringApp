from prompt.chatgpt.callback_server import (
    ChatGPTCallbackServer,
)


def main() -> None:
    server = ChatGPTCallbackServer()
    server.start()

    print(
        f"Waiting for callback on: "
        f"{server.redirect_uri}"
    )

    callback = server.wait_for_callback()

    print(callback.model_dump_json(indent=2))


if __name__ == "__main__":
    main()
