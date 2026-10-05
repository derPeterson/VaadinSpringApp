from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse

from pydantic import BaseModel


class ChatGPTAuthCallback(BaseModel):
    code: str
    state: str
    client_id: str | None = None
    scope: str | None = None


class ChatGPTCallbackServer:

    def __init__(
            self,
            host: str = "127.0.0.1",
            port: int = 1455,
    ):
        self.host = host
        self.port = port

        self.callback: ChatGPTAuthCallback | None = None
        self._server: HTTPServer | None = None

    @property
    def redirect_uri(self) -> str:
        return (
            f"http://{self.host}:{self.port}"
            "/auth/callback"
        )

    def start(self) -> None:
        if self._server is not None:
            return

        self._server = HTTPServer(
            (self.host, self.port),
            self._create_handler(),
        )

    def wait_for_callback(self) -> ChatGPTAuthCallback:
        if self._server is None:
            raise RuntimeError(
                "Callback server must be started first."
            )

        try:
            self._server.handle_request()
        finally:
            self._server.server_close()
            self._server = None

        if self.callback is None:
            raise RuntimeError(
                "No ChatGPT authorization callback was received."
            )

        return self.callback

    def _create_handler(self):
        parent = self

        class CallbackHandler(BaseHTTPRequestHandler):

            def do_GET(self) -> None:
                parsed_url = urlparse(self.path)

                if parsed_url.path != "/auth/callback":
                    self.send_response(404)
                    self.end_headers()
                    return

                parameters = parse_qs(parsed_url.query)

                error = parameters.get("error", [None])[0]

                if error is not None:
                    self.send_response(400)
                    self.end_headers()

                    self.wfile.write(
                        b"ChatGPT authorization failed. "
                        b"You can close this browser window."
                    )
                    return

                code = parameters.get("code", [None])[0]
                state = parameters.get("state", [None])[0]
                client_id = parameters.get("client_id", [None])[0]
                scope = parameters.get("scope", [None])[0]

                if code is None or state is None:
                    self.send_response(400)
                    self.end_headers()

                    self.wfile.write(
                        b"Invalid ChatGPT authorization callback."
                    )
                    return

                parent.callback = ChatGPTAuthCallback(
                    code=code,
                    state=state,
                    client_id=client_id,
                    scope=scope,
                )

                self.send_response(200)
                self.send_header(
                    "Content-Type",
                    "text/html; charset=utf-8",
                )
                self.end_headers()

                self.wfile.write(
                    b"""
                    <html>
                        <body>
                            <h2>ChatGPT authorization successful.</h2>
                            <p>You can close this browser window.</p>
                        </body>
                    </html>
                    """
                )

            def log_message(
                    self,
                    format: str,
                    *args,
            ) -> None:
                pass

        return CallbackHandler
