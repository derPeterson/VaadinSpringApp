"""Offline provider/auth regression tests: fake responses and fake keyring only."""
import csv
from datetime import datetime, timezone
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import Mock, patch

import httpx
import keyring
from prompt.chatgpt.credentials import ChatGPTCredentials, ChatGPTRefreshResponse
from prompt.chatgpt.credential_store import ChatGPTCredentialStore
from prompt.chatgpt.token_client import ChatGPTTokenClient
from prompt.chatgpt.session_lock import session_lock
from prompt.chatgpt_client import ChatGPTClient
from prompt.openai_client import OpenAIClient
from prompt.ollama_client import OllamaClient
from prompt.telemetry import PromptProviderError, UsageObserver, number
from prompt.execution import improve
from prompt.models import PromptImproverConfig
from workflow import benchmark, prompt, runner

VALID = json.dumps(dict(goal="Review only", scope=[], requirements=[], non_goals=[], verification=[], uncertainties=[]))


def response(status="completed", text=VALID, usage=True, output=None):
    return dict(status=status, output_text=text, output=output or [], usage=dict(
        input_tokens=100, output_tokens=30, total_tokens=130,
        input_tokens_details=dict(cached_tokens=80, cache_write_tokens=0),
        output_tokens_details=dict(reasoning_tokens=10)) if usage else None)


def credentials():
    return ChatGPTCredentials(email="test@example.invalid", issuer="issuer", subject="subject",
                              client_id="fixture", ext_agent_host_id="host", access_token="fake-access",
                              refresh_token="fake-refresh", id_token="fake-id", token_type="Bearer",
                              expires_in=3600, scopes=["chatgpt.tokens.use.direct"],
                              saved_at=datetime.now(timezone.utc).isoformat())


class FakeKeyring:
    def __init__(self, path=None):
        self.data = {}
        self.path = path

    def read(self):
        return json.loads(self.path.read_text()) if self.path and self.path.exists() else self.data

    def write(self, data):
        if self.path:
            self.path.write_text(json.dumps(data), encoding="utf-8")
        else:
            self.data = data

    def get_password(self, service, key):
        return self.read().get(service + "/" + key)

    def set_password(self, service, key, value):
        data = self.read()
        data[service + "/" + key] = value
        self.write(data)

    def delete_password(self, service, key):
        data = self.read()
        data.pop(service + "/" + key, None)
        self.write(data)


def fake_keyring(backend):
    return patch.multiple(keyring, get_password=backend.get_password,
                          set_password=backend.set_password, delete_password=backend.delete_password)


