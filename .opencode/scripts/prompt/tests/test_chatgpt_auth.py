from prompt.chatgpt.auth import ChatGPTAuth


def main() -> None:
    host_id = ChatGPTAuth.create_host_id()

    attempt = ChatGPTAuth.create_attempt(
        ext_agent_host_id=host_id
    )

    authorization_url = ChatGPTAuth.build_authorization_url(
        attempt=attempt,
        redirect_uri="http://127.0.0.1:1455/auth/callback",
        agent_name="Vaadin Prompt Improver",
    )

    print(authorization_url)


if __name__ == "__main__":
    main()
