from pydantic import BaseModel, ConfigDict, Field


class PromptImproverConfig(BaseModel):
    provider: str
    model: str

    default_non_goals: list[str] = Field(default_factory=list)
    default_verification: list[str] = Field(default_factory=list)


class ImprovedPrompt(BaseModel):
    model_config = ConfigDict(
        extra="forbid"
    )

    goal: str
    scope: list[str]
    requirements: list[str]
    non_goals: list[str]
    verification: list[str]
    uncertainties: list[str]

    def to_markdown(self) -> str:
        return "\n".join([
            "## Goal",
            self.goal,
            "",
            "## Scope",
            self._format_list(self.scope),
            "",
            "## Requirements",
            self._format_list(self.requirements),
            "",
            "## Non-goals",
            self._format_list(self.non_goals),
            "",
            "## Verification",
            self._format_list(self.verification),
            "",
            "## Uncertainties",
            self._format_list(self.uncertainties),
        ])

    @staticmethod
    def _format_list(values: list[str]) -> str:
        if not values:
            return "- Not specified"

        return "\n".join(f"- {value}" for value in values)
