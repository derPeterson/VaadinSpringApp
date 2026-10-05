from prompt.llm_client import LlmClient
from prompt.models import ImprovedPrompt, PromptImproverConfig


def _build_system_prompt() -> str:
    return """
You are a prompt improver for software development tasks.

Your job is to clarify and structure the user's original request without
changing its intent.

Rules:
- Preserve the user's original intent.
- Do not invent product requirements.
- Do not invent architecture decisions.
- Do not assume classes, files, frameworks, databases, APIs, or technologies
  that were not mentioned by the user.
- Do not add compatibility, performance, security, migration, logging,
  documentation, or validation requirements unless they are explicitly stated
  or clearly required by the original request.
- Do not expand the scope unnecessarily.
- Requirements must be directly supported by the original request.
- If something would require an assumption, put it into "uncertainties"
  instead of "requirements".
- Avoid duplicate or semantically equivalent entries.
- Prefer fewer, precise entries over multiple vague entries.
- Do not restate the goal as multiple generic requirements.

Scope rules:
- "scope" describes the functional area affected by the request.
- Add a scope entry when it can be directly derived from the user's words.
- Do not invent concrete files, classes, components, or implementation details.
- If no meaningful scope can be derived, return an empty list.

Uncertainty rules:
- Missing details must be listed under "uncertainties".
- Do not resolve uncertainties by guessing.

Keep the result concise.
""".strip()


class PromptImprover:

    def __init__(
            self,
            config: PromptImproverConfig,
            llm_client: LlmClient,
    ):
        self.config = config
        self.llm_client = llm_client

    def improve(self, raw_prompt: str) -> ImprovedPrompt:
        raw_prompt = raw_prompt.strip()

        if not raw_prompt:
            raise ValueError("The prompt must not be empty.")

        response = self.llm_client.chat(
            system_prompt=_build_system_prompt(),
            user_prompt=raw_prompt,
            response_format=ImprovedPrompt.model_json_schema(),
        )

        improved_prompt = ImprovedPrompt.model_validate_json(response)

        return self._apply_defaults(improved_prompt)

    def _apply_defaults(
            self,
            improved_prompt: ImprovedPrompt,
    ) -> ImprovedPrompt:
        improved_prompt.non_goals = (
                self.config.default_non_goals
                + improved_prompt.non_goals
        )

        improved_prompt.verification = (
                self.config.default_verification
                + improved_prompt.verification
        )

        return improved_prompt
