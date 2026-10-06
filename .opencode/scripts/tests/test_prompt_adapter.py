import json
import unittest
from pathlib import Path
from unittest.mock import Mock, patch
import tempfile

from workflow import prompt
from prompt.models import ImprovedPrompt, PromptImproverConfig
from prompt.prompt_improver import PromptImprover
from prompt.llm_client_factory import LlmClientFactory


class PromptTests(unittest.TestCase):
    def test_adapter_uses_config_factory_and_existing_artifact_writer(self):
        response = ImprovedPrompt(goal="Validate usernames", scope=[], requirements=["Reject empty names"],
                                  non_goals=[], verification=[], uncertainties=[])
        client = Mock()
        client.chat.return_value = response.model_dump_json()
        with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory:
            with patch.object(LlmClientFactory, "create", return_value=client) as factory:
                result = prompt.improve("Reject empty names", Path(directory))
            config = json.loads((Path(__file__).resolve().parents[1] / "prompt/config.json").read_text())
            factory.assert_called_once_with(config["provider"], config["model"])
            self.assertIn("Validate usernames", result)
            self.assertEqual("Reject empty names\n", (Path(directory) / "original-prompt.md").read_text())
            self.assertEqual(result + "\n", (Path(directory) / "improved-prompt.md").read_text())
            schema = client.chat.call_args.kwargs["response_format"]
            self.assertFalse(schema["additionalProperties"])

    def test_prompt_empty_and_invalid_structured_output_rejected(self):
        client = Mock()
        improver = PromptImprover(PromptImproverConfig(provider="chatgpt", model="test"), client)
        with self.assertRaises(ValueError):
            improver.improve(" ")
        client.chat.assert_not_called()
        client.chat.return_value = '{"invented":"field"}'
        with self.assertRaises(ValueError):
            improver.improve("task")

    def test_prompt_overrides_are_separate_from_benchmark_model(self):
        response = ImprovedPrompt(goal="Task", scope=[], requirements=[], non_goals=[], verification=[], uncertainties=[])
        client = Mock()
        client.chat.return_value = response.model_dump_json()
        with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory, patch.object(LlmClientFactory, "create", return_value=client) as factory:
            prompt.improve("task", Path(directory), "ollama", "local-model")
            factory.assert_called_once_with("ollama", "local-model")

    def test_callback_smoke_starts_server_before_wait(self):
        from prompt.tests import test_chatgpt_callback
        with patch.object(test_chatgpt_callback, "ChatGPTCallbackServer") as factory:
            test_chatgpt_callback.main()
            self.assertEqual(["start", "wait_for_callback"],
                             [call[0] for call in factory.return_value.method_calls])