class ProviderTests(unittest.TestCase):
    def openai(self, value):
        with patch("prompt.openai_client.OpenAI") as factory:
            client = OpenAIClient("fixture-model")
        client.client.responses.create.return_value = value
        return client, factory

    def chatgpt(self, events):
        factory = Mock()
        sdk = factory.return_value.__enter__ = Mock(return_value=Mock())
        factory.return_value.__exit__ = Mock(return_value=False)
        stream = Mock()
        stream.__enter__ = Mock(return_value=iter(events))
        stream.__exit__ = Mock(return_value=False)
        sdk.return_value.responses.create.return_value = stream
        client = ChatGPTClient("fixture-model")
        client.credential_store = Mock()
        client.credential_store.credentials_for_request.return_value = credentials()
        return client, factory, stream

    def test_openai_strict_output_and_reported_usage(self):
        client, factory = self.openai(response())
        self.assertEqual(VALID, client.chat("system", "task", {"type": "object"}))
        factory.assert_called_once_with(max_retries=0, timeout=120.0)
        self.assertTrue(client.client.responses.create.call_args.kwargs["text"]["format"]["strict"])
        data = client.get_usage()
        self.assertEqual((100, 30, 10, 80, 0, 130), tuple(data[k] for k in
                         ("inputTokens", "outputTokens", "reasoningTokens", "cacheReadTokens", "cacheWriteTokens", "totalTokens")))
        self.assertEqual((1, 1, 0), (data["requests"], data["completedRequests"], data["retries"]))
        self.assertIsNone(data["estimatedCostUSD"])
        self.assertEqual("unavailable", data["costStatus"])

    def test_missing_token_details_stay_unknown(self):
        value = response()
        value["usage"] = dict(input_tokens=0, output_tokens=0, total_tokens=0)
        client, _ = self.openai(value)
        client.chat("s", "u")
        self.assertEqual(0, client.get_usage()["inputTokens"])
        self.assertIsNone(client.get_usage()["cacheReadTokens"])
        self.assertIsNone(client.get_usage()["reasoningTokens"])

    def test_missing_usage_is_not_zero(self):
        client, _ = self.openai(response(usage=False))
        client.chat("s", "u")
        self.assertEqual("unavailable", client.get_usage()["status"])
        self.assertIsNone(client.get_usage()["inputTokens"])
        self.assertEqual(1, client.get_usage()["completedRequests"])

    def test_empty_usage_object_is_unavailable(self):
        value = response()
        value["usage"] = {}
        client, _ = self.openai(value)
        client.chat("s", "u")
        self.assertEqual("unavailable", client.get_usage()["status"])

    def test_actual_sdk_response_and_completed_stream_event_contract(self):
        from openai.types.responses import Response, ResponseCompletedEvent
        value = response()
        value.pop("output_text")
        value.update(id="resp_fixture", created_at=1, error=None, incomplete_details=None,
                     instructions=None, metadata={}, model="fixture", object="response", parallel_tool_calls=True,
                     tool_choice="auto", tools=[], temperature=1, top_p=1,
                     output=[dict(id="msg_fixture", type="message", role="assistant", status="completed",
                                  content=[dict(type="output_text", text=VALID, annotations=[], logprobs=[])])])
        parsed = Response.model_validate(value)
        client, _ = self.openai(parsed)
        self.assertEqual(VALID, client.chat("s", "u"))
        event = ResponseCompletedEvent.model_validate(dict(type="response.completed", sequence_number=1, response=value))
        streaming, factory, _ = self.chatgpt([event])
        with patch("prompt.chatgpt_client.OpenAI", factory):
            self.assertEqual(VALID, streaming.chat("s", "u"))
        self.assertEqual(10, streaming.get_usage()["reasoningTokens"])
        self.assertEqual(80, streaming.get_usage()["cacheReadTokens"])

    def test_login_marker_write_failure_keeps_auth_error_sanitized(self):
        client, factory, _ = self.chatgpt([])
        error = RuntimeError("private access token")
        error.status_code = 401
        factory.return_value.__enter__.return_value.responses.create.side_effect = error
        client.credential_store.require_login.side_effect = RuntimeError("private storage secret")
        with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertEqual("authentication", caught.exception.category)
        self.assertNotIn("private", str(caught.exception))
        self.assertIn("reauthentication marker", caught.exception.__notes__[0])

    def test_nonfinite_negative_boolean_counters_rejected(self):
        for value in (-1, True, float("nan"), float("inf"), "20"):
            self.assertIsNone(number(value))
        self.assertEqual(0, number(0))

    def test_incomplete_response_preserves_usage_and_rejects_text(self):
        client, _ = self.openai(response("incomplete"))
        with self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertEqual("incomplete", caught.exception.category)
        self.assertEqual(100, client.get_usage()["inputTokens"])
        self.assertEqual(0, client.get_usage()["completedRequests"])

    def test_refusal_does_not_become_an_improved_prompt(self):
        client, _ = self.openai(response(output=[dict(content=[dict(type="refusal", refusal="private text")])]))
        with self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertEqual("refusal", caught.exception.category)
        self.assertNotIn("private text", str(caught.exception))

    def test_empty_output_rejected(self):
        client, _ = self.openai(response(text=" "))
        with self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertEqual("empty_output", caught.exception.category)

    def test_provider_http_errors_sanitized_without_retry(self):
        for status, category in ((401, "authentication"), (403, "authentication"), (429, "rate_limit"), (503, "provider_failure")):
            with self.subTest(status=status):
                client, _ = self.openai(response())
                error = RuntimeError("Bearer secret-and-request-body")
                error.status_code = status
                client.client.responses.create.side_effect = error
                with self.assertRaises(PromptProviderError) as caught:
                    client.chat("s", "u")
                self.assertEqual(category, caught.exception.category)
                self.assertNotIn("secret", str(caught.exception))
                self.assertEqual(1, client.client.responses.create.call_count)
                self.assertEqual(0, client.get_usage()["completedRequests"])

    def test_timeout_and_connection_errors_classified(self):
        for error, expected in ((httpx.ReadTimeout("private"), "timeout"), (httpx.ConnectError("private"), "connection")):
            client, _ = self.openai(response())
            client.client.responses.create.side_effect = error
            with self.assertRaises(PromptProviderError) as caught:
                client.chat("s", "u")
            self.assertEqual(expected, caught.exception.category)

    def test_chatgpt_final_response_owns_text_and_usage(self):
        events = [dict(type="response.output_text.delta", delta="wrong duplicate"),
                  dict(type="response.completed", response=response())]
        client, factory, stream = self.chatgpt(events)
        with patch("prompt.chatgpt_client.OpenAI", factory):
            self.assertEqual(VALID, client.chat("s", "u", {}))
        self.assertEqual(130, client.get_usage()["totalTokens"])
        self.assertEqual(1, client.get_usage()["requests"])
        stream.__exit__.assert_called_once()

    def test_chatgpt_actual_sdk_text_events_with_empty_terminal_output(self):
        from openai.types.responses import ResponseCompletedEvent, ResponseTextDeltaEvent, ResponseTextDoneEvent
        raw = response(text="")
        raw.pop("output_text")
        raw.update(id="resp_fixture", created_at=1, error=None, incomplete_details=None,
                   instructions=None, metadata={}, model="fixture", object="response", parallel_tool_calls=True,
                   tool_choice="auto", tools=[], temperature=1, top_p=1)
        events = [ResponseTextDeltaEvent.model_validate(dict(type="response.output_text.delta",
                  item_id="msg_fixture", output_index=0, content_index=0, sequence_number=1, delta=VALID, logprobs=[])),
                  ResponseTextDoneEvent.model_validate(dict(type="response.output_text.done",
                  item_id="msg_fixture", output_index=0, content_index=0, sequence_number=2, text=VALID, logprobs=[])),
                  ResponseCompletedEvent.model_validate(dict(type="response.completed", sequence_number=3, response=raw))]
        client, factory, _ = self.chatgpt(events)
        with patch("prompt.chatgpt_client.OpenAI", factory):
            self.assertEqual(VALID, client.chat("s", "u"))
        self.assertEqual((1, 1, 0, 130), tuple(client.get_usage()[k] for k in
                         ("requests", "completedRequests", "retries", "totalTokens")))

    def test_chatgpt_delta_only_text_with_completed_response(self):
        events = [dict(type="response.output_text.delta", delta=VALID[:20]),
                  dict(type="response.output_text.delta", delta=VALID[20:]),
                  dict(type="response.completed", response=response(text=""))]
        client, factory, _ = self.chatgpt(events)
        with patch("prompt.chatgpt_client.OpenAI", factory):
            self.assertEqual(VALID, client.chat("s", "u"))

    def test_chatgpt_done_text_is_not_added_to_deltas(self):
        events = [dict(type="response.output_text.delta", delta="partial"),
                  dict(type="response.output_text.done", text=VALID),
                  dict(type="response.completed", response=response(text=""))]
        client, factory, _ = self.chatgpt(events)
        with patch("prompt.chatgpt_client.OpenAI", factory):
            self.assertEqual(VALID, client.chat("s", "u"))

    def test_chatgpt_done_only_text_preserved(self):
        events = [dict(type="response.output_text.done", text=VALID),
                  dict(type="response.completed", response=response(text=""))]
        client, factory, _ = self.chatgpt(events)
        with patch("prompt.chatgpt_client.OpenAI", factory):
            self.assertEqual(VALID, client.chat("s", "u"))

    def test_chatgpt_parts_ordered_and_finalized_independently(self):
        events = [dict(type="response.output_text.delta", output_index=1, content_index=0, delta=VALID[20:]),
                  dict(type="response.output_text.delta", output_index=0, content_index=1, delta=VALID[10:20]),
                  dict(type="response.output_text.delta", output_index=0, content_index=0, delta=VALID[:10]),
                  dict(type="response.output_text.done", output_index=0, content_index=0, text=VALID[:10]),
                  dict(type="response.completed", response=response(text=""))]
        client, factory, _ = self.chatgpt(events)
        with patch("prompt.chatgpt_client.OpenAI", factory):
            self.assertEqual(VALID, client.chat("s", "u"))

    def test_chatgpt_stream_refusal_rejects_text_without_terminal_refusal(self):
        for kind in ("response.refusal.delta", "response.refusal.done"):
            client, factory, _ = self.chatgpt([dict(type="response.output_text.delta", delta=VALID),
                                               dict(type=kind), dict(type="response.completed", response=response())])
            with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
                client.chat("s", "u")
            self.assertEqual("refusal", caught.exception.category)

    def test_chatgpt_failure_or_incomplete_never_accepts_stream_text(self):
        for status, category in (("failed", "provider_failure"), ("incomplete", "incomplete")):
            client, factory, _ = self.chatgpt([dict(type="response.output_text.delta", delta=VALID),
                                               dict(type="response." + status, response=response(status, text=""))])
            with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
                client.chat("s", "u")
            self.assertEqual(category, caught.exception.category)

    def test_chatgpt_genuine_empty_output_remains_failure_with_usage(self):
        client, factory, _ = self.chatgpt([dict(type="response.completed", response=response(text=""))])
        with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertEqual("empty_output", caught.exception.category)
        self.assertEqual(1, client.get_usage()["completedRequests"])
        self.assertEqual(30, client.get_usage()["outputTokens"])

    def test_chatgpt_stream_text_reaches_schema_validation_and_artifacts(self):
        client, factory, _ = self.chatgpt([dict(type="response.output_text.delta", delta=VALID),
                                          dict(type="response.completed", response=response(text=""))])
        config = PromptImproverConfig(provider="chatgpt", model="fixture")
        with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory:
            output = Path(directory)
            with patch("prompt.chatgpt_client.OpenAI", factory), patch("prompt.execution.LlmClientFactory.create", return_value=client):
                result = improve(config, "Analyze UserService only", output)
            self.assertEqual("Review only", result.goal)
            self.assertEqual("Analyze UserService only\n", (output / "original-prompt.md").read_text())
            self.assertIn("Review only", (output / "improved-prompt.md").read_text())
            data = json.loads((output / "prompt-metadata.json").read_text())
            self.assertEqual("completed", data["status"])
            self.assertEqual(1, data["usage"]["requests"])

    def test_chatgpt_stream_invalid_json_still_fails_schema(self):
        client, factory, _ = self.chatgpt([dict(type="response.output_text.done", text="{broken"),
                                          dict(type="response.completed", response=response(text=""))])
        with patch("prompt.chatgpt_client.OpenAI", factory), patch("prompt.execution.LlmClientFactory.create", return_value=client), self.assertRaises(PromptProviderError) as caught:
            improve(PromptImproverConfig(provider="chatgpt", model="fixture"), "task")
        self.assertEqual("invalid_structured_output", caught.exception.category)

    def test_response_dictionary_text_blocks_without_sdk_convenience_property(self):
        value = response()
        value.pop("output_text")
        value["output"] = [dict(type="reasoning", summary=[dict(text="not prompt text")]),
                           dict(type="message", content=[dict(type="output_text", text=VALID)])]
        client, _ = self.openai(value)
        self.assertEqual(VALID, client.chat("s", "u"))

    def test_chatgpt_content_part_or_item_done_text_preserved(self):
        for event in (dict(type="response.content_part.done", part=dict(type="output_text", text=VALID)),
                      dict(type="response.output_item.done", item=dict(type="message",
                           content=[dict(type="output_text", text=VALID)]))):
            client, factory, _ = self.chatgpt([event, dict(type="response.completed", response=response(text=""))])
            with patch("prompt.chatgpt_client.OpenAI", factory):
                self.assertEqual(VALID, client.chat("s", "u"))

    def test_chatgpt_content_part_or_item_refusal_rejected(self):
        for event in (dict(type="response.content_part.done", part=dict(type="refusal", refusal="private")),
                      dict(type="response.output_item.done", item=dict(type="message",
                           content=[dict(type="refusal", refusal="private")]))):
            client, factory, _ = self.chatgpt([event, dict(type="response.completed", response=response())])
            with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
                client.chat("s", "u")
            self.assertEqual("refusal", caught.exception.category)
            self.assertNotIn("private", str(caught.exception))

    def test_chatgpt_empty_finalized_part_is_not_replaced_by_old_delta(self):
        client, factory, _ = self.chatgpt([dict(type="response.output_text.delta", delta=VALID),
                                          dict(type="response.output_text.done", text=""),
                                          dict(type="response.completed", response=response(text=""))])
        with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertEqual("empty_output", caught.exception.category)

    def test_chatgpt_finalized_text_without_response_completed_rejected(self):
        client, factory, _ = self.chatgpt([dict(type="response.output_text.done", text=VALID)])
        with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertEqual("interrupted_stream", caught.exception.category)

    def test_chatgpt_stream_text_not_reused_by_next_request(self):
        client, factory, _ = self.chatgpt([dict(type="response.output_text.delta", delta=VALID),
                                          dict(type="response.completed", response=response(text=""))])
        with patch("prompt.chatgpt_client.OpenAI", factory):
            self.assertEqual(VALID, client.chat("s", "u"))
        empty, next_factory, _ = self.chatgpt([dict(type="response.completed", response=response(text=""))])
        with patch("prompt.chatgpt_client.OpenAI", next_factory), self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertEqual("empty_output", caught.exception.category)

    def test_chatgpt_terminal_failure_variants(self):
        for event, expected in ((dict(type="response.failed", response=response("failed")), "provider_failure"),
                                (dict(type="response.incomplete", response=response("incomplete")), "incomplete"),
                                (dict(type="error", message="private"), "provider_failure")):
            client, factory, stream = self.chatgpt([event])
            with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
                client.chat("s", "u")
            self.assertEqual(expected, caught.exception.category)
            stream.__exit__.assert_called_once()

    def test_chatgpt_interrupted_stream_keeps_unknown_tokens(self):
        client, factory, _ = self.chatgpt([dict(type="response.output_text.delta", delta=VALID)])
        with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertEqual("interrupted_stream", caught.exception.category)
        self.assertIsNone(client.get_usage()["inputTokens"])

    def test_chatgpt_auth_failure_does_not_claim_inference_request(self):
        client, factory, _ = self.chatgpt([])
        client.credential_store.credentials_for_request.side_effect = PromptProviderError("authentication", "Sign in.")
        with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError):
            client.chat("s", "u")
        factory.assert_not_called()
        self.assertEqual(0, client.get_usage()["requests"])

    def test_chatgpt_rejected_access_token_requires_explicit_login(self):
        client, factory, _ = self.chatgpt([])
        error = RuntimeError("secret")
        error.status_code = 401
        factory.return_value.__enter__.return_value.responses.create.side_effect = error
        with patch("prompt.chatgpt_client.OpenAI", factory), self.assertRaises(PromptProviderError) as caught:
            client.chat("s", "u")
        self.assertIn("test_chatgpt_login", str(caught.exception))
        client.credential_store.require_login.assert_called_once_with("fixture", "fake-access")

    def test_ollama_counts_and_nanoseconds_without_fake_reasoning_cache_cost(self):
        with patch("prompt.ollama_client.ollama.Client") as factory:
            client = OllamaClient("fixture")
        client.client.chat.return_value = dict(done=True, prompt_eval_count=10, eval_count=4,
                                              total_duration=1500000000, message=dict(content=VALID, thinking="private"))
        self.assertEqual(VALID, client.chat("s", "u", {}))
        factory.assert_called_once_with(timeout=120.0)
        data = client.get_usage()
        self.assertEqual((10, 4, 14, 1.5), tuple(data[k] for k in
                         ("inputTokens", "outputTokens", "totalTokens", "providerDurationSeconds")))
        for key in ("reasoningTokens", "cacheReadTokens", "cacheWriteTokens", "estimatedCostUSD"):
            self.assertIsNone(data[key])

    def test_ollama_unfinished_output_rejected(self):
        with patch("prompt.ollama_client.ollama.Client"):
            client = OllamaClient("fixture")
        client.client.chat.return_value = dict(done=False, message=dict(content=VALID))
        with self.assertRaises(PromptProviderError):
            client.chat("s", "u")
        self.assertEqual(0, client.get_usage()["completedRequests"])

    def test_usage_observer_is_a_snapshot_and_resets_between_calls(self):
        client, _ = self.openai(response())
        client.chat("s", "u")
        data = client.get_usage()
        data["inputTokens"] = 500
        self.assertEqual(100, client.get_usage()["inputTokens"])
        client.client.responses.create.return_value = response(usage=False)
        client.chat("s", "u")
        self.assertIsNone(client.get_usage()["inputTokens"])


class AuthTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(dir=Path.cwd())
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.backend = FakeKeyring()
        self.patch = fake_keyring(self.backend)
        self.patch.start()
        self.addCleanup(self.patch.stop)
        self.store = ChatGPTCredentialStore(self.root / "profile", self.root / "session.lock")

    def test_legacy_credentials_load_and_migrate_without_plaintext_secrets(self):
        old = credentials()
        from prompt.chatgpt.credential_store import ChatGPTProfileMetadata
        self.store.profile_path.parent.mkdir()
        self.store.profile_path.write_text(ChatGPTProfileMetadata.model_validate(old.model_dump()).model_dump_json())
        for name in ("access_token", "refresh_token", "id_token"):
            self.backend.set_password(self.store.KEYRING_SERVICE, f"fixture:{name}:meta", "1")
            self.backend.set_password(self.store.KEYRING_SERVICE, f"fixture:{name}:0", getattr(old, name))
        self.assertEqual(old, self.store.load_credentials())
        self.store.save_credentials(old)
        self.assertEqual(old, self.store.load_credentials())
        self.assertFalse(any(":access_token:" in key or ":refresh_token:" in key for key in self.backend.data))
        self.assertNotIn("fake-access", self.store.profile_path.read_text())

    def test_replacing_large_snapshot_removes_old_chunks(self):
        old = credentials()
        old.access_token = "a" * 4000
        self.store.save_credentials(old)
        keys = set(self.backend.data)
        new = credentials()
        self.store.save_credentials(new)
        self.assertEqual(new, self.store.load_credentials())
        self.assertFalse((keys - {self.store.KEYRING_SERVICE + "/fixture:credentials:meta"}) & set(self.backend.data))

    def test_failed_chunk_write_preserves_previous_whole_snapshot(self):
        old = credentials()
        self.store.save_credentials(old)
        new = credentials()
        new.access_token = "new" * 1000
        original_set = self.backend.set_password
        count = [0]

        def fail(service, key, value):
            count[0] += 1
            if count[0] == 2:
                raise keyring.errors.KeyringError("fixture write failure")
            original_set(service, key, value)

        with patch.object(keyring, "set_password", side_effect=fail), self.assertRaises(keyring.errors.KeyringError):
            self.store.save_credentials(new)
        self.assertEqual(old, self.store.load_credentials())

    def test_failed_pointer_publish_preserves_previous_snapshot(self):
        old = credentials()
        self.store.save_credentials(old)
        original_set = self.backend.set_password

        def fail(service, key, value):
            if key.endswith(":meta"):
                raise keyring.errors.KeyringError("fixture pointer failure")
            original_set(service, key, value)

        with patch.object(keyring, "set_password", side_effect=fail), self.assertRaises(keyring.errors.KeyringError):
            self.store.save_credentials(credentials())
        self.assertEqual(old, self.store.load_credentials())

    def test_refresh_snapshot_survives_profile_write_failure(self):
        old = credentials()
        self.store.save_credentials(old)
        new = credentials()
        new.access_token = "rotated"
        with patch.object(self.store, "_atomic_text", side_effect=OSError("fixture")), self.assertRaises(OSError):
            self.store.save_credentials(new)
        self.assertEqual(new, self.store.load_credentials())

    def test_valid_credentials_do_not_refresh(self):
        value = credentials()
        self.store.save_credentials(value)
        client = Mock()
        self.assertEqual(value, self.store.credentials_for_request(client))
        client.refresh.assert_not_called()

    def test_refresh_preserves_omitted_id_and_scopes(self):
        value = credentials()
        value.expires_in = 0
        self.store.save_credentials(value)
        client = Mock()
        client.refresh.return_value = ChatGPTRefreshResponse(access_token="new-access", refresh_token="new-refresh",
                                                            token_type="Bearer", expires_in=3600)
        new = self.store.credentials_for_request(client)
        self.assertEqual(("new-access", "new-refresh", "fake-id", value.scopes),
                         (new.access_token, new.refresh_token, new.id_token, new.scopes))
        self.assertEqual(new, self.store.load_credentials())

    def test_invalid_refresh_requires_login_without_repeated_refreshes(self):
        value = credentials()
        value.expires_in = 0
        self.store.save_credentials(value)
        client = Mock()
        client.refresh.side_effect = PromptProviderError("authentication", "Sign in")
        for _ in range(2):
            with self.assertRaises(PromptProviderError):
                self.store.credentials_for_request(client)
        self.assertEqual(1, client.refresh.call_count)
        self.assertTrue(self.store.load_credentials().reauth_required)

    def test_transient_refresh_failure_preserves_credentials(self):
        value = credentials()
        value.expires_in = 0
        self.store.save_credentials(value)
        client = Mock()
        client.refresh.side_effect = httpx.ReadTimeout("private bearer")
        with self.assertRaises(PromptProviderError) as caught:
            self.store.credentials_for_request(client)
        self.assertEqual("timeout", caught.exception.category)
        self.assertEqual(value, self.store.load_credentials())

    def test_late_401_does_not_invalidate_newer_token(self):
        value = credentials()
        self.store.save_credentials(value)
        self.store.require_login("fixture", "old-access")
        self.assertFalse(self.store.load_credentials().reauth_required)
        self.store.require_login("fixture", value.access_token)
        self.assertTrue(self.store.load_credentials().reauth_required)

    def test_missing_or_corrupt_snapshot_fails_explicitly(self):
        with self.assertRaises(PromptProviderError):
            self.store.credentials_for_request(Mock())
        self.store.save_credentials(credentials())
        key = next(key for key in self.backend.data if not key.endswith(":meta"))
        del self.backend.data[key]
        with self.assertRaises(PromptProviderError) as caught:
            self.store.credentials_for_request(Mock())
        self.assertEqual("credential_storage", caught.exception.category)

    def test_invalid_grant_and_other_refresh_error_codes(self):
        for code in ("invalid_grant", "invalid_refresh_token", "refresh_token_reused", "refresh_token_expired"):
            request = httpx.Request("POST", "https://fixture.invalid/token")
            reply = httpx.Response(400, request=request, json={"error": code})
            with patch("prompt.chatgpt.token_client.httpx.post", return_value=reply), self.assertRaises(PromptProviderError) as caught:
                ChatGPTTokenClient().refresh("fixture", "fake-refresh")
            self.assertEqual("authentication", caught.exception.category)
            self.assertNotIn("fake-refresh", str(caught.exception))

    def test_refresh_response_validates_new_token_and_optional_fields(self):
        request = httpx.Request("POST", "https://fixture.invalid/token")
        reply = httpx.Response(200, request=request, json=dict(access_token="new", refresh_token="replacement",
                                                             token_type="Bearer", expires_in=3600))
        with patch("prompt.chatgpt.token_client.httpx.post", return_value=reply):
            self.assertIsNone(ChatGPTTokenClient().refresh("fixture", "fake").id_token)
        from pydantic import ValidationError
        with self.assertRaises(ValidationError):
            ChatGPTRefreshResponse(access_token="new", token_type="Bearer", expires_in=3600)

    def test_lock_timeout_and_release(self):
        with session_lock(self.store.lock_path):
            with self.assertRaises(PromptProviderError):
                with session_lock(self.store.lock_path, timeout=0.05):
                    self.fail("second lock acquired")
        with session_lock(self.store.lock_path, timeout=0.05):
            pass

    def test_two_processes_refresh_rotating_token_only_once(self):
        path = self.root / "fake-keyring.json"
        shared = FakeKeyring(path)
        value = credentials()
        value.expires_in = 0
        with fake_keyring(shared):
            self.store.save_credentials(value)
        children = []
        try:
            for index in range(2):
                children.append(subprocess.Popen([sys.executable, "-B", "-m", "tests.test_prompt_providers",
                                                  "--refresh-worker", str(self.root), str(index)],
                                                 stdout=subprocess.PIPE, stderr=subprocess.PIPE))
            deadline = time.monotonic() + 10
            while not all((self.root / f"ready-{i}").exists() for i in range(2)):
                if time.monotonic() > deadline:
                    self.fail("workers did not become ready")
                time.sleep(0.02)
            (self.root / "go").write_text("go")
            for child in children:
                stdout, stderr = child.communicate(timeout=10)
                self.assertEqual(0, child.returncode, stderr.decode())
            self.assertEqual("1", (self.root / "refresh-count").read_text())
            with fake_keyring(shared):
                self.assertEqual("rotated-refresh", self.store.load_credentials().refresh_token)
        finally:
            for child in children:
                if child.poll() is None:
                    child.kill()
                child.communicate()


class ExecutionTests(unittest.TestCase):
    def test_explicit_login_reauth_uses_existing_verified_browser_flow(self):
        from prompt.tests import test_chatgpt_login as login
        from prompt.chatgpt.credentials import ChatGPTTokenResponse
        from types import SimpleNamespace
        with patch.object(login, "ChatGPTCredentialStore") as store_factory, \
                patch.object(login, "ChatGPTTokenClient") as tokens, \
                patch.object(login, "ChatGPTAuth") as auth, \
                patch.object(login, "ChatGPTCallbackServer") as callbacks, \
                patch.object(login, "ChatGPTIdentityVerifier") as verifier, patch.object(login.webbrowser, "open") as browser:
            store = store_factory.return_value
            store.credentials_for_request.side_effect = PromptProviderError("authentication", "Sign in")
            store.load_host_id.return_value = "host"
            auth.create_attempt.return_value = SimpleNamespace(state="state", nonce="nonce", code_verifier="verifier")
            callbacks.return_value.wait_for_callback.return_value = SimpleNamespace(state="state", client_id="fixture", code="code")
            tokens.return_value.exchange_code.return_value = ChatGPTTokenResponse(access_token="fake-access", refresh_token="fake-refresh",
                id_token="fake-id", token_type="Bearer", expires_in=3600, scope="chatgpt.tokens.use.direct")
            verifier.return_value.verify.return_value = SimpleNamespace(email="fixture@example.invalid", issuer="issuer", subject="subject")
            login.main()
            callbacks.return_value.start.assert_called_once()
            browser.assert_called_once()
            verifier.return_value.verify.assert_called_once_with(id_token="fake-id", client_id="fixture", expected_nonce="nonce")
            self.assertFalse(store.save_credentials.call_args.args[0].reauth_required)

    def test_explicit_login_transient_failure_does_not_open_browser(self):
        from prompt.tests import test_chatgpt_login as login
        with patch.object(login, "ChatGPTCredentialStore") as store_factory, \
                patch.object(login.webbrowser, "open") as browser:
            store_factory.return_value.credentials_for_request.side_effect = PromptProviderError("timeout", "Try later")
            with self.assertRaises(PromptProviderError):
                login.main()
            browser.assert_not_called()

    def test_execution_closes_provider_connection(self):
        with patch("prompt.openai_client.OpenAI"):
            client = OpenAIClient("fixture")
        client.client.responses.create.return_value = response()
        with patch("prompt.execution.LlmClientFactory.create", return_value=client):
            improve(PromptImproverConfig(provider="openai", model="fixture"), "task")
        client.client.close.assert_called_once()

    def test_invalid_json_archives_provider_usage_and_sanitized_error(self):
        with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory:
            with patch("prompt.openai_client.OpenAI"):
                client = OpenAIClient("fixture")
            client.client.responses.create.return_value = response(text="secret response that is not JSON")
            with patch("prompt.execution.LlmClientFactory.create", return_value=client), self.assertRaises(PromptProviderError) as caught:
                improve(PromptImproverConfig(provider="openai", model="fixture"), "task", Path(directory))
            self.assertEqual("invalid_structured_output", caught.exception.category)
            metadata = json.loads((Path(directory) / "prompt-usage.json").read_text())
            self.assertEqual("failed", metadata["status"])
            self.assertEqual(100, metadata["usage"]["inputTokens"])
            self.assertNotIn("secret response", json.dumps(metadata))
            self.assertFalse((Path(directory) / "improved-prompt.md").exists())

    def test_factory_failure_and_blank_input_archive_zero_attempts(self):
        for task in ("task", " "):
            with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory:
                with patch("prompt.execution.LlmClientFactory.create", side_effect=ValueError("secret")), self.assertRaises(PromptProviderError):
                    improve(PromptImproverConfig(provider="openai", model="fixture"), task, Path(directory))
                data = json.loads((Path(directory) / "prompt-usage.json").read_text())
                self.assertEqual(0, data["usage"]["requests"])
                self.assertIsNone(data["usage"]["estimatedCostUSD"])

    def test_archive_error_does_not_mask_provider_failure(self):
        with patch("prompt.execution.LlmClientFactory.create", side_effect=RuntimeError("private")), \
                patch("prompt.execution.write_json", side_effect=OSError("fixture disk full")), self.assertRaises(PromptProviderError) as caught:
            improve(PromptImproverConfig(provider="openai", model="fixture"), "task", Path("unused"))
        self.assertIn("archive failed", caught.exception.__notes__[0])

    def test_result_and_csv_keep_improver_separate_and_upgrade_old_rows(self):
        with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory:
            root = Path(directory)
            state = dict(folder=str(root), modules=["prompt"], id="fixture", task="task", before={},
                         promptMetadata=dict(provider="openai", model="fixture", status="completed", errorCategory=None,
                                             durationSeconds=0.5, usage=dict(inputTokens=100, outputTokens=30,
                                             cacheReadTokens=80, cacheWriteTokens=0, reasoningTokens=10, totalTokens=130,
                                             requests=1, completedRequests=1, retries=0, costStatus="unavailable", status="reported")))
            value = runner.enrich_result(state, root, dict(Id="fixture"), None)
            self.assertEqual(100, value["PromptInputTokens"])
            self.assertIsNone(value["InputTokens"])
            self.assertIsNone(value["EstimatedCostUSD"])
            csv_path = root / "results.csv"
            legacy = {k: v for k, v in value.items() if k not in prompt.USAGE_COLUMNS and k not in prompt.METADATA_COLUMNS}
            legacy["Id"] = "old"
            benchmark.append_csv(csv_path, legacy)
            benchmark.append_csv(csv_path, value)
            with csv_path.open(encoding="utf-8") as handle:
                rows = list(csv.DictReader(handle))
            self.assertEqual("", rows[0]["PromptInputTokens"])
            self.assertEqual("100", rows[1]["PromptInputTokens"])


def refresh_worker(root, index):
    root = Path(root)
    backend = FakeKeyring(root / "fake-keyring.json")
    store = ChatGPTCredentialStore(root / "profile", root / "session.lock")
    class Tokens:
        def refresh(self, **kwargs):
            path = root / "refresh-count"
            count = int(path.read_text()) if path.exists() else 0
            path.write_text(str(count + 1))
            if kwargs["refresh_token"] != "fake-refresh":
                raise RuntimeError("duplicate refresh of rotated token")
            time.sleep(0.15)
            return ChatGPTRefreshResponse(access_token="rotated-access", refresh_token="rotated-refresh",
                                          token_type="Bearer", expires_in=3600)
    (root / f"ready-{index}").write_text("ready")
    deadline = time.monotonic() + 10
    while not (root / "go").exists():
        if time.monotonic() > deadline:
            raise RuntimeError("barrier timeout")
        time.sleep(0.02)
    with fake_keyring(backend):
        result = store.credentials_for_request(Tokens())
        assert result.refresh_token == "rotated-refresh"


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--refresh-worker":
        refresh_worker(sys.argv[2], sys.argv[3])
    else:
        unittest.main()
